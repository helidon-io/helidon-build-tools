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
package io.helidon.build.archetype.engine.v2;

import org.junit.jupiter.api.Test;

import java.nio.file.InvalidPathException;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class ContextPathResolverTest {

    private static final String ROOT = "flavor.base";

    @Test
    public void testResolveAbsolutePath() {
        String path = "favor";
        assertThat(ContextPathResolver.resolvePathWithPrefix(ROOT, path),
                is("flavor.base.favor"));
    }

    @Test
    public void testResolveAbsolutePath1() {
        String path = "flavor";
        assertThat(ContextPathResolver.resolvePathWithPrefix(ROOT, path),
                is("flavor"));
    }

    @Test
    public void testResolveAbsolutePath2() {
        String path = "flavor.base";
        assertThat(ContextPathResolver.resolvePathWithPrefix(ROOT, path),
                is("flavor.base"));
    }

    @Test
    public void testResolveAbsolutePath3() {
        String path = "flavor.base.media-type.provider";
        assertThat(ContextPathResolver.resolvePathWithPrefix(ROOT, path),
                is("flavor.base.media-type.provider"));
    }

    @Test
    public void testResolveAbsolutePath4() {
        String path = "security.authentication.provider";
        assertThat(ContextPathResolver.resolvePathWithPrefix(ROOT, path),
                is("flavor.base.security.authentication.provider"));
    }

    @Test
    public void testResolveAbsolutePath5() {
        String path = "flavor.foo";
        try {
            ContextPathResolver.resolvePathWithPrefix(ROOT, path);
            fail();
        } catch (InvalidPathException e) {
             assertThat("Invalid path: flavor.foo", is(e.getMessage()));
        }
    }

    @Test
    public void testContextNodePath() {
        //flavor -> base -> node1 -> node2
        ContextNode node2 = new ContextNodeImpl("node2", null);
        List<ContextNode> list = new LinkedList<>();
        list.add(node2);
        ContextNode node1 = new ContextNodeImpl("node1", list);
        list = new LinkedList<>();
        list.add(node1);
        ContextNode node0 = new ContextNodeImpl("base", list);
        list = new LinkedList<>();
        list.add(node0);
        ContextNode rootNode = new ContextNodeImpl("flavor", list);

        ContextNode node = ContextPathResolver.resolvePath(rootNode, "flavor");
        assertThat(node.name(), is("flavor"));

        node = ContextPathResolver.resolvePath(rootNode, "flavor.base");
        assertThat(node.name(), is("base"));

        node = ContextPathResolver.resolvePath(rootNode, "flavor.base.node1");
        assertThat(node.name(), is("node1"));

        node = ContextPathResolver.resolvePath(rootNode, "flavor.base.node1.node2");
        assertThat(node.name(), is("node2"));

        try {
            ContextPathResolver.resolvePath(rootNode, "flavor.base.wrongPath");
            fail();
        } catch (InvalidPathException e) {
            assertThat("Invalid path, cannot find children: flavor.base.wrongPath", is(e.getMessage()));
        }
    }

    @Test
    public void testPropertiesPath() {
        //flavor -> base -> security -> authentication -> provider
        //              \-> wrongPath                 \-> wrongPath
        ContextNode node6 = new ContextNodeImpl("wrongPath", null);
        ContextNode node5 = new ContextNodeImpl("provider", null);
        List<ContextNode> list = new LinkedList<>();
        list.add(node5);
        list.add(node6);
        ContextNode node4 = new ContextNodeImpl("authentication", list);
        list = new LinkedList<>();
        list.add(node4);
        list.add(node6);
        ContextNode node3 = new ContextNodeImpl("security", list);
        list = new LinkedList<>();
        list.add(node3);
        ContextNode node2 = new ContextNodeImpl("base", list);
        list = new LinkedList<>();
        list.add(node2);
        ContextNode root = new ContextNodeImpl("flavor", list);
        list = new LinkedList<>();
        list.add(root);

        ContextNode node = ContextPathResolver.resolvePath(root, "ROOT.flavor.base.security.authentication.provider");
        assertThat(node.name(), is("provider"));

        node = ContextPathResolver.resolvePath(root, "PARENT.security.authentication.provider");
        assertThat(node.name(), is("provider"));
    }
}
