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
package io.helidon.build.archetype.engine;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.ArchetypeDescriptor.Choice;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.FileSet;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.FileSets;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.FlowNode;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.Input;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.InputFlow;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.Replacement;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.Transformation;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.Property;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.Select;
import io.helidon.build.archetype.engine.ArchetypeDescriptor.TemplateSets;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;

/**
 * Tests {@link ArchetypeDescriptor}.
 */
public class ArchetypeDescriptorTest {

    @Test
    public void testUnmarshall() {
        InputStream is = ArchetypeDescriptorTest.class.getClassLoader()
                .getResourceAsStream(ArchetypeEngine.DESCRIPTOR_RESOURCE_NAME);
        assertThat(is, is(notNullValue()));

        ArchetypeDescriptor desc = ArchetypeDescriptor.read(is);
        assertThat(desc.modelVersion(), is(ArchetypeDescriptor.MODEL_VERSION));
        assertThat(desc.name(), is("helidon-quickstart-se"));
        Map<String, Property> properties = desc.properties().stream().collect(Collectors.toMap(Property::id, (p) -> p));
        assertThat(properties.entrySet(), is(not(empty())));
        assertThat(properties.size(), is(8));
        assertThat(properties.keySet(), hasItems("groupId", "artifactId", "version", "name", "package", "gradle", "maven", "helidonVersion"));
        assertThat(properties.get("version").value().get(), is("1.0-SNAPSHOT"));
        assertThat(properties.get("gradle").isExported(), is(false));
        assertThat(properties.get("gradle").isExported(), is(false));
        Property helidonVersion = properties.get("helidonVersion");
        assertThat(helidonVersion.isExported(), is(false));
        assertThat(helidonVersion.isReadonly(), is(true));
        assertThat(helidonVersion.value().orElse(null), is("2.0.0-SNAPSHOT"));

        Map<String, Transformation> transformations = desc.transformations().stream()
                .collect(Collectors.toMap(Transformation::id, (o) -> o));
        assertThat(transformations.size(), is(2));
        assertThat(transformations.keySet(), hasItems("packaged", "mustache"));
        List<Replacement> packaged = transformations.get("packaged").replacements();
        assertThat(packaged, is(not(empty())));
        assertThat(packaged.stream().map(Replacement::regex).collect(Collectors.toList()), hasItems("__pkg__"));
        assertThat(packaged.stream().map(Replacement::replacement).collect(Collectors.toList()),
                hasItems("${package/\\./\\/}"));
        List<Replacement> mustache = transformations.get("mustache").replacements();
        assertThat(mustache, is(not(empty())));
        assertThat(mustache.stream().map(Replacement::regex).collect(Collectors.toList()), hasItems("\\.mustache$"));
        assertThat(mustache.stream().map(Replacement::replacement).collect(Collectors.toList()), hasItems(""));

        assertThat(desc.templateSets().isPresent(), is(true));
        TemplateSets templateSets = desc.templateSets().get();
        assertThat(templateSets.transformations().stream().map(Transformation::id).collect(Collectors.toList()),
                hasItems("mustache"));
        assertThat(templateSets.templateSets().size(), is(4));

        FileSet ts1 = templateSets.templateSets().get(0);
        assertThat(ts1.transformations().stream().map(Transformation::id).collect(Collectors.toList()),
                hasItems("packaged"));
        assertThat(ts1.directory().get(), is("src/main/java"));
        assertThat(ts1.includes(), hasItems("**/*.mustache"));
        assertThat(ts1.excludes(), is(empty()));
        assertThat(ts1.ifProperties(), is(empty()));
        assertThat(ts1.unlessProperties(), is(empty()));

        FileSet ts2 = templateSets.templateSets().get(1);
        assertThat(ts2.transformations().stream().map(Transformation::id).collect(Collectors.toList()),
                hasItems("packaged"));
        assertThat(ts2.directory().get(), is("src/test/java"));
        assertThat(ts2.includes(), hasItems("**/*.mustache"));
        assertThat(ts2.excludes(), is(empty()));
        assertThat(ts2.ifProperties(), is(empty()));
        assertThat(ts2.unlessProperties(), is(empty()));

        FileSet ts3 = templateSets.templateSets().get(2);
        assertThat(ts3.ifProperties().stream().map(Property::id).collect(Collectors.toList()), hasItems("gradle"));
        assertThat(ts3.unlessProperties(), is(empty()));
        assertThat(ts3.transformations(), is(empty()));
        assertThat(ts3.directory().get(), is("."));
        assertThat(ts3.includes(), hasItems("build.gradle.mustache"));
        assertThat(ts3.excludes(), is(empty()));

        FileSet ts4 = templateSets.templateSets().get(3);
        assertThat(ts4.ifProperties().stream().map(Property::id).collect(Collectors.toList()), hasItems("maven"));
        assertThat(ts4.unlessProperties(), is(empty()));
        assertThat(ts4.transformations(), is(empty()));
        assertThat(ts4.directory().get(), is("."));
        assertThat(ts4.includes(), hasItems("pom.xml.mustache"));
        assertThat(ts4.excludes(), is(empty()));

        assertThat(desc.fileSets().isPresent(), is(true));
        FileSets fileSets = desc.fileSets().get();
        assertThat(fileSets.transformations(), is(empty()));
        assertThat(fileSets.fileSets().size(), is(4));
        FileSet fs1 = fileSets.fileSets().get(0);
        assertThat(fs1.transformations().stream().map(Transformation::id).collect(Collectors.toList()),
                hasItems("packaged"));
        assertThat(fs1.directory().get(), is("src/main/java"));
        assertThat(fs1.includes(), is(empty()));
        assertThat(fs1.excludes(), hasItems("**/*.mustache"));
        assertThat(fs1.ifProperties(), is(empty()));
        assertThat(fs1.unlessProperties(), is(empty()));

        FileSet fs2 = fileSets.fileSets().get(1);
        assertThat(fs2.transformations(), is(empty()));
        assertThat(fs2.directory().get(), is("src/main/resources"));
        assertThat(fs2.excludes(), is(empty()));
        assertThat(fs2.includes(), hasItems("**/*"));
        assertThat(fs2.ifProperties(), is(empty()));
        assertThat(fs2.unlessProperties(), is(empty()));

        FileSet fs3 = fileSets.fileSets().get(2);
        assertThat(fs3.transformations().stream().map(Transformation::id).collect(Collectors.toList()),
                hasItems("packaged"));
        assertThat(fs3.directory().get(), is("src/test/java"));
        assertThat(fs3.includes(), is(empty()));
        assertThat(fs3.excludes(), hasItems("**/*.mustache"));
        assertThat(fs3.ifProperties(), is(empty()));
        assertThat(fs3.unlessProperties(), is(empty()));

        FileSet fs4 = fileSets.fileSets().get(3);
        assertThat(fs4.transformations(), is(empty()));
        assertThat(fs4.directory().get(), is("src/test/resources"));
        assertThat(fs4.includes(), is(empty()));
        assertThat(fs4.excludes(), hasItems("**/*"));
        assertThat(fs4.ifProperties(), is(empty()));
        assertThat(fs4.unlessProperties(), is(empty()));

        InputFlow inputFlow = desc.inputFlow();
        assertThat(inputFlow.nodes().size(), is(5));

        FlowNode fn1 = inputFlow.nodes().get(0);
        assertThat(fn1, is(instanceOf(Input.class)));
        assertThat(((Input) fn1).property().id(), is("name"));
        assertThat(fn1.text(), is("Project name"));
        assertThat(((Input) fn1).defaultValue().isPresent(), is(true));
        assertThat(((Input) fn1).defaultValue().get(), is("${name}"));
        assertThat(fn1.ifProperties(), is(empty()));
        assertThat(fn1.unlessProperties(), is(empty()));

        FlowNode fn2 = inputFlow.nodes().get(1);
        assertThat(fn2, is(instanceOf(Input.class)));
        assertThat(((Input) fn2).property().id(), is("groupId"));
        assertThat(fn2.text(), is("Project groupId"));
        assertThat(((Input) fn2).defaultValue().isPresent(), is(true));
        assertThat(((Input) fn2).defaultValue().get(), is("${groupId}"));
        assertThat(fn2.ifProperties().stream().map(Property::id).collect(Collectors.toList()), hasItems("maven"));
        assertThat(fn2.unlessProperties(), is(empty()));

        FlowNode fn3 = inputFlow.nodes().get(2);
        assertThat(fn3, is(instanceOf(Input.class)));
        assertThat(((Input) fn3).property().id(), is("artifactId"));
        assertThat(fn3.text(), is("Project artifactId"));
        assertThat(((Input) fn3).defaultValue().isPresent(), is(true));
        assertThat(((Input) fn3).defaultValue().get(), is("${artifactId}"));
        assertThat(fn3.ifProperties(), is(empty()));
        assertThat(fn3.unlessProperties(), is(empty()));

        FlowNode fn4 = inputFlow.nodes().get(3);
        assertThat(fn4, is(instanceOf(Input.class)));
        assertThat(((Input) fn4).property().id(), is("version"));
        assertThat(fn4.text(), is("Project version"));
        assertThat(((Input) fn4).defaultValue().isPresent(), is(true));
        assertThat(((Input) fn4).defaultValue().get(), is("${version}"));
        assertThat(fn4.ifProperties(), is(empty()));
        assertThat(fn4.unlessProperties(), is(empty()));

        FlowNode fn5 = inputFlow.nodes().get(4);
        assertThat(fn5, is(instanceOf(Input.class)));
        assertThat(((Input) fn5).property().id(), is("package"));
        assertThat(fn5.text(), is("Java package name"));
        assertThat(((Input) fn5).defaultValue().isPresent(), is(true));
        assertThat(((Input) fn5).defaultValue().get(), is("${package}"));
        assertThat(fn5.ifProperties(), is(empty()));
        assertThat(fn5.unlessProperties(), is(empty()));
    }
}
