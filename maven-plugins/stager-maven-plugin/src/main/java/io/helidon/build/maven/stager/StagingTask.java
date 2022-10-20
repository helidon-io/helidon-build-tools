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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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
    private final String target;
    private final boolean join;

    StagingTask(String elementName, List<StagingAction> nested, ActionIterators iterators, Map<String, String> attrs) {
        this.elementName = Strings.requireValid(elementName, "elementName is required");
        this.nested = nested == null ? List.of() : nested;
        this.iterators = iterators;
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
    public String describe(Path dir, Map<String, String> vars) {
        return elementName() + "{"
                + "join=" + join
                + "}";
    }

    @Override
    public CompletionStage<Void> execute(StagingContext ctx, Path dir, Map<String, String> vars) {
        if (iterators == null || iterators.isEmpty()) {
            return execTask(ctx, dir, vars);
        }
        return execIterators(ctx, dir, vars);
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
        return allOf(Lists.map(iterators, it -> execIterations(ctx, dir, it, vars)));
    }

    /**
     * Execute iterator and combine the results into a single stage.
     *
     * @param ctx  staging context
     * @param dir  directory
     * @param vars substitution variables
     * @return completion stage that is completed the task and its sub-tasks have been executed
     */
    protected CompletableFuture<Void> execIterations(StagingContext ctx, Path dir, ActionIterator it, Map<String, String> vars) {
        return allOf(Lists.map(Lists.of(it.baseVariables(vars)), m -> execTask(ctx, dir, m)));
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
        return execNestedTasks(ctx, dir, vars).thenCompose(v -> execBody(ctx, dir, vars));
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
        return allOf(futures);
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
        int taskTimeout = ctx.taskTimeout();
        int maxRetries = ctx.maxRetries();
        if (taskTimeout > 0 && maxRetries > 0) {
            return withTimeout(() -> doExecBody(ctx, dir, vars), ctx::logError, taskTimeout, 0, maxRetries);
        }
        return doExecBody(ctx, dir, vars);
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
        return doExecBody(ctx, dir, vars);
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
        if (source == null || source.isEmpty()) {
            return source;
        }
        for (Map.Entry<String, String> variable : vars.entrySet()) {
            //noinspection RegExpRedundantEscape
            source = source.replaceAll("\\{" + variable.getKey() + "\\}", variable.getValue());
        }
        return source;
    }

    private CompletableFuture<Void> withTimeout(Supplier<CompletableFuture<Void>> supplier,
                                                Consumer<Throwable> consumer,
                                                long timeout,
                                                int retry,
                                                int maxRetry) {

        CompletableFuture<Void> future = supplier.get();
        return future.orTimeout(timeout, TimeUnit.MILLISECONDS)
                     .thenApply(v -> (Throwable) null)
                     .exceptionally(Function.identity())
                     .thenCompose(ex -> {
                         if (ex == null) {
                             return completedStage(null);
                         }
                         Throwable cause = Unchecked.unwrap(ex);
                         consumer.accept(cause);
                         if (retry < maxRetry) {
                             return withTimeout(supplier, consumer, timeout, retry + 1, maxRetry);
                         }
                         return failedStage(cause);
                     });
    }

    private static CompletableFuture<Void> allOf(Collection<CompletableFuture<Void>> futures) {
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }
}
