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

/**
 * Text style functions.
 */
public enum StyleFunction implements Function<Object, String> {

    /**
     * Plain.
     */
    Plain("plain"),

    /**
     * Bold.
     */
    Bold("bold"),

    /**
     * Italic.
     */
    Italic("italic"),

    /**
     * Bold, italic.
     */
    BoldItalic("_bold_"),

    /**
     * Blue.
     */
    Blue("blue"),

    /**
     * Italic blue.
     */
    ItalicBlue("_blue_"),

    /**
     * Bright blue.
     */
    BrightBlue("blue!"),

    /**
     * Bold blue.
     */
    BoldBlue("BLUE"),

    /**
     * Bold, italic blue.
     */
    BoldItalicBlue("_BLUE_"),

    /**
     * Bold, bright blue.
     */
    BoldBrightBlue("BLUE!"),

    /**
     * Bold, bright, italic blue.
     */
    BoldBrightItalicBlue("_BLUE_!"),

    /**
     * Cyan.
     */
    Cyan("cyan"),

    /**
     * Italic cyan.
     */
    ItalicCyan("_cyan_"),

    /**
     * Bright cyan.
     */
    BrightCyan("cyan!"),

    /**
     * Bold cyan.
     */
    BoldCyan("CYAN"),

    /**
     * Bold, italic cyan.
     */
    BoldItalicCyan("_CYAN_"),

    /**
     * Bold, bright cyan.
     */
    BoldBrightCyan("CYAN!"),

    /**
     * Bold, bright, italic cyan.
     */
    BoldBrightItalicCyan("_CYAN_!"),

    /**
     * Green.
     */
    Green("green"),

    /**
     * Italic green.
     */
    ItalicGreen("_green_"),

    /**
     * Bright green.
     */
    BrightGreen("green!"),

    /**
     * Bold green.
     */
    BoldGreen("GREEN"),

    /**
     * Bold, italic green.
     */
    BoldItalicGreen("_GREEN_"),

    /**
     * Bold, bright green.
     */
    BoldBrightGreen("GREEN!"),

    /**
     * Bold, bright, italic green.
     */
    BoldBrightItalicGreen("_GREEN_!"),

    /**
     * Yellow.
     */
    Yellow("yellow"),

    /**
     * Italic yellow.
     */
    ItalicYellow("_yellow_"),

    /**
     * Bright yellow.
     */
    BrightYellow("yellow!"),

    /**
     * Bold yellow.
     */
    BoldYellow("YELLOW"),

    /**
     * Bold, italic yellow.
     */
    BoldItalicYellow("_YELLOW_"),

    /**
     * Bold, bright yellow.
     */
    BoldBrightYellow("YELLOW!"),

    /**
     * Bold, bright, italic yellow.
     */
    BoldBrightItalicYellow("_YELLOW_!"),

    /**
     * Red.
     */
    Red("red"),

    /**
     * Italic red.
     */
    ItalicRed("_red_"),

    /**
     * Bright red.
     */
    BrightRed("red!"),

    /**
     * Bold red.
     */
    BoldRed("RED"),

    /**
     * Bold, italic red.
     */
    BoldItalicRed("_RED_"),

    /**
     * Bold, bright red.
     */
    BoldBrightRed("RED!"),

    /**
     * Bold, bright, italic red.
     */
    BoldBrightItalicRed("_RED_!");

    private final Style style;

    StyleFunction(String name) {
        this.style = Style.named(name, true);
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
        return style.apply(message);
    }
}
