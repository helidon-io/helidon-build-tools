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
package io.helidon.build.archetype.engine.v2.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.archetype.engine.v2.Context;
import io.helidon.build.archetype.engine.v2.InputResolver;
import io.helidon.build.archetype.engine.v2.ScriptLoader;
import io.helidon.build.archetype.engine.v2.VisitorAdapter;
import io.helidon.build.archetype.engine.v2.Walker;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Condition;
import io.helidon.build.archetype.engine.v2.ast.Input;
import io.helidon.build.archetype.engine.v2.ast.Input.NamedInput;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Position;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Script;
import io.helidon.build.archetype.engine.v2.ast.Value;
import io.helidon.build.archetype.engine.v2.ast.ValueTypes;
import io.helidon.build.archetype.engine.v2.util.InputTree.Node.Kind;
import io.helidon.build.common.GenericType;
import io.helidon.build.common.VirtualFileSystem;

import static io.helidon.build.common.PropertyEvaluator.evaluate;
import static java.util.Objects.requireNonNull;

/**
 * Collapses an archetype into a tree containing only the inputs, primarily to support
 * generating input combinations.
 */
public class InputTree {
    private final BiFunction<List<String>, List<String>, List<List<String>>> listCombiner;
    private final Node root;
    private final int nodeCount;
    private List<Node> allNodes;

    /**
     * Returns a new builder.
     *
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the root node.
     *
     * @return The root.
     */
    public Node root() {
        return root;
    }

    /**
     * Returns the tree flattened into a list, in depth-first order.
     *
     * @return The list.
     */
    public List<Node> asList() {
        if (allNodes == null) {
            allNodes = new ArrayList<>(nodeCount);
            addNodes(root);
        }
        return allNodes;
    }

    /**
     * Returns the tree as a stream, in depth-first order.
     *
     * @return The stream.
     */
    public Stream<Node> stream() {
        return asList().stream();
    }

    /**
     * Returns the number of nodes in the tree.
     *
     * @return The count.
     */
    public int size() {
        return nodeCount;
    }

    /**
     * Prints the tree to standard out.
     */
    public void print() {
        root.print();
    }

    /**
     * Collect the current combination.
     *
     * @param combination where to add the combination
     */
    public void collect(Map<String, String> combination) {
        root.collect(combination);
    }


    private InputTree(Builder builder) {
        this.listCombiner = builder.listCombiner;
        this.root = builder.root();
        this.nodeCount = builder.nextId;
    }

    private void addNodes(Node node) {
        allNodes.add(node);
        node.children().forEach(this::addNodes);
    }

    public abstract static class Node {
        private int id;      // non-final to allow prune() to fix this to avoid sparse arrays
        private Node parent; // non-final to allow preset siblings to become children
        private final Path script;
        private final int line;
        private final String path;
        private final Kind kind;
        private final List<Node> children;
        private NodeIndex index;

        /**
         * The node kind.
         */
        public enum Kind {
            /**
             * Root node.
             */
            ROOT,
            /**
             * Presets node.
             */
            PRESETS,
            /**
             * Boolean node.
             */
            BOOLEAN,
            /**
             * Enum node.
             */
            ENUM,
            /**
             * List node.
             */
            LIST,
            /**
             * Text node.
             */
            TEXT,
            /**
             * Value node.
             */
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

        /**
         * Returns the node id.
         *
         * @return The id.
         */
        public int id() {
            return id;
        }

        /**
         * Returns the script path.
         *
         * @return The path.
         */
        public Path script() {
            return script;
        }

        /**
         * Returns the line number in the script at which this node was defined.
         *
         * @return The line number.
         */
        public int line() {
            return line;
        }

        /**
         * Returns the input path.
         *
         * @return The path.
         */
        public String path() {
            return path;
        }

        /**
         * Returns the node kind.
         *
         * @return The kind.
         */
        public Kind kind() {
            return kind;
        }

