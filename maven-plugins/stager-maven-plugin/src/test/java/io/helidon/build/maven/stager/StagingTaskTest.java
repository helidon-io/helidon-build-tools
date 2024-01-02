/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import io.helidon.build.common.CurrentThreadExecutorService;
import io.helidon.build.common.Unchecked;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link StagingTask}.
 */
class StagingTaskTest {

    @Test
    void testIterator() {
        Variables variables = new Variables();
        variables.add(new Variable("foo", new VariableValue.ListValue("foo1", "foo2")));
        ActionIterators taskIterators = new ActionIterators(List.of(new ActionIterator(variables)), null);
        List<String> renderedTargets = new LinkedList<>();
        StagingTask task = new StagingTask(null, null, taskIterators, Map.of("target", "{foo}")) {
            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                renderedTargets.add(resolveVar(target(), vars));
            }
        };
        StagingContext context = new TestContextImpl(new CurrentThreadExecutorService());
        task.execute(context, null, Map.of());
        assertThat(renderedTargets, hasItems("foo1", "foo2"));
    }

    @Test
    void testIteratorsWithManyVariables() {
        Variables variables = new Variables();
        variables.add(new Variable("foo", new VariableValue.ListValue("foo1", "foo2", "foo3")));
        variables.add(new Variable("bar", new VariableValue.ListValue("bar1", "bar2")));
        variables.add(new Variable("bob", new VariableValue.ListValue("bob1", "bob2", "bob3", "bob4")));
        ActionIterators taskIterators = new ActionIterators(List.of(new ActionIterator(variables)), null);
        List<String> renderedTargets = new LinkedList<>();
        StagingTask task = new StagingTask(null, null, taskIterators, Map.of("target", "{foo}-{bar}-{bob}")) {
            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                renderedTargets.add(resolveVar(target(), vars));
            }
        };
        StagingContext context = new TestContextImpl(new CurrentThreadExecutorService());
        task.execute(context, null, Map.of());
        assertThat(renderedTargets, hasItems(
                "foo1-bar1-bob1", "foo1-bar1-bob2", "foo1-bar1-bob3", "foo1-bar1-bob4",
                "foo1-bar2-bob1", "foo1-bar2-bob2", "foo1-bar2-bob3", "foo1-bar2-bob4",
                "foo2-bar1-bob1", "foo2-bar1-bob2", "foo2-bar1-bob3", "foo2-bar1-bob4",
                "foo2-bar2-bob1", "foo2-bar2-bob2", "foo2-bar2-bob3", "foo2-bar2-bob4",
                "foo3-bar1-bob1", "foo3-bar1-bob2", "foo3-bar1-bob3", "foo3-bar1-bob4",
                "foo3-bar2-bob1", "foo3-bar2-bob2", "foo3-bar2-bob3", "foo3-bar2-bob4"));
    }

    @Test
    void testHandleRetry() throws InterruptedException {
        StagingTask task = new StagingTask() {

            @Override
            protected CompletableFuture<Void> execBody(StagingContext ctx, Path dir, Map<String, String> vars) {
                return handleRetry(() -> doExecBody(ctx, dir, vars), ctx, 1, 3);
            }

            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                throw new UnsupportedOperationException("boo");
            }
        };
        StagingContext context = new TestContextImpl(new CurrentThreadExecutorService());
        try {
            task.execute(context, null, Map.of()).toCompletableFuture().get();
            Assertions.fail();
        } catch (ExecutionException e) {
            assertThat(e.getCause(), is(instanceOf(UnsupportedOperationException.class)));
        }
    }

    @Test
    void testHandleTimeout() throws InterruptedException {
        StagingTask task = new StagingTask() {

            @Override
            protected CompletableFuture<Void> execBody(StagingContext ctx, Path dir, Map<String, String> vars) {
                return handleTimeout(() -> doExecBody(ctx, dir, vars), ctx, 500, 0);
            }

            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                sleep(60);
            }
        };
        StagingContext context = new TestContextImpl(Executors.newSingleThreadExecutor());
        try {
            task.execute(context, null, Map.of()).toCompletableFuture().get();
            Assertions.fail("task should timeout");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), is(instanceOf(TimeoutException.class)));
        }
    }

    @Test
    void testNoFailFast() throws InterruptedException {
        AtomicInteger count = new AtomicInteger();
        Function<Throwable, Void> exceptionally = ex -> {
            count.incrementAndGet();
            throw Unchecked.wrap(ex);
        };
        StagingTask subTask1 = new StagingTask() {

            @Override
            public CompletionStage<Void> execute(StagingContext ctx, Path dir, Map<String, String> vars) {
                return super.execute(ctx, dir, vars).exceptionally(exceptionally);
            }

            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                sleep(5); // sleep to ensure the 2nd sub-task fails first
                throw new IllegalStateException();
            }
        };
        StagingTask task = withFailedSubTask(exceptionally, subTask1);
        StagingContext context = new TestContextImpl(Executors.newCachedThreadPool());
        try {
            task.execute(context, null, Map.of()).toCompletableFuture().get();
            Assertions.fail("task should fail");
        } catch (ExecutionException e) {
            assertThat(count.get(), is(2));
            assertThat(e.getCause(), is(instanceOf(IllegalStateException.class)));
        }
    }

    @Test
    void testFailedSiblingNoJoin() throws InterruptedException {
        StagingTask task = new StagingTask(null, List.of(
                new StagingTask() {

                    @Override
                    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                        throw new IllegalStateException();
                    }
                }, new StagingTask()),
                null, null);
        StagingContext context = new TestContextImpl(Executors.newCachedThreadPool());
        try {
            task.execute(context, null, Map.of()).toCompletableFuture().get();
            Assertions.fail("task should fail");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), is(instanceOf(IllegalStateException.class)));
        }
    }

    @Test
    void testFailedJoin() throws InterruptedException {
        List<Integer> list = Collections.synchronizedList(new LinkedList<>());
        StagingTask task = new StagingTask(null, List.of(
                new StagingTask() {

                    @Override
                    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                        list.add(1);
                        throw new IllegalStateException();
                    }
                }, new StagingTask(null, null, null, Map.of("join", "true")) {
                    @Override
                    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                        list.add(2);
                    }
                }),
                null, null);
        StagingContext context = new TestContextImpl(Executors.newCachedThreadPool());
        try {
            task.execute(context, null, Map.of()).toCompletableFuture().get();
            Assertions.fail("task should fail");
        } catch (ExecutionException e) {
            assertThat(list, is(List.of(1)));
            assertThat(e.getCause(), is(instanceOf(IllegalStateException.class)));
        }
    }

    @Test
    void testJoin() throws InterruptedException, ExecutionException {
        List<Integer> list = Collections.synchronizedList(new LinkedList<>());
        StagingTask task = new StagingTask(null, List.of(
                new StagingTask() {

                    @Override
                    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                        sleep(5);
                        list.add(1);
                    }
                }, new StagingTask(null, null, null, Map.of("join", "true")) {
                    @Override
                    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                        list.add(2);
                    }
                }),
                null, null);
        StagingContext context = new TestContextImpl(Executors.newCachedThreadPool());
        task.execute(context, null, Map.of()).toCompletableFuture().get();
        assertThat(list, is(List.of(1, 2)));
    }

    private static StagingTask withFailedSubTask(Function<Throwable, Void> exceptionally, StagingTask subTask1) {
        return new StagingTask(null, List.of(subTask1, new StagingTask() {

            @Override
            public CompletionStage<Void> execute(StagingContext ctx, Path dir, Map<String, String> vars) {
                return super.execute(ctx, dir, vars).exceptionally(exceptionally);
            }

            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                throw new IllegalStateException();
            }
        }), null, null);
    }

    static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    record TestContextImpl(Executor executor) implements StagingContext {
    }
}