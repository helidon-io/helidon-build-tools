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
 * Parser for a specific block node.
 */
interface BlockParser {
    /**
     * Return true if the block that is parsed is a container (contains other blocks), or false if it's a leaf.
     */
    default boolean container(){
        return false;
    }

    /**
     * Return true if the block can have lazy continuation lines.
     */
    default boolean canHaveLazyContinuationLines(){
        return false;
    }

    default boolean canContain(Block childBlock){
        return false;
    }

    Block block();

    BlockContinue tryContinue(ParserState parserState);

    default void addLine(SourceLine line) {
    }

    default void closeBlock() {
    }

    default void parseInlines(InlineParser inlineParser) {
    }
}
