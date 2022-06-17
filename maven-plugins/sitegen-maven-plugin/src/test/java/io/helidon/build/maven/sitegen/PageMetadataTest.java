/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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

import java.nio.file.Path;

import io.helidon.build.maven.sitegen.models.Page.Metadata;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests {@link Metadata}.
 */
class PageMetadataTest {

    private static final Path SOURCE_DIR = targetDir(PageMetadataTest.class).resolve("test-classes/metadata");
    private static final Site SITE = Site.create(new Backend("dummy") {});

    @Test
    public void testPageWithNoTitle() {
        try {
            readMetadata("no_title.adoc");
            fail("no_title.adoc is not a valid document");
        } catch (IllegalArgumentException ex) {
            // do nothing
        }
    }

    @Test
    public void testPageWithNoDescription() {
        Metadata m = null;
        try {
            m = readMetadata("no_description.adoc");
        } catch (Throwable ex) {
            fail("no_description.adoc is a valid document", ex);
        }
        assertThat(m.title(), is("This is a title"));
        assertThat(m.h1(), is("This is a title"));
        assertThat(m.description(), is(nullValue()));
    }

    @Test
    public void testPageWithTitleAndH1() {
        Metadata m = null;
        try {
            m = readMetadata("title_and_h1.adoc");
        } catch (Throwable ex) {
            fail("title_and_h1.adoc is a valid document", ex);
        }
        assertThat(m.title(), is("This is the document title"));
        assertThat(m.h1(), is("This is an h1 title"));
    }

    @Test
    public void testPageWithDescription() {
        Metadata m = null;
        try {
            m = readMetadata("with_description.adoc");
        } catch (Throwable ex) {
            fail("with_description.adoc is a valid document", ex);
        }
        assertThat(m.description(), is("This is a description"));
    }

    @Test
    public void testPageWithKeywords() {
        Metadata m = null;
        try {
            m = readMetadata("with_keywords.adoc");
        } catch (Throwable ex) {
            fail("with_keywords.adoc is a valid document", ex);
        }
        assertThat(m.keywords(), is("keyword1, keyword2, keyword3"));
    }

    private static Metadata readMetadata(String filename) {
        return SITE.readMetadata(SOURCE_DIR.resolve(filename));
    }
}
