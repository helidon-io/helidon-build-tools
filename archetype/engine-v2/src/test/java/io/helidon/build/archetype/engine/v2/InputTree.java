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
package io.helidon.build.archetype.engine.v2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.InputTree.Node.Kind;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Input.NamedInput;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Position;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.common.VirtualFileSystem;

import static io.helidon.build.common.PropertyEvaluator.evaluate;
import static java.util.Objects.requireNonNull;

/**
 * Collapses an archetype into a tree containing only the inputs.
 */
public class InputTree {
    private final Node root;
    private final int nodeCount;
    private List<Node> allNodes;

    public static Builder builder() {
        return new Builder();
    }

    public Node root() {
        return root;
    }

    public List<Node> asList() {
        if (allNodes == null) {
            allNodes = new ArrayList<>(nodeCount);
            addNodes(root);
        }
        return allNodes;
    }

    public void print() {
        root.print();
    }

    private InputTree(Builder builder) {
        this.root = builder.root();
        this.nodeCount = builder.nextId;
    }

    private void addNodes(Node node) {
        allNodes.add(node);
        node.children().forEach(this::addNodes);
    }

    /*

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
    */

    public static abstract class Node {
        private int id;      // non-final to allow prune() to fix this to avoid sparse arrays
        private Node parent; // non-final to allow preset siblings to become children
        private final Path script;
        private final int line;
        private final String path;
        private final Kind kind;
        private final List<Node> children;

        enum Kind {
            ROOT,
            PRESETS,
            BOOLEAN,
            ENUM,
            LIST,
            TEXT,
            VALUE
        }

        Node(int id, Node parent, String path, Kind kind, Path script, int line) {
            this.id = id;
            this.script = script;
            this.line = line;
            this.path = path;
            this.kind = kind;
            this.parent = parent;
            this.children = new ArrayList<>();
            if (parent != null) {
                parent.addChild(this);
            }
        }

        int id() {
            return id;
        }

        Path script() {
            return script;
        }

        int line() {
            return line;
        }

        String path() {
            return path;
        }

        Kind kind() {
            return kind;
        }

        Node parent() {
            return parent;
        }

        void addChild(Node child) {
            children.add(child);
        }

        void removeChild(Node child) {
            if (!children.remove(child)) {
                throw new IllegalStateException();
            }
        }

        List<Node> children() {
            return children;
        }

        abstract PermutationIndex createPermutationIndex();

        abstract void collect(PermutationState state, Map<String, String> values);

        abstract boolean isValue();

        private void id(int id) {
            this.id = id;
        }

        private void parent(Node parent) {
            this.parent = parent;
        }

        void print() {
            print(this, 0);
        }

        private void print(Node node, int depth) {
            for (int i = 0; i < depth; i++) {
                System.out.print("|   ");
            }
            System.out.println(node);
            for (Node child : node.children()) {
                print(child, depth + 1);
            }
        }

        protected String toString(Object value) {
            String path = path();
            if (path == null) {
                if (value == null) {
                    return String.format("%d %s from %s:%s", id(), kind(), script(), line());
                } else {
                    return String.format("%d %s '%s' from %s:%s", id(), kind(), value, script(), line());
                }
            } else {
                if (value == null) {
                    return String.format("%d %s '%s' from %s:%s", id(), kind(), path, script(), line());
                } else {
                    return String.format("%d %s '%s' = '%s' from %s:%s", id(), kind(), path, value, script(), line());
                }
            }
        }
    }

    public static class PermutationState {
        private final List<PermutationIndex> states;

        PermutationState(Node root) {
            this.states = new ArrayList<>();
            initState(root);
        }

        private void initState(Node node) {
            states.add(node.id(), node.createPermutationIndex());
            node.children().forEach(this::initState);
        }

        PermutationIndex get(int nodeId) {
            return states.get(nodeId);
        }
    }

    public static class PermutationIndex {
        private final int permutations;
        private int current;
        private boolean completed;

        PermutationIndex(int permutationCount) {
            this.permutations = permutationCount;
        }

        int permutations() {
            return permutations;
        }

        void reset() {
            current = 0;
            completed = false;
        }

        boolean next() {
            if (++current >= permutations) {
                current = 0;
                completed = true;
                return true;
            }
            return false;
        }

        int current() {
            return current;
        }

        boolean completed() {
            return completed;
        }
    }

    public static class InputNode extends Node {

