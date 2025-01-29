/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

/**
 * A fluent {@link BitSet} utility.
 */
public final class BitSets {

    private BitSets() {
    }

    /**
     * Test if all the given bits are set.
     *
     * @param bs1 bits
     * @param bs2 bits
     * @return {@code true} if all bits are set
     */
    public static boolean containsAll(BitSet bs1, BitSet bs2) {
        return and(copyOf(bs1), bs2).equals(bs1);
    }

    /**
     * Create a copy.
     *
     * @param bits bits
     * @return BitSet
     */
    public static BitSet copyOf(BitSet bits) {
        BitSet bs = new BitSet();
        bs.or(bits);
        return bs;
    }

    /**
     * Create a new instance.
     *
     * @param bits bits
     * @return BitSet
     */
    public static BitSet of(int... bits) {
        BitSet bs = new BitSet();
        for (int bit : bits) {
            bs.set(bit);
        }
        return bs;
    }

    /**
     * Create a new instance.
     *
     * @param words words
     * @return BitSet
     */
    public static BitSet of(long... words) {
        return BitSet.valueOf(words);
    }

    /**
     * See {@link BitSet#or(BitSet)}.
     *
     * @param bs1 bits
     * @param bs2 bits
     * @return BitSet
     */
    public static BitSet or(BitSet bs1, BitSet bs2) {
        bs1.or(bs2);
        return bs1;
    }

    /**
     * See {@link BitSet#and(BitSet)}.
     *
     * @param bs1 bits
     * @param bs2 bits
     * @return BitSet
     */
    public static BitSet and(BitSet bs1, BitSet bs2) {
        bs1.and(bs2);
        return bs1;
    }

    /**
     * See {@link BitSet#andNot(BitSet)}.
     *
     * @param bs1 bits
     * @param bs2 bits
     * @return BitSet
     */
    public static BitSet andNot(BitSet bs1, BitSet bs2) {
        bs1.andNot(bs2);
        return bs1;
    }
}
