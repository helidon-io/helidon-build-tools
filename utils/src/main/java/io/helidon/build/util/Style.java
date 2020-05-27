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
     * Plain.
     */
    Plain(false, false, false, null),

    /**
     * Bold.
     */
    Bold(true, false, false, null),

    /**
     * Italic.
     */
    Italic(false, false, true, null),

    /**
     * Bold, italic.
     */
    BoldItalic(true, false, true, null),

    /**
     * Blue.
     */
    Blue(false, false, false, Ansi.Color.BLUE),

    /**
     * Italic blue.
     */
    ItalicBlue(false, false, true, Ansi.Color.BLUE),

    /**
     * Bold blue.
     */
    BoldBlue(true, false, false, Ansi.Color.BLUE),
    /**
     * Bold, italic blue.
     */
    BoldItalicBlue(true, false, true, Ansi.Color.BLUE),

    /**
     * Bold, bright blue.
     */
    BoldBrightBlue(true, true, false, Ansi.Color.BLUE),

    /**
     * Bold, bright, italic blue.
     */
    BoldBrightItalicBlue(true, true, true, Ansi.Color.BLUE),

    /**
     * Cyan.
     */
    Cyan(false, false, false, Ansi.Color.CYAN),

    /**
     * Italic cyan.
     */
    ItalicCyan(false, false, true, Ansi.Color.CYAN),

    /**
     * Bold cyan.
     */
    BoldCyan(true, false, false, Ansi.Color.CYAN),

    /**
     * Bold, italic cyan.
     */
    BoldItalicCyan(true, false, true, Ansi.Color.CYAN),

    /**
     * Bold, bright cyan.
     */
    BoldBrightCyan(true, true, false, Ansi.Color.CYAN),

    /**
     * Bold, bright, italic cyan.
     */
    BoldBrightItalicCyan(true, true, true, Ansi.Color.CYAN),

    /**
     * Green.
     */
    Green(false, false, false, Ansi.Color.GREEN),

    /**
     * Italic green.
     */
    ItalicGreen(false, false, true, Ansi.Color.GREEN),

    /**
     * Bold green.
     */
    BoldGreen(true, false, false, Ansi.Color.GREEN),

    /**
     * Bold, italic green.
     */
    BoldItalicGreen(true, false, true, Ansi.Color.GREEN),

    /**
     * Bold, bright green.
     */
    BoldBrightGreen(true, true, false, Ansi.Color.GREEN),

    /**
     * Bold, bright, italic green.
     */
    BoldBrightItalicGreen(true, true, true, Ansi.Color.GREEN),

    /**
     * Yellow.
     */
    Yellow(false, false, false, Ansi.Color.YELLOW),

    /**
     * Italic yellow.
     */
    ItalicYellow(false, false, true, Ansi.Color.YELLOW),

    /**
     * Bold yellow.
     */
    BoldYellow(true, false, false, Ansi.Color.YELLOW),

    /**
     * Bold, italic yellow.
     */
    BoldItalicYellow(true, false, true, Ansi.Color.YELLOW),

    /**
     * Bold, bright yellow.
     */
    BoldBrightYellow(true, true, false, Ansi.Color.YELLOW),

    /**
     * Bold, bright, italic yellow.
     */
    BoldBrightItalicYellow(true, true, true, Ansi.Color.YELLOW),

    /**
     * Red.
     */
    Red(false, false, false, Ansi.Color.RED),

    /**
     * Italic red.
     */
    ItalicRed(false, false, true, Ansi.Color.RED),

    /**
     * Bold red.
     */
    BoldRed(true, false, false, Ansi.Color.RED),

    /**
     * Bold, italic red.
     */
    BoldItalicRed(true, false, true, Ansi.Color.RED),

    /**
     * Bold, bright red.
     */
    BoldBrightRed(true, true, false, Ansi.Color.RED),

    /**
     * Bold, bright, italic red.
     */
    BoldBrightItalicRed(true, true, true, Ansi.Color.RED);

    private static final boolean ENABLED = AnsiConsoleInstaller.ensureInstalled();
    private static final String ANSI_ESCAPE_BEGIN = "\033[";
    private final boolean bold;
    private final boolean bright;
    private final boolean italic;
    private final Ansi.Color color;

    Style(boolean bold, boolean bright, boolean italic, Ansi.Color color) {
        this.bold = bold;
        this.bright = bright;
        this.italic = italic;
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
            if (italic) {
                ansi.a(Ansi.Attribute.ITALIC);
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
        return text != null && text.contains(ANSI_ESCAPE_BEGIN);
    }
}
