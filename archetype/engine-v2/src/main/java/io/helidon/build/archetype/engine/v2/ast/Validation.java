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

package io.helidon.build.archetype.engine.v2.ast;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validation block.
 */
public class Validation extends Block {

    private final String id;
    private final String description;

    private Validation(Builder builder) {
        super(builder);
        this.id = builder.attribute("id", true).asString();
        this.description = builder.attribute("description", false).asString();
    }

    @Override
    public <A> VisitResult accept(Block.Visitor<A> visitor, A arg) {
        return visitor.visitValidation(this, arg);
    }

    @Override
    public <A> VisitResult acceptAfter(Block.Visitor<A> visitor, A arg) {
        return visitor.postVisitValidation(this, arg);
    }

    /**
     * Visit this block.
     *
     * @param visitor validation visitor
     * @param arg     visitor argument
     * @param <A>     visitor argument type
     * @return visit result
     */
    public <A> VisitResult accept(Validation.Visitor<A> visitor, A arg) {
        return visitor.visitValidation(this, arg);
    }

    /**
     * Visit this block after traversing the nested nodes.
     *
     * @param visitor validation visitor
     * @param arg     visitor argument
     * @param <A>     visitor argument type
     * @return visit result
     */
    public <A> VisitResult acceptAfter(Validation.Visitor<A> visitor, A arg) {
        return visitor.postVisitValidation(this, arg);
    }

    /**
     * Get the id.
     *
     * @return id
     */
    public String id() {
        return id;
    }

    /**
     * Get description.
     *
     * @return description
     */
    public String description() {
        return description;
    }

    /**
     * Validation visitor.
     *
     * @param <A> argument type
     */
    public interface Visitor<A> {

        /**
         * Visit a validation block.
         *
         * @param validation validation
         * @param arg        visitor argument
         * @return result
         */
        default VisitResult visitValidation(Validation validation, A arg) {
            return visitAny(validation, arg);
        }

        /**
         * Visit a validation block after traversing the nested nodes.
         *
         * @param validation validation
         * @param arg        visitor argument
         * @return result
         */
        default VisitResult postVisitValidation(Validation validation, A arg) {
            return visitAny(validation, arg);
        }

        /**
         * Visit a regex block.
         *
         * @param regex regular expression
         * @param arg   visitor argument
         * @return  result
         */
        default VisitResult visitRegex(Regex regex, A arg) {
            return visitAny(regex, arg);
        }

        /**
         * Visit a regex block after traversing the nested nodes.
         *
         * @param regex regular expression
         * @param arg   visitor argument
         * @return  result
         */
        default VisitResult postVisitRegex(Regex regex, A arg) {
            return visitAny(regex, arg);
        }

        /**
         * Visit any block.
         *
         * @param block block
         * @param arg   arg
         * @return visitResult
         */
        @SuppressWarnings("unused")
        default VisitResult visitAny(Block block, A arg) {
            return VisitResult.CONTINUE;
        }
    }

    /**
     * Regular expression block.
     */
    public static final class Regex extends Block {

        private final Pattern pattern;

        private Regex(Builder builder) {
            super(builder);
            String value = Objects.requireNonNull(builder.value(), "Regex must not be null");
            this.pattern = Pattern.compile(value);
        }

        @Override
        public <A> VisitResult accept(Block.Visitor<A> visitor, A arg) {
            return visitor.visitRegex(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Block.Visitor<A> visitor, A arg) {
            return visitor.postVisitRegex(this, arg);
        }

        /**
         * Visit this block.
         *
         * @param visitor validation visitor
         * @param arg     visitor argument
         * @param <A>     visitor argument type
         * @return visit result
         */
        public <A> VisitResult accept(Validation.Visitor<A> visitor, A arg) {
            return visitor.visitRegex(this, arg);
        }

        /**
         * Visit this block after traversing the nested nodes.
         *
         * @param visitor validation visitor
         * @param arg     visitor argument
         * @param <A>     visitor argument type
         * @return visit result
         */
        public <A> VisitResult acceptAfter(Validation.Visitor<A> visitor, A arg) {
            return visitor.postVisitRegex(this, arg);
        }

        /**
         * Get the regex.
         *
         * @return regex
         */
        public Pattern pattern() {
            return pattern;
        }
    }

    /**
     * Create a new validation block builder.
     *
     * @param info      builder info
     * @param blockKind block kind
     * @return builder
     */
    public static Builder builder(BuilderInfo info, Kind blockKind) {
        return new Builder(info, blockKind);
    }

    /**
     * Validation block builder.
     */
    public static class Builder extends Block.Builder {

        private Builder(BuilderInfo info, Kind blockKind) {
            super(info, blockKind);
        }

        @Override
        protected Block doBuild() {
            Kind kind = kind();
            switch (kind) {
                case VALIDATION:
                    return new Validation(this);
                case REGEX:
                    return new Regex(this);
                default:
                    throw new IllegalArgumentException("Unknown validation block kind: " + kind);
            }
        }
    }
}
