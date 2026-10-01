/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.build.maven.stager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import io.helidon.build.common.Strings;
import io.helidon.build.common.Unchecked;
import io.helidon.build.common.xml.XMLElement;

import static io.helidon.build.common.Unchecked.unchecked;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.completedStage;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * Base class for all tasks.
 */
class StagingTask implements Joinable {

    private final String name;
    private final Map<String, String> attributes;
    private final List<StagingTask> tasks;
    private final ActionIterators iterators;

    StagingTask(XMLElement element) {
        this(element, null);
    }

    StagingTask(XMLElement element, List<StagingTask> tasks) {
        this.name = element.name();
        this.attributes = element.attributes();
        this.tasks = tasks == null ? List.of() : tasks;
        this.iterators = element.child("iterators")
                .map(ActionIterators::new)
                .orElse(null);
    }

    @Override
    public boolean join() {
        return Boolean.parseBoolean(attributes.get("join"));
    }

    /**
     * Describe the task.
     *
     * @param dir  stage directory
     * @param vars variables for the current iteration
     * @return String that describes the task
     */
    public String toString(Path dir, Map<String, String> vars) {
        return name + "{"
                + "attrs=" + attributes
                + ", dir=" + dir
                + ", vars=" + vars
                + "}";
    }

    /**
     * Execute the action.
     *
     * @param ctx  staging context
     * @param dir  directory
     * @param vars substitution variables
     * @return completion stage that is completed when all tasks have been executed
     */
    public CompletionStage<Void> execute(StagingContext ctx, Path dir, Map<String, String> vars) {
        if (iterators == null || iterators.isEmpty()) {
            return execTask(ctx, dir, vars);
        } else {
            return execIterators(ctx, dir, vars);
        }
    }

    /**
     * Execute iterators and combine the results into a single stage.
     *
     * @param ctx  staging context
     * @param dir  directory
     * @param vars substitution variables
     * @return completion stage that is completed the task and its sub-tasks have been executed
     */
    protected CompletableFuture<Void> execIterators(StagingContext ctx, Path dir, Map<String, String> vars) {
        Span span = new Span(ctx, dir, vars);
        return allOf(iterators, it -> execIterations(ctx, dir, it, vars)).thenRun(span::end);
    }

    /**
     * Execute iterator and combine the results into a single stage.
     *
     * @param ctx  staging context
     * @param dir  directory
     * @param vars substitution variables
     * @return completion stage that is completed the task and its sub-tasks have been executed
     */
    protected CompletableFuture<Void> execIterations(StagingContext ctx,
                                                     Path dir,
                                                     ActionIterator it,
                                                     Map<String, String> vars) {

        Span span = new Span(ctx, dir, vars);
        List<Map<String, String>> itVars = it.forVariables(vars);
        CompletableFuture<Void> future = allOf(itVars, m -> it.join(), m -> execTask(ctx, dir, m));
        return future.thenRun(span::end);
    }

    /**
     * Execute the nested task and then the task body, see {@link #execBody(StagingContext, Path, Map)}.
     *
     * @param ctx  staging context
     * @param dir  directory
     * @param vars substitution variables
     * @return completion stage that is completed the task and its sub-tasks have been executed
     */
    protected CompletableFuture<Void> execTask(StagingContext ctx, Path dir, Map<String, String> vars) {
        Span span = new Span(ctx, dir, vars);
        return execNestedTasks(ctx, dir, vars)
                .thenCompose(v -> execBody(ctx, dir, vars))
                .thenRun(span::end);
    }

    /**
     * Execute the nested tasks.
     *
     * @param ctx  staging context
     * @param dir  directory
     * @param vars substitution variables
     * @return completion stage that is completed the task and its sub-tasks have been executed
     */
    protected CompletableFuture<Void> execNestedTasks(StagingContext ctx, Path dir, Map<String, String> vars) {
        Span span = new Span(ctx, dir, vars);
        CompletableFuture<Void> future = allOf(tasks, task -> task.execute(ctx, dir, vars).toCompletableFuture());
        return future.thenRun(span::end);
    }

