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
 * Custom delimiter processor.
 * <p>
 * Note that implementations of this need to be thread-safe, the same instance may be used by multiple parsers.
 */
interface DelimiterProcessor {

    /**
     * @return the character that marks the beginning of a delimited node, must not clash with any other special
     * characters
     */
    char openingCharacter();

    /**
     * @return the character that marks the the ending of a delimited node, must not clash with any other special
     * characters. Note that for a symmetric delimiter such as "*", this is the same as the opening.
     */
    char closingCharacter();

    /**
     * Minimum number of delimiter characters that are needed to activate this. Must be at least 1.
     */
    int minLength();

    /**
     * Process the delimiter runs.
     * <p>
     * The processor can examine the runs and the nodes and decide if it wants to process or not. If not, it should not
     * change any nodes and return 0. If yes, it should do the processing (wrapping nodes, etc) and then return how many
     * delimiters were used.
     * <p>
     * Note that removal (unlinking) of the used delimiter {@link Text} nodes is done by the caller.
     *
     * @param opening the opening delimiter run
     * @param closing the closing delimiter run
     * @return how many delimiters were used; must not be greater than length of either opener or closer
     */
    int process(Delimiter opening, Delimiter closing);

}
