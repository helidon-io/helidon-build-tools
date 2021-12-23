/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.common.maven.url;

import io.helidon.build.common.maven.url.mvn.Handler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.FileUtils.zip;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests {@link Handler}.
 */
class HandlerTest {

    private static final String ARTIFACT_DIR = "com/example/test-artifact/1.2.3";
    private static final String ZIP_ARTIFACT_FILE = "test-artifact-1.2.3.zip";
    private static final String JAR_ARTIFACT_FILE = "test-artifact-1.2.3.jar";
    private static final String PKG_HANDLER = "java.protocol.handler.pkgs";

    private static Path localRepo;

    @BeforeAll
    static void beforeAllTests() throws IOException {
        Path targetDir = targetDir(HandlerTest.class);
        localRepo = unique(targetDir.resolve("handler-ut"), "repo");
        System.setProperty(MavenFileResolver.LOCAL_REPO_PROPERTY, localRepo.toString());
        Path artifact = localRepo.resolve(ARTIFACT_DIR).resolve(ZIP_ARTIFACT_FILE);
        Files.createDirectories(artifact.getParent());
        zip(artifact, targetDir.resolve("test-classes/test-artifact"));
        Files.copy(artifact, localRepo.resolve(ARTIFACT_DIR).resolve(JAR_ARTIFACT_FILE));
        System.setProperty(PKG_HANDLER, "io.helidon.build.common.maven.url");
    }

    @Test
    void testDefaultType() throws IOException {
        MavenURLConnection con = openConnection("mvn://com.example:test-artifact:1.2.3!/file1.xml");
        assertThat(con.groupId(), is("com.example"));
        assertThat(con.artifactId(), is("test-artifact"));
        assertThat(con.version(), is("1.2.3"));
        assertThat(con.type(), is("jar"));
        assertThat(con.pathFromArchive(), is("/file1.xml"));
        assertThat(con.getInputStream(), is(notNullValue()));
        assertThat(con.artifactFile(), is(localRepo.toAbsolutePath().resolve(ARTIFACT_DIR).resolve(JAR_ARTIFACT_FILE)));
    }

    @Test
    void testJar() throws IOException {
        MavenURLConnection con = openConnection("mvn://com.example:test-artifact:1.2.3:jar!/dir1/file2.xml");
        assertThat(con.groupId(), is("com.example"));
        assertThat(con.artifactId(), is("test-artifact"));
        assertThat(con.version(), is("1.2.3"));
        assertThat(con.type(), is("jar"));
        assertThat(con.getInputStream(), is(notNullValue()));
        assertThat(con.artifactFile(), is(localRepo.toAbsolutePath().resolve(ARTIFACT_DIR).resolve(JAR_ARTIFACT_FILE)));
    }

    @Test
    void testZip() throws IOException {
        MavenURLConnection con = openConnection("mvn://com.example:test-artifact:1.2.3:zip!/dir1/file2.xml");
        assertThat(con.groupId(), is("com.example"));
        assertThat(con.artifactId(), is("test-artifact"));
        assertThat(con.version(), is("1.2.3"));
        assertThat(con.type(), is("zip"));
        assertThat(con.getInputStream(), is(notNullValue()));
        assertThat(con.artifactFile(), is(localRepo.toAbsolutePath().resolve(ARTIFACT_DIR).resolve(ZIP_ARTIFACT_FILE)));
    }

    private MavenURLConnection openConnection(String url) throws IOException {
        URLConnection con = URI.create(url).toURL().openConnection();
        assertThat(con, is(instanceOf(MavenURLConnection.class)));
        return (MavenURLConnection) con;
    }
}
