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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class InlineParser {

    private final BitSet specialCharacters;
    private final Map<Character, DelimiterProcessor> delimiterProcessors;
    private final InlineParserContext context;
    private final Map<Character, List<InlineContentParser>> inlineParsers;

    private Scanner scanner;

    /**
     * Top delimiter (emphasis, strong emphasis or custom emphasis). (Brackets are on a separate stack, different
     * from the algorithm described in the spec.)
     */
    private Delimiter lastDelimiter;

    /**
     * Top opening bracket (<code>[</code> or <code>![)</code>).
     */
    private Bracket lastBracket;

    InlineParser(InlineParserContext inlineParserContext) {
        this.delimiterProcessors = calculateDelimiterProcessors(inlineParserContext.delimiterProcessors());

        this.context = inlineParserContext;
        this.inlineParsers = new HashMap<>();
        this.inlineParsers.put('`', Collections.singletonList(new BackticksInlineParser()));

        this.specialCharacters = calculateSpecialCharacters(this.delimiterProcessors.keySet(), inlineParsers.keySet());
    }

    public static BitSet calculateSpecialCharacters(Set<Character> delimiterCharacters, Set<Character> characters) {
        BitSet bitSet = new BitSet();
        for (Character c : delimiterCharacters) {
            bitSet.set(c);
        }
        for (Character c : characters) {
            bitSet.set(c);
        }
        bitSet.set('[');
        bitSet.set(']');
        return bitSet;
    }

    public static Map<Character, DelimiterProcessor> calculateDelimiterProcessors(List<DelimiterProcessor> delimiterProcessors) {
        Map<Character, DelimiterProcessor> map = new HashMap<>();
        addDelimiterProcessors(Arrays.asList(new AsteriskDelimiterProcessor(), new UnderscoreDelimiterProcessor()), map);
        addDelimiterProcessors(delimiterProcessors, map);
        return map;
    }

    /**
     * Return a scanner for the input for the current position (on the character that the inline parser registered
     * interest for).
     */
    public Scanner scanner() {
        return scanner;
    }

    private static void addDelimiterProcessors(Iterable<DelimiterProcessor> delimiterProcessors,
                                               Map<Character, DelimiterProcessor> map) {
        for (DelimiterProcessor delimiterProcessor : delimiterProcessors) {
            char opening = delimiterProcessor.openingCharacter();
            char closing = delimiterProcessor.closingCharacter();
            if (opening == closing) {
                addDelimiterProcessorForChar(opening, delimiterProcessor, map);
            } else {
                addDelimiterProcessorForChar(opening, delimiterProcessor, map);
                addDelimiterProcessorForChar(closing, delimiterProcessor, map);
            }
        }
    }

    private static void addDelimiterProcessorForChar(char delimiterChar, DelimiterProcessor toAdd,
                                                     Map<Character, DelimiterProcessor> delimiterProcessors) {
        DelimiterProcessor existing = delimiterProcessors.put(delimiterChar, toAdd);
        if (existing != null) {
            throw new IllegalArgumentException("Delimiter processor conflict with delimiter char '" + delimiterChar + "'");
        }
    }

    /**
     * Parse content in block into inline children, appending them to the block node.
     *
     * @param lines the source content to parse as inline
     * @param block the node to append resulting nodes to (as children)
     */
    public void parse(SourceLines lines, Node block) {
        reset(lines);

        while (true) {
            List<? extends Node> nodes = parseInline();
            if (nodes != null) {
                for (Node node : nodes) {
                    block.appendChild(node);
                }
            } else {
                break;
            }
        }

        processDelimiters(null);
        mergeChildTextNodes(block);
    }

    void reset(SourceLines lines) {
        this.scanner = Scanner.of(lines);
        this.lastDelimiter = null;
        this.lastBracket = null;
    }

    private Text text(SourceLines sourceLines) {
        return new Text(sourceLines.content());
    }

    /**
     * Parse the next inline element in subject, advancing our position.
     * On success, return the new inline node.
     * On failure, return null.
     */
    private List<? extends Node> parseInline() {
        char c = scanner.peek();

        switch (c) {
            case '[':
                return Collections.singletonList(parseOpenBracket());
            case ']':
                return Collections.singletonList(parseCloseBracket());
            case Scanner.END:
                return null;
            default:
        }

        if (!specialCharacters.get(c)) {
            return Collections.singletonList(parseText());
        }

        List<InlineContentParser> inlineParsers = this.inlineParsers.get(c);
        if (inlineParsers != null) {
            Position position = scanner.position();
            for (InlineContentParser inlineParser : inlineParsers) {
                ParsedInline parsedInline = inlineParser.tryParse(this);
                if (parsedInline != null) {
                    Node node = parsedInline.node();
                    scanner.position(parsedInline.position());
                    return Collections.singletonList(node);
                } else {
                    scanner.position(position);
                }
            }
        }

        DelimiterProcessor delimiterProcessor = delimiterProcessors.get(c);
        if (delimiterProcessor != null) {
            List<? extends Node> nodes = parseDelimiters(delimiterProcessor, c);
            if (nodes != null) {
                return nodes;
            }
        }

        return Collections.singletonList(parseText());
    }

    /**
     * Attempt to parse delimiters like emphasis, strong emphasis or custom delimiters.
     */
    private List<? extends Node> parseDelimiters(
            DelimiterProcessor delimiterProcessor, char delimiterChar) {
        InlineParser.DelimiterData res = scanDelimiters(delimiterProcessor, delimiterChar);
        if (res == null) {
            return null;
        }

        List<Text> characters = res.characters;

        lastDelimiter = new Delimiter(characters, delimiterChar, res.canOpen, res.canClose, lastDelimiter);
        if (lastDelimiter.previous() != null) {
            lastDelimiter.previous().next(lastDelimiter);
        }

        return characters;
    }

    /**
     * Add open bracket to delimiter stack and add a text node to block's children.
     */
    private Node parseOpenBracket() {
        Position start = scanner.position();
        scanner.next();
        Position contentPosition = scanner.position();

        Text node = text(scanner.source(start, contentPosition));

        addBracket(Bracket.link(node, start, contentPosition, lastBracket, lastDelimiter));

        return node;
    }

    /**
     * Try to match close bracket against an opening in the delimiter stack. Return either a link or image, or a
     * plain [ character. If there is a matching delimiter, remove it from the delimiter stack.
     */
    private Node parseCloseBracket() {
        Position beforeClose = scanner.position();
        scanner.next();
        Position afterClose = scanner.position();

        Bracket opener = lastBracket;
        if (opener == null) {
            return text(scanner.source(beforeClose, afterClose));
        }

        if (!opener.allowed()) {
            removeLastBracket();
            return text(scanner.source(beforeClose, afterClose));
        }

        String dest = null;
        String title = null;

        if (scanner.next('(')) {
            scanner.whitespace();
            dest = parseLinkDestination(scanner);
            if (dest == null) {
                scanner.position(afterClose);
            } else {
                int whitespace = scanner.whitespace();
                if (whitespace >= 1) {
                    title = parseLinkTitle(scanner);
                    scanner.whitespace();
                }
                if (!scanner.next(')')) {
                    scanner.position(afterClose);
                    dest = null;
                    title = null;
                }
            }
        }

        if (dest == null) {
            String ref = parseLinkLabel(scanner);
            if (ref == null) {
                scanner.position(afterClose);
            }
            if ((ref == null || ref.isEmpty()) && !opener.bracketAfter()) {
                ref = scanner.source(opener.contentPosition(), beforeClose).content();
            }
        }

        if (dest != null) {
            Node link = new Link(dest, title);

            Node node = opener.node().next();
            while (node != null) {
                Node next = node.next();
                link.appendChild(node);
                node = next;
            }

            processDelimiters(opener.previousDelimiter());
            mergeChildTextNodes(link);
            opener.node().unlink();
            removeLastBracket();

            Bracket bracket = lastBracket;
            while (bracket != null) {
                bracket.allowed(false);
                bracket = bracket.previous();
            }

            return link;

        } else {
            removeLastBracket();

            scanner.position(afterClose);
            return text(scanner.source(beforeClose, afterClose));
        }
    }

    private void addBracket(Bracket bracket) {
        if (lastBracket != null) {
            lastBracket.bracketAfter(true);
        }
        lastBracket = bracket;
    }

    private void removeLastBracket() {
        lastBracket = lastBracket.previous();
    }

    /**
     * Attempt to parse link destination, returning the string or null if no match.
     */
    private String parseLinkDestination(Scanner scanner) {
        char delimiter = scanner.peek();
        Position start = scanner.position();
        if (!LinkScanner.scanLinkDestination(scanner)) {
            return null;
        }

        String dest;
        if (delimiter == '<') {
            String rawDestination = scanner.source(start, scanner.position()).content();
            dest = rawDestination.substring(1, rawDestination.length() - 1);
        } else {
            dest = scanner.source(start, scanner.position()).content();
        }

        return Escaping.unescapeString(dest);
    }

    /**
     * Attempt to parse link title (sans quotes), returning the string or null if no match.
     */
    private String parseLinkTitle(Scanner scanner) {
        Position start = scanner.position();
        if (!LinkScanner.scanLinkTitle(scanner)) {
            return null;
        }

        String rawTitle = scanner.source(start, scanner.position()).content();
        String title = rawTitle.substring(1, rawTitle.length() - 1);
        return Escaping.unescapeString(title);
    }

    /**
     * Attempt to parse a link label, returning the label between the brackets or null.
     */
    String parseLinkLabel(Scanner scanner) {
        if (!scanner.next('[')) {
            return null;
        }

        Position start = scanner.position();
        if (!LinkScanner.scanLinkLabelContent(scanner)) {
            return null;
        }
        Position end = scanner.position();

        if (!scanner.next(']')) {
            return null;
        }

        String content = scanner.source(start, end).content();
        if (content.length() > 999) {
            return null;
        }

        return content;
    }

    /**
     * Parse the next character as plain text, and possibly more if the following characters are non-special.
     */
    private Node parseText() {
        Position start = scanner.position();
        scanner.next();
        char c;
        while (true) {
            c = scanner.peek();
            if (c == Scanner.END || specialCharacters.get(c)) {
                break;
            }
            scanner.next();
        }

        SourceLines source = scanner.source(start, scanner.position());
        String content = source.content();

        if (c == '\n') {
            int end = Parsing.skipBackwards(' ', content, content.length() - 1, 0) + 1;
            content = content.substring(0, end);
        } else if (c == Scanner.END) {
            int end = Parsing.skipSpaceTabBackwards(content, content.length() - 1, 0) + 1;
            content = content.substring(0, end);
        }

        return new Text(content);
    }

    /**
     * Scan a sequence of characters with code delimiterChar, and return information about the number of delimiters
     * and whether they are positioned such that they can open and/or close emphasis or strong emphasis.
     *
     * @return information about delimiter run, or {@code null}
     */
    private InlineParser.DelimiterData scanDelimiters(
            DelimiterProcessor delimiterProcessor, char delimiterChar) {
        int before = scanner.peekPreviousCodePoint();
        Position start = scanner.position();

        int delimiterCount = scanner.matchMultiple(delimiterChar);
        if (delimiterCount < delimiterProcessor.minLength()) {
            scanner.position(start);
            return null;
        }

        List<Text> delimiters = new ArrayList<>();
        scanner.position(start);
        Position positionBefore = start;
        while (scanner.next(delimiterChar)) {
            delimiters.add(text(scanner.source(positionBefore, scanner.position())));
            positionBefore = scanner.position();
        }

        int after = scanner.peekCodePoint();

        boolean beforeIsPunctuation = before == Scanner.END || Parsing.punctuationCodePoint(before);
        boolean beforeIsWhitespace = before == Scanner.END || Parsing.whitespaceCodePoint(before);
        boolean afterIsPunctuation = after == Scanner.END || Parsing.punctuationCodePoint(after);
        boolean afterIsWhitespace = after == Scanner.END || Parsing.whitespaceCodePoint(after);

        boolean leftFlanking = !afterIsWhitespace
                && (!afterIsPunctuation || beforeIsWhitespace || beforeIsPunctuation);
        boolean rightFlanking = !beforeIsWhitespace
                && (!beforeIsPunctuation || afterIsWhitespace || afterIsPunctuation);
        boolean canOpen;
        boolean canClose;
        if (delimiterChar == '_') {
            canOpen = leftFlanking && (!rightFlanking || beforeIsPunctuation);
            canClose = rightFlanking && (!leftFlanking || afterIsPunctuation);
        } else {
            canOpen = leftFlanking && delimiterChar == delimiterProcessor.openingCharacter();
            canClose = rightFlanking && delimiterChar == delimiterProcessor.closingCharacter();
        }

        return new InlineParser.DelimiterData(delimiters, canOpen, canClose);
    }

    private void processDelimiters(Delimiter stackBottom) {

        Map<Character, Delimiter> openersBottom = new HashMap<>();

        Delimiter closer = lastDelimiter;
        while (closer != null && closer.previous() != stackBottom) {
            closer = closer.previous();
        }
        while (closer != null) {
            char delimiterChar = closer.delimiterChar();

            DelimiterProcessor delimiterProcessor = delimiterProcessors.get(delimiterChar);
            if (!closer.canClose() || delimiterProcessor == null) {
                closer = closer.next();
                continue;
            }

            char openingDelimiterChar = delimiterProcessor.openingCharacter();

            int usedDelims = 0;
            boolean openerFound = false;
            boolean potentialOpenerFound = false;
            Delimiter opener = closer.previous();
            while (opener != null && opener != stackBottom && opener != openersBottom.get(delimiterChar)) {
                if (opener.canOpen() && opener.delimiterChar() == openingDelimiterChar) {
                    potentialOpenerFound = true;
                    usedDelims = delimiterProcessor.process(opener, closer);
                    if (usedDelims > 0) {
                        openerFound = true;
                        break;
                    }
                }
                opener = opener.previous();
            }

            if (!openerFound) {
                if (!potentialOpenerFound) {
                    openersBottom.put(delimiterChar, closer.previous());
                    if (!closer.canOpen()) {
                        removeDelimiterKeepNode(closer);
                    }
                }
                closer = closer.next();
                continue;
            }

            for (int i = 0; i < usedDelims; i++) {
                Text delimiter = opener.characters().remove(opener.characters().size() - 1);
                delimiter.unlink();
            }
            for (int i = 0; i < usedDelims; i++) {
                Text delimiter = closer.characters().remove(0);
                delimiter.unlink();
            }

            removeDelimitersBetween(opener, closer);

            if (opener.length() == 0) {
                removeDelimiterAndNodes(opener);
            }

            if (closer.length() == 0) {
                Delimiter next = closer.next();
                removeDelimiterAndNodes(closer);
                closer = next;
            }
        }

        while (lastDelimiter != null && lastDelimiter != stackBottom) {
            removeDelimiterKeepNode(lastDelimiter);
        }
    }

    private void removeDelimitersBetween(Delimiter opener, Delimiter closer) {
        Delimiter delimiter = closer.previous();
        while (delimiter != null && delimiter != opener) {
            Delimiter previousDelimiter = delimiter.previous();
            removeDelimiterKeepNode(delimiter);
            delimiter = previousDelimiter;
        }
    }

    /**
     * Remove the delimiter and the corresponding text node. For used delimiters, e.g. `*` in `*foo*`.
     */
    private void removeDelimiterAndNodes(Delimiter delim) {
        removeDelimiter(delim);
    }

    /**
     * Remove the delimiter but keep the corresponding node as text. For unused delimiters such as `_` in `foo_bar`.
     */
    private void removeDelimiterKeepNode(Delimiter delim) {
        removeDelimiter(delim);
    }

    private void removeDelimiter(Delimiter delim) {
        if (delim.previous() != null) {
            delim.previous().next(delim.next());
        }
        if (delim.next() == null) {
            lastDelimiter = delim.previous();
        } else {
            delim.next().previous(delim.previous());
        }
    }

    private void mergeChildTextNodes(Node node) {
        if (node.firstChild() == null) {
            return;
        }

        mergeTextNodesInclusive(node.firstChild(), node.lastChild());
    }

    private void mergeTextNodesInclusive(Node fromNode, Node toNode) {
        Text first = null;
        Text last = null;
        int length = 0;

        Node node = fromNode;
        while (node != null) {
            if (node instanceof Text) {
                Text text = (Text) node;
                if (first == null) {
                    first = text;
                }
                length += text.literal().length();
                last = text;
            } else {
                mergeIfNeeded(first, last, length);
                first = null;
                last = null;
                length = 0;

                mergeChildTextNodes(node);
            }
            if (node == toNode) {
                break;
            }
            node = node.next();
        }

        mergeIfNeeded(first, last, length);
    }

    private void mergeIfNeeded(Text first, Text last, int textLength) {
        if (first != null && last != null && first != last) {
            StringBuilder sb = new StringBuilder(textLength);
            sb.append(first.literal());
            Node node = first.next();
            Node stop = last.next();
            while (node != stop) {
                sb.append(((Text) node).literal());

                Node unlink = node;
                node = node.next();
                unlink.unlink();
            }
            String literal = sb.toString();
            first.literal(literal);
        }
    }

    private static class DelimiterData {

        private final List<Text> characters;
        private final boolean canClose;
        private final boolean canOpen;

        DelimiterData(List<Text> characters, boolean canOpen, boolean canClose) {
            this.characters = characters;
            this.canOpen = canOpen;
            this.canClose = canClose;
        }
    }
}
