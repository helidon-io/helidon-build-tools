/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.test.utils.TestFiles;
import io.helidon.build.common.xml.XMLElement;
import io.helidon.build.maven.cache.CacheConfig.LifecycleConfig;
import io.helidon.build.maven.cache.CacheConfig.ModuleSet;
import io.helidon.build.maven.cache.CacheConfig.ReactorRule;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.Maps.toProperties;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@link CacheConfig}.
 */
class CacheConfigTest {

    @Test
    void testConfig() throws Exception {
        Path configFile = TestFiles.testResourcePath(CacheConfigTest.class, "cache-config.xml");
        XMLElement elt = XMLElement.parse(Files.newInputStream(configFile));
        CacheConfig config = new CacheConfig(elt, toProperties(Map.of()), toProperties(Map.of()));

        assertThat(config.enabled(), is(true));
        assertThat(config.enableChecksums(), is(true));
        assertThat(config.includeAllChecksums(), is(true));

        assertThat(config.lifecyleConfig().size(), is(2));

        LifecycleConfig lifecycleConfig1 = config.lifecyleConfig().get(0);
        assertThat(lifecycleConfig1.path(), is("a-path"));
        assertThat(lifecycleConfig1.glob(), is("a-glob"));
        assertThat(lifecycleConfig1.regex(), is("a-regex"));
        assertThat(lifecycleConfig1.enabled(), is(true));
        assertThat(lifecycleConfig1.executionsExcludes(), is(List.of("exec-exclude")));
        assertThat(lifecycleConfig1.executionsIncludes(), is(List.of("exec-include")));
        assertThat(lifecycleConfig1.projectFilesExcludes(), is(List.of("project-exclude")));

        LifecycleConfig lifecycleConfig2 = config.lifecyleConfig().get(1);
        assertThat(lifecycleConfig2.glob(), is("foo/**"));
        assertThat(lifecycleConfig2.enabled(), is(false));

        assertThat(config.reactorRules().size(), is(1));

        ReactorRule reactorRule = config.reactorRules().get(0);
        assertThat(reactorRule.name(), is("reactorRule1"));
        assertThat(reactorRule.profiles(), is(List.of("profile1")));
        assertThat(reactorRule.moduleSets().size(), is(1));

        ModuleSet moduleSet = reactorRule.moduleSets().get(0);
        assertThat(moduleSet.name(), is("moduleSet1"));
        assertThat(moduleSet.includes(), is(List.of("module-include")));
        assertThat(moduleSet.excludes(), is(List.of("module-exclude")));
    }
}
