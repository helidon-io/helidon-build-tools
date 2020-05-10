/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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
    BoldGreen(true, false, Ansi.Color.GREEN),

    /**
     * Bold, bright green.
     */
    BoldBrightGreen(true, true, Ansi.Color.GREEN),

    /**
     * Yellow.
     */
    Yellow(false, false, Ansi.Color.YELLOW),

    /**
     * Bold yellow.
     */
    BoldYellow(true, false, Ansi.Color.YELLOW),

    /**
     * Bold, bright yellow.
     */
    BoldBrightYellow(true, true, Ansi.Color.YELLOW),

    /**
     * Red.
     */
    Red(false, false, Ansi.Color.RED),

    /**
     * Bold red.
     */
    BoldRed(true, false, Ansi.Color.RED),

    /**
     * Bold, bright red.
     */
    BoldBrightRed(true, true, Ansi.Color.RED),

    /**
     * Bold.
     */
    Bold(true, false, null);

    private static final boolean ENABLED = AnsiConsoleInstaller.ensureInstalled();
    private static final String ANSI_ESCAPE_BEGIN = "\033[";
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
     * Returns the message in this style, if Ansi escapes are supported.
     *
     * @param message The message.
     * @return The message.
     */
    @Override
    public String apply(Object message) {
        if (ENABLED) {
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
        } else {
            return message.toString();
        }
    }

    /**
     * Renders the embedded {@link StyleRenderer style DSL}, if styles are supported, after formatting.
     *
     * @param format The message format.
     * @param args The message arguments.
     * @return The message.
     */
    public static String render(String format, Object... args) {
        return StyleRenderer.render(String.format(format, args));
    }

    /**
     * Tests whether or not the given text contains an Ansi escape sequence.
     *
     * @param text The text.
     * @return {@code true} if an Ansi escape sequence found.
     */
    public static boolean isStyled(String text) {
        return text.contains(ANSI_ESCAPE_BEGIN);
    }
}
