/*
 * Copyright (c) 2021, 2026 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.common.test.utils.ConfigurationParameterSource;
import io.helidon.build.common.test.utils.JUnitLauncher;
import io.helidon.build.common.xml.XMLElement;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;

import static io.helidon.build.common.test.utils.FileMatchers.fileExists;
import static io.helidon.build.common.test.utils.LogMatchers.matches;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Integration test that verifies the projects under {@code src/it/projects}.
 */
@EnabledIfSystemProperty(named = JUnitLauncher.IDENTITY_PROP, matches = "true")
class ProjectsTestIT {

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test1(String basedir) throws IOException {
        Path baseDir = Path.of(basedir);
        String actual = Files.readString(baseDir.resolve("build.log"));
        assertThat(actual, matches("""
                ~...
                [INFO] BUILD SUCCESS
                ~...
                [INFO] BUILD SUCCESS
                ~...
                [INFO] Cached | org.apache.maven.plugins:maven-resources-plugin:2.7:resources@default-resources
                [INFO] Cached | org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile@default-compile
                [INFO] Cached | org.apache.maven.plugins:maven-resources-plugin:2.7:testResources@default-testResources
                [INFO] Cached | org.apache.maven.plugins:maven-compiler-plugin:3.13.0:testCompile@default-testCompile
                [INFO] Diff   | org.apache.maven.plugins:maven-surefire-plugin:2.12.4:test@default-test
                [INFO]            +- skipTests was 'true' but is now '${skipTests}'
                [INFO] Cached | org.apache.maven.plugins:maven-jar-plugin:2.4:jar@default-jar
                [INFO] Cached | org.apache.maven.plugins:maven-install-plugin:2.4:install@default-install
                ~...
                [INFO] BUILD SUCCESS
                ~...
                """));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test2(String basedir) throws IOException {
        Path baseDir = Path.of(basedir);
        String actual = Files.readString(baseDir.resolve("build.log"));
        assertThat(actual, matches("""
                ~...
                [INFO] BUILD SUCCESS
                ~...
                [INFO] Cache is disabled
                ~...
                [INFO] Downstream state(s) not available, state is ignored
                ~...
                [INFO] BUILD SUCCESS
                ~...
                """));
    }

    @ParameterizedTest
    @ConfigurationParameterSource("basedir")
    void test3(String basedir) throws IOException {
        Path basepath = Path.of(basedir);
        Path apidocs = basepath.resolve("target/apidocs");
        Path greetings = apidocs.resolve("io/helidon/build/cache/test/Greeting.html");
        assertThat(greetings, fileExists());

        Path stagingDir = basepath.resolve("staging");
        Path artifactDir = stagingDir.resolve("io/helidon/build-tools/cache/tests/test3/1.0.0-SNAPSHOT");

        Path mavenMetadataFile = artifactDir.resolve("maven-metadata.xml");
        assertThat(mavenMetadataFile, fileExists());

        XMLElement elt = XMLElement.read(mavenMetadataFile);
        String timestamp = elt.childAt("versioning", "snapshot", "timestamp")
                .map(XMLElement::value)
                .orElseThrow(() -> new IllegalStateException("Unable to get timestamp"));

        String version = "1.0.0-" + timestamp + "-1";

        List<String> files;
        try (Stream<Path> dirStream = Files.list(artifactDir)) {
            files = dirStream.filter(Files::isRegularFile)
                    .map(artifactDir::relativize)
                    .map(Path::toString)
                    .sorted()
                    .collect(Collectors.toList());
        }

        assertThat(files, is(List.of(
                "maven-metadata.xml",
                "maven-metadata.xml.md5",
                "maven-metadata.xml.sha1",
                "test3-" + version + "-javadoc.jar",
                "test3-" + version + "-javadoc.jar.md5",
                "test3-" + version + "-javadoc.jar.sha1",
                "test3-" + version + ".jar",
                "test3-" + version + ".jar.md5",
                "test3-" + version + ".jar.sha1",
                "test3-" + version + ".pom",
                "test3-" + version + ".pom.md5",
                "test3-" + version + ".pom.sha1"
        )));
    }
}
