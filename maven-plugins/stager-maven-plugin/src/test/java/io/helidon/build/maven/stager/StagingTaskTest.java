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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import io.helidon.build.common.CurrentThreadExecutorService;

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
        ActionIterators taskIterators = new ActionIterators(List.of(new ActionIterator(variables)));
        TestTask task = new TestTask(taskIterators, Map.of("target", "{foo}"));
        StagingContext context = new TestContextImpl(new CurrentThreadExecutorService());
        task.execute(context, null, Map.of());
        assertThat(task.renderedTargets, hasItems("foo1", "foo2"));
    }

    @Test
    void testIteratorsWithManyVariables() {
        Variables variables = new Variables();
        variables.add(new Variable("foo", new VariableValue.ListValue("foo1", "foo2", "foo3")));
        variables.add(new Variable("bar", new VariableValue.ListValue("bar1", "bar2")));
        variables.add(new Variable("bob", new VariableValue.ListValue("bob1", "bob2", "bob3", "bob4")));
        ActionIterators taskIterators = new ActionIterators(List.of(new ActionIterator(variables)));
        TestTask task = new TestTask(taskIterators, Map.of("target", "{foo}-{bar}-{bob}"));
        StagingContext context = new TestContextImpl(new CurrentThreadExecutorService());
        task.execute(context, null, Map.of());
        assertThat(task.renderedTargets, hasItems(
                "foo1-bar1-bob1", "foo1-bar1-bob2", "foo1-bar1-bob3", "foo1-bar1-bob4",
                "foo1-bar2-bob1", "foo1-bar2-bob2", "foo1-bar2-bob3", "foo1-bar2-bob4",
                "foo2-bar1-bob1", "foo2-bar1-bob2", "foo2-bar1-bob3", "foo2-bar1-bob4",
                "foo2-bar2-bob1", "foo2-bar2-bob2", "foo2-bar2-bob3", "foo2-bar2-bob4",
                "foo3-bar1-bob1", "foo3-bar1-bob2", "foo3-bar1-bob3", "foo3-bar1-bob4",
                "foo3-bar2-bob1", "foo3-bar2-bob2", "foo3-bar2-bob3", "foo3-bar2-bob4"));
    }

    @Test
    void testHandleRetry() throws InterruptedException {
        StagingTask task = new StagingTask("test", null, null, Map.of("target", "/dev/null")) {

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
        StagingTask task = new StagingTask("test", null, null, Map.of("target", "/dev/null")) {

            @Override
            protected CompletableFuture<Void> execBody(StagingContext ctx, Path dir, Map<String, String> vars) {
                return handleTimeout(() -> doExecBody(ctx, dir, vars), ctx, 500, 0);
            }

            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
                throw new IOException("foo");
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
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

    // TODO test a pseudo downloads task with multiple nested task that fail

    static final class TestTask extends StagingTask {

        private final List<String> renderedTargets;

        TestTask(ActionIterators iterators, Map<String, String> attrs) {
            super("test", null, iterators, attrs);
            renderedTargets = new LinkedList<>();
        }

        @Override
        protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
            renderedTargets.add(resolveVar(target(), vars));
        }
    }

    static class TestContextImpl implements StagingContext {

        final Executor executor;

        TestContextImpl(Executor executor) {
            this.executor = executor;
        }

        @Override
        public Executor executor() {
            return executor;
        }
    }
}