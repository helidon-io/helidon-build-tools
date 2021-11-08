/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.common.markdown;

/**
 * Result object for continuing parsing of a block, see static methods for constructors.
 */
class BlockContinue {

    private final int newIndex;
    private final int newColumn;
    private final boolean shouldFinalize;

    BlockContinue(int newIndex, int newColumn, boolean finalize) {
        this.newIndex = newIndex;
        this.newColumn = newColumn;
        this.shouldFinalize = finalize;
    }

    public int newIndex() {
        return newIndex;
    }

    public int newColumn() {
        return newColumn;
    }

    public boolean shouldFinalize() {
        return shouldFinalize;
    }

    public static BlockContinue none() {
        return null;
    }

    public static BlockContinue atIndex(int newIndex) {
        return new BlockContinue(newIndex, -1, false);
    }

    public static BlockContinue atColumn(int newColumn) {
        return new BlockContinue(-1, newColumn, false);
    }

    public static BlockContinue finished() {
        return new BlockContinue(-1, -1, true);
    }
}
