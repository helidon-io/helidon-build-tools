/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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

package io.helidon.build.maven.sitegen;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.helidon.build.common.FileUtils;
import io.helidon.build.common.SubstitutionVariables;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Simple config.
 */
@SuppressWarnings("unused")
public final class Config {

    /**
     * Config mapping exception.
     */
    public static class MappingException extends RuntimeException {

        private MappingException(Class<?> actual, Class<?> expected) {
            super(String.format("Cannot get a %s as %s", actual.getSimpleName(), expected.getSimpleName()));
        }

        private MappingException(Throwable cause) {
            super(cause);
        }
    }

    private final Object value;
    private final Config parent;
    private final SubstitutionVariables substitutions;

    private Config(Object value, Map<String, String> properties) {
        this.value = value;
        this.parent = null;
        this.substitutions = SubstitutionVariables.of(properties);
    }

    private Config(Object value, Config parent) {
        this.value = value;
        this.parent = parent;
        this.substitutions = parent.substitutions;
    }

    /**
     * Get the parent node.
     *
     * @return parent node
     */
    public Config parent() {
        return parent;
    }

    /**
     * Test if this is a root config node.
     *
     * @return {@code true} if root, {@code false} otherwise
     */
    public boolean isRoot() {
        return parent == this;
    }

    /**
     * Get a config node.
     *
     * @param key key
     * @return node
     */
    public Config get(String key) {
        Object v = value instanceof Map ? ((Map<?, ?>) value).get(key) : null;
        return new Config(v, this);
    }

    /**
     * Test if this node contains the given key.
     *
     * @param key key
     * @return {@code true} if the node contains the key, {@code false} otherwise
     */
    public boolean containsKey(String key) {
        if (value instanceof Map) {
            return ((Map<?, ?>) value).containsKey(key);
        }
        return false;
    }

    /**
     * Get this config node as an optional.
     *
     * @return optional
     */
    public Optional<Config> asOptional() {
        return value == null ? Optional.empty() : Optional.of(this);
    }

    /**
     * Get the value as a string.
     *
     * @return optional
     */
    public Optional<String> asString() {
        return as(String.class);
    }

    /**
     * Get the value as a boolean.
     *
     * @return optional
     */
    public Optional<Boolean> asBoolean() {
        return as(Boolean.class);
    }

