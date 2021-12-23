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

import io.helidon.build.common.maven.url.MavenURLParser;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link MavenURLParser}.
 */
class MavenURLParserTest {

    @Test
    void testVersion() throws MalformedURLException {
        String urls = "mvn://io.helidon.handler:helidon-handler:3.0.0-SNAPSHOT!/some/useless/directory/test-file.xml";
        MavenURLParser parser = new MavenURLParser(urls);

        assertThat(parser.groupId(), is("io.helidon.handler"));
        assertThat(parser.artifactId(), is("helidon-handler"));
        assertThat(parser.version(), is("3.0.0-SNAPSHOT"));
        assertThat(parser.type(), is("jar"));
        assertThat(parser.path(), is("/some/useless/directory/test-file.xml"));
    }

    @Test
    void testClassifier() throws MalformedURLException {
        String urls = "mvn://io.helidon.handler:helidon-handler:3.0.0-SNAPSHOT:classifier!/some/useless/directory/test-file.xml";
        MavenURLParser parser = new MavenURLParser(urls);

        assertThat(parser.groupId(), is("io.helidon.handler"));
        assertThat(parser.artifactId(), is("helidon-handler"));
        assertThat(parser.version(), is("3.0.0-SNAPSHOT"));
        assertThat(parser.classifier(), is("classifier"));
        assertThat(parser.path(), is("/some/useless/directory/test-file.xml"));
    }

    @Test
    void testTypeJar() throws MalformedURLException {
        String urls = "mvn://io.helidon.handler:helidon-handler:3.0.0-SNAPSHOT:jar!/some/useless/directory/test-file.xml";
        MavenURLParser parser = new MavenURLParser(urls);

        assertThat(parser.groupId(), is("io.helidon.handler"));
        assertThat(parser.artifactId(), is("helidon-handler"));
        assertThat(parser.version(), is("3.0.0-SNAPSHOT"));
        assertThat(parser.type(), is("jar"));
        assertThat(parser.path(), is("/some/useless/directory/test-file.xml"));
    }

    @Test
    void testTypeZip() throws MalformedURLException {
        String urls = "mvn://io.helidon.handler:helidon-handler:3.0.0-SNAPSHOT:zip!/some/useless/directory/test-file.xml";
        MavenURLParser parser = new MavenURLParser(urls);

        assertThat(parser.groupId(), is("io.helidon.handler"));
        assertThat(parser.artifactId(), is("helidon-handler"));
        assertThat(parser.version(), is("3.0.0-SNAPSHOT"));
        assertThat(parser.type(), is("zip"));
        assertThat(parser.path(), is("/some/useless/directory/test-file.xml"));
    }

    @Test
    void testComplete() throws MalformedURLException {
        String urls = "mvn://io.helidon.handler:helidon-handler:3.0.0-SNAPSHOT:classifier:jar!/some/useless/directory/test-file.xml";
        MavenURLParser parser = new MavenURLParser(urls);

        assertThat(parser.groupId(), is("io.helidon.handler"));
        assertThat(parser.artifactId(), is("helidon-handler"));
        assertThat(parser.version(), is("3.0.0-SNAPSHOT"));
        assertThat(parser.classifier(), is("classifier"));
        assertThat(parser.type(), is("jar"));
        assertThat(parser.path(), is("/some/useless/directory/test-file.xml"));
    }
}
