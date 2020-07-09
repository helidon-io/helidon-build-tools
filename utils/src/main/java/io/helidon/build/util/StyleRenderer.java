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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiRenderer;
import org.fusesource.jansi.AnsiRenderer.Code;

import static org.fusesource.jansi.Ansi.ansi;


/**
 * A string formatter with a substitutions for Ansi escapes. Similar to {@link AnsiRenderer}, but supports a syntax that is
 * easier to read and write.
 * <p></p>
 * The following syntax supports applying one or more Ansi escape codes by name to a block
 * of text enclosed by {@code "$("} and {@code ")}:
 * <p></p>
 * <pre>
 *   <tt>$(</tt><em>code</em>[<tt>,</tt><em>code</em>]* <em>text</em><tt>)</tt>
 * </pre>
 * <p></p>
 * Supported code names are the case insensitive names of the non-color {@link Code}s, and the case <em>sensitive</em> set of
 * aliases for all colors (i.e. {@link Code#isColor()} {@code == true}:
 * <ol>
 *     <li>plain color: lower case (e.g. {@code "red")}</li>
 *     <li>bright color: lower case + {@code '!'} (e.g. {@code "red!")}</li>
 *     <li>bold color: upper case (e.g. {@code "RED")}</li>
 *     <li>bright bold color: upper case + {@code '!'} (e.g. {@code "RED!")}</li>
 * </ol>
 *
 * Note that background colors (e.g. {@link Code#BG_BLUE}) have the same set of aliases. Examples:
 *
 * <ol>
 *     <li>
 *         {@code "This is a bold $(bold example)."}
 *     </li>
 *     <li>
 *         {@code "This is a red $(red example containing an escaped \\) close paren)."}
 *     </li>
 *     <li>
 *         {@code "This is a bright bold cyan $(CYAN! example)."}
 *     </li>
 *     <li>
 *         {@code "This is a bright green background $(bg_green! example)."}
 *     </li>
 *     <li>
 *         {@code "This is a bold blue underlined $(BLUE,underline example)."}
 *     </li>
 *     <li>
 *         {@code "This is a bold red $(RED,BG_YELLOW! example) on a bright bold yellow background."}
 *     </li>
 * </ol>
 */
public class StyleRenderer {
    private static final String START_TOKEN = "$(";
    private static final int START_TOKEN_LEN = START_TOKEN.length();
    private static final char ESCAPE_CHAR = '\\';
    private static final char CODES_SEP_CHAR = ' ';
    private static final String CODE_SEP = ",";
    private static final char END_TOKEN_CHAR = ')';
    private static final String ESCAPED_END_TOKEN = "\\)";
    private static final String END_TOKEN = ")";
    private static final String BRIGHT = "!";
    private static final Map<String, Consumer<Ansi>> ALIASES = aliases();

    private StyleRenderer() {
    }

    /**
     * Substitute the Ansi sequences in the given text.
     *
     * @param text The text.
     * @return The substituted text.
     */
    public static String render(String text) {
        final int begin = text.indexOf(START_TOKEN);
        if (begin >= 0) {
            return replace(text, begin, new StringBuilder()).toString();
        } else {
            return text;
        }
    }

    private static StringBuilder replace(String text, int tokenStart, StringBuilder sb) {
        final int textLength = text.length();
        int textStart = 0;

        while (tokenStart >= 0) {
            sb.append(text, textStart, tokenStart);
            final int codesStart = tokenStart + START_TOKEN_LEN;
            final int tokenEnd = tokenEnd(text, codesStart, textLength);
            if (tokenEnd < 0) {
                break; // unclosed
            }
            final int codesEnd = text.indexOf(CODES_SEP_CHAR, codesStart);
            if (codesEnd < 0) {
                break; // malformed
            }
            final Ansi ansi = ansi();
            final String codes = text.substring(codesStart, codesEnd);
            final String styledText = text.substring(codesEnd + 1, tokenEnd);
            for (final String codeName : codes.split(CODE_SEP)) {
                apply(ansi, codeName);
            }
            sb.append(toStyled(ansi, styledText));
            textStart = tokenEnd + 1;
            tokenStart = text.indexOf(START_TOKEN, textStart);
        }
        return textStart < textLength ? sb.append(text.substring(textStart)) : sb;
    }

    private static String toStyled(Ansi ansi, String text) {
        final String unescaped = text.replace(ESCAPED_END_TOKEN, END_TOKEN);
        return ansi.a(unescaped).reset().toString();
    }

    private static void apply(Ansi ansi, String codeName) {
        Consumer<Ansi> alias = ALIASES.get(codeName);
        if (alias != null) {
            alias.accept(ansi);
        } else {
            boolean bright = false;
            if (codeName.endsWith(BRIGHT)) {
                bright = true;
                codeName = codeName.substring(0, codeName.length() - 1);
            }
            apply(ansi, Code.valueOf(codeName.toUpperCase(Locale.ENGLISH)), bright);
        }
    }

    private static void apply(Ansi ansi, List<Code> codes, boolean bright) {
        for (Code code : codes) {
            apply(ansi, code, bright);
        }
    }

    private static void apply(Ansi ansi, Code code, boolean bright) {
        if (code.isColor()) {
            final Color color = code.getColor();
            if (code.isBackground()) {
                if (bright) {
                    ansi.bgBright(color);
                } else {
                    ansi.bg(color);
                }
            } else {
                if (bright) {
                    ansi.fgBright(color);
                } else {
                    ansi.fg(color);
                }
            }
        } else if (code.isAttribute()) {
            ansi.a(code.getAttribute());
        }
    }

    private static int tokenEnd(String text, int codesStart, int textLength) {
        while (codesStart < textLength) {
            int tokenEnd = text.indexOf(END_TOKEN_CHAR, codesStart);
            if (tokenEnd < 0) {
                break;
            } else if (text.charAt(tokenEnd - 1) != ESCAPE_CHAR) {
                return tokenEnd;
            } else {
                codesStart = tokenEnd + 1;
            }
        }
        return -1;
    }

    private static Map<String, Consumer<Ansi>> aliases() {
        final Map<String, Consumer<Ansi>> aliases = new HashMap<>();
        aliases.put("blinking", ansi -> ansi.a(Ansi.Attribute.BLINK_SLOW));
        aliases.put("negative", ansi -> ansi.a(Ansi.Attribute.NEGATIVE_ON));
        aliases.put("conceal", ansi -> ansi.a(Ansi.Attribute.CONCEAL_ON));
        Stream.of(Code.values()).forEach(code -> {
            if (code.isColor()) {
                final String colorName = code.name().toLowerCase(Locale.ENGLISH);
                final String brightColorName = colorName + BRIGHT;
                final String boldColorName = colorName.toUpperCase(Locale.ENGLISH);
                final String boldBrightColorName = boldColorName + BRIGHT;

                aliases.put(colorName, ansi -> StyleRenderer.apply(ansi, List.of(code), false));
                aliases.put(brightColorName, ansi -> StyleRenderer.apply(ansi, List.of(code), true));
                aliases.put(boldColorName, ansi -> StyleRenderer.apply(ansi, List.of(Code.BOLD, code), false));
                aliases.put(boldBrightColorName, ansi -> StyleRenderer.apply(ansi, List.of(Code.BOLD, code), true));
            }
        });

        return aliases;
    }
}
