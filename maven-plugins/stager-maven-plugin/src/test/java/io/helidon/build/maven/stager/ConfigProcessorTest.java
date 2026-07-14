/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.helidon.build.common.Strings;
import io.helidon.build.common.xml.XMLElement;
import io.helidon.build.common.xml.XMLException;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link ConfigProcessor}.
 */
class ConfigProcessorTest {

    @TempDir
    private Path tempDir;

    @Test
    void testInterpolation() {
        Map<String, String> properties = Map.of(
                "stage.path", "target/stage",
                "version", "${major}.0.0",
                "major", "4",
                "file.name", "${version}/index.txt",
                "text", "Helidon ${version}");

        XMLElement config = process("""
                <configuration>
                    <directories>
                        <directory target="${stage.path}/${version}/${missing}">
                            <files>
                                <file target="${file.name}">${text}</file>
                                <file target="{iteratorVar}"/>
                            </files>
                        </directory>
                    </directories>
                </configuration>
                """, properties);

        assertThat(config.childrenAt("directory"), contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("directory")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/stage/4.0.0/${missing}")))
        ))));

        assertThat(config.childrenAt("directory", "files", "file"), contains(List.of(
                allOf(
                        hasProperty("name", XMLElement::name, is("file")),
                        hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "4.0.0/index.txt"))),
                        hasProperty("value", XMLElement::value, is("Helidon 4.0.0"))
                ),
                allOf(
                        hasProperty("name", XMLElement::name, is("file")),
                        hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "{iteratorVar}")))
                )
        )));
    }

    @Test
    void testMavenStylePropertiesInterpolateAttributesAndText() {
        XMLElement config = process("""
                <configuration>
                    <properties>
                        <stage.path>target/stage</stage.path>
                        <major>4</major>
                        <version>${major}.0.0</version>
                        <message>Helidon ${version}</message>
                    </properties>
                    <directories>
                        <directory target="${stage.path}/${version}">
                            <files>
                                <file target="${version}/index.txt">${message}</file>
                            </files>
                        </directory>
                    </directories>
                </configuration>
                """, Map.of());

        assertThat(config.childrenAt("directory"), contains(List.of(
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/stage/4.0.0")))
        )));
        assertThat(config.childrenAt("directory", "files", "file"), contains(List.of(allOf(
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "4.0.0/index.txt"))),
                hasProperty("value", XMLElement::value, is("Helidon 4.0.0"))
        ))));
    }

    @Test
    void testMixedPropertyFormsUseEffectiveOrder() {
        XMLElement config = process("""
                <configuration>
                    <properties>
                        <property name="stage.path" value="target/old"/>
                        <version>4.0.0</version>
                        <stage.path>target/new</stage.path>
                        <property name="message" value="Version ${version}"/>
                    </properties>
                    <directories>
                        <directory target="${stage.path}">
                            <files>
                                <file target="${version}/info.txt">${message}</file>
                            </files>
                        </directory>
                    </directories>
                </configuration>
                """, Map.of());

        assertThat(config.childrenAt("directory"), contains(List.of(
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/new")))
        )));
        assertThat(config.childrenAt("directory", "files", "file"), contains(List.of(allOf(
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "4.0.0/info.txt"))),
                hasProperty("value", XMLElement::value, is("Version 4.0.0"))
        ))));
    }

    @Test
    void testDuplicateMavenStylePropertiesUseLaterDefinition() {
        XMLElement config = process("""
                <configuration>
                    <properties>
                        <stage.path>target/one</stage.path>
                        <stage.path>target/two</stage.path>
                    </properties>
                    <directories>
                        <directory target="${stage.path}"/>
                    </directories>
                </configuration>
                """, Map.of());

        assertThat(config.childrenAt("directory"), contains(List.of(
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/two")))
        )));
    }

    @Test
    void testFallbackResolverUsedWhenNoStagerPropertyExists() {
        XMLElement config = process("""
                <configuration>
                    <properties>
                        <stage.path>target/stage</stage.path>
                    </properties>
                    <directories>
                        <directory target="${stage.path}/${version}"/>
                    </directories>
                </configuration>
                """, Map.of("version", "4.0.0"));

        assertThat(config.childrenAt("directory"), contains(List.of(
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/stage/4.0.0")))
        )));
    }

    @Test
    void testMalformedLegacyPropertyMissingNameFailsAtRuntime() {
        XMLException ex = assertThrows(XMLException.class, () -> process("""
                        <configuration>
                            <properties>
                                <property value="ignored"/>
                            </properties>
                            <directories>
                                <directory target="${version}"/>
                            </directories>
                        </configuration>
                        """, Map.of("version", "4.0.0")));

        assertThat(ex.getMessage(), containsString("Missing required attribute 'name'"));
    }

    @Test
    void testDirectDirectoryConfig() {
        XMLElement config = process("""
                <configuration>
                    <directories>
                        <directory target="target/stage">
                            <files>
                                <file target="index.txt">content</file>
                            </files>
                        </directory>
                    </directories>
                </configuration>
                """, Map.of());

        assertThat(config.childrenAt("directory"), contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("directory")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/stage")))
        ))));

        assertThat(config.childrenAt("directory", "files", "file"), contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("file")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "index.txt"))),
                hasProperty("value", XMLElement::value, is("content"))
        ))));
    }

    @Test
    void testRootIncludeFullStagerFile() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/stager.xml"/>
                </stager>
                """);
        write("fragments/stager.xml", """
                <stager>
                    <properties>
                        <property name="stage.path" value="target/stage"/>
                    </properties>
                    <directories>
                        <directory target="${stage.path}"/>
                    </directories>
                </stager>
                """);

        XMLElement config = process(main, Map.of());

        List<XMLElement> directories = config.childrenAt("directory");
        assertThat(directories, contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("directory")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/stage")))
        ))));
    }

    @Test
    void testCallerPropertyAfterIncludeOverridesIncludedProperty() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/stager.xml"/>
                    <properties>
                        <property name="stage.path" value="target/caller"/>
                    </properties>
                    <directories>
                        <directory target="${stage.path}/main"/>
                    </directories>
                </stager>
                """);
        write("fragments/stager.xml", """
                <stager>
                    <properties>
                        <property name="stage.path" value="target/included"/>
                    </properties>
                    <directories>
                        <directory target="${stage.path}/included"/>
                    </directories>
                </stager>
                """);

        XMLElement config = process(main, Map.of());

        assertThat(config.childrenAt("directory"), contains(List.of(
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/caller/included"))),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/caller/main")))
        )));
    }

    @Test
    void testLaterIncludeOverridesEarlierIncludeDefaults() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/one.xml"/>
                    <include src="fragments/two.xml"/>
                    <directories>
                        <directory target="${stage.path}/{channel}/main"/>
                    </directories>
                </stager>
                """);
        write("fragments/one.xml", """
                <stager>
                    <properties>
                        <property name="stage.path" value="target/one"/>
                    </properties>
                    <variables>
                        <variable name="channel" value="one"/>
                    </variables>
                    <directories>
                        <directory target="${stage.path}/{channel}/one"/>
                    </directories>
                </stager>
                """);
        write("fragments/two.xml", """
                <stager>
                    <properties>
                        <property name="stage.path" value="target/two"/>
                    </properties>
                    <variables>
                        <variable name="channel" value="two"/>
                    </variables>
                    <directories>
                        <directory target="${stage.path}/{channel}/two"/>
                    </directories>
                </stager>
                """);

        XMLElement config = process(main, Map.of());

        assertThat(config.childrenAt("variables", "variable"), contains(List.of(
                hasProperty("attributes", XMLElement::attributes, is(Map.of("name", "channel", "value", "two")))
        )));
        assertThat(config.childrenAt("directory"), contains(List.of(
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/two/{channel}/one"))),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/two/{channel}/two"))),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/two/{channel}/main")))
        )));
    }

    @Test
    void testRootIncludeRelativePath() throws Exception {
        Path main = write("config/stager.xml", """
                <stager>
                    <include src="fragments/stager.xml"/>
                </stager>
                """);
        write("config/fragments/stager.xml", """
                <stager>
                    <directories>
                        <directory target="target/stage"/>
                    </directories>
                </stager>
                """);

        XMLElement config = process(main, Map.of());

        List<XMLElement> directories = config.childrenAt("directory");
        assertThat(directories, contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("directory")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/stage")))
        ))));
    }

    @Test
    void testValidRootOrder() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/stager.xml"/>
                    <properties>
                        <property name="stage.path" value="target/stage"/>
                    </properties>
                    <variables>
                        <variable name="channel" value="stable"/>
                    </variables>
                    <directories>
                        <directory target="${stage.path}/{channel}/main"/>
                    </directories>
                </stager>
                """);
        write("fragments/stager.xml", """
                <stager>
                    <directories>
                        <directory target="${stage.path}/{channel}/included"/>
                    </directories>
                </stager>
                """);

        XMLElement config = process(main, Map.of());

        List<XMLElement> directories = config.childrenAt("directory");
        assertThat(directories, contains(List.of(
                allOf(
                        hasProperty("name", XMLElement::name, is("directory")),
                        hasProperty("attributes", XMLElement::attributes,
                                is(Map.of("target", "target/stage/{channel}/included")))
                ),
                allOf(
                        hasProperty("name", XMLElement::name, is("directory")),
                        hasProperty("attributes", XMLElement::attributes,
                                is(Map.of("target", "target/stage/{channel}/main")))
                )
        )));
    }

    @Test
    void testMergeVariables() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/stager.xml"/>
                    <variables>
                        <variable name="root" value="caller"/>
                    </variables>
                    <directories>
                        <variables>
                            <variable name="directories" value="caller"/>
                        </variables>
                        <directory target="target/main"/>
                    </directories>
                </stager>
                """);
        write("fragments/stager.xml", """
                <stager>
                    <variables>
                        <variable name="included-root" value="fallback"/>
                    </variables>
                    <directories>
                        <variables>
                            <variable name="included-directories" value="fallback"/>
                        </variables>
                        <directory target="target/included"/>
                    </directories>
                </stager>
                """);

        XMLElement config = process(main, Map.of());

        assertThat(config.children("variables"), hasSize(1));
        assertThat(config.childrenAt("variables", "variable"), contains(List.of(
                hasProperty("attributes", XMLElement::attributes, is(Map.of("name", "included-root", "value", "fallback"))),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("name", "root", "value", "caller"))),
                hasProperty("attributes", XMLElement::attributes,
                        is(Map.of("name", "included-directories", "value", "fallback"))),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("name", "directories", "value", "caller")))
        )));
        assertThat(config.childrenAt("directory"), contains(List.of(
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/included"))),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/main")))
        )));
    }

    @Test
    void testOmitEmptyVariableBlocks() {
        XMLElement config = process("""
                <configuration>
                    <variables/>
                    <directories>
                        <variables/>
                        <directory target="target/stage"/>
                    </directories>
                </configuration>
                """, Map.of());

        assertThat(config.children("variables"), hasSize(0));
        assertThat(config.childrenAt("directory"), hasSize(1));
    }

    @Test
    void testCallerVariableWins() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/fallback.xml"/>
                    <variables>
                        <variable name="version" value="caller"/>
                    </variables>
                    <directories>
                        <directory target="target/stage">
                            <template source="template.mustache" target="index.txt">
                                <variables>
                                    <variable ref="version"/>
                                </variables>
                            </template>
                        </directory>
                    </directories>
                </stager>
                """);
        write("fragments/fallback.xml", """
                <stager>
                    <variables>
                        <variable name="version" value="fallback"/>
                    </variables>
                </stager>
                """);

        XMLElement config = process(main, Map.of());
        assertThat(config.childrenAt("variables", "variable"), contains(List.of(
                hasProperty("attributes", XMLElement::attributes, is(Map.of(
                        "name", "version",
                        "value", "caller")))
        )));
    }

    @Test
    void testIncludedFallback() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/fallback.xml"/>
                    <directories>
                        <directory target="target/stage">
                            <template source="template.mustache" target="index.txt">
                                <variables>
                                    <variable ref="version"/>
                                </variables>
                            </template>
                        </directory>
                    </directories>
                </stager>
                """);
        write("fragments/fallback.xml", """
                <stager>
                    <variables>
                        <variable name="version" value="fallback"/>
                    </variables>
                </stager>
                """);

        XMLElement config = process(main, Map.of());
        assertThat(config.childrenAt("variables", "variable"), contains(List.of(
                hasProperty("attributes", XMLElement::attributes, is(Map.of(
                        "name", "version",
                        "value", "fallback")))
        )));
    }

    @Test
    void testIncludedEmptyFallback() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/fallback.xml"/>
                    <directories>
                        <directory target="target/stage"/>
                    </directories>
                </stager>
                """);
        write("fragments/fallback.xml", """
                <stager>
                    <variables>
                        <variable name="version"/>
                    </variables>
                </stager>
                """);

        XMLElement config = process(main, Map.of());
        assertThat(config.childrenAt("variables", "variable"), contains(List.of(
                hasProperty("attributes", XMLElement::attributes, is(Map.of("name", "version")))
        )));
    }

    @Test
    void testValueOverridesEmptyFallback() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/fallback.xml"/>
                    <variables>
                        <variable name="version" value="caller"/>
                    </variables>
                    <directories>
                        <directory target="target/stage"/>
                    </directories>
                </stager>
                """);
        write("fragments/fallback.xml", """
                <stager>
                    <variables>
                        <variable name="version"/>
                    </variables>
                </stager>
                """);

        XMLElement config = process(main, Map.of());
        assertThat(config.childrenAt("variables", "variable"), contains(List.of(
                hasProperty("attributes", XMLElement::attributes, is(Map.of(
                        "name", "version",
                        "value", "caller")))
        )));
    }

    @Test
    void testEmptyOverridesValueFallback() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/fallback.xml"/>
                    <variables>
                        <variable name="version"/>
                    </variables>
                    <directories>
                        <directory target="target/stage"/>
                    </directories>
                </stager>
                """);
        write("fragments/fallback.xml", """
                <stager>
                    <variables>
                        <variable name="version" value="fallback"/>
                    </variables>
                </stager>
                """);

        XMLElement config = process(main, Map.of());
        assertThat(config.childrenAt("variables", "variable"), contains(List.of(
                hasProperty("attributes", XMLElement::attributes, is(Map.of("name", "version")))
        )));
    }

    @Test
    void testUnknownRootChildrenIgnored() throws Exception {
        write("fragments/stager.xml", """
                <stager/>
                """);

        XMLElement config = process("""
                <configuration>
                    <unknown/>
                    <include src="fragments/stager.xml"/>
                    <properties>
                        <property name="stage.path" value="target/stage"/>
                    </properties>
                    <variables>
                        <variable name="channel" value="stable"/>
                    </variables>
                    <directories>
                        <directory target="${stage.path}/{channel}"/>
                    </directories>
                </configuration>
                """, Map.of());

        assertThat(config.childrenAt("directory"), contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("directory")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/stage/{channel}")))
        ))));
    }

    @Test
    void testConfigIncludeStagerFile() throws Exception {
        write("fragments/stager.xml", """
                <stager>
                    <properties>
                        <property name="stage.path" value="target/stage"/>
                    </properties>
                    <directories>
                        <directory target="${stage.path}">
                            <files>
                                <file target="index.txt">${message}</file>
                            </files>
                        </directory>
                    </directories>
                </stager>
                """);
        XMLElement config = process("""
                <configuration>
                    <include src="fragments/stager.xml"/>
                    <properties>
                        <property name="message" value="content"/>
                    </properties>
                </configuration>
                """, Map.of());

        List<XMLElement> files = config.childrenAt("directory", "files", "file");
        assertThat(files, contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("file")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "index.txt"))),
                hasProperty("value", XMLElement::value, is("content"))
        ))));
    }

    @Test
    void testIncludeSourceDoesNotUseProperties() throws Exception {
        write("fragments/stager.xml", """
                <stager/>
                """);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> process("""
                        <configuration>
                            <include src="${fragments.dir}/stager.xml"/>
                            <properties>
                                <property name="fragments.dir" value="fragments"/>
                            </properties>
                        </configuration>
                        """, Map.of("fragments.dir", "fragments")));

        assertThat(Strings.normalizePath(ex.getMessage()),
                containsString("Missing or unreadable include '${fragments.dir}/stager.xml'"));
    }

    @Test
    void testIncludeSourceWithExpressionCharactersIsLiteral() throws Exception {
        write("${fragments.dir}/stager.xml", """
                <stager>
                    <directories>
                        <directory target="target/literal"/>
                    </directories>
                </stager>
                """);

        XMLElement config = process("""
                <configuration>
                    <include src="${fragments.dir}/stager.xml"/>
                </configuration>
                """, Map.of("fragments.dir", "fragments"));

        assertThat(config.childrenAt("directory"), contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("directory")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/literal")))
        ))));
    }

    @Test
    void testRootIncludeWrapperFile() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/wrapper.xml"/>
                </stager>
                """);
        write("fragments/wrapper.xml", """
                <fragment>
                    <properties>
                        <property name="stage.path" value="target/stage"/>
                    </properties>
                    <directories>
                        <directory target="${stage.path}"/>
                    </directories>
                </fragment>
                """);

        XMLElement config = process(main, Map.of());

        List<XMLElement> directories = config.childrenAt("directory");
        assertThat(directories, contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("directory")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/stage")))
        ))));
    }

    @Test
    void testRootIncludeWithoutSourceFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> process("""
                        <configuration>
                            <include/>
                        </configuration>
                        """, Map.of()));

        assertThat(ex.getMessage(), containsString("Missing required 'src' attribute for include"));
    }

    @Test
    void testTaskIncludeWithSrc() {
        XMLElement config = process("""
                <configuration>
                    <properties>
                        <property name="task.fragment" value="task-fragment.xml"/>
                    </properties>
                    <directories>
                        <directory target="target/stage">
                            <include src="${task.fragment}"/>
                        </directory>
                    </directories>
                </configuration>
                """, Map.of());

        List<XMLElement> includes = config.childrenAt("directory", "include");
        assertThat(includes, contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("include")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("src", "task-fragment.xml")))
        ))));
    }

    @Test
    void testIncludedTaskIncludeWithSrc() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/wrapper.xml"/>
                </stager>
                """);
        write("fragments/wrapper.xml", """
                <fragment>
                    <directories>
                        <directory target="target/stage">
                            <include src="task-fragment.xml"/>
                            <files>
                                <file target="sitemap.txt">
                                    <list-files dir="docs">
                                        <include src="filter.txt">**/*.html</include>
                                    </list-files>
                                </file>
                            </files>
                        </directory>
                    </directories>
                </fragment>
                """);

        XMLElement config = process(main, Map.of());

        assertThat(config.childrenAt("directory", "include"), contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("include")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("src", "task-fragment.xml")))
        ))));
        assertThat(config.childrenAt("directory", "files", "file", "list-files", "include"), contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("include")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("src", "filter.txt"))),
                hasProperty("value", XMLElement::value, is("**/*.html"))
        ))));
    }

    @Test
    void testListFilesIncludeRemainsTextFilter() {
        XMLElement config = process("""
                <configuration>
                    <directories>
                        <directory target="target/stage">
                            <files>
                                <file target="sitemap.txt">
                                    <list-files dir="docs">
                                        <include>**/*.html</include>
                                    </list-files>
                                </file>
                            </files>
                        </directory>
                    </directories>
                </configuration>
                """, Map.of());

        List<XMLElement> includes = config.childrenAt("directory", "files", "file", "list-files", "include");
        assertThat(includes, contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("include")),
                hasProperty("value", XMLElement::value, is("**/*.html"))
        ))));
    }

    @Test
    void testListFilesIncludeWithSrc() {
        XMLElement config = process("""
                <configuration>
                    <properties>
                        <property name="filter.file" value="filter.txt"/>
                        <property name="filter.pattern" value="**/*.html"/>
                    </properties>
                    <directories>
                        <directory target="target/stage">
                            <files>
                                <file target="sitemap.txt">
                                    <list-files dir="docs">
                                        <include src="${filter.file}">${filter.pattern}</include>
                                    </list-files>
                                </file>
                            </files>
                        </directory>
                    </directories>
                </configuration>
                """, Map.of());

        List<XMLElement> includes = config.childrenAt("directory", "files", "file", "list-files", "include");
        assertThat(includes, contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("include")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("src", "filter.txt"))),
                hasProperty("value", XMLElement::value, is("**/*.html"))
        ))));
    }

    @Test
    void testListFilesWrappedIncludeRemainsTextFilter() {
        XMLElement config = process("""
                <configuration>
                    <directories>
                        <directory target="target/stage">
                            <files>
                                <file target="sitemap.txt">
                                    <list-files dir="docs">
                                        <includes>
                                            <include>**/*.html</include>
                                        </includes>
                                    </list-files>
                                </file>
                            </files>
                        </directory>
                    </directories>
                </configuration>
                """, Map.of());

        List<XMLElement> includes = config.childrenAt("directory", "files", "file", "list-files", "includes", "include");
        assertThat(includes, contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("include")),
                hasProperty("value", XMLElement::value, is("**/*.html"))
        ))));
    }

    @Test
    void testNestedWrapperIncludePath() throws Exception {
        Path main = write("root.xml", """
                <stager>
                    <include src="fragments/level1/wrapper.xml"/>
                </stager>
                """);
        write("fragments/level1/wrapper.xml", """
                <fragment>
                    <include src="level2/wrapper.xml"/>
                </fragment>
                """);
        write("fragments/level1/level2/wrapper.xml", """
                <anything>
                    <properties>
                        <property name="message" value="content"/>
                    </properties>
                    <directories>
                        <directory target="target/stage">
                            <files>
                                <file target="index.txt">${message}</file>
                            </files>
                        </directory>
                    </directories>
                </anything>
                """);

        XMLElement config = process(main, Map.of());

        List<XMLElement> files = config.childrenAt("directory", "files", "file");
        assertThat(files, contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("file")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "index.txt"))),
                hasProperty("value", XMLElement::value, is("content"))
        ))));
    }

    @Test
    void testNestedStagerIncludePath() throws Exception {
        Path main = write("root.xml", """
                <stager>
                    <include src="fragments/level1/stager.xml"/>
                </stager>
                """);
        write("fragments/level1/stager.xml", """
                <stager>
                    <include src="level2/stager.xml"/>
                </stager>
                """);
        write("fragments/level1/level2/stager.xml", """
                <stager>
                    <properties>
                        <property name="message" value="content"/>
                    </properties>
                    <directories>
                        <directory target="target/stage">
                            <files>
                                <file target="index.txt">${message}</file>
                            </files>
                        </directory>
                    </directories>
                </stager>
                """);

        XMLElement config = process(main, Map.of());

        List<XMLElement> files = config.childrenAt("directory", "files", "file");
        assertThat(files, contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("file")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "index.txt"))),
                hasProperty("value", XMLElement::value, is("content"))
        ))));
    }

    @Test
    void testAbsoluteIncludePath() throws Exception {
        Path fragment = write("fragments/stager.xml", """
                <stager>
                    <directories>
                        <directory target="target/absolute"/>
                    </directories>
                </stager>
                """);
        Path main = write("stager.xml", """
                <stager>
                    <include src="%s"/>
                </stager>
                """.formatted(fragment));

        XMLElement config = process(main, Map.of());

        assertThat(config.childrenAt("directory"), contains(List.of(allOf(
                hasProperty("name", XMLElement::name, is("directory")),
                hasProperty("attributes", XMLElement::attributes, is(Map.of("target", "target/absolute")))
        ))));
    }

    @Test
    void testIncludeCycleFails() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="a.xml"/>
                </stager>
                """);
        write("a.xml", """
                <stager>
                    <include src="b.xml"/>
                </stager>
                """);
        write("b.xml", """
                <stager>
                    <include src="a.xml"/>
                </stager>
                """);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> process(main, Map.of()));

        assertThat(ex.getMessage(), is("Include cycle detected: stager.xml:2:14 -> a.xml:2:14 -> b.xml:2:14"));
    }

    @Test
    void testMissingIncludeFails() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="missing.xml"/>
                </stager>
                """);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> process(main, Map.of()));

        assertThat(ex.getMessage(), is("Missing or unreadable include 'missing.xml' referenced from stager.xml:2:14"));
    }

    @Test
    void testConfigMissingIncludeFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> process("""
                        <configuration>
                            <include src="missing.xml"/>
                        </configuration>
                        """, Map.of()));

        assertThat(ex.getMessage(), is("Missing or unreadable include 'missing.xml' referenced from unknown:2:14"));
    }

    @Test
    void testConfigIncludeCycleFails() throws Exception {
        write("a.xml", """
                <stager>
                    <include src="b.xml"/>
                </stager>
                """);
        write("b.xml", """
                <stager>
                    <include src="a.xml"/>
                </stager>
                """);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> process("""
                        <configuration>
                            <include src="a.xml"/>
                        </configuration>
                        """, Map.of()));

        assertThat(ex.getMessage(), is("Include cycle detected: unknown:2:14 -> a.xml:2:14 -> b.xml:2:14"));
    }

    @Test
    void testDirectoriesBeforeIncludeFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> process("""
                        <configuration>
                            <directories/>
                            <include src="fragments/stager.xml"/>
                        </configuration>
                        """, Map.of()));

        assertThat(ex.getMessage(), containsString("<include> must appear before <directories>"));
    }

    @Test
    void testDocumentOrderUsesTreeOrder() throws Exception {
        write("fragments/stager.xml", """
                <stager/>
                """);

        XMLElement rawConfig = XMLElement.builder()
                .name("configuration")
                .child(builder -> builder
                        .name("directories")
                        .location(new XMLElement.Location("pom.xml", 3, 17)))
                .child(builder -> builder
                        .name("include")
                        .attributes(Map.of("src", "fragments/stager.xml"))
                        .location(new XMLElement.Location("pom.xml", 2, 17)))
                .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ConfigProcessor.process(rawConfig, tempDir, Function.identity()));

        assertThat(ex.getMessage(), is("Invalid element order: <include> must appear before <directories> in pom.xml:2:17"));
    }

    @Test
    void testPropertiesBeforeIncludeFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> process("""
                        <configuration>
                            <properties/>
                            <include src="fragments/stager.xml"/>
                        </configuration>
                        """, Map.of()));

        assertThat(ex.getMessage(), containsString("<include> must appear before <properties>"));
    }

    @Test
    void testVariablesBeforeIncludeFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> process("""
                        <configuration>
                            <variables/>
                            <include src="fragments/stager.xml"/>
                        </configuration>
                        """, Map.of()));

        assertThat(ex.getMessage(), containsString("<include> must appear before <variables>"));
    }

    @Test
    void testPropertiesAfterDirectoriesFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> process("""
                        <configuration>
                            <directories/>
                            <properties/>
                        </configuration>
                        """, Map.of()));

        assertThat(ex.getMessage(), containsString("<properties> must appear before <directories>"));
    }

    @Test
    void testWrapperInvalidOrderFails() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/wrapper.xml"/>
                </stager>
                """);
        write("fragments/wrapper.xml", """
                <fragment>
                    <directories/>
                    <variables/>
                </fragment>
                """);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> process(main, Map.of()));

        assertThat(ex.getMessage(), containsString("<variables> must appear before <directories>"));
    }

    @Test
    void testStagerInvalidOrderFails() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/stager.xml"/>
                </stager>
                """);
        write("fragments/stager.xml", """
                <stager>
                    <directories/>
                    <variables/>
                </stager>
                """);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> process(main, Map.of()));

        assertThat(ex.getMessage(), containsString("<variables> must appear before <directories>"));
    }

    Path write(String path, String xml) throws IOException {
        Path file = tempDir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, xml, StandardCharsets.UTF_8);
        return file;
    }

    XMLElement process(Path file, Map<String, String> properties) {
        XMLElement rawConfig = XMLElement.read(file, file.getFileName().toString(), false);
        return ConfigProcessor.process(rawConfig, file.getParent(), properties::get);
    }

    XMLElement process(String str, Map<String, String> properties) {
        XMLElement rawConfig = XMLElement.read(str, "unknown", false);
        return ConfigProcessor.process(rawConfig, tempDir, properties::get);
    }

    static <T, U> Matcher<T> hasProperty(String name, Function<T, U> extractor, Matcher<U> subMatcher) {
        return new FeatureMatcher<>(subMatcher, "has property " + name, name) {
            @Override
            protected U featureValueOf(T target) {
                return extractor.apply(target);
            }
        };
    }
}
