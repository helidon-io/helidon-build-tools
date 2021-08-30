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

package io.helidon.build.archetype.engine.v2.descriptor;

import java.util.Objects;

import io.helidon.build.archetype.engine.v2.interpreter.Visitor;

/**
 * Archetype boolean in {@link Context} nodes.
 */
public class ContextBoolean extends ContextNode {

    private boolean bool;

    protected ContextBoolean(String path) {
        super(path);
    }

    /**
     * Get the boolean value.
     *
     * @return boolean
     */
    public boolean bool() {
        return bool;
    }

    /**
     * Set the boolean value.
     *
     * @param bool boolean to be set
     */
    void bool(boolean bool) {
        this.bool = bool;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ContextBoolean b = (ContextBoolean) o;
        return bool == b.bool;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bool);
    }

    @Override
    public String toString() {
        return "ContextBoolean{"
                + "path=" + path()
                + ", bool=" + bool()
                + '}';
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }
}
