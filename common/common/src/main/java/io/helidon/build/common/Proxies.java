/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
            final StringBuilder sb = new StringBuilder();
            for (String host : value.split(NO_PROXY_VAR_SEP)) {
                if (sb.length() > 0) {
                    sb.append(NO_PROXY_PROP_SEP);
                }
                if (host.startsWith(NO_PROXY_NEEDS_WILDCARD_PREFIX)) {
                    sb.append(NO_PROXY_WILDCARD);
                }
                sb.append(host);
            }
            setProperty(HTTP_NON_PROXY_PROP, sb.toString(), properties);
            setProperty(HTTPS_NON_PROXY_PROP, sb.toString(), properties);
        }
    }

    private static void setProxy(String envVarName, Map<String, String> env, Properties properties) {
        final String value = envVar(envVarName, env);
        if (value != null) {
            final String[] split = value.split(":");
            String host = null;
            String port = null;
            if (split.length == 3) {
                host = split[1];
                port = split[2];
                if (host.startsWith("//")) {
                    host = host.substring(2);
                }
            } else if (split.length == 2) {
                host = split[0];
                port = split[1];
            }
            if (host != null && port != null) {
                final String protocol = envVarName.startsWith(HTTPS_PREFIX) ? HTTPS_PROP_PREFIX : HTTP_PROP_PREFIX;
                setProperty(protocol + PROXY_HOST_PROP_SUFFIX, host, properties);
                setProperty(protocol + PROXY_PORT_PROP_SUFFIX, port, properties);
            }
        }
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
}
