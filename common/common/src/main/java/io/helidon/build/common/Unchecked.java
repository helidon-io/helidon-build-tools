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
package io.helidon.build.common;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility to deal with checked exceptions in lambdas.
 */
public interface Unchecked {

    /**
     * Checked consumer.
     *
     * @param <T> item type
     * @param <E> checked exception type
     */
    interface CheckedConsumer<T, E extends Exception> {
        /**
         * Accept an item.
         *
         * @param item item
         * @throws E if an error occurs
         */
        void accept(T item) throws E;
    }

    /**
     * Checked consumer.
     *
     * @param <T> 1st item type
     * @param <U> 2nd item type
     * @param <E> checked exception type
     */
    interface CheckedBiConsumer<T, U, E extends Exception> {

        /**
         * Accept an item.
         *
         * @param item1 1st item
         * @param item2 2nd item
         * @throws E if an error occurs
         */
        void accept(T item1, U item2) throws E;
    }

    /**
     * Checked consumer.
     *
     * @param <T> input type
     * @param <U> output type
     * @param <E> checked exception type
     */
    interface CheckedFunction<T, U, E extends Exception> {

        /**
         * Accept an item.
         *
         * @param item input item
         * @return U
         * @throws E if an error occurs
         */
        U apply(T item) throws E;
    }

    /**
     * Checked runnable.
     *
     * @param <E> checked exception type
     */
    interface CheckedRunnable<E extends Exception> {

        /**
         * Run.
         *
         * @throws E if an error occurs
         */
        void run() throws E;
    }

    /**
     * Checked supplier.
     *
     * @param <T> supplier type
     * @param <E> checked exception type
     */
    interface CheckedSupplier<T, E extends Exception> {

        /**
         * Get the value.
         *
         * @return T value
         * @throws E if an error occurs
         */
        T get() throws E;
    }

    /**
     * Wrap a {@link CheckedSupplier} into a {@link Supplier}.
     *
     * @param supplier checked supplier
     * @param <T>      supplier type
     * @param <E>      checked exception type
     * @return Supplier
     */
    static <T, E extends Exception> Supplier<T> unchecked(CheckedSupplier<T, E> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception ex) {
                throw new UncheckedException(ex);
            }
        };
    }

    /**
     * Wrap a {@link CheckedRunnable} into a {@link Runnable}.
     *
     * @param runnable checked runnable
     * @param <E>      checked exception type
     * @return Consumer
     */
    static <E extends Exception> Runnable unchecked(CheckedRunnable<E> runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception ex) {
                throw new UncheckedException(ex);
            }
        };
    }

    /**
     * Wrap a {@link CheckedConsumer} into a {@link Consumer}.
     *
     * @param consumer checked consumer
     * @param <T>      item type
     * @param <E>      checked exception type
     * @return Consumer
     */
    static <T, E extends Exception> Consumer<T> unchecked(CheckedConsumer<T, E> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Exception ex) {
                throw new UncheckedException(ex);
            }
        };
    }

    /**
     * Wrap a {@link CheckedBiConsumer} into a {@link BiConsumer}.
     *
     * @param consumer checked consumer
     * @param <T>      1st item type
     * @param <U>      2nd item type
     * @param <E>      checked exception type
     * @return BiConsumer
     */
    static <T, U, E extends Exception> BiConsumer<T, U> unchecked(CheckedBiConsumer<T, U, E> consumer) {
        return (t, u) -> {
            try {
                consumer.accept(t, u);
            } catch (Exception ex) {
                throw new UncheckedException(ex);
            }
        };
    }

    /**
     * Wrap a {@link CheckedBiConsumer} into a {@link BiConsumer}.
     *
     * @param function checked function
     * @param <T>      1st item type
     * @param <U>      2nd item type
     * @param <E>      checked exception type
     * @return Function
     */
    static <T, U, E extends Exception> Function<T, U> unchecked(CheckedFunction<T, U, E> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception ex) {
                throw new UncheckedException(ex);
            }
        };
    }

    /**
     * Unwrap a checked exception wrapped with {@link UncheckedException}.
     *
     * @param ex exception to unwrap
     * @return exception
     */
    static Throwable unwrap(Throwable ex) {
        if (ex instanceof UncheckedException) {
            return ex.getCause();
        }
        return ex;
    }

    /**
     * Unchecked exception.
     */
    class UncheckedException extends RuntimeException {

        /**
         * Create a new unchecked exception.
         *
         * @param cause cause
         */
        public UncheckedException(Exception cause) {
            super(cause);
        }
    }
}
