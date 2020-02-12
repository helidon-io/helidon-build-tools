/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.util;

import java.util.function.Function;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Text styles.
 */
public enum Style implements Function<Object, String> {

    /**
     * Blue.
     */
    Blue(false, false, Ansi.Color.BLUE),

    /**
     * Bold blue.
     */
    BoldBlue(true, false, Ansi.Color.BLUE),

    /**
     * Bold, bright blue.
     */
    BoldBrightBlue(true, true, Ansi.Color.BLUE),

    /**
     * Cyan.
     */
    Cyan(false, false, Ansi.Color.CYAN),

    /**
     * Bold cyan.
     */
    BoldCyan(true, false, Ansi.Color.CYAN),

    /**
     * Bold, bright cyan.
     */
    BoldBrightCyan(true, true, Ansi.Color.CYAN),

    /**
     * Green.
     */
    Green(false, false, Ansi.Color.GREEN),

    /**
     * Bold green.
     */
    BoldGreen(false, false, Ansi.Color.GREEN),

    /**
     * Bold, bright green.
     */
    BoldBrightGreen(false, true, Ansi.Color.GREEN),

    /**
     * Yellow.
     */
    Yellow(false, false, Ansi.Color.YELLOW),

    /**
     * Bold yellow.
     */
    BoldYellow(false, false, Ansi.Color.YELLOW),

    /**
     * Bold, bright yellow.
     */
    BoldBrightYellow(false, true, Ansi.Color.YELLOW),

    /**
     * Bold.
     */
    Bold(true, false, null);

    private final boolean bold;
    private final boolean bright;
    private final Ansi.Color color;

    Style(boolean bold, boolean bright, Ansi.Color color) {
        this.bold = bold;
        this.bright = bright;
        this.color = color;
    }

    /**
     * Returns the message in this style, if styles are supported.
     *
     * @param format The message format.
     * @param args The message arguments.
     * @return The message.
     */
    public String format(String format, Object... args) {
        return apply(String.format(format, args));
    }

    /**
     * Returns the message in this style, if styles are supported.
     *
     * @param message The message.
     * @return The message.
     */
    @Override
    public String apply(Object message) {
        final Ansi ansi = ansi();
        if (bold) {
            ansi.bold();
        }
        if (bright) {
            ansi.fgBright(color);
        } else if (color != null) {
            ansi.fg(color);
        }
        return ansi.a(message).reset().toString();
    }

    static {
        AnsiConsole.systemInstall();
    }
}
