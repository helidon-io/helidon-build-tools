/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.helidon.build.common.xml.XMLElement;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link StagingFactory}.
 */
class StagingFactoryTest {

    @TempDir
    private Path tempDir;

    @Test
    void testCreate() {
        InputStream is = StagingFactoryTest.class.getResourceAsStream("/test-config.xml");
        assertThat(is, is(not(nullValue())));

        XMLElement rawConfig = XMLElement.read(is, "test-config.xml", false);
        XMLElement config = ConfigProcessor.process(rawConfig, tempDir, k -> null);
        StagingTasks root = StagingFactory.create(config);

        assertThat(root, hasProperty("tasks", StagingTasks::tasks, contains(List.of(isElement(StagingDirectory.class,
                hasProperty("target", StagingDirectory::target, is("${project.build.directory}/site")),
                hasProperty("tasks", StagingTask::tasks, contains(List.of(
                        isUnpackArtifacts(),
                        isSymlinks(),
                        isDownloads(),
                        isArchives(),
                        isTemplates(),
                        isFiles(),
                        isUnpacks()
                )))
        )))));
    }

    @Test
    void testEmptyIteratorVariable() {
        StagingTasks root = create("""
                <configuration>
                    <variables>
                        <variable name="version"/>
                    </variables>
                    <directories>
                        <directory target="target/stage">
                            <files>
                                <file target="{version}">
                                    <iterators>
                                        <variables>
                                            <variable ref="version"/>
                                        </variables>
                                    </iterators>
                                </file>
                            </files>
                        </directory>
                    </directories>
                </configuration>
                """);

        FileTask file = firstFile(root);
        assertThat(iterators(file), is(List.of(List.of())));
    }

    @Test
    void testStringIteratorVariable() {
        StagingTasks root = create("""
                <configuration>
                    <variables>
                        <variable name="version" value="1.0.0"/>
                    </variables>
                    <directories>
                        <directory target="target/stage">
                            <files>
                                <file target="{version}">
                                    <iterators>
                                        <variables>
                                            <variable ref="version"/>
                                        </variables>
                                    </iterators>
                                </file>
                            </files>
                        </directory>
                    </directories>
                </configuration>
                """);

        FileTask file = firstFile(root);
        assertThat(iterators(file), is(List.of(List.of(Map.of("version", "1.0.0")))));
    }

    @Test
    void testNamedAndUnnamedIteratorReferences() {
        StagingTasks root = create("""
                <configuration>
                    <variables>
                        <variable name="version" value="1.0.0"/>
                    </variables>
                    <directories>
                        <directory target="target/stage">
                            <files>
                                <file target="{version}-{release}">
                                    <iterators>
                                        <variables>
                                            <variable ref="version"/>
                                            <variable name="release" ref="version"/>
                                        </variables>
                                    </iterators>
                                </file>
                            </files>
                        </directory>
                    </directories>
                </configuration>
                """);

        FileTask file = firstFile(root);
        assertThat(iterators(file), is(List.of(List.of(Map.of("version", "1.0.0", "release", "1.0.0")))));
    }

    @Test
    void testReferenceTakesPrecedenceOverNestedContent() {
        StagingTasks root = create("""
                <configuration>
                    <variables>
                        <variable name="versions"><value>1.0.0</value></variable>
                    </variables>
                    <directories>
                        <directory target="target/stage">
                            <files>
                                <file target="{release}">
                                    <iterators>
                                        <variables>
                                            <variable name="release" ref="versions" value="ignored">
                                                <variable ref="missing"/>
                                            </variable>
                                        </variables>
                                    </iterators>
                                </file>
                            </files>
                        </directory>
                    </directories>
                </configuration>
                """);

        assertThat(iterators(firstFile(root)), is(List.of(List.of(Map.of("release", "1.0.0")))));
    }

