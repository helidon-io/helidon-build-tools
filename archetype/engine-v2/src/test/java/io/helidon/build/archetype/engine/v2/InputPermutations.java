/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine.v2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Value;

/**
 * TODO: Describe
 */
public class InputPermutations {
    private final List<InputIterator> inputs;
    private final Map<String, String> result;
    private final Map<String, String> immutableResult;
    private final InputIterator firstInput;
    private final int inputCount;
    private final int lastInput;
    private int currentInput;

    static InputPermutations create(Script script, Context context) {
        return new InputPermutations(AllInputs.collect(script, context));
    }

    private InputPermutations(AllInputs.InputCollector collector) {
        this.inputs = collector.inputs;
        this.result = new LinkedHashMap<>();
        this.immutableResult = Collections.unmodifiableMap(result);
        this.firstInput = inputs.get(0);
        this.inputCount = inputs.size();
        this.lastInput = inputCount - 1;
        currentInput = lastInput;
        // Fill the result with the current values. As we iterate, we only update one value at a time
        inputs.forEach(iterator -> result.put(iterator.path, iterator.current()));
    }

    // TODO: tree structured iterators, recursive!

    public int permutations() {
        int permutations = inputs.get(0).size();
        for (int i = 1; i < inputCount; i++) {
            permutations *= inputs.get(i).size();
        }
        return permutations;
    }

    public boolean hasNext() {
        return !firstInput.completed();
    }

    public Map<String, String> next() {

        // Update the result with the next value for the current input

        InputIterator iterator = inputs.get(currentInput);
        String value = iterator.next();
        result.put(iterator.path(), value);

        // Did we wrap?

        if (iterator.wrapped()) {

            // Yes. Are we at the first input?

            if (currentInput > 0) {

                // No, so move up until we reach an iterator that did not wrap
                // Note that this will terminate since we know that the first
                // iterator did not wrap

                do {
                    currentInput--;
                } while (currentInput >= 0 && !inputs.get(currentInput).wrapped());
            }

        } else {

            // No, start over at the end

            currentInput = lastInput;
        }

        return immutableResult;
    }

    private static class InputIterator {
        private final String path;
        private final List<String> values;
        private int current;
        private boolean wrapped;
        private boolean completed;

        private InputIterator(String path, List<String> values) {
            this.path = path;
            this.values = values;
            this.current = 0;
        }

        /**
         * Returns the path of this input.
         *
         * @return The path.
         */
        String path() {
            return path;
        }

        /**
         * Returns the number of values this iterator contains.
         *
         * @return The count.
         */
        int size() {
            return values.size();
        }

        /**
         * Returns whether the last value returned caused this iterator to wrap to the first value.
         *
         * @return true if wrapped.
         */
        boolean wrapped() {
            return wrapped;
        }

        /**
         * Returns whether all values have been returned at least once.
         *
         * @return true if all values have been returned at least once.
         */
        boolean completed() {
            return completed;
        }

        /**
         * Returns the current value.
         *
         * @return The value.
         */
        String current() {
            return values.get(current);
        }

        /**
         * Returns the next value for the input, wrapping around as needed.
         *
         * @return The next value.
         */
        String next() {
            current++;
            wrapped = false;
            if (current >= values.size()) {
                current = 0;
                wrapped = true;
                completed = true;
            }
            return values.get(current);
        }

        @Override
        public String toString() {
            return "InputIterator{" +
                   "path='" + path + '\'' +
                   ", values=" + values +
                   ", current=" + current +
                   ", wrapped=" + wrapped +
                   ", completed=" + completed +
                   '}';
        }
    }

    private static class AllInputs extends VisitorAdapter<Context> {
        private final Set<Path> visitedScripts;

        static InputCollector collect(Script script, Context context) {
            InputCollector collector = new InputCollector();
            Walker.walk(new AllInputs(collector), script, context);
            return collector;
        }

        AllInputs(InputCollector collector) {
            super(collector, null, null);
            this.visitedScripts = new HashSet<>();
        }


        @Override
        public VisitResult visitPreset(Preset preset, Context ctx) {
            ctx.put(preset.path(), preset.value());
            return VisitResult.CONTINUE;
        }

        @Override
        public VisitResult visitBlock(Block block, Context ctx) {
            if (block.kind() == Block.Kind.INVOKE_DIR) {
                ctx.pushCwd(block.scriptPath().getParent());
                return VisitResult.CONTINUE;
            }
            if (block.kind() == Block.Kind.SCRIPT) {
                Path scriptPath = block.scriptPath();
                if (visitedScripts.contains(scriptPath)) {
                    // Skip it, we've already seen it
                    return VisitResult.SKIP_SUBTREE;
                }
                visitedScripts.add(scriptPath);
            }
            return super.visitBlock(block, ctx);
        }

        @Override
        public VisitResult postVisitBlock(Block block, Context ctx) {
            if (block.kind() == Block.Kind.INVOKE_DIR) {
                ctx.popCwd();
                return VisitResult.CONTINUE;
            }
            return super.postVisitBlock(block, ctx);
        }

        @Override
        public VisitResult visitCondition(Condition condition, Context context) {
            return VisitResult.CONTINUE;
        }

        private static class InputCollector extends InputResolver {
            private static final List<String> BOOLEANS = List.of("true", "false");
            private static final List<String> TEXT = List.of("text");
            private final List<InputIterator> inputs = new ArrayList<>();

            VisitResult push(Input.NamedInput input, List<String> values, Context context) {
                String path = context.path(input.name());
                if (!input.isGlobal()) {
                    context.push(path, false);
                }
                inputs.add(new InputIterator(path, values));
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitBoolean(Input.Boolean input, Context context) {
                VisitResult result = onVisitInput(input, context);
                if (result != null) {
                    // TODO: grab values from context?
                    return result;
                }
                return push(input, BOOLEANS, context);
            }

            @Override
            public VisitResult visitText(Input.Text input, Context context) {
                VisitResult result = onVisitInput(input, context);
                if (result != null) {
                    // TODO: grab values from context?
                    return result;
                }
                Value defaultValue = defaultValue(input, context);
                List<String> value;
                if (defaultValue == null) {
                    value = TEXT;
                } else {
                    value = List.of(defaultValue.asString());
                }
                return push(input, value, context);
            }

            @Override
            public VisitResult visitEnum(Input.Enum input, Context context) {
                VisitResult result = onVisitInput(input, context);
                if (result != null) {
                    // TODO: grab values from context?
                    return result;
                }
                List<String> values = values(input);
                return push(input, values, context);
            }

            @Override
            public VisitResult visitList(Input.List input, Context context) {
                VisitResult result = onVisitInput(input, context);
                if (result != null) {
                    // TODO: grab values from context?
                    return result;
                }
                List<String> values = values(input);
                StringBuilder value = new StringBuilder();
                for (int i = 0; i < values.size(); i++) {
                    if (i > 0) {
                        value.append(",");
                    }
                    value.append(values.get(i));
                }
                return push(input, List.of(value.toString()), context);
            }

            @Override
            public VisitResult visitOption(Input.Option option, Context context) {
                return VisitResult.CONTINUE;
            }

            private static List<String> values(Input.Options input) {
                return input.options()
                            .stream()
                            .map(option -> input.normalizeOptionValue(option.value()))
                            .collect(Collectors.toList());
            }
        }
    }
}