        InputNode(int id, Node parent, Kind kind, String path, Path script, int line) {
            super(id, parent, path, kind, script, line);
        }

        @Override
        PermutationIndex createPermutationIndex() {
            Optional<Node> presets = children().stream().filter(n -> n.kind() == Kind.PRESETS).findFirst();
            if (presets.isPresent()) {
                PresetNode preset = (PresetNode) presets.get();
                // TODO
                return new PermutationIndex(children().size());
            } else {

                // No preset, so all children
                return new PermutationIndex(children().size());
            }
        }
/* TODO

        static class PresetIndex extends PermutationIndex {
             private final PresetNode presets;
             PresetIndex(PresetNode presets, int child) {

                 this.presets = presets;
             }
        }
*/

        @Override
        void collect(PermutationState state, Map<String, String> values) {
            PermutationIndex index = state.get(id());
            int current = index.current();
            Node child = children().get(current);
            child.collect(state, values);
        }

        @Override
        boolean isValue() {
            return false;
        }

        @Override
        public String toString() {
            return toString(null);
        }
    }

    public static class Root extends InputNode {
        Root(int id) {
            super(id, null, Kind.ROOT, "", Path.of("/"), 0);
        }

        @Override
        public String toString() {
            return id() + " ROOT";
        }
    }

    public static class ListNode extends InputNode {
        private final List<String> defaults;

        ListNode(int id, Node parent, String path, List<String> defaults, Path script, int line) {
            super(id, parent, Kind.LIST, path, script, line);
            this.defaults = defaults;
        }

        @Override
        PermutationIndex createPermutationIndex() {
            List<String> values = children().stream().map(c -> ((ValueNode) c).value()).collect(Collectors.toList());
            return ListPermutations.create(values, defaults);
        }

        static class ListPermutations extends PermutationIndex {
            private static final int MAX_PERMUTATIONS = 5;
            private final List<List<String>> permutations; // TODO Convert to List<String> !!

            static ListPermutations create(List<String> values, List<String> defaults) {
                List<List<String>> permutations = new ArrayList<>();
                permutations.add(defaults);

                int size = values.size();
                int permutationCount = MAX_PERMUTATIONS;
                if (size <= MAX_PERMUTATIONS) {
                    // Compute all combinations
                    permutationCount = ListPermutations.factorial(size);
                }

                // TODO ADD permutations!

                return new ListPermutations(permutations);
            }

            ListPermutations(List<List<String>> permutations) {
                super(permutations.size());
                this.permutations = permutations;
            }

            static int factorial(int n) {
                if (n == 0) {
                    return 1;
                }
                return n * factorial(n - 1);
            }
        }

        @Override
        boolean isValue() {
            return false;
        }

        @Override
        void collect(PermutationState state, Map<String, String> values) {
            PermutationIndex index = state.get(id());
            ListPermutations permutations = (ListPermutations) index;
            int current = index.current();
            List<String> permutation = permutations.permutations.get(current);
            StringBuilder b = new StringBuilder();
            permutation.forEach(v -> {
                if (b.length() > 0) {
                    b.append(',');
                }
                b.append(v);
            });

            // TODO unless we can find a way to know that the existing values have NOT changed,
            //      we have to do substitution on each pass. We could implement some pre-parsed
            //      variant to do this faster though.
            String value = evaluate(b.toString(), values);
            values.put(path(), value);
        }
    }

    public static class ValueNode extends Node {
        private final String value;

        ValueNode(int id, Node parent, String path, String value, Path script, int line) {
            super(id, parent, path, Kind.VALUE, script, line);
            this.value = value;
        }

        String value() {
            return value;
        }

        @Override
        PermutationIndex createPermutationIndex() {
            return new PermutationIndex(1);
        }

        @Override
        void collect(PermutationState state, Map<String, String> values) {
            // We can ignore the index since we have only one permutation
            // TODO unless we can find a way to know that the existing values have NOT changed,
            //      we have to do substitution on each pass. We could implement some pre-parsed
            //      variant to do this faster though.
            values.put(path(), evaluate(value, values));
        }

        @Override
        boolean isValue() {
            return true;
        }

        @Override
        public String toString() {
            return toString(value());
        }
    }

    public static class PresetNode extends Node {
        private final Map<String, String> presets;

        PresetNode(int id, Node parent, String path, Path script, int line) {
            super(id, parent, path, Kind.PRESETS, script, line);
            this.presets = new LinkedHashMap<>();
        }

