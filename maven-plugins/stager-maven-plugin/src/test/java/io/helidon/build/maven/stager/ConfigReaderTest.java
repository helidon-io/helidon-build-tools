/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.maven.stager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.github.mustachejava.util.DecoratedCollection;
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
 * Tests {@link ConfigReader}.
 */
class ConfigReaderTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testConverter() throws Exception {
        InputStream is = ConfigReaderTest.class.getResourceAsStream("/test-config.xml");
        assertThat(is, is(not(nullValue())));
        Reader reader = new InputStreamReader(is);
        PlexusConfiguration plexusConfig = new XmlPlexusConfiguration(Xpp3DomBuilder.build(reader));
        ConfigReader configReader = new ConfigReader(new StagingElementFactory());
        StagingAction action = configReader.read(new PlexusConfigNode(plexusConfig, null));
        assertThat(action, is(not(nullValue())));
        assertThat(action, is(instanceOf(StagingActions.class)));
        StagingActions<StagingAction> container = (StagingActions<StagingAction>) action;
        assertThat(container.actions().size(), is(1));
        StagingDirectory directory = (StagingDirectory) container.actions().get(0);
        assertThat(directory.target(), is("${project.build.directory}/site"));
        List<StagingAction> directoryStagingActionss = directory.actions();
        assertThat(directoryStagingActionss.size(), is(6));

        directoryStagingActionss.forEach(c -> assertThat(c, is(instanceOf(StagingActions.class))));

        List<UnpackArtifactTask> unpackArtifacts = ((StagingActions<UnpackArtifactTask>) directoryStagingActionss.get(0)).actions();
        assertThat(unpackArtifacts.size(), is(2));

        UnpackArtifactTask unpack1 = unpackArtifacts.get(0);
        assertThat(unpack1.gav().groupId(), is("io.helidon"));
        assertThat(unpack1.gav().artifactId(), is("helidon-docs"));
        assertThat(unpack1.gav().version(), is("{version}"));
        assertThat(unpack1.excludes(), is("META-INF/**"));
        assertThat(unpack1.includes(), is(nullValue()));
        assertThat(unpack1.target(), is("docs/{version}"));
        assertThat(unpack1.iterators().size(), is(1));
        assertThat(unpack1.iterators().get(0).next().get("version"), is("${docs.1.version}"));
        assertThat(unpack1.iterators().get(0).next().get("version"), is("1.4.3"));
        assertThat(unpack1.iterators().get(0).next().get("version"), is("1.4.2"));
        assertThat(unpack1.iterators().get(0).next().get("version"), is("1.4.1"));
        assertThat(unpack1.iterators().get(0).next().get("version"), is("1.4.0"));
        assertThat(unpack1.iterators().get(0).hasNext(), is(false));

        UnpackArtifactTask unpack2 = unpackArtifacts.get(1);
        assertThat(unpack2.gav().groupId(), is("io.helidon"));
        assertThat(unpack2.gav().artifactId(), is("helidon-project"));
        assertThat(unpack2.gav().version(), is("{version}"));
        assertThat(unpack2.gav().classifier(), is("site"));
        assertThat(unpack2.excludes(), is("META-INF/**"));
        assertThat(unpack2.includes(), is(nullValue()));
        assertThat(unpack2.target(), is("docs/{version}"));
        assertThat(unpack2.iterators().size(), is(1));
        assertThat(unpack2.iterators().get(0).next().get("version"), is("${docs.2.version}"));
        assertThat(unpack2.iterators().get(0).hasNext(), is(false));

        List<SymlinkTask> symlinks = ((StagingActions<SymlinkTask>) directoryStagingActionss.get(1)).actions();
        assertThat(symlinks.size(), is(4));

        SymlinkTask symlink1 = symlinks.get(0);
        assertThat(symlink1.source(), is("./${docs.latest.version}"));
        assertThat(symlink1.target(), is("docs/latest"));

        SymlinkTask symlink2 = symlinks.get(1);
        assertThat(symlink2.source(), is("./${docs.1.version}"));
        assertThat(symlink2.target(), is("docs/v1"));

        SymlinkTask symlink3 = symlinks.get(2);
        assertThat(symlink3.source(), is("./${docs.2.version}"));
        assertThat(symlink3.target(), is("docs/v2"));