        /**
         * Returns the node parent.
         *
         * @return The parent. Will be {@code null} if this is the root node.
         */
        public Node parent() {
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

        /**
         * Returns the child nodes.
         *
         * @return The child nodes.
         */
        public List<Node> children() {
            return children;
        }

        /**
         * Returns the node index.
         *
         * @return The index.
         */
        public NodeIndex index() {
            if (index == null) {
                index = createIndex();
            }
            return index;
        }

        protected abstract NodeIndex createIndex();

        /**
         * Collect the values of this node and its children.
         *
         * @param values Where to collect the values.
         */
        public void collect(Map<String, String> values) {
            if (!children.isEmpty()) {
                NodeIndex index = index();
                int current = index.current();
                Node child = children.get(current);
                child.collect(values);
            }
        }

        /**
         * Recurse to find the current non-value leaf node.
         *
         * @return The leaf node.
         */
        Node findLeafNode() {
            if (!children.isEmpty()) {
                int index = index().current();
                Node nextChild = children.get(index);
                return nextChild.findLeafNode();
            }
            if (isValue()) {
                return parent();
            } else {
                return this;
            }
        }

        /**
         * Returns {@code true} if the kind is {@code VALUE}.
         *
         * @return {@code true} if value.
         */
        boolean isValue() {
            return kind == Kind.VALUE;
        }

        List<Node> collectCurrentInputs(List<Node> inputs) {
            inputs.clear();
            Node leaf = findLeafNode();
            collectParentInputs(leaf, inputs);
            collectLeafInputs(leaf, inputs);
            return inputs;
        }

        private void collectParentInputs(Node node, List<Node> inputs) {
            while (true) {
                Node parent = node.parent();
                if (parent == null) {
                    break;
                } else if (!parent.isValue()) {
                    inputs.add(parent);
                }
                node = parent;
            }
            Collections.reverse(inputs);
        }

        private void collectLeafInputs(Node leaf, List<Node> inputs) {
            inputs.add(leaf);
            // Add any siblings
            Node parent = leaf.parent();
            for (Node sibling : parent.children()) {
                if (sibling != leaf && !sibling.isValue()) {
                    inputs.add(sibling);
                }
            }
        }

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

    static class NodeIndex {
        private final int maxIndex;
        private int current;
        private boolean completed;

        NodeIndex(int combinationCount) {
            this.maxIndex = combinationCount - 1;
        }

        /**
         * Returns the number of values this index can be used to retrieve.
         *
         * @return The count.
         */
        int size() {
            return maxIndex + 1;
        }

        void reset() {
            current = 0;
            completed = false;
        }

        boolean next() {
            current++;
            if (current > maxIndex) {
                completed = true;
            }
            return completed;
        }

        int current() {
            if (current > maxIndex) {
                current = 0;
                completed = true;
            }
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
        protected NodeIndex createIndex() {
            return new NodeIndex(children().size());
        }

        @Override
        public String toString() {
            return toString(null);
        }
    }

    public static class Root extends Node {
        Root(int id) {
            super(id, null, null, Kind.ROOT, Path.of("/"), 0);
        }

        @Override
        protected NodeIndex createIndex() {
            return new NodeIndex(1);
        }

        @Override
        public void collect(Map<String, String> values) {
            values.clear();
            children().forEach(child -> child.collect(values));
        }

        @Override
        public String toString() {
            return id() + " ROOT";
        }
    }

    public static class ListNode extends InputNode {
        private final List<String> defaults;
        private final BiFunction<List<String>, List<String>, List<List<String>>> combiner;

        ListNode(int id, Node parent, String path, List<String> defaults, Path script, int line,
                 BiFunction<List<String>, List<String>, List<List<String>>> combiner) {
            super(id, parent, Kind.LIST, path, script, line);
            this.defaults = defaults;
            this.combiner = combiner;
        }

        @Override
        protected NodeIndex createIndex() {
            List<String> values = children().stream().map(c -> ((ValueNode) c).value()).collect(Collectors.toList());
            return new ListIndex(combiner.apply(values, defaults));
        }

        static class ListIndex extends NodeIndex {
            private final List<String> valuesAsString;

            ListIndex(List<List<String>> combinations) {
                super(combinations.size());
                this.valuesAsString = new ArrayList<>(combinations.size());
                combinations.forEach(combination -> valuesAsString.add(asString(combination)));
            }
        }

        static List<List<String>> defaultListCombinations(List<String> listValues, List<String> defaults) {

            // To be complete, we would generate all possible combinations of the list of values (not permutations, since
            // we don't care about order). We *could* do that, but the number grows large when the list is large
            // (see https://www.baeldung.com/java-combinations-algorithm for example implementations). Since the total
            // combinations of the tree is exponential, we need to reduce where we can.

            // So we take a pragmatic approach that should cover most cases we care about:
            //
            // 1. the default item(s), if any
            // 2. no items
            // 3. each individual item
            // 4. all items

            List<List<String>> combinations = new ArrayList<>();
            if (defaults != null && !defaults.isEmpty()) {
                combinations.add(defaults);
            }
            combinations.add(List.of());
            listValues.forEach(v -> combinations.add(List.of(v)));
            combinations.add(listValues);
            return combinations;
        }

        @Override
        public void collect(Map<String, String> values) {
            ListIndex index = (ListIndex) index();
            int current = index.current();
            String combination = index.valuesAsString.get(current);
            if (combination.length() > 0) {
                // TODO unless we can find a way to know that the existing values have NOT changed,
                //      we have to do substitution on each pass. We could implement some pre-parsed
                //      variant to do this faster though.
                String value = evaluate(combination, values);
                values.put(path(), value);
            }
        }

        private static String asString(List<String> values) {
            StringBuilder b = new StringBuilder();
            values.forEach(v -> {
                if (b.length() > 0) {
                    b.append(',');
                }
                b.append(v);
            });
            return b.toString();
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
        protected NodeIndex createIndex() {
            return new NodeIndex(1);
        }

        @Override
        public void collect(Map<String, String> values) {
            // We can ignore the index since we have only one combination
            // TODO unless we can find a way to know that the existing values have NOT changed,
            //      we have to do substitution on each pass. We could implement some pre-parsed
            //      variant to do this faster though.
            values.put(path(), evaluate(value, values));
            children().forEach(child -> child.collect(values));
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
        protected NodeIndex createIndex() {
            return new NodeIndex(1);
        }

        @Override
        public void collect(Map<String, String> values) {
            // We can ignore the index since we have only one combination
            // TODO unless we can find a way to know that the existing values have NOT changed,
            //      we have to do substitution on each pass. We could implement some pre-parsed
            //      variant to do this faster though.
            presets.keySet().forEach(key -> {
                String value = evaluate(presets.get(key), values);
                values.put(key, value);
            });

            children().forEach(child -> child.collect(values));
        }

        void add(String path, String value) {
            presets.put(path, value);
        }

        @Override
        public String toString() {
            return toString(presets().toString());
        }
    }

    /**
     * Builder.
     */
    public static class Builder {
        private static final String MAIN_FILE = "main.xml";
        private final Deque<Node> nodes;
        private final Node root;
        private int nextId;
        private Path archetypePath;
        private String entryPoint;
        private int presetDepth = -1;
        private boolean prune;
        private boolean verbose;
        private boolean movePresetSiblings;
        private BiFunction<List<String>, List<String>, List<List<String>>> listCombiner;

        private Builder() {
            nodes = new ArrayDeque<>();
            root = new Root(nextId++);
            prune = true;
            movePresetSiblings = true;
        }

        /**
         * Sets the list combiner used to produce the set of combinations of the given list and its defaults (if any).
         *
         * @param listCombiner The list combiner.
         * @return This instance, for chaining.
         */
        public Builder listCombiner(BiFunction<List<String>, List<String>, List<List<String>>> listCombiner) {
            this.listCombiner = listCombiner;
            return this;
        }

        /**
         * Set the required archetype path.
         *
         * @param archetypePath The path.
         * @return This instance, for chaining.
         */
        public Builder archetypePath(Path archetypePath) {
            this.archetypePath = archetypePath;
            return this;
        }

        /**
         * Set the entry point file name. Defaults to "main.xml".
         *
         * @param entryPointFileName The file name.
         * @return This instance, for chaining.
         */
        public Builder entryPointFile(String entryPointFileName) {
            this.entryPoint = entryPointFileName;
            return this;
        }

        /**
         * Remove nodes that match preset values. Default is {@code true}.
         * Intended to support testing.
         *
         * @param prunePresets {@code false} if preset nodes should be removed.
         * @return This instance, for chaining.
         */
        public Builder prunePresets(boolean prunePresets) {
            this.prune = prunePresets;
            return this;
        }

        /**
         * Move PRESET siblings to children. Default is {@code true}.
         * Intended to support testing.
         *
         * @param movePresetSiblings {@code false} if siblings should not be moved.
         * @return This instance, for chaining.
         */
        public Builder movePresetSiblings(boolean movePresetSiblings) {
            this.movePresetSiblings = movePresetSiblings;
            return this;
        }

        /**
         * Set to print the tree.
         *
         * @param verbose {@code true} if the tree should be printed.
         * @return This instance, for chaining.
         */
        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        /**
         * Build the tree.
         *
         * @return The tree.
         */
        public InputTree build() {
            if (listCombiner == null) {
                listCombiner = ListNode::defaultListCombinations;
            }
            String scriptName = entryPoint == null ? MAIN_FILE : entryPoint;

            FileSystem fs = fileSystem();
            Path cwd = fs.getPath("/");
            Context context = Context.create(cwd, Map.of(), Map.of());
            Script script = ScriptLoader.load(cwd.resolve(scriptName));

            Walker.walk(new AllInputs(this), script, context);

            if (verbose) {
                root.print();
            }
            if (prune) {
                prune();
            }
            if (movePresetSiblings) {
                movePresetSiblings();
            }
            if (verbose && (prune || movePresetSiblings)) {
                System.out.printf("%nUPDATED%n%n");
                root.print();
            }
            return new InputTree(this);
        }

        /**
         * Remove any nodes for which presets exist.
         */
        void prune() {
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

        void movePresetSiblings() {
            List<Node> presets = collect(root, Kind.PRESETS, new ArrayList<>());
            for (Node preset : presets) {
                Node parent = preset.parent();
                List<Node> siblings = new ArrayList<>(parent.children());
                for (Node sibling : siblings) {
                    if (sibling != preset) {
                        preset.addChild(sibling);
                        parent.removeChild(sibling);
                        sibling.parent(preset);
                    }
                }
            }
        }

        List<Node> collect(Node node, Kind kind, List<Node> nodes) {
            if (node.kind() == kind) {
                nodes.add(node);
            }
            for (Node child : node.children()) {
                collect(child, kind, nodes);
            }
            return nodes;
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
            requireNonNull(archetypePath, "archetypePath is required");
            if (Files.isDirectory(archetypePath)) {
                return VirtualFileSystem.create(archetypePath);
            }
            try {
                return FileSystems.newFileSystem(archetypePath, this.getClass().getClassLoader());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
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
            push(new ListNode(nextId++, parent, path, defaults, script, position.lineNumber(), listCombiner));
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
            presets.add(path, asString(value));
            return VisitResult.CONTINUE;
        }

        private static String asString(Value value) {
            GenericType<?> type = value.type();
            if (type == ValueTypes.STRING) {
                return value.asString();
            } else if (type == ValueTypes.BOOLEAN) {
                return value.asBoolean().toString();
            } else if (type == ValueTypes.INT) {
                return value.asInt().toString();
            } else if (type == ValueTypes.STRING_LIST) {
                List<String> list = value.asList();
                StringBuilder b = new StringBuilder();
                list.forEach(v -> {
                    if (b.length() > 0) {
                        b.append(',');
                    }
                    b.append(v);
                });
                return b.toString();
            }
            throw new IllegalStateException("unknown type: " + type);
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
                    case BOOLEAN:
                        Node parent = builder.current().parent();
                        builder.addValue(parent, "no", input.scriptPath(), input.position());
                        // pop 2
                        builder.pop();
                        builder.pop();
                        break;
                    case TEXT:
                        // pop 2
                        builder.pop();
                        builder.pop();
                        break;
                    case ENUM:
                    case LIST:
                    case OPTION:
                        builder.pop();
                        break;
                    default:
                        throw new IllegalStateException("unknown kind: " + input.kind());
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