    /**
     * Execute the task body.
     *
     * @param ctx  staging context
     * @param dir  directory
     * @param vars substitution variables
     * @return completion stage that is completed the task and its sub-tasks have been executed
     */
    protected CompletableFuture<Void> execBodyWithTimeout(StagingContext ctx, Path dir, Map<String, String> vars) {
        Span span = new Span(ctx, dir, vars);
        int taskTimeout = ctx.taskTimeout();
        int maxRetries = ctx.maxRetries();
        CompletableFuture<Void> future;
        if (taskTimeout > 0 && maxRetries > 0) {
            future = handleTimeout(() -> doExecBody(ctx, dir, vars), ctx, taskTimeout, maxRetries);
        } else {
            future = doExecBody(ctx, dir, vars);
        }
        return future.thenRun(span::end);
    }

    /**
     * Execute the task body.
     * Can be overridden to invoke {@link #execBodyWithTimeout(StagingContext, Path, Map)} in order to support timeouts.
     *
     * @param ctx  staging context
     * @param dir  directory
     * @param vars substitution variables
     * @return completion stage that is completed the task and its sub-tasks have been executed
     */
    protected CompletableFuture<Void> execBody(StagingContext ctx, Path dir, Map<String, String> vars) {
        Span span = new Span(ctx, dir, vars);
        return doExecBody(ctx, dir, vars).thenRun(span::end);
    }

    /**
     * Execute the task body.
     *
     * @param ctx  staging context
     * @param dir  directory
     * @param vars substitution variables
     * @return completion stage that is completed the task and its sub-tasks have been executed
     */
    protected CompletableFuture<Void> doExecBody(StagingContext ctx, Path dir, Map<String, String> vars) {
        CompletableFuture<Void> future = runAsync(unchecked(() -> doExecute(ctx, dir, vars)), ctx.executor());
        return exceptionallyCompose(future, ex -> {
            ctx.logError(ex);
            return failedFuture(ex);
        });
    }

