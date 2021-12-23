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
import java.util.Objects;

/**
 * Invocation.
 */
public final class Invocation extends Node {

    private final Kind kind;
    private final String src;
    private final String url;

    private Invocation(Builder builder) {
        super(builder);
        this.kind = Objects.requireNonNull(builder.kind, "kind is null");
        this.src = builder.attribute("src", false);
        this.url = builder.attribute("url", false);
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

    /**
     * Get the invocation kind.
     *
     * @return kind
     */
    public Kind kind() {
        return kind;
    }

    @Override
    public <A> VisitResult accept(Node.Visitor<A> visitor, A arg) {
        return visitor.visitInvocation(this, arg);
    }

    @Override
    public String toString() {
        return "Invocation{"
                + "kind=" + kind
                + ", src='" + src + '\''
                + '}';
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
        SOURCE
    }

    /**
     * Create a new builder.
     *
     * @param scriptPath script path
     * @param position   position
     * @param kind       kind
     * @return builder
     */
    public static Builder builder(Path scriptPath, Position position, Kind kind) {
        return new Builder(scriptPath, position, kind);
    }

    /**
     * Invocation builder.
     */
    public static final class Builder extends Node.Builder<Invocation, Builder> {

        private final Kind kind;

        private Builder(Path scriptPath, Position position, Kind kind) {
            super(scriptPath, position);
            this.kind = kind;
        }

        @Override
        protected Invocation doBuild() {
            return new Invocation(this);
        }
    }
}
