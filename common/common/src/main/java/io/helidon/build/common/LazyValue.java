/*
 * Copyright (c) 2019, 2024 Oracle and/or its affiliates.
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

/**
 * A lazy value that defers initialization via a {@link Supplier} until first access.
 *
 * @param <T> The type of the instance.
 */
public class LazyValue<T> {

    private static final VarHandle LATCH;
    private static final VarHandle STATE;

    static {
        try {
            STATE = MethodHandles.lookup().findVarHandle(LazyValue.class, "state", int.class);
            LATCH = MethodHandles.lookup().findVarHandle(LazyValue.class, "latch", CountDownLatch.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Supplier<T> supplier;
    @SuppressWarnings("unused")
    private volatile int state;
    @SuppressWarnings("unused")
    private volatile CountDownLatch latch;
    private T value;

    /**
     * Create a new instance.
     *
     * @param supplier value supplier.
     */
    public LazyValue(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    /**
     * Get the value.
     *
     * @return The value.
     */
    public T get() {
        int stateCopy = state;
        CountDownLatch latchCopy;
        if (stateCopy == 0) {
            // init
            if (STATE.compareAndSet(this, 0, 1)) {
                try {
                    value = supplier.get();
                    state = 2;
                } catch (Throwable th) {
                    state = 0;
                    throw th;
                } finally {
                    latchCopy = latch;
                    if (latchCopy != null) {
                        latchCopy.countDown();
                    }
                }
            }
            stateCopy = state;
        }
        if (stateCopy == 1) {
            // init race
            latchCopy = latch;
            if (latchCopy == null) {
                LATCH.compareAndSet(this, null, new CountDownLatch(1));
                latchCopy = latch;
            }
            try {
                latchCopy.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return value;
    }
}
