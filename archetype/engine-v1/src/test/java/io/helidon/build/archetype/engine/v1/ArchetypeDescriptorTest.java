/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.engine.v1;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.FileSet;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.FileSets;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.FlowNode;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Input;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.InputFlow;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Replacement;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Transformation;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Property;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.TemplateSets;
import io.helidon.build.common.Lists;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;

/**
 * Tests {@link ArchetypeDescriptor}.
 */
class ArchetypeDescriptorTest {

    @Test
    void testUnmarshall() {
        InputStream is = getClass().getClassLoader().getResourceAsStream(ArchetypeEngine.DESCRIPTOR_RESOURCE_NAME);
        assertThat(is, is(notNullValue()));

        ArchetypeDescriptor desc = ArchetypeDescriptor.read(is);
        assertThat(desc.modelVersion(), is(ArchetypeDescriptor.MODEL_VERSION));
        assertThat(desc.name(), is("helidon-quickstart-se"));
        Map<String, Property> properties = desc.properties();
        assertThat(properties.entrySet(), is(not(empty())));
        assertThat(properties.size(), is(8));
        assertThat(properties.keySet(), is(Set.of(
                "groupId", "artifactId", "version", "name", "package", "gradle", "maven", "helidonVersion")));
        assertThat(properties.get("version").value().orElse(null), is("1.0-SNAPSHOT"));
        assertThat(properties.get("gradle").isExported(), is(false));
        assertThat(properties.get("gradle").isExported(), is(false));
        Property helidonVersion = properties.get("helidonVersion");
        assertThat(helidonVersion.isExported(), is(false));
        assertThat(helidonVersion.isReadonly(), is(true));
        assertThat(helidonVersion.value().orElse(null), is("2.0.0-SNAPSHOT"));

        Map<String, Transformation> transformations = desc.transformations();
        assertThat(transformations.size(), is(2));
        assertThat(transformations.keySet(), is(Set.of("packaged", "mustache")));
        List<Replacement> packaged = transformations.get("packaged").replacements();
        assertThat(packaged, is(not(empty())));
        assertThat(Lists.map(packaged, Replacement::regex), is(List.of("__pkg__")));
        assertThat(Lists.map(packaged, Replacement::replacement), is(List.of("${package/\\./\\/}")));
        List<Replacement> mustache = transformations.get("mustache").replacements();
        assertThat(mustache, is(not(empty())));
        assertThat(Lists.map(mustache, Replacement::regex), is(List.of("\\.mustache$")));
        assertThat(Lists.map(mustache, Replacement::replacement), is(List.of("")));

        assertThat(desc.templateSets().isPresent(), is(true));
        TemplateSets templateSets = desc.templateSets().get();
        assertThat(Lists.map(templateSets.transformations(), Transformation::id), is(List.of("mustache")));
        assertThat(templateSets.templateSets().size(), is(4));

        FileSet ts1 = templateSets.templateSets().get(0);
        assertThat(Lists.map(ts1.transformations(), Transformation::id), is(List.of("packaged")));
        assertThat(ts1.directory().orElse(null), is("src/main/java"));
        assertThat(ts1.includes(), is(List.of("**/*.mustache")));
        assertThat(ts1.excludes(), is(empty()));
        assertThat(ts1.ifProperties(), is(empty()));
        assertThat(ts1.unlessProperties(), is(empty()));

        FileSet ts2 = templateSets.templateSets().get(1);
        assertThat(Lists.map(ts2.transformations(), Transformation::id), is(List.of("packaged")));
        assertThat(ts2.directory().orElse(null), is("src/test/java"));
        assertThat(ts2.includes(), is(List.of("**/*.mustache")));
        assertThat(ts2.excludes(), is(empty()));
        assertThat(ts2.ifProperties(), is(empty()));
        assertThat(ts2.unlessProperties(), is(empty()));

        FileSet ts3 = templateSets.templateSets().get(2);
        assertThat(Lists.map(ts3.ifProperties(), Property::id), is(List.of("gradle")));
        assertThat(ts3.unlessProperties(), is(empty()));
        assertThat(ts3.transformations(), is(empty()));
        assertThat(ts3.directory().orElse(null), is("."));
        assertThat(ts3.includes().contains("build.gradle.mustache"), is(true));
        assertThat(ts3.excludes(), is(empty()));

        FileSet ts4 = templateSets.templateSets().get(3);
        assertThat(Lists.map(ts4.ifProperties(), Property::id), is(List.of("maven")));
        assertThat(ts4.unlessProperties(), is(empty()));
        assertThat(ts4.transformations(), is(empty()));
        assertThat(ts4.directory().orElse(null), is("."));
        assertThat(ts4.includes().contains("pom.xml.mustache"), is(true));
        assertThat(ts4.excludes(), is(empty()));

        assertThat(desc.fileSets().isPresent(), is(true));
        FileSets fileSets = desc.fileSets().get();
        assertThat(fileSets.transformations(), is(empty()));
        assertThat(fileSets.fileSets().size(), is(4));
        FileSet fs1 = fileSets.fileSets().get(0);
        assertThat(Lists.map(fs1.transformations(), Transformation::id), is(List.of("packaged")));
        assertThat(fs1.directory().orElse(null), is("src/main/java"));
        assertThat(fs1.includes(), is(empty()));
        assertThat(fs1.excludes(), is(List.of("**/*.mustache")));
        assertThat(fs1.ifProperties(), is(empty()));
        assertThat(fs1.unlessProperties(), is(empty()));

        FileSet fs2 = fileSets.fileSets().get(1);
        assertThat(fs2.transformations(), is(empty()));
        assertThat(fs2.directory().orElse(null), is("src/main/resources"));
        assertThat(fs2.excludes(), is(empty()));
        assertThat(fs2.includes(), is(List.of("**/*")));
        assertThat(fs2.ifProperties(), is(empty()));
        assertThat(fs2.unlessProperties(), is(empty()));

        FileSet fs3 = fileSets.fileSets().get(2);
        assertThat(Lists.map(fs3.transformations(), Transformation::id), is(List.of("packaged")));
        assertThat(fs3.directory().orElse(null), is("src/test/java"));
        assertThat(fs3.includes(), is(empty()));
        assertThat(fs3.excludes(), is(List.of("**/*.mustache")));
        assertThat(fs3.ifProperties(), is(empty()));
        assertThat(fs3.unlessProperties(), is(empty()));

        FileSet fs4 = fileSets.fileSets().get(3);
        assertThat(fs4.transformations(), is(empty()));
        assertThat(fs4.directory().orElse(null), is("src/test/resources"));
        assertThat(fs4.includes(), is(empty()));
        assertThat(fs4.excludes(), is(List.of("**/*")));
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
        assertThat(Lists.map(fn2.ifProperties(), Property::id), is(List.of("maven")));
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
