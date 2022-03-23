/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
import io.helidon.build.common.RichTextProvider;
import io.helidon.build.common.RichTextStyle;
import io.helidon.build.common.RichTextStyle.StyleList;

import org.fusesource.jansi.Ansi;

/**
 * Ansi implementation of {@link RichTextProvider}.
 */
public class AnsiTextProvider implements RichTextProvider {

    private final boolean enabled;

    /**
     * Create a new instance.
     */
    public AnsiTextProvider() {
        enabled = AnsiConsoleInstaller.install();
        Ansi.setEnabled(enabled);
    }

    /**
     * Get the enabled flag.
     *
     * @return {@code true} if ansi console is enabled, {@code false} otherwise
     */
    static boolean isEnabled() {
        if (Holder.INSTANCE instanceof AnsiTextProvider) {
            return ((AnsiTextProvider) Holder.INSTANCE).enabled;
        }
        return false;
    }

    @Override
    public RichText richText() {
        return new AnsiText();
    }

    @Override
    public RichTextStyle styleOf(String... names) {
        if (names.length == 0) {
            return RichTextStyle.NONE;
        } else if (names.length == 1) {
            return AnsiTextStyle.named(names[0]);
        } else {
            return new StyleList(names);
        }
    }
}
