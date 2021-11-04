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

abstract class EmphasisDelimiterProcessor implements DelimiterProcessor {

    private final char delimiterChar;

    protected EmphasisDelimiterProcessor(char delimiterChar) {
        this.delimiterChar = delimiterChar;
    }

    @Override
    public char getOpeningCharacter() {
        return delimiterChar;
    }

    @Override
    public char getClosingCharacter() {
        return delimiterChar;
    }

    @Override
    public int getMinLength() {
        return 1;
    }

    @Override
    public int process(Delimiter opening, Delimiter closing) {
        if ((opening.canClose() || closing.canOpen())
                && closing.originalLength() % 3 != 0
                && (opening.originalLength() + closing.originalLength()) % 3 == 0) {
            return 0;
        }

        int usedDelimiters;
        Node emphasis;
        if (opening.length() >= 2 && closing.length() >= 2) {
            usedDelimiters = 2;
            emphasis = new StrongEmphasis(String.valueOf(delimiterChar) + delimiterChar);
        } else {
            usedDelimiters = 1;
            emphasis = new Emphasis(String.valueOf(delimiterChar));
        }

        Text opener = opening.getOpener();
        for (Node node : Nodes.between(opener, closing.getCloser())) {
            emphasis.appendChild(node);
        }

        opener.insertAfter(emphasis);

        return usedDelimiters;
    }
}
