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

package io.helidon.build.archetype.engine.v2.markdown;

/**
 * A line or part of a line from the input source.
 */
class SourceLine {

    private final CharSequence content;

    public static SourceLine of(CharSequence content) {
        return new SourceLine(content);
    }

    private SourceLine(CharSequence content) {
        if (content == null) {
            throw new NullPointerException("content must not be null");
        }
        this.content = content;
    }

    public CharSequence getContent() {
        return content;
    }

    public SourceLine substring(int beginIndex, int endIndex) {
        CharSequence newContent = content.subSequence(beginIndex, endIndex);
        return SourceLine.of(newContent);
    }
}
