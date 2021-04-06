/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates.
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

package io.helidon.build.common;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * An instance cache that defers creation until first access.
 *
 * @param <T> The type of the instance.
 */
public class Instance<T> {
    private final AtomicReference<T> cache;
    private final Supplier<T> supplier;

    /**
     * Constructor.
     *
     * @param supplier The instance supplier.
     */
    public Instance(Supplier<T> supplier) {
        this.cache = new AtomicReference<>();
        this.supplier = supplier;
    }

    /**
     * Returns the instance, creating it if required.
     *
     * @return The instance.
     */
    public T instance() {
        T result = cache.get();
        if (result == null) {
            synchronized (cache) {
                result = cache.get();
                if (result == null) {
                    result = supplier.get();
                    cache.set(result);
                }
            }
        }
        return result;
    }
}
