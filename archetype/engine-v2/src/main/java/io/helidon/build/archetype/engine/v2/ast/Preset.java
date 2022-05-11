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

import java.util.List;

/**
 * Preset.
 */
public final class Preset extends DeclaredValue {

    private Preset(DeclaredValue.Builder builder, List<String> values) {
        super(builder, values);
    }

    @Override
    public String toString() {
        return "Preset{"
                + "path='" + path() + '\''
                + ", value=" + value() + '\''
                + '}';
    }

    @Override
    public <A> VisitResult accept(Block.Visitor<A> visitor, A arg) {
        return visitor.visitPreset(this, arg);
    }

    /**
     * Create a new builder.
     *
     * @param info builder info
     * @param kind kind
     * @return builder
     */
    public static Builder builder(BuilderInfo info, Block.Kind kind) {
        return new Builder(info, kind);
    }

    /**
     * Preset builder.
     */
    public static final class Builder extends DeclaredValue.Builder {

        private Builder(BuilderInfo info, Block.Kind kind) {
            super(info, kind);
        }

        @Override
        protected DeclaredValue doBuild(DeclaredValue.Builder builder, List<String> values) {
            return new Preset(builder, values);
        }
    }
}
