/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.sitegen;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import io.helidon.build.sitegen.asciidoctor.AsciidocEngine;

import org.junit.jupiter.api.Test;

import static io.helidon.common.CollectionsHelper.listOf;
import static io.helidon.common.CollectionsHelper.mapOf;
import static io.helidon.build.sitegen.TestHelper.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

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
                .pages(listOf(SourcePathFilter.builder()
                        .includes(listOf("**/*.adoc"))
                        .build()))
                .assets(listOf(StaticAsset.builder()
                        .includes(listOf("sunset.jpg"))
                        .target("images")
                        .build()))
                .backend(VuetifyBackend.builder()
                        .homePage("home.adoc")
                        .releases(listOf("1.0"))
                        .navigation(VuetifyNavigation.builder()
                                .title("Pet Project doc")
                                .glyph(Glyph.builder()
                                        .type("icon")
                                        .value("import_contacts")
                                        .build())
                                .items(listOf(VuetifyNavigation.Group.builder()
                                        .items(listOf(
                                                VuetifyNavigation.SubGroup.builder()
                                                        .pathprefix("/about")
                                                        .items(listOf(VuetifyNavigation.Pages.builder()
                                                                .includes(listOf("about/*.adoc"))
                                                                .build()))
                                                        .title("What is it about?")
                                                        .glyph(Glyph.builder()
                                                                .type("icon")
                                                                .value("weekend")
                                                                .build())
                                                        .build(),
                                                VuetifyNavigation.SubGroup.builder()
                                                        .pathprefix("/getting-started")
                                                        .items(listOf(VuetifyNavigation.Pages.builder()
                                                                .includes(listOf("getting-started/*.adoc"))
                                                                .build()))
                                                        .title("Getting started")
                                                        .glyph(Glyph.builder()
                                                                .type("icon")
                                                                .value("play_circle_outline")
                                                                .build())
                                                        .build(),
                                                VuetifyNavigation.SubGroup.builder()
                                                        .pathprefix("/lets-code")
                                                        .items(listOf(VuetifyNavigation.Pages.builder()
                                                                .includes(listOf("lets-code/*.adoc"))
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
                .pages(listOf(
                        SourcePathFilter.builder()
                                .includes(listOf("**/*.adoc"))
                                .build()))
                .backend(VuetifyBackend.builder()
                        .homePage("home.adoc")
                        .build())
                .engine(SiteEngine.builder()
                        .asciidoctor(AsciidocEngine.builder()
                                .libraries(listOf("asciidoctor-diagram"))
                                .attributes(mapOf("plantumlconfig", "_plantuml-config.txt"))
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
