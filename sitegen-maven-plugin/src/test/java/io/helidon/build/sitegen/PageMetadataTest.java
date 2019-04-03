/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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

import io.helidon.build.sitegen.Page.Metadata;
import io.helidon.build.sitegen.asciidoctor.AsciidocEngine;
import io.helidon.build.sitegen.asciidoctor.AsciidocPageRenderer;
import io.helidon.build.sitegen.freemarker.FreemarkerEngine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.helidon.build.sitegen.TestHelper.SOURCE_DIR_PREFIX;
import static io.helidon.build.sitegen.TestHelper.assertString;
import static io.helidon.build.sitegen.TestHelper.getFile;
import org.junit.jupiter.api.AfterAll;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author rgrecour
 */
public class PageMetadataTest {

    private static final File SOURCEDIR = getFile(SOURCE_DIR_PREFIX + "testmetadata");
    private static final String BACKEND_NAME = "dummy";
    private static AsciidocPageRenderer pageRenderer;

    @BeforeAll
    public static void init(){
        SiteEngine.register(BACKEND_NAME, new SiteEngine(
                new FreemarkerEngine(BACKEND_NAME, null, null),
                new AsciidocEngine(BACKEND_NAME, null, null, null)));
        pageRenderer = new AsciidocPageRenderer(BACKEND_NAME);
    }

    @AfterAll
    public static void cleanup(){
        SiteEngine.get(BACKEND_NAME).asciidoc().unregister();
        SiteEngine.deregister(BACKEND_NAME);
    }

    private static Metadata readMetadata(String fname){
        return pageRenderer.readMetadata(new File(SOURCEDIR, fname));
    }

    @Test
    public void testPageWithNoTitle(){
        try {
            readMetadata("no_title.adoc");
            fail("no_title.adoc is not a valid document");
        } catch (IllegalArgumentException ex) {
        }
    }

    @Test
    public void testPageWithNoDescription(){
        Metadata m = null;
        try {
            m = readMetadata("no_description.adoc");
        } catch (Throwable ex) {
            fail("no_description.adoc is a valid document", ex);
        }
        if (m == null) {
            throw new AssertionError("metadata is null");
        }
        assertString("This is a title", m.getTitle(), "metadata.title");
        assertString("This is a title", m.getH1(), "metadata.h1");
        assertNull(m.getDescription(), "metadata.description");
    }

    @Test
    public void testPageWithTitleAndH1(){
        Metadata m = null;
        try {
            m = readMetadata("title_and_h1.adoc");
        } catch (Throwable ex) {
            fail("title_and_h1.adoc is a valid document", ex);
        }
        if (m == null) {
            throw new AssertionError("metadata is null");
        }
        assertString("This is the document title", m.getTitle(), "metadata.title");
        assertString("This is an h1 title", m.getH1(), "metadata.h1");
    }

    @Test
    public void testPageWithDescription(){
        Metadata m = null;
        try {
            m = readMetadata("with_description.adoc");
        } catch (Throwable ex) {
            fail("with_description.adoc is a valid document", ex);
        }
        if (m == null) {
            throw new AssertionError("metadata is null");
        }
        assertString("This is a description", m.getDescription(), "metadata.description");
    }

    @Test
    public void testPageWithKeywords(){
        Metadata m = null;
        try {
            m = readMetadata("with_keywords.adoc");
        } catch (Throwable ex) {
            fail("with_keywords.adoc is a valid document", ex);
        }
        if (m == null) {
            throw new AssertionError("metadata is null");
        }
        assertString("keyword1, keyword2, keyword3", m.getKeywords(), "metadata.keywords");
    }
}
