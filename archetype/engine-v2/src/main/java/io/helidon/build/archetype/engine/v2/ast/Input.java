/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.helidon.build.archetype.engine.v2.InvalidInputException;

import static java.util.Collections.emptyList;

/**
 * Input block.
 */
public abstract class Input extends Block {

    private final String name;
    private final String description;
    private final String help;
    private final String prompt;

    private Input(Input.Builder builder) {
        super(builder);
        name = builder.attribute("name", false).asString();
        description = builder.attribute("description", false).asString();
        prompt = builder.attribute("prompt", false).asString();
        help = builder.attribute("help", false).asString();
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
     * Get the input description.
     *
     * @return description
     */
    public String description() {
        return description;
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
         * Visit a boolean input after traversing the nested nodes.
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
         * Visit a text input after traversing the nested nodes.
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
         * Visit an option after traversing the nested nodes.
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
         * Visit an enum input after traversing the nested nodes.
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
         * Visit a list input after traversing the nested nodes.
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
         * Visit any input after traversing the nested nodes.
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
     * Visit this input after traversing the nested nodes.
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
     * Declared input.
     */
    public abstract static class DeclaredInput extends Input {

        private final String id;
        private final boolean optional;
        private final boolean global;

        private DeclaredInput(Input.Builder builder) {
            super(builder);
            this.id = builder.attribute("id", true).asString();
            this.optional = builder.attribute("optional", false).asBoolean();
            this.global = builder.attribute("global", false).asBoolean();
        }

        /**
         * Get the input id.
         *
         * @return id
         */
        public String id() {
            return id;
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
         * Test if this input is global.
         *
         * @return {@code true} if global, {@code false} otherwise
         */
        public boolean isGlobal() {
            return global;
        }

        /**
         * Get the default value.
         *
         * @return value
         */
        public abstract Value defaultValue();

        /**
         * Validate the given value against this input.
         *
         * @param value value
         * @param path  input path
         * @throws InvalidInputException if the value is invalid
         */
        @SuppressWarnings("unused")
        public void validate(Value value, String path) throws InvalidInputException {
        }

        /**
         * Compute the visit result for the given value.
         *
         * @param value value
         * @return visit result
         */
        public VisitResult visitValue(Value value) {
            return VisitResult.CONTINUE;
        }

        /**
         * Compute the visit result for the given option.
         *
         * @param value  current value
         * @param option option
         * @return visit result
         */
        public VisitResult visitOption(Value value, Option option) {
            return VisitResult.CONTINUE;
        }

        /**
         * Normalize the given value.
         *
         * @param value value
         * @return normalized value
         */
        public String normalizeOptionValue(String value) {
            return value;
        }

        @Override
        public String toString() {
            return "DeclaredInput{"
                    + "id='" + id + '\''
                    + ", optional=" + optional
                    + ", global=" + global
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
            this.value = builder.attribute("value", true).asString();
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

        @Override
        public String toString() {
            return "InputOption{"
                    + "value='" + value + '\''
                    + '}';
        }
    }

    /**
     * Text input.
     */
    public static final class Text extends DeclaredInput {

        private final String defaultValue;

        private Text(Input.Builder builder) {
            super(builder);
            this.defaultValue = builder.attribute("default", false).asString();
        }


        @Override
        public Value defaultValue() {
            return Value.create(defaultValue);
        }

        @Override
        public <A> VisitResult accept(Input.Visitor<A> visitor, A arg) {
            return visitor.visitText(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Input.Visitor<A> visitor, A arg) {
            return visitor.postVisitText(this, arg);
        }

        @Override
        public String toString() {
            return "InputText{"
                    + "id='" + id() + '\''
                    + ", name='" + name() + '\''
                    + ", optional=" + isOptional()
                    + ", global=" + isGlobal()
                    + ", defaultValue='" + defaultValue + '\''
                    + '}';
        }
    }

    /**
     * Boolean input.
     */
    public static final class Boolean extends DeclaredInput {

        private final boolean defaultValue;

        private Boolean(Input.Builder builder) {
            super(builder);
            defaultValue = builder.attribute("default", false).asBoolean();
        }

        @Override
        public Value defaultValue() {
            return Value.create(defaultValue);
        }

        @Override
        public VisitResult visitValue(Value value) {
            return value.asBoolean() ? VisitResult.CONTINUE : VisitResult.SKIP_SUBTREE;
        }

        @Override
        public <A> VisitResult accept(Input.Visitor<A> visitor, A arg) {
            return visitor.visitBoolean(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Input.Visitor<A> visitor, A arg) {
            return visitor.postVisitBoolean(this, arg);
        }

        /**
         * Returns the boolean value of the given input.
         *
         * @param input The input.
         * @return {@code true} if input is "y", "yes" or "true".
         */
        public static boolean valueOf(String input) {
            return valueOf(input, false);
        }

        /**
         * Returns the boolean value of the given input.
         *
         * @param input  The input.
         * @param strict {@code true} if an exception should be thrown if the value is not one of
         *               "y", "yes", "true", "n", "no", or "false". If {@code false}, any unknown value is treated
         *               as false.
         * @return The boolean value.
         */
        public static boolean valueOf(String input, boolean strict) {
            switch (input.trim().toLowerCase()) {
                case "y":
                case "yes":
                case "true":
                    return true;
                case "n":
                case "no":
                case "false":
                    return false;
                default:
                    if (strict) {
                        throw new IllegalArgumentException(input + " is not boolean");
                    }
                    return false;
            }
        }

        /**
         * Return a string representation of the given boolean value.
         *
         * @param value value
         * @return string representation or {@code null} if the given value is {@code null}
         */
        public static String asString(Value value) {
            return value != null ? value.asBoolean() ? "yes" : "no" : null;
        }

        @Override
        public String toString() {
            return "InputBoolean{"
                    + "id='" + id() + '\''
                    + ", name='" + name() + '\''
                    + ", optional=" + isOptional()
                    + ", global=" + isGlobal()
                    + ", defaultValue='" + defaultValue + '\''
                    + '}';
        }
    }

    /**
     * Selection based input.
     */
    public abstract static class Options extends DeclaredInput {

        private Options(Input.Builder builder) {
            super(builder);
        }

        /**
         * Get the options.
         *
         * @return options
         */
        public java.util.List<Option> options() {
            return options(n -> true);
        }

        /**
         * Get the options.
         *
         * @param filter node predicate
         * @return options
         */
        public java.util.List<Option> options(Predicate<Node> filter) {
            return children(filter, Option.class).collect(Collectors.toList());
        }

        @Override
        public String normalizeOptionValue(String value) {
            return value.toLowerCase();
        }
    }

    /**
     * List input.
     */
    public static final class List extends Options {

        private final String[] defaultValue;

        private List(Input.Builder builder) {
            super(builder);
            defaultValue = builder.attribute("default", ValueTypes.STRING_LIST, emptyList())
                                  .toArray(new String[0]);
        }

        @Override
        public Value defaultValue() {
            return Value.create(Arrays.asList(defaultValue));
        }

        @Override
        public VisitResult visitOption(Value value, Option option) {
            if (containsIgnoreCase(value.asList(), option.value)) {
                return VisitResult.CONTINUE;
            }
            return VisitResult.SKIP_SUBTREE;
        }

        @Override
        public <A> VisitResult accept(Input.Visitor<A> visitor, A arg) {
            return visitor.visitList(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Input.Visitor<A> visitor, A arg) {
            return visitor.postVisitList(this, arg);
        }

        @Override
        public String toString() {
            return "InputList{"
                    + "id='" + id() + '\''
                    + ", name='" + name() + '\''
                    + ", optional=" + isOptional()
                    + ", global=" + isGlobal()
                    + ", defaultValue='" + Arrays.toString(defaultValue) + '\''
                    + '}';
        }

        private static boolean containsIgnoreCase(Collection<String> values, String expected) {
            for (String optionName : values) {
                if (optionName.equalsIgnoreCase(expected)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Enum input.
     */
    public static final class Enum extends Options {

        private final String defaultValue;

        private Enum(Input.Builder builder) {
            super(builder);
            this.defaultValue = builder.attribute("default", false).asString();
        }

        /**
         * Get the index.
         *
         * @param optionName option name
         * @param options    the options to process
         * @return index
         */
        public static int optionIndex(String optionName, java.util.List<Option> options) {
            if (optionName == null) {
                return -1;
            }
            return IntStream.range(0, options.size())
                            .boxed()
                            .filter(i -> optionName.equalsIgnoreCase(options.get(i).value))
                            .findFirst()
                            .orElse(-1);
        }

        @Override
        public Value defaultValue() {
            return Value.create(defaultValue);
        }

        @Override
        public void validate(Value value, String path) throws InvalidInputException {
            String option = value.asString();
            if (optionIndex(option, options(n -> true)) == -1) {
                throw new InvalidInputException(option, path);
            }
        }

        @Override
        public VisitResult visitOption(Value value, Option option) {
            if (value.asString().equalsIgnoreCase(option.value())) {
                return VisitResult.SKIP_SIBLINGS;
            }
            return VisitResult.SKIP_SUBTREE;
        }

        @Override
        public <A> VisitResult accept(Input.Visitor<A> visitor, A arg) {
            return visitor.visitEnum(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Input.Visitor<A> visitor, A arg) {
            return visitor.postVisitEnum(this, arg);
        }

        @Override
        public String toString() {
            return "InputEnum{"
                    + "id='" + id() + '\''
                    + ", name='" + name() + '\''
                    + ", optional=" + isOptional()
                    + ", global=" + isGlobal()
                    + ", defaultValue='" + defaultValue + '\''
                    + '}';
        }
    }

    /**
     * Create a new input block builder.
     *
     * @param info      builder info
     * @param blockKind block kind
     * @return builder
     */
    public static Builder builder(BuilderInfo info, Kind blockKind) {
        return new Builder(info, blockKind);
    }

    /**
     * Input block builder.
     */
    public static class Builder extends Block.Builder {

        private Builder(BuilderInfo info, Kind blockKind) {
            super(info, blockKind);
        }

        @Override
        protected Block doBuild() {
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
