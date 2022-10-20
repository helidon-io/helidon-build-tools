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

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import io.helidon.build.common.CurrentThreadExecutorService;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link StagingTask}.
 */
class StagingTaskTest {

    private final StagingContext context = new ContextTestImpl();

    @Test
    public void testIterator() {
        Variables variables = new Variables();
        variables.add(new Variable("foo", new VariableValue.ListValue("foo1", "foo2")));
        ActionIterators taskIterators = new ActionIterators(List.of(new ActionIterator(variables)));
        TestTask task = new TestTask(taskIterators, Map.of("target", "{foo}"));
        task.execute(context, null, Map.of());
        assertThat(task.renderedTargets, hasItems("foo1", "foo2"));
    }

    @Test
    public void testIteratorsWithManyVariables() {
        Variables variables = new Variables();
        variables.add(new Variable("foo", new VariableValue.ListValue("foo1", "foo2", "foo3")));
        variables.add(new Variable("bar", new VariableValue.ListValue("bar1", "bar2")));
        variables.add(new Variable("bob", new VariableValue.ListValue("bob1", "bob2", "bob3", "bob4")));
        ActionIterators taskIterators = new ActionIterators(List.of(new ActionIterator(variables)));
        TestTask task = new TestTask(taskIterators, Map.of("target", "{foo}-{bar}-{bob}"));
        task.execute(context, null, Map.of());
        assertThat(task.renderedTargets, hasItems(
                "foo1-bar1-bob1", "foo1-bar1-bob2", "foo1-bar1-bob3", "foo1-bar1-bob4",
                "foo1-bar2-bob1", "foo1-bar2-bob2", "foo1-bar2-bob3", "foo1-bar2-bob4",
                "foo2-bar1-bob1", "foo2-bar1-bob2", "foo2-bar1-bob3", "foo2-bar1-bob4",
                "foo2-bar2-bob1", "foo2-bar2-bob2", "foo2-bar2-bob3", "foo2-bar2-bob4",
                "foo3-bar1-bob1", "foo3-bar1-bob2", "foo3-bar1-bob3", "foo3-bar1-bob4",
                "foo3-bar2-bob1", "foo3-bar2-bob2", "foo3-bar2-bob3", "foo3-bar2-bob4"));
    }

    private static final class TestTask extends StagingTask {

        private final List<String> renderedTargets;

        TestTask(ActionIterators iterators, Map<String, String> attrs) {
            super("test", null, iterators, attrs);
            this.renderedTargets = new LinkedList<>();
        }

        @Override
        protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
            renderedTargets.add(resolveVar(target(), vars));
        }

        @Override
        public String elementName() {
            return "test";
        }

        @Override
        public String describe(Path dir, Map<String, String> vars) {
            return "test";
        }
    }

    private static final class ContextTestImpl implements StagingContext {

        private final Executor executor = new CurrentThreadExecutorService();

        @Override
        public void unpack(Path archive, Path target, String excludes, String includes) {
        }

        @Override
        public void archive(Path directory, Path target, String excludes, String includes) {
        }

        @Override
        public Path resolve(String path) {
            return null;
        }

        @Override
        public Path resolve(ArtifactGAV gav) {
            return null;
        }

        @Override
        public Path createTempDirectory(String prefix) {
            return null;
        }

        @Override
        public void logInfo(String msg, Object... args) {
        }

        @Override
        public void logWarning(String msg, Object... args) {
        }

        @Override
        public void logError(String msg, Object... args) {
        }

        @Override
        public void logDebug(String msg, Object... args) {
        }

        @Override
        public Executor executor() {
            return executor;
        }
    }
}