/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

import java.util.HashMap;
import java.util.Map;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;
import org.junit.jupiter.api.Test;

import static org.fusesource.jansi.Ansi.Color.BLACK;
import static org.fusesource.jansi.Ansi.Color.BLUE;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.DEFAULT;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.MAGENTA;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.WHITE;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for class {@link Style}.
 */
class StyleTest {

    static final String RED_TEXT_CODE = color(RED);
    static final String GREEN_TEXT_CODE = color(GREEN);
    static final String YELLOW_TEXT_CODE = color(YELLOW);
    static final String BLUE_TEXT_CODE = color(BLUE);
    static final String MAGENTA_TEXT_CODE = color(MAGENTA);
    static final String CYAN_TEXT_CODE = color(CYAN);
    static final String WHITE_TEXT_CODE = color(WHITE);
    static final String BLACK_TEXT_CODE = color(BLACK);
    static final String DEFAULT_TEXT_CODE = color(DEFAULT);

    static final String RED_BACKGROUND_CODE = backgroundColor(RED);
    static final String GREEN_BACKGROUND_CODE = backgroundColor(GREEN);
    static final String YELLOW_BACKGROUND_CODE = backgroundColor(YELLOW);
    static final String BLUE_BACKGROUND_CODE = backgroundColor(BLUE);
    static final String MAGENTA_BACKGROUND_CODE = backgroundColor(MAGENTA);
    static final String CYAN_BACKGROUND_CODE = backgroundColor(CYAN);
    static final String WHITE_BACKGROUND_CODE = backgroundColor(WHITE);
    static final String BLACK_BACKGROUND_CODE = backgroundColor(BLACK);
    static final String DEFAULT_BACKGROUND_CODE = backgroundColor(DEFAULT);

    static final String BRIGHT_RED_TEXT_CODE = brightColor(RED);
    static final String BRIGHT_GREEN_TEXT_CODE = brightColor(GREEN);
    static final String BRIGHT_YELLOW_TEXT_CODE = brightColor(YELLOW);
    static final String BRIGHT_BLUE_TEXT_CODE = brightColor(BLUE);
    static final String BRIGHT_MAGENTA_TEXT_CODE = brightColor(MAGENTA);
    static final String BRIGHT_CYAN_TEXT_CODE = brightColor(CYAN);
    static final String BRIGHT_WHITE_TEXT_CODE = brightColor(WHITE);
    static final String BRIGHT_BLACK_TEXT_CODE = brightColor(BLACK);
    static final String BRIGHT_DEFAULT_TEXT_CODE = brightColor(DEFAULT);

    static final String BRIGHT_RED_BACKGROUND_CODE = brightBackgroundColor(RED);
    static final String BRIGHT_GREEN_BACKGROUND_CODE = brightBackgroundColor(GREEN);
    static final String BRIGHT_YELLOW_BACKGROUND_CODE = brightBackgroundColor(YELLOW);
    static final String BRIGHT_BLUE_BACKGROUND_CODE = brightBackgroundColor(BLUE);
    static final String BRIGHT_MAGENTA_BACKGROUND_CODE = brightBackgroundColor(MAGENTA);
    static final String BRIGHT_CYAN_BACKGROUND_CODE = brightBackgroundColor(CYAN);
    static final String BRIGHT_WHITE_BACKGROUND_CODE = brightBackgroundColor(WHITE);
    static final String BRIGHT_BLACK_BACKGROUND_CODE = brightBackgroundColor(BLACK);
    static final String BRIGHT_DEFAULT_BACKGROUND_CODE = brightBackgroundColor(DEFAULT);

    static final String BOLD_EMPHASIS_CODE = attribute(Attribute.INTENSITY_BOLD);
    static final String PLAIN_EMPHASIS_CODE = attribute(Attribute.RESET);
    static final String FAINT_EMPHASIS_CODE = attribute(Attribute.INTENSITY_FAINT);
    static final String ITALIC_EMPHASIS_CODE = attribute(Attribute.ITALIC);
    static final String UNDERLINE_EMPHASIS_CODE = attribute(Attribute.UNDERLINE);
    static final String STRIKETHROUGH_EMPHASIS_CODE = attribute(Attribute.STRIKETHROUGH_ON);
    static final String NEGATIVE_EMPHASIS_CODE = attribute(Attribute.NEGATIVE_ON);
    static final String CONCEAL_EMPHASIS_CODE = attribute(Attribute.CONCEAL_ON);
    static final String BLINK_EMPHASIS_CODE = attribute(Attribute.BLINK_SLOW);

