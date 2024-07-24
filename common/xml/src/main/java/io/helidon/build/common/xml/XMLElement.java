/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
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
     * Get the name.
     *
     * @return name, never {@code null}
     */
    String name();

    /**
     * Get the parent.
     *
     * @return parent or {@code null} if root element
     */
    XMLElement parent();

    /**
     * Get the children.
     *
     * @return children, never {@code null}
     */
    List<XMLElement> children();

    /**
     * Get children by name.
     *
     * @param name name
     * @return XMLElement, or {@code null} if not found
     */
    List<XMLElement> children(String name);

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
    Optional<XMLElement> child(String name);

    /**
     * Get the attributes.
     *
     * @return attributes, never {@code null}
     */
    Map<String, String> attributes();

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
        return parseBoolean(attribute(key, defaultValue ? "true" : "false"));
    }

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
     */
    void value(String value);

    /**
     * Traverse this element.
     *
     * @param visitor visitor
     */
    void visit(Visitor visitor);

    /**
     * Create a detached copy where this element is the root.
     *
     * @return XMLElement
     */
    XMLElement detach();

    /**
     * Visitor.
     */
    interface Visitor {
        /**
         * Visit (entering) an element.
         *
         * @param elt element
         */
        default void visitElement(XMLElement elt) {
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
     * Parse a document.
     *
     * @param is input stream
     * @return XMLElement, never null
     * @throws IOException  if an IO error occurs
     * @throws XMLException if a parsing error occurs
     */
    static XMLElement parse(InputStream is) throws IOException {
        ReaderImpl reader = new ReaderImpl();
        new XMLParser(is, reader).parse();
        if (!reader.stack.isEmpty()) {
            return reader.stack.pop().build();
        }
        throw new XMLException("document is empty");
    }

    final class UnmodifiableXMLElement extends XMLElementImpl {

        private final Map<String, List<XMLElement>> names = new HashMap<>();

        private UnmodifiableXMLElement(XMLElement parent, List<XMLElement> children, Builder builder) {
            super(parent, unmodifiableList(children), unmodifiableMap(builder.attributes), builder.name, builder.value);
        }

        @Override
        public void value(String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<XMLElement> children(String name) {
            return names.computeIfAbsent(name, super::children);
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    class XMLElementImpl implements XMLElement {

        private final XMLElement parent;
        private final List<XMLElement> children;
        private final Map<String, String> attributes;
        private final String name;
        private String value;

        private XMLElementImpl(XMLElement parent,
                               List<XMLElement> children,
                               Map<String, String> attributes,
                               String name,
                               String value) {
            this.parent = parent;
            this.children = children;
            this.attributes = attributes;
            this.name = Objects.requireNonNull(name, "name is null!");
            this.value = value;
        }

        private XMLElementImpl(XMLElement parent, List<XMLElement> children, Builder builder) {
            this(parent, children, builder.attributes, builder.name, builder.value);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public XMLElement parent() {
            return parent;
        }

        @Override
        public List<XMLElement> children() {
            return children;
        }

        @Override
        public List<XMLElement> children(String name) {
            return children.stream().filter(e -> e.name().equals(name)).collect(Collectors.toList());
        }

        @Override
        public Optional<XMLElement> child(String name) {
            return children(name).stream().findFirst();
        }

        @Override
        public Map<String, String> attributes() {
            return attributes;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public void value(String value) {
            this.value = value;
        }

        @Override
        public void visit(Visitor visitor) {
            Deque<XMLElement> stack = new ArrayDeque<>();
            stack.push(this);
            XMLElement parent = this.parent;
            while (!stack.isEmpty()) {
                XMLElement elt = stack.peek();
                if (elt == parent) {
                    visitor.postVisitElement(elt);
                    parent = elt.parent();
                    stack.pop();
                } else {
                    visitor.visitElement(elt);
                    parent = elt;
                    List<XMLElement> children = elt.children();
                    for (int i = children.size() - 1; i >= 0; i--) {
                        stack.push(children.get(i));
                    }
                }
            }
        }

        @Override
        public XMLElement detach() {
            Deque<XMLElement.Builder> stack = new ArrayDeque<>();
            visit(new Visitor() {
                @Override
                public void visitElement(XMLElement elt) {
                    Builder builder = XMLElement.builder()
                            .modifiable(!(elt instanceof UnmodifiableXMLElement))
                            .name(elt.name())
                            .attributes(elt.attributes())
                            .value(elt.value());
                    if (!stack.isEmpty()) {
                        stack.peek().child(builder);
                    }
                    stack.push(builder);
                }

                @Override
                public void postVisitElement(XMLElement elt) {
                    if (stack.size() > 1) {
                        stack.pop();
                    }
                }
            });
            return stack.pop().build();
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

        @Override
        public String toString() {
            StringWriter buf = new StringWriter();
            XMLWriter writer = new XMLWriter(buf);
            writer.append(this);
            writer.close();
            return buf.toString();
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
     * Builder of {@link XMLElement}.
     */
    final class Builder {

        private Builder parent;
        private String name;
        private final List<Builder> childBuilders = new ArrayList<>();
        private Map<String, String> attributes = Map.of();
        private String value = "";
        private boolean modifiable;

        private XMLElement elt;
        private List<XMLElement> childElements = List.of();

        /**
         * Set the element name.
         *
         * @param name name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the element attributes.
         *
         * @param attributes attributes
         * @return this builder
         */
        public Builder attributes(Map<String, String> attributes) {
            this.attributes = new HashMap<>(attributes);
            return this;
        }

        /**
         * Set the element value.
         *
         * @param value value
         * @return this builder
         */
        public Builder value(String value) {
            if (value != null) {
                this.value = value;
            }
            return this;
        }

        /**
         * Add a child.
         *
         * @param child child builder
         * @return this builder
         */
        public Builder child(Builder child) {
            childBuilders.add(child);
            child.parent = this;
            return this;
        }

        /**
         * Add a child.
         *
         * @param consumer child builder consumer
         * @return this builder
         */
        public Builder child(Consumer<Builder> consumer) {
            Builder child = new Builder().modifiable(modifiable);
            consumer.accept(child);
            return child(child);
        }

        /**
         * Configure whether to create a modifiable element.
         *
         * @param modifiable {@code} true to create a modifiable element
         * @return this builder
         */
        public Builder modifiable(boolean modifiable) {
            this.modifiable = modifiable;
            return this;
        }

        /**
         * Build the element.
         *
         * @return XMLElement
         */
        public XMLElement build() {
            Deque<Builder> builders = new ArrayDeque<>(childBuilders);
            elt = build0();
            while (!builders.isEmpty()) {
                Builder builder = builders.pop();
                builder.elt = builder.build0();
                builder.parent.childElements.add(builder.elt);
                for (int i = builder.childBuilders.size() - 1; i >= 0; i--) {
                    builders.push(builder.childBuilders.get(i));
                }
            }
            return elt;
        }

        private XMLElement build0() {
            childElements = new ArrayList<>();
            XMLElement parentElt = parent != null ? parent.elt : null;
            if (modifiable) {
                return new XMLElementImpl(parentElt, childElements, this);
            }
            return new UnmodifiableXMLElement(parentElt, childElements, this);
        }
    }

    final class ReaderImpl implements XMLReader {

        private final Deque<Builder> stack = new ArrayDeque<>();

        @Override
        public void startElement(String name, Map<String, String> attributes) {
            Builder ctx = new Builder();
            ctx.parent = stack.peek();
            ctx.name = name;
            ctx.attributes = attributes;
            stack.push(ctx);
        }

        @Override
        public void endElement(String name) {
            Builder ctx = peek();
            if (ctx.parent != null) {
                ctx.parent.childBuilders.add(ctx);
            }
            if (stack.size() > 1) {
                stack.pop();
            }
        }

        @Override
        public void elementText(String data) {
            Builder ctx = peek();
            ctx.value = data;
        }

        private Builder peek() {
            Builder ctx = stack.peek();
            if (ctx == null) {
                throw new IllegalStateException("context is null");
            }
            return ctx;
        }
    }
}
