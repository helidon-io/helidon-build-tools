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
package io.helidon.build.common;

/**
 * Rich text.
 */
public interface RichText {

    /**
     * Append the source value with the given delimiters.
     *
     * @param value source value
     * @param start start index
     * @param end   end index
     * @return this instance for chaining
     */
    RichText append(CharSequence value, int start, int end);

    /**
     * Append the source value.
     *
     * @param value source value
     * @return this instance for chaining
     */
    RichText append(String value);

    /**
     * Get the text.
     *
     * @return String
     */
    String text();

    /**
     * Reset this instance.
     * @return this instance for chaining
     */
    RichText reset();
}