        SymlinkTask symlink4 = symlinks.get(3);
        assertThat(symlink4.source(), is("./${cli.latest.version}"));
        assertThat(symlink4.target(), is("cli/latest"));

        List<DownloadTask> downloads = ((StagingActions<DownloadTask>) directoryStagingActionss.get(2)).actions();
        assertThat(downloads.size(), is(2));

        DownloadTask download1 = downloads.get(0);
        assertThat(download1.url(), is("https://helidon.io/cli-data/{version}/cli-data.zip"));
        assertThat(download1.target(), is("cli-data/{version}/cli-data.zip"));
        assertThat(download1.iterators().size(), is(1));
        assertThat(download1.iterators().get(0).next().get("version"), is("2.0.0-M1"));
        assertThat(download1.iterators().get(0).hasNext(), is(false));

        DownloadTask download2 = downloads.get(1);
        assertThat(download2.url(), is("${build-tools.download.url}/{version}/helidon-cli-{platform}-amd64"));
        assertThat(download2.target(), is("cli/{version}"));
        assertThat(download2.iterators().size(), is(1));
        Map<String, String> download2It1 = download2.iterators().get(0).next();
        assertThat(download2It1.get("platform"), is("darwin"));
        assertThat(download2It1.get("version"), is("${cli.latest.version}"));
        assertThat(download2.iterators().get(0).hasNext(), is(true));
        Map<String, String> download2It2 = download2.iterators().get(0).next();
        assertThat(download2It2.get("platform"), is("linux"));
        assertThat(download2It2.get("version"), is("2.0.0-M4"));
        assertThat(download2.iterators().get(0).hasNext(), is(true));
        Map<String, String> download2It3 = download2.iterators().get(0).next();
        assertThat(download2It3.get("platform"), is("darwin"));
        assertThat(download2It3.get("version"), is("2.0.0-M4"));
        assertThat(download2.iterators().get(0).hasNext(), is(true));
        Map<String, String> download2It4 = download2.iterators().get(0).next();
        assertThat(download2It4.get("platform"), is("linux"));
        assertThat(download2It4.get("version"), is("${cli.latest.version}"));
        assertThat(download2.iterators().get(0).hasNext(), is(false));

        List<ArchiveTask> archives = ((StagingActions<ArchiveTask>) directoryStagingActionss.get(3)).actions();
        assertThat(archives.size(), is(1));

        ArchiveTask archive = archives.get(0);
        List<StagingAction> tasks = archive.tasks();
        assertThat(tasks.size(), is(2));
        tasks.forEach(c -> assertThat(c, is(instanceOf(StagingActions.class))));

        List<CopyArtifactTask> copyArtifacts = ((StagingActions<CopyArtifactTask>) tasks.get(0)).actions();
        assertThat(copyArtifacts.size(), is(3));

        CopyArtifactTask archiveCopyArtifact1 = copyArtifacts.get(0);
        assertThat(archiveCopyArtifact1.gav().groupId(), is("io.helidon.archetypes"));
        assertThat(archiveCopyArtifact1.gav().artifactId(), is("helidon-archetype-catalog"));
        assertThat(archiveCopyArtifact1.gav().version(), is("${cli.data.latest.version}"));
        assertThat(archiveCopyArtifact1.gav().type(), is("xml"));
        assertThat(archiveCopyArtifact1.target(), is("archetype-catalog.xml"));

        CopyArtifactTask archiveCopyArtifact2 = copyArtifacts.get(1);
        assertThat(archiveCopyArtifact2.gav().groupId(), is("io.helidon.archetypes"));
        assertThat(archiveCopyArtifact2.gav().artifactId(), is("helidon-bare-se"));
        assertThat(archiveCopyArtifact2.gav().version(), is("${cli.data.latest.version}"));

        CopyArtifactTask archiveCopyArtifact3 = copyArtifacts.get(2);
        assertThat(archiveCopyArtifact3.gav().groupId(), is("io.helidon.archetypes"));
        assertThat(archiveCopyArtifact3.gav().artifactId(), is("helidon-bare-mp"));
        assertThat(archiveCopyArtifact3.gav().version(), is("${cli.data.latest.version}"));

