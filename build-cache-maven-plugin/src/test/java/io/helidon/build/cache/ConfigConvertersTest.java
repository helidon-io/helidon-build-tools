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
package io.helidon.build.cache;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;

import static io.helidon.build.cache.ConfigNodeTest.configNode;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class ConfigConvertersTest {

    @Test
    void testStringConverter() throws Exception {
        assertThat(ConfigConverters.toString(configNode("<a>b</a>")), is("a=b"));
        assertThat(ConfigConverters.toString(configNode("<a b=\"c\">d</a>")), is("a<b:c>=d"));
        assertThat(ConfigConverters.toString(configNode("<a b=\"c\" d=\"e\">f</a>")), is("a<b:c,d:e>=f"));
        assertThat(ConfigConverters.toString(configNode("<a><b>c</b></a>")), is("a[b=c]"));
        assertThat(ConfigConverters.toString(configNode("<a><b>c</b><d>e</d></a>")), is("a[b=c,d=e]"));
        assertThat(ConfigConverters.toString(configNode("<a b=\"c\" d=\"e\"><f><g>h</g><i><j>k</j></i></f></a>")),
                is("a<b:c,d:e>[f[g=h,i[j=k]]]"));
    }

    @Test
    void testXpp3Converter() throws Exception {
        Xpp3Dom elt;

        elt = ConfigConverters.toXpp3Dom(configNode("<a>b</a>"));
        assertThat(elt.getName(), is("a"));
        assertThat(elt.getValue(), is("b"));

        elt = ConfigConverters.toXpp3Dom(configNode("<a b=\"c\">d</a>"));
        assertThat(elt.getName(), is("a"));
        assertThat(elt.getAttribute("b"), is("c"));
        assertThat(elt.getValue(), is("d"));

        elt = ConfigConverters.toXpp3Dom(configNode("<a><b>c</b></a>"));
        assertThat(elt.getName(), is("a"));
        elt = elt.getChild(0);
        assertThat(elt, is(notNullValue()));
        assertThat(elt.getName(), is("b"));
        assertThat(elt.getValue(), is("c"));
    }
}
