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
 * State of the parser that is used in block parsers.
 */
interface ParserState {
    /**
     * @return the current source line being parsed (full line)
     */
    SourceLine line();

    /**
     * @return the current index within the line (0-based)
     */
    int index();

    /**
     * @return the index of the next non-space character starting from {@link #index()} (may be the same) (0-based)
     */
    int nextNonSpaceIndex();

    /**
     * @return the indentation in columns (either by spaces or tab stop of 4)
     */
    int indent();

    /**
     * @return true if the current line is blank starting from the index
     */
    boolean blank();

    /**
     * @return the deepest open block parser
     */
    BlockParser activeBlockParser();

}
