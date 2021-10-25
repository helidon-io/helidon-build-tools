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
package io.helidon.build.archetype.engine.v2.ast;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Boolean.parseBoolean;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Input block.
 */
public abstract class Input extends Block {

    private final String label;
    private final String help;
    private final String prompt;

    private Input(Input.Builder builder) {
        super(builder);
        label = builder.attributes().get("label");
        prompt = builder.attributes().get("prompt");
        help = builder.help;
    }

    /**
     * Get the input label.
     *
     * @return label
     */
    public String label() {
        return label;
    }

    /**
     * Get the input prompt.
     *
     * @return prompt
     */
    public String prompt() {
        return prompt;
    }

    /**
     * Get the input help.
     *
     * @return name
     */
    public String help() {
        return help;
    }

    /**
     * Input visitor.
     *
     * @param <A> argument type
     */
    public interface Visitor<A> {

        /**
         * Visit a boolean input.
         *
         * @param input input
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult visitBoolean(Boolean input, A arg) {
            return visitAny(input, arg);
        }

        /**
         * Visit a boolean input after traversing the nested statements.
         *
         * @param input input
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult postVisitBoolean(Boolean input, A arg) {
            return postVisitAny(input, arg);
        }

        /**
         * Visit a text input.
         *
         * @param input input
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult visitText(Text input, A arg) {
            return visitAny(input, arg);
        }

        /**
         * Visit a text input after traversing the nested statements.
         *
         * @param input input
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult postVisitText(Text input, A arg) {
            return postVisitAny(input, arg);
        }

        /**
         * Visit an option.
         *
         * @param option option
         * @param arg    visitor argument
         * @return result
         */
        default VisitResult visitOption(Option option, A arg) {
            return visitAny(option, arg);
        }

        /**
         * Visit an option after traversing the nested statements.
         *
         * @param option option
         * @param arg    visitor argument
         * @return result
         */
        default VisitResult postVisitOption(Option option, A arg) {
            return postVisitAny(option, arg);
        }

        /**
         * Visit an enum input.
         *
         * @param input input
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult visitEnum(Enum input, A arg) {
            return visitAny(input, arg);
        }

        /**
         * Visit an enum input after traversing the nested statements.
         *
         * @param input input
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult postVisitEnum(Enum input, A arg) {
            return postVisitAny(input, arg);
        }

        /**
         * Visit a list input.
         *
         * @param input input
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult visitList(List input, A arg) {
            return visitAny(input, arg);
        }

        /**
         * Visit a list input after traversing the nested statements.
         *
         * @param input input
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult postVisitList(List input, A arg) {
            return postVisitAny(input, arg);
        }

        /**
         * Visit any input.
         *
         * @param input input
         * @param arg   visitor argument
         * @return result
         */
        @SuppressWarnings("unused")
        default VisitResult visitAny(Input input, A arg) {
            return VisitResult.CONTINUE;
        }

        /**
         * Visit any input after traversing the nested statements.
         *
         * @param input input
         * @param arg   visitor argument
         * @return result
         */
        @SuppressWarnings("unused")
        default VisitResult postVisitAny(Input input, A arg) {
            return VisitResult.CONTINUE;
        }
    }

    /**
     * Visit this input.
     *
     * @param visitor visitor
     * @param arg     visitor argument
     * @param <A>     visitor argument type
     * @return result
     */
    public abstract <A> VisitResult accept(Visitor<A> visitor, A arg);

    /**
     * Visit this input after traversing the nested statements.
     *
     * @param visitor visitor
     * @param arg     visitor argument
     * @param <A>     visitor argument type
     * @return result
     */
    public abstract <A> VisitResult acceptAfter(Visitor<A> visitor, A arg);

    @Override
    public <A> VisitResult accept(Block.Visitor<A> visitor, A arg) {
        return visitor.visitInput(this, arg);
    }

    @Override
    public <A> VisitResult acceptAfter(Block.Visitor<A> visitor, A arg) {
        return visitor.postVisitInput(this, arg);
    }

    /**
     * Named input.
     */
    public abstract static class NamedInput extends Input {

        private final String name;
        private final boolean optional;

        private NamedInput(Input.Builder builder) {
            super(builder);
            this.name = builder.attribute("name");
            this.optional = parseBoolean(builder.attributes().get("optional"));
        }

        /**
         * Get the input name.
         *
         * @return name
         */
        public String name() {
            return name;
        }

        /**
         * Test if this input is optional.
         *
         * @return {@code true} if optional, {@code false} otherwise
         */
        public boolean isOptional() {
            return optional;
        }

        /**
         * Get the default value.
         *
         * @return value
         */
        public abstract Value defaultValue();

        @Override
        public String toString() {
            return "NamedInput{"
                    + "name='" + name + '\''
                    + ", optional=" + optional
                    + '}';
        }
    }

    /**
     * Option input.
     */
    public static final class Option extends Input {

        private final String value;

        private Option(Input.Builder builder) {
            super(builder);
            this.value = builder.attribute("value");
        }

        @Override
        public <A> VisitResult accept(Input.Visitor<A> visitor, A arg) {
            return visitor.visitOption(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Input.Visitor<A> visitor, A arg) {
            return visitor.postVisitOption(this, arg);
        }

        /**
         * Get the option value.
         *
         * @return value
         */
        public String value() {
            return value;
        }
    }

    /**
     * Text input.
     */
    public static final class Text extends NamedInput {

