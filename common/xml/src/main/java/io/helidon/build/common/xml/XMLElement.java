/*
 * Copyright (c) 2024, 2026 Oracle and/or its affiliates.
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
package io.helidon.build.common.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Boolean.parseBoolean;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * XML element.
 */
public interface XMLElement {

    /**
     * Get the parent.
     *
     * @return parent or {@code null} if root element
     */
    XMLElement parent();

    /**
     * Set the parent.
     *
     * @param parent parent
     * @return this instance
     */
    XMLElement parent(XMLElement parent);

    /**
     * Get the name.
     *
     * @return name, never {@code null}
     */
    String name();

    /**
     * Get the value.
     *
     * @return value, never {@code null}
     */
    String value();

    /**
     * Set the value.
     *
     * @param value value
     * @return this instance
     */
    XMLElement value(String value);

    /**
     * Get the attributes.
     *
     * @return attributes, never {@code null}
     */
    Map<String, String> attributes();

    /**
     * Get the children.
     *
     * @return children, never {@code null}
     */
    List<XMLElement> children();

    /**
     * Get the source location.
     *
     * @return Location, never {@code null}
     */
    Location location();

    /**
     * Get children by name.
     *
     * @param name name
     * @return XMLElement, or {@code null} if not found
     */
    default List<XMLElement> children(String name) {
        return children().stream()
                .filter(e -> e.name().equals(name))
                .collect(Collectors.toList());
    }

    /**
     * Get children by path.
     *
     * @param path path
     * @return XMLElement, or {@code null} if not found
     */
    default List<XMLElement> childrenAt(String... path) {
        if (path.length == 0) {
            return List.of();
        }
        Stream<XMLElement> stream = Stream.of(this);
        for (String p : path) {
            stream = stream.flatMap(e -> e.children(p).stream());
        }
        return stream.collect(Collectors.toList());
    }

    /**
     * Get a child by name.
     *
     * @param name name
     * @return optional
     */
    default Optional<XMLElement> child(String name) {
        return children(name).stream().findFirst();
    }

    /**
     * Get a child by path.
     *
     * @param path path
     * @return optional
     */
    default Optional<XMLElement> childAt(String... path) {
        if (path.length == 0) {
            return Optional.empty();
        }
        Stream<XMLElement> stream = Stream.of(this);
        for (String p : path) {
            stream = stream.flatMap(e -> e.children(p).stream());
        }
        return stream.findFirst();
    }

    /**
     * Traverse this element.
     *
     * @return Iterable
     */
    default Iterable<XMLElement> traverse() {
        return traverse(n -> true);
    }

    /**
     * Traverse this element.
     *
     * @param predicate predicate
     * @return Iterable
     */
    default Iterable<XMLElement> traverse(Predicate<XMLElement> predicate) {
        return traverse(predicate, ignored -> {});
    }

