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
import java.util.Map;
import java.util.function.Function;

import io.helidon.build.common.Strings;
import io.helidon.build.common.xml.XMLElement;
import io.helidon.build.common.xml.XMLException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
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

        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage/4.0.0/${missing}">
                        <files>
                            <file target="4.0.0/index.txt">Helidon 4.0.0</file>
                            <file target="{iteratorVar}"/>
                        </files>
                    </directory>
                </directories>
                """));
    }

    @Test
    void testSelfInterpolation() {
        XMLElement config = process("""
                <configuration>
                    <properties>
                        <version>4.0.0</version>
                    </properties>
                    <directories>
                        <directory target="target/stage/${version}">
                            <files>
                                <file target="${version}.txt">Helidon ${version}</file>
                            </files>
                        </directory>
                    </directories>
                </configuration>
                """, Map.of());

        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage/4.0.0">
                        <files>
                            <file target="4.0.0.txt">Helidon 4.0.0</file>
                        </files>
                    </directory>
                </directories>
                """));
    }

    @Test
    void testPropertiesOrder() {
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

        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/new">
                        <files>
                            <file target="4.0.0/info.txt">Version 4.0.0</file>
                        </files>
                    </directory>
                </directories>
                """));
    }

    @Test
    void testDuplicateProperties() {
        XMLElement config = process("""
                <configuration>
                    <properties>
                        <stage.path>target/one</stage.path>
                        <stage.path>target/two</stage.path>
                    </properties>
                    <directories>
                        <directory target="${stage.path}">
                            <template />
                        </directory>
                    </directories>
                </configuration>
                """, Map.of("stage.path", "target/external"));

        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/two">
                        <template>
                            <model>
                                <properties>
                                    <stage.path>target/two</stage.path>
                                </properties>
                            </model>
                        </template>
                    </directory>
                </directories>
                """));
    }

    @Test
    void testInjectedModelProperties() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/defaults.xml"/>
                    <properties>
                        <prop1>${prop2}</prop1>
                        <unresolved>${missing}</unresolved>
                    </properties>
                    <directories>
                        <directory>
                            <template>
                                <iterators/>
                            </template>
                        </directory>
                    </directories>
                </stager>
                """);
        write("fragments/defaults.xml", """
                <stager>
                    <properties>
                        <prop2>${prop3}</prop2>
                    </properties>
                </stager>
                """);

        XMLElement config = process(main, Map.of("prop3", "value1"));

        assertThat(config, hasToString("""
                <directories>
                    <directory>
                        <template>
                            <model>
                                <properties>
                                    <prop2>value1</prop2>
                                    <prop1>value1</prop1>
                                    <unresolved>${missing}</unresolved>
                                </properties>
                            </model>
                            <iterators/>
                        </template>
                    </directory>
                </directories>
                """));
    }

    @Test
    void testExplicitModelProperties() {
        XMLElement config = process("""
                <configuration>
                    <properties>
                        <prop1>value1</prop1>
                    </properties>
                    <directories>
                        <directory>
                            <template>
                                <model>
                                    <properties>
                                        <prop1>not-value1</prop1>
                                    </properties>
                                </model>
                            </template>
                        </directory>
                    </directories>
                </configuration>
                """, Map.of());

        assertThat(config, hasToString("""
                <directories>
                    <directory>
                        <template>
                            <model>
                                <properties>
                                    <prop1>not-value1</prop1>
                                </properties>
                            </model>
                        </template>
                    </directory>
                </directories>
                """));
    }

    @Test
    void testInjectedModelPropertiesInAllPaths() {
        XMLElement config = process("""
                <configuration>
                    <properties>
                        <prop1>value1</prop1>
                    </properties>
                    <directories>
                        <directory>
                            <template/>
                            <templates>
                                <template/>
                            </templates>
                            <directory>
                                <template/>
                            </directory>
                            <directories>
                                <directory>
                                    <templates>
                                        <template/>
                                    </templates>
                                </directory>
                            </directories>
                            <archive>
                                <template/>
                            </archive>
                            <archives>
                                <archive>
                                    <templates>
                                        <template/>
                                    </templates>
                                </archive>
                            </archives>
                            <directories>
                                <directory>
                                    <archives>
                                        <archive>
                                            <directory>
                                                <archives>
                                                    <archive>
                                                        <directories>
                                                            <directory>
                                                                <templates>
                                                                    <template/>
                                                                </templates>
                                                            </directory>
                                                        </directories>
                                                    </archive>
                                                </archives>
                                            </directory>
                                        </archive>
                                    </archives>
                                </directory>
                            </directories>
                        </directory>
                        <directory>
                            <templates>
                                <template/>
                            </templates>
                        </directory>
                    </directories>
                </configuration>
                """, Map.of());

        assertThat(config, hasToString("""
                <directories>
                    <directory>
                        <template>
                            <model>
                                <properties>
                                    <prop1>value1</prop1>
                                </properties>
                            </model>
                        </template>
                        <templates>
                            <template>
                                <model>
                                    <properties>
                                        <prop1>value1</prop1>
                                    </properties>
                                </model>
                            </template>
                        </templates>
                        <directory>
                            <template>
                                <model>
                                    <properties>
                                        <prop1>value1</prop1>
                                    </properties>
                                </model>
                            </template>
                        </directory>
                        <directories>
                            <directory>
                                <templates>
                                    <template>
                                        <model>
                                            <properties>
                                                <prop1>value1</prop1>
                                            </properties>
                                        </model>
                                    </template>
                                </templates>
                            </directory>
                        </directories>
                        <archive>
                            <template>
                                <model>
                                    <properties>
                                        <prop1>value1</prop1>
                                    </properties>
                                </model>
                            </template>
                        </archive>
                        <archives>
                            <archive>
                                <templates>
                                    <template>
                                        <model>
                                            <properties>
                                                <prop1>value1</prop1>
                                            </properties>
                                        </model>
                                    </template>
                                </templates>
                            </archive>
                        </archives>
                        <directories>
                            <directory>
                                <archives>
                                    <archive>
                                        <directory>
                                            <archives>
                                                <archive>
                                                    <directories>
                                                        <directory>
                                                            <templates>
                                                                <template>
                                                                    <model>
                                                                        <properties>
                                                                            <prop1>value1</prop1>
                                                                        </properties>
                                                                    </model>
                                                                </template>
                                                            </templates>
                                                        </directory>
                                                    </directories>
                                                </archive>
                                            </archives>
                                        </directory>
                                    </archive>
                                </archives>
                            </directory>
                        </directories>
                    </directory>
                    <directory>
                        <templates>
                            <template>
                                <model>
                                    <properties>
                                        <prop1>value1</prop1>
                                    </properties>
                                </model>
                            </template>
                        </templates>
                    </directory>
                </directories>
                """));
    }

    @Test
    void testModelTemplateNotProcessed() {
        XMLElement config = process("""
                <configuration>
                    <properties>
                        <version>4.2.0</version>
                    </properties>
                    <directories>
                        <directory>
                            <template>
                                <model>
                                    <template>
                                        <value>model data</value>
                                    </template>
                                </model>
                            </template>
                        </directory>
                    </directories>
                </configuration>
                """, Map.of());

        assertThat(config, hasToString("""
                <directories>
                    <directory>
                        <template>
                            <model>
                                <properties>
                                    <version>4.2.0</version>
                                </properties>
                                <template>
                                    <value>model data</value>
                                </template>
                            </model>
                        </template>
                    </directory>
                </directories>
                """));
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

        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage/4.0.0"/>
                </directories>
                """));
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

        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage">
                        <files>
                            <file target="index.txt">content</file>
                        </files>
                    </directory>
                </directories>
                """));
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
        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage"/>
                </directories>
                """));
    }

    @Test
    void testCallerPropertyOverride() throws Exception {
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
        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/caller/included"/>
                    <directory target="target/caller/main"/>
                </directories>
                """));
    }

    @Test
    void testLaterIncludePropertyOverride() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/one.xml"/>
                    <include src="fragments/two.xml"/>
                    <directories>
                        <directory target="${stage.path}/main"/>
                    </directories>
                </stager>
                """);
        write("fragments/one.xml", """
                <stager>
                    <properties>
                        <property name="stage.path" value="target/one"/>
                    </properties>
                    <directories>
                        <directory target="${stage.path}/one"/>
                    </directories>
                </stager>
                """);
        write("fragments/two.xml", """
                <stager>
                    <properties>
                        <property name="stage.path" value="target/stage"/>
                    </properties>
                    <directories>
                        <directory target="${stage.path}/two"/>
                    </directories>
                </stager>
                """);

        XMLElement config = process(main, Map.of());
        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage/one"/>
                    <directory target="target/stage/two"/>
                    <directory target="target/stage/main"/>
                </directories>
                """));
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
        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage"/>
                </directories>
                """));
    }

    @Test
    void testValidRootOrder() throws Exception {
        Path main = write("stager.xml", """
                <stager>
                    <include src="fragments/stager.xml"/>
                    <properties>
                        <property name="stage.path" value="target/stage"/>
                    </properties>
                    <directories>
                        <directory target="${stage.path}/main"/>
                    </directories>
                </stager>
                """);
        write("fragments/stager.xml", """
                <stager>
                    <directories>
                        <directory target="${stage.path}/included"/>
                    </directories>
                </stager>
                """);

        XMLElement config = process(main, Map.of());
        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage/included"/>
                    <directory target="target/stage/main"/>
                </directories>
                """));
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
                    <directories>
                        <directory target="${stage.path}"/>
                    </directories>
                </configuration>
                """, Map.of());

        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage"/>
                </directories>
                """));
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

        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage">
                        <files>
                            <file target="index.txt">content</file>
                        </files>
                    </directory>
                </directories>
                """));
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

        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/literal"/>
                </directories>
                """));
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
        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage"/>
                </directories>
                """));
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

        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage">
                        <include src="task-fragment.xml"/>
                    </directory>
                </directories>
                """));
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
        assertThat(config, hasToString("""
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
                """));
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

        assertThat(config, hasToString("""
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
                """));
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

        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage">
                        <files>
                            <file target="sitemap.txt">
                                <list-files dir="docs">
                                    <include src="filter.txt">**/*.html</include>
                                </list-files>
                            </file>
                        </files>
                    </directory>
                </directories>
                """));
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

        assertThat(config, hasToString("""
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
                """));
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
        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage">
                        <files>
                            <file target="index.txt">content</file>
                        </files>
                    </directory>
                </directories>
                """));
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
        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/stage">
                        <files>
                            <file target="index.txt">content</file>
                        </files>
                    </directory>
                </directories>
                """));
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
        assertThat(config, hasToString("""
                <directories>
                    <directory target="target/absolute"/>
                </directories>
                """));
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
                    <properties/>
                </fragment>
                """);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> process(main, Map.of()));

        assertThat(ex.getMessage(), containsString("<properties> must appear before <directories>"));
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
                    <properties/>
                </stager>
                """);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> process(main, Map.of()));

        assertThat(ex.getMessage(), containsString("<properties> must appear before <directories>"));
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
}
