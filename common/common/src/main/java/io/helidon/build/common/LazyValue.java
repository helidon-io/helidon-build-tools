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
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * A lazy value that defers initialization via a {@link Supplier} until first access.
 *
 * @param <T> The type of the instance.
 */
public class LazyValue<T> implements Supplier<T> {

    private static final VarHandle LOCK;
    private static final VarHandle STATE;

    static {
        try {
            LOCK = MethodHandles.lookup().findVarHandle(LazyValue.class, "lock", Semaphore.class);
            STATE = MethodHandles.lookup().findVarHandle(LazyValue.class, "state", int.class);
        } catch (Exception e) {
            throw new Error("Unable to obtain VarHandle's", e);
        }
    }

    private T value;
    private Supplier<T> delegate;

    @SuppressWarnings("unused")
    private volatile Semaphore lock;
    private volatile int state;

    /**
     * Create a new loaded instance.
     *
     * @param value value
     */
    public LazyValue(T value) {
        this.value = value;
        this.state = 2;
    }

    /**
     * Create a new instance.
     *
     * @param supplier value supplier.
     */
    public LazyValue(Supplier<T> supplier) {
        this.delegate = supplier;
    }

    /**
     * Get the value.
     *
     * @return The value.
     */
    @Override
    public T get() {
        int stateCopy = state;
        if (stateCopy == 2) {
            return value;
        }
        Semaphore lockCopy = lock;
        while (stateCopy != 2 && !STATE.compareAndSet(this, 0, 1)) {
            if (lockCopy == null) {
                LOCK.compareAndSet(this, null, new Semaphore(0));
                lockCopy = lock;
            }
            stateCopy = state;
            if (stateCopy == 1) {
                lockCopy.acquireUninterruptibly();
                stateCopy = state;
            }
        }

        try {
            if (stateCopy == 2) {
                return value;
            }
            stateCopy = 0;
            value = delegate.get();
            delegate = null;
            stateCopy = 2;
            state = 2;
        } finally {
            if (stateCopy == 0) {
                state = 0;
            }
            lockCopy = lock;
            if (lockCopy != null) {
                lockCopy.release();
            }
        }
        return value;
    }
}
