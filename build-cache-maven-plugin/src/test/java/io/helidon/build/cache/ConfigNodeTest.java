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

import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ConfigNodeTest {

    static ConfigNode configNode(String config) throws Exception {
        return new ConfigNode(ConfigAdapters.create(Xpp3DomBuilder.build(new StringReader(config))), null);
    }

    @Test
    void testRoot() throws Exception {
        ConfigNode node = configNode("<a/>");
        assertThat(node.index(), is(0));
        assertThat(node.path(), is("/"));
    }

    @Test
    void testNoSiblings() throws Exception {
        ConfigNode node = configNode("<a><b><c>d</c></b></a>").children().get(0).children().get(0);
        assertThat(node.index(), is(0));
        assertThat(node.path(), is("/a{0}/b{0}"));
        assertThat(node.toString(), is("c=d"));
    }

    @Test
    void testSiblings() throws Exception {
        List<ConfigNode> nodes = configNode("<a><b><c>d</c><e>f</e></b></a>").children().get(0).children();
        ConfigNode node0 = nodes.get(0);
        assertThat(node0.index(), is(0));
        assertThat(node0.path(), is("/a{0}/b{0}"));
        ConfigNode node1 = nodes.get(1);
        assertThat(node1.index(), is(1));
        assertThat(node1.path(), is("/a{0}/b{1}"));
    }
}