    /**
     * Map the value using a mapping function.
     *
     * @param mapper mapping function
     * @param <T>    requested type
     * @return optional
     */
    public <T> Optional<T> as(Function<Object, T> mapper) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(mapper.apply(value));
        } catch (Throwable ex) {
            throw new MappingException(ex);
        }
    }

    /**
     * Map the value to a given type.
     *
     * @param type requested type
     * @param <T>  requested type
     * @return optional
     */
    public <T> Optional<T> as(Class<T> type) {
        return as(o -> convert(o, type));
    }

    /**
     * Map the value to a list of nodes.
     *
     * @return optional
     */
    public Optional<List<Config>> asNodeList() {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof List) {
            return Optional.of(((List<?>) value).stream()
                    .map(e -> new Config(e, this))
                    .collect(Collectors.toList()));
        }
        throw new MappingException(value.getClass(), List.class);
    }

    /**
     * Get the value as a list.
     *
     * @return optional
     */
    public Optional<List<String>> asList() {
        return asList(Function.identity());
    }

    /**
     * Get the value as a list.
     *
     * @param type value type
     * @param <T>  value type
     * @return optional
     */
    public <T> Optional<List<T>> asList(Class<T> type) {
        return asList(e -> convert(e, type));
    }

    /**
     * Get the value as a list.
     *
     * @param mapper value mapper
     * @param <T>    value type
     * @return optional
     */
    public <T> Optional<List<T>> asList(Function<String, T> mapper) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof List) {
            List<T> values = new ArrayList<>();
            traverse((prefix, entry) -> {
                T value = mapper.apply(entry.getValue());
                values.add(value);
            });
            return Optional.of(values);
        }
        throw new MappingException(value.getClass(), List.class);
    }

    /**
     * Get the value as a map.
     *
     * @return optional
     */
    public Optional<Map<String, String>> asMap() {
        return asMap(Function.identity());
    }

    /**
     * Get the value as a map.
     *
     * @param type value type
     * @param <T>  value type
     * @return optional
     */
    public <T> Optional<Map<String, T>> asMap(Class<T> type) {
        return asMap(e -> convert(e, type));
    }

    /**
     * Get the value as a map.
     *
     * @param mapper value mapper
     * @param <T>    value type
     * @return optional
     */
    public <T> Optional<Map<String, T>> asMap(Function<String, T> mapper) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Map) {
            Map<String, T> values = new TreeMap<>();
            traverse((prefix, entry) -> {
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                values.put(key, mapper.apply(entry.getValue()));
            });
            return Optional.of(values);
        }
        throw new MappingException(value.getClass(), Map.class);
    }

    /**
     * Create a new instance.
     *
     * @param value      underlying value
     * @param properties substitution properties
     * @return new instance
     */
    public static Config create(Object value, Map<String, String> properties) {
        return new Config(value, properties);
    }

    /**
     * Create a new instance.
     *
     * @param path       resource path
     * @param clazz      class used to load the resource
     * @param properties substitution properties
     * @return new instance
     */
    public static Config create(String path, Class<?> clazz, Map<String, String> properties) {
        return create(FileUtils.resourceAsPath(path, clazz), properties);
    }

    /**
     * Create a new instance.
     *
     * @param path       path
     * @param properties substitution properties
     * @return new instance
     */
    public static Config create(Path path, Map<String, String> properties) {
        try {
            return create(Files.newBufferedReader(path), properties);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Create a new instance.
     *
     * @param reader     reader
     * @param properties substitution properties
     * @return new instance
     */
    public static Config create(Reader reader, Map<String, String> properties) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        return create((Object) yaml.loadAs(reader, Object.class), properties);
    }

    private void traverse(BiConsumer<String, Map.Entry<String, String>> visitLeaf) {
        Deque<String> path = new ArrayDeque<>();
        traverse((k, v) -> {
            if (k != null) {
                path.addLast(substitutions.resolve(k));
            }
        }, (k, v) -> {
            if (!path.isEmpty()) {
                path.removeLast();
            }
        }, (k, v) -> {
            String prefix = String.join(".", path);
            visitLeaf.accept(prefix, Map.entry(substitutions.resolve(k), substitutions.resolve(v)));
        });
    }

    private void traverse(BiConsumer<String, Object> visitNode,
                          BiConsumer<String, Object> postVisitNode,
                          BiConsumer<String, String> visitLeaf) {

        Deque<Object> parents = new ArrayDeque<>();
        Deque<String> keys = new ArrayDeque<>();
        Deque<Object> stack = new ArrayDeque<>();
        stack.push(value);
        while (!stack.isEmpty()) {
            Object parent = parents.peek();
            String key = keys.peek();
            Object node = stack.peek();
            if (parent == node) {
                postVisitNode.accept(key, node);
                stack.pop();
                if (!stack.isEmpty()) {
                    parents.pop();
                    keys.pop();
                }
            } else if (node instanceof Map) {
                List<? extends Map.Entry<?, ?>> entries = List.copyOf(((Map<?, ?>) node).entrySet());
                ListIterator<? extends Map.Entry<?, ?>> it = entries.listIterator(entries.size());
                while (it.hasPrevious()) {
                    Map.Entry<?, ?> previous = it.previous();
                    stack.push(previous.getValue());
                    keys.push(previous.getKey().toString());
                }
                parents.push(node);
                visitNode.accept(key, node);
            } else if (node instanceof List) {
                List<?> list = (List<?>) node;
                ListIterator<?> it = list.listIterator(list.size());
                while (it.hasPrevious()) {
                    keys.push(String.valueOf(it.previousIndex()));
                    stack.push(it.previous());
                }
                parents.push(node);
                visitNode.accept(key, node);
            } else {
                visitLeaf.accept(key, String.valueOf(node));
                stack.pop();
                keys.pop();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convert(Object obj, Class<T> type) {
        String value = substitutions.resolve(String.valueOf(obj));
        if (type.equals(String.class)) {
            return (T) value;
        }
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return (T) Boolean.valueOf(value);
        }
        if (type.equals(Character.class) || type.equals(char.class)) {
            return (T) Character.valueOf(value.charAt(0));
        }
        if (type.equals(Byte.class) || type.equals(byte.class)) {
            return (T) Byte.valueOf(value);
        }
        if (type.equals(Short.class) || type.equals(short.class)) {
            return (T) Short.valueOf(value);
        }
        if (type.equals(Integer.class) || type.equals(int.class)) {
            return (T) Integer.valueOf(value);
        }
        if (type.equals(Long.class) || type.equals(long.class)) {
            return (T) Long.valueOf(value);
        }
        if (type.equals(Float.class) || type.equals(float.class)) {
            return (T) Float.valueOf(value);
        }
        if (type.equals(Double.class) || type.equals(double.class)) {
            return (T) Double.valueOf(value);
        }
        throw new MappingException(obj.getClass(), type);
    }
}
