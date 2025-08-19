/*
 * Copyright (c) 2021, 2025 Oracle and/or its affiliates.
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
import java.io.InputStream;

import io.helidon.build.common.xml.XMLException;

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
    void testBuilder() {
        MavenModel mavenModel = MavenModel.builder()
                .parentGroupId("com.acme")
                .parentArtifactId("acme-parent")
                .parentVersion("1.0.0-SNAPSHOT")
                .artifactId("acme-project")
                .name("ACME Project")
                .description("A project by ACME")
                .build();

        assertThat(mavenModel, is(not(nullValue())));
        assertThat(mavenModel.parent(), is(not(nullValue())));
        assertThat(mavenModel.parent().groupId(), is("com.acme"));
        assertThat(mavenModel.parent().artifactId(), is("acme-parent"));
        assertThat(mavenModel.parent().version(), is("1.0.0-SNAPSHOT"));
        assertThat(mavenModel.groupId(), is("com.acme"));
        assertThat(mavenModel.artifactId(), is("acme-project"));
        assertThat(mavenModel.version(), is("1.0.0-SNAPSHOT"));
        assertThat(mavenModel.name(), is("ACME Project"));
        assertThat(mavenModel.description(), is("A project by ACME"));
    }

    @Test
    void testRead1() {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("test-pom1.xml");
        assertThat(inputStream, is(not(nullValue())));
        MavenModel mavenModel = MavenModel.read(inputStream);
        assertThat(mavenModel, is(not(nullValue())));
        assertThat(mavenModel.parent(), is(not(nullValue())));
        assertThat(mavenModel.parent().groupId(), is("com.acme"));
        assertThat(mavenModel.parent().artifactId(), is("acme-parent"));
        assertThat(mavenModel.parent().version(), is("1.0.0-SNAPSHOT"));
        assertThat(mavenModel.groupId(), is("com.acme"));
        assertThat(mavenModel.artifactId(), is("acme-project"));
        assertThat(mavenModel.version(), is("1.0.0-SNAPSHOT"));
        assertThat(mavenModel.name(), is("ACME Project"));
        assertThat(mavenModel.description(), is("A project by ACME"));
    }

    @Test
    void testRead2() {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("test-pom2.xml");
        assertThat(inputStream, is(not(nullValue())));
        MavenModel mavenModel = MavenModel.read(inputStream);
        assertThat(mavenModel, is(not(nullValue())));
        assertThat(mavenModel.parent(), is(not(nullValue())));
        assertThat(mavenModel.parent().groupId(), is("com.acme"));
        assertThat(mavenModel.parent().artifactId(), is("acme-parent"));
        assertThat(mavenModel.parent().version(), is("1.0.0-SNAPSHOT"));
        assertThat(mavenModel.groupId(), is("com.acme"));
        assertThat(mavenModel.artifactId(), is("acme-project"));
        assertThat(mavenModel.version(), is("1.0.0-SNAPSHOT"));
        assertThat(mavenModel.name(), is("ACME Project"));
        assertThat(mavenModel.description(), is(nullValue()));
    }

    @Test
    void testSelfClosedRelativePath() {
        MavenModel mavenModel = MavenModel.read(new ByteArrayInputStream((
                "<?xml?>"
                + "<project>"
                + "<parent>"
                + "<groupId>com.acme</groupId>"
                + "<artifactId>acme-parent</artifactId>"
                + "<version>1.0.0-SNAPSHOT</version>"
                + "<relativePath/>"
                + "</parent>"
                + "<groupId>com.acme</groupId>"
                + "<artifactId>acme-project</artifactId>"
                + "<version>1.0.0-SNAPSHOT</version>"
                + "</project>")
                .getBytes(UTF_8)));
        assertThat(mavenModel, is(not(nullValue())));
        assertThat(mavenModel.parent().groupId(), is("com.acme"));
        assertThat(mavenModel.parent().artifactId(), is("acme-parent"));
        assertThat(mavenModel.parent().version(), is("1.0.0-SNAPSHOT"));
        assertThat(mavenModel.groupId(), is("com.acme"));
        assertThat(mavenModel.artifactId(), is("acme-project"));
        assertThat(mavenModel.version(), is("1.0.0-SNAPSHOT"));
    }

    @Test
    void testParentAfter() {
        MavenModel mavenModel = MavenModel.read(new ByteArrayInputStream((
                "<?xml?>"
                + "<project>"
                + "<groupId>com.acme.project</groupId>"
                + "<artifactId>acme-project</artifactId>"
                + "<version>1.0.1-SNAPSHOT</version>"
                + "<parent>"
                + "<groupId>com.acme</groupId>"
                + "<artifactId>acme-parent</artifactId>"
                + "<version>1.0.0-SNAPSHOT</version>"
                + "</parent>"
                + "</project>")
                .getBytes(UTF_8)));
        assertThat(mavenModel, is(not(nullValue())));
        assertThat(mavenModel.parent().groupId(), is("com.acme"));
        assertThat(mavenModel.parent().artifactId(), is("acme-parent"));
        assertThat(mavenModel.parent().version(), is("1.0.0-SNAPSHOT"));
        assertThat(mavenModel.groupId(), is("com.acme.project"));
        assertThat(mavenModel.artifactId(), is("acme-project"));
        assertThat(mavenModel.version(), is("1.0.1-SNAPSHOT"));
    }

    @Test
    void testPartialParsing1() {
        try {
            MavenModel mavenModel = MavenModel.read(new ByteArrayInputStream((
                    "<?xml?>"
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
            assertThat(mavenModel.parent(), is(not(nullValue())));
            assertThat(mavenModel.parent().groupId(), is("com.acme"));
            assertThat(mavenModel.parent().artifactId(), is("acme-parent"));
            assertThat(mavenModel.parent().version(), is("1.0.0-SNAPSHOT"));
            assertThat(mavenModel.groupId(), is("com.acme"));
            assertThat(mavenModel.artifactId(), is("acme-project"));
            assertThat(mavenModel.version(), is("1.0.0-SNAPSHOT"));
            assertThat(mavenModel.name(), is("ACME Project"));
            assertThat(mavenModel.description(), is("A project by ACME"));
        } catch (IllegalStateException ex) {
            fail("Should not be thrown", ex);
        }
    }

    @Test
    void testPartialParsing2() {
        try {
            MavenModel mavenModel = MavenModel.read(new ByteArrayInputStream((
                    "<?xml?>"
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
            assertThat(mavenModel.parent(), is(not(nullValue())));
            assertThat(mavenModel.parent().groupId(), is("com.acme"));
            assertThat(mavenModel.parent().artifactId(), is("acme-parent"));
            assertThat(mavenModel.parent().version(), is("1.0.0-SNAPSHOT"));
            assertThat(mavenModel.groupId(), is("com.acme"));
            assertThat(mavenModel.artifactId(), is("acme-project"));
            assertThat(mavenModel.version(), is("1.0.0-SNAPSHOT"));
            assertThat(mavenModel.name(), is("ACME Project"));
            assertThat(mavenModel.description(), is("A project by ACME"));
        } catch (IllegalStateException ex) {
            fail("Should not be thrown", ex);
        }
    }

    @Test
    void testPartialParsing3() {
        try {
            MavenModel mavenModel = MavenModel.read(new ByteArrayInputStream((
                    "<?xml?>"
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
            assertThat(mavenModel.parent(), is(nullValue()));
            assertThat(mavenModel.groupId(), is("com.acme"));
            assertThat(mavenModel.artifactId(), is("acme-project"));
            assertThat(mavenModel.version(), is("1.0.0-SNAPSHOT"));
            assertThat(mavenModel.name(), is("ACME Project"));
            assertThat(mavenModel.description(), is("A project by ACME"));
        } catch (XMLException ex) {
            fail("Should not be thrown", ex);
        }
    }
}