    static final String RESET_CODE = attribute(Attribute.RESET);
    static final String NONE_CODE = "";

    static final Map<String, String> CODES_BY_NAME = buildCodesByName();

    static final Map<String, Style> STYLES = Style.styles();

    static Map<String, String> buildCodesByName() {
        Map<String, String> codes = new HashMap<>();

        codes.put("none", NONE_CODE);
        codes.put("bg_none", NONE_CODE);

        codes.put("red", RED_TEXT_CODE);
        codes.put("green", GREEN_TEXT_CODE);
        codes.put("yellow", YELLOW_TEXT_CODE);
        codes.put("blue", BLUE_TEXT_CODE);
        codes.put("magenta", MAGENTA_TEXT_CODE);
        codes.put("cyan", CYAN_TEXT_CODE);
        codes.put("white", WHITE_TEXT_CODE);
        codes.put("black", BLACK_TEXT_CODE);
        codes.put("default", DEFAULT_TEXT_CODE);
        codes.put("negative", NEGATIVE_EMPHASIS_CODE);

        codes.put("bg_red", RED_BACKGROUND_CODE);
        codes.put("bg_green", GREEN_BACKGROUND_CODE);
        codes.put("bg_yellow", YELLOW_BACKGROUND_CODE);
        codes.put("bg_blue", BLUE_BACKGROUND_CODE);
        codes.put("bg_magenta", MAGENTA_BACKGROUND_CODE);
        codes.put("bg_cyan", CYAN_BACKGROUND_CODE);
        codes.put("bg_white", WHITE_BACKGROUND_CODE);
        codes.put("bg_black", BLACK_BACKGROUND_CODE);
        codes.put("bg_default", DEFAULT_BACKGROUND_CODE);
        codes.put("bg_negative", NEGATIVE_EMPHASIS_CODE);

        codes.put("red!", BRIGHT_RED_TEXT_CODE);
        codes.put("green!", BRIGHT_GREEN_TEXT_CODE);
        codes.put("yellow!", BRIGHT_YELLOW_TEXT_CODE);
        codes.put("blue!", BRIGHT_BLUE_TEXT_CODE);
        codes.put("magenta!", BRIGHT_MAGENTA_TEXT_CODE);
        codes.put("cyan!", BRIGHT_CYAN_TEXT_CODE);
        codes.put("white!", BRIGHT_WHITE_TEXT_CODE);
        codes.put("black!", BRIGHT_BLACK_TEXT_CODE);
        codes.put("default!", BRIGHT_DEFAULT_TEXT_CODE);
        codes.put("negative!", NEGATIVE_EMPHASIS_CODE);

        codes.put("bg_red!", BRIGHT_RED_BACKGROUND_CODE);
        codes.put("bg_green!", BRIGHT_GREEN_BACKGROUND_CODE);
        codes.put("bg_yellow!", BRIGHT_YELLOW_BACKGROUND_CODE);
        codes.put("bg_blue!", BRIGHT_BLUE_BACKGROUND_CODE);
        codes.put("bg_magenta!", BRIGHT_MAGENTA_BACKGROUND_CODE);
        codes.put("bg_cyan!", BRIGHT_CYAN_BACKGROUND_CODE);
        codes.put("bg_white!", BRIGHT_WHITE_BACKGROUND_CODE);
        codes.put("bg_black!", BRIGHT_BLACK_BACKGROUND_CODE);
        codes.put("bg_default!", BRIGHT_DEFAULT_BACKGROUND_CODE);
        codes.put("bg_negative!", NEGATIVE_EMPHASIS_CODE);

        codes.put("bold", BOLD_EMPHASIS_CODE);
        codes.put("plain", PLAIN_EMPHASIS_CODE);
        codes.put("faint", FAINT_EMPHASIS_CODE);
        codes.put("italic", ITALIC_EMPHASIS_CODE);
        codes.put("underline", UNDERLINE_EMPHASIS_CODE);
        codes.put("strikethrough", STRIKETHROUGH_EMPHASIS_CODE);
        codes.put("conceal", CONCEAL_EMPHASIS_CODE);
        codes.put("blink", BLINK_EMPHASIS_CODE);

        return codes;
    }

