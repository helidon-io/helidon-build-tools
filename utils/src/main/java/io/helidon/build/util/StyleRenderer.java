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

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiRenderer;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * A string formatter with a substitutions for rich text. Similar to {@link AnsiRenderer}, but supports a syntax that is
 * easier to read and write.
 * <p></p>
 * Colors and styles are applied to text enclosed by {@code "$("} and {@code ")"}, e.g.:
 * <p></p>
 * <pre>
 *    "Here is $(red styled) text"
 * </pre>
 * In this example, the word {@code styled} will (normally) appear in red. If the styled text itself contains parentheses,
 * the closing paren should be escaped with a backslash:
 * <p></p>
 * <pre>
 *    "Here is $(red (and example of\\) styled) text"
 * </pre>
 * The DSL syntax is:
 * <p></p>
 * <pre>
 *   <tt>$(</tt><em>style</em>[<tt>,</tt><em>style</em>]* <em>text</em><tt>)</tt>
 * </pre>
 * where {@code style} is a case-sensitive name for a color, background color, emphasis or an alias. Nesting is supported.
 * <p></p>
 * NOTE: Terminals often provide mappings between the standard color names used here and what they actually render. So, for
 * example, you may declare {@code red} but a terminal could be configured to render it as an entirely different color. Further,
 * not all styles are supported or enabled in every terminal so e.g. the (really annoying) styles like {@code blink} may do
 * nothing.
 * <p></p>
 * <h3>Colors</h3>
 * <ul>
 *     <li>{@code red}</li>
 *     <li>{@code green}</li>
 *     <li>{@code yellow}</li>
 *     <li>{@code blue}</li>
 *     <li>{@code magenta}</li>
 *     <li>{@code cyan}</li>
 *     <li>{@code white}</li>
 *     <li>{@code black}</li>
 *     <li>{@code default}</li>
 * </ul>
 * <p></p>
 * <h3>Background Colors</h3>
 * <ul>
 *     <li>{@code bg_red}</li>
 *     <li>{@code bg_green}</li>
 *     <li>{@code bg_yellow}</li>
 *     <li>{@code bg_blue}</li>
 *     <li>{@code bg_magenta}</li>
 *     <li>{@code bg_cyan}</li>
 *     <li>{@code bg_white}</li>
 *     <li>{@code bg_black}</li>
 *     <li>{@code bg_default}</li>
 * </ul>
 * <p></p>
 * <h3>Emphases</h3>
 * <ul>
 *     <li>{@code bold}</li>
 *     <li>{@code bright}</li>
 *     <li>{@code faint}</li>
 *     <li>{@code plain}</li>
 *     <li>{@code italic}</li>
 *     <li>{@code underline}</li>
 *     <li>{@code strikethrough}</li>
 *     <li>{@code negative}</li>
 *     <li>{@code conceal}</li>
 *     <li>{@code blink}</li>
 * </ul>
 * <p></p>
 * <h3>Aliases</h3>
 * <p></p>
 * Every color has the following aliases:
 * <ul>
 *      <li>Bold variant with an uppercase name (e.g. {@code RED})</li>
 *      <li>Bold variant with {@code '*'} prefix and suffix (e.g. {@code *red*})</li>
 *      <li>Italic variant with {@code '_'} prefix and suffix (e.g. {@code _red_})</li>
 *      <li>Bold italic variant with {@code '_*'} prefix and {@code '*_'} suffix (e.g. {@code _*red*_} or {@code *_red_*})</li>
 *      <li>Bright variants of the color and all the above with a {@code '!'} suffix
 *      (e.g. {@code red!}, {@code RED!}, {@code *red*!}, {@code _red_!}</li>
 * </ul>
 * <p></p>
 * Every background color has the following aliases:
 * <ul>
 *     <li> Bright variants with a {@code '!'} suffix (e.g. {@code bg_yellow!})</li>
 * </ul>
 * <p></p>
 * Use of {@code bold} is often preferable bold black or white as it is independent of the background color. Similarly,
 * {@code negative} may be preferable to background black or white.
 * <p></p>
 * <h3>Examples</h3>
 * <p></p>
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
 *         {@code "This is a bold red $(RED,bg_yellow! example) on a bright yellow background."}
 *     </li>
 * </ol>
 */
public class StyleRenderer {
    private static final String START_TOKEN = "$(";
    private static final int START_TOKEN_LEN = START_TOKEN.length();
    private static final char ESCAPE_CHAR = '\\';
    private static final char STYLES_SEP_CHAR = ' ';
    private static final String STYLE_SEP = ",";
    private static final char END_TOKEN_CHAR = ')';
    private static final String ESCAPED_END_TOKEN = "\\)";
    private static final String END_TOKEN = ")";

    private final String text;
    private final int textLength;
    private final Ansi ansi;
    private int textStart;
    private int tokenStart;
    private int tokenEnd;

    private StyleRenderer(String text, int tokenStart) {
        this.text = text;
        this.textLength = text.length();
        this.ansi = ansi();
        this.tokenStart = tokenStart;
    }

    /**
     * Substitute the DSL in the given text.
     *
     * @param text The text.
     * @return The substituted text.
     */
    public static String render(String text) {
        final int tokenStart = text.indexOf(START_TOKEN);
        if (tokenStart >= 0) {
            return new StyleRenderer(text, tokenStart).render();
        } else {
            return text;
        }
    }

    /**
     * Substitute the DSL after formatting.
     *
     * @param format The message format.
     * @param args The message arguments.
     * @return The message.
     */
    public static String render(String format, Object... args) {
        return render(String.format(format, args));
    }

    private String render() {
        while (tokenStart >= 0) {
            if (textStart < tokenStart) {
                ansi.a(text, textStart, tokenStart);
            }
            if (!replaceNext(tokenStart)) {
                break;
            }
        }
        if (textStart < textLength) {
            ansi.a(text.substring(textStart));
        }
        return ansi.toString();
    }

    private boolean replaceNext(int tokenStart) {
        final int stylesStart = tokenStart + START_TOKEN_LEN;
        final int stylesEnd = text.indexOf(STYLES_SEP_CHAR, stylesStart);
        if (stylesEnd < 0) {
            return false; // malformed
        }
        tokenEnd = tokenEnd(stylesEnd);
        if (tokenEnd < 0) {
            return false; // unclosed
        }
        return replaceNext(stylesStart, stylesEnd);
    }

    private boolean replaceNext(int stylesStart, int stylesEnd) {
        int nextTokenStart = tokenStart(stylesEnd);
        final boolean nested = nextTokenStart >= 0 && nextTokenStart < tokenEnd;
        final int styledTextStart = stylesEnd + 1;
        final int styledTextEnd = nested ? nextTokenStart : tokenEnd;
        final String styles = text.substring(stylesStart, stylesEnd);
        final String styledText = text.substring(styledTextStart, styledTextEnd);
        final String unescapedText = styledText.replace(ESCAPED_END_TOKEN, END_TOKEN);
        final Style style = Style.of(styles.split(STYLE_SEP));
        style.apply(ansi).a(unescapedText);

        if (nested) {
            if (replaceNext(nextTokenStart)) {
                style.apply(ansi);
                tokenEnd = tokenEnd(textStart);
                if (tokenEnd < 0) {
                    return false;
                }
                ansi.a(text, textStart, tokenEnd);
                nextTokenStart = tokenStart(tokenEnd);
            } else {
                return false;
            }
        }
        this.tokenStart = nextTokenStart;
        textStart = tokenEnd + 1;
        style.reset(ansi);
        return true;
    }

    private int tokenStart(int stylesEnd) {
        return text.indexOf(START_TOKEN, stylesEnd + 1);
    }

    private int tokenEnd(int stylesEnd) {
        while (stylesEnd < textLength) {
            int tokenEnd = text.indexOf(END_TOKEN_CHAR, stylesEnd);
            if (tokenEnd < 0) {
                break;
            } else if (text.charAt(tokenEnd - 1) != ESCAPE_CHAR) {
                return tokenEnd;
            } else {
                stylesEnd = tokenEnd + 1;
            }
        }
        return -1;
    }
}
