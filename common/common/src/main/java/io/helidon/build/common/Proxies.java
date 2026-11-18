/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.common.logging.Log;

/**
 * Proxy utilities.
 */
public class Proxies {

    private static final String HTTP_PROXY_VAR = "HTTP_PROXY";
    private static final String HTTPS_PROXY_VAR = "HTTPS_PROXY";
    private static final String NO_PROXY_VAR = "NO_PROXY";
    private static final String HTTPS_PREFIX = "HTTPS";
    private static final String PROXY_HOST_PROP_SUFFIX = ".proxyHost";
    private static final String PROXY_PORT_PROP_SUFFIX = ".proxyPort";
    private static final String HTTP_PROP_PREFIX = "http";
    private static final String HTTPS_PROP_PREFIX = "https";
    private static final String NO_PROXY_NEEDS_WILDCARD_PREFIX = ".";
    private static final String NO_PROXY_WILDCARD = "*";
    private static final String NO_PROXY_VAR_SEP = ",";
    private static final String NO_PROXY_PROP_SEP = "|";
    private static final String SCHEME_SEP = "://";
    private static final String AUTHORITY_END_CHARS = "/?#";
    private static final String DEFAULT_PROXY_PORT = "1080";
    private static final String DEFAULT_HTTPS_PROXY_PORT = "443";
    private static final String HTTP_NON_PROXY_PROP = "http.nonProxyHosts";
    private static final String HTTPS_NON_PROXY_PROP = "https.nonProxyHosts";
    private static final List<String> PROXY_PROPS = List.of(
            "http.proxyHost",
            "http.proxyPort",
            "http.nonProxyHosts",
            "https.proxyHost",
            "https.proxyPort",
            "https.nonProxyHosts"
    );
    private static final AtomicReference<List<String>> PROXY_ARGS = new AtomicReference<>();

    /**
     * Sets the proxy system properties from the corresponding environment variables, if any.
     * The supported environment variables are:
     * <ul>
     * <li>HTTP_PROXY</li>
     * <li>HTTPS_PROXY</li>
     * <li>NO_PROXY</li>
     * </ul>
     * along with their lowercase variants. If both {@code NO_PROXY} and {@code NO_PROXY_HOSTS} are set, the latter takes
     * precedence.
     * <p>
     * The proxy properties are:
     * <ul>
     * <li>http.proxyHost</li>
     * <li>http.proxyPort</li>
     * <li>http.nonProxyHosts</li>
     * <li>https.proxyHost</li>
     * <li>https.proxyPort</li>
     * <li>https.nonProxyHosts</li>
     * </ul>
     */
    public static void setProxyPropertiesFromEnv() {
        setProxyPropertiesFrom(System.getenv(), System.getProperties());
    }

    /**
     * Returns the command line arguments to set proxy properties, iff any of the proxy properties are already set.
     * The proxy properties are:
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
    public static List<String> javaProxyArgs() {
        List<String> args = PROXY_ARGS.get();
        if (args == null) {
            args = collectPropertyArgs();
            PROXY_ARGS.set(args);
        }
        return args;
    }

    /**
     * Returns the list of proxy properties.
     *
     * @return The list.
     */
    public static List<String> proxyProperties() {
        return PROXY_PROPS;
    }

    static List<String> collectPropertyArgs() {
        final List<String> args = new ArrayList<>();
        PROXY_PROPS.forEach(key -> {
            final String value = System.getProperty(key);
            if (value != null) {
                args.add("-D" + key + "=" + value);
            }
        });
        return args;
    }

    static void setProxyPropertiesFrom(Map<String, String> env, Properties properties) {
        setProxy(HTTP_PROXY_VAR, env, properties);
        setProxy(HTTPS_PROXY_VAR, env, properties);
        setNoProxy(NO_PROXY_VAR, env, properties);
    }

    private static void setNoProxy(String envVarName, Map<String, String> env, Properties properties) {
        final String value = envVar(envVarName, env);
        if (value != null) {
            final String nonProxyHosts = parseNoProxy(value);
            setProperty(HTTP_NON_PROXY_PROP, nonProxyHosts, properties);
            setProperty(HTTPS_NON_PROXY_PROP, nonProxyHosts, properties);
        }
    }

    private static void setProxy(String envVarName, Map<String, String> env, Properties properties) {
        final String value = envVar(envVarName, env);
        if (value != null) {
            final HostAndPort proxy = parseProxy(value);
            if (proxy != null) {
                final String protocol = envVarName.startsWith(HTTPS_PREFIX) ? HTTPS_PROP_PREFIX : HTTP_PROP_PREFIX;
                setProperty(protocol + PROXY_HOST_PROP_SUFFIX, proxy.host(), properties);
                setProperty(protocol + PROXY_PORT_PROP_SUFFIX, proxy.port(), properties);
            } else {
                Log.debug("Ignoring environment variable \"%s\", not a supported HTTP proxy", envVarName);
            }
        }
    }

