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
package io.helidon.build.common.ansi;

import io.helidon.build.common.RichText;

import org.fusesource.jansi.Ansi;

/**
 * Ansi based implementation of {@link RichText}.
 */
public final class AnsiText implements RichText {

    private final Ansi ansi;

    AnsiText() {
        ansi = Ansi.ansi();
    }

    /**
     * Get the underlying {@link Ansi} instance.
     *
     * @return Ansi
     */
    Ansi ansi() {
        return ansi;
    }

    @Override
    public RichText append(CharSequence value, int start, int end) {
        ansi.a(value, start, end);
        return this;
    }

    @Override
    public RichText append(String value) {
        ansi.a(value);
        return this;
    }

    @Override
    public String text() {
        return ansi.toString();
    }

    @Override
    public RichText reset() {
        ansi.reset();
        return this;
    }
}
