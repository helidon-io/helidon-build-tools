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
 * Opening bracket for links (<code>[</code>).
 */
class Bracket {

    private final Text node;

    /**
     * The position of the marker for the bracket (<code>[</code>).
     */
    private final Position markerPosition;

    /**
     * The position of the content (after the opening bracket).
     */
    private final Position contentPosition;

    /**
     * Previous bracket.
     */
    private final Bracket previous;

    /**
     * Previous delimiter (emphasis, etc) before this bracket.
     */
    private final Delimiter previousDelimiter;

    /**
     * Whether this bracket is allowed to form a link (also known as "active").
     */
    private boolean allowed = true;

    /**
     * Whether there is an unescaped bracket (opening or closing) anywhere after this opening bracket.
     */
    private boolean bracketAfter = false;

    public static Bracket link(Text node, Position markerPosition, Position contentPosition, Bracket previous,
                               Delimiter previousDelimiter) {
        return new Bracket(node, markerPosition, contentPosition, previous, previousDelimiter);
    }

    private Bracket(Text node, Position markerPosition, Position contentPosition, Bracket previous, Delimiter previousDelimiter) {
        this.node = node;
        this.markerPosition = markerPosition;
        this.contentPosition = contentPosition;
        this.previous = previous;
        this.previousDelimiter = previousDelimiter;
    }

    public Text node() {
        return node;
    }

    public Position markerPosition() {
        return markerPosition;
    }

    public Position contentPosition() {
        return contentPosition;
    }

    public Bracket previous() {
        return previous;
    }

    public Delimiter previousDelimiter() {
        return previousDelimiter;
    }

    public boolean allowed() {
        return allowed;
    }

    public void allowed(boolean allowed) {
        this.allowed = allowed;
    }

    public boolean bracketAfter() {
        return bracketAfter;
    }

    public void bracketAfter(boolean bracketAfter) {
        this.bracketAfter = bracketAfter;
    }
}
