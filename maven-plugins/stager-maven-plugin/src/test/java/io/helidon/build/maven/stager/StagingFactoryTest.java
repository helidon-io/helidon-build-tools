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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.helidon.build.common.xml.XMLElement;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;

/**
 * Tests {@link StagingFactory}.
 */
class StagingFactoryTest {

    @TempDir
    private Path tempDir;

    @Test
    void testWrappers() {
        StagingTasks root = StagingFactory.createTasks(XMLElement.read("""
                <directories>
                    <directory target="${project.build.directory}/site">
                        <unpack-artifacts>
                            <unpack-artifact
                                    groupId="unpack-groupId"
                                    artifactId="unpack-artifactId"
                                    version="unpack-version"
                                    target="unpack-target"/>
                        </unpack-artifacts>
                        <copy-artifacts>
                            <copy-artifact
                                    groupId="copy-groupId"
                                    artifactId="copy-artifactId"
                                    version="copy-version"
                                    target="copy-target"/>
                        </copy-artifacts>
                        <symlinks>
                            <symlink source="symlink-source" target="symlink-target"/>
                        </symlinks>
                        <downloads>
                            <download url="download-url" target="download-target"/>
                        </downloads>
                        <archives>
                            <archive target="archive-target"/>
                        </archives>
                        <templates>
                            <template source="template-source" target="template-target"/>
                        </templates>
                        <files>
                            <file target="file-target">file-text</file>
                        </files>
                        <unpacks>
                            <unpack url="unpack-url" target="unpack-target" ext="unpack-ext"/>
                        </unpacks>
                    </directory>
                </directories>
                """));

        assertThat(root.tasks(), contains(
                isTask(StagingDirectory.class,
                        hasProperty("tasks", StagingTask::tasks, contains(
                                isTasks("unpack-artifacts", isTask(UnpackArtifactTask.class)),
                                isTasks("copy-artifacts", isTask(CopyArtifactTask.class)),
                                isTasks("symlinks", isTask(SymlinkTask.class)),
                                isTasks("downloads", isTask(DownloadTask.class)),
                                isTasks("archives", isTask(ArchiveTask.class)),
                                isTasks("templates", isTask(TemplateTask.class)),
                                isTasks("files", isTask(FileTask.class)),
                                isTasks("unpacks", isTask(UnpackTask.class))
                        ))
                )
        ));
    }

    @Test
    void testCopyParsesFilters() {
        StagingTasks root = StagingFactory.createTasks(XMLElement.read("""
                <directories>
                    <directory target="target/stage">
                        <copies>
                            <copy source="src/{version}" target="assets/{version}">
                                <includes>
                                    <include>**/*.txt</include>
                                </includes>
                                <exclude>**/draft/**</exclude>
                            </copy>
                        </copies>
                    </directory>
                </directories>
                """));

        assertThat(root.tasks(), contains(
                hasProperty("tasks", StagingTask::tasks, contains(
                        isTasks("copies", isTask(CopyTask.class,
                                hasProperty("includes", CopyTask::includes, is(List.of("**/*.txt"))),
                                hasProperty("excludes", CopyTask::excludes, is(List.of("**/draft/**")))
                        ))
                ))
        ));
    }

    @Test
    void testTaskOwnedElementsPreserveDirectAndWrappedOrder() {
        StagingTasks root = StagingFactory.createTasks(XMLElement.read("""
                <directories>
                    <directory target="target/stage">
                        <copy source="src" target="copy"/>
                        <unpack url="https://example.com/archive.zip" target="unpack"/>
                        <file target="files.txt">
                            <list-files />
                        </file>
                        <template source="template.hbs" target="template.txt"/>
                    </directory>
                </directories>
                """));

        assertThat(root.tasks(), contains(
                hasProperty("tasks", StagingTask::tasks, contains(
                        isTask(CopyTask.class),
                        isTask(UnpackTask.class),
                        isTask(FileTask.class, hasProperty("tasks", FileTask::tasks, contains(
                                isTask(ListFilesTask.class)
                        ))),
                        isTask(TemplateTask.class)
                ))
        ));
    }

