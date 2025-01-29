/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.helidon.build.common.LazyValue;
import io.helidon.build.common.Lists;

import static io.helidon.build.common.PropertyEvaluator.evaluate;
import static java.lang.Character.isLetterOrDigit;
import static java.util.Objects.requireNonNull;

/**
 * Archetype context.
 * Holds the state for an archetype invocation.
 *
 * <ul>
 *     <li>Maintains the current working directory for resolving files and scripts.</li>
 *     <li>Maintains the scope for storing and resolving values and variables</li>
 * </ul>
 */
public final class Context {

    /**
     * Context value kind.
     */
    public enum ValueKind {
        /**
         * Preset value.
         */
        PRESET,

        /**
         * Local variable value.
         */
        LOCAL_VAR,

        /**
         * Default value.
         */
        DEFAULT,

        /**
         * External value.
         */
        EXTERNAL,

        /**
         * User value.
         */
        USER
    }

    /**
     * Scope visibility.
     */
    public enum Visibility {

        /**
         * Global visibility.
         * <br/>
         * <br/>
         * A scope can be global only if its parent is also global.
         * Global parent scopes are optional segments.
         * <br/>
         * <br/>
         * E.g., Consider {@code a}, {@code b} as global and {@code c} as local
         * <ul>
         *     <li>{@code "a.b.c", "b.c", "c"} are equivalent</li>
         *     <li>{@code "a.c", "a.b.c"} are equivalent only if {@code "a.b.c"} is created first</li>
         * </ul>
         *
         * @see Key
         */
        GLOBAL,

        /**
         * Local visibility.
         * <br/>
         * <br/>
         * Local parent scopes are mandatory segments.
         * <br/>
         * <br/>
         * E.g.
         * <ul>
         *     <li>{@code "a.b" == "b.c" == "c"} if {@code a,b} are global and {@code c} is local</li>
         *     <li>{@code "a.b.c" != "b.c" != "c"} if all nodes are local</li>
         * </ul>
         *
         * @see Key
         */
        LOCAL,

        /**
         * Unset visibility.
         * Behaves like {@link #LOCAL}, but can be changed later on to either {@link #GLOBAL} or {@link #LOCAL}.
         */
        UNSET
    }

    private final Map<String, Value<?>> defaults = new HashMap<>();
    private final Deque<Path> directories = new ArrayDeque<>();
    private Scope scope = new Scope();

    Context() {
    }

    /**
     * Add external defaults.
     *
     * @param externalDefaults external defaults
     * @return this instance
     */
    public Context externalDefaults(Map<String, String> externalDefaults) {
        externalDefaults.forEach((k, v) -> defaults.put(k, Value.dynamic(() -> scope.interpolate(v))));
        return this;
    }

    /**
     * Add external values.
     *
     * @param externalValues external values
     * @return this instance
     */
    public Context externalValues(Map<String, String> externalValues) {
        externalValues.forEach((k, v) -> scope.getOrCreate(k)
                .value(Value.dynamic(() -> evaluate(v, s -> externalValues.getOrDefault(s, defaultValue(s).orElse(null)))),
                        ValueKind.EXTERNAL));
        return this;
    }

    /**
     * Push a new working directory.
     *
     * @param workDir directory
     * @return this instance
     */
    public Context pushCwd(Path workDir) {
        if (workDir == null) {
            throw new IllegalArgumentException("workDir is null");
        }
        directories.push(workDir.toAbsolutePath());
        return this;
    }

    /**
     * Pop the current working directory.
     *
     * @throws IllegalStateException if the current cwd is the initial one
     */
    public void popCwd() {
        if (directories.size() == 1) {
            throw new IllegalStateException("Cannot pop the initial working directory");
        }
        directories.pop();
    }

    /**
     * Get the current working directory.
     *
     * @return Path
     */
    public Path cwd() {
        return directories.peek();
    }

    /**
     * Push a scope.
     *
     * @param op op
     * @return the new current scope
     */
    public Scope pushScope(UnaryOperator<Scope> op) {
        return pushScope(op.apply(scope));
    }

