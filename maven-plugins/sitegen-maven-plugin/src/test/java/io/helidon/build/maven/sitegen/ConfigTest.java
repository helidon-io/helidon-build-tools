/*
 * Copyright (c) 2018, 2025 Oracle and/or its affiliates.
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

package io.helidon.build.maven.sitegen;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import io.helidon.build.maven.sitegen.asciidoctor.AsciidocEngine;
import io.helidon.build.maven.sitegen.freemarker.FreemarkerEngine;
import io.helidon.build.maven.sitegen.models.Header;
import io.helidon.build.maven.sitegen.models.Nav;
import io.helidon.build.maven.sitegen.models.WebResource.Location;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests config.
 */
class ConfigTest {

    @Test
    void testAsMap() {
        Config config = Config.create("/config/map.yaml", ConfigTest.class, Map.of(
                "alice", "alice1",
                "key", "some-key"));

        assertThat(config.get("strings").asMap().orElseThrow(), is(Map.of(
                "foo", "foo",
                "bar", "bar")));
        assertThat(config.get("numbers").asMap(byte.class).orElseThrow(), is(Map.of(
                "one", (byte) 1,
                "two", (byte) 2,
                "three",
                (byte) 3)));
        assertThat(config.get("numbers").asMap(short.class).orElseThrow(), is(Map.of(
                "one", (short) 1,
                "two", (short) 2,
                "three", (short) 3)));
        assertThat(config.get("numbers").asMap(int.class).orElseThrow(), is(Map.of(
                "one", 1,
                "two", 2,
                "three", 3)));
        assertThat(config.get("numbers").asMap(long.class).orElseThrow(), is(Map.of(
                "one", 1L,
                "two", 2L,
                "three", 3L)));
        assertThat(config.get("numbers").asMap(double.class).orElseThrow(), is(Map.of(
                "one", 1d,
                "two", 2d,
                "three", 3d)));
        assertThat(config.get("numbers").asMap(float.class).orElseThrow(), is(Map.of(
                "one", 1f,
                "two", 2f,
                "three", 3f)));
        assertThat(config.get("booleans").asMap(boolean.class).orElseThrow(), is(Map.of(
                "t", true,
                "f", false)));
        assertThat(config.get("chars").asMap(char.class).orElseThrow(), is(Map.of(
                "a", 'a',
                "b", 'b',
                "c", 'c')));

        assertThat(config.get("complex-map").asMap().orElseThrow(), is(Map.of(
                "foo.0.bar.bar1.0", "bar1a",
                "foo.0.bar.bar1.1", "bar1b",
                "foo.0.bar.bar2.0", "bar2a",
                "foo.0.bar.bar2.1", "bar2b",
                "foo.1.bob.bob1.0", "bob1a",
                "foo.1.bob.bob1.1", "bob1b",
                "foo.1.bob.bob2.0", "bob2a",
                "foo.1.bob.bob2.1", "bob2b",
                "alice", "alice1",
                "some-key", "some-value"
        )));
    }

    @Test
    void testAsList() {
        Config config = Config.create("/config/list.yaml", ConfigTest.class, Map.of(
                "foo1", "foo1",
                "foo2", "foo2"));

        assertThat(config.get("strings").asList().orElseThrow(), is(List.of("foo", "bar")));
        assertThat(config.get("numbers").asList(byte.class).orElseThrow(), is(List.of((byte) 1, (byte) 2, (byte) 3)));
        assertThat(config.get("numbers").asList(short.class).orElseThrow(), is(List.of((short) 1, (short) 2, (short) 3)));
        assertThat(config.get("numbers").asList(int.class).orElseThrow(), is(List.of(1, 2, 3)));
        assertThat(config.get("numbers").asList(long.class).orElseThrow(), is(List.of(1L, 2L, 3L)));
        assertThat(config.get("numbers").asList(double.class).orElseThrow(), is(List.of(1d, 2d, 3d)));
        assertThat(config.get("numbers").asList(float.class).orElseThrow(), is(List.of(1f, 2f, 3f)));
        assertThat(config.get("booleans").asList(boolean.class).orElseThrow(), is(List.of(true, false)));
        assertThat(config.get("chars").asList(char.class).orElseThrow(), is(List.of('a', 'b', 'c')));

        assertThat(config.get("simple-list").asList().orElseThrow(), is(List.of("foo1", "foo2")));
        assertThat(config.get("complex-list").asList().orElseThrow(), is(List.of(
                "foo1",
                "foo2",
                "bar1",
                "bar2"
        )));
    }

    @Test
    void testAsBoolean() {
        Config config = Config.create(new StringReader("value: true"), Map.of());
        assertThat(config.get("value").asBoolean().orElseThrow(), is(true));
        assertThat(config.get("value").as(Boolean.class).orElseThrow(), is(true));
        assertThat(config.get("value").as(boolean.class).orElseThrow(), is(true));
    }