    /**
     * Implementation of the task body.
     *
     * @param ctx  staging context
     * @param dir  stage directory
     * @param vars variables for the current iteration
     * @throws IOException if an IO error occurs
     */
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        // no-op
    }

    /**
     * Resolve variables in a given string.
     *
     * @param source source to be resolved
     * @param vars   variables used to perform the resolution
     * @return resolve string
     */
    protected static String resolveVar(String source, Map<String, String> vars) {
        if (Strings.isValid(source)) {
            for (Map.Entry<String, String> variable : vars.entrySet()) {
                String placeholder = "{" + variable.getKey() + "}";
                int placeholderIndex = source.indexOf(placeholder);
                if (placeholderIndex < 0) {
                    continue;
                }
                StringBuilder resolved = new StringBuilder(source.length());
                int sourceIndex = 0;
                while (placeholderIndex >= 0) {
                    resolved.append(source, sourceIndex, placeholderIndex);
                    if (placeholderIndex > 0 && source.charAt(placeholderIndex - 1) == '$') {
                        resolved.append(placeholder);
                    } else {
                        resolved.append(variable.getValue());
                    }
                    sourceIndex = placeholderIndex + placeholder.length();
                    placeholderIndex = source.indexOf(placeholder, sourceIndex);
                }
                resolved.append(source, sourceIndex, source.length());
                source = resolved.toString();
            }
        }
        return source;
    }

    /**
     * Handle errors and retry until successful.
     *
     * @param supplier    task
     * @param ctx         staging context
     * @param attempt     attempt
     * @param maxAttempts max attempt
     * @return completion stage
     */
    protected static CompletableFuture<Void> handleRetry(Supplier<CompletableFuture<Void>> supplier,
                                                         StagingContext ctx,
                                                         int attempt,
                                                         int maxAttempts) {

        CompletableFuture<Void> future = supplier.get();
        return exceptionallyCompose(future, ex -> {
            ctx.logError(ex);
            if (attempt <= maxAttempts) {
                ctx.logInfo(String.format("retry %d of %d", attempt, maxAttempts));
                return handleRetry(supplier, ctx, attempt + 1, maxAttempts);
            }
            return failedFuture(ex);
        });
    }

    /**
     * Decorate the future supplier to handle timeouts and retry until successful.
     *
     * @param supplier    task
     * @param ctx         staging context
     * @param maxAttempts max attempt
     * @return completion stage
     */
    protected static CompletableFuture<Void> handleTimeout(Supplier<CompletableFuture<Void>> supplier,
                                                           StagingContext ctx,
                                                           long timeout,
                                                           int maxAttempts) {

        return handleRetry(() -> supplier.get().orTimeout(timeout, TimeUnit.MILLISECONDS), ctx, 1, maxAttempts);
    }

    String name() {
        return name;
    }

    List<StagingTask> tasks() {
        return tasks;
    }

    static List<XMLElement> elements(XMLElement element, String name, String wrapper) {
        List<XMLElement> result = new ArrayList<>();
        for (XMLElement child : element.children()) {
            if (name.equals(child.name())) {
                result.add(child);
            } else if (wrapper.equals(child.name())) {
                for (XMLElement wrapped : child.children()) {
                    if (name.equals(wrapped.name())) {
                        result.add(wrapped);
                    }
                }
            }
        }
        return result;
    }

    private static <T> CompletableFuture<Void> allOf(List<T> items,
                                                     Function<T, Boolean> isJoinable,
                                                     Function<T, CompletableFuture<Void>> function) {

        Deque<CompletableFuture<Void>> futures = new ArrayDeque<>();
        futures.push(completedFuture(null));
        for (T item : items) {
            if (isJoinable.apply(item)) {
                CompletableFuture<Void> future = futures.pop();
                futures.push(future.thenCompose(v -> function.apply(item)));
            } else {
                futures.push(function.apply(item));
            }
        }
        return allOf(futures);
    }

    private static <T extends Joinable> CompletableFuture<Void> allOf(List<T> items,
                                                                      Function<T, CompletableFuture<Void>> function) {

        return allOf(items, Joinable::join, function);
    }

    private static CompletableFuture<Void> allOf(Collection<CompletableFuture<Void>> futures) {
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static CompletableFuture<Void> exceptionallyCompose(CompletableFuture<Void> future,
                                                                Function<Throwable, CompletableFuture<Void>> function) {

        return future.thenApply(v -> (Throwable) null)
                     .exceptionally(ex -> ex)
                     .thenCompose(ex -> {
                         if (ex == null) {
                             return completedStage(null);
                         }
                         Throwable cause;
                         if (ex instanceof CompletionException) {
                             cause = Unchecked.unwrap(ex.getCause());
                         } else {
                             cause = Unchecked.unwrap(ex);
                         }
                         return function.apply(cause);
                     });
    }

    private static final AtomicInteger NEXT_SPAN_ID = new AtomicInteger(0);

    private class Span {

        private final long id;
        private final String method;
        private final StagingContext ctx;
        private final Path dir;
        private final Map<String, String> vars;
        private long startTime = 0;

        Span(StagingContext ctx, Path dir, Map<String, String> vars) {
            this.id = NEXT_SPAN_ID.incrementAndGet();
            this.method = StackWalker.getInstance()
                                     .walk(frames -> frames.skip(1)
                                                           .findFirst()
                                                           .map(StackWalker.StackFrame::getMethodName))
                                     .orElse("unknown");
            this.ctx = ctx;
            this.dir = dir;
            this.vars = vars;
            start();
        }

        void start() {
            if (ctx.isDebugEnabled()) {
                startTime = System.currentTimeMillis();
                ctx.logDebug("[trace] [id=%d,t=%d] [start] %s.%s(attrs=%s,dir=%s,vars=%s)",
                        id, startTime, name, method, attributes, dir, vars);
            }
        }

        void end() {
            if (ctx.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                ctx.logDebug("[trace] [id=%d,t=%d] [end] %s.%s(attrs=%s,dir=%s,vars=%s) [total-time=%d]",
                        id, startTime, name, method, attributes, dir, vars, endTime - startTime);
            }
        }
    }
}