    @Test
    void testAggregateListOfMapsInTemplateAndIterator() {
        StagingTasks root = create("""
                <configuration>
                    <variables>
                        <variable name="archetype.v2.versions">
                            <value>
                                <variable name="version" value="2.6.0"/>
                                <variable name="generation" value="2"/>
                            </value>
                        </variable>
                        <variable name="archetype.v3.versions">
                            <value>
                                <variable name="version" value="3.2.0"/>
                                <variable name="generation" value="3"/>
                            </value>
                        </variable>
                        <variable name="archetype.v4.versions">
                            <value>
                                <variable name="version" value="4.1.0"/>
                                <variable name="generation" value="4"/>
                            </value>
                        </variable>
                        <variable name="archetype.versions">
                            <variable ref="archetype.v2.versions"/>
                            <variable ref="archetype.v3.versions"/>
                            <variable ref="archetype.v4.versions"/>
                        </variable>
                    </variables>
                    <directories>
                        <directory target="target/stage">
                            <template source="versions.mustache" target="versions.txt">
                                <iterators>
                                    <variables>
                                        <variable name="release" ref="archetype.versions"/>
                                    </variables>
                                </iterators>
                            </template>
                        </directory>
                    </directories>
                </configuration>
                """);

        StagingDirectory directory = (StagingDirectory) root.tasks().get(0);
        TemplateTask template = (TemplateTask) directory.tasks().get(0);
        List<Map<String, String>> releases = List.of(
                Map.of("version", "2.6.0", "generation", "2"),
                Map.of("version", "3.2.0", "generation", "3"),
                Map.of("version", "4.1.0", "generation", "4"));
        assertThat(iterators(template), is(List.of(releases)));
    }

    @Test
    void testAggregateRejectsNonListSources() {
        IllegalArgumentException scalar = assertThrows(IllegalArgumentException.class, () -> create("""
                <configuration>
                    <variables>
                        <variable name="version" value="1.0.0"/>
                        <variable name="versions">
                            <variable ref="version"/>
                        </variable>
                    </variables>
                </configuration>
                """));
        assertThat(scalar.getMessage(), containsString("requires list-valued reference 'version'"));

        IllegalArgumentException empty = assertThrows(IllegalArgumentException.class, () -> create("""
                <configuration>
                    <variables>
                        <variable name="version"/>
                        <variable name="versions">
                            <variable ref="version"/>
                        </variable>
                    </variables>
                </configuration>
                """));
        assertThat(empty.getMessage(), containsString("requires list-valued reference 'version'"));
    }

