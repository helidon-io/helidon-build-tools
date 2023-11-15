/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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
package io.helidon.build.common.maven;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit test for class {@link MavenModel}.
 */
class MavenModelTest {

    @Test
    void testRead() {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("test-pom.xml");
        assertThat(inputStream, is(not(nullValue())));
        MavenModel mavenModel = MavenModel.read(inputStream);
        assertThat(mavenModel, is(not(nullValue())));
        assertThat(mavenModel.getParent(), is(not(nullValue())));
        assertThat(mavenModel.getParent().getGroupId(), is("com.acme"));
        assertThat(mavenModel.getParent().getArtifactId(), is("acme-parent"));
        assertThat(mavenModel.getParent().getVersion(), is("1.0.0-SNAPSHOT"));
        assertThat(mavenModel.getGroupId(), is("com.acme"));
        assertThat(mavenModel.getArtifactId(), is("acme-project"));
        assertThat(mavenModel.getVersion(), is("1.0.0-SNAPSHOT"));
        assertThat(mavenModel.getName(), is("ACME Project"));
        assertThat(mavenModel.getDescription(), is("A project by ACME"));
    }

    @Test
    void testPartialParsing1() {
        try {
            MavenModel mavenModel = MavenModel.read(new ByteArrayInputStream((""
                    + "<project>"
                    + "<parent>"
                    + "<groupId>com.acme</groupId>"
                    + "<artifactId>acme-parent</artifactId>"
                    + "<version>1.0.0-SNAPSHOT</version>"
                    + "</parent>"
                    + "<groupId>com.acme</groupId>"
                    + "<artifactId>acme-project</artifactId>"
                    + "<version>1.0.0-SNAPSHOT</version>"
                    + "<name>ACME Project</name>"
                    + "<description>A project by ACME</description>"
                    + "<packaging>jar</packaging>"
                    + "<#INVALID#>"
                    + "</project>").getBytes(UTF_8)));
            assertThat(mavenModel, is(not(nullValue())));
            assertThat(mavenModel.getParent(), is(not(nullValue())));
            assertThat(mavenModel.getParent().getGroupId(), is("com.acme"));
            assertThat(mavenModel.getParent().getArtifactId(), is("acme-parent"));
            assertThat(mavenModel.getParent().getVersion(), is("1.0.0-SNAPSHOT"));
            assertThat(mavenModel.getGroupId(), is("com.acme"));
            assertThat(mavenModel.getArtifactId(), is("acme-project"));
            assertThat(mavenModel.getVersion(), is("1.0.0-SNAPSHOT"));
            assertThat(mavenModel.getName(), is("ACME Project"));
            assertThat(mavenModel.getDescription(), is("A project by ACME"));
        } catch (IllegalStateException ex) {
            fail("Should not be thrown", ex);
        }
    }

    @Test
    void testPartialParsing2() {
        try {
            MavenModel mavenModel = MavenModel.read(new ByteArrayInputStream((""
                    + "<project>"
                    + "<parent>"
                    + "<groupId>com.acme</groupId>"
                    + "<artifactId>acme-parent</artifactId>"
                    + "<version>1.0.0-SNAPSHOT</version>"
                    + "</parent>"
                    + "<groupId>com.acme</groupId>"
                    + "<artifactId>acme-project</artifactId>"
                    + "<version>1.0.0-SNAPSHOT</version>"
                    + "<name>ACME Project</name>"
                    + "<description>A project by ACME</description>"
                    + "</project>"
                    + "INVALID").getBytes(UTF_8)));
            assertThat(mavenModel, is(not(nullValue())));
            assertThat(mavenModel.getParent(), is(not(nullValue())));
            assertThat(mavenModel.getParent().getGroupId(), is("com.acme"));
            assertThat(mavenModel.getParent().getArtifactId(), is("acme-parent"));
            assertThat(mavenModel.getParent().getVersion(), is("1.0.0-SNAPSHOT"));
            assertThat(mavenModel.getGroupId(), is("com.acme"));
            assertThat(mavenModel.getArtifactId(), is("acme-project"));
            assertThat(mavenModel.getVersion(), is("1.0.0-SNAPSHOT"));
            assertThat(mavenModel.getName(), is("ACME Project"));
            assertThat(mavenModel.getDescription(), is("A project by ACME"));
        } catch (IllegalStateException ex) {
            fail("Should not be thrown", ex);
        }
    }

    @Test
    void testPartialParsing3() {
        try {
            MavenModel mavenModel = MavenModel.read(new ByteArrayInputStream((""
                    + "<project>"
                    + "<parent/>"
                    + "<groupId>com.acme</groupId>"
                    + "<artifactId>acme-project</artifactId>"
                    + "<version>1.0.0-SNAPSHOT</version>"
                    + "<name>ACME Project</name>"
                    + "<description>A project by ACME</description>"
                    + "</project>"
                    + "INVALID").getBytes(UTF_8)));
            assertThat(mavenModel, is(not(nullValue())));
            assertThat(mavenModel.getParent(), is(nullValue()));
            assertThat(mavenModel.getGroupId(), is("com.acme"));
            assertThat(mavenModel.getArtifactId(), is("acme-project"));
            assertThat(mavenModel.getVersion(), is("1.0.0-SNAPSHOT"));
            assertThat(mavenModel.getName(), is("ACME Project"));
            assertThat(mavenModel.getDescription(), is("A project by ACME"));
        } catch (IllegalStateException ex) {
            fail("Should not be thrown", ex);
        }
    }
}
