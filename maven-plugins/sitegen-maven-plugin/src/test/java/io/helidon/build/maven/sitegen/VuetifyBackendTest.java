/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
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

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import io.helidon.build.maven.sitegen.asciidoctor.AsciidocEngine;

import org.junit.jupiter.api.Test;

import static io.helidon.build.maven.sitegen.TestHelper.SOURCE_DIR_PREFIX;
import static io.helidon.build.maven.sitegen.TestHelper.getFile;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author rgrecour
 */
public class VuetifyBackendTest {

    @Test
    public void testVuetify1() throws Exception {

        File sourcedir = getFile(SOURCE_DIR_PREFIX + "testvuetify1");
        Path outputdirPath = FileSystems.getDefault().getPath("target", "vuetify-backend-test", "testvuetify1");
        File outputdir = getFile(outputdirPath.toString());

        Site.builder()
                .pages(List.of(SourcePathFilter.builder()
                        .includes(List.of("**/*.adoc"))
                        .build()))
                .assets(List.of(StaticAsset.builder()
                        .includes(List.of("sunset.jpg"))
                        .target("images")
                        .build()))
                .backend(VuetifyBackend.builder()
                        .homePage("home.adoc")
                        .releases(List.of("1.0"))
                        .navigation(VuetifyNavigation.builder()
                                .title("Pet Project doc")
                                .glyph(Glyph.builder()
                                        .type("icon")
                                        .value("import_contacts")
                                        .build())
                                .items(List.of(VuetifyNavigation.Group.builder()
                                        .items(List.of(
                                                VuetifyNavigation.SubGroup.builder()
                                                        .pathprefix("/about")
                                                        .items(List.of(VuetifyNavigation.Pages.builder()
                                                                .includes(List.of("about/*.adoc"))
                                                                .build()))
                                                        .title("What is it about?")
                                                        .glyph(Glyph.builder()
                                                                .type("icon")
                                                                .value("weekend")
                                                                .build())
                                                        .build(),
                                                VuetifyNavigation.SubGroup.builder()
                                                        .pathprefix("/getting-started")
                                                        .items(List.of(VuetifyNavigation.Pages.builder()
                                                                .includes(List.of("getting-started/*.adoc"))
                                                                .build()))
                                                        .title("Getting started")
                                                        .glyph(Glyph.builder()
                                                                .type("icon")
                                                                .value("play_circle_outline")
                                                                .build())
                                                        .build(),
                                                VuetifyNavigation.SubGroup.builder()
                                                        .pathprefix("/lets-code")
                                                        .items(List.of(VuetifyNavigation.Pages.builder()
                                                                .includes(List.of("lets-code/*.adoc"))
                                                                .build()))
                                                        .title("Let's code!")
                                                        .glyph(Glyph.builder()
                                                                .type("icon")
                                                                .value("code")
                                                                .build())
                                                        .build(),
                                                VuetifyNavigation.Link.builder()
                                                        .href("https://docs.oracle.com/javase/8/docs/api/")
                                                        .title("Javadocs")
                                                        .glyph(Glyph.builder()
                                                                .type("icon")
                                                                .value("info")
                                                                .build())
                                                        .build()))
                                        .title("Main documentation")
                                        .build()))
                                .build())
                        .build())
                .build()
                .generate(sourcedir, outputdir);

        Files.copy(
                new File(sourcedir, "sunset.jpg").toPath(),
                new File(outputdir, "sunset.jpg").toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        File index = new File(outputdir, "index.html");
        assertTrue(index.exists());
        File config = new File(outputdir, "main/config.js");
        assertTrue(config.exists());

        Path homePath = outputdirPath.resolve("pages/home.js");
        assertTrue(Files.exists(homePath));

        assertTrue(Files.readAllLines(homePath).stream()
                .filter(line -> line.contains("to an anchor<br>"))
                .findFirst()
                .isPresent()
        );

    }

    @Test
    public void testVuetify2() throws Exception {
        File sourcedir = getFile(SOURCE_DIR_PREFIX + "testvuetify2");
        File outputdir = getFile("target/vuetify-backend-test/testvuetify2");
        Site.builder()
                .pages(List.of(
                        SourcePathFilter.builder()
                                .includes(List.of("**/*.adoc"))
                                .build()))
                .backend(VuetifyBackend.builder()
                        .homePage("home.adoc")
                        .build())
                .engine(SiteEngine.builder()
                        .asciidoctor(AsciidocEngine.builder()
                                .libraries(List.of("asciidoctor-diagram"))
                                .attributes(Map.of("plantumlconfig", "_plantuml-config.txt"))
                                .build())
                        .build())
                .build()
                .generate(sourcedir, outputdir);

        File index = new File(outputdir, "index.html");
        assertTrue(index.exists());
        File config = new File(outputdir, "main/config.js");
        assertTrue(config.exists());

        File home = new File(outputdir, "pages/home.js");
        assertTrue(home.exists());

        File homeCustom = new File(outputdir, "pages/home_custom.js");
        assertTrue(homeCustom.exists());
    }
}