    @Test
    void testAsString() {
        Config config = Config.create(new StringReader("value: str"), Map.of());
        assertThat(config.get("value").asString().orElseThrow(), is("str"));
        assertThat(config.get("value").as(String.class).orElseThrow(), is("str"));
    }

    @Test
    void testAsCharacter() {
        Config config = Config.create(new StringReader("value: a"), Map.of());
        assertThat(config.get("value").as(Character.class).orElseThrow(), is('a'));
        assertThat(config.get("value").as(char.class).orElseThrow(), is('a'));
    }

    @Test
    void testAsInteger() {
        Config config = Config.create(new StringReader("value: 2"), Map.of());
        assertThat(config.get("value").as(Integer.class).orElseThrow(), is(2));
        assertThat(config.get("value").as(int.class).orElseThrow(), is(2));
    }

    @Test
    void testAsLong() {
        Config config = Config.create(new StringReader("value: 2"), Map.of());
        assertThat(config.get("value").as(Long.class).orElseThrow(), is(2L));
        assertThat(config.get("value").as(long.class).orElseThrow(), is(2L));
    }

    @Test
    void testAsByte() {
        Config config = Config.create(new StringReader("value: 2"), Map.of());
        assertThat(config.get("value").as(Byte.class).orElseThrow(), is((byte) 2));
        assertThat(config.get("value").as(byte.class).orElseThrow(), is((byte) 2));
    }

    @Test
    void testAsShort() {
        Config config = Config.create(new StringReader("value: 2"), Map.of());
        assertThat(config.get("value").as(Short.class).orElseThrow(), is((short) 2));
        assertThat(config.get("value").as(short.class).orElseThrow(), is((short) 2));
    }

    @Test
    void testAsFloat() {
        Config config = Config.create(new StringReader("value: 2"), Map.of());
        assertThat(config.get("value").as(Float.class).orElseThrow(), is(2f));
        assertThat(config.get("value").as(float.class).orElseThrow(), is(2f));
    }

    @Test
    void testAsDouble() {
        Config config = Config.create(new StringReader("value: 2"), Map.of());
        assertThat(config.get("value").as(Double.class).orElseThrow(), is(2d));
        assertThat(config.get("value").as(double.class).orElseThrow(), is(2d));
    }

    @Test
    public void testNavConfig() {
        Config config = Config.create("/config/nav.yaml", ConfigTest.class, Map.of());
        verifyNav(Nav.create(config));
    }

    @Test
    public void testSiteConfig() {
        Config config = Config.create("/config/basic.yaml", ConfigTest.class, Map.of("basedir", "/ws"));
        Site site = Site.create(config);

        Backend backend = site.backend();
        assertThat(backend.name(), is("basic"));

        SiteEngine engine = site.engine();
        AsciidocEngine asciidoc = engine.asciidoc();
        assertThat(asciidoc.imagesDir(), is("./images"));
        assertThat(asciidoc.libraries(), hasItems("test-lib"));
        assertThat(asciidoc.attributes(), is(Map.of("bob", "alice")));

        FreemarkerEngine freemarker = engine.freemarker();
        assertThat(freemarker.directives(), is(Map.of("foo", "com.acme.foo.FooDirective")));
        assertThat(freemarker.model(), is(Map.of("key", "value")));

        Header header = site.header();
        assertThat(header, is(not(nullValue())));

        assertThat(header.favicon(), is(not(nullValue())));
        assertThat(header.favicon().type(), is(nullValue()));
        assertThat(header.favicon().location().type(), is(Location.Type.PATH));
        assertThat(header.favicon().location().value(), is("assets/images/favicon.ico"));

        assertThat(header.stylesheets().size(), is(2));
        assertThat(header.stylesheets().get(0).location().type(), is(Location.Type.PATH));
        assertThat(header.stylesheets().get(0).location().value(), is("assets/css/style.css"));
        assertThat(header.stylesheets().get(1).location().type(), is(Location.Type.HREF));
        assertThat(header.stylesheets().get(1).location().value(), is("https://css.com/style.css"));

        assertThat(header.scripts().size(), is(2));
        assertThat(header.scripts().get(0).location().type(), is(Location.Type.PATH));
        assertThat(header.scripts().get(0).location().value(), is("assets/js/script.js"));
        assertThat(header.scripts().get(1).location().type(), is(Location.Type.HREF));
        assertThat(header.scripts().get(1).location().value(), is("https://js.com/script.js"));

        assertThat(header.meta(), is(Map.of("description", "a global description")));

        assertThat(site.assets().size(), is(1));
        assertThat(site.assets().get(0).target(), is("/assets"));
        assertThat(site.assets().get(0).includes(), hasItems("/ws/assets/**"));
        assertThat(site.assets().get(0).excludes(), hasItems("**/_*"));

        assertThat(site.pages().size(), is(1));
        assertThat(site.pages().get(0).includes(), hasItems("docs/**/*.adoc"));
        assertThat(site.pages().get(0).excludes(), hasItems("**/_*"));
    }

