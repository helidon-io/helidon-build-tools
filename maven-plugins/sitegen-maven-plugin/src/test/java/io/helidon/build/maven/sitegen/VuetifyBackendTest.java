/*
 * Copyright (c) 2018, 2024 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.Patch;
import io.helidon.build.maven.sitegen.models.Header;
import io.helidon.build.maven.sitegen.models.Nav;
import io.helidon.build.maven.sitegen.models.PageFilter;
import io.helidon.build.maven.sitegen.models.StaticAsset;

import io.helidon.build.maven.sitegen.models.WebResource;
import io.helidon.build.maven.sitegen.models.WebResource.Location;
import org.junit.jupiter.api.Test;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static io.helidon.build.maven.sitegen.Site.Options.STRICT_IMAGES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests {@link VuetifyBackend}.
 */
class VuetifyBackendTest {

    @Test
    void testNav1() {
        try {
            Nav.builder()
               .title("root")
               .item(Nav.Type.GROUPS, groups ->
                       groups.item(Nav.Type.GROUP, g ->
                                     g.title("group1")
                                      .item(Nav.Type.MENU, m ->
                                              m.title("menu1.1")
                                               .pathprefix("/menu1.1"))
                                      .item(Nav.Type.MENU, m ->
                                              m.title("menu1.2")
                                               .pathprefix("/menu1.2")))
                             .item(Nav.Type.GROUP, g ->
                                     g.title("group2")
                                      .item(Nav.Type.MENU, m ->
                                              m.title("menu2.1")
                                               .pathprefix("/menu2.1"))
                                      .item(Nav.Type.PAGE, p ->
                                              p.source("page2.2.adoc"))))
               .item(Nav.Type.HEADER, h -> h.title("header"))
               .item(Nav.Type.LINK, l ->
                       l.title("link")
                        .href("https://example.com"))
               .build();
        } catch (IllegalArgumentException ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Test
    void testNav2() {
        try {
            Nav.builder()
               .title("root")
               .item(Nav.Type.MENU, m ->
                       m.title("menu1")
                        .pathprefix("/menu1"))
               .item(Nav.Type.MENU, m ->
                       m.title("menu2")
                        .pathprefix("/menu2")
                        .item(Nav.Type.PAGE, p ->
                                p.source("page2.1.adoc")))
               .item(Nav.Type.PAGE, p ->
                       p.source("page3.adoc"))
               .build();
        } catch (IllegalArgumentException ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Test
    void testVuetify1() throws Exception {
        Path targetDir = targetDir(VuetifyBackendTest.class);
        Path sourceDir = targetDir.resolve("test-classes/vuetify1");
        Path outputDir = targetDir.resolve("vuetify/testvuetify1");

        Site.builder()
            .page(PageFilter.builder().includes("**/*.adoc"))
            .asset(StaticAsset.builder().includes("images/sunset.jpg").target("/"))
            .asset(StaticAsset.builder().includes("css/*.css").target("/"))
            .header(Header.builder().stylesheet(WebResource.builder().location(Location.Type.PATH, "css/styles.css")))
            .backend(VuetifyBackend.builder()
                                   .home("home.adoc")
                                   .releases("1.0")
                                   .nav(Nav.builder()
                                           .title("Pet Project doc")
                                           .glyph("icon", "import_contacts")
                                           .item(Nav.Type.GROUPS, groups ->
                                                   groups.item(Nav.Type.GROUP, g ->
                                                                 g.title("Cool Stuff")
                                                                  .item(Nav.Type.MENU, m ->
                                                                          m.title("What is it about?")
                                                                           .glyph("icon", "weekend")
                                                                           .pathprefix("/about")
                                                                           .includes("about/*.adoc"))
                                                                  .item(Nav.Type.MENU, m ->
                                                                          m.title("Getting started")
                                                                           .glyph("icon", "play_circle_outline")
                                                                           .pathprefix("/getting-started")
                                                                           .includes("getting-started/*.adoc")))
                                                         .item(Nav.Type.GROUP, g ->
                                                                 g.title("Boring Stuff")
                                                                  .item(Nav.Type.MENU, m ->
                                                                          m.title("Let's code!")
                                                                           .glyph("icon", "code")
                                                                           .pathprefix("/lets-code")
                                                                           .includes("lets-code/*.adoc"))
                                                                  .item(Nav.Type.PAGE, p ->
                                                                          p.source("playtime.adoc")
                                                                           .glyph("icon", "home"))))
                                           .item(Nav.Type.HEADER, h -> h.title("Additional Resources"))
                                           .item(Nav.Type.LINK, l ->
                                                   l.title("Javadocs")
                                                    .glyph("icon", "info")
                                                    .href("https://docs.oracle.com/javase/8/docs/api/"))))
            .build()
            .generate(sourceDir, outputDir);

        Path index = outputDir.resolve("index.html");
        assertThat(Files.exists(index), is(true));

        Path actualConfig = outputDir.resolve("main/config.js");
        assertThat(Files.exists(actualConfig), is(true));
        assertRendering(actualConfig, sourceDir.resolve("expected-config"));

        Path home = outputDir.resolve("pages/home.js");
        assertThat(Files.exists(home), is(true));

        assertThat(Files.readAllLines(home)
                        .stream()
                        .anyMatch(line -> line.contains("to an anchor<br>")), is(true));
    }

    @Test
    void testVuetify2() {
        Path targetDir = targetDir(VuetifyBackendTest.class);
        Path sourceDir = targetDir.resolve("test-classes/vuetify2");
        Path outputDir = targetDir.resolve("vuetify/testvuetify2");
        Site site = Site.builder()
                        .options(Map.of(STRICT_IMAGES, false))
                        .page(PageFilter.builder().includes("**/*.adoc"))
                        .backend(VuetifyBackend.builder().home("home.adoc"))
                        .build();

        site.generate(sourceDir, outputDir);

        Path index = outputDir.resolve("index.html");
        assertThat(Files.exists(index), is(true));

        Path config = outputDir.resolve("main/config.js");
        assertThat(Files.exists(config), is(true));

        Path home = outputDir.resolve("pages/home.js");
        assertThat(Files.exists(home), is(true));

        Path homeCustom = outputDir.resolve("pages/home_custom.js");
        assertThat(Files.exists(homeCustom), is(true));
    }

    private static void assertRendering(Path actual, Path expected) throws IOException, DiffException {
        Patch<String> patch = DiffUtils.diff(
                Files.readAllLines(expected),
                Files.readAllLines(actual));
        if (patch.getDeltas().size() > 0) {
            fail("rendered file " + actual.toAbsolutePath() + " differs from expected: " + patch);
        }
    }
}