    @Test
    void testHierarchy() {
        StagingTasks root = StagingFactory.createTasks(XMLElement.read("""
                <directories>
                    <directory target="target/stage">
                        <file target="1st">first</file>
                        <files join="true">
                            <file target="2nd"><list-files dir="docs2"/></file>
                            <file target="3rd"><list-files dir="docs3"/></file>
                        </files>
                        <file target="last">last</file>
                    </directory>
                </directories>
                """));

        assertThat(root.tasks(), contains(
                hasProperty("tasks", StagingTask::tasks, contains(
                        isTask(FileTask.class,
                                hasProperty("target", FileTask::target, is("1st"))),
                        allOf(
                                hasProperty("join", Joinable::join, is(true)),
                                isTasks("files",
                                        isTask(FileTask.class,
                                                hasProperty("target", FileTask::target, is("2nd")),
                                                hasProperty("tasks", FileTask::tasks, contains(
                                                        isTask(ListFilesTask.class,
                                                                hasProperty("dir", ListFilesTask::dir, is("docs2")))
                                                ))
                                        ),
                                        isTask(FileTask.class,
                                                hasProperty("target", FileTask::target, is("3rd")),
                                                hasProperty("tasks", FileTask::tasks, contains(
                                                        isTask(ListFilesTask.class,
                                                                hasProperty("dir", ListFilesTask::dir, is("docs3")))
                                                ))
                                        )
                                )
                        ),
                        isTask(FileTask.class,
                                hasProperty("target", FileTask::target, is("last"))
                        )
                ))
        ));
    }

    @Test
    void testTemplateModelAreNotTasks() {
        StagingTasks root = StagingFactory.createTasks(XMLElement.read("""
                <directories>
                    <directory target="target/stage">
                        <template source="template.hbs" target="template.txt">
                            <model>
                                <file target="not-a-task">value</file>
                                <downloads>
                                    <download url="not-a-url" target="not-a-target"/>
                                </downloads>
                                <variables>
                                    <variable name="not-a-variable">
                                        <value>model data</value>
                                    </variable>
                                </variables>
                            </model>
                        </template>
                    </directory>
                </directories>
                """));

        assertThat(root.tasks(), contains(
                hasProperty("tasks", StagingTask::tasks, contains(
                        isTask(TemplateTask.class,
                                hasProperty("tasks", TemplateTask::tasks, is(List.of()))
                        )
                ))
        ));
    }

    @Test
    void testCopyArtifactDefaultTarget() {
        StagingTasks root = StagingFactory.createTasks(XMLElement.read("""
                <directories>
                    <directory target="target/stage">
                        <copy-artifact groupId="io.helidon" artifactId="helidon" version="4.2.0"/>
                    </directory>
                </directories>
                """));

        assertThat(root.tasks(), contains(
                hasProperty("tasks", StagingTask::tasks, contains(
                        isTask(CopyArtifactTask.class,
                                hasProperty("target", CopyArtifactTask::target, is(
                                        "{artifactId}-{version}.{type}"
                                ))
                        )
                ))
        ));
    }

    @SafeVarargs
    static <T> Matcher<Iterable<? extends T>> contains(Matcher<T>... matchers) {
        return Matchers.contains(List.of(matchers));
    }

    @SafeVarargs
    static Matcher<StagingTask> isTasks(String name, Matcher<StagingTask>... matchers) {
        return isTask(StagingTasks.class,
                hasProperty("name", StagingTask::name, is(name)),
                hasProperty("tasks", StagingTask::tasks, contains(matchers)));
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <T extends StagingTask> Matcher<StagingTask> isTask(Class<T> type, Matcher<T>... matchers) {
        var list = new ArrayList<Matcher<? super StagingTask>>();
        list.add(Matchers.instanceOf(type));
        for (var matcher : matchers) {
            list.add((Matcher<StagingTask>) matcher);
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