        List<TemplateTask> templates = ((StagingActions<TemplateTask>) tasks.get(1)).actions();
        assertThat(templates.size(), is(1));

        TemplateTask archiveTemplate1 = templates.get(0);
        assertThat(archiveTemplate1.source(), is("src/cli-metadata.properties.mustache"));
        assertThat(archiveTemplate1.target(), is("metadata.properties"));
        assertThat(archiveTemplate1.templateVariables().size(), is(4));
        assertThat(archiveTemplate1.templateVariables().containsKey("helidonVersion"), is(true));
        assertThat(archiveTemplate1.templateVariables().get("helidonVersion"), is(instanceOf(String.class)));
        assertThat(archiveTemplate1.templateVariables().get("helidonVersion"), is("${cli.data.latest.version}"));
        assertThat(archiveTemplate1.templateVariables().containsKey("buildToolsVersion"), is(true));
        assertThat(archiveTemplate1.templateVariables().get("buildToolsVersion"), is(instanceOf(String.class)));
        assertThat(archiveTemplate1.templateVariables().get("buildToolsVersion"), is("${cli.maven.plugin.version}"));
        assertThat(archiveTemplate1.templateVariables().containsKey("cliVersion"), is(true));
        assertThat(archiveTemplate1.templateVariables().get("cliVersion"), is(instanceOf(String.class)));
        assertThat(archiveTemplate1.templateVariables().get("cliVersion"), is("${cli.latest.version}"));
        assertThat(archiveTemplate1.templateVariables().containsKey("cliUpdateMessages"), is(true));
        assertThat(archiveTemplate1.templateVariables().get("cliUpdateMessages"), is(instanceOf(Collection.class)));

        DecoratedCollection<Object> cliUpdateMessages = (DecoratedCollection<Object>) archiveTemplate1.templateVariables()
                                                                                                      .get("cliUpdateMessages");

        int index = 0;
        for (Object cliUpdateMessage : cliUpdateMessages) {
            Field valueField = cliUpdateMessage.getClass().getDeclaredField("value");
            valueField.setAccessible(true);
            Object item = valueField.get(cliUpdateMessage);
            assertThat(item, is(instanceOf(Map.class)));
            assertThat(((Map<String, Object>) item).size(), is(2));
            assertThat(((Map<String, Object>) item).containsKey("version"), is(true));
            assertThat(((Map<String, Object>) item).get("version"), is(instanceOf(String.class)));
            assertThat(((Map<String, Object>) item).containsKey("message"), is(true));
            assertThat(((Map<String, Object>) item).get("message"), is(instanceOf(String.class)));
            switch (index) {
                case 0:
                    assertThat(((Map<String, Object>) item).get("version"), is("2.0.0-M2"));
                    assertThat(((Map<String, Object>) item).get("message"), is("Major dev command enhancements"));
                    break;
                case 1:
                    assertThat(((Map<String, Object>) item).get("version"), is("2.0.0-M4"));
                    assertThat(((Map<String, Object>) item).get("message"), is("Helidon archetype support"));
                    break;
                case 2:
                    assertThat(((Map<String, Object>) item).get("version"), is("2.0.0-RC1"));
                    assertThat(((Map<String, Object>) item).get("message"), is("Performance improvements"));
                    break;
                default:
                    throw new AssertionError("Invalid index: " + index);
            }
            index++;
        }
        assertThat(index, is(3));

        List<TemplateTask> templates1 = ((StagingActions<TemplateTask>) directoryStagingActionss.get(4)).actions();
        assertThat(templates1.size(), is(3));

        TemplateTask template1 = templates1.get(0);
        assertThat(template1.source(), is("redirect.html.mustache"));
        assertThat(template1.target(), is("docs/index.html"));
        assertThat(template1.templateVariables().size(), is(5));
        assertThat(template1.templateVariables().containsKey("location"), is(true));
        assertThat(template1.templateVariables().get("location"), is(instanceOf(String.class)));
        assertThat(template1.templateVariables().get("location"), is("./latest/index.html"));
        assertThat(template1.templateVariables().containsKey("title"), is(true));
        assertThat(template1.templateVariables().get("title"), is(instanceOf(String.class)));
        assertThat(template1.templateVariables().get("title"), is("Helidon Documentation"));
        assertThat(template1.templateVariables().containsKey("description"), is(true));
        assertThat(template1.templateVariables().get("description"), is(instanceOf(String.class)));
        assertThat(template1.templateVariables().get("description"), is("Helidon Documentation"));
        assertThat(template1.templateVariables().containsKey("og-url"), is(true));
        assertThat(template1.templateVariables().get("og-url"), is(instanceOf(String.class)));
        assertThat(template1.templateVariables().get("og-url"), is("https://helidon.io/docs"));
        assertThat(template1.templateVariables().containsKey("og-description"), is(true));
        assertThat(template1.templateVariables().get("og-description"), is(instanceOf(String.class)));
        assertThat(template1.templateVariables().get("og-description"), is("Documentation"));

