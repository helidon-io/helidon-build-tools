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

import java.util.List;

/**
 * Delimiter (emphasis, strong emphasis or custom emphasis).
 */
class Delimiter {

    private final List<Text> characters;
    private final char delimiterChar;
    private final int originalLength;
    private final boolean canOpen;
    private final boolean canClose;
    private Delimiter previous;
    private Delimiter next;

    Delimiter(List<Text> characters, char delimiterChar, boolean canOpen, boolean canClose, Delimiter previous) {
        this.characters = characters;
        this.delimiterChar = delimiterChar;
        this.canOpen = canOpen;
        this.canClose = canClose;
        this.previous = previous;
        this.originalLength = characters.size();
    }

    public Delimiter previous() {
        return previous;
    }

    public Delimiter next() {
        return next;
    }

    public void previous(Delimiter previous) {
        this.previous = previous;
    }

    public void next(Delimiter next) {
        this.next = next;
    }

    public List<Text> characters() {
        return characters;
    }

    public char delimiterChar() {
        return delimiterChar;
    }

    public boolean canOpen() {
        return canOpen;
    }

    public boolean canClose() {
        return canClose;
    }

    public int length() {
        return characters.size();
    }

    public int originalLength() {
        return originalLength;
    }

    public Text opener() {
        return characters.get(characters.size() - 1);
    }

    public Text closer() {
        return characters.get(0);
    }
}
