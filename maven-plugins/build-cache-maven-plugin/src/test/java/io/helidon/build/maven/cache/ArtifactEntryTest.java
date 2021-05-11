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
package io.helidon.build.maven.cache;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link ArtifactEntry}.
 */
class ArtifactEntryTest {

    @Test
    void testFromXml() throws IOException, XmlPullParserException {
        ArtifactEntry artifact;

        artifact = ArtifactEntry.fromXml(Xpp3DomBuilder.build(new StringReader(
                "<artifact " +
                        "file=\"foo.zip\" " +
                        "extension=\"zip\" " +
                        "language=\"bar\" " +
                        "includesDependencies=\"false\" " +
                        "addedToClasspath=\"true\" " +
                        "/>"
        )));
        assertThat(artifact.file(), is("foo.zip"));
        assertThat(artifact.extension(), is("zip"));
        assertThat(artifact.classifier(), is(nullValue()));
        assertThat(artifact.language(), is("bar"));
        assertThat(artifact.includesDependencies(), is(false));
        assertThat(artifact.addedToClasspath(), is(true));

        artifact = ArtifactEntry.fromXml(Xpp3DomBuilder.build(new StringReader(
                "<artifact " +
                        "file=\"foo.zip\" " +
                        "extension=\"zip\" " +
                        "classifier=\"alice\" " +
                        "language=\"bar\" " +
                        "includesDependencies=\"true\" " +
                        "addedToClasspath=\"false\" " +
                        "/>"
        )));
        assertThat(artifact.file(), is("foo.zip"));
        assertThat(artifact.extension(), is("zip"));
        assertThat(artifact.classifier(), is("alice"));
        assertThat(artifact.language(), is("bar"));
        assertThat(artifact.includesDependencies(), is(true));
        assertThat(artifact.addedToClasspath(), is(false));
    }

    @Test
    void testToXml() {
        Xpp3Dom artifact;

        artifact = new ArtifactEntry(
                "foo.zip",
                "zip",
                null,
                "bar",
                false,
                true)
                .toXml();
        assertThat(artifact.getAttribute("file"), is("foo.zip"));
        assertThat(artifact.getAttribute("extension"), is("zip"));
        assertThat(artifact.getAttribute("classifier"), is(nullValue()));
        assertThat(artifact.getAttribute("language"), is("bar"));
        assertThat(artifact.getAttribute("includesDependencies"), is("false"));
        assertThat(artifact.getAttribute("addedToClasspath"), is("true"));

        artifact = new ArtifactEntry(
                "foo.zip",
                "zip",
                "alice",
                "bar",
                true,
                false)
                .toXml();
        assertThat(artifact.getAttribute("file"), is("foo.zip"));
        assertThat(artifact.getAttribute("extension"), is("zip"));
        assertThat(artifact.getAttribute("classifier"), is("alice"));
        assertThat(artifact.getAttribute("language"), is("bar"));
        assertThat(artifact.getAttribute("includesDependencies"), is("true"));
        assertThat(artifact.getAttribute("addedToClasspath"), is("false"));
    }

    @Test
    void testToArtifact() {
        Build build = new Build() {
            @Override
            public String getDirectory() {
                return "target";
            }
        };
        Model model = new Model() {
            @Override
            public Build getBuild() {
                return build;
            }

            @Override
            public File getProjectDirectory() {
                return new File(".");
            }
        };
        MavenProject project = new MavenProject() {
            @Override
            public Model getModel() {
                return model;
            }

            @Override
            public String getGroupId() {
                return "com.acme";
            }

            @Override
            public String getArtifactId() {
                return "foo";
            }

            @Override
            public String getVersion() {
                return "1.0";
            }
        };
        Artifact artifact;
        ArtifactHandler artifactHandler;

        artifact = new ArtifactEntry("foo.zip",
                "zip",
                null,
                "bar",
                false,
                true)
                .toArtifact(project);
        assertThat(artifact.getGroupId(), is("com.acme"));
        assertThat(artifact.getArtifactId(), is("foo"));
        assertThat(artifact.getVersion(), is("1.0"));
        assertThat(artifact.getClassifier(), is(nullValue()));
        assertThat(artifact.getType(), is("zip"));
        artifactHandler = artifact.getArtifactHandler();
        assertThat(artifactHandler, is(not(nullValue())));
        assertThat(artifactHandler.getLanguage(), is("bar"));
        assertThat(artifactHandler.getClassifier(), is(nullValue()));
        assertThat(artifactHandler.getExtension(), is("zip"));
        assertThat(artifactHandler.isIncludesDependencies(), is(false));
        assertThat(artifactHandler.isAddedToClasspath(), is(true));

        artifact = new ArtifactEntry("foo.zip",
                "zip",
                "alice",
                "bar",
                true,
                false)
                .toArtifact(project);
        assertThat(artifact.getGroupId(), is("com.acme"));
        assertThat(artifact.getArtifactId(), is("foo"));
        assertThat(artifact.getVersion(), is("1.0"));
        assertThat(artifact.getClassifier(), is("alice"));
        assertThat(artifact.getType(), is("zip"));
        artifactHandler = artifact.getArtifactHandler();
        assertThat(artifactHandler, is(not(nullValue())));
        assertThat(artifactHandler.getLanguage(), is("bar"));
        assertThat(artifactHandler.getClassifier(), is(nullValue()));
        assertThat(artifactHandler.getExtension(), is("zip"));
        assertThat(artifactHandler.isIncludesDependencies(), is(true));
        assertThat(artifactHandler.isAddedToClasspath(), is(false));
    }
}
