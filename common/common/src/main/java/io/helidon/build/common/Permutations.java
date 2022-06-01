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
import java.util.LinkedList;
import java.util.List;

/**
 * Permutation utility.
 */
public final class Permutations {

    private Permutations() {
        // cannot be instantiated
    }

    /**
     * Compute the non-repetitive permutations of the given elements.
     *
     * @param lists elements for which to compute the permutations
     * @param <T>      element type
     * @return list of permutations
     */
    public static <T> List<List<T>> ofList(List<List<T>> lists) {
        List<List<T>> permutations = new LinkedList<>();
        if (lists.isEmpty()) {
            return permutations;
        }
        // all elements are represented as distinct bits
        // when a bit is set, it is included in the current permutation
        // a list without a bit set has been cycled through, start-over with the first element
        BitSet bitSet = new BitSet();

        boolean started = false;

        // all permutations are completed when there is zero bit set
        while (!started || !bitSet.isEmpty()) {
            started = true;
            int offset = 0;
            int p_offset = 0;
            List<T> permutation = new LinkedList<>();
            for (List<T> list : lists) {
                int size = list.size();
                if (size == 0) {
                    continue;
                }
                int pos = bitSet.nextSetBit(offset);
                // position relative to element
                int r_pos = pos - offset;
                if (r_pos < 0 || r_pos >= size) {
                    // out of bound, start over
                    pos = offset;
                    bitSet.set(pos);
                }
                permutation.add(list.get(pos - offset));

                // if first list
                // OR not the first AND none of the previous list bit is set
                if (offset == 0 || offset > 0 && bitSet.previousSetBit(offset - 1) < p_offset) {
                    // clear the current bit
                    bitSet.clear(pos);
                    if (pos - offset + 1 < size) {
                        // if next pos is within bound, set its bit
                        // otherwise, there is no bit set for this element
                        bitSet.set(pos + 1);
                    }
                }
                p_offset = offset;
                offset += size;
            }
            if (!permutation.isEmpty()) {
                permutations.add(permutation);
            }
        }
        return permutations;
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
        long p_len = 1L << size; // 2 ^ length
        List<List<T>> permutations = new LinkedList<>();
        permutations.add(List.of());
        for (long p = 1; p < p_len; p++) {
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
