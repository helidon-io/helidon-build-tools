/*
 * Copyright (c) 2022, 2025 Oracle and/or its affiliates.
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.Long.numberOfTrailingZeros;

/**
 * Variation utility.
 */
public final class Variations {

    private Variations() {
        // cannot be instantiated
    }

    /**
     * An iterator that computes the non-repetitive variations of the given lists.
     *
     * @param <T> element type
     */
    public static class ListIterator<T> implements Iterator<List<T>> {

        private final List<List<T>> elements;
        private final BitSet bitSet = new BitSet();
        private boolean started;
        private List<T> variation;

        /**
         * Create a new instance.
         *
         * @param elements elements for which to compute the variations
         */
        public ListIterator(List<List<T>> elements) {
            this.elements = elements;
        }

        @Override
        public boolean hasNext() {
            // all elements are represented as distinct bits
            // when a bit is set; it is included in the current variation
            // a list without a bit set has been cycled through, start-over with the first element
            if (started && bitSet.isEmpty()) {
                // all variations are completed when there are no bit set
                return false;
            }
            if (variation != null) {
                return !variation.isEmpty();
            }
            started = true;
            int offset = 0;
            int pOffset = 0;
            variation = new LinkedList<>();
            for (List<T> list : elements) {
                int size = list.size();
                if (size == 0) {
                    continue;
                }
                int pos = bitSet.nextSetBit(offset);
                // position relative to the element
                int rPos = pos - offset;
                if (rPos < 0 || rPos >= size) {
                    // out of bound, start over
                    pos = offset;
                    bitSet.set(pos);
                }
                variation.add(list.get(pos - offset));

                // if first list
                // OR not the first AND none of the previous list bit is set
                if (offset == 0 || offset > 0 && bitSet.previousSetBit(offset - 1) < pOffset) {
                    // clear the current bit
                    bitSet.clear(pos);
                    if (pos - offset + 1 < size) {
                        // if the next pos is within bound, set its bit
                        // otherwise; there is no bit set for this element
                        bitSet.set(pos + 1);
                    }
                }
                pOffset = offset;
                offset += size;
            }
            return !variation.isEmpty();
        }

        @Override
        public List<T> next() {
            List<T> next = variation;
            variation = null;
            return next;
        }
    }

    /**
     * Compute the non-repetitive variation of the given lists.
     *
     * @param lists elements for which to compute the variations
     * @param <T>   element type
     * @return stream of variations
     */
    public static <T> Stream<List<T>> ofList0(List<List<T>> lists) {
        Iterator<List<T>> iterator = new ListIterator<>(lists);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL), false);
    }

    /**
     * Compute the non-repetitive variations of the given lists.
     *
     * @param lists elements for which to compute the variations
     * @param <T>   element type
     * @return list of variations
     */
    public static <T> List<List<T>> ofList(List<List<T>> lists) {
        return ofList0(lists).collect(Collectors.toList());
    }

    /**
     * Compute the non-repetitive variations of the given elements.
     *
     * @param elements list for which to compute the variations
     * @param <T>      element type
     * @return variations
     * @throws UnsupportedOperationException if the collection size is greater or equal to 64
     */
    public static <T> List<List<T>> of(List<T> elements) {
        if (elements.size() >= 64) {
            throw new UnsupportedOperationException("size >= 64");
        }
        int size = elements.size();
        long len = (1L << size) - 1; // 2 ^ length
        List<List<T>> result = new ArrayList<>(size);
        result.add(List.of());
        for (long p = 1; p <= len; p++) {
            List<T> entry = new ArrayList<>();
            for (int i = numberOfTrailingZeros(p); i < 64; i = numberOfTrailingZeros(p & (-1L << i + 1))) {
                entry.add(elements.get(i));
            }
            result.add(entry);
        }
        return result;
    }
}
