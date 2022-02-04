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
package io.helidon.build.common;

import io.helidon.build.common.RichTextProvider.Holder;
import io.helidon.build.common.RichTextStyle.StyleList;

/**
 * A string formatter with a substitutions for rich text. The syntax that is easy to read and write.
 * <br><br>
 * Colors and styles are applied to text enclosed by {@code "$("} and {@code ")"}, e.g.:
 * <br><br>
 * <pre>
 *    "Here is $(red styled) text"
 * </pre>
 * In this example, the word {@code styled} will (normally) appear in red. If the styled text itself contains parentheses,
 * the closing paren should be escaped with a backslash:
 * <br><br>
 * <pre>
 *    "Here is $(red (and example of\\) styled) text"
 * </pre>
 * The DSL syntax is:
 * <br><br>
 * <pre>
 *   $(style[,style]* text)
 * </pre>
 * where {@code style} is a case-sensitive style name.
 * Nesting is supported.
 */
public class RichTextRenderer {

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
    private final RichText richText;
    private int textStart;
    private int tokenStart;
    private int tokenEnd;
    private RichTextStyle nestedStyle;

    private RichTextRenderer(String text, int tokenStart) {
        this.text = text;
        this.richText = Holder.INSTANCE.richText();
        this.textLength = text.length();
        this.tokenStart = tokenStart;
        this.nestedStyle = RichTextStyle.NONE;
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
            return new RichTextRenderer(text, tokenStart).render();
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
                richText.append(text, textStart, tokenStart);
            }
            if (!replaceNext(tokenStart)) {
                break;
            }
        }
        if (textStart < textLength) {
            richText.append(text.substring(textStart));
        }
        return richText.text();
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
        final String styleNames = text.substring(stylesStart, stylesEnd);
        final String styledText = text.substring(styledTextStart, styledTextEnd);
        final String unescapedText = styledText.replace(ESCAPED_END_TOKEN, END_TOKEN);
        RichTextStyle style = Holder.INSTANCE.styleOf(styleNames.split(STYLE_SEP));
        style.apply(richText).append(unescapedText);

        if (nested) {
            push(style);
            if (replaceNext(nextTokenStart)) {
                pop();
                tokenEnd = tokenEnd(textStart);
                if (tokenEnd < 0) {
                    style.reset(richText);
                    return false;
                }
                richText.append(text, textStart, tokenEnd);
                nextTokenStart = tokenStart(tokenEnd);
            } else {
                return false;
            }
        }
        tokenStart = nextTokenStart;
        textStart = tokenEnd + 1;
        style.reset(richText);
        return true;
    }

    private void push(RichTextStyle style) {
        if (nestedStyle == RichTextStyle.NONE) {
            nestedStyle = style;
        } else if (!(nestedStyle instanceof StyleList)) {
            nestedStyle = new StyleList(nestedStyle).add(style);
        } else {
            ((StyleList) nestedStyle).add(style);
        }
    }

    private void pop() {
        nestedStyle.apply(richText);
        if (nestedStyle instanceof StyleList) {
            ((StyleList) nestedStyle).pop();
        }
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
