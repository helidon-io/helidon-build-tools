/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * {@link StagingAction} Container and execute them with provided executor.
 * @param <T> StagingAction
 */
public class Container<T extends StagingAction> implements StagingAction {

    private static final Collection<Callable<CompletionStage<Void>>> TASKS_QUEUE = new ConcurrentLinkedQueue<>();
    private static ExecutorService executor;
    private final String elementName;
    private final List<T> actions;
    private boolean join;

    Container(List<T> actions, String join, String name) {
        this.elementName = name;
        this.actions = actions;
        this.join = Boolean.parseBoolean(join);
    }

    Container(List<T> actions) {
        this(actions, "false", "container");
    }

    /**
     * Nested actions.
     *
     * @return actions
     */
    public List<T> actions() {
        return actions;
    }

    /**
     * Submit {@link Callable} task to be executed.
     *
     * @param task to be executed
     */
    static void submit(Callable<CompletionStage<Void>> task) {
        TASKS_QUEUE.add(task);
    }

    @Override
    public void execute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
        for (T action : actions) {
            action.execute(context, dir, variables);
        }
        if (join) {
            awaitTermination();
        }
    }

    @Override
    public String describe(Path dir, Map<String, String> variables) {
        return elementName + "{"
                + "join=" + join
                + "}";
    }

    @Override
    public String elementName() {
        return elementName;
    }

    static void executor(ExecutorComponent component) {
        if (Objects.isNull(executor)) {
            executor = Objects.isNull(component)
                    ? new ExecutorComponent.CurrentThreadExecutorService()
                    : component.select();
        }
    }

    /**
     * join executor tasks.
     */
    public void join() {
        join = true;
    }

    /**
     * Execute the container tasks.
     */
    public static void awaitTermination() {
        try {
            executor.invokeAll(TASKS_QUEUE).forEach(future -> {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
            TASKS_QUEUE.clear();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
