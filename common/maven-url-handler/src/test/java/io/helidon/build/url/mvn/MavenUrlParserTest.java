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

package io.helidon.build.url.mvn;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MavenUrlParserTest {

    @Test
    public void testURLVersion() throws MalformedURLException {
        String mvnPath = "io.helidon.handler:helidon-archetype:3.0.0-SNAPSHOT/some/useless/directory/test-file.xml";
        MavenUrlParser parser = new MavenUrlParser(mvnPath);

        assertThat(parser.groupId(), is("io.helidon.handler"));
        assertThat(parser.artifactId(), is("helidon-archetype"));
        assertThat(parser.version(), is("3.0.0-SNAPSHOT"));
        assertThat(parser.type(), is("jar"));
        assertThat(parser.pathFromArchive(), is("some/useless/directory/test-file.xml"));
    }

    @Test
    public void testURLClassifier() throws MalformedURLException {
        String mvnPath = "io.helidon.archetypes:helidon-archetype:3.0.0-SNAPSHOT:classifier/some/useless/directory/test-file.xml";
        MavenUrlParser parser = new MavenUrlParser(mvnPath);

        assertThat(parser.groupId(), is("io.helidon.archetypes"));
        assertThat(parser.artifactId(), is("helidon-archetype"));
        assertThat(parser.version(), is("3.0.0-SNAPSHOT"));
        assertThat(parser.classifier().get(), is("classifier"));
        assertThat(parser.pathFromArchive(), is("some/useless/directory/test-file.xml"));
    }

    @Test
    public void testURLTypeJar() throws MalformedURLException {
        String mvnPath = "io.helidon.archetypes:helidon-archetype:3.0.0-SNAPSHOT:jar/some/useless/directory/test-file.xml";
        MavenUrlParser parser = new MavenUrlParser(mvnPath);

        assertThat(parser.groupId(), is("io.helidon.archetypes"));
        assertThat(parser.artifactId(), is("helidon-archetype"));
        assertThat(parser.version(), is("3.0.0-SNAPSHOT"));
        assertThat(parser.type(), is("jar"));
        assertThat(parser.pathFromArchive(), is("some/useless/directory/test-file.xml"));
    }

    @Test
    public void testURLTypeZip() throws MalformedURLException {
        String mvnPath = "io.helidon.archetypes:helidon-archetype:3.0.0-SNAPSHOT:zip/some/useless/directory/test-file.xml";
        MavenUrlParser parser = new MavenUrlParser(mvnPath);

        assertThat(parser.groupId(), is("io.helidon.archetypes"));
        assertThat(parser.artifactId(), is("helidon-archetype"));
        assertThat(parser.version(), is("3.0.0-SNAPSHOT"));
        assertThat(parser.type(), is("zip"));
        assertThat(parser.pathFromArchive(), is("some/useless/directory/test-file.xml"));
    }

    @Test
    public void testURLComplete() throws MalformedURLException {
        String mvnPath = "io.helidon.archetypes:helidon-archetype:3.0.0-SNAPSHOT:classifier:jar/some/useless/directory/test-file.xml";
        MavenUrlParser parser = new MavenUrlParser(mvnPath);

        assertThat(parser.groupId(), is("io.helidon.archetypes"));
        assertThat(parser.artifactId(), is("helidon-archetype"));
        assertThat(parser.version(), is("3.0.0-SNAPSHOT"));
        assertThat(parser.classifier().get(), is("classifier"));
        assertThat(parser.type(), is("jar"));
        assertThat(parser.pathFromArchive(), is("some/useless/directory/test-file.xml"));
    }

}
