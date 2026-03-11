/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

import java.time.Duration;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.v2.Context.Scope;
import io.helidon.build.archetype.engine.v2.Context.ScopeValue;
import io.helidon.build.archetype.engine.v2.Context.ValueKind;
import io.helidon.build.archetype.engine.v2.InputResolver.InvalidInputException;
import io.helidon.build.archetype.engine.v2.InputResolver.ResolvedKind;
import io.helidon.build.archetype.engine.v2.Node.Kind;
import io.helidon.build.archetype.engine.v2.ScriptInvoker.InvocationException;
import io.helidon.build.common.BitSets;
import io.helidon.build.common.Lists;
import io.helidon.build.common.Maps;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;

import static io.helidon.build.archetype.engine.v2.Nodes.optionIndex;
import static java.util.Objects.requireNonNull;

/**
 * Computed variations represented as an immutable set of entries.
 * <p>
 * A variation is exhaustive when every active user input is enumerated by the computed entries.
 * Some inputs, such as free-form text inputs, cannot be exhaustively enumerated.
 * <p>
 * An input is active for a given entry when it participates in the reachable configuration represented by that
 * entry.
 * <p>
 * Inputs excluded by conditions, selected options, or other branch-specific pruning are not active for that
 * entry and therefore do not affect exhaustiveness.
 * <p>
 * Those inputs are tracked as unbounded on the affected entries and surfaced across the whole set through
 * {@link #unboundedInputs()}.
 * <p>
 * An unbounded entry still carries one representative value for the input, but that value stands in for an
 * open-ended range of possible user values rather than a complete enumeration.
 */
public final class Variations extends AbstractSet<Variations.Entry> {
    private final Set<Entry> entries;
    private final Set<String> unboundedInputs;

    private Variations(Collection<Entry> entries) {
        this.entries = Collections.unmodifiableSet(new TreeSet<>(requireNonNull(entries)));
        Set<String> unboundedInputs = new TreeSet<>();
        for (Entry entry : this.entries) {
            unboundedInputs.addAll(entry.unbounded());
        }
        this.unboundedInputs = Collections.unmodifiableSet(unboundedInputs);
    }

    /**
     * Create an exhaustive variation entry.
     *
     * @param map variation values
     * @return exhaustive variation entry
     */
    public static Entry entry(Map<String, String> map) {
        return entry(map, Set.of());
    }

    /**
     * Create a variation entry.
     *
     * @param map       variation values
     * @param unbounded unbounded input ids
     * @return variation entry
     */
    public static Entry entry(Map<String, String> map, Set<String> unbounded) {
        return new Entry(map, unbounded, null);
    }

    /**
     * Create a singleton variation set.
     *
     * @param map       variation values
     * @param unbounded unbounded input ids
     * @return singleton variation set
     */
    public static Variations of(Map<String, String> map, Set<String> unbounded) {
        return new Variations(Set.of(entry(map, unbounded)));
    }

    /**
     * Merge multiple computed variation sets into one.
     * <p>
     * Entries with the same resolved values are merged so the result preserves a single representative entry
     * and the union of their unbounded inputs.
     *
     * @param variations computed variations to merge
     * @return merged variations
     */
    public static Variations union(Collection<Variations> variations) {
        requireNonNull(variations);
        Map<String, Entry> merged = new HashMap<>();
        for (Variations variation : variations) {
            requireNonNull(variation);
            for (Entry entry : variation) {
                merged.merge(entry.identity(), entry, Variations.Entry::merge);
            }
        }
        return new Variations(merged.values());
    }

    /**
     * Create a variation set from existing entries.
     *
     * @param entries variation entries
     * @return variation set
     */
    public static Variations of(Collection<Entry> entries) {
        return new Variations(entries);
    }

    /**
     * Create a variation set from existing entries.
     *
     * @param entries variation entries
     * @return variation set
     */
    public static Variations of(Entry... entries) {
        return new Variations(Set.of(entries));
    }

