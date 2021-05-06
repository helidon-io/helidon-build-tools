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

import io.helidon.build.maven.sitegen.maven.GenerateMojo;

import org.junit.jupiter.api.Test;

import static io.helidon.build.maven.sitegen.TestHelper.assertList;
import static io.helidon.build.maven.sitegen.TestHelper.assertString;
import static io.helidon.build.maven.sitegen.TestHelper.assertType;
import static io.helidon.build.maven.sitegen.TestHelper.getFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * @author rgrecour
 */
public class GenerateMojoTest {

    private static final File OUTPUT_DIR = getFile("target/generate-mojo");

    @Test
    public void testBasicBackendConfiguration() throws Exception {
        GenerateMojo mojo = MavenPluginHelper.getInstance().getMojo(
                "generate-mojo/pom-basic-backend.xml",
                OUTPUT_DIR,
                "generate",
                GenerateMojo.class);
        mojo.execute();
        Site site = mojo.getSite();
        assertNotNull(site, "site is null");
        assertEquals("basic", site.getBackend().getName());
    }

    @Test
    public void testVuetifyBackendConfiguration() throws Exception {
        GenerateMojo mojo = MavenPluginHelper.getInstance().getMojo(
                "generate-mojo/pom-vuetify-backend.xml",
                OUTPUT_DIR,
                "generate",
                GenerateMojo.class);
        mojo.execute();
        Site site = mojo.getSite();
        assertNotNull(site, "site is null");
        assertEquals("vuetify", site.getBackend().getName());
        VuetifyBackend backend = assertType(site.getBackend(), VuetifyBackend.class, "vuetify backend class");
        assertList(2, backend.getReleases(), "backend.releases");
        assertString("bar", backend.getReleases().get(0), "backend.releases[0]");
        assertString("test-version", backend.getReleases().get(1), "backend.releases[1]");
    }
}