    /**
     * Parse the value of a {@code NO_PROXY} environment variable into the format of the
     * {@code http.nonProxyHosts} system property.
     * Entries are trimmed, empty entries are skipped, and a leading {@code .} is turned into {@code *.}.
     *
     * @param value environment variable value, e.g. {@code "localhost, .acme.com"}
     * @return property value, e.g. {@code "localhost|*.acme.com"}
     */
    static String parseNoProxy(String value) {
        final StringBuilder sb = new StringBuilder();
        for (String entry : value.split(NO_PROXY_VAR_SEP)) {
            final String host = entry.trim();
            if (host.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(NO_PROXY_PROP_SEP);
            }
            if (host.startsWith(NO_PROXY_NEEDS_WILDCARD_PREFIX)) {
                sb.append(NO_PROXY_WILDCARD);
            }
            sb.append(host);
        }
        return sb.toString();
    }

    /**
     * Parse the value of an {@code HTTP_PROXY} or {@code HTTPS_PROXY} environment variable.
     * <p>
     * There is no formal standard for these variables. This follows what curl accepts, see
     * <a href="https://curl.se/libcurl/c/CURLOPT_PROXY.html">CURLOPT_PROXY</a> and
     * <a href="https://everything.curl.dev/usingcurl/proxies/env.html">Proxy environment variables</a>;
     * <a href="https://about.gitlab.com/blog/2021/01/27/we-need-to-talk-no-proxy/">this comparison</a>
     * describes how other tools differ:
     * <ul>
     * <li>{@code [scheme://][user[:password]@]host[:port][/]}, where host may be a bracketed IPv6 address</li>
     * <li>the scheme is {@code http} if missing; {@code https} is accepted as well</li>
     * <li>other schemes (e.g. {@code socks5://}) are not HTTP proxies and are ignored</li>
     * <li>anything after the authority (e.g. a trailing {@code /}) is ignored</li>
     * <li>the user info is ignored</li>
     * <li>the port defaults to {@code 443} for {@code https://}, and to {@code 1080} otherwise</li>
     * </ul>
     *
     * @param value environment variable value
     * @return host and port, or {@code null} if the value is not a supported HTTP proxy
     */
    static HostAndPort parseProxy(String value) {
        String authority = value.trim();
        String scheme = HTTP_PROP_PREFIX;
        final int schemeEnd = authority.indexOf(SCHEME_SEP);
        if (schemeEnd >= 0) {
            scheme = authority.substring(0, schemeEnd).toLowerCase(Locale.ENGLISH);
            authority = authority.substring(schemeEnd + SCHEME_SEP.length());
        }
        if (!scheme.equals(HTTP_PROP_PREFIX) && !scheme.equals(HTTPS_PROP_PREFIX)) {
            return null;
        }
        for (int i = 0; i < authority.length(); i++) {
            if (AUTHORITY_END_CHARS.indexOf(authority.charAt(i)) >= 0) {
                authority = authority.substring(0, i);
                break;
            }
        }
        authority = authority.substring(authority.lastIndexOf('@') + 1);
        final String host;
        String port = null;
        if (authority.startsWith("[")) {
            final int hostEnd = authority.indexOf(']');
            if (hostEnd < 0) {
                return null;
            }
            host = authority.substring(1, hostEnd);
            final String rest = authority.substring(hostEnd + 1);
            if (rest.startsWith(":")) {
                port = rest.substring(1);
            } else if (!rest.isEmpty()) {
                return null;
            }
        } else {
            final int portSep = authority.lastIndexOf(':');
            if (portSep >= 0) {
                host = authority.substring(0, portSep);
                port = authority.substring(portSep + 1);
            } else {
                host = authority;
            }
        }
        if (host.isEmpty()) {
            return null;
        }
        if (port == null || port.isEmpty()) {
            port = scheme.equals(HTTPS_PROP_PREFIX) ? DEFAULT_HTTPS_PROXY_PORT : DEFAULT_PROXY_PORT;
        } else if (!isPort(port)) {
            return null;
        }
        return new HostAndPort(host, port);
    }

    private static boolean isPort(String value) {
        if (value.length() > 5) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < '0' || value.charAt(i) > '9') {
                return false;
            }
        }
        final int port = Integer.parseInt(value);
        return port > 0 && port <= 65535;
    }

    private static void setProperty(String name, String value, Properties properties) {
        final String existing = properties.getProperty(name);
        if (existing == null) {
            Log.debug("Setting system entry \"%s\" to \"%s\"", name, value);
            properties.setProperty(name, value);
        } else {
            Log.debug("Skip set system entry \"%s\" to \"%s\". Already set to \"%s\"", name, value, existing);
        }
    }

    private static String envVar(String name, Map<String, String> env) {
        String value = env.get(name);
        if (value == null) {
            value = env.get(name.toLowerCase(Locale.ENGLISH));
        }
        return value;
    }

    private Proxies() {
    }

    /**
     * Proxy host and port.
     */
    static final class HostAndPort {

        private final String host;
        private final String port;

        HostAndPort(String host, String port) {
            this.host = host;
            this.port = port;
        }

        String host() {
            return host;
        }

        String port() {
            return port;
        }
    }
}