    /**
     * Compute variations for a compiler, filters, and external inputs.
     *
     * @param compiler         compiler
     * @param filters          filters
     * @param externalValues   fixed external values
     * @param externalDefaults external defaults
     * @param max              max projected number of variations
     * @return computed variations
     * @throws IllegalStateException if the projected variation count exceeds max
     */
    public static Variations compute(ScriptCompiler compiler,
                                     List<Expression> filters,
                                     Map<String, String> externalValues,
                                     Map<String, String> externalDefaults,
                                     long max) {

        requireNonNull(compiler);
        requireNonNull(filters);
        requireNonNull(externalValues);
        requireNonNull(externalDefaults);
        if (max < 0) {
            throw new IllegalArgumentException("max must be >= 0");
        }

        Node sourceNode = compiler.sourceNode();
        Collection<Entry> variations = new ArrayList<>();
        sourceNode.visit(new VisitorImpl(
                compiler,
                sourceNode,
                variations,
                filters,
                externalValues,
                externalDefaults,
                max));
        return new Variations(variations);
    }

    @Override
    public Iterator<Entry> iterator() {
        return entries.iterator();
    }

    @Override
    public int size() {
        return entries.size();
    }

    /**
     * Get the input ids that remain unbounded across the computed variations.
     *
     * @return unbounded input ids
     */
    public Set<String> unboundedInputs() {
        return unboundedInputs;
    }

    /**
     * Returns whether the computed variations are exhaustive.
     *
     * @return {@code true} if no input remains unbounded
     */
    public boolean exhaustive() {
        return unboundedInputs.isEmpty();
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * Render the computed variations with a custom separator between entries.
     *
     * @param separator separator between rendered entries
     * @return rendered variations
     */
    public String toString(String separator) {
        return toString(true, separator);
    }

    /**
     * Render the computed variations.
     *
     * @param markUnbounded whether non-exhaustive entries should include their unbounded input ids
     * @return rendered variations
     */
    public String toString(boolean markUnbounded) {
        return toString(markUnbounded, System.lineSeparator());
    }

    /**
     * Render the computed variations with a custom separator between entries.
     *
     * @param markUnbounded whether non-exhaustive entries should include their unbounded input ids
     * @param separator     separator between rendered entries
     * @return rendered variations
     */
    public String toString(boolean markUnbounded, String separator) {
        requireNonNull(separator);
        return entries.stream()
                .map(entry -> entry.toString(markUnbounded))
                .collect(Collectors.joining(separator));
    }

    /**
     * Computed variation.
     */
    public static final class Entry extends AbstractMap<String, String> implements Comparable<Variations.Entry> {
        private final Map<String, String> map;
        private final String signature;
        private final Set<String> unbounded;

        private Entry(Map<String, String> values, Set<String> unbounded, String signature) {
            requireNonNull(values);
            requireNonNull(unbounded);
            this.map = Collections.unmodifiableMap(new LinkedHashMap<>(values));
            this.signature = signature;
            this.unbounded = Collections.unmodifiableSet(new TreeSet<>(unbounded));
        }

        @Override
        public Set<Map.Entry<String, String>> entrySet() {
            return map.entrySet();
        }

        /**
         * Get the active input ids that remain unbounded.
         *
         * @return unbounded input ids
         */
        public Set<String> unbounded() {
            return unbounded;
        }

        /**
         * Returns whether this variation is exhaustive.
         *
         * @return {@code true} if the variation has no unbounded inputs
         */
        public boolean exhaustive() {
            return unbounded.isEmpty();
        }

        @Override
        public int compareTo(Variations.Entry o) {
            int result = Maps.compare(this, o);
            if (result != 0) {
                return result;
            }
            return Lists.compare(unbounded, o.unbounded);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Variations.Entry)) {
                return false;
            }
            Variations.Entry other = (Variations.Entry) o;
            return Objects.equals(map, other.map)
                   && Objects.equals(unbounded, other.unbounded);
        }

        @Override
        public int hashCode() {
            return Objects.hash(map, unbounded);
        }

        String signature() {
            if (signature != null) {
                return signature;
            }
            return Lists.join(new TreeMap<>(map).entrySet(), " ");
        }

        String identity() {
            return Lists.join(new TreeMap<>(map).entrySet(), " ");
        }

        @Override
        public String toString() {
            return toString(true);
        }

        /**
         * Render the variation entry with a custom separator between values.
         *
         * @param separator separator between rendered values
         * @return rendered variation entry
         */
        public String toString(String separator) {
            return toString(true, separator);
        }

        /**
         * Render the variation entry.
         *
         * @param markUnbounded whether to include unbounded input ids for non-exhaustive entries
         * @return rendered variation entry
         */
        public String toString(boolean markUnbounded) {
            return toString(markUnbounded, ", ");
        }