    @Test
    public void testVuetifyConfig() {
        Config config = Config.create("/config/vuetify.yaml", ConfigTest.class, Map.of());
        Site site = Site.create(config);

        Backend backend = site.backend();
        assertThat(backend.name(), is("vuetify"));

        assertThat(backend, is(instanceOf(VuetifyBackend.class)));
        VuetifyBackend vuetify = (VuetifyBackend) backend;

        assertThat(vuetify.home(), is("home.adoc"));
        assertThat(vuetify.releases(), hasItems("1.0"));

        Nav nav = vuetify.nav();
        assertThat(nav, is(not(nullValue())));
        verifyNav(nav);

        assertThat(vuetify.theme(), is(
                Map.of(
                        "primary", "#1976D2",
                        "secondary", "#424242",
                        "accent", "#82B1FF",
                        "error", "#FF5252",
                        "info", "#2196F3",
                        "success", "#4CAF50",
                        "warning", "#FFC107",
                        "toolbar.enabled", "true",
                        "navmenu.enabled", "true",
                        "navfooter.enabled", "true"
                )));
    }

    @SuppressWarnings("ConstantConditions")
    private void verifyNav(Nav nav) {
        Deque<Nav> stack = new ArrayDeque<>();

        assertThat(nav, is(not(nullValue())));
        assertThat(nav.title(), is("Pet Project Documentation"));
        assertThat(nav.type(), is(Nav.Type.ROOT));
        assertThat(nav.glyph(), is(not(nullValue())));
        assertThat(nav.glyph().type(), is("icon"));
        assertThat(nav.glyph().value(), is("import_contacts"));
        assertThat(nav.items().size(), is(3));
        stack.push(nav);

        nav = nav.items().get(0);
        assertThat(nav.type(), is(Nav.Type.GROUPS));
        stack.push(nav);

        nav = stack.peek().items().get(0);
        assertThat(nav.type(), is(Nav.Type.GROUP));
        assertThat(nav.title(), is("Main Documentation"));
        assertThat(nav.glyph(), is(nullValue()));
        assertThat(nav.pathprefix(), is("/"));
        assertThat(nav.items().size(), is(3));
        stack.push(nav);

        nav = stack.peek().items().get(0);
        assertThat(nav.type(), is(Nav.Type.MENU));
        assertThat(nav.title(), is("About"));
        assertThat(nav.pathprefix(), is("/about"));
        assertThat(nav.glyph(), is(not(nullValue())));
        assertThat(nav.glyph().type(), is("icon"));
        assertThat(nav.glyph().value(), is("weekend"));

        nav = stack.peek().items().get(1);
        assertThat(nav.type(), is(Nav.Type.MENU));
        assertThat(nav.title(), is("Getting Started"));
        assertThat(nav.pathprefix(), is("/getting-started"));
        assertThat(nav.glyph(), is(not(nullValue())));
        assertThat(nav.glyph().type(), is("icon"));
        assertThat(nav.glyph().value(), is("weekend"));
        assertThat(nav.includes().size(), is(0));
        assertThat(nav.excludes(), hasItem("**/start*.adoc"));

        nav = stack.peek().items().get(2);
        assertThat(nav.type(), is(Nav.Type.MENU));
        assertThat(nav.title(), is("Let's code"));
        assertThat(nav.pathprefix(), is("/lets-code"));
        assertThat(nav.glyph(), is(not(nullValue())));
        assertThat(nav.glyph().type(), is("icon"));
        assertThat(nav.glyph().value(), is("weekend"));
        assertThat(nav.includes(), hasItem("**/*.adoc"));
        assertThat(nav.excludes().size(), is(0));

        stack.pop();
        assertThat(stack.isEmpty(), is(false));

        stack.pop();
        assertThat(stack.isEmpty(), is(false));

        nav = stack.peek().items().get(1);
        assertThat(nav.type(), is(Nav.Type.MENU));
        assertThat(nav.title(), is("Extra Resources"));
        assertThat(nav.glyph(), is(nullValue()));
        assertThat(nav.items().size(), is(2));
        stack.push(nav);

        nav = stack.peek().items().get(0);
        assertThat(nav.type(), is(Nav.Type.LINK));
        assertThat(nav.title(), is("Google"));
        assertThat(nav.href(), is("https://google.com"));
        assertThat(nav.target(), is("_blank"));
        assertThat(nav.items().size(), is(0));

        nav = stack.peek().items().get(1);
        assertThat(nav.type(), is(Nav.Type.LINK));
        assertThat(nav.title(), is("Amazon"));
        assertThat(nav.href(), is("https://amazon.com"));
        assertThat(nav.target(), is("_blank"));
        assertThat(nav.items().size(), is(0));


        stack.pop();
        assertThat(stack.isEmpty(), is(false));

        nav = stack.peek().items().get(2);
        assertThat(nav.type(), is(Nav.Type.LINK));
        assertThat(nav.title(), is("GitHub"));
        assertThat(nav.href(), is("https://github.com"));
        assertThat(nav.target(), is("_blank"));
        assertThat(nav.items().size(), is(0));
    }
}