    @Test
    void testAggregateRejectsMixedChildren() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> create("""
                <configuration>
                    <variables>
                        <variable name="base"><value>1.0.0</value></variable>
                        <variable name="versions">
                            <value>2.0.0</value>
                            <variable ref="base"/>
                        </variable>
                    </variables>
                </configuration>
                """));

        assertThat(ex.getMessage(), containsString("cannot mix <value> and <variable> children"));
    }

    @Test
    void testAggregateRejectsDirectDefinitions() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> create("""
                <configuration>
                    <variables>
                        <variable name="versions">
                            <variable name="inline"><value>1.0.0</value></variable>
                        </variable>
                    </variables>
                </configuration>
                """));

        assertThat(ex.getMessage(), containsString("requires direct variable children to use ref"));
    }

    @Test
    void testAggregateRejectsForwardReference() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> create("""
                <configuration>
                    <variables>
                        <variable name="versions">
                            <variable ref="later"/>
                        </variable>
                        <variable name="later"><value>1.0.0</value></variable>
                    </variables>
                </configuration>
                """));

        assertThat(ex.getMessage(), containsString("Unresolved variable: later"));
    }

    @Test
    void testCopyParsesFiltersAndIterators() {
        StagingTasks root = create("""
                <configuration>
                    <variables>
                        <variable name="version">
                            <value>1.0.0</value>
                            <value>2.0.0</value>
                        </variable>
                    </variables>
                    <directories>
                        <directory target="target/stage">
                            <copies>
                                <copy source="src/{version}" target="assets/{version}">
                                    <includes>
                                        <include>**/*.txt</include>
                                    </includes>
                                    <exclude>**/draft/**</exclude>
                                    <iterators>
                                        <variables>
                                            <variable ref="version"/>
                                        </variables>
                                    </iterators>
                                </copy>
                            </copies>
                        </directory>
                    </directories>
                </configuration>
                """);

        StagingDirectory directory = (StagingDirectory) root.tasks().get(0);
        StagingTasks copies = (StagingTasks) directory.tasks().get(0);
        CopyTask copy = (CopyTask) copies.tasks().get(0);
        assertThat(copy.source(), is("src/{version}"));
        assertThat(copy.target(), is("assets/{version}"));
        assertThat(copy.includes(), is(List.of("**/*.txt")));
        assertThat(copy.excludes(), is(List.of("**/draft/**")));
        assertThat(iterators(copy), is(List.of(List.of(
                Map.of("version", "1.0.0"),
                Map.of("version", "2.0.0")))));
    }

    static Matcher<StagingElement> isUnpackArtifacts() {
        return isElement(StagingTasks.class, hasProperty("tasks", StagingTasks::tasks, contains(List.of(
                isElement(UnpackArtifactTask.class,
                        hasProperty("gav", it -> it.gav().toString(), is("io.helidon:helidon-docs:{version}:jar")),
                        hasProperty("excludes", UnpackArtifactTask::excludes, is("META-INF/**")),
                        hasProperty("includes", UnpackArtifactTask::includes, is(nullValue())),
                        hasProperty("target", UnpackArtifactTask::target, is("docs/{version}")),
                        hasProperty("mappers", UnpackArtifactTask::mappers, contains(List.of(
                                isElement(Mapper.class,
                                        hasProperty("match", Mapper::match, is("^apidocs/(.*)$")),
                                        hasProperty("replace", Mapper::replace, is("$1")))
                        ))),
                        hasProperty("iterators", StagingFactoryTest::iterators, hasItem(
                                List.of(
                                        Map.of("version", "${docs.1.version}"),
                                        Map.of("version", "1.4.3"),
                                        Map.of("version", "1.4.2"),
                                        Map.of("version", "1.4.1"),
                                        Map.of("version", "1.4.0")
                                )))),
                isElement(UnpackArtifactTask.class,
                        hasProperty("gav", it -> it.gav().toString(), is("io.helidon:helidon-project:{version}:site:jar")),
                        hasProperty("excludes", UnpackArtifactTask::excludes, is("META-INF/**")),
                        hasProperty("includes", UnpackArtifactTask::includes, is(nullValue())),
                        hasProperty("target", UnpackArtifactTask::target, is("docs/{version}")),
                        hasProperty("iterators", StagingFactoryTest::iterators, hasItem(
                                List.of(Map.of("version", "${docs.2.version}"))
                        )))
        ))));
    }

    static Matcher<StagingElement> isSymlinks() {
        return isElement(StagingTasks.class, hasProperty("tasks", StagingTasks::tasks, contains(List.of(
                isElement(SymlinkTask.class,
                        hasProperty("source", SymlinkTask::source, is("./${docs.latest.version}")),
                        hasProperty("target", SymlinkTask::target, is("docs/latest"))),
                isElement(SymlinkTask.class,
                        hasProperty("source", SymlinkTask::source, is("./${docs.1.version}")),
                        hasProperty("target", SymlinkTask::target, is("docs/v1"))),
                isElement(SymlinkTask.class,
                        hasProperty("source", SymlinkTask::source, is("./${docs.2.version}")),
                        hasProperty("target", SymlinkTask::target, is("docs/v2"))),
                isElement(SymlinkTask.class,
                        hasProperty("source", SymlinkTask::source, is("./${cli.latest.version}")),
                        hasProperty("target", SymlinkTask::target, is("cli/latest")))
        ))));
    }

    static Matcher<StagingElement> isDownloads() {
        return isElement(StagingTasks.class, hasProperty("tasks", StagingTasks::tasks, contains(List.of(
                        isElement(DownloadTask.class,
                                hasProperty("url", DownloadTask::url, is("https://helidon.io/cli-data/{version}/cli-data.zip")),
                                hasProperty("target", DownloadTask::target, is("cli-data/{version}/cli-data.zip")),
                                hasProperty("iterators", StagingFactoryTest::iterators, hasItem(
                                        List.of(Map.of("version", "2.0.0-M1"))
                                ))),
                        isElement(DownloadTask.class,
                                hasProperty("url", DownloadTask::url,
                                        is("${build-tools.download.url}/{version}/helidon-cli-{platform}-amd64")),
                                hasProperty("target", DownloadTask::target, is("cli/{version}")),
                                hasProperty("iterators", StagingFactoryTest::iterators, hasItem(
                                        List.of(
                                                Map.of("version", "${cli.latest.version}", "platform", "darwin"),
                                                Map.of("version", "2.0.0-M4", "platform", "linux"),
                                                Map.of("version", "2.0.0-M4", "platform", "darwin"),
                                                Map.of("version", "${cli.latest.version}", "platform", "linux")
                                        ))
                                ))
                )
        )));
    }

    static Matcher<StagingElement> isCopyArtifacts() {
        return isElement(StagingTasks.class, hasProperty("tasks", StagingTasks::tasks, contains(List.of(
                isElement(CopyArtifactTask.class,
                        hasProperty("gav", it -> it.gav().toString(),
                                is("io.helidon.archetypes:helidon-archetype-catalog:${cli.data.latest.version}:xml")),
                        hasProperty("target", CopyArtifactTask::target, is("archetype-catalog.xml"))),
                isElement(CopyArtifactTask.class,
                        hasProperty("gav", it -> it.gav().toString(),
                                is("io.helidon.archetypes:helidon-bare-se:${cli.data.latest.version}:jar"))),
                isElement(CopyArtifactTask.class,
                        hasProperty("gav", it -> it.gav().toString(),
                                is("io.helidon.archetypes:helidon-bare-mp:${cli.data.latest.version}:jar")))
        ))));
    }

    static Matcher<StagingElement> isArchives() {
        return isElement(StagingTasks.class, hasProperty("tasks", StagingTasks::tasks, contains(List.of(
                isElement(ArchiveTask.class,
                        hasProperty("target", ArchiveTask::target, is("cli-data/${cli.data.latest.version}/cli-data.zip")),
                        hasProperty("tasks", ArchiveTask::tasks, contains(List.of(
                                isCopyArtifacts(), isElement(StagingTasks.class,
                                        hasProperty("tasks", StagingTasks::tasks, contains(List.of(
                                        isElement(TemplateTask.class,
                                                hasProperty("source", TemplateTask::source,
                                                        is("src/cli-metadata.properties.mustache")),
                                                hasProperty("target", TemplateTask::target, is("metadata.properties")),
                                                hasProperty("model", StagingFactoryTest::model, is(Map.of(
                                                        "helidonVersion", "${cli.data.latest.version}",
                                                        "buildToolsVersion", "${cli.maven.plugin.version}",
                                                        "cliVersion", "${cli.latest.version}",
                                                        "cliUpdateMessages", Map.of(
                                                                "major", Map.of("version", "2.0.0-M2",
                                                                        "message", "Major dev command enhancements"),
                                                                "archetype", Map.of("version", "2.0.0-M4",
                                                                        "message", "Helidon archetype support"),
                                                                "performance", Map.of("version", "2.0.0-RC1",
                                                                        "message", "Performance improvements"))
                                                )))
                                        )
                                ))))
                        )))
                )
        ))));
    }

    static Matcher<StagingElement> isTemplates() {
        return isElement(StagingTasks.class, hasProperty("tasks", StagingTasks::tasks, contains(List.of(
                isElement(TemplateTask.class,
                        hasProperty("source", TemplateTask::source, is("redirect.html.mustache")),
                        hasProperty("target", TemplateTask::target, is("docs/index.html")),
                        hasProperty("model", StagingFactoryTest::model, is(Map.of(
                                "location", "./latest/index.html",
                                "title", "Helidon Documentation",
                                "description", "Helidon Documentation",
                                "og-url", "https://helidon.io/docs",
                                "og-description", "Documentation"
                        )))),
                isElement(TemplateTask.class,
                        hasProperty("source", TemplateTask::source, is("redirect.html.mustache")),
                        hasProperty("target", TemplateTask::target, is("guides/index.html")),
                        hasProperty("model", StagingFactoryTest::model, is(Map.of(
                                "location", "../docs/latest/index.html#/guides/01_overview",
                                "title", "Helidon Guides",
                                "description", "Helidon Guides",
                                "og-url", "https://helidon.io/guides",
                                "og-description", "Guides"
                        )))),
                isElement(TemplateTask.class,
                        hasProperty("source", TemplateTask::source, is("redirect.html.mustache")),
                        hasProperty("target", TemplateTask::target, is("javadocs/index.html")),
                        hasProperty("model", StagingFactoryTest::model, is(Map.of(
                                "location", "../docs/latest/apidocs/index.html?overview-summary.html",
                                "title", "Helidon Javadocs",
                                "description", "Helidon Javadocs",
                                "og-url", "https://helidon.io/javadocs",
                                "og-description", "Javadocs"
                        ))))
        ))));
    }

    static Matcher<StagingElement> isFiles() {
        return isElement(StagingTasks.class, hasProperty("tasks", StagingTasks::tasks, contains(List.of(
                isElement(FileTask.class,
                        hasProperty("target", FileTask::target, is("CNAME")),
                        hasProperty("content", FileTask::content, is("${cname}"))),
                isElement(FileTask.class,
                        hasProperty("target", FileTask::target, is("cli-data/latest")),
                        hasProperty("content", FileTask::content, is("${cli.data.latest.version}"))),
                isElement(FileTask.class,
                        hasProperty("target", FileTask::target, is("sitemap.txt")),
                        hasProperty("tasks", StagingTask::tasks, contains(List.of(
                                isElement(ListFilesTask.class,
                                        hasProperty("dir", ListFilesTask::dir, is("docs")),
                                        hasProperty("includes", ListFilesTask::includes, is(List.of(
                                                "**/foo/**",
                                                "**/bar/**"
                                        ))),
                                        hasProperty("excludes", ListFilesTask::excludes, is(List.of(
                                                "**/bob/**",
                                                "**/alice/**"
                                        ))),
                                        hasProperty("substitutions", ListFilesTask::substitutions, contains(List.of(
                                                isElement(Substitution.class,
                                                        hasProperty("regex", Substitution::isRegex, is(true)),
                                                        hasProperty("match", Substitution::match,
                                                                is("^(?<path>([^/]+/)*)index.html$")),
                                                        hasProperty("replace", Substitution::replace, is("{path}")))
                                        )))
                                )
                        )))
                )
        ))));
    }

    static Matcher<StagingElement> isUnpacks() {
        return isElement(StagingTasks.class, hasProperty("tasks", StagingTasks::tasks, contains(List.of(
                isElement(UnpackTask.class,
                        hasProperty("url", UnpackTask::url,
                                is("https://repo1.maven.org/maven2/com/example/archive/1.0.0/archive-1.0.0.jar")),
                        hasProperty("includes", UnpackTask::includes, is("apidocs/**")),
                        hasProperty("excludes", UnpackTask::excludes, is(nullValue())),
                        hasProperty("mappers", UnpackTask::mappers, contains(List.of(
                                isElement(Mapper.class,
                                        hasProperty("match", Mapper::match, is("^apidocs/(.*)$")),
                                        hasProperty("replace", Mapper::replace, is("$1")))
                        )))
                )
        ))));
    }

    static Map<String, Object> model(TemplateTask task) {
        return task.model().root();
    }

    static List<List<Map<String, String>>> iterators(StagingTask task) {
        List<List<Map<String, String>>> lists = new ArrayList<>();
        for (ActionIterator it : task.iterators()) {
            List<Map<String, String>> list = new ArrayList<>();
            while (it.hasNext()) {
                list.add(it.next());
            }
            lists.add(list);
        }
        return lists;
    }

    private StagingTasks create(String str) {
        XMLElement rawConfig = XMLElement.read(str, "unknown", false);
        XMLElement config = ConfigProcessor.process(rawConfig, tempDir, k -> null);
        return StagingFactory.create(config);
    }

    private static FileTask firstFile(StagingTasks root) {
        StagingDirectory directory = (StagingDirectory) root.tasks().get(0);
        StagingTasks files = (StagingTasks) directory.tasks().get(0);
        return (FileTask) files.tasks().get(0);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <T extends StagingElement> Matcher<StagingElement> isElement(Class<T> type, Matcher<T>... matchers) {
        var list = new ArrayList<Matcher<? super StagingElement>>();
        list.add(Matchers.instanceOf(type));
        for (var matcher : matchers) {
            list.add((Matcher<StagingElement>) matcher);
        }
        return allOf(list);
    }

    private static <T, U> Matcher<T> hasProperty(String name, Function<T, U> getter, Matcher<U> matcher) {
        return new FeatureMatcher<>(matcher, "has property " + name, name) {
            @Override
            protected U featureValueOf(T actual) {
                return getter.apply(actual);
            }
        };
    }
}
