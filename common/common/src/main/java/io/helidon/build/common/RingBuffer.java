/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;

/**
 * Ring buffer backed by a {@link Queue}.
 *
 * @param <E> element type
 */
public final class RingBuffer<E> implements Iterable<E> {

    private final Queue<E> queue;
    private final int size;

    /**
     * Create a new instance.
     *
     * @param size size
     */
    public RingBuffer(int size) {
        this.queue = new ArrayDeque<>(size);
        this.size = size;
    }

    /**
     * Add an element.
     *
     * @param e element to insert
     * @return {@code true} (as specified by {@link java.util.Collection#add})
     */
    public boolean add(E e) {
        if (queue.size() == size) {
            queue.poll();
        }
        return queue.add(e);
    }

    @Override
    public int hashCode() {
        return queue.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RingBuffer<?> that = (RingBuffer<?>) o;
        return size == that.size && Objects.equals(queue, that.queue);
    }

    @Override
    public String toString() {
        return queue.toString();
    }

    @Override
    public Iterator<E> iterator() {
        return queue.iterator();
    }
}
