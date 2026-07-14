/*
 * Copyright (c) 2026, 2026 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
    void testValues() throws Exception {
        String output = execute("{{title}} {{git.url}} {{#properties}}{{cli.version}}{{/properties}}", """
                <model>
                    <title>Helidon</title>
                    <git><url>https://github.com/helidon-io/helidon</url></git>
                    <properties><cli.version>4.2.0</cli.version></properties>
                </model>
                """, Map.of());

        assertThat(output, is("Helidon https://github.com/helidon-io/helidon 4.2.0"));
    }

    @Test
    void testIteration() throws Exception {
        String output = execute("""
                {{#properties}}
                {{index}}:{{first}}:{{last}}:{{name}}={{value}}:{{cli.version}}
                {{/properties}}
                {{#components}}
                {{name}}={{title}}:{{git.url}}
                {{/components}}
                """, """
                <model>
                    <properties>
                        <cli.version>4.2.0</cli.version>
                        <schemas.version>2026.07</schemas.version>
                    </properties>
                    <components>
                        <cli>
                            <title>CLI</title>
                            <git>
                                <url>https://example.com/cli</url>
                            </git>
                        </cli>
                    </components>
                </model>
                """, Map.of());

        assertThat(output, is("""
                0:true:false:cli.version=4.2.0:4.2.0
                1:false:true:schemas.version=2026.07:4.2.0
                cli=CLI:https://example.com/cli
                """));
    }

    @Test
    void testPlaceholders() throws Exception {
        TemplateTask task = task("{{version}}", "<model><version>{version}</version></model>");
        Path outputDir = tempDir.resolve("stage");

        task.execute(context(), outputDir, Map.of("version", "4.0.0")).toCompletableFuture().get();
        assertThat(Files.readString(outputDir.resolve("index.txt")), is("4.0.0"));

        task.execute(context(), outputDir, Map.of("version", "5.0.0")).toCompletableFuture().get();
        assertThat(Files.readString(outputDir.resolve("index.txt")), is("5.0.0"));
    }

    @Test
    void testRepeatedValues() throws Exception {
        String output = execute("""
                {{version}}
                {{#versions}}
                {{index}}:{{first}}:{{last}}:{{name}}={{value}}
                {{/versions}}
                """, """
                <model>
                    <version>4.0.0</version>
                    <versions>
                        <version>4.0.0</version>
                        <version>4.1.0</version>
                    </versions>
                </model>
                """, Map.of());

        assertThat(output, is("""
                4.0.0
                0:true:false:version=4.0.0
                1:false:true:version=4.1.0
                """));
    }

    @Test
    void testRepeatedNestedElements() throws Exception {
        String output = execute("""
                {{#components}}
                {{index}}:{{first}}:{{last}}:{{name}}={{value.title}}:{{title}}
                {{/components}}
                {{#components.component}}
                {{index}}:{{first}}:{{last}}:{{name}}={{value.title}}:{{title}}
                {{/components.component}}
                """, """
                <model>
                    <components>
                        <component><title>CLI</title></component>
                        <component><title>WebServer</title></component>
                    </components>
                </model>
                """, Map.of());

        assertThat(output, is("""
                0:true:false:component=CLI:CLI
                1:false:true:component=WebServer:WebServer
                0:true:false:component=CLI:CLI
                1:false:true:component=WebServer:WebServer
                """));
    }

    @Test
    void testAttributes() {
        assertThrows(IllegalArgumentException.class,
                () -> TemplateModel.create(XMLElement.read("""
                        <model>
                            <properties type="x"/>
                        </model>
                        """)));
    }

    @Test
    void testMixedContent() {
        XMLElement model = XMLElement.builder().name("model").value("text");
        model.children().add(XMLElement.builder().name("version").value("4.0.0"));

        assertThrows(IllegalArgumentException.class,
                () -> TemplateModel.create(model));
    }

    private String execute(String template, String model, Map<String, String> vars) throws Exception {
        TemplateTask task = task(template, model);
        Path outputDir = tempDir.resolve("stage");
        task.execute(context(), outputDir, vars).toCompletableFuture().get();
        return Files.readString(outputDir.resolve("index.txt"));
    }

    private TemplateTask task(String template, String model) throws IOException {
        Files.writeString(tempDir.resolve("template.mustache"), template);
        return new TemplateTask(null, Map.of("source", "template.mustache", "target", "index.txt"),
                TemplateModel.create(XMLElement.read(model)));
    }

    private StagingContext context() {
        return new StagingContext() {
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
        };
    }
}
