/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link StagingTask}.
 */
class StagingTaskTest {

    @BeforeAll
    public static void setUp() {
        Container.executor(null);
    }

    @Test
    public void testIterator() throws IOException {
        Variables variables = new Variables();
        variables.add(new Variable("foo", new VariableValue.ListValue("foo1", "foo2")));
        ActionIterators taskIterators = new ActionIterators(List.of(new ActionIterator(variables)));
        TestTask task = new TestTask(taskIterators, "{foo}");
        task.execute(null, null, Map.of());
        assertThat(task.renderedTargets, hasItems("foo1", "foo2"));
    }

    @Test
    public void testIteratorsWithManyVariables() throws IOException {
        Variables variables = new Variables();
        variables.add(new Variable("foo", new VariableValue.ListValue("foo1", "foo2", "foo3")));
        variables.add(new Variable("bar", new VariableValue.ListValue("bar1", "bar2")));
        variables.add(new Variable("bob", new VariableValue.ListValue("bob1", "bob2", "bob3", "bob4")));
        ActionIterators taskIterators = new ActionIterators(List.of(new ActionIterator(variables)));
        TestTask task = new TestTask(taskIterators, "{foo}-{bar}-{bob}");
        task.execute(null, null, Map.of());
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

        TestTask(ActionIterators iterators, String target) {
            super(iterators, target);
            this.renderedTargets = new LinkedList<>();
        }

        @Override
        public void execute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
            super.execute(context, dir, variables);
            Container.awaitTermination();
        }

        @Override
        protected void doExecute(StagingContext context, Path dir, Map<String, String> variables) {
            renderedTargets.add(resolveVar(target(), variables));
        }

        @Override
        public String elementName() {
            return "test";
        }

        @Override
        public String describe(Path dir, Map<String, String> variables) {
            return "test";
        }
    }
}