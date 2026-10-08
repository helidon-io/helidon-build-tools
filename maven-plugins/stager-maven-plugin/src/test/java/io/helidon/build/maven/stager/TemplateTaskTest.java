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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import io.helidon.build.common.CurrentThreadExecutorService;
import io.helidon.build.common.xml.XMLElement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link TemplateTask}.
 */
class TemplateTaskTest {

    @TempDir
    private Path tempDir;

    @Test
    void testScalarNestedAndLiteralDottedValues() throws IOException {
        Files.writeString(tempDir.resolve("template.hbs"), "{{title}}|{{git.url}}|{{properties.cli.version}}");
        execute(new TemplateTask(XMLElement.read("""
                <template source="template.hbs" target="output.txt">
                    <model>
                        <title>Helidon</title>
                        <git><url>https://github.com/helidon-io/helidon</url></git>
                        <properties><cli.version>4.2.0</cli.version></properties>
                    </model>
                </template>
                """)), Map.of());

        String output = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output, is("Helidon|https://github.com/helidon-io/helidon|4.2.0"));
    }

    @Test
    void testExactDottedNamesPrecedeTraversal() throws IOException {
        Files.writeString(tempDir.resolve("template.hbs"), "{{cli.version}}|{{nested.cli.version}}");
        execute(new TemplateTask(XMLElement.read("""
                <template source="template.hbs" target="output.txt">
                    <model>
                        <cli.version>literal</cli.version>
                        <cli><version>traversed</version></cli>
                        <nested>
                            <cli.version>nested-literal</cli.version>
                            <cli><version>nested-traversed</version></cli>
                        </nested>
                    </model>
                </template>
                """)), Map.of());

        String output = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output, is("literal|nested-literal"));
    }

    @Test
    void testContainerAndNamedSelectionIterationOrder() throws IOException {
        Files.writeString(tempDir.resolve("template.hbs"), """
                {{#container}}
                {{index}}:{{first}}:{{last}}:{{name}}={{value}}
                {{/container}}
                {{#container.a}}
                {{index}}:{{first}}:{{last}}:{{name}}={{value}}
                {{/container.a}}
                """);
        execute(new TemplateTask(XMLElement.read("""
                <template source="template.hbs" target="output.txt">
                    <model>
                        <container>
                            <a>one</a>
                            <b>two</b>
                            <a>three</a>
                        </container>
                    </model>
                </template>
                """)), Map.of());

        String output = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output, is("""
                0:true:false:a=one
                1:false:false:b=two
                2:false:true:a=three
                0:true:false:a=one
                1:false:true:a=three
                """));
    }

    @Test
    void testMetadataCollisionFallback() throws IOException {
        Files.writeString(tempDir.resolve("template.hbs"), """
                {{#container.item}}
                {{name}}|{{_name}}|{{__name}}
                {{value}}|{{_value.name}}
                {{index}}|{{_index}}|{{__index}}|{{___index}}
                {{first}}|{{_first}}|{{last}}
                {{/container.item}}
                """);
        execute(new TemplateTask(XMLElement.read("""
                <template source="template.hbs" target="output.txt">
                    <model>
                        <container>
                            <item>
                                <name>xml-name</name>
                                <_name>xml-underscore-name</_name>
                                <value>xml-value</value>
                                <index>xml-index</index>
                                <_index>xml-underscore-index</_index>
                                <__index>xml-double-underscore-index</__index>
                                <first>xml-first</first>
                            </item>
                        </container>
                    </model>
                </template>
                """)), Map.of());

        String output = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output, is("""
                xml-name|xml-underscore-name|item
                xml-value|xml-name
                xml-index|xml-underscore-index|xml-double-underscore-index|0
                xml-first|true|true
                """));
    }

    @Test
    void testLocalThenContainingThenOuterLookupAtOneCallSite() throws IOException {
        Files.writeString(tempDir.resolve("template.hbs"), """
                {{#container.entry}}
                {{kind}}={{title}}:{{rootOnly}}
                {{/container.entry}}
                """);
        execute(new TemplateTask(XMLElement.read("""
                <template source="template.hbs" target="output.txt">
                    <model>
                        <rootOnly>root</rootOnly>
                        <container>
                            <title>parent</title>
                            <entry><kind>first</kind><title>local</title></entry>
                            <entry><kind>second</kind></entry>
                        </container>
                    </model>
                </template>
                """)), Map.of());

        String output = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output, is("""
                first=local:root
                second=parent:root
                """));
    }

    @Test
    void testCurrentValueAndComplexValue() throws IOException {
        Files.writeString(tempDir.resolve("template.hbs"), """
                {{#container}}
                {{name}}=[{{.}}]=[{{value}}]:{{value.title}}
                {{/container}}
                """);
        execute(new TemplateTask(XMLElement.read("""
                <template source="template.hbs" target="output.txt">
                    <model>
                        <container>
                            <leaf>text</leaf>
                            <complex><title>Title</title></complex>
                        </container>
                    </model>
                </template>
                """)), Map.of());

        String output = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output, is("""
                leaf=[text]=[text]:
                complex=[]=[]:Title
                """));
    }

    @Test
    void testEmptyAndInvertedSections() throws IOException {
        Files.writeString(tempDir.resolve("template.hbs"), """
                {{empty}}|{{empty.@state}}
                {{#empty}}nonempty{{/empty}}{{^empty}}empty{{/empty}}
                {{#full}}[{{.}}]{{/full}}{{^full}}missing{{/full}}
                {{#container}}nonempty{{/container}}{{^container}}empty-container{{/container}}
                {{^missing}}missing{{/missing}}
                """);
        execute(new TemplateTask(XMLElement.read("""
                <template source="template.hbs" target="output.txt">
                    <model>
                        <empty state="missing"/>
                        <full>yes</full>
                        <container/>
                    </model>
                </template>
                """)), Map.of());

        String output = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output, is("""
                |missing
                empty
                [yes]
                empty-container
                missing
                """));
    }

    @Test
    void testDottedLookupCannotUseJavaMembersOrEscapeToOuterFields() throws IOException {
        Files.writeString(tempDir.resolve("template.hbs"), """
                [{{title.length}}][{{title.empty}}][{{title.class}}][{{title.toString}}][{{class}}]
                """);
        execute(new TemplateTask(XMLElement.read("""
                <template source="template.hbs" target="output.txt">
                    <model>
                        <title>Helidon</title>
                        <length>outer-length</length>
                        <empty>outer-empty</empty>
                        <class>xml-class</class>
                    </model>
                </template>
                """)), Map.of());

        String output = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output, is("[][][][][xml-class]\n"));
    }

    @Test
    void testPlaceholders() throws Exception {
        Files.writeString(tempDir.resolve("template.hbs"), "{{version}}");
        TemplateTask task = new TemplateTask(XMLElement.read("""
                <template source="template.hbs" target="output.txt">
                    <model>
                        <version>{version}</version>
                    </model>
                </template>
                """));

        execute(task, Map.of("version", "4.0.0"));
        String output1 = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output1, is("4.0.0"));

        execute(task, Map.of("version", "5.0.0"));
        String output2 = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output2, is("5.0.0"));
    }

    @Test
    void testAttributedScalarValues() throws IOException {
        Files.writeString(tempDir.resolve("template.hbs"), """
                {{version}}|{{version.@order}}|{{version.@channel}}
                {{empty}}|{{empty.@state}}|{{empty.@reason}}
                """);
        execute(new TemplateTask(XMLElement.read("""
                <template source="template.hbs" target="output.txt">
                    <model>
                        <version order="1" channel="stable">4.2.0</version>
                        <empty state="missing" reason="not-published"/>
                    </model>
                </template>
                """)), Map.of());

        String output = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output, is("""
                4.2.0|1|stable
                |missing|not-published
                """));
    }

    @Test
    void testAttributePlaceholders() throws IOException {
        Files.writeString(tempDir.resolve("template.hbs"), "{{version}}:{{version.@order}}");
        execute(new TemplateTask(XMLElement.read("""
                <template source="template.hbs" target="output.txt">
                    <model>
                        <version order="{order}">{version}</version>
                    </model>
                </template>
                """)), Map.of("version", "4.2.0", "order", "1"));

        String output = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output, is("4.2.0:1"));
    }

    @Test
    void testAttributesAndChildrenUseSeparateNames() throws IOException {
        Files.writeString(tempDir.resolve("template.hbs"), """
                {{release.version}}:{{release.@version}}:{{release.@channel}}
                {{#release}}
                {{index}}:{{name}}={{value}}:{{version}}:{{@version}}
                {{/release}}
                """);
        execute(new TemplateTask(XMLElement.read("""
                <template source="template.hbs" target="output.txt">
                    <model>
                        <release version="4.2.0" channel="stable">
                            <version>child-version</version>
                            <title>Helidon</title>
                        </release>
                    </model>
                </template>
                """)), Map.of());

        String output = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output, is("""
                child-version:4.2.0:stable
                0:version=child-version:child-version:4.2.0
                1:title=Helidon:child-version:4.2.0
                """));
    }

    @Test
    void testRepeatedAttributedScalars() throws IOException {
        Files.writeString(tempDir.resolve("template.hbs"), """
                {{#versions.version}}
                {{index}}:{{first}}:{{last}}:{{name}}={{value}}:{{@order}}:{{value.@name}}:{{@channel}}
                {{/versions.version}}
                """);
        execute(new TemplateTask(XMLElement.read("""
                <template source="template.hbs" target="output.txt">
                    <model>
                        <versions channel="stable">
                            <version order="1" name="first-name">4.1.0</version>
                            <version order="2" name="second-name">4.2.0</version>
                        </versions>
                    </model>
                </template>
                """)), Map.of());

        String output = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output, is("""
                0:true:false:version=4.1.0:1:first-name:stable
                1:false:true:version=4.2.0:2:second-name:stable
                """));
    }

    @Test
    void testExplicitPropertiesRetainResolvedAttributes() throws IOException {
        Files.writeString(tempDir.resolve("template.hbs"), """
                {{properties.@source}}:{{properties.version}}:{{properties.version.@order}}
                {{#properties}}
                {{name}}={{value}}:{{@order}}:{{@source}}
                {{/properties}}
                """);
        execute(new TemplateTask(XMLElement.read("""
                <template source="template.hbs" target="output.txt">
                    <model>
                        <properties source="{source}">
                            <version order="{order}">{version}</version>
                        </properties>
                    </model>
                </template>
                """)), Map.of("source", "local", "order", "1", "version", "4.2.0"));

        String output = Files.readString(tempDir.resolve("output.txt"));
        assertThat(output, is("""
                local:4.2.0:1
                version=4.2.0:1:local
                """));
    }

    @Test
    void testRejectsMixedTemplateContent() {
        XMLElement element = XMLElement.builder(XMLElement.read("""
                <template source="template.hbs" target="output.txt"/>
                """));
        XMLElement model = XMLElement.builder().name("model").parent(element);
        XMLElement release = XMLElement.builder().name("release").value("text").parent(model);
        release.children().add(XMLElement.builder().name("version").value("4.2.0").parent(release));
        model.children().add(release);
        element.children().add(model);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new TemplateTask(element));

        assertThat(ex.getMessage(), is("Model element 'model.release' cannot mix text and child elements"));
    }

    void execute(TemplateTask task, Map<String, String> vars) {
        try {
            task.execute(new StagingContext() {
                @Override
                public Path resolve(String path) {
                    return tempDir.resolve(path);
                }

                @Override
                public void ensureDirectory(Path directory) {
                    try {
                        Files.createDirectories(directory);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public Executor executor() {
                    return new CurrentThreadExecutorService();
                }
            }, tempDir, vars).toCompletableFuture().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e.getCause());
        }
    }
}
