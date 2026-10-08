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

import io.helidon.build.common.xml.XMLElement;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link StagingFactory}.
 */
class StagingFactoryTest {

    @Test
    void testWrappers() {
        StagingTasks root = StagingFactory.createTasks(XMLElement.read("""
                <directories>
                    <directory target="target/stage">
                        <unpack-artifacts>
                            <unpack-artifact groupId="com" artifactId="acme" version="1.0" target="unpacked"/>
                        </unpack-artifacts>
                        <copy-artifacts>
                            <copy-artifact groupId="com" artifactId="acme" version="1.0"/>
                        </copy-artifacts>
                        <copies>
                            <copy source="src" target="copied"/>
                        </copies>
                        <symlinks>
                            <symlink source="1.0" target="latest"/>
                        </symlinks>
                        <downloads>
                            <download url="https://example.com/help.txt" target="help.txt"/>
                        </downloads>
                        <archives>
                            <archive target="archive.zip"/>
                        </archives>
                        <templates>
                            <template source="template.hbs" target="README.md"/>
                        </templates>
                        <files>
                            <file target="files.txt"><list-files/></file>
                        </files>
                        <unpacks>
                            <unpack url="https://example.com/archive.zip" target="unpacked"/>
                        </unpacks>
                    </directory>
                </directories>
                """));

        assertThat(hierarchy(root), is("""
                StagingTasks[directories]
                  StagingDirectory
                    StagingTasks[unpack-artifacts]
                      UnpackArtifactTask
                    StagingTasks[copy-artifacts]
                      CopyArtifactTask
                    StagingTasks[copies]
                      CopyTask
                    StagingTasks[symlinks]
                      SymlinkTask
                    StagingTasks[downloads]
                      DownloadTask
                    StagingTasks[archives]
                      ArchiveTask
                    StagingTasks[templates]
                      TemplateTask
                    StagingTasks[files]
                      FileTask
                        ListFilesTask
                    StagingTasks[unpacks]
                      UnpackTask
                """));
    }

    @Test
    void testPreserveOrder() {
        StagingTasks root = StagingFactory.createTasks(XMLElement.read("""
                <directories>
                    <directory target="target/stage">
                        <file target="first.txt">first</file>
                        <files>
                            <file target="second.txt"><list-files dir="docs"/></file>
                            <file target="third.txt">third</file>
                        </files>
                        <copy source="src" target="copied"/>
                        <copies>
                            <copy source="other" target="other-copy"/>
                        </copies>
                        <file target="last.txt">last</file>
                    </directory>
                </directories>
                """));

        assertThat(hierarchy(root), is("""
                StagingTasks[directories]
                  StagingDirectory
                    FileTask
                    StagingTasks[files]
                      FileTask
                        ListFilesTask
                      FileTask
                    CopyTask
                    StagingTasks[copies]
                      CopyTask
                    FileTask
                """));
    }

    @Test
    void testTemplateModelNotFactoryTasks() {
        StagingTasks root = StagingFactory.createTasks(XMLElement.read("""
                <directories>
                    <directory target="target/stage">
                        <template source="test.hbs" target="file.txt">
                            <model>
                                <files>
                                    <file target="not-a-task">value</file>
                                    <copy source="not-a-source" target="not-a-target"/>
                                </files>
                            </model>
                        </template>
                    </directory>
                </directories>
                """));

        assertThat(hierarchy(root), is("""
                StagingTasks[directories]
                  StagingDirectory
                    TemplateTask
                """));
    }

    private static String hierarchy(StagingTask root) {
        var result = new StringBuilder();
        appendHierarchy(result, root, 0);
        return result.toString();
    }

    private static void appendHierarchy(StringBuilder result, StagingTask task, int depth) {
        result.append("  ".repeat(depth)).append(task.getClass().getSimpleName());
        if (task instanceof StagingTasks) {
            result.append('[').append(task.name()).append(']');
        }
        result.append('\n');
        for (var child : task.tasks()) {
            appendHierarchy(result, child, depth + 1);
        }
    }
}
