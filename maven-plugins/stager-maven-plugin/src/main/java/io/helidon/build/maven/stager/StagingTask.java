/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import io.helidon.build.common.Lists;
import io.helidon.build.common.Strings;
import io.helidon.build.common.Unchecked;

import static io.helidon.build.common.Unchecked.unchecked;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.completedStage;
import static java.util.concurrent.CompletableFuture.failedStage;
import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * Base class for all tasks.
 */
abstract class StagingTask implements StagingAction {

    private final String elementName;
    private final List<StagingAction> nested;
    private final ActionIterators iterators;
    private final Map<String, String> attrs;
    private final String target;
    private final boolean join;

    StagingTask(String elementName, List<StagingAction> nested, ActionIterators iterators, Map<String, String> attrs) {
        this.elementName = Strings.requireValid(elementName, "elementName is required");
        this.nested = nested == null ? List.of() : nested;
        this.iterators = iterators;
        this.attrs = Objects.requireNonNull(attrs, "attrs is null");
        this.target = Strings.requireValid(attrs.get("target"), "target is required");
        this.join = Boolean.parseBoolean(attrs.get("join"));
    }

    /**
     * Get the element name.
     *
     * @return name
     */
    public String elementName() {
        return elementName;
    }

    /**
     * Get the nested tasks.
     *
     * @return tasks, never {@code null}
     */
    List<StagingAction> tasks() {
        return nested;
    }

    /**
     * Get the task iterators.
     *
     * @return task iterators, may be {@code null}
     */
    ActionIterators iterators() {
        return iterators;
    }

    /**
     * Get the target.
     *
     * @return target, never {@code nul}
     */
    String target() {
        return target;
    }

    @Override
    public boolean join() {
        return join;
    }

    @Override
    public String toString(Path dir, Map<String, String> vars) {
        return elementName + "{"
                + "attrs=" + attrs
                + ", dir=" + dir
                + ", vars=" + vars
                + "}";
    }

    @Override
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
        return allOf(Lists.map(iterators, it -> execIterations(ctx, dir, it, vars))).thenRun(span::end);
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
        return allOf(Lists.map(Lists.of(it.forVariables(vars)), m -> execTask(ctx, dir, m)))
                .thenRun(span::end);
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
        Deque<CompletableFuture<Void>> futures = new ArrayDeque<>();
        futures.push(completedFuture(null));
        for (StagingAction task : nested) {
            CompletableFuture<Void> future = futures.pop();
            if (task.join()) {
                futures.push(future.thenCompose(v -> task.execute(ctx, dir, vars)));
            } else {
                futures.push(task.execute(ctx, dir, vars).toCompletableFuture());
            }
        }
        return allOf(futures).thenRun(span::end);
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
        return runAsync(unchecked(() -> doExecute(ctx, dir, vars)), ctx.executor());
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
                source = source.replaceAll("\\{" + variable.getKey() + "}", variable.getValue());
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
        return future.thenApply(v -> {
                        return (Throwable) null;
                     })
                     .exceptionally(ex -> {
                         return ex;
                     })
                     .thenCompose(ex -> {
                         if (ex == null) {
                             return completedStage(null);
                         }
                         Throwable cause = Unchecked.unwrap(ex);
                         ctx.logError(cause);
                         if (attempt <= maxAttempts) {
                             ctx.logInfo(String.format("retry %d of %d", attempt, maxAttempts));
                             return handleRetry(supplier, ctx, attempt + 1, maxAttempts);
                         }
                         return failedStage(cause);
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

    private static CompletableFuture<Void> allOf(Collection<CompletableFuture<Void>> futures) {
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static final AtomicInteger NEXT_SPAN_ID = new AtomicInteger(0);

    private class Span {

        private final long id;
        private final String method;
        private final StagingContext ctx;
        private final Path dir;
        private final Map<String, String> vars;
        private long startTime = 0;

        public Span(StagingContext ctx, Path dir, Map<String, String> vars) {
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

        private void start() {
            if (ctx.isDebugEnabled()) {
                startTime = System.currentTimeMillis();
                ctx.logDebug("[trace] [id=%d,t=%d] [start] %s.%s(attrs=%s,dir=%s,vars=%s)",
                        id, startTime, elementName, method, attrs, dir, vars);
            }
        }

        void end() {
            if (ctx.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                ctx.logDebug("[trace] [id=%d,t=%d] [end] %s.%s(attrs=%s,dir=%s,vars=%s) [total-time=%d]",
                        id, startTime, elementName, method, attrs, dir, vars, endTime - startTime);
            }
        }
    }
}