        private final String defaultValue;

        private Text(Input.Builder builder) {
            super(builder);
            this.defaultValue = builder.attributes().get("default");
        }


        @Override
        public Value defaultValue() {
            return defaultValue != null ? Value.create(defaultValue) : null;
        }

        @Override
        public <A> VisitResult accept(Input.Visitor<A> visitor, A arg) {
            return visitor.visitText(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Input.Visitor<A> visitor, A arg) {
            return visitor.postVisitText(this, arg);
        }
    }

    /**
     * Boolean input.
     */
    public static final class Boolean extends NamedInput {

        private final boolean defaultValue;

        private Boolean(Input.Builder builder) {
            super(builder);
            defaultValue = parseBoolean(builder.attributes().get("default"));
        }

        @Override
        public Value defaultValue() {
            return Value.create(defaultValue);
        }

        @Override
        public <A> VisitResult accept(Input.Visitor<A> visitor, A arg) {
            return visitor.visitBoolean(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Input.Visitor<A> visitor, A arg) {
            return visitor.postVisitBoolean(this, arg);
        }
    }

    /**
     * Selection based input.
     */
    public abstract static class Options extends NamedInput {

        private final java.util.List<Option> options;

        private Options(Input.Builder builder) {
            super(builder);
            options = builder.statements().stream()
                             .map(Statement.Builder::build)
                             .filter(Option.class::isInstance)
                             .map(Option.class::cast)
                             .collect(Collectors.toUnmodifiableList());
        }

        /**
         * Get the options.
         *
         * @return options
         */
        public java.util.List<Option> options() {
            return options;
        }
    }

    /**
     * List input.
     */
    public static final class List extends Options {

        private final java.util.List<String> defaultValue;

        private List(Input.Builder builder) {
            super(builder);
            String rawDefault = builder.attributes().get("default");
            if (rawDefault != null) {
                defaultValue = Arrays.stream(rawDefault.split(","))
                                     .map(String::trim)
                                     .collect(toList());
            } else {
                defaultValue = emptyList();
            }
        }

        /**
         * Get the index.
         *
         * @param optionNames option names convert to indexes
         * @return default indexes
         */
        public java.util.List<Integer> optionIndexes(java.util.List<String> optionNames) {
            if (optionNames == null) {
                return java.util.List.of();
            }
            java.util.List<Option> options = options();
            return IntStream.range(0, options.size())
                            .boxed()
                            .filter(i -> optionNames.contains(options.get(0).value))
                            .collect(Collectors.toList());
        }

        /**
         * Parse a response text.
         *
         * @param response response text
         * @return response values
         */
        public java.util.List<String> parseResponse(String response) {
            java.util.List<Option> options = options();
            return Arrays.stream(response.trim().split("\\s+"))
                         .map(Integer::parseInt)
                         .distinct()
                         .map(i -> options.get(i - 1))
                         .map(Input.Option::value)
                         .collect(Collectors.toList());
        }

        @Override
        public Value defaultValue() {
            return Value.create(defaultValue);
        }

        @Override
        public <A> VisitResult accept(Input.Visitor<A> visitor, A arg) {
            return visitor.visitList(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Input.Visitor<A> visitor, A arg) {
            return visitor.postVisitList(this, arg);
        }
    }

    /**
     * Enum input.
     */
    public static final class Enum extends Options {

        private final String defaultValue;

        private Enum(Input.Builder builder) {
            super(builder);
            this.defaultValue = builder.attributes().get("default");
        }

        /**
         * Get the index.
         *
         * @param optionName option name
         * @return index
         */
        public int optionIndex(String optionName) {
            if (optionName == null) {
                return -1;
            }
            java.util.List<Option> options = options();
            return IntStream.range(0, options.size())
                            .boxed()
                            .filter(i -> optionName.equals(options.get(0).value))
                            .findFirst()
                            .orElse(-1);
        }

        @Override
        public Value defaultValue() {
            return defaultValue != null ? Value.create(defaultValue) : null;
        }

        @Override
        public <A> VisitResult accept(Input.Visitor<A> visitor, A arg) {
            return visitor.visitEnum(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Input.Visitor<A> visitor, A arg) {
            return visitor.postVisitEnum(this, arg);
        }
    }

    /**
     * Create a new input block builder.
     *
     * @param scriptPath script path
     * @param position   position
     * @param blockKind  block kind
     * @return builder
     */
    public static Builder builder(Path scriptPath, Position position, Kind blockKind) {
        return new Builder(scriptPath, position, blockKind);
    }

    /**
     * Input block builder.
     */
    public static class Builder extends Block.Builder {

        private String help;

        private Builder(Path scriptPath, Position position, Kind blockKind) {
            super(scriptPath, position, blockKind);
        }

        private boolean doRemove(Noop.Builder b) {
            if (b.kind() == Noop.Kind.HELP) {
                help = b.value();
            }
            return true;
        }

        @Override
        protected Block doBuild() {
            remove(statements(), Noop.Builder.class, this::doRemove);
            Kind kind = kind();
            switch (kind) {
                case BOOLEAN:
                    return new Input.Boolean(this);
                case TEXT:
                    return new Input.Text(this);
                case ENUM:
                    return new Input.Enum(this);
                case LIST:
                    return new Input.List(this);
                case OPTION:
                    return new Input.Option(this);
                default:
                    throw new IllegalArgumentException("Unknown input block kind: " + kind);
            }
        }
    }
}
