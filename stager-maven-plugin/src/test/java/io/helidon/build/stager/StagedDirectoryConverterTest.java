/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.stager;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link StagedDirectoryConverter}.
 */
class StagedDirectoryConverterTest {

    @Test
    public void testConverter() throws Exception {
        Reader reader = new InputStreamReader(StagedDirectoryConverterTest.class.getResourceAsStream("/testconfig.xml"));
        PlexusConfiguration plexusConfiguration = new XmlPlexusConfiguration(Xpp3DomBuilder.build(reader));
        StagedDirectory stagedDirectory = new StagedDirectoryConverter().fromConfiguration(plexusConfiguration);
        assertThat(stagedDirectory, is(not(nullValue())));
        assertThat(stagedDirectory.target(), is("${project.build.directory}/site"));
        assertThat(stagedDirectory.tasks().size(), is(14));
        List<StagingTask> tasks = stagedDirectory.tasks();

        assertThat(tasks.get(0), is(instanceOf(UnpackArtifactTask.class)));
        UnpackArtifactTask unpack1 = (UnpackArtifactTask) tasks.get(0);
        assertThat(unpack1.gav().groupId(), is("io.helidon"));
        assertThat(unpack1.gav().artifactId(), is("helidon-docs"));
        assertThat(unpack1.gav().version(), is("{version}"));
        assertThat(unpack1.excludes(), is("META-INF/**"));
        assertThat(unpack1.includes(), is(nullValue()));
        assertThat(unpack1.target(), is("docs/{version}"));
        assertThat(unpack1.iterators().size(), is(1));
        assertThat(unpack1.iterators().get(0).size(), is(1));
        assertThat(unpack1.iterators().get(0).containsKey("version"), is(true));
        assertThat(unpack1.iterators().get(0).get("version").size(), is(5));
        assertThat(unpack1.iterators().get(0).get("version").get(0), is("${docs.1.version}"));
        assertThat(unpack1.iterators().get(0).get("version").get(1), is("1.4.3"));
        assertThat(unpack1.iterators().get(0).get("version").get(2), is("1.4.2"));
        assertThat(unpack1.iterators().get(0).get("version").get(3), is("1.4.1"));
        assertThat(unpack1.iterators().get(0).get("version").get(4), is("1.4.0"));

        assertThat(tasks.get(1), is(instanceOf(UnpackArtifactTask.class)));
        UnpackArtifactTask unpack2 = (UnpackArtifactTask) tasks.get(1);
        assertThat(unpack2.gav().groupId(), is("io.helidon"));
        assertThat(unpack2.gav().artifactId(), is("helidon-docs"));
        assertThat(unpack2.gav().version(), is("{version}"));
        assertThat(unpack2.gav().classifier(), is("site"));
        assertThat(unpack2.excludes(), is("META-INF/**"));
        assertThat(unpack2.includes(), is(nullValue()));
        assertThat(unpack2.target(), is("docs/{version}"));
        assertThat(unpack2.iterators().size(), is(1));
        assertThat(unpack2.iterators().get(0).size(), is(1));
        assertThat(unpack2.iterators().get(0).containsKey("version"), is(true));
        assertThat(unpack2.iterators().get(0).get("version").size(), is(1));
        assertThat(unpack2.iterators().get(0).get("version").get(0), is("${docs.2.version}"));

        assertThat(tasks.get(2), is(instanceOf(SymlinkTask.class)));
        SymlinkTask symlink1 = (SymlinkTask) tasks.get(2);
        assertThat(symlink1.source(), is("./${docs.latest.version}"));
        assertThat(symlink1.target(), is("docs/latest"));

        assertThat(tasks.get(3), is(instanceOf(SymlinkTask.class)));
        SymlinkTask symlink2 = (SymlinkTask) tasks.get(3);
        assertThat(symlink2.source(), is("./${docs.1.version}"));
        assertThat(symlink2.target(), is("docs/v1"));

        assertThat(tasks.get(4), is(instanceOf(SymlinkTask.class)));
        SymlinkTask symlink3 = (SymlinkTask) tasks.get(4);
        assertThat(symlink3.source(), is("./${docs.2.version}"));
        assertThat(symlink3.target(), is("docs/v2"));

        assertThat(tasks.get(5), is(instanceOf(SymlinkTask.class)));
        SymlinkTask symlink4 = (SymlinkTask) tasks.get(5);
        assertThat(symlink4.source(), is("./${cli.latest.version}"));
        assertThat(symlink4.target(), is("cli/latest"));

        assertThat(tasks.get(6), is(instanceOf(DownloadTask.class)));
        DownloadTask download1 = (DownloadTask) tasks.get(6);
        assertThat(download1.url(), is("https://helidon.io/cli-data/{version}/cli-data.zip"));
        assertThat(download1.target(), is("cli-data/{version}/cli-data.zip"));
        assertThat(download1.iterators().size(), is(1));
        assertThat(download1.iterators().get(0).size(), is(1));
        assertThat(download1.iterators().get(0).containsKey("version"), is(true));
        assertThat(download1.iterators().get(0).get("version").size(), is(1));
        assertThat(download1.iterators().get(0).get("version").get(0), is("2.0.0-M1"));

        assertThat(tasks.get(7), is(instanceOf(DownloadTask.class)));
        DownloadTask download2 = (DownloadTask) tasks.get(7);
        assertThat(download2.url(), is("${build-tools.download.url}/{version}/helidon-cli-{platform}-amd64"));
        assertThat(download2.target(), is("cli/{version}"));
        assertThat(download2.iterators().size(), is(1));
        assertThat(download2.iterators().get(0).size(), is(2));
        assertThat(download2.iterators().get(0).containsKey("platform"), is(true));
        assertThat(download2.iterators().get(0).get("platform").size(), is(2));
        assertThat(download2.iterators().get(0).get("platform").get(0), is("darwin"));
        assertThat(download2.iterators().get(0).get("platform").get(1), is("linux"));
        assertThat(download2.iterators().get(0).containsKey("version"), is(true));
        assertThat(download2.iterators().get(0).get("version").size(), is(2));
        assertThat(download2.iterators().get(0).get("version").get(0), is("${cli.latest.version}"));
        assertThat(download2.iterators().get(0).get("version").get(1), is("2.0.0-M4"));

        assertThat(tasks.get(8), is(instanceOf(ArchiveTask.class)));
        ArchiveTask archive = (ArchiveTask) tasks.get(8);
        assertThat(archive.tasks().size(), is(4));

        assertThat(archive.tasks().get(0), is(instanceOf(CopyArtifactTask.class)));
        CopyArtifactTask archiveCopyArtifact1 = (CopyArtifactTask) archive.tasks().get(0);
        assertThat(archiveCopyArtifact1.gav().groupId(), is("io.helidon.archetypes"));
        assertThat(archiveCopyArtifact1.gav().artifactId(), is("helidon-archetype-catalog"));
        assertThat(archiveCopyArtifact1.gav().version(), is("${cli.data.latest.version}"));
        assertThat(archiveCopyArtifact1.gav().type(), is("xml"));
        assertThat(archiveCopyArtifact1.target(), is("archetype-catalog.xml"));

        assertThat(archive.tasks().get(1), is(instanceOf(CopyArtifactTask.class)));
        CopyArtifactTask archiveCopyArtifact2 = (CopyArtifactTask) archive.tasks().get(1);
        assertThat(archiveCopyArtifact2.gav().groupId(), is("io.helidon.archetypes"));
        assertThat(archiveCopyArtifact2.gav().artifactId(), is("helidon-bare-se"));
        assertThat(archiveCopyArtifact2.gav().version(), is("${cli.data.latest.version}"));

        assertThat(archive.tasks().get(2), is(instanceOf(CopyArtifactTask.class)));
        CopyArtifactTask archiveCopyArtifact3 = (CopyArtifactTask) archive.tasks().get(2);
        assertThat(archiveCopyArtifact3.gav().groupId(), is("io.helidon.archetypes"));
        assertThat(archiveCopyArtifact3.gav().artifactId(), is("helidon-bare-mp"));
        assertThat(archiveCopyArtifact3.gav().version(), is("${cli.data.latest.version}"));

        assertThat(archive.tasks().get(3), is(instanceOf(TemplateTask.class)));
        TemplateTask archiveTemplate1 = (TemplateTask) archive.tasks().get(3);
        assertThat(archiveTemplate1.source(), is("src/cli-metadata.properties.mustache"));
        assertThat(archiveTemplate1.target(), is("metadata.properties"));
        assertThat(archiveTemplate1.variables().size(), is(4));
        assertThat(archiveTemplate1.variables().containsKey("helidonVersion"), is(true));
        assertThat(archiveTemplate1.variables().get("helidonVersion").isSimple(), is(true));
        assertThat(archiveTemplate1.variables().get("helidonVersion").asSimple().text(), is("${cli.data.latest.version}"));
        assertThat(archiveTemplate1.variables().containsKey("buildtoolsVersion"), is(true));
        assertThat(archiveTemplate1.variables().get("buildtoolsVersion").isSimple(), is(true));
        assertThat(archiveTemplate1.variables().get("buildtoolsVersion").asSimple().text(), is("${cli.maven.plugin.version}"));
        assertThat(archiveTemplate1.variables().containsKey("cliVersion"), is(true));
        assertThat(archiveTemplate1.variables().get("cliVersion").isSimple(), is(true));
        assertThat(archiveTemplate1.variables().get("cliVersion").asSimple().text(), is("${cli.latest.version}"));
        assertThat(archiveTemplate1.variables().containsKey("cliUpdateMessages"), is(true));
        assertThat(archiveTemplate1.variables().get("cliUpdateMessages").isList(), is(true));

        List<VariableValue> cliUpdateMessages = archiveTemplate1.variables().get("cliUpdateMessages").asList().list();

        assertThat(cliUpdateMessages.size(), is(3));
        assertThat(cliUpdateMessages.get(0).isMap(), is(true));
        assertThat(cliUpdateMessages.get(0).asMap().map().size(), is(2));
        assertThat(cliUpdateMessages.get(0).asMap().map().containsKey("version"), is(true));
        assertThat(cliUpdateMessages.get(0).asMap().map().get("version").isSimple(), is(true));
        assertThat(cliUpdateMessages.get(0).asMap().map().get("version").asSimple().text(), is("2.0.0-M2"));
        assertThat(cliUpdateMessages.get(0).asMap().map().containsKey("message"), is(true));
        assertThat(cliUpdateMessages.get(0).asMap().map().get("message").isSimple(), is(true));
        assertThat(cliUpdateMessages.get(0).asMap().map().get("message").asSimple().text(), is("Major dev command enhancements"));

        assertThat(cliUpdateMessages.get(1).isMap(), is(true));
        assertThat(cliUpdateMessages.get(1).asMap().map().size(), is(2));
        assertThat(cliUpdateMessages.get(1).asMap().map().containsKey("version"), is(true));
        assertThat(cliUpdateMessages.get(1).asMap().map().get("version").isSimple(), is(true));
        assertThat(cliUpdateMessages.get(1).asMap().map().get("version").asSimple().text(), is("2.0.0-M4"));
        assertThat(cliUpdateMessages.get(1).asMap().map().containsKey("message"), is(true));
        assertThat(cliUpdateMessages.get(1).asMap().map().get("message").isSimple(), is(true));
        assertThat(cliUpdateMessages.get(1).asMap().map().get("message").asSimple().text(), is("Helidon archetype support"));

        assertThat(cliUpdateMessages.get(2).isMap(), is(true));
        assertThat(cliUpdateMessages.get(2).asMap().map().size(), is(2));
        assertThat(cliUpdateMessages.get(2).asMap().map().containsKey("version"), is(true));
        assertThat(cliUpdateMessages.get(2).asMap().map().get("version").isSimple(), is(true));
        assertThat(cliUpdateMessages.get(2).asMap().map().get("version").asSimple().text(), is("2.0.0-RC1"));
        assertThat(cliUpdateMessages.get(2).asMap().map().containsKey("message"), is(true));
        assertThat(cliUpdateMessages.get(2).asMap().map().get("message").isSimple(), is(true));
        assertThat(cliUpdateMessages.get(2).asMap().map().get("message").asSimple().text(), is("Performance improvements"));

        assertThat(tasks.get(9), is(instanceOf(TemplateTask.class)));
        TemplateTask template1 = (TemplateTask) tasks.get(9);
        assertThat(template1.source(), is("redirect.html.mustache"));
        assertThat(template1.target(), is("docs/index.html"));
        assertThat(template1.variables().size(), is(5));
        assertThat(template1.variables().containsKey("location"), is(true));
        assertThat(template1.variables().get("location").isSimple(), is(true));
        assertThat(template1.variables().get("location").asSimple().text(), is("./latest/index.html"));
        assertThat(template1.variables().containsKey("title"), is(true));
        assertThat(template1.variables().get("title").isSimple(), is(true));
        assertThat(template1.variables().get("title").asSimple().text(), is("Helidon Documentation"));
        assertThat(template1.variables().containsKey("description"), is(true));
        assertThat(template1.variables().get("description").isSimple(), is(true));
        assertThat(template1.variables().get("description").asSimple().text(), is("Helidon Documentation"));
        assertThat(template1.variables().containsKey("og-url"), is(true));
        assertThat(template1.variables().get("og-url").isSimple(), is(true));
        assertThat(template1.variables().get("og-url").asSimple().text(), is("https://helidon.io/docs"));
        assertThat(template1.variables().containsKey("og-description"), is(true));
        assertThat(template1.variables().get("og-description").isSimple(), is(true));
        assertThat(template1.variables().get("og-description").asSimple().text(), is("Documentation"));

        assertThat(tasks.get(10), is(instanceOf(TemplateTask.class)));
        TemplateTask template2 = (TemplateTask) tasks.get(10);
        assertThat(template2.source(), is("redirect.html.mustache"));
        assertThat(template2.target(), is("guides/index.html"));
        assertThat(template2.variables().size(), is(5));
        assertThat(template2.variables().containsKey("location"), is(true));
        assertThat(template2.variables().get("location").isSimple(), is(true));
        assertThat(template2.variables().get("location").asSimple().text(), is("../docs/latest/index.html#/guides/01_overview"));
        assertThat(template2.variables().containsKey("title"), is(true));
        assertThat(template2.variables().get("title").isSimple(), is(true));
        assertThat(template2.variables().get("title").asSimple().text(), is("Helidon Guides"));
        assertThat(template2.variables().containsKey("description"), is(true));
        assertThat(template2.variables().get("description").isSimple(), is(true));
        assertThat(template2.variables().get("description").asSimple().text(), is("Helidon Guides"));
        assertThat(template2.variables().containsKey("og-url"), is(true));
        assertThat(template2.variables().get("og-url").isSimple(), is(true));
        assertThat(template2.variables().get("og-url").asSimple().text(), is("https://helidon.io/guides"));
        assertThat(template2.variables().containsKey("og-description"), is(true));
        assertThat(template2.variables().get("og-description").isSimple(), is(true));
        assertThat(template2.variables().get("og-description").asSimple().text(), is("Guides"));

        assertThat(tasks.get(11), is(instanceOf(TemplateTask.class)));
        TemplateTask template3 = (TemplateTask) tasks.get(11);
        assertThat(template3.source(), is("redirect.html.mustache"));
        assertThat(template3.target(), is("javadocs/index.html"));
        assertThat(template3.variables().size(), is(5));
        assertThat(template3.variables().containsKey("location"), is(true));
        assertThat(template3.variables().get("location").isSimple(), is(true));
        assertThat(template3.variables().get("location").asSimple().text(), is("../docs/latest/apidocs/index.html?overview-summary.html"));
        assertThat(template3.variables().containsKey("title"), is(true));
        assertThat(template3.variables().get("title").isSimple(), is(true));
        assertThat(template3.variables().get("title").asSimple().text(), is("Helidon Javadocs"));
        assertThat(template3.variables().containsKey("description"), is(true));
        assertThat(template3.variables().get("description").isSimple(), is(true));
        assertThat(template3.variables().get("description").asSimple().text(), is("Helidon Javadocs"));
        assertThat(template3.variables().containsKey("og-url"), is(true));
        assertThat(template3.variables().get("og-url").isSimple(), is(true));
        assertThat(template3.variables().get("og-url").asSimple().text(), is("https://helidon.io/javadocs"));
        assertThat(template3.variables().containsKey("og-description"), is(true));
        assertThat(template3.variables().get("og-description").isSimple(), is(true));
        assertThat(template3.variables().get("og-description").asSimple().text(), is("Javadocs"));

        assertThat(tasks.get(12), is(instanceOf(FileTask.class)));
        FileTask file1 = (FileTask) tasks.get(12);
        assertThat(file1.source(), is(nullValue()));
        assertThat(file1.target(), is("CNAME"));
        assertThat(file1.content(), is("${cname}"));

        assertThat(tasks.get(13), is(instanceOf(FileTask.class)));
        FileTask file2 = (FileTask) tasks.get(13);
        assertThat(file2.source(), is(nullValue()));
        assertThat(file2.target(), is("cli-data/latest"));
        assertThat(file2.content(), is("${cli.data.latest.version}"));
    }
}