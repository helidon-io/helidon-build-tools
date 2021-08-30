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

import java.util.LinkedList;
import java.util.Objects;

import io.helidon.build.archetype.engine.v2.interpreter.Visitable;
import io.helidon.build.archetype.engine.v2.interpreter.Visitor;

/**
 * Archetype transformation in {@link Output}.
 */
public class Transformation implements Visitable {

    private final String id;
    private final LinkedList<Replacement> replacements;

    Transformation(String id) {
        this.id = Objects.requireNonNull(id, "id is null");
        this.replacements = new LinkedList<>();
    }

    /**
     * Get the transformation id.
     *
     * @return transformation id, never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Get the replacements.
     *
     * @return list of replacement, never {@code null}
     */
    public LinkedList<Replacement> replacements() {
        return replacements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transformation that = (Transformation) o;
        return id.equals(that.id)
                && replacements.equals(that.replacements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, replacements);
    }

    @Override
    public String toString() {
        return "Transformation{"
                + "id='" + id + '\''
                + ", replacements=" + replacements
                + '}';
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }
}
