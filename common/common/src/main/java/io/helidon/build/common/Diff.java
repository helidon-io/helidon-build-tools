/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
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
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Diff.
 *
 * @param <T> element type
 */
public class Diff<T> {

    private final int orig;
    private final int actual;
    private final T element;

    /**
     * Create a diff with no element.
     *
     * @param orig    original index
     * @param actual  actual index
     * @param element element, may be {@code null}
     */
    public Diff(int orig, int actual, T element) {
        this.orig = orig;
        this.actual = actual;
        this.element = element;
    }

    /**
     * Create a diff with no element.
     *
     * @param orig   original index
     * @param actual actual index
     */
    public Diff(int orig, int actual) {
        this(orig, actual, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Diff)) {
            return false;
        }
        Diff<?> diff = (Diff<?>) o;
        return orig == diff.orig && actual == diff.actual && Objects.equals(element, diff.element);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orig, actual, element);
    }

    /**
     * Get the actual index.
     *
     * @return actual index
     */
    public int actual() {
        return actual;
    }

    /**
     * Get the original index.
     *
     * @return original index
     */
    public int orig() {
        return orig;
    }

    /**
     * Get the element.
     *
     * @return element, may be {@code null}
     */
    public T element() {
        return element;
    }

    /**
     * Test if this diff represents an addition.
     *
     * @return {@code true} if this is an addition, {@code false} otherwise
     */
    public boolean isAdd() {
        return orig < 0;
    }

    /**
     * Test if this diff represents a removal.
     *
     * @return {@code true} if this is a removal, {@code false} otherwise
     */
    public boolean isRemove() {
        return actual < 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isAdd()) {
            sb.append("ADD(")
                    .append(actual);
        } else if (isRemove()) {
            sb.append("DEL(")
                    .append(orig);
        } else {
            sb.append("MOVE(")
                    .append(orig)
                    .append(',')
                    .append(actual);
        }
        if (element != null) {
            sb.append(',').append(element);
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * Apply a patch.
     *
     * @param diffs  patch to apply
     * @param target list to update
     * @param <T>    diff type
     */
    public static <T> void apply(List<Diff<T>> diffs, List<T> target) {
        apply(diffs, target, Function.identity());
    }

    /**
     * Apply a patch.
     *
     * @param diffs  patch to apply
     * @param target list to update
     * @param mapper function to map from the diff type to the target type
     * @param <T>    diff type
     * @param <U>    target type
     */
    public static <T, U> void apply(List<Diff<T>> diffs, List<U> target, Function<T, U> mapper) {
        for (var diff : diffs) {
            if (diff.isAdd()) {
                target.add(diff.actual, mapper.apply(diff.element));
            } else if (diff.isRemove()) {
                target.remove(diff.orig);
            } else {
                var moved = target.remove(diff.orig);
                target.add(diff.actual, moved);
            }
        }
    }

    /**
     * Compute differences.
     *
     * @param orig   original list
     * @param actual actual list
     * @param <T>    element type
     * @return differences
     */
    public static <T> List<Diff<T>> diff(List<T> orig, List<T> actual) {
        var obits = new BitSet(orig.size());
        var abits = new BitSet(actual.size());
        var stack = new ArrayDeque<Diff<T>>();

        // 1st pass
        // iterate over actual to build usages and indexes
        for (var it = actual.listIterator(); it.hasNext();) {
            var e = it.next();
            for (int i = obits.nextClearBit(0); i < orig.size(); i = obits.nextClearBit(i + 1)) {
                if (orig.get(i).equals(e)) {
                    obits.set(i);
                    abits.set(it.previousIndex());
                    stack.add(new Diff<>(i, it.previousIndex(), e));
                    break;
                }
            }
        }

        var diffs = new ArrayList<Diff<T>>();

        // 2nd pass
        // iterate over the union to build the diffs
        int offset = 0;
        var origIt = orig.listIterator();
        var actualIt = actual.listIterator();
        while (origIt.hasNext() || actualIt.hasNext()) {
            boolean removed = false;
            if (origIt.hasNext()) {
                var e = origIt.next();
                int pos = origIt.previousIndex();
                if (!obits.get(pos)) {
                    int apos = Math.max(0, pos - offset);
                    diffs.add(new Diff<>(apos, -1, e));
                    removed = true;
                    offset++;
                }
            }
            if (actualIt.hasNext()) {
                var e = actualIt.next();
                int pos = actualIt.previousIndex();
                if (!abits.get(pos)) {
                    diffs.add(new Diff<>(-1, pos, e));
                    if (removed) {
                        offset--;
                    }
                } else if (!removed) {
                    var moved = stack.pop();
                    int apos = Math.max(0, pos - offset);
                    if (apos != moved.actual) {
                        diffs.add(moved);
                    }
                }
            }
        }
        return diffs;
    }

    /**
     * Mapper function to map a diff element.
     *
     * @param <T> diff type
     * @param <U> target type
     */
    @FunctionalInterface
    public interface Mapper<T, U> {
        /**
         * Map a diff.
         *
         * @param orig    original index
         * @param actual  actual index
         * @param element element
         * @return mapped element
         */
        U apply(int orig, int actual, T element);
    }
}
