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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * Allows http, https and mailto protocols for url.
 * Also allows protocol relative urls, and relative urls.
 * Implementation based on https://github.com/OWASP/java-html-sanitizer/blob/f07e44b034a45d94d6fd010279073c38b6933072/src/main/java/org/owasp/html/FilterUrlByProtocolAttributePolicy.java
 */
class UrlSanitizer {
    private final Set<String> protocols;

    UrlSanitizer() {
        this(Arrays.asList("http", "https", "mailto"));
    }

    UrlSanitizer(Collection<String> protocols) {
        this.protocols = new HashSet<>(protocols);
    }

    public String sanitizeLinkUrl(String url) {
        url = stripHtmlSpaces(url);
        protocol_loop:
        for (int i = 0, n = url.length(); i < n; ++i) {
            switch (url.charAt(i)) {
                case '/':
                case '#':
                case '?':  // No protocol.
                    break protocol_loop;
                case ':':
                    String protocol = url.substring(0, i).toLowerCase();
                    if (!protocols.contains(protocol)) {
                        return "";
                    }
                    break protocol_loop;
                default:
            }
        }
        return url;
    }

    private String stripHtmlSpaces(String s) {
        int i = 0;
        int n = s.length();
        for (; n > i; --n) {
            if (!htmlSpace(s.charAt(n - 1))) {
                break;
            }
        }
        for (; i < n; ++i) {
            if (!htmlSpace(s.charAt(i))) {
                break;
            }
        }
        if (i == 0 && n == s.length()) {
            return s;
        }
        return s.substring(i, n);
    }

    private boolean htmlSpace(int ch) {
        switch (ch) {
            case ' ':
            case '\t':
            case '\n':
            case '\u000c':
            case '\r':
                return true;
            default:
                return false;

        }
    }
}
