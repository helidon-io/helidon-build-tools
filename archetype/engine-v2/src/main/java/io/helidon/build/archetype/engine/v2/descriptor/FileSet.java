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

import io.helidon.build.archetype.engine.v2.interpreter.Visitable;
import io.helidon.build.archetype.engine.v2.interpreter.Visitor;

/**
 * Archetype file in {@link Output} archetype.
 */
public class FileSet extends Conditional implements Visitable {

    private final String source;
    private final String target;

    FileSet(String source, String target, String ifProperties) {
        super(ifProperties);
        this.source = source;
        this.target = target;
    }

    /**
     * Get the source.
     *
     * @return source
     */
    public String source() {
        return source;
    }

    /**
     * Get the target.
     *
     * @return target
     */
    public String target() {
        return target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FileSet that = (FileSet) o;
        return source.equals(that.source)
                && target.equals(that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), source, target);
    }

    @Override
    public String toString() {
        return "File{"
                + "source=" + source()
                + ", target=" + target()
                + ", if=" + ifProperties()
                + '}';
    }

    @Override
    public <A> void accept(Visitor<A> visitor, A arg) {
        visitor.visit(this, arg);
    }
}
