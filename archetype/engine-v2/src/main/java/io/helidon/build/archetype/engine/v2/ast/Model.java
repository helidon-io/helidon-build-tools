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

/**
 * Model block.
 */
public abstract class Model extends Block {

    private Model(Model.Builder builder) {
        super(builder);
    }

    /**
     * Model visitor.
     * @param <A> argument type
     */
    public interface Visitor<A> {

        /**
         * Visit a list model.
         *
         * @param list list
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult visitList(List list, A arg) {
            return visitAny(list, arg);
        }

        /**
         * Visit a list after traversing the nested statements.
         *
         * @param list list
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult postVisitList(List list, A arg) {
            return postVisitAny(list, arg);
        }

        /**
         * Visit a map model.
         *
         * @param map map
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult visitMap(Map map, A arg) {
            return visitAny(map, arg);
        }

        /**
         * Visit a map after traversing the nested statements.
         *
         * @param map map
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult postVisitMap(Map map, A arg) {
            return postVisitAny(map, arg);
        }

        /**
         * Visit a value.
         *
         * @param value value
         * @param arg   visitor argument
         * @return result
         */
        default VisitResult visitValue(Value value, A arg) {
            return visitAny(value, arg);
        }

        /**
         * Visit any model.
         *
         * @param model model
         * @param arg   visitor argument
         * @return result
         */
        @SuppressWarnings("unused")
        default VisitResult visitAny(Model model, A arg) {
            return VisitResult.CONTINUE;
        }

        /**
         * Visit any model after traversing the nested statements.
         *
         * @param model model
         * @param arg   visitor argument
         * @return result
         */
        @SuppressWarnings("unused")
        default VisitResult postVisitAny(Model model, A arg) {
            return VisitResult.CONTINUE;
        }
    }

    /**
     * Visit this model.
     *
     * @param visitor visitor
     * @param arg visitor argument
     * @param <A> visitor argument type
     * @return result
     */
    public abstract <A> VisitResult accept(Visitor<A>  visitor, A arg);

    /**
     * Visit this model after traversing the nested statements.
     *
     * @param visitor visitor
     * @param arg visitor argument
     * @param <A> visitor argument type
     * @return result
     */
    public <A> VisitResult acceptAfter(Visitor<A>  visitor, A arg) {
        return VisitResult.CONTINUE;
    }

    @Override
    public <A> VisitResult accept(Block.Visitor<A> visitor, A arg) {
        return visitor.visitModel(this, arg);
    }

    @Override
    public <A> VisitResult acceptAfter(Block.Visitor<A> visitor, A arg) {
        return visitor.postVisitModel(this, arg);
    }

    /**
     * List model.
     */
    public static final class List extends MergeableModel {

        private List(Model.Builder builder) {
            super(builder);
        }

        @Override
        public <A> VisitResult accept(Model.Visitor<A> visitor, A arg) {
            return visitor.visitList(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Model.Visitor<A> visitor, A arg) {
            return visitor.postVisitList(this, arg);
        }
    }

    /**
     * Mergeable model.
     */
    public abstract static class MergeableModel extends Model {

        private final String key;
        private final int order;

        private MergeableModel(Model.Builder builder) {
            super(builder);
            String rawOrder = builder.attributes().get("order");
            this.order = rawOrder != null ? Integer.parseInt(rawOrder) : 100;
            this.key = builder.attributes().get("key");
        }

        /**
         * Get the key.
         *
         * @return key
         */
        public String key() {
            return key;
        }

        /**
         * Get the order.
         *
         * @return order
         */
        public int order() {
            return order;
        }
    }

    /**
     * Map model.
     */
    public static final class Map extends MergeableModel {

        private Map(Model.Builder builder) {
            super(builder);
        }

        @Override
        public <A> VisitResult accept(Model.Visitor<A> visitor, A arg) {
            return visitor.visitMap(this, arg);
        }

        @Override
        public <A> VisitResult acceptAfter(Model.Visitor<A> visitor, A arg) {
            return visitor.postVisitMap(this, arg);
        }
    }

    /**
     * Model value.
     */
    public static final class Value extends MergeableModel {

        private final String value;

        private Value(Model.Builder builder) {
            super(builder);
            this.value = builder.value;
            // TODO file
            // TODO template engine
        }

        /**
         * Get the value.
         *
         * @return value
         */
        public String value() {
            return value;
        }

        @Override
        public <A> VisitResult accept(Model.Visitor<A> visitor, A arg) {
            return visitor.visitValue(this, arg);
        }
    }

    /**
     * Create a new model block builder.
     *
     * @param scriptPath script path
     * @param position   position
     * @param kind       block kind
     * @return builder
     */
    public static Builder builder(Path scriptPath, Position position, Kind kind) {
        return new Builder(scriptPath, position, kind);
    }

    /**
     * Model block builder.
     */
    public static class Builder extends Block.Builder {

        private String value;

        private Builder(Path scriptPath, Position position, Kind kind) {
            super(scriptPath, position, kind);
        }

        @Override
        public Block.Builder value(String value) {
            this.value = value;
            return this;
        }

        private static Block.Builder valueBuilder(Noop.Builder noop) {
            return new Builder(noop.scriptPath(), noop.position(), Kind.VALUE)
                    .value(noop.value())
                    .attributes(noop.attributes());
        }

        @Override
        protected Block doBuild() {
            statements().replaceAll(b -> {
                if (b instanceof Noop.Builder) {
                    return valueBuilder((Noop.Builder) b);
                } else if (b instanceof Condition.Builder) {
                    Condition.Builder c = (Condition.Builder) b;
                    Statement.Builder<?, ?> then = c.then();
                    if (then instanceof Noop.Builder) {
                        c.then(valueBuilder((Noop.Builder) then));
                    }
                }
                return b;
            });
            Kind kind = kind();
            switch (kind) {
                case MAP:
                    return new Map(this);
                case LIST:
                    return new List(this);
                case VALUE:
                    return new Value(this);
                default:
                    throw new IllegalArgumentException("Unknown model block: " + kind);
            }
        }
    }
}