        Map<String, String> presets() {
            return presets;
        }

        @Override
        PermutationIndex createPermutationIndex() {
            return new PermutationIndex(1);
        }

        @Override
        boolean isValue() {
            return true;
        }

        @Override
        void collect(PermutationState state, Map<String, String> values) {
            // We can ignore the index since we have only one permutation
            // TODO unless we can find a way to know that the existing values have NOT changed,
            //      we have to do substitution on each pass. We could implement some pre-parsed
            //      variant to do this faster though.
            presets.keySet().forEach(key -> {
                String value = evaluate(presets.get(key), values);
                values.put(key, value);
            });
        }

        void add(String path, String value) {
            presets.put(path, value);
        }

        Set<String> paths() {
            return presets.keySet();
        }

        @Override
        public String toString() {
            return toString(presets().toString());
        }
    }

    public static class Builder {
        private static final String MAIN_FILE = "main.xml";
        private final Deque<Node> nodes;
        private int nextId;
        private Path archetypePath;
        private String entryPoint;
        private Node root;
        private int presetDepth = -1;

        public Builder archetypePath(Path archetypePath) {
            this.archetypePath = archetypePath;
            return this;
        }

        public Builder entryPointFile(String entryPointFileName) {
            this.entryPoint = entryPointFileName;
            return this;
        }

        public InputTree build() {
            String scriptName = entryPoint == null ? MAIN_FILE : entryPoint;
            FileSystem fs = fileSystem();
            Path cwd = fs.getPath("/");
            Context context = Context.create(cwd, Map.of(), Map.of());
            Script script = ScriptLoader.load(cwd.resolve(scriptName));

            Walker.walk(new AllInputs(this), script, context);
            prune(); // Remove any nodes for which presets exist
            return new InputTree(this);
        }

        void prune() {
            root.print();
            Map<Node, String> matching = new LinkedHashMap<>();
            findPresetInputNodes(root, new HashMap<>(), matching, 0);
            if (!matching.isEmpty()) {
                matching.forEach((node, value) -> {
                    Kind kind = node.kind();
                    if (kind == Kind.BOOLEAN || kind == Kind.ENUM) {
                        // Remove all children except matching
                        List<Node> children = new ArrayList<>(node.children());
                        for (Node child : children) {
                            boolean keep;
                            String childValue = ((ValueNode) child).value();
                            if (kind == Kind.BOOLEAN) {
                                keep = Input.Boolean.valueOf(childValue) == Input.Boolean.valueOf(value);
                            } else {
                                keep = childValue.equals(value);
                            }
                            if (!keep) {
                                node.removeChild(child);
                            }
                        }
                    } else {
                        // Remove node itself
                        Node parent = node.parent();
                        parent.removeChild(node);
                    }
                });
                nextId = 0;
                updateId(root);
            }
        }

        void updateId(Node node) {
            node.id(nextId++);
            for (Node child : node.children()) {
                updateId(child);
            }
        }

        void findPresetInputNodes(Node node, Map<String, String> presets, Map<Node, String> matching, int depth) {
            if (depth < presetDepth) {
                presets.clear();
                presetDepth = -1;
            }
            if (node.kind() == Kind.PRESETS) {
                Map<String, String> current = ((PresetNode) node).presets();
                presets.putAll(current);
                if (presetDepth < 0) {
                    presetDepth = depth;
                }
            } else if (node.kind() != Kind.VALUE && presets.containsKey(node.path())) {
                matching.put(node, presets.get(node.path()));
            }
            for (Node child : node.children()) {
                findPresetInputNodes(child, presets, matching, depth + 1);
            }
        }

