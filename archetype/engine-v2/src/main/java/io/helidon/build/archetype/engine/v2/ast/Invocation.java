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

import java.nio.file.Path;
import java.util.Objects;

import io.helidon.build.archetype.engine.v2.ScriptLoader;

/**
 * Invocation.
 */
public abstract class Invocation extends Node {

    private final Kind kind;

    private Invocation(Builder builder) {
        super(builder);
        this.kind = Objects.requireNonNull(builder.kind, "kind is null");
    }

    /**
     * Get the invocation kind.
     *
     * @return kind
     */
    public Kind kind() {
        return kind;
    }

    /**
     * Script invocation.
     */
    public static final class ScriptInvocation extends Invocation {

        private final String src;
        private final String url;

        private ScriptInvocation(Invocation.Builder builder) {
            super(builder);
            this.src = builder.attribute("src", false).asString();
            this.url = builder.attribute("url", false).asString();
            if (src == null && url == null) {
                throw new IllegalArgumentException("Invocation has no 'src' or 'url' attribute");
            }
        }

        /**
         * Get the src.
         *
         * @return src
         */
        public String src() {
            return src;
        }

        /**
         * Get the url.
         *
         * @return url
         */
        public String url() {
            return url;
        }

        @Override
        public <A> VisitResult accept(Invocation.Visitor<A> visitor, A arg) {
            return visitor.visitScriptInvocation(this, arg);
        }

        @Override
        public String toString() {
            return "ScriptInvocation{"
                    + "kind=" + kind()
                    + ", src='" + src + '\''
                    + ", url='" + url + '\''
                    + '}';
        }
    }

    /**
     * Method invocation.
     */
    public static final class MethodInvocation extends Invocation {

        private final String method;

        private MethodInvocation(Invocation.Builder builder) {
            super(builder);
            this.method = builder.attribute("method", false).asString();
        }

        /**
         * Get the method.
         *
         * @return method
         */
        public String method() {
            return method;
        }

        @Override
        public <A> VisitResult accept(Invocation.Visitor<A> visitor, A arg) {
            return visitor.visitMethodInvocation(this, arg);
        }

        @Override
        public String toString() {
            return "MethodInvocation{"
                    + "kind=" + kind()
                    + ", method='" + method + '\''
                    + '}';
        }
    }

    /**
     * Invocation visitor.
     *
     * @param <A> argument type
     */
    public interface Visitor<A> {

        /**
         * Visit an script exec invocation.
         *
         * @param invocation invocation
         * @param arg        visitor argument
         * @return result
         */
        default VisitResult visitScriptInvocation(ScriptInvocation invocation, A arg) {
            return visitAny(invocation, arg);
        }

        /**
         * Visit a method invocation.
         *
         * @param invocation invocation
         * @param arg        visitor argument
         * @return result
         */
        default VisitResult visitMethodInvocation(MethodInvocation invocation, A arg) {
            return visitAny(invocation, arg);
        }

        /**
         * Visit any invocation.
         *
         * @param invocation invocation
         * @param arg        visitor argument
         * @return result
         */
        @SuppressWarnings("unused")
        default VisitResult visitAny(Invocation invocation, A arg) {
            return VisitResult.CONTINUE;
        }
    }

    /**
     * Visit this invocation.
     *
     * @param visitor visitor
     * @param arg     visitor argument
     * @param <A>     visitor argument type
     * @return result
     */
    public abstract <A> VisitResult accept(Visitor<A> visitor, A arg);

    @Override
    public <A> VisitResult accept(Node.Visitor<A> visitor, A arg) {
        return visitor.visitInvocation(this, arg);
    }

    /**
     * Invocation kind.
     */
    public enum Kind {

        /**
         * Exec.
         */
        EXEC,

        /**
         * Source.
         */
        SOURCE,

        /**
         * Method call.
         */
        CALL
    }

    /**
     * Create a new builder.
     *
     * @param loader     script loader
     * @param scriptPath script path
     * @param location   location
     * @param kind       kind
     * @return builder
     */
    public static Builder builder(ScriptLoader loader, Path scriptPath, Location location, Kind kind) {
        return new Builder(loader, scriptPath, location, kind);
    }

    /**
     * Invocation builder.
     */
    public static final class Builder extends Node.Builder<Invocation, Builder> {

        private final Kind kind;

        private Builder(ScriptLoader loader, Path scriptPath, Location location, Kind kind) {
            super(loader, scriptPath, location);
            this.kind = kind;
        }

        @Override
        protected Invocation doBuild() {
            switch (kind) {
                case SOURCE:
                case EXEC:
                    return new ScriptInvocation(this);
                default:
                    return new MethodInvocation(this);
            }
        }
    }
}
