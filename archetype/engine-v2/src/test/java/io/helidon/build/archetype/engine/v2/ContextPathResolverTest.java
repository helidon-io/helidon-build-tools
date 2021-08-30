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

    private static final String PREFIX = "flavor.base";
    private final ContextPathResolver resolver = new ContextPathResolver();

    @Test
    public void testPathFromPrefix() {
        String path = "favor";
        assertThat(ContextPathResolver.resolvePathWithPrefix(PREFIX, path),
                is("flavor.base.favor"));
    }

    @Test
    public void testPrefixPath() {
        String path = "flavor";
        assertThat(ContextPathResolver.resolvePathWithPrefix(PREFIX, path),
                is("flavor"));
    }

    @Test
    public void testPathEqualsPrefix() {
        String path = "flavor.base";
        assertThat(ContextPathResolver.resolvePathWithPrefix(PREFIX, path),
                is("flavor.base"));
    }

    @Test
    public void testPathContainsPrefix() {
        String path = "flavor.base.media-type.provider";
        assertThat(ContextPathResolver.resolvePathWithPrefix(PREFIX, path),
                is("flavor.base.media-type.provider"));
    }

    @Test
    public void testMergePrefixAndPath() {
        String path = "security.authentication.provider";
        assertThat(ContextPathResolver.resolvePathWithPrefix(PREFIX, path),
                is("flavor.base.security.authentication.provider"));
    }

    @Test
    public void testWrongPath() {
        String path = "flavor.foo";
        try {
            ContextPathResolver.resolvePathWithPrefix(PREFIX, path);
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

        ContextNode node = resolver.resolvePath(rootNode, "flavor");
        assertThat(node.name(), is("flavor"));

        node = resolver.resolvePath(rootNode, "flavor.base");
        assertThat(node.name(), is("base"));

        node = resolver.resolvePath(rootNode, "flavor.base.node1");
        assertThat(node.name(), is("node1"));

        node = resolver.resolvePath(rootNode, "flavor.base.node1.node2");
        assertThat(node.name(), is("node2"));

        try {
            resolver.resolvePath(rootNode, "flavor.base.wrongPath");
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
        ContextNode node3 = new ContextNodeImpl("security", list);
        list = new LinkedList<>();
        list.add(node3);
        list.add(node6);
        ContextNode node2 = new ContextNodeImpl("base", list);
        list = new LinkedList<>();
        list.add(node2);
        ContextNode root = new ContextNodeImpl("flavor", list);
        list = new LinkedList<>();
        list.add(root);

        ContextNode node = resolver.resolvePath(root, "ROOT.flavor.base.security.authentication.provider");
        assertThat(node.name(), is("provider"));

        node = resolver.resolvePath(node3, "ROOT.security.authentication.provider");
        assertThat(node.name(), is("provider"));

        node = resolver.resolvePath(node6, "PARENT.security.authentication.provider");
        assertThat(node.name(), is("provider"));
    }

    @Test
    public void testInvalidPath() {
        //flavor -> base -> node1 -> node2
        ContextNode node2 = new ContextNodeImpl("node2", null);
        List<ContextNode> list = new LinkedList<>();
        list.add(node2);
        ContextNode node1 = new ContextNodeImpl("node1", list);
        list = new LinkedList<>();
        list.add(node1);
        ContextNode base = new ContextNodeImpl("base", list);
        list = new LinkedList<>();
        list.add(base);
        ContextNode root = new ContextNodeImpl("flavor", list);

        try {
            resolver.resolvePath(root, "PARENT.flavor");
            fail();
        } catch (NullPointerException ignore) {
        }

        try {
            resolver.resolvePath(root, "ROOT.dummy.path");
            fail();
        } catch (InvalidPathException ignore) {
        }

        try {
            resolver.resolvePath(base, "ROOT.base.path");
            fail();
        } catch (InvalidPathException ignore) {
        }

        try {
            new ContextPathResolver().resolvePath(node1, "PARENT.base.node1.node2");
            fail();
        } catch (InvalidPathException ignore) {
        }
    }
}
