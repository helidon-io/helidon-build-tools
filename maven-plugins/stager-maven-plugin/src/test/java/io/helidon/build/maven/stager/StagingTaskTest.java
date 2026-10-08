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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
    void testIteratorGroups() {
        List<String> versions = new ArrayList<>();
        execute(new StagingTask(XMLElement.read("""
                <task>
                    <iterators>
                        <variables>
                            <variable name="version"><value>1.0.0</value><value>2.0.0</value></variable>
                        </variables>
                        <variables join="true">
                            <variable name="version"><value>3.0.0</value><value>4.0.0</value></variable>
                        </variables>
                    </iterators>
                </task>
                """)) {
            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                versions.add(vars.get("version"));
            }
        }, CURRENT_THREAD);
        assertThat(versions, is(List.of("1.0.0", "2.0.0", "3.0.0", "4.0.0")));
    }

    @Test
    void testIteratorCartesianProduct() {
        List<String> combinations = new ArrayList<>();
        execute(new StagingTask(XMLElement.read("""
                <task>
                    <iterators>
                        <variables>
                            <variable name="letter"><value>a</value><value>b</value></variable>
                            <variable name="number"><value>1</value><value>2</value><value>3</value></variable>
                        </variables>
                    </iterators>
                </task>
                """)) {
            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                combinations.add(vars.get("letter") + vars.get("number"));
            }
        }, CURRENT_THREAD);
        assertThat(combinations, is(List.of("a1", "a2", "a3", "b1", "b2", "b3")));
    }

    @Test
    void testAttributedIteratorValues() {
        List<Map<String, String>> iterations = new ArrayList<>();
        execute(new StagingTask(XMLElement.read("""
                <task>
                    <iterators>
                        <variables>
                            <variable name="version"><value>iterator</value></variable>
                            <variable name="coordinates"><value version="later" classifier="tests"/></variable>
                        </variables>
                    </iterators>
                </task>
                """)) {
            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                iterations.add(Map.copyOf(vars));
            }
        }, CURRENT_THREAD, Map.of("version", "inherited", "repository", "central"));
        assertThat(iterations, is(List.of(
                Map.of("version", "later", "repository", "central", "classifier", "tests"))));
    }

    @Test
    void testEmptyIteratorVariable() {
        AtomicInteger invocations = new AtomicInteger();
        execute(new StagingTask(XMLElement.read("""
                <task>
                    <iterators>
                        <variables>
                            <variable name="version"/>
                        </variables>
                    </iterators>
                </task>
                """)) {
            @Override
            protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
                invocations.incrementAndGet();
            }
        }, CURRENT_THREAD);
        assertThat(invocations.get(), is(0));
    }

    @Test
    void testTraces() {
        List<String> messages = new ArrayList<>();
        StagingTask task = new StagingTask(ELEMENT) {
            @Override
            public String toString() {
                return "DiagnosticTask{name='trace'}";
            }
        };
        StagingContext context = new StagingContext() {
            @Override
            public boolean isDebugEnabled() {
                return true;
            }

            @Override
            public void logDebug(String message, Object... args) {
                messages.add(message.formatted(args));
            }

            @Override
            public Executor executor() {
                return CURRENT_THREAD;
            }
        };

        task.execute(context, Path.of("stage"), Map.of()).toCompletableFuture().join();

        assertThat(messages, hasItem(containsString("[start]")));
        assertThat(messages, hasItem(containsString("[end]")));
        assertThat(messages, everyItem(containsString("DiagnosticTask{name='trace'}")));
        assertThat(messages, everyItem(not(containsString("StagingTask$Span@"))));
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
        execute(task, executor, Map.of());
    }

    static void execute(StagingTask task, Executor executor, Map<String, String> vars) {
        try {
            task.execute(() -> executor, null, vars).toCompletableFuture().get();
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

}
