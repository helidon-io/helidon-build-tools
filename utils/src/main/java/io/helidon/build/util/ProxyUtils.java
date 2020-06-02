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

import java.util.List;

/**
 * Proxy utilities.
 */
public class ProxyUtils {
    private static final List<String> PROXY_PROPS = List.of(
            "http.proxyHost",
            "http.proxyPort",
            "http.nonProxyHosts",
            "https.proxyHost",
            "https.proxyPort",
            "https.nonProxyHosts"
    );
    private static final String PROXY_ARGS = collectArgs();

    static String collectArgs() {
        final StringBuilder sb = new StringBuilder();
        PROXY_PROPS.forEach(key -> {
            final String value = System.getProperty(key);
            if (value != null) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append("-D").append(key).append('=').append(value);
            }
        });
        return sb.toString();
    }

    /**
     * Returns the command line arguments to set proxy properties, iff any of the following are already set:
     * <ul>
     * <li>http.proxyHost</li>
     * <li>http.proxyPort</li>
     * <li>http.nonProxyHosts</li>
     * <li>https.proxyHost</li>
     * <li>https.proxyPort</li>
     * <li>https.nonProxyHosts</li>
     * </ul>
     *
     * @return The args, may be empty.
     */
    public static String javaProxyArgs() {
        return PROXY_ARGS;
    }

    /**
     * Returns the list of proxy properties.
     *
     * @return The list.
     */
    public static List<String> proxyProperties() {
        return PROXY_PROPS;
    }

    private ProxyUtils() {
    }
}
