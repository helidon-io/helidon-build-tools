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

package io.helidon.build.maven.sitegen.maven;

import java.nio.file.Path;

import io.helidon.build.common.test.utils.WorkDirExtension;
import io.helidon.build.maven.sitegen.Site;
import io.helidon.build.maven.sitegen.VuetifyBackend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.helidon.build.maven.sitegen.maven.MavenPluginHelper.mojo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link GenerateMojo}.
 */
@ExtendWith(WorkDirExtension.class)
class GenerateMojoTest {

    @Test
    void testBasicBackendConfiguration(Path workDir) throws Exception {
        GenerateMojo mojo = mojo("generate-mojo/pom-basic-backend.xml", workDir, "generate", GenerateMojo.class);
        mojo.execute();
        Site site = mojo.getSite();
        assertThat(site, is(not(nullValue())));
        assertThat(site.backend().name(), is("basic"));
    }

    @Test
    void testVuetifyBackendConfiguration(Path workDir) throws Exception {
        GenerateMojo mojo = mojo("generate-mojo/pom-vuetify-backend.xml", workDir, "generate", GenerateMojo.class);
        mojo.execute();
        Site site = mojo.getSite();
        assertThat(site, is(not(nullValue())));
        assertThat(site.backend().name(), is("vuetify"));
        assertThat(site.backend(), is(instanceOf(VuetifyBackend.class)));
        VuetifyBackend backend = (VuetifyBackend) site.backend();
        assertThat(backend.releases(), hasItems("bar", "test-version"));
    }
}