    /**
     * Push a scope.
     *
     * @param scope scope
     * @return the new current scope
     */
    public Scope pushScope(Scope scope) {
        if (this.scope != scope.parent) {
            throw new IllegalArgumentException(String.format(
                    "Invalid scope: current=%s, given=%s", this.scope.internalKey(), scope.internalKey()));
        }
        this.scope = scope;
        return scope;
    }

    /**
     * Pop the current scope.
     *
     * @throws NoSuchElementException if the current scope is the root scope
     */
    public void popScope() {
        if (scope.parent == null) {
            throw new NoSuchElementException();
        }
        Scope previous = scope;
        scope = scope.parent;
        previous.visit(it -> {
            // clear children, and re-create from the root
            it.children.clear();
            Scope sc = scope.root.getOrCreate(it.key(), it.model);
            sc.visibility = it.visibility;
            sc.value = it.value;
        });
    }

    /**
     * Get the current scope.
     *
     * @return Scope
     */
    public Scope scope() {
        return scope;
    }

    /**
     * Get an external default value.
     *
     * @param key key
     * @return value
     */
    public Value<String> defaultValue(String key) {
        return defaults.getOrDefault(key, Value.empty()).asString();
    }

    /**
     * Convert the context to a map.
     *
     * @return map
     */
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        scope.visit(node -> {
            ScopeValue<?> value = node.value();
            if (node.parent() != null
                && value.isPresent()
                && !node.visibility().equals(Visibility.UNSET)
                && (value.isSerializable())) {
                map.put(node.key(), Value.toString(value));
            }
        });
        return map;
    }

    /**
     * A notation to represent the path of a value in the context tree.
     *
     * <ul>
     *     <li>A key contains segments separated by {@code "."} characters</li>
     *     <li>Segments can contain only letters, digits and separator {@code "-"}</li>
     *     <li>The segment separator {@code "-"} must be used between valid characters; ({@code "--"} is prohibited</li>
     *     <li>Two reference operators are available, root scope: {@code "~"} ; parent scope: {@code ".."}</li>
     *     <li>A key that starts with {@code "~"} is absolute. I.e., relative to the root scope</li>
     *     <li>A key that starts with a segment is relative, or a parent reference is relative</li>
     * </ul>
     *
     * @see Scope
     */
    public static final class Key {

        private static final String PARENT_REF = "..";
        private static final String ROOT_REF = "~";
        private static final char ROOT_REF_CHAR = '~';
        private static final char SEPARATOR_CHAR = '.';
        private static final String SEPARATOR = ".";

        private static final char SEGMENT_SEPARATOR = '-';

        private Key() {
        }

        /**
         * Get the last segment of the key.
         *
         * @param segments segments
         * @return id
         * @throws IllegalArgumentException if the key is empty, or if the last segment contains any {@code "."}
         */
        public static String id(String[] segments) {
            if (segments.length == 0) {
                throw new IllegalArgumentException("Empty segments");
            }
            String id = segments[segments.length - 1];
            if (id.indexOf(SEPARATOR_CHAR) > 0) {
                throw new IllegalArgumentException("Invalid scope id: " + id);
            }
            return id;
        }

        /**
         * Normalize the given key.
         *
         * @param key key
         * @return String
         */
        public static String normalize(String key) {
            return toString(parse(key));
        }

        /**
         * Get this key as a string.
         *
         * @param segments segments
         * @return String
         */
        public static String toString(String[] segments) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < segments.length; i++) {
                if (i == 0 && segments[i].equals(ROOT_REF)) {
                    continue;
                }
                sb.append(segments[i]);
                if (i + 1 < segments.length
                    && segments[i].indexOf(SEPARATOR_CHAR) < 0
                    && segments[i + 1].indexOf(SEPARATOR_CHAR) < 0) {
                    sb.append(SEPARATOR_CHAR);
                }
            }
            return sb.toString();
        }

        /**
         * Parse a context key.
         *
         * @param key key
         * @return segments
         * @throws IllegalArgumentException if the key is not valid
         * @throws NullPointerException     if key is {@code null}
         */
        public static String[] parse(String key) {
            requireNonNull(key, "key is null");
            LinkedList<String> segments = new LinkedList<>();
            StringBuilder buf = new StringBuilder();
            char[] chars = key.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                switch (c) {
                    case ROOT_REF_CHAR:
                        if (i == 0) {
                            segments.add(ROOT_REF);
                            if (i + 2 < chars.length
                                && chars[i + 1] == SEPARATOR_CHAR
                                && chars[i + 2] == SEPARATOR_CHAR) {
                                // "~.." to "~"
                                i += 2;
                            }
                            continue;
                        }
                        throw new InvalidKeyException(c, key, i);
                    case SEPARATOR_CHAR:
                        if (i + 1 < chars.length) {
                            if (chars[i + 1] == SEPARATOR_CHAR) {
                                // ".."
                                i++;
                                if (i > 1 && isLetterOrDigit(chars[i - 2])) {
                                    // ".foo.." to "."
                                    segments.removeLast();
                                    continue;
                                }
                                segments.add(PARENT_REF);
                                continue;
                            }
                            if (i > 0 && isLetterOrDigit(chars[i + 1])) {
                                // valid "." as separator
                                continue;
                            }
                        }
                        throw new InvalidKeyException(c, key, i);
                    default:
                        if (c == SEGMENT_SEPARATOR) {
                            if ((i > 0 && i + 1 < chars.length)
                                && isLetterOrDigit(chars[i - 1])
                                && isLetterOrDigit(chars[i + 1])) {
                                buf.append(c);
                                continue;
                            }
                            throw new InvalidKeyException(c, key, i);
                        }
                        if (!isLetterOrDigit(c)) {
                            throw new InvalidKeyException(c, key, i);
                        }
                        buf.append(c);
                        if (i + 1 == chars.length || chars[i + 1] == SEPARATOR_CHAR) {
                            segments.add(buf.toString());
                            buf.setLength(0);
                        }
                }
            }
            if (buf.length() > 0) {
                throw new IllegalStateException("parse error");
            }
            return segments.toArray(new String[0]);
        }
    }

    /**
     * Context tree node.
     */
    public static class Scope {

        static final Scope EMPTY = new Scope() {
            @Override
            public ScopeValue<?> value(Value<?> newValue, ValueKind kind, Object... qualifiers) {
                throw new UnsupportedOperationException("Empty scope");
            }

            @Override
            public String key(boolean internal) {
                throw new UnsupportedOperationException("Empty scope");
            }

            @Override
            public ScopeValue<?> value() {
                return ScopeValue.empty();
            }
        };

        private final List<Scope> children = new ArrayList<>();
        private final Scope root;
        private final Scope parent;
        private final String id;
        private final boolean model;
        private final LazyValue<String> key = new LazyValue<>(() -> computeKey(false));
        private final LazyValue<String> internalKey = new LazyValue<>(() -> computeKey(true));
        private ScopeValue<?> value = ScopeValue.empty();
        private Visibility visibility;

        Scope() {
            this.root = this;
            this.parent = null;
            this.id = null;
            this.model = false;
        }

        private Scope(Scope parent, String id, Visibility visibility, boolean model) {
            if (id == null || id.isEmpty() || id.indexOf(Key.SEPARATOR_CHAR) >= 0) {
                throw new IllegalArgumentException("Invalid id");
            }
            this.parent = requireNonNull(parent, "parent is null");
            this.root = requireNonNull(parent.root, "root is null");
            this.id = id;
            this.visibility = visibility;
            this.model = model;
        }

        /**
         * Get the scope identifier.
         *
         * @return scope id
         */
        public String id() {
            return id;
        }

        /**
         * Test if this node should be used as a model value.
         *
         * @return {@code true} if this node should be used as a model value
         */
        public boolean isModel() {
            return model;
        }

        /**
         * Get the parent scope.
         *
         * @return parent, or {@code null} if this scope is a root scope.
         */
        public Scope parent() {
            return parent;
        }

        /**
         * Get the visibility.
         *
         * @return visibility
         */
        public Visibility visibility() {
            return visibility;
        }

        /**
         * Get the children.
         *
         * @return children
         */
        public List<Scope> children() {
            return children;
        }

        /**
         * Get the value.
         *
         * @return value
         */
        public ScopeValue<?> value() {
            return value;
        }

        /**
         * Update the value.
         *
         * @param newValue   value
         * @param kind       kind
         * @param qualifiers qualifiers
         * @return value
         */
        public ScopeValue<?> value(Value<?> newValue, ValueKind kind, Object... qualifiers) {
            if (value.isWritable()) {
                value = new ScopeValue<>(this, newValue, kind, qualifiers);
            }
            if (!Value.isEqual(newValue, value.delegate)) {
                throw new IllegalStateException(String.format(
                        "Cannot set value, key=%s, current=%s, new=%s",
                        internalKey(),
                        value,
                        newValue));
            }
            return value;
        }

        /**
         * Compute the key for this scope.
         *
         * @param internal if {@code true}, include all global parents
         * @return key
         */
        public String key(boolean internal) {
            return internal ? internalKey.get() : key.get();
        }

        /**
         * Get the internal key.
         *
         * @return internal key
         */
        public String internalKey() {
            return internalKey.get();
        }

        /**
         * Get the key.
         *
         * @return key
         */
        public String key() {
            return key.get();
        }

        /**
         * Resolve the absolute key.
         *
         * @param key key to resolve
         * @return absolute key
         */
        public String key(String key) {
            Scope scope = get(key);
            return scope == EMPTY ? key : scope.key();
        }

        /**
         * Resolve a scope.
         *
         * @param key key to resolve
         * @return scope
         */
        public Scope get(String key) {
            String[] segments = Key.parse(key);
            return resolve(segments, Scope::find);
        }

        /**
         * Get or create a scope.
         *
         * @param key key to resolve
         * @return scope
         */
        public Scope getOrCreate(String key) {
            return getOrCreate(key, false);
        }

        /**
         * Get or create a scope.
         *
         * @param key   key to resolve
         * @param model can be used as a model value
         * @return scope
         */
        public Scope getOrCreate(String key, boolean model) {
            String[] segments = Key.parse(key);
            return resolve(segments, (s, sid) -> s.getOrCreate(sid, model, Visibility.UNSET));
        }

        /**
         * Get or create a scope.
         *
         * @param node node
         * @return scope
         */
        public Scope getOrCreate(Node node) {
            return getOrCreate(
                    node.attribute("id").getString(),
                    node.attribute("model").asBoolean().orElse(false),
                    node.attribute("global").asBoolean().orElse(false));
        }

        /**
         * Get or create a scope.
         *
         * @param key    key
         * @param model  can be used as a model value
         * @param global {@code true} if the scope should be global, {@code false} if local.
         * @return scope
         */
        public Scope getOrCreate(String key, boolean model, boolean global) {
            return getOrCreate(key, model, global ? Visibility.GLOBAL : Visibility.LOCAL);
        }

        /**
         * Get or create a scope.
         *
         * @param key        key
         * @param model      can be used as a model value
         * @param visibility visibility
         * @return scope
         */
        public Scope getOrCreate(String key, boolean model, Visibility visibility) {
            if (key.indexOf(Key.SEPARATOR_CHAR) >= 0 || key.indexOf(Key.ROOT_REF_CHAR) >= 0) {
                throw new IllegalArgumentException("Invalid id");
            }
            Scope node = find(key);
            if (node != null) {
                node.visibility(visibility);
                return node;
            }
            node = new Scope(this, key, visibility, model);
            if (this != root) {
                if (visibility == Visibility.GLOBAL || this.visibility == Visibility.GLOBAL) {
                    Scope found = root.find(key);
                    if (found != null) {
                        node.visibility(found.visibility);
                        found.copy(node);
                        found.parent.children.remove(found);
                    }
                }
            }
            children.add(node);
            return node;
        }

        /**
         * Get all values.
         *
         * @return map of values
         */
        public Map<String, ScopeValue<?>> values() {
            Map<String, ScopeValue<?>> values = new LinkedHashMap<>();
            visit(node -> {
                ScopeValue<?> value = node.value();
                if (value.isPresent()) {
                    values.put(node.key(), value);
                }
            });
            return values;
        }

        /**
         * Substitute the properties of the form {@code ${key}} within the given string.
         * The keys are relative to the current scope.
         * Properties are substituted until the resulting string does not contain any references.
         *
         * @param value string to process
         * @return processed string
         * @throws IllegalArgumentException if the string contains any unresolved variable
         */
        public String interpolate(String value) {
            if (value == null) {
                return null;
            }
            String input = null;
            String output = value;
            while (!output.equals(input)) {
                input = output;
                output = evaluate(output, var -> {
                    ScopeValue<?> resolved = get(var).value;
                    if (resolved == ScopeValue.EMPTY) {
                        throw new IllegalArgumentException("Unresolved variable: " + var);
                    }
                    return resolved.asString().orElse(null);
                });
            }
            return output;
        }

        /**
         * Print this node.
         *
         * @return String
         */
        public String print() {
            StringBuilder sb = new StringBuilder();
            visit(node -> {
                Scope parent = node.parent;
                if (parent != null) {
                    int startIndex = sb.length();
                    for (Scope n = node.parent; n != null && n.parent != null; n = n.parent) {
                        sb.append(' ');
                        sb.append(Lists.isLast(n.parent.children, n) ? ' ' : '|');
                        n = n.parent;
                    }
                    sb.append(' ');
                    sb.append(Lists.isLast(node.parent.children, node) ? '\\' : '+');
                    sb.append("- ");
                    String indent = " ".repeat((sb.length() + 1) - startIndex);
                    sb.append(node.id);
                    sb.append(System.lineSeparator());
                    sb.append(indent);
                    sb.append(": ");
                    sb.append(node.value.orElse(null));
                    sb.append(" (");
                    sb.append(node.value.kind());
                    sb.append(')');
                    sb.append(System.lineSeparator());
                }
            });
            return sb.toString();
        }

        @Override
        public String toString() {
            return "ContextScope{"
                   + "id='" + id + '\''
                   + ", visibility=" + visibility
                   + '}';
        }

        void visit(Consumer<Scope> visitor) {
            Deque<Scope> stack = new ArrayDeque<>();
            stack.push(this);
            while (!stack.isEmpty()) {
                Scope node = stack.pop();
                List<Scope> children = node.children;
                if (!children.isEmpty()) {
                    ListIterator<Scope> it = children.listIterator(children.size());
                    while (it.hasPrevious()) {
                        Scope previous = it.previous();
                        stack.push(previous);
                    }
                }
                visitor.accept(node);
            }
        }

        private String computeKey(boolean internal) {
            StringBuilder resolved = new StringBuilder();
            Scope node = this;
            while (node.parent != null) {
                if (resolved.length() == 0) {
                    resolved.append(node.id);
                } else {
                    resolved.insert(0, node.id + Key.SEPARATOR);
                }
                if (!internal && (
                        node.visibility == Visibility.GLOBAL
                        || node.parent.visibility == Visibility.GLOBAL)) {
                    break;
                }
                node = node.parent;
            }
            return resolved.toString();
        }

        private Scope resolve(String[] segments, BiFunction<Scope, String, Scope> fn) {
            Scope node = this;
            for (int i = 0; i < segments.length; i++) {
                String segment = segments[i];
                if (i == 0 && Key.ROOT_REF.equals(segment)) {
                    node = root;
                } else if (Key.PARENT_REF.equals(segment)) {
                    node = i == 0 ? parent : node.parent;
                    if (node == null) {
                        // it was a root node
                        node = root;
                    }
                } else {
                    node = fn.apply(node, segment);
                    if (node == null) {
                        if (this != root) {
                            return root.resolve(segments, fn);
                        }
                        return Scope.EMPTY;
                    }
                }
            }
            return node;
        }

        private Scope find(String id) {
            if (id.equals(this.id)) {
                return this;
            }
            Deque<Scope> stack = new ArrayDeque<>(children);
            while (!stack.isEmpty()) {
                Scope node = stack.pop();
                if (node.id.equals(id)) {
                    return node;
                }
                if (node.visibility == Visibility.GLOBAL) {
                    stack.addAll(node.children);
                }
            }
            return null;
        }

        private void copy(Scope target) {
            Deque<Scope> stack = new ArrayDeque<>();
            Deque<Scope> copyStack = new ArrayDeque<>();
            if (target.value.isEmpty()) {
                target.value(value.delegate, value.kind());
            }
            for (Scope child : children) {
                stack.push(child);
                copyStack.push(target);
            }
            while (!stack.isEmpty()) {
                Scope src = stack.pop();
                Scope parent = copyStack.pop();
                Scope copy;
                copy = new Scope(parent, src.id, src.visibility, src.model);
                copy.value(src.value.delegate, src.value.kind());
                parent.children.add(copy);
                for (Scope contextScope : src.children) {
                    copyStack.push(copy);
                    stack.push(contextScope);
                }
            }
        }

        private void visibility(Visibility vis) {
            if (visibility != vis && vis != Visibility.UNSET) {
                if (visibility == Visibility.UNSET) {
                    visibility = vis;
                } else {
                    throw new IllegalStateException(String.format(
                            "Visibility mismatch, id=%s, current=%s, requested=%s",
                            id, visibility, vis));
                }
            }
        }
    }

    /**
     * Scope value.
     *
     * @param <T> value type
     */
    public static final class ScopeValue<T> implements Value<T> {

        private static final ScopeValue<?> EMPTY = new ScopeValue<>(Scope.EMPTY, Value.empty(), ValueKind.DEFAULT);

        private final Scope scope;
        private final Value<T> delegate;
        private final ValueKind kind;
        private final Object[] qualifiers;

        ScopeValue(Scope scope, Value<T> delegate, ValueKind kind, Object... qualifiers) {
            this.scope = scope;
            this.delegate = delegate;
            this.kind = kind;
            this.qualifiers = qualifiers;
        }

        Object[] qualifiers() {
            return qualifiers;
        }

        Scope scope() {
            return scope;
        }

        ValueKind kind() {
            return kind;
        }

        boolean isWritable() {
            switch (kind) {
                case EXTERNAL:
                case PRESET:
                    return false;
                default:
                    return true;
            }
        }

        boolean isSerializable() {
            switch (kind) {
                case EXTERNAL:
                case USER:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public String toString() {
            return "ContextValue{"
                   + "kind=" + kind
                   + ", value=" + delegate
                   + '}';
        }

        @SuppressWarnings("unchecked")
        static <T> ScopeValue<T> empty() {
            return (ScopeValue<T>) EMPTY;
        }

        /**
         * Filter this context value if present.
         *
         * @param predicate predicate
         * @return filtered value
         */
        public ScopeValue<T> filter(Predicate<ScopeValue<T>> predicate) {
            return isPresent() && predicate.test(this) ? this : empty();
        }

        @Override
        public ScopeValue<T> or(Supplier<Value<T>> supplier) {
            if (isPresent()) {
                return this;
            }
            if (supplier != null) {
                Value<T> value = supplier.get();
                if (value != null) {
                    return new ScopeValue<>(scope, value, kind);
                }
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> ScopeValue<R> map(Function<T, R> function) {
            return isEmpty() ? (ScopeValue<R>) this : new ScopeValue<>(scope, delegate.map(function), kind);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ScopeValue)) {
                return false;
            }
            ScopeValue<?> that = (ScopeValue<?>) o;
            return Objects.equals(scope, that.scope)
                   && kind == that.kind
                   && Value.isEqual(delegate, that.delegate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scope, kind, delegate);
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public Type type() {
            return delegate.type();
        }

        @Override
        public T get() {
            return delegate.get();
        }

        @Override
        public ScopeValue<Boolean> asBoolean() {
            if (delegate.type() == Type.BOOLEAN) {
                return cast();
            }
            return new ScopeValue<>(scope, delegate.asBoolean(), kind);
        }

        @Override
        public ScopeValue<String> asString() {
            switch (delegate.type()) {
                case STRING:
                case DYNAMIC:
                    return cast();
                default:
                    return new ScopeValue<>(scope, delegate.asString(), kind);
            }
        }

        @Override
        public ScopeValue<Integer> asInt() {
            if (delegate.type() == Type.INTEGER) {
                return cast();
            }
            return new ScopeValue<>(scope, delegate.asInt(), kind);
        }

        @Override
        public ScopeValue<List<String>> asList() {
            if (delegate.type() == Type.LIST) {
                return cast();
            }
            return new ScopeValue<>(scope, delegate.asList(), kind);
        }

        @SuppressWarnings("unchecked")
        private <U> ScopeValue<U> cast() {
            return (ScopeValue<U>) this;
        }
    }

    private static final class InvalidKeyException extends IllegalArgumentException {
        InvalidKeyException(char c, String key, int index) {
            super(String.format("Invalid character: %c, key=%s, index=%s", c, key, index));
        }
    }
}