    static String color(Color color) {
        return Ansi.ansi().fg(color).toString();
    }

    static String backgroundColor(Color color) {
        return Ansi.ansi().bg(color).toString();
    }

    static String brightColor(Color color) {
        return Ansi.ansi().fgBright(color).toString();
    }

    static String brightBackgroundColor(Color color) {
        return Ansi.ansi().bgBright(color).toString();
    }

    static String attribute(Attribute attribute) {
        return Ansi.ansi().a(attribute).toString();
    }

    static String codeByName(String name) {
        return CODES_BY_NAME.get(name);
    }

    static void assertExpectedStyle(String name) {
        assertExpectedStyle(name, codeByName(name));
    }

    static void assertExpectedStyle(String name, String code) {
        assertExpectedStyle(STYLES.get(name), code);
    }

    static void assertExpectedStyle(Style style, String code) {
        assertThat(style, is(not(nullValue())));
        assertThat(code, is(not(nullValue())));
        String example = style.apply("example");
        int start = example.indexOf("example");
        String codes = example.substring(0, start);
        assertThat(codes.contains(code), is(true));
        assertThat(example.endsWith(RESET_CODE), is(true));
    }

    @Test
    void testRequiredNameNotFound() {
        assertThrows(IllegalArgumentException.class, () -> Style.named("foo", true));
    }

    @Test
    void testNameNotFound() {
        assertThat(Style.named("foo"), is(Style.none()));
    }

    @Test
    void testNamed() {
        buildCodesByName().forEach(StyleTest::assertExpectedStyle);
    }

    @Test
    void testAliases() {
        Map<String, Style> styles = Style.styles();
        Log.info("Checking all %d style names and aliases", styles.size());
        styles.forEach((styleName, style) -> {
            Log.debug("    checking %s", styleName);
            int start = 0;
            int end = styleName.length();
            String parsedStyleName = styleName;
            boolean background = parsedStyleName.startsWith("bg_");
            if (background) {
                parsedStyleName = parsedStyleName.substring(3);
                end -= 3;
            }
            boolean bold = false;
            boolean italic = false;

            if (parsedStyleName.contains("**_") || parsedStyleName.contains("__*")) {
                bold = true;
                italic = true;
                start += 3;
                end -= 3;
            } else if (parsedStyleName.contains("**") || parsedStyleName.contains("__")) {
                bold = true;
                start += 2;
                end -= 2;
            } else if (parsedStyleName.contains("*") || parsedStyleName.contains("_")) {
                italic = true;
                start++;
                end--;
            }
            boolean bright = parsedStyleName.endsWith("!");
            if (bright) {
                end--;
            }
            String name = parsedStyleName.substring(start, end);
            if (Character.isUpperCase(name.charAt(0))) {
                bold = true;
                name = name.toLowerCase();
            }
            if (background) {
                name = "bg_" + name;
            }
            if (bright) {
                name += "!";
            }
            assertExpectedStyle(name);
            if (bold) {
                assertExpectedStyle("bold", BOLD_EMPHASIS_CODE);
            }
            if (italic) {
                assertExpectedStyle("italic", ITALIC_EMPHASIS_CODE);
            }
        });
    }

    @Test
    void testStrip() {
        String sample = "sample";
        Style style = Style.of("RED!");
        String styled = style.apply(sample);
        String stripped = Style.strip(styled);
        assertThat(Style.isStyled(styled), is(true));
        assertThat(Style.isStyled(stripped), is(false));
        assertThat(stripped, is(sample));
    }

    @Test
    void logTables() {
        Style.logSummaryTables();
    }
}
