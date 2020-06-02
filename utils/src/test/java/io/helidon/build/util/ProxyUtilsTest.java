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

import java.util.Properties;

import io.helidon.config.test.infra.RestoreSystemPropertiesExt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;


/**
 * Unit test for class {@link ProxyUtils}.
 */
@ExtendWith(RestoreSystemPropertiesExt.class)
class ProxyUtilsTest {

    private static Properties clearProxyProperties() {
        final Properties properties = System.getProperties();
        ProxyUtils.proxyProperties().forEach(properties::remove);
        return properties;
    }

    @Test
    void testNoneSet() {
        clearProxyProperties();
        String args = ProxyUtils.collectArgs();
        assertThat(args, is(not(nullValue())));
        assertThat(args, is(""));
    }

    @Test
    void testOneSet() {
        Properties properties = clearProxyProperties();
        properties.setProperty("http.proxyHost", "acme.com");
        String args = ProxyUtils.collectArgs();
        assertThat(args, is(not(nullValue())));
        assertThat(args, is("-Dhttp.proxyHost=acme.com"));
    }

    @Test
    void testTwoSet() {
        Properties properties = clearProxyProperties();
        properties.setProperty("http.proxyHost", "acme.com");
        properties.setProperty("http.proxyPort", "8192");
        String args = ProxyUtils.collectArgs();
        assertThat(args, is(not(nullValue())));
        assertThat(args, is("-Dhttp.proxyHost=acme.com -Dhttp.proxyPort=8192"));
    }
}