        /**
         * Render the variation entry with a custom separator between values.
         *
         * @param markUnbounded whether to include unbounded input ids for non-exhaustive entries
         * @param separator     separator between rendered values
         * @return rendered variation entry
         */
        public String toString(boolean markUnbounded, String separator) {
            requireNonNull(separator);
            String values = entrySet().stream()
                    .map(Map.Entry::toString)
                    .collect(Collectors.joining(separator));
            if (!markUnbounded || exhaustive()) {
                return values;
            }
            return values + " unbounded=" + unbounded;
        }

        private Variations.Entry merge(Variations.Entry other) {
            requireNonNull(other);
            Variations.Entry selected = representativeCompare(other) >= 0 ? this : other;
            Set<String> merged = new TreeSet<>(unbounded);
            merged.addAll(other.unbounded);
            if (selected.unbounded.equals(merged)) {
                return selected;
            }
            return new Variations.Entry(selected, merged, selected.signature);
        }

        private int representativeCompare(Variations.Entry other) {
            int result = Integer.compare(size(), other.size());
            return result != 0 ? result : Maps.compare(map, other.map);
        }
    }

    private static final class VisitorImpl implements Node.Visitor {
        private final ScriptCompiler compiler;
        private final Node sourceNode;

        private final List<Table> inputs = new ArrayList<>();
        private final List<Column> columns = new ArrayList<>();
        private final Map<Column, Integer> indexes = new LinkedHashMap<>();
        private final Map<BitSet, Map<String, String>> variationCache = new ConcurrentHashMap<>();
        private final Set<String> textInputs = new LinkedHashSet<>();
        private final Collection<Entry> variations;
        private final List<Expression> filters;
        private final Map<String, String> externalValues;
        private final Map<String, String> externalDefaults;
        private final Map<String, Value.Type> inputTypes;
        private final Map<String, String> resolvedExternalValues;
        private final Map<String, String> resolvedExternalDefaults;
        private final long max;

        VisitorImpl(ScriptCompiler compiler,
                    Node sourceNode,
                    Collection<Entry> variations,
                    List<Expression> filters,
                    Map<String, String> externalValues,
                    Map<String, String> externalDefaults,
                    long max) {
            this.compiler = compiler;
            this.sourceNode = sourceNode;
            this.variations = variations;
            this.filters = filters;
            this.externalValues = Collections.unmodifiableMap(new LinkedHashMap<>(externalValues));
            this.externalDefaults = Collections.unmodifiableMap(new LinkedHashMap<>(externalDefaults));
            this.inputTypes = inputTypes();
            this.resolvedExternalValues = resolvedExternalValues();
            this.resolvedExternalDefaults = resolvedExternalDefaults();
            this.max = max;
        }

        @Override
        public boolean visit(Node node) {
            if (!active(node)) {
                return false;
            }
            Table table;
            List<Node> options;
            List<Node> optionNodes;
            switch (node.kind()) {
                case INPUT_TEXT:
                    table = table(node);
                    String textValue = declaredValue(node, table.id).asString()
                            .or(() -> externalDefaultValue(table.id))
                            .or(() -> node.attribute("default").asString())
                            .orElse("<?>");
                    table.columns.add(new Column(table.id, textValue));
                    table.addRow(BitSets.of(0), Expression.TRUE);
                    inputs.add(table);
                    textInputs.add(table.id);
                    break;
                case INPUT_BOOLEAN:
                    table = table(node);
                    table.columns.add(new Column(table.id, "true"));
                    table.columns.add(new Column(table.id, "false"));
                    Value<Boolean> boolValue = declaredValue(node, table.id).asBoolean();
                    if (boolValue.isPresent()) {
                        table.addRow(BitSets.of(boolValue.get() ? 0 : 1), Expression.TRUE);
                    } else {
                        table.addRow(BitSets.of(0), Expression.TRUE);
                        table.addRow(BitSets.of(1), Expression.TRUE);
                    }
                    inputs.add(table);
                    break;
                case INPUT_ENUM:
                    table = table(node);
                    optionNodes = optionNodes(node);
                    options = Lists.map(optionNodes, Node::unwrap);
                    for (Node o : options) {
                        table.columns.add(new Column(table.id, o.value().getString()));
                    }
                    int index = declaredValue(node, table.id).asString()
                            .map(o -> requiredOptionIndex(table.id, o, options))
                            .orElse(-1);
                    if (index >= 0) {
                        // only add the preset option
                        Node n = optionNodes.get(index);
                        table.addRow(BitSets.of(index), prune(node, n.expression()));
                    } else {
                        for (int i = 0; i < table.columns.size(); i++) {
                            Node n = optionNodes.get(i);
                            table.addRow(BitSets.of(i), prune(node, n.expression()));
                        }
                    }
                    inputs.add(table);
                    break;
                case INPUT_LIST:
                    table = table(node);
                    optionNodes = optionNodes(node);
                    options = Lists.map(optionNodes, Node::unwrap);
                    for (Node o : options) {
                        table.columns.add(new Column(table.id, o.value().getString()));
                    }
                    Value<List<String>> value = declaredValue(node, table.id).asList();
                    if (value.isPresent()) {
                        // only add the preset options
                        BitSet bits = new BitSet();
                        Expression expr = Expression.TRUE;
                        for (String o : value.getList()) {
                            int i = requiredOptionIndex(table.id, o, options);
                            bits.set(i);
                            Node n = optionNodes.get(i);
                            expr = expr.and(prune(node, n.expression()));
                        }
                        table.addRow(bits, expr);
                    } else {
                        for (int p = 1, permSize = 1 << table.columns.size(); p < permSize; p++) {
                            Expression expr = Expression.TRUE;
                            BitSet bits = BitSets.of((long) p);
                            for (int i = bits.nextSetBit(0); i >= 0 && i < Integer.MAX_VALUE; i = bits.nextSetBit(i + 1)) {
                                Node n = optionNodes.get(i);
                                expr = expr.and(prune(node, n.expression()));
                            }
                            table.addRow(bits, expr);
                        }
                    }
                    table.columns.add(new Column(table.id, "none"));
                    table.addRow(BitSets.of(table.columns.size() - 1), Expression.TRUE);
                    inputs.add(table);
                    break;
                default:
            }
            return true;
        }

        @Override
        public void postVisit(Node node) {
            if (node.kind() == Kind.SCRIPT) {

                // compute the variations
                long computeStartTime = System.currentTimeMillis();

                // aggregate all columns
                List<Table> orderedInputs = new ArrayList<>(inputs);
                orderedInputs.sort(Comparator
                        .comparingInt((Table table) -> inputOrder(table.id))
                        .thenComparingInt(table -> table.node.id()));
                for (Table table : orderedInputs) {
                    for (Column column : table.columns) {
                        int index = indexes.computeIfAbsent(column, e -> indexes.size());
                        if (index == columns.size()) {
                            columns.add(column);
                        }
                    }
                }

                // remap against the aggregated columns
                List<Table> tables = new ArrayList<>();
                for (Table input : inputs) {
                    Table table = table(input.node);
                    table.columns.addAll(input.columns);
                    for (Row row : input.rows) {
                        BitSet bitSet = new BitSet();
                        for (int i = row.bits.nextSetBit(0); i >= 0 && i < Integer.MAX_VALUE; i = row.bits.nextSetBit(i + 1)) {
                            bitSet.set(indexes.get(input.columns.get(i)));
                        }
                        table.addRow(bitSet, row.expr);
                    }
                    tables.add(table);
                }

                // collect the input ids that each remapped table depends on
                Set<String> inputIds = tables.stream().map(t -> t.id)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                for (Table table : tables) {
                    table.dependencies.addAll(dependencies(table, inputIds));
                }

                // tables that still need to be joined
                List<Table> pending = new ArrayList<>(tables);

                // number of joined table fragments per input id
                Map<String, Integer> joined = new HashMap<>();

                // input ids whose full set of fragments has already been joined
                Set<String> available = new LinkedHashSet<>();

                // total table fragments per input id
                Map<String, Integer> totals = new HashMap<>();
                for (Table table : tables) {
                    totals.compute(table.id, (k, v) -> v == null ? 1 : v + 1);
                }

                // current intermediate rows that have not yet been expanded by a later join
                Set<BitSet> merged = new LinkedHashSet<>();
                for (int i = 0; i < tables.size(); i++) {
                    // pick the next table whose guard allows expansion
                    Join join = nextJoin(pending, available, merged);
                    if (join.cost > max) {
                        throw new IllegalStateException(String.format(
                                "Projected variation count %d exceeds the configured limit of %d",
                                join.cost,
                                max));
                    }

                    // remove the rows this join will expand
                    join.filtered.forEach(merged::remove);

                    // selected table is no longer pending
                    pending.remove(join.table);

                    // some logical inputs produce multiple table fragments with the same id
                    // mark that id available only after all of them join
                    int count = joined.compute(join.table.id, (k, v) -> v == null ? 1 : v + 1);
                    if (count == totals.getOrDefault(join.table.id, -1)) {
                        available.add(join.table.id);
                    }

                    Log.debug("Progress: %d/%d - %s - filtered: %d, merged: %d",
                            i + 1,
                            tables.size(),
                            join.table,
                            join.filtered.size(),
                            merged.size());

                    // compute variations for the input
                    List<Row> computed = new ArrayList<>();
                    if (join.filtered.isEmpty()) {
                        if (merged.isEmpty()) {
                            // use this table as the initial intermediate result
                            computed.addAll(join.table.rows);
                        }
                    } else {
                        // combine each eligible intermediate row with each row from this table
                        for (Row row1 : join.table.rows) {
                            for (BitSet row2 : join.filtered) {
                                computed.add(new Row(BitSets.or(BitSets.copyOf(row1.bits), row2), row1.expr));
                            }
                        }
                    }

                    // apply excludes
                    for (Row row : computed) {
                        if (join.table.expr == Expression.TRUE && row.expr == Expression.TRUE && filters.isEmpty()) {
                            merged.add(row.bits);
                            continue;
                        }
                        Map<String, String> vars = variation(row.bits);
                        if (eval(join.table.node, join.table.expr, vars)
                            && eval(join.table.node, row.expr, vars)
                            && filter(node, vars)) {
                            merged.add(row.bits);
                        }
                    }
                }
                logDuration(computeStartTime, "Computed " + merged.size() + " variations");

                // normalize variations
                // perform an execution and use the context values
                long normalizeStartTime = System.currentTimeMillis();
                Collection<Entry> normalized = merged.parallelStream()
                        .map(this::normalize)
                        .filter(Objects::nonNull)
                        .collect(new NormalizedCollector());
                logDuration(normalizeStartTime, "Normalized " + merged.size() + " variations");

                long sortStartTime = System.currentTimeMillis();
                variations.addAll(normalized);
                logDuration(sortStartTime, "Sorted " + variations.size() + " variations");
            }
        }

        Map<String, String> variation(BitSet row) {
            return variationCache.computeIfAbsent(row, this::variation0);
        }

        Map<String, String> variation0(BitSet row) {
            Map<String, String> variation = new LinkedHashMap<>();
            for (int i = row.nextSetBit(0); i >= 0 && i < Integer.MAX_VALUE; i = row.nextSetBit(i + 1)) {
                Column column = columns.get(i);
                variation.compute(column.name, (k, v) -> v == null ? column.value : v + "," + column.value);
            }
            variation.putAll(resolvedExternalValues);
            return Collections.unmodifiableMap(variation);
        }

        Entry normalize(BitSet row) {
            Map<String, String> variation = variation(row);
            Map<String, ScopeValue<?>> effective = execute(variation);
            if (effective.isEmpty()) {
                return null;
            }

            Map<String, String> values = Maps.mapValue(effective, v -> Value.toString(v));
            Set<String> unbounded = effective.entrySet().stream()
                    .filter(e -> textInputs.contains(e.getKey()) && e.getValue().kind() != ValueKind.EXTERNAL)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(TreeSet::new));

            // compute signature, sorted user values only
            Map<String, String> normalized = Maps.mapValue(effective,
                    (k, v) -> v.kind() == ValueKind.USER, v -> Value.toString(v), TreeMap::new);

            String signature = Lists.join(normalized.entrySet(), " ");
            return new Entry(values, unbounded, signature);
        }

        boolean filter(Node node, Map<String, String> variation) {
            if (filters.isEmpty()) {
                return true;
            }
            for (Expression exclude : filters) {
                if (eval(node, exclude, variation)) {
                    if (LogLevel.isDebug()) {
                        Log.debug("Excluding variation, rule: %s, entries: %s", exclude.literal(), variation);
                    }
                    return false;
                }
            }
            return true;
        }

        boolean eval(Node node, Expression expr, Map<String, String> variation) {
            if (expr == Expression.TRUE) {
                return true;
            }
            if (expr == Expression.FALSE) {
                return false;
            }
            try {
                Scope scope = compiler.scope(node);
                return expr.eval(s -> {
                    String v = variation.get(scope.key(s));
                    if (v != null) {
                        return Value.dynamic(v);
                    }
                    return null;
                });
            } catch (Expression.UnresolvedVariableException ignored) {
                return false;
            }
        }

        boolean active(Node node) {
            return prune(node, activation(node)) != Expression.FALSE;
        }

        Expression activation(Node node) {
            switch (node.kind()) {
                case INPUT_TEXT:
                case INPUT_BOOLEAN:
                case INPUT_ENUM:
                case INPUT_LIST:
                    return node.parent() == null ? Expression.TRUE : compiler.expression(node.parent());
                default:
                    return compiler.expression(node);
            }
        }

        Expression prune(Node node, Expression expr) {
            if (expr == Expression.TRUE || expr == Expression.FALSE || resolvedExternalValues.isEmpty()) {
                return expr;
            }
            Scope scope = compiler.scope(node);
            return expr.inline(s -> {
                String key = scope.key(s);
                String value = resolvedExternalValues.get(key);
                if (value == null) {
                    return null;
                }
                return typedValue(key, value);
            });
        }

        Map<String, ScopeValue<?>> execute(Map<String, String> variation) {
            try {
                // initialize a context with the variations
                Context context = new Context()
                        .externalValues(externalValues)
                        .externalDefaults(externalDefaults)
                        .pushCwd(compiler.cwd());
                variation.forEach((k, v) -> {
                    if (externalValues.containsKey(k)) {
                        return;
                    }
                    Scope scope = context.scope().getOrCreate(k);
                    scope.value(Value.dynamic(v), ValueKind.USER);
                });

                // record the scopes in traversal order
                Set<Scope> scopes = new LinkedHashSet<>();
                ScriptInvoker.invoke(sourceNode, context, new InputResolver.BatchResolver(context), n -> {
                    scopes.add(context.scope());
                    return true;
                });

                // return values in traversal order
                Map<String, ScopeValue<?>> values = new LinkedHashMap<>();
                for (Scope scope : scopes) {
                    if (scope.parent() != null) {
                        scope.values().forEach((k, v) -> {
                            if (v.isPresent()) {
                                switch (v.kind()) {
                                    case USER:
                                    case EXTERNAL:
                                        if (!scopes.contains(v.scope())) {
                                            // not visited, discard
                                            return;
                                        }
                                        break;
                                    case DEFAULT:
                                        for (Object o : v.qualifiers()) {
                                            if (o == ResolvedKind.AUTO_CREATED) {
                                                return;
                                            }
                                        }
                                        break;
                                    default:
                                        return;
                                }
                                values.putIfAbsent(k, v);
                            }
                        });
                    }
                }
                return values;
            } catch (InvocationException ex) {
                if (!(ex.getCause() instanceof InvalidInputException)) {
                    Log.debug("Execution error: %s, inputs: %s",
                            ex.getCause().getMessage(),
                            variation);
                }
                return Map.of();
            }
        }

        Value<?> declaredValue(Node node, String key) {
            String value = resolvedExternalValues.get(key);
            if (value != null) {
                return Value.typed(Value.dynamic(value), node.kind().valueType());
            }
            return compiler.declaredValue(node, key);
        }

        Value<String> externalDefaultValue(String key) {
            String value = resolvedExternalDefaults.get(key);
            if (value == null) {
                value = externalDefaults.get(key);
            }
            return Value.of(value);
        }

        Map<String, String> resolvedExternalValues() {
            if (externalValues.isEmpty()) {
                return Map.of();
            }
            Context context = new Context()
                    .externalValues(externalValues)
                    .externalDefaults(externalDefaults)
                    .pushCwd(compiler.cwd());
            Map<String, String> values = new LinkedHashMap<>();
            for (String key : externalValues.keySet()) {
                ScopeValue<?> value = context.scope().getOrCreate(key).value();
                if (value.isPresent()) {
                    values.put(key, Value.toString(value));
                }
            }
            return Collections.unmodifiableMap(values);
        }

        Map<String, Value.Type> inputTypes() {
            Map<String, Value.Type> types = new LinkedHashMap<>();
            for (Node input : sourceNode.traverse(Kind::isInput)) {
                types.putIfAbsent(compiler.scopeId(input), input.kind().valueType());
            }
            return Collections.unmodifiableMap(types);
        }

        Value<?> typedValue(String key, String value) {
            Value.Type type = inputTypes.get(key);
            if (type == null) {
                return Value.of(value);
            }
            switch (type) {
                case BOOLEAN:
                    return Value.parseBoolean(value);
                case INTEGER:
                    return Value.parseInt(value);
                case LIST:
                    return Value.parseList(value);
                default:
                    return Value.of(value);
            }
        }

        int inputOrder(String key) {
            int index = 0;
            for (String id : inputTypes.keySet()) {
                if (id.equals(key)) {
                    return index;
                }
                index++;
            }
            return Integer.MAX_VALUE;
        }

        Map<String, String> resolvedExternalDefaults() {
            if (externalDefaults.isEmpty()) {
                return Map.of();
            }
            Context context = new Context()
                    .externalValues(externalValues)
                    .externalDefaults(externalDefaults)
                    .pushCwd(compiler.cwd());
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : externalDefaults.entrySet()) {
                try {
                    String value = Value.toString(context.defaultValue(entry.getKey()));
                    if (value != null) {
                        values.put(entry.getKey(), value);
                    }
                } catch (RuntimeException ignored) {
                    values.put(entry.getKey(), entry.getValue());
                }
            }
            return Collections.unmodifiableMap(values);
        }

        // choose the next table to join from the currently joinable candidates
        Join nextJoin(List<Table> tables, Set<String> available, Set<BitSet> merged) {
            if (tables.isEmpty()) {
                throw new IllegalStateException("No tables available for join");
            }
            Table best = null;
            Table fallback = null;
            int mergedSize = merged.size();
            long bestCost = Long.MAX_VALUE;
            for (Table table : tables) {
                if (fallback == null) {
                    fallback = table;
                }

                // prefer tables whose referenced inputs are already fully materialized
                if (!available.containsAll(table.dependencies)) {
                    continue;
                }

                // pick the join expected to produce the smallest next intermediate set
                long cost = estimateJoinSize(table, mergedSize);
                if (best == null || cost < bestCost || (cost == bestCost && table.rows.size() < best.rows.size())) {
                    best = table;
                    bestCost = cost;
                }
            }
            return join(best != null ? best : fallback, merged);
        }

        // cheap heuristic to estimate the next intermediate result size
        long estimateJoinSize(Table table, int mergedSize) {
            if (table.expr == Expression.FALSE) {
                // never matches, current rows are unchanged.
                return mergedSize;
            }
            if (mergedSize == 0) {
                // initial intermediate set
                return table.rows.size();
            }
            if (table.expr == Expression.TRUE) {
                // always matches
                // every merged row combines with every row in the table
                return (long) mergedSize * table.rows.size();
            }
            if (table.rows.size() <= 1) {
                // a single-row table keeps the merged row count unchanged
                return mergedSize;
            }

            // conditional multi-row joins
            // assume half the current rows survive the guard
            long estimatedFiltered = mergedSize >>> 1;
            return mergedSize - estimatedFiltered + estimatedFiltered * table.rows.size();
        }

        // estimate how many existing rows survive the table guard and would need expanding
        Join join(Table table, Set<BitSet> merged) {
            if (table.expr == Expression.FALSE) {
                // never matches, current rows are unchanged.
                return new Join(table, List.of(), merged.size());
            }
            if (merged.isEmpty()) {
                // initial intermediate set
                return new Join(table, List.of(), table.rows.size());
            }
            if (table.expr == Expression.TRUE) {
                // always matches
                // every merged row combines with every row in the table
                long cost = (long) merged.size() * table.rows.size();
                return new Join(table, new ArrayList<>(merged), cost);
            }

            // collect the rows that should be expanded.
            List<BitSet> filtered = new ArrayList<>();
            for (BitSet row : merged) {
                Map<String, String> variation = variation(row);
                if (eval(table.node, table.expr, variation)) {
                    filtered.add(row);
                }
            }

            // cost = unchanged rows (merged - filtered) + expanded rows (filtered * table rows)
            long cost = merged.size() - filtered.size() + (long) filtered.size() * table.rows.size();
            return new Join(table, filtered, cost);
        }

        // collect every input id that must be available before this table can join
        Set<String> dependencies(Table table, Set<String> inputIds) {
            Scope scope = compiler.scope(table.node);
            // join order only depends on input ids referenced by table and row predicates
            Set<String> dependencies = new LinkedHashSet<>(dependencies(table.expr, scope, table.id, inputIds));
            for (Row row : table.rows) {
                dependencies.addAll(dependencies(row.expr, scope, table.id, inputIds));
            }
            return dependencies;
        }

        // resolve expression variables to input ids and keep only real inter-table dependencies
        Set<String> dependencies(Expression expr, Scope scope, String self, Set<String> inputIds) {
            if (expr == Expression.TRUE || expr == Expression.FALSE) {
                return Set.of();
            }
            Set<String> dependencies = new LinkedHashSet<>();
            for (String variable : expr.variables()) {
                String key = scope.key(variable);
                // ignore self-references because they do not constrain when this table can join
                if (!key.equals(self) && inputIds.contains(key)) {
                    dependencies.add(key);
                }
            }
            return dependencies;
        }

        Table table(Node node) {
            Expression expr = node.parent() == null ? Expression.TRUE : compiler.expression(node.parent());
            return new Table(node, compiler.scopeId(node), prune(node, expr));
        }

        static List<Node> optionNodes(Node node) {
            return node.children().stream()
                    .filter(n -> n.unwrap().kind() == Kind.INPUT_OPTION)
                    .collect(Collectors.toList());
        }

        static int requiredOptionIndex(String inputId, String option, List<Node> options) {
            int index = optionIndex(option, options);
            if (index >= 0) {
                return index;
            }
            String values = options.stream()
                    .map(Node::value)
                    .map(v -> Value.toString(v))
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException(String.format(
                    "Invalid value '%s' for input '%s', available options: %s",
                    option,
                    inputId,
                    values));
        }

        static void logDuration(long startTime, String msg) {
            long endTime = System.currentTimeMillis();
            Duration duration = Duration.ofMillis(endTime - startTime);
            Log.debug("%s in %d.%ds", msg, duration.toSeconds(), duration.toMillisPart());
        }
    }

    /**
     * One named value selected from a table row for a variation.
     */
    private static final class Column {
        private final String name;
        private final String value;

        Column(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Column)) {
                return false;
            }
            Column column = (Column) o;
            return Objects.equals(name, column.name)
                   && Objects.equals(value, column.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }

        @Override
        public String toString() {
            return name + "=" + value;
        }
    }

    /**
     * One candidate selection for an input, with the predicate that keeps it active.
     */
    private static final class Row {
        private final BitSet bits;
        private final Expression expr;

        Row(BitSet bits, Expression expr) {
            this.bits = bits;
            this.expr = expr.reduce();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Row)) {
                return false;
            }
            Row row = (Row) o;
            return Objects.equals(bits, row.bits)
                   && Objects.equals(expr, row.expr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bits, expr);
        }
    }

    /**
     * Joinable rows for a single input, including its columns, guard, and input dependencies.
     */
    private static class Table {
        private final List<Column> columns = new ArrayList<>();
        private final Set<Row> rows = new LinkedHashSet<>();
        private final Set<String> dependencies = new LinkedHashSet<>();
        private final String id;
        private final Node node;
        private final Expression expr;

        Table(Node node, String id, Expression expr) {
            this.id = id;
            this.node = node;
            this.expr = expr;
        }

        void addRow(BitSet bits, Expression expr) {
            expr = expr.reduce();
            if (expr != Expression.FALSE) {
                rows.add(new Row(bits, expr));
            }
        }

        @Override
        public String toString() {
            return id;
        }
    }

    /**
     * Candidate join state.
     */
    private static class Join {
        private final Table table;
        private final List<BitSet> filtered;
        private final long cost;

        Join(Table table, List<BitSet> filtered, long cost) {
            this.table = table;
            this.filtered = filtered;
            this.cost = cost;
        }
    }

    /**
     * Collector that merges normalized entries by signature.
     */
    private static class NormalizedCollector implements Collector<Entry, Map<String, Entry>, Collection<Entry>> {

        @Override
        public Supplier<Map<String, Entry>> supplier() {
            return HashMap::new;
        }

        @Override
        public BiConsumer<Map<String, Entry>, Entry> accumulator() {
            return (map, entry) -> map.merge(entry.signature(), entry, Variations.Entry::merge);
        }

        @Override
        public BinaryOperator<Map<String, Entry>> combiner() {
            return (left, right) -> {
                right.forEach((key, entry) -> left.merge(key, entry, Variations.Entry::merge));
                return left;
            };
        }

        @Override
        public Function<Map<String, Entry>, Collection<Entry>> finisher() {
            return Map::values;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }
}
