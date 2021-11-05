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

class BlockStart {

    private final BlockParser[] blockParsers;
    private int newIndex = -1;
    private int newColumn = -1;

    BlockStart(BlockParser... blockParsers) {
        this.blockParsers = blockParsers;
    }

    public BlockParser[] blockParsers() {
        return blockParsers;
    }

    public int newIndex() {
        return newIndex;
    }

    public int newColumn() {
        return newColumn;
    }

    public BlockStart atIndex(int newIndex) {
        this.newIndex = newIndex;
        return this;
    }

    public static BlockStart of(BlockParser... blockParsers) {
        return new BlockStart(blockParsers);
    }

    public BlockStart atColumn(int newColumn) {
        this.newColumn = newColumn;
        return this;
    }

    public static BlockStart none() {
        return null;
    }

}
