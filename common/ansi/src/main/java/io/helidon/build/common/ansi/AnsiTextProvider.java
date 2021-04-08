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
import io.helidon.build.common.RichTextProvider;
import io.helidon.build.common.RichTextStyle;
import io.helidon.build.common.RichTextStyle.StyleList;

/**
 * Ansi implementation of {@link RichTextProvider}.
 */
public class AnsiTextProvider implements RichTextProvider {

    /**
     * Get the enabled flag if {@link Holder#INSTANCE} is {@link AnsiTextProvider}.
     *
     * @return {@code true} if ansi console is enabled, {@code false} otherwise
     */
    public static final boolean ANSI_ENABLED = Holder.INSTANCE.as(AnsiTextProvider.class)
                                                       .map(AnsiTextProvider::isEnabled)
                                                       .orElse(false);

    private final boolean enabled;

    /**
     * Create a new instance.
     */
    public AnsiTextProvider() {
        enabled = AnsiConsoleInstaller.install();
    }

    /**
     * Get the enabled flag.
     *
     * @return {@code true} if ansi console is enabled, {@code false} otherwise
     */
    boolean isEnabled() {
        return enabled;
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