        TemplateTask template2 = templates1.get(1);
        assertThat(template2.source(), is("redirect.html.mustache"));
        assertThat(template2.target(), is("guides/index.html"));
        assertThat(template2.templateVariables().size(), is(5));
        assertThat(template2.templateVariables().containsKey("location"), is(true));
        assertThat(template2.templateVariables().get("location"), is(instanceOf(String.class)));
        assertThat(template2.templateVariables().get("location"), is("../docs/latest/index.html#/guides/01_overview"));
        assertThat(template2.templateVariables().containsKey("title"), is(true));
        assertThat(template2.templateVariables().get("title"), is(instanceOf(String.class)));
        assertThat(template2.templateVariables().get("title"), is("Helidon Guides"));
        assertThat(template2.templateVariables().containsKey("description"), is(true));
        assertThat(template2.templateVariables().get("description"), is(instanceOf(String.class)));
        assertThat(template2.templateVariables().get("description"), is("Helidon Guides"));
        assertThat(template2.templateVariables().containsKey("og-url"), is(true));
        assertThat(template2.templateVariables().get("og-url"), is(instanceOf(String.class)));
        assertThat(template2.templateVariables().get("og-url"), is("https://helidon.io/guides"));
        assertThat(template2.templateVariables().containsKey("og-description"), is(true));
        assertThat(template2.templateVariables().get("og-description"), is(instanceOf(String.class)));
        assertThat(template2.templateVariables().get("og-description"), is("Guides"));

        TemplateTask template3 = templates1.get(2);
        assertThat(template3.source(), is("redirect.html.mustache"));
        assertThat(template3.target(), is("javadocs/index.html"));
        assertThat(template3.templateVariables().size(), is(5));
        assertThat(template3.templateVariables().containsKey("location"), is(true));
        assertThat(template3.templateVariables().get("location"), is(instanceOf(String.class)));
        assertThat(template3.templateVariables().get("location"), is("../docs/latest/apidocs/index.html?overview-summary.html"));
        assertThat(template3.templateVariables().containsKey("title"), is(true));
        assertThat(template3.templateVariables().get("title"), is(instanceOf(String.class)));
        assertThat(template3.templateVariables().get("title"), is("Helidon Javadocs"));
        assertThat(template3.templateVariables().containsKey("description"), is(true));
        assertThat(template3.templateVariables().get("description"), is(instanceOf(String.class)));
        assertThat(template3.templateVariables().get("description"), is("Helidon Javadocs"));
        assertThat(template3.templateVariables().containsKey("og-url"), is(true));
        assertThat(template3.templateVariables().get("og-url"), is(instanceOf(String.class)));
        assertThat(template3.templateVariables().get("og-url"), is("https://helidon.io/javadocs"));
        assertThat(template3.templateVariables().containsKey("og-description"), is(true));
        assertThat(template3.templateVariables().get("og-description"), is(instanceOf(String.class)));
        assertThat(template3.templateVariables().get("og-description"), is("Javadocs"));

        List<FileTask> files = ((StagingActions<FileTask>) directoryStagingActionss.get(5)).actions();
        assertThat(files.size(), is(2));

        FileTask file1 = files.get(0);
        assertThat(file1.source(), is(nullValue()));
        assertThat(file1.target(), is("CNAME"));
        assertThat(file1.content(), is("${cname}"));

        FileTask file2 = files.get(1);
        assertThat(file2.source(), is(nullValue()));
        assertThat(file2.target(), is("cli-data/latest"));
        assertThat(file2.content(), is("${cli.data.latest.version}"));
    }
}
