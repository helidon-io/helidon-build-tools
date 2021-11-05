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

package io.helidon.build.common.markdown;

class Parsing {

    private static final int CODE_BLOCK_INDENT = 4;

    private Parsing() {

    }

    public static int codeBlockIndent() {
        return CODE_BLOCK_INDENT;
    }

    public static int columnsToNextTabStop(int column) {
        return 4 - (column % 4);
    }

    public static int find(char c, CharSequence s, int startIndex) {
        int length = s.length();
        for (int i = startIndex; i < length; i++) {
            if (s.charAt(i) == c) {
                return i;
            }
        }
        return -1;
    }

    public static int findLineBreak(CharSequence s, int startIndex) {
        int length = s.length();
        for (int i = startIndex; i < length; i++) {
            switch (s.charAt(i)) {
                case '\n':
                case '\r':
                    return i;
                default:
            }
        }
        return -1;
    }

    public static boolean hasNonSpace(CharSequence s) {
        int length = s.length();
        int skipped = skip(' ', s, 0, length);
        return skipped != length;
    }

    public static boolean letter(CharSequence s, int index) {
        int codePoint = Character.codePointAt(s, index);
        return Character.isLetter(codePoint);
    }

    public static boolean escapable(char c) {
        switch (c) {
            case '!':
            case '"':
            case '#':
            case '$':
            case '%':
            case '&':
            case '\'':
            case '(':
            case ')':
            case '*':
            case '+':
            case ',':
            case '-':
            case '.':
            case '/':
            case ':':
            case ';':
            case '<':
            case '=':
            case '>':
            case '?':
            case '@':
            case '[':
            case '\\':
            case ']':
            case '^':
            case '_':
            case '`':
            case '{':
            case '|':
            case '}':
            case '~':
                return true;
            default:
        }
        return false;
    }

    public static boolean punctuationCodePoint(int codePoint) {
        switch (Character.getType(codePoint)) {
            case Character.CONNECTOR_PUNCTUATION:
            case Character.DASH_PUNCTUATION:
            case Character.END_PUNCTUATION:
            case Character.FINAL_QUOTE_PUNCTUATION:
            case Character.INITIAL_QUOTE_PUNCTUATION:
            case Character.OTHER_PUNCTUATION:
            case Character.START_PUNCTUATION:
                return true;
            default:
                switch (codePoint) {
                    case '$':
                    case '+':
                    case '<':
                    case '=':
                    case '>':
                    case '^':
                    case '`':
                    case '|':
                    case '~':
                        return true;
                    default:
                        return false;
                }
        }
    }

    public static boolean whitespaceCodePoint(int codePoint) {
        switch (codePoint) {
            case ' ':
            case '\t':
            case '\r':
            case '\n':
            case '\f':
                return true;
            default:
                return Character.getType(codePoint) == Character.SPACE_SEPARATOR;
        }
    }

    /**
     * Prepares the input line replacing {@code \0}.
     */
    public static CharSequence prepareLine(CharSequence line) {
        StringBuilder sb = null;
        int length = line.length();
        for (int i = 0; i < length; i++) {
            char c = line.charAt(i);
            if (c == '\0') {
                if (sb == null) {
                    sb = new StringBuilder(length);
                    sb.append(line, 0, i);
                }
                sb.append('\uFFFD');
            } else {
                if (sb != null) {
                    sb.append(c);
                }
            }
        }

        if (sb != null) {
            return sb.toString();
        } else {
            return line;
        }
    }

    public static int skip(char skip, CharSequence s, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            if (s.charAt(i) != skip) {
                return i;
            }
        }
        return endIndex;
    }

    public static int skipBackwards(char skip, CharSequence s, int startIndex, int lastIndex) {
        for (int i = startIndex; i >= lastIndex; i--) {
            if (s.charAt(i) != skip) {
                return i;
            }
        }
        return lastIndex - 1;
    }

    public static int skipSpaceTab(CharSequence s, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            switch (s.charAt(i)) {
                case ' ':
                case '\t':
                    break;
                default:
                    return i;
            }
        }
        return endIndex;
    }

    public static int skipSpaceTabBackwards(CharSequence s, int startIndex, int lastIndex) {
        for (int i = startIndex; i >= lastIndex; i--) {
            switch (s.charAt(i)) {
                case ' ':
                case '\t':
                    break;
                default:
                    return i;
            }
        }
        return lastIndex - 1;
    }

}
