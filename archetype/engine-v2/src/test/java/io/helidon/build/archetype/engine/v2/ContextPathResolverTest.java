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
        assertThat(getAbsolutePath(path), is("flavor.base.favor"));
    }

    @Test
    public void testResolveAbsolutePath1() {
        String path = "flavor";
        assertThat(getAbsolutePath(path), is("flavor"));
    }

    @Test
    public void testResolveAbsolutePath2() {
        String path = "flavor.base";
        assertThat(getAbsolutePath(path), is("flavor.base"));
    }

    @Test
    public void testResolveAbsolutePath3() {
        String path = "flavor.base.media-type.provider";
        assertThat(getAbsolutePath(path), is("flavor.base.media-type.provider"));
    }

    @Test
    public void testResolveAbsolutePath4() {
        String path = "security.authentication.provider";
        assertThat(getAbsolutePath(path), is("flavor.base.security.authentication.provider"));
    }

    @Test
    public void testResolveAbsolutePath5() {
        String path = "flavor.foo";
        ContextPathResolver.setPrefixPath(ROOT);
        try {
            ContextPathResolver.resolveRelativePath(path);
            fail();
        } catch (InvalidPathException e) {
             assertThat("Invalid path: flavor.foo", is(e.getMessage()));
        }
    }

    private String getAbsolutePath(String path) {
        ContextPathResolver.setPrefixPath(ROOT);
        return ContextPathResolver.resolveRelativePath(path);
    }

    @Test
    public void testContextNodePath() {
        ContextNode node2 = new ContextNodeImpl("node2", null);
        List<ContextNode> list = new LinkedList<>();
        list.add(node2);
        ContextNode node1 = new ContextNodeImpl("node1", list);
        list = new LinkedList<>();
        list.add(node1);
        ContextNode rootNode = new ContextNodeImpl("root", list);

        ContextPathResolver.setRoot(rootNode);
        ContextPathResolver.setPrefixPath(null);

        ContextNode node = ContextPathResolver.resolvePath("root");
        assertThat(node.name(), is("root"));

        node = ContextPathResolver.resolvePath("root.node1");
        assertThat(node.name(), is("node1"));

        node = ContextPathResolver.resolvePath("root.node1.node2");
        assertThat(node.name(), is("node2"));

        try {
            ContextPathResolver.resolvePath("root.wrongPath");
        } catch (InvalidPathException e) {
            assertThat("Invalid path, cannot find children: root.wrongPath", is(e.getMessage()));
        }
    }

    @Test
    public void testPropertiesPath() {
        ContextNode node5 = new ContextNodeImpl("provider", null);
        List<ContextNode> list = new LinkedList<>();
        list.add(node5);
        ContextNode node4 = new ContextNodeImpl("authentication", list);
        list = new LinkedList<>();
        list.add(node4);
        ContextNode node3 = new ContextNodeImpl("security", list);
        list = new LinkedList<>();
        list.add(node3);
        ContextNode node2 = new ContextNodeImpl("base", list);
        list = new LinkedList<>();
        list.add(node2);
        ContextNode root = new ContextNodeImpl("flavor", list);
        list = new LinkedList<>();
        list.add(root);

        ContextPathResolver.setRoot(root);
        ContextNode node = ContextPathResolver.resolvePath("ROOT.flavor.base.security.authentication.provider");
        assertThat(node.name(), is("provider"));

        node = ContextPathResolver.resolvePath("PARENT.security.authentication.provider");
        assertThat(node.name(), is("provider"));
    }
}
