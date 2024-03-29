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

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Escaping {

    private Escaping() {

    }

    public static final String ESCAPABLE = "[!\"#$%&\'()*+,./:;<=>?@\\[\\\\\\]^_`{|}~-]";

    public static final String ENTITY = "&(?:#x[a-f0-9]{1,6}|#[0-9]{1,7}|[a-z][a-z0-9]{1,31});";

    private static final Pattern BACKSLASH_OR_AMP = Pattern.compile("[\\\\&]");

    private static final Pattern ENTITY_OR_ESCAPED_CHAR =
            Pattern.compile("\\\\" + ESCAPABLE + '|' + ENTITY, Pattern.CASE_INSENSITIVE);

    // From RFC 3986 (see "reserved", "unreserved") except don't escape '[' or ']' to be compatible with JS encodeURI
    private static final Pattern ESCAPE_IN_URI =
            Pattern.compile("(%[a-fA-F0-9]{0,2}|[^:/?#@!$&'()*+,;=a-zA-Z0-9\\-._~])");

    private static final char[] HEX_DIGITS =
            new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static final Replacer UNESCAPE_REPLACER = (input, sb) -> {
        if (input.charAt(0) == '\\') {
            sb.append(input, 1, input.length());
        }
    };

    private static final Replacer URI_REPLACER = (input, sb) -> {
        if (input.startsWith("%")) {
            if (input.length() == 3) {
                sb.append(input);
            } else {
                // %25 is the percent-encoding for %
                sb.append("%25");
                sb.append(input, 1, input.length());
            }
        } else {
            byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
            for (byte b : bytes) {
                sb.append('%');
                sb.append(HEX_DIGITS[(b >> 4) & 0xF]);
                sb.append(HEX_DIGITS[b & 0xF]);
            }
        }
    };

    public static String escapeHtml(String input) {
        StringBuilder sb = null;

        loop:
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            String replacement;
            switch (c) {
                case '&':
                    replacement = "&amp;";
                    break;
                case '<':
                    replacement = "&lt;";
                    break;
                case '>':
                    replacement = "&gt;";
                    break;
                case '\"':
                    replacement = "&quot;";
                    break;
                default:
                    if (sb != null) {
                        sb.append(c);
                    }
                    continue loop;
            }
            if (sb == null) {
                sb = new StringBuilder();
                sb.append(input, 0, i);
            }
            sb.append(replacement);
        }

        return sb != null ? sb.toString() : input;
    }

    /**
     * Replace entities and backslash escapes with literal characters.
     */
    public static String unescapeString(String s) {
        if (BACKSLASH_OR_AMP.matcher(s).find()) {
            return replaceAll(ENTITY_OR_ESCAPED_CHAR, s, UNESCAPE_REPLACER);
        } else {
            return s;
        }
    }

    public static String percentEncodeUrl(String s) {
        return replaceAll(ESCAPE_IN_URI, s, URI_REPLACER);
    }

    private static String replaceAll(Pattern p, String s, Replacer replacer) {
        Matcher matcher = p.matcher(s);

        if (!matcher.find()) {
            return s;
        }

        StringBuilder sb = new StringBuilder(s.length() + 16);
        int lastEnd = 0;
        do {
            sb.append(s, lastEnd, matcher.start());
            replacer.replace(matcher.group(), sb);
            lastEnd = matcher.end();
        } while (matcher.find());

        if (lastEnd != s.length()) {
            sb.append(s, lastEnd, s.length());
        }
        return sb.toString();
    }

    private interface Replacer {
        void replace(String input, StringBuilder sb);
    }
}
