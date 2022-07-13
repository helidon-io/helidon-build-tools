/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Permutation utility.
 */
public final class Permutations {

    private Permutations() {
        // cannot be instantiated
    }

    /**
     * An iterator that computes the non-repetitive permutations of the given permutations.
     *
     * @param <T> element type
     */
    public static class ListIterator<T> implements Iterator<List<T>> {

        private final List<List<T>> elements;
        private final BitSet bitSet = new BitSet();
        private boolean started;
        private List<T> permutation;

        /**
         * Create a new instance.
         *
         * @param elements permutations for which to compute the permutations
         */
        public ListIterator(List<List<T>> elements) {
            this.elements = elements;
        }

        @Override
        public boolean hasNext() {
            // all elements are represented as distinct bits
            // when a bit is set, it is included in the current permutation
            // a list without a bit set has been cycled through, start-over with the first element
            if (started && bitSet.isEmpty()) {
                // all permutations are completed when there is zero bit set
                return false;
            }
            if (permutation != null) {
                return !permutation.isEmpty();
            }
            started = true;
            int offset = 0;
            int pOffset = 0;
            permutation = new LinkedList<>();
            for (List<T> list : elements) {
                int size = list.size();
                if (size == 0) {
                    continue;
                }
                int pos = bitSet.nextSetBit(offset);
                // position relative to element
                int rPos = pos - offset;
                if (rPos < 0 || rPos >= size) {
                    // out of bound, start over
                    pos = offset;
                    bitSet.set(pos);
                }
                permutation.add(list.get(pos - offset));

                // if first list
                // OR not the first AND none of the previous list bit is set
                if (offset == 0 || offset > 0 && bitSet.previousSetBit(offset - 1) < pOffset) {
                    // clear the current bit
                    bitSet.clear(pos);
                    if (pos - offset + 1 < size) {
                        // if next pos is within bound, set its bit
                        // otherwise, there is no bit set for this element
                        bitSet.set(pos + 1);
                    }
                }
                pOffset = offset;
                offset += size;
            }
            return !permutation.isEmpty();
        }

        @Override
        public List<T> next() {
            List<T> next = permutation;
            permutation = null;
            return next;
        }
    }

    /**
     * Compute the non-repetitive permutations of the given permutations.
     *
     * @param lists permutations for which to compute the permutations
     * @param <T>   element type
     * @return stream of permutations
     */
    public static <T> Stream<List<T>> ofList0(List<List<T>> lists) {
        Iterator<List<T>> iterator = new ListIterator<>(lists);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL), false);
    }

    /**
     * Compute the non-repetitive permutations of the given permutations.
     *
     * @param lists element for which to compute the permutations
     * @param <T>   element type
     * @return list of permutations
     */
    public static <T> List<List<T>> ofList(List<List<T>> lists) {
        return ofList0(lists).collect(Collectors.toList());
    }

    /**
     * Compute the non-repetitive permutations of the given elements.
     *
     * @param list elements for which to compute the permutations
     * @param <T>  element type
     * @return list of permutations
     * @throws UnsupportedOperationException if the list size is greater or equal to 64
     */
    public static <T> List<List<T>> of(List<T> list) {
        if (list.size() >= 64) {
            throw new UnsupportedOperationException("list size >= 64");
        }
        int size = list.size();
        long len = 1L << size; // 2 ^ length
        List<List<T>> permutations = new LinkedList<>();
        permutations.add(List.of());
        for (long p = 1; p < len; p++) {
            List<T> permutation = new LinkedList<>();
            // the permutation number (p) is a binary mask to filter the item to include
            for (int i = 0; i < size; i++) {
                if ((1L << i & p) > 0) {
                    // if the current index bitset is included in the mask
                    permutation.add(list.get(i));
                }
            }
            permutations.add(permutation);
        }
        return permutations;
    }
}
