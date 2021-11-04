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

package io.helidon.build.archetype.engine.v2.markdown;

class LinkScanner {

    private LinkScanner() {

    }

    /**
     * Attempt to scan the contents of a link label (inside the brackets), stopping after the content or returning false.
     * The stopped position can bei either the closing {@code ]}, or the end of the line if the label continues on
     * the next line.
     */
    public static boolean scanLinkLabelContent(Scanner scanner) {
        while (scanner.hasNext()) {
            switch (scanner.peek()) {
                case '\\':
                    scanner.next();
                    if (Parsing.isEscapable(scanner.peek())) {
                        scanner.next();
                    }
                    break;
                case ']':
                    return true;
                case '[':
                    return false;
                default:
                    scanner.next();
            }
        }
        return true;
    }

    /**
     * Attempt to scan a link destination, stopping after the destination or returning false.
     */
    public static boolean scanLinkDestination(Scanner scanner) {
        if (!scanner.hasNext()) {
            return false;
        }

        if (scanner.next('<')) {
            while (scanner.hasNext()) {
                switch (scanner.peek()) {
                    case '\\':
                        scanner.next();
                        if (Parsing.isEscapable(scanner.peek())) {
                            scanner.next();
                        }
                        break;
                    case '\n':
                    case '<':
                        return false;
                    case '>':
                        scanner.next();
                        return true;
                    default:
                        scanner.next();
                }
            }
            return false;
        } else {
            return scanLinkDestinationWithBalancedParens(scanner);
        }
    }

    public static boolean scanLinkTitle(Scanner scanner) {
        if (!scanner.hasNext()) {
            return false;
        }

        char endDelimiter;
        switch (scanner.peek()) {
            case '"':
                endDelimiter = '"';
                break;
            case '\'':
                endDelimiter = '\'';
                break;
            case '(':
                endDelimiter = ')';
                break;
            default:
                return false;
        }
        scanner.next();

        if (!scanLinkTitleContent(scanner, endDelimiter)) {
            return false;
        }
        if (!scanner.hasNext()) {
            return false;
        }
        scanner.next();
        return true;
    }

    public static boolean scanLinkTitleContent(Scanner scanner, char endDelimiter) {
        while (scanner.hasNext()) {
            char c = scanner.peek();
            if (c == '\\') {
                scanner.next();
                if (Parsing.isEscapable(scanner.peek())) {
                    scanner.next();
                }
            } else if (c == endDelimiter) {
                return true;
            } else if (endDelimiter == ')' && c == '(') {
                return false;
            } else {
                scanner.next();
            }
        }
        return true;
    }

    private static boolean scanLinkDestinationWithBalancedParens(Scanner scanner) {
        int parens = 0;
        boolean empty = true;
        while (scanner.hasNext()) {
            char c = scanner.peek();
            switch (c) {
                case ' ':
                    return !empty;
                case '\\':
                    scanner.next();
                    if (Parsing.isEscapable(scanner.peek())) {
                        scanner.next();
                    }
                    break;
                case '(':
                    parens++;
                    if (parens > 32) {
                        return false;
                    }
                    scanner.next();
                    break;
                case ')':
                    if (parens == 0) {
                        return true;
                    } else {
                        parens--;
                    }
                    scanner.next();
                    break;
                default:
                    if (Character.isISOControl(c)) {
                        return !empty;
                    }
                    scanner.next();
                    break;
            }
            empty = false;
        }
        return true;
    }
}
