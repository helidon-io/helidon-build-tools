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
 * A link with a destination and an optional title; the link text is in child nodes.
 * <p>
 * Example for an inline link :
 * <pre><code>
 * [link](/uri "title")
 * </code></pre>
 * <p>
 * The corresponding Link node would look like this:
 * <ul>
 * <li>{@link #destination()} returns {@code "/uri"}
 * <li>{@link #title()} returns {@code "title"}
 * <li>A {@link Text} child node with {@link Text#literal() getLiteral} that returns {@code "link"}</li>
 * </ul>
 * <p>
 */
class Link extends Node {

    private String destination;
    private String title;

    Link(String destination, String title) {
        this.destination = destination;
        this.title = title;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public String destination() {
        return destination;
    }

    public String title() {
        return title;
    }

    public void title(String title) {
        this.title = title;
    }

    @Override
    protected String toStringAttributes() {
        return "destination=" + destination + ", title=" + title;
    }
}
