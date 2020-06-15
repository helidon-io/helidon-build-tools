/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.stager;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link StagingTask}.
 */
class StagingTaskTest {

    @Test
    public void testIterator() throws IOException {
        TestTask task = new TestTask(List.of(Map.of("foo", List.of("foo1", "foo2"))), "{foo}");
        task.execute(null, null, Map.of());
        assertThat(task.renderedTargets, hasItems("foo1", "foo2"));
    }

    @Test
    public void testIteratorsWithManyVariables() throws IOException {
        Map<String, List<String>> taskIterator = new LinkedHashMap<>();
        LinkedList<String> foos = new LinkedList<>();
        foos.add("foo1");
        foos.add("foo2");
        foos.add("foo3");
        taskIterator.put("foo", foos);
        LinkedList<String> bars = new LinkedList<>();
        bars.add("bar1");
        bars.add("bar2");
        taskIterator.put("bar", bars);
        LinkedList<String> bobs = new LinkedList<>();
        bobs.add("bob1");
        bobs.add("bob2");
        bobs.add("bob3");
        bobs.add("bob4");
        taskIterator.put("bob", bobs);
        TestTask task = new TestTask(List.of(taskIterator), "{foo}-{bar}-{bob}");
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

        TestTask(List<Map<String, List<String>>> iterators, String target) {
            super(iterators, target);
            this.renderedTargets = new LinkedList<>();
        }

        @Override
        protected void doExecute(StagingContext context, Path dir, Map<String, String> variables) {
            renderedTargets.add(resolveVar(target(), variables));
        }
    }
}