        private FileSystem fileSystem() {
            if (Files.isDirectory(archetypePath)) {
                return VirtualFileSystem.create(archetypePath);
            }
            try {
                return FileSystems.newFileSystem(archetypePath, this.getClass().getClassLoader());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        Builder() {
            nodes = new ArrayDeque<>();
            root = new Root(nextId++);
        }

        Node root() {
            return root;
        }

        Node current() {
            return nodes.peek();
        }

        Node pushInput(Kind kind, String path, Path script, Position position) {
            Node parent = parent();
            return push(new InputNode(nextId++, parent, kind, path, script, position.lineNumber()));
        }

        void pushInputList(String path, List<String> defaults, Path script, Position position) {
            Node parent = parent();
            push(new ListNode(nextId++, parent, path, defaults, script, position.lineNumber()));
        }

        void pushPresets(String path, Path script, Position position) {
            Node parent = parent();
            push(new PresetNode(nextId++, parent, path, script, position.lineNumber()));
        }

        void pushValue(String value, Path script, Position position) {
            Node parent = requireNonNull(parent());
            push(new ValueNode(nextId++, parent, parent.path(), value, script, position.lineNumber()));
        }

        void addValue(Node parent, String value, Path script, Position position) {
            // Adds self to parent
            new ValueNode(nextId++, parent, parent.path(), value, script, position.lineNumber());
        }

        Node parent() {
            Node parent = current();
            if (parent == null) {
                parent = root;
            }
            return parent;
        }

        Node push(Node node) {
            nodes.push(node);
            return node;
        }

        void pop() {
            nodes.pop();
        }
    }

    private static class AllInputs extends VisitorAdapter<Context> {
        private final Builder builder;

        AllInputs(Builder builder) {
            super(new InputCollector(builder), null, null);
            this.builder = builder;
        }

        @Override
        public VisitResult visitPreset(Preset preset, Context ctx) {
            String path = preset.path();
            Value value = preset.value();
            ctx.put(path, value);

            PresetNode presets = (PresetNode) builder.current();
            presets.add(path, value.asString());
            return VisitResult.CONTINUE;
        }

        @Override
        public VisitResult visitBlock(Block block, Context ctx) {
            if (block.kind() == Block.Kind.INVOKE_DIR) {
                ctx.pushCwd(block.scriptPath().getParent());
                return VisitResult.CONTINUE;
            } else if (block.kind() == Block.Kind.PRESETS) {
                builder.pushPresets(ctx.path(), block.scriptPath(), block.position());
            }
            return super.visitBlock(block, ctx);
        }

        @Override
        public VisitResult postVisitBlock(Block block, Context ctx) {
            if (block.kind() == Block.Kind.INVOKE_DIR) {
                ctx.popCwd();
                return VisitResult.CONTINUE;
            } else if (block.kind() == Block.Kind.PRESETS) {
                builder.pop();
            }
            return super.postVisitBlock(block, ctx);
        }

        @Override
        public VisitResult visitCondition(Condition condition, Context context) {
            return VisitResult.CONTINUE;
        }

        private static class InputCollector extends InputResolver {
            private static final String TEXT = "text";
            private final Builder builder;

            InputCollector(Builder builder) {
                this.builder = builder;
            }

            String push(NamedInput input, Context context) {
                String path = context.path(input.name());
                if (!input.isGlobal()) {
                    context.push(path, false);
                }
                return path;
            }

            @Override
            public VisitResult visitBoolean(Input.Boolean input, Context context) {
                String path = push(input, context);
                Node node = builder.pushInput(Kind.BOOLEAN, path, input.scriptPath(), input.position());
                builder.pushValue("yes", input.scriptPath(), input.position());
// TODO REMOVE                builder.addValue(node, "no", input.scriptPath(), input.position());
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitText(Input.Text input, Context context) {
                Value defaultValue = defaultValue(input, context);
                String value;
                if (defaultValue == null) {
                    value = TEXT;
                } else {
                    value = defaultValue.asString();
                }
                String path = push(input, context);
                builder.pushInput(Kind.TEXT, path, input.scriptPath(), input.position());
                builder.pushValue(value, input.scriptPath(), input.position());
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitEnum(Input.Enum input, Context context) {
                String path = push(input, context);
                builder.pushInput(Kind.ENUM, path, input.scriptPath(), input.position());
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitList(Input.List input, Context context) {
                String path = push(input, context);
                builder.pushInputList(path, input.defaultValue().asList(), input.scriptPath(), input.position());
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult visitOption(Input.Option option, Context context) {
                String value = option.value();
                builder.pushValue(value, option.scriptPath(), option.position());
                return VisitResult.CONTINUE;
            }

            @Override
            public VisitResult postVisitAny(Input input, Context context) {
                switch (input.kind()) {
                    case BOOLEAN:  {
                        Node parent = builder.current().parent();
                        builder.addValue(parent, "no", input.scriptPath(), input.position());
                    }
                    case TEXT: {
                        // pop 2
                        builder.pop();
                        builder.pop();
                        break;
                    }
                    case ENUM:
                    case LIST:
                    case OPTION: {
                        builder.pop();
                    }
                }
                return super.postVisitAny(input, context);
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
