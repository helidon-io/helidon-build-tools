/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype;

import io.helidon.build.archetype.engine.v2.ScriptLoader;
import io.helidon.build.archetype.engine.v2.Walker;
import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.ast.Invocation;
import io.helidon.build.archetype.engine.v2.ast.Node;
import io.helidon.build.archetype.engine.v2.ast.Node.VisitResult;
import io.helidon.build.archetype.engine.v2.ast.Preset;
import io.helidon.build.archetype.engine.v2.ast.Script;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static io.helidon.build.maven.archetype.Converter.convert;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link Converter}.
 */
class ConverterTest {

    private static final Path OUTPUT_DIR =  targetDir(ConverterTest.class).resolve("converter-ut");

    @BeforeAll
    static void beforeAll() throws IOException {
        Files.createDirectories(OUTPUT_DIR);
    }

    @Test
    void testConvert() throws XmlPullParserException, IOException {
        InputStream is = getClass().getResourceAsStream("/converter/simple.xml");
        assertThat(is, is(not(nullValue())));
        Reader reader = new InputStreamReader(is);
        PlexusConfiguration plexusConfiguration = new XmlPlexusConfiguration(Xpp3DomBuilder.build(reader));

        Path scriptFile = convert(plexusConfiguration, unique(OUTPUT_DIR, "simple", ".xml"));
        assertThat(Files.exists(scriptFile), is(true));

        Script script = ScriptLoader.load(scriptFile);
        List<Preset> presets = new LinkedList<>();
        List<Invocation> invocations = new LinkedList<>();
        Walker.walk(new Node.Visitor<Void>() {
            @Override
            public VisitResult visitBlock(Block block, Void arg) {
                return block.accept(new Block.Visitor<Void>() {
                    @Override
                    public VisitResult visitPreset(Preset preset, Void arg) {
                        assertThat(invocations, is(empty()));
                        presets.add(preset);
                        return VisitResult.CONTINUE;
                    }
                }, null);
            }

            @Override
            public VisitResult visitInvocation(Invocation invocation, Void arg) {
                invocations.add(invocation);
                // stop traversing as the paths are fake
                return VisitResult.SKIP_SUBTREE;
            }
        }, script, null);

        assertThat(presets.size(), is(4));

        Iterator<Preset> iterator = presets.iterator();
        Preset preset;

        preset = iterator.next();
        assertThat(preset.kind(), is(Block.Kind.ENUM));
        assertThat(preset.path(), is("theme"));
        assertThat(preset.value().asString(), is("colors"));

        preset = iterator.next();
        assertThat(preset.kind(), is(Block.Kind.BOOLEAN));
        assertThat(preset.path(), is("do-colors"));
        assertThat(preset.value().asBoolean(), is(false));

        preset = iterator.next();
        assertThat(preset.kind(), is(Block.Kind.TEXT));
        assertThat(preset.path(), is("palette-name"));
        assertThat(preset.value().asString(), is("rainbow"));

        preset = iterator.next();
        assertThat(preset.kind(), is(Block.Kind.LIST));
        assertThat(preset.path(), is("colors"));
        assertThat(preset.value().asList(), hasItems("red", "blue"));

        assertThat(invocations.size(), is(1));
        assertThat(invocations.get(0).kind(), is(Invocation.Kind.EXEC));
        assertThat(invocations.get(0).src(), is("mvn://com.example:archetype:1.0.0/main.xml"));
    }
}
