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
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Statement.
 */
public abstract class Statement extends Node {

    /**
     * Create a new statement.
     *
     * @param builder builder
     */
    protected Statement(Builder<?, ?> builder) {
        super(builder);
    }

    /**
     * Create a new statement.
     *
     * @param scriptPath script path
     * @param position   position
     */
    protected Statement(Path scriptPath, Position position) {
        super(scriptPath, position);
    }

    /**
     * Remove builders from the list of statement builders.
     *
     * @param statements list of statement builders
     * @param type       class used to match the builders to remove
     * @param function   function invoked to control removal
     */
    static <T> void remove(List<Builder<? extends Statement, ?>> statements,
                           Class<T> type,
                           Function<T, Boolean> function) {

        Iterator<Builder<?, ?>> it = statements.iterator();
        while (it.hasNext()) {
            Statement.Builder<?, ?> b = it.next();
            if (type.isInstance(b)) {
                T tb = type.cast(b);
                if (function.apply(tb)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Base builder class for statement types.
     *
     * @param <T> statement sub-type
     * @param <U> builder sub-type
     */
    public abstract static class Builder<T extends Statement, U extends Builder<T, U>> extends Node.Builder<T, U> {

        /**
         * Create a new statement builder.
         *
         * @param scriptPath script path
         * @param position   position
         */
        protected Builder(Path scriptPath, Position position) {
            super(scriptPath, position);
        }
    }
}