    /**
     * Traverse this element.
     *
     * @param predicate predicate
     * @param postVisit post-visit consumer
     * @return Iterable
     */
    default Iterable<XMLElement> traverse(Predicate<XMLElement> predicate, Consumer<XMLElement> postVisit) {
        XMLElement self = this;
        return () -> {
            final class TraversalFrame {
                private final XMLElement element;
                private final boolean exiting;

                private TraversalFrame(XMLElement element, boolean exiting) {
                    this.element = element;
                    this.exiting = exiting;
                }
            }

            Deque<TraversalFrame> stack = new ArrayDeque<>();
            stack.push(new TraversalFrame(self, false));
            return new Iterator<>() {
                private XMLElement next;

                @Override
                public boolean hasNext() {
                    while (next == null && !stack.isEmpty()) {
                        TraversalFrame frame = stack.pop();
                        XMLElement node = frame.element;
                        if (frame.exiting) {
                            postVisit.accept(node);
                            continue;
                        }
                        stack.push(new TraversalFrame(node, true));
                        List<XMLElement> children = node.children();
                        for (int i = children.size() - 1; i >= 0; i--) {
                            stack.push(new TraversalFrame(children.get(i), false));
                        }
                        if (predicate.test(node)) {
                            next = node;
                        }
                    }
                    return next != null;
                }

                @Override
                public XMLElement next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    XMLElement node = next;
                    next = null;
                    return node;
                }
            };
        };
    }

    /**
     * Traverse this element, selecting elements whose paths match the pattern.
     * The pattern matches the complete path relative to this element, including
     * this element's name and a leading slash.
     *
     * @param pattern path pattern
     * @return Iterable
     */
    default Iterable<XMLElement> traverse(Pattern pattern) {
        XMLElement self = this;
        return () -> {
            StringBuilder path = new StringBuilder();
            return self.traverse(element -> {
                path.append('/').append(element.name());
                return pattern.matcher(path).matches();
            }, element -> path.setLength(path.length() - element.name().length() - 1)).iterator();
        };
    }

    /**
     * Get an attribute value.
     *
     * @param key attribute key
     * @return value
     * @throws XMLException if the attribute is not found
     */
    default String attribute(String key) {
        String value = attributes().get(key);
        if (value == null) {
            throw new XMLException(String.format(
                    "Missing required attribute '%s', element: '%s'", key, name()));
        }
        return value;
    }

    /**
     * Get an attribute value.
     *
     * @param key          attribute key
     * @param defaultValue default value
     * @return value
     */
    default String attribute(String key, String defaultValue) {
        return attributes().getOrDefault(key, defaultValue);
    }

    /**
     * Get an attribute value.
     *
     * @param key          attribute key
     * @param mapper       value mapper
     * @param defaultValue default value
     * @param <T>          value type
     * @return value
     */
    default <T> T attribute(String key, Function<String, T> mapper, T defaultValue) {
        String value = attributes().get(key);
        if (value != null) {
            return mapper.apply(value);
        }
        return defaultValue;
    }

    /**
     * Get an attribute as list value.
     *
     * @param key       attribute key
     * @param separator separator character
     * @return value
     */
    default List<String> attributeList(String key, String separator) {
        String value = attribute(key, null);
        return value == null ? List.of() : Arrays.asList(value.split(separator));
    }

    /**
     * Get an attribute as a boolean value.
     *
     * @param key          attribute key
     * @param defaultValue default value
     * @return value
     */
    default boolean attributeBoolean(String key, boolean defaultValue) {
        return parseBoolean(attribute(key, Boolean.toString(defaultValue)));
    }

    /**
     * Traverse this element.
     * A visitor may skip an element's descendants by returning {@code false}
     * from {@link Visitor#visitElement(XMLElement)}. The element's
     * {@link Visitor#postVisitElement(XMLElement)} callback is still invoked.
     *
     * @param visitor visitor
     * @param <T>     visitor type
     * @return visitor
     */
    default <T extends Visitor> T visit(T visitor) {
        Deque<XMLElement> stack = new ArrayDeque<>();
        stack.push(this);
        XMLElement parent = this.parent();
        while (!stack.isEmpty()) {
            XMLElement elt = stack.peek();
            if (elt == parent) {
                visitor.postVisitElement(elt);
                parent = elt.parent();
                stack.pop();
            } else {
                if (visitor.visitElement(elt)) {
                    List<XMLElement> children = elt.children();
                    for (int i = children.size() - 1; i >= 0; i--) {
                        stack.push(children.get(i));
                    }
                }
                if (parent != elt.parent()) {
                    throw new IllegalStateException("Parent mismatch");
                }
                parent = elt;
            }
        }
        return visitor;
    }

    /**
     * Create a detached copy where this element is the root.
     *
     * @return XMLElement
     */
    XMLElement detach();

    /**
     * Render an element to a string.
     *
     * @param pretty {@code true} for newlines and indentation
     * @return String
     */
    default String toString(boolean pretty) {
        StringWriter buf = new StringWriter();
        try (XMLGenerator writer = new XMLGenerator(buf, pretty)) {
            writer.append(this);
        }
        if (pretty) {
            buf.append("\n");
        }
        return buf.toString();
    }

    /**
     * Visitor.
     */
    interface Visitor {
        /**
         * Visit (entering) an element.
         *
         * @param elt element
         * @return {@code true} to visit the element's children, {@code false}
         *         to skip them
         */
        default boolean visitElement(XMLElement elt) {
            return true;
        }

        /**
         * Post-visit (leaving) an element.
         *
         * @param elt element
         */
        default void postVisitElement(XMLElement elt) {
        }
    }

    /**
     * Read a document.
     *
     * @param file file
     * @return XMLElement, never null
     */
    static XMLElement read(Path file) {
        return read(file, Objects.toString(file.getFileName(), file.toString()), true);
    }

    /**
     * Read a document.
     *
     * @param file   file
     * @param source source
     * @param readOnly {@code true} to create a read-only document, {@code false} otherwise
     * @return XMLElement, never null
     */
    static XMLElement read(Path file, String source, boolean readOnly) {
        try (InputStream is = Files.newInputStream(file)) {
            return read(is, source, readOnly);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read file: " + file, ex);
        }
    }

    /**
     * Read a document.
     *
     * @param str raw string
     * @return XMLElement, never null
     */
    static XMLElement read(String str) {
        return read(str, "unknown", true);
    }

    /**
     * Read a document.
     *
     * @param str raw string
     * @param source source
     * @param readOnly {@code true} to create a read-only document, {@code false} otherwise
     * @return XMLElement, never null
     */
    static XMLElement read(String str, String source, boolean readOnly) {
        return read(new ByteArrayInputStream(str.getBytes(Charset.defaultCharset())), source, readOnly);
    }

    /**
     * Read a document.
     *
     * @param is input stream
     * @return XMLElement, never null
     */
    static XMLElement read(InputStream is) {
        return read(is, "unknown", true);
    }

    /**
     * Read a document.
     *
     * @param is     input stream
     * @param source source
     * @param readOnly {@code true} to create a read-only document, {@code false} otherwise
     * @return XMLElement, never null
     */
    static XMLElement read(InputStream is, String source, boolean readOnly) {
        try (XMLReader reader = new XMLReader(is, source)) {
            return reader.readElement(readOnly);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex.getMessage(), ex);
        }
    }

    /**
     * Create a new builder.
     *
     * @return Builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Create a builder copy of an element.
     *
     * @param elt element to copy
     * @return Builder
     */
    static Builder builder(XMLElement elt) {
        return (Builder) elt.visit(new CopyVisitor<>((e, b) -> b, XMLElement::parent)).element();
    }

    /**
     * Source location.
     */
    final class Location {

        /**
         * Unknown location.
         */
        public static final Location UNKNOWN = new Location("[unknown]", 0, 0);

        private final String source;
        private final int lineNo;
        private final int colNo;

        /**
         * Create a new instance.
         *
         * @param source source
         * @param lineNo line number
         * @param colNo  column number
         */
        public Location(String source, int lineNo, int colNo) {
            this.source = Objects.requireNonNull(source, "source is null");
            if (lineNo < 0) {
                throw new IllegalArgumentException("Invalid line number: " + lineNo);
            }
            this.lineNo = lineNo;
            if (colNo < 0) {
                throw new IllegalArgumentException("Invalid line character number: " + colNo);
            }
            this.colNo = colNo;
        }

        /**
         * Get the file name.
         *
         * @return file name
         */
        public String source() {
            return source;
        }

        /**
         * Get the line number.
         *
         * @return line number
         */
        public int lineNumber() {
            return lineNo;
        }

        @Override
        public String toString() {
            return source + ":" + lineNo + ":" + colNo;
        }
    }

    /**
     * {@link XMLElementImpl} implementation.
     */
    final class XMLElementImpl implements XMLElement {

        private final Map<String, List<XMLElement>> names = new HashMap<>();
        private final XMLElementImpl parent;
        private final String name;
        private final Map<String, String> attributes;
        private final List<XMLElement> children;
        private final Location location;
        private String value;

        XMLElementImpl(XMLElementImpl parent, XMLElement builder) {
            this.parent = parent;
            this.name = Objects.requireNonNull(builder.name(), "name is null!");
            this.attributes = unmodifiableMap(builder.attributes());
            this.children = unmodifiableList(builder.children());
            this.location = builder.location();
        }

        @Override
        public XMLElementImpl parent() {
            return parent;
        }

        @Override
        public XMLElement parent(XMLElement parent) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String value() {
            return value != null ? value : "";
        }

        @Override
        public XMLElement value(String value) {
            if (this.value != null) {
                throw new IllegalStateException("Value already set");
            }
            this.value = value;
            return this;
        }

        @Override
        public List<XMLElement> children() {
            return children;
        }

        @Override
        public Map<String, String> attributes() {
            return attributes;
        }

        @Override
        public Location location() {
            return location;
        }

        @Override
        public List<XMLElement> children(String name) {
            return names.computeIfAbsent(name, XMLElement.super::children);
        }

        @Override
        public XMLElement detach() {
            if (parent == null) {
                return this;
            }
            return visit(new CopyVisitor<>(XMLElementImpl::new, XMLElementImpl::parent)).element();
        }

        @Override
        public String toString() {
            return toString(true);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            XMLElementImpl that = (XMLElementImpl) o;
            return Objects.equals(children, that.children)
                   && Objects.equals(attributes, that.attributes)
                   && Objects.equals(name, that.name)
                   && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(children, attributes, name, value);
        }
    }

    /**
     * Builder of {@link XMLElement}.
     */
    final class Builder implements XMLElement {

        private final List<XMLElement> children = new ArrayList<>();
        private final Map<String, String> attributes = new LinkedHashMap<>();
        private Builder parent;
        private String name;
        private String value;
        private Location location = Location.UNKNOWN;

        private Builder() {
        }

        @Override
        public Builder parent() {
            return parent;
        }

        @Override
        public Builder parent(XMLElement parent) {
            this.parent = (Builder) parent;
            return this;
        }

        @Override
        public String name() {
            return String.valueOf(name);
        }

        @Override
        public String value() {
            return value != null ? value : "";
        }

        @Override
        public Builder value(String value) {
            if (value != null) {
                this.value = value;
            }
            return this;
        }

        @Override
        public List<XMLElement> children() {
            return children;
        }

        @Override
        public Map<String, String> attributes() {
            return attributes;
        }

        @Override
        public Location location() {
            return location;
        }

        @Override
        public Builder detach() {
            if (parent == null) {
                return this;
            }
            return visit(new CopyVisitor<Builder>((b, e) -> b, Builder::parent)).element();
        }

        @Override
        public String toString() {
            return toString(true);
        }

        /**
         * Set the element name.
         *
         * @param name name
         * @return this builder
         */
        public Builder name(String name) {
            if (name != null && !name.isBlank()) {
                this.name = name;
            }
            return this;
        }

        /**
         * Add element attributes.
         *
         * @param attributes attributes
         * @return this builder
         */
        public Builder attributes(Map<String, String> attributes) {
            this.attributes.putAll(attributes);
            return this;
        }

        /**
         * Set the element name.
         *
         * @param location location
         * @return this builder
         */
        public Builder location(Location location) {
            if (location != null) {
                this.location = location;
            }
            return this;
        }

        /**
         * Add a child.
         *
         * @param consumer child builder consumer
         * @return this builder
         */
        public Builder child(Consumer<Builder> consumer) {
            Builder builder = new Builder();
            consumer.accept(builder);
            children.add(builder);
            builder.parent(this);
            return this;
        }

        /**
         * Build the element.
         *
         * @return XMLElement
         */
        public XMLElement build() {
            return visit(new CopyVisitor<>(XMLElementImpl::new, XMLElementImpl::parent)).element();
        }
    }

    /**
     * Copy visitor.
     *
     * @param <T> output element type
     */
    final class CopyVisitor<T extends XMLElement> implements Visitor {

        private XMLElement builder = null;
        private T node;
        private T last = null;
        private final BiFunction<T, XMLElement, T> pushFunc;
        private final Function<T, T> popFunc;

        CopyVisitor(BiFunction<T, XMLElement, T> pushFunc, Function<T, T> popFunc) {
            this.pushFunc = pushFunc;
            this.popFunc = popFunc;
        }

        @Override
        public boolean visitElement(XMLElement elt) {
            builder = XMLElement.builder()
                    .parent(builder)
                    .name(elt.name())
                    .value(elt.value())
                    .attributes(elt.attributes())
                    .location(elt.location());
            node = pushFunc.apply(node, builder);
            node.value(elt.value());
            if (builder.parent() != null) {
                builder.parent().children().add(node);
            }
            return true;
        }

        @Override
        public void postVisitElement(XMLElement elt) {
            if (node != null) {
                T parent = popFunc.apply(node);
                if (parent == null) {
                    last = node;
                }
                node = parent;
                builder = builder.parent();
            }
        }

        /**
         * Get the copy.
         *
         * @return copy
         * @throws NoSuchElementException if the element is {@code null}
         */
        public T element() {
            if (last == null) {
                throw new NoSuchElementException("No element found");
            }
            return last;
        }
    }
}
