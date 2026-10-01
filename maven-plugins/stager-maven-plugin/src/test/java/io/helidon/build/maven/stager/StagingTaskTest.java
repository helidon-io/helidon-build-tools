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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.build.common.CurrentThreadExecutorService;
import io.helidon.build.common.Unchecked;
import io.helidon.build.common.xml.XMLElement;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link StagingTask}.
 */
class StagingTaskTest {

    static final XMLElement ELEMENT = XMLElement.builder()
            .name("task")
            .build();

    static final Executor CURRENT_THREAD = new CurrentThreadExecutorService();

    @Test
    void testResolveVar() {
        Map<String, String> vars = Map.of(
                "version", "1.2.3",
                "special", "$value\\path");

        assertThat(StagingTask.resolveVar("{version}", vars), is("1.2.3"));
        assertThat(StagingTask.resolveVar("${version}", vars), is("${version}"));
        assertThat(StagingTask.resolveVar("stager-{version}-maven-${version}", vars), is("stager-1.2.3-maven-${version}"));
        assertThat(StagingTask.resolveVar("{special}", vars), is("$value\\path"));
    }

    @Test
    void testIterator() {
        List<String> targets = new ArrayList<>();
        execute(new TestTask(XMLElement.read("""
                <task target="{it}">
                    <iterators>
                        <variables>
                            <variable name="it">
                                <value>one</value>
                                <value>two</value>
                            </variable>
                        </variables>
                    </iterators>
                </task>
                """)) {
            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                targets.add(resolveVar(target(), vars));
            }
        }, CURRENT_THREAD);
        assertThat(targets, hasItems("one", "two"));
    }

    @Test
    void testEmptyIteratorVariable() {
        List<String> targets = new ArrayList<>();
        execute(new TestTask(XMLElement.read("""
                <task target="{version}">
                    <iterators>
                        <variables>
                            <variable name="version"/>
                        </variables>
                    </iterators>
                </task>
                """)) {

            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                targets.add(resolveVar(target(), vars));
            }
        }, CURRENT_THREAD);
        assertThat(targets, is(empty()));
    }

    @Test
    void testIteratorPreserveOrder() {
        List<String> targets = new ArrayList<>();
        execute(new TestTask(XMLElement.read("""
                <task target="{version}">
                    <iterators>
                        <variables>
                            <variable name="version">
                                <value>1.0.0</value>
                            </variable>
                        </variables>
                        <variables>
                            <variable name="version">
                                <value>2.0.0</value>
                                <value>3.0.0</value>
                            </variable>
                        </variables>
                    </iterators>
                </task>
                """)) {

            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                targets.add(resolveVar(target(), vars));
            }
        }, CURRENT_THREAD);
        assertThat(targets, is(List.of("1.0.0", "2.0.0", "3.0.0")));
    }

    @Test
    void testIteratorVariableInterpolation() {
        List<String> targets = new ArrayList<>();
        execute(new TestTask(XMLElement.read("""
                <task target="{version}/{channel}">
                    <iterators>
                        <variables join="true">
                            <variable name="release">
                                <value version="${stable.version}" channel="stable"/>
                                <value version="4.3.0" channel="preview"/>
                            </variable>
                        </variables>
                    </iterators>
                </task>
                """)) {

            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                targets.add(resolveVar(target(), vars));
            }
        }, CURRENT_THREAD);
        assertThat(targets, is(List.of("${stable.version}/stable", "4.3.0/preview")));
    }

    @Test
    void testIteratorsCartesianProduct() {
        List<String> targets = new ArrayList<>();
        execute(new TestTask(XMLElement.read("""
                <task target="{foo}-{bar}-{bob}">
                    <iterators>
                        <variables>
                            <variable name="foo"><value>foo1</value><value>foo2</value><value>foo3</value></variable>
                            <variable name="bar"><value>bar1</value><value>bar2</value></variable>
                            <variable name="bob">
                                <value>bob1</value><value>bob2</value><value>bob3</value><value>bob4</value>
                            </variable>
                        </variables>
                    </iterators>
                </task>
                """)) {
            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                targets.add(resolveVar(target(), vars));
            }
        }, CURRENT_THREAD);
        assertThat(targets, hasItems(
                "foo1-bar1-bob1", "foo1-bar1-bob2", "foo1-bar1-bob3", "foo1-bar1-bob4",
                "foo1-bar2-bob1", "foo1-bar2-bob2", "foo1-bar2-bob3", "foo1-bar2-bob4",
                "foo2-bar1-bob1", "foo2-bar1-bob2", "foo2-bar1-bob3", "foo2-bar1-bob4",
                "foo2-bar2-bob1", "foo2-bar2-bob2", "foo2-bar2-bob3", "foo2-bar2-bob4",
                "foo3-bar1-bob1", "foo3-bar1-bob2", "foo3-bar1-bob3", "foo3-bar1-bob4",
                "foo3-bar2-bob1", "foo3-bar2-bob2", "foo3-bar2-bob3", "foo3-bar2-bob4"));
    }

    @Test
    void testHandleRetry() {
        AtomicInteger count = new AtomicInteger();
        assertThrows(UnsupportedOperationException.class, () -> {
            // force a retry
            execute(new StagingTask(ELEMENT) {

                @Override
                protected CompletableFuture<Void> execBody(StagingContext ctx, Path dir, Map<String, String> vars) {
                    return handleRetry(() -> doExecBody(ctx, dir, vars), ctx, 1, 3);
                }

                @Override
                protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                    count.incrementAndGet();
                    throw new UnsupportedOperationException();
                }
            }, CURRENT_THREAD);
        });
        assertThat(count.get(), is(4));
    }

    @Test
    void testHandleTimeout() {
        RuntimeException re = assertThrows(RuntimeException.class, () -> {
            // force a timeout
            execute(new StagingTask(ELEMENT) {

                @Override
                protected CompletableFuture<Void> execBody(StagingContext ctx, Path dir, Map<String, String> vars) {
                    return handleTimeout(() -> doExecBody(ctx, dir, vars), ctx, 500, 0);
                }

                @Override
                protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                    sleep(60);
                }
            }, Executors.newSingleThreadExecutor());
        });
        assertThat(re.getCause(), is(instanceOf(TimeoutException.class)));
    }

    @Test
    void testNoFailFast() {
        AtomicInteger count = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> {
            // first tasks executed after delay even though the 2nd task failed
            execute(new StagingTask(ELEMENT, List.of(new StagingTask(ELEMENT) {

                @Override
                public CompletionStage<Void> execute(StagingContext ctx, Path dir, Map<String, String> vars) {
                    return super.execute(ctx, dir, vars).exceptionally(ex -> {
                        count.incrementAndGet();
                        throw Unchecked.wrap(ex);
                    });
                }

                @Override
                protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                    sleep(5); // sleep to ensure the 2nd sub-task fails first
                    throw new IllegalStateException();
                }
            }, new StagingTask(ELEMENT) {

                @Override
                public CompletionStage<Void> execute(StagingContext ctx, Path dir, Map<String, String> vars) {
                    return super.execute(ctx, dir, vars).exceptionally(ex -> {
                        count.incrementAndGet();
                        throw Unchecked.wrap(ex);
                    });
                }

                @Override
                protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                    throw new IllegalStateException();
                }
            })), Executors.newCachedThreadPool());
        });
        assertThat(count.get(), is(2));
    }

    @Test
    void testFailedSiblingNoJoin() {
        assertThrows(IllegalStateException.class, () -> {
            // first child fails
            execute(new StagingTask(ELEMENT, List.of(new StagingTask(ELEMENT) {

                @Override
                protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                    throw new IllegalStateException();
                }
            }, new StagingTask(ELEMENT))), Executors.newCachedThreadPool());
        });
    }

    @Test
    void testFailedJoin() {
        List<Integer> result = Collections.synchronizedList(new ArrayList<>());
        assertThrows(IllegalStateException.class, () -> {
            // first child fails, 2nd child joins
            execute(new StagingTask(ELEMENT, List.of(new StagingTask(ELEMENT) {

                @Override
                protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                    result.add(1);
                    throw new IllegalStateException();
                }
            }, new StagingTask(XMLElement.read("<task join=\"true\"/>")) {
                @Override
                protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                    result.add(2);
                }
            })), Executors.newCachedThreadPool());
        });
        assertThat(result, is(List.of(1)));
    }

    @Test
    void testJoin() {
        List<Integer> result = Collections.synchronizedList(new ArrayList<>());
        execute(new StagingTask(ELEMENT, List.of(new StagingTask(ELEMENT) {

            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                sleep(5);
                result.add(1);
            }
        }, new StagingTask(XMLElement.read("<task join=\"true\"/>")) {
            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                result.add(2);
            }
        })), Executors.newCachedThreadPool());
        assertThat(result, is(List.of(1, 2)));
    }

    static void execute(StagingTask task, Executor executor) {
        try {
            task.execute(() -> executor, null, Map.of()).toCompletableFuture().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e.getCause());
        }
    }

    static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static class TestTask extends StagingTask {

        private final String target;

        TestTask(XMLElement element) {
            super(element);
            this.target = element.attribute("target", null);
        }

        String target() {
            return target;
        }
    }
}
