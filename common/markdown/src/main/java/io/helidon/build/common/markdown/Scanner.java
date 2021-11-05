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

import java.util.List;

class Scanner {

    /**
     * Character representing the end of input source (or outside of the text in case of the "previous" methods).
     * <p>
     * Note that we can use NULL to represent this because CommonMark does not allow those in the input (we replace them
     * in the beginning of parsing).
     */
    public static final char END = '\0';

    private final List<SourceLine> lines;
    private int lineIndex;
    private int index;

    private SourceLine line = SourceLine.of("");
    private int lineLength = 0;

    Scanner(List<SourceLine> lines, int lineIndex, int index) {
        this.lines = lines;
        this.lineIndex = lineIndex;
        this.index = index;
        if (!lines.isEmpty()) {
            checkPosition(lineIndex, index);
            line(lines.get(lineIndex));
        }
    }

    public static Scanner of(SourceLines lines) {
        return new Scanner(lines.lines(), 0, 0);
    }

    public char peek() {
        if (index < lineLength) {
            return line.content().charAt(index);
        } else {
            if (lineIndex < lines.size() - 1) {
                return '\n';
            } else {
                return END;
            }
        }
    }

    public int peekCodePoint() {
        if (index < lineLength) {
            char c = line.content().charAt(index);
            if (Character.isHighSurrogate(c) && index + 1 < lineLength) {
                char low = line.content().charAt(index + 1);
                if (Character.isLowSurrogate(low)) {
                    return Character.toCodePoint(c, low);
                }
            }
            return c;
        } else {
            if (lineIndex < lines.size() - 1) {
                return '\n';
            } else {
                return END;
            }
        }
    }

    public int peekPreviousCodePoint() {
        if (index > 0) {
            int prev = index - 1;
            char c = line.content().charAt(prev);
            if (Character.isLowSurrogate(c) && prev > 0) {
                char high = line.content().charAt(prev - 1);
                if (Character.isHighSurrogate(high)) {
                    return Character.toCodePoint(high, c);
                }
            }
            return c;
        } else {
            if (lineIndex > 0) {
                return '\n';
            } else {
                return END;
            }
        }
    }

    public boolean hasNext() {
        if (index < lineLength) {
            return true;
        } else {
            return lineIndex < lines.size() - 1;
        }
    }

    public void next() {
        index++;
        if (index > lineLength) {
            lineIndex++;
            if (lineIndex < lines.size()) {
                line(lines.get(lineIndex));
            } else {
                line(SourceLine.of(""));
            }
            index = 0;
        }
    }

    /**
     * Check if the specified char is next and advance the position.
     *
     * @param c the char to check (including newline characters)
     * @return true if matched and position was advanced, false otherwise
     */
    public boolean next(char c) {
        if (peek() == c) {
            next();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if we have the specified content on the line and advanced the position. Note that if you want to match
     * newline characters, use {@link #next(char)}.
     *
     * @param content the text content to match on a single line (excluding newline characters)
     * @return true if matched and position was advanced, false otherwise
     */
    public boolean next(String content) {
        if (index < lineLength && index + content.length() <= lineLength) {
            for (int i = 0; i < content.length(); i++) {
                if (line.content().charAt(index + i) != content.charAt(i)) {
                    return false;
                }
            }
            index += content.length();
            return true;
        } else {
            return false;
        }
    }

    public int matchMultiple(char c) {
        int count = 0;
        while (peek() == c) {
            count++;
            next();
        }
        return count;
    }

    public int whitespace() {
        int count = 0;
        while (true) {
            switch (peek()) {
                case ' ':
                case '\t':
                case '\n':
                case '\u000B':
                case '\f':
                case '\r':
                    count++;
                    next();
                    break;
                default:
                    return count;
            }
        }
    }

    public int find(char c) {
        int count = 0;
        while (true) {
            char cur = peek();
            if (cur == Scanner.END) {
                return -1;
            } else if (cur == c) {
                return count;
            }
            count++;
            next();
        }
    }

    public Position position() {
        return new Position(lineIndex, index);
    }

    public void position(Position position) {
        checkPosition(position.lineIndex(), position.index());
        this.lineIndex = position.lineIndex();
        this.index = position.index();
        line(lines.get(this.lineIndex));
    }

    public SourceLines source(Position begin, Position end) {
        if (begin.lineIndex() == end.lineIndex()) {
            SourceLine line = lines.get(begin.lineIndex());
            CharSequence newContent = line.content().subSequence(begin.index(), end.index());
            return SourceLines.of(SourceLine.of(newContent));
        } else {
            SourceLines sourceLines = SourceLines.empty();

            SourceLine firstLine = lines.get(begin.lineIndex());
            sourceLines.addLine(firstLine.substring(begin.index(), firstLine.content().length()));

            for (int line = begin.lineIndex() + 1; line < end.lineIndex(); line++) {
                sourceLines.addLine(lines.get(line));
            }

            SourceLine lastLine = lines.get(end.lineIndex());
            sourceLines.addLine(lastLine.substring(0, end.index()));
            return sourceLines;
        }
    }

    private void line(SourceLine line) {
        this.line = line;
        this.lineLength = line.content().length();
    }

    private void checkPosition(int lineIndex, int index) {
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            throw new IllegalArgumentException("Line index " + lineIndex + " out of range, number of lines: " + lines.size());
        }
        SourceLine line = lines.get(lineIndex);
        if (index < 0 || index > line.content().length()) {
            throw new IllegalArgumentException("Index " + index + " out of range, line length: " + line.content().length());
        }
    }
}
