/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link Proxies}.
 */
class ProxiesTest {

    private static Properties sysProps;

    private static Properties clearProxyProperties() {
        final Properties properties = System.getProperties();
        Proxies.proxyProperties().forEach(properties::remove);
        return properties;
    }

    @BeforeAll
    public static void beforeAllTests() {
        sysProps = System.getProperties();
        Properties copy = new Properties();
        copy.putAll(System.getProperties());
        System.setProperties(copy);
    }

    @AfterAll
    public static void afterAllTests() {
        System.setProperties(sysProps);
    }

    @Test
    void testNoArgsSet() {
        clearProxyProperties();
        List<String> args = Proxies.collectPropertyArgs();
        assertThat(args, is(not(nullValue())));
        assertThat(args.isEmpty(), is(true));
    }

    @Test
    void testOneArgSet() {
        Properties properties = clearProxyProperties();
        properties.setProperty("http.proxyHost", "acme.com");
        List<String> args = Proxies.collectPropertyArgs();
        assertThat(args, is(not(nullValue())));
        assertThat(args.size(), is(1));
        assertThat(args.get(0), is("-Dhttp.proxyHost=acme.com"));
    }

    @Test
    void testTwoArgsSet() {
        Properties properties = clearProxyProperties();
        properties.setProperty("http.proxyHost", "acme.com");
        properties.setProperty("http.proxyPort", "8192");
        List<String> args = Proxies.collectPropertyArgs();
        assertThat(args, is(not(nullValue())));
        assertThat(args.size(), is(2));
        Collections.sort(args);
        assertThat(args.get(0), is("-Dhttp.proxyHost=acme.com"));
        assertThat(args.get(1), is("-Dhttp.proxyPort=8192"));
    }

    @Test
    void testNoPropsSet() {
        Map<String, String> env = Map.of();
        Properties props = new Properties();
        Proxies.setProxyPropertiesFrom(env, props);
        assertThat(props.isEmpty(), is(true));
    }

    @Test
    void testHttpProxySet() {
        Map<String, String> env = Map.of("http_proxy", "www-proxy.us.acme.com:80");
        Properties props = new Properties();
        Proxies.setProxyPropertiesFrom(env, props);
        assertThat(props.size(), is(2));
        assertThat(props.get("http.proxyHost"), is("www-proxy.us.acme.com"));
        assertThat(props.get("http.proxyPort"), is("80"));
    }

    @Test
    void testHttpsProxySet() {
        Map<String, String> env = Map.of("https_proxy", "www-proxy.us.acme.com:80");
        Properties props = new Properties();
        Proxies.setProxyPropertiesFrom(env, props);
        assertThat(props.size(), is(2));
        assertThat(props.get("https.proxyHost"), is("www-proxy.us.acme.com"));
        assertThat(props.get("https.proxyPort"), is("80"));
    }

    @Test
    void testHttpProxyUrlSet() {
        Map<String, String> env = Map.of("http_proxy", "http://www-proxy.us.acme.com:80");
        Properties props = new Properties();
        Proxies.setProxyPropertiesFrom(env, props);
        assertThat(props.size(), is(2));
        assertThat(props.get("http.proxyHost"), is("www-proxy.us.acme.com"));
        assertThat(props.get("http.proxyPort"), is("80"));
    }

    @Test
    void testHttpsProxyUrlSet() {
        Map<String, String> env = Map.of("https_proxy", "https://www-proxy.us.acme.com:80");
        Properties props = new Properties();
        Proxies.setProxyPropertiesFrom(env, props);
        assertThat(props.size(), is(2));
        assertThat(props.get("https.proxyHost"), is("www-proxy.us.acme.com"));
        assertThat(props.get("https.proxyPort"), is("80"));
    }

    @Test
    void testNonProxySet() {
        Map<String, String> env = Map.of("no_proxy", "localhost,127.0.0.1,.acmecorp.com,.acme.com,.jenkins,.vault");
        Properties props = new Properties();
        Proxies.setProxyPropertiesFrom(env, props);
        assertThat(props.size(), is(2));
        assertThat(props.get("http.nonProxyHosts"), is("localhost|127.0.0.1|*.acmecorp.com|*.acme.com|*.jenkins|*.vault"));
        assertThat(props.get("https.nonProxyHosts"), is("localhost|127.0.0.1|*.acmecorp.com|*.acme.com|*.jenkins|*.vault"));
    }

    @Test
    void testSkipIfSet() {
        Map<String, String> env = Map.of("http_proxy", "www-proxy.us.acme.com:80");
        Properties props = new Properties();
        props.setProperty("http.proxyHost", "acme.com");
        props.setProperty("http.proxyPort", "8192");
        Proxies.setProxyPropertiesFrom(env, props);
        assertThat(props.size(), is(2));
        assertThat(props.get("http.proxyHost"), is("acme.com"));
        assertThat(props.get("http.proxyPort"), is("8192"));
    }
}
