/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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

package io.helidon.lsp.server.management;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import io.helidon.build.common.FileUtils;
import io.helidon.lsp.common.Dependency;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MavenSupportTest {

    private static final String LOCAL_REPO_ARG;

    static {
        String localRepository = System.getProperty("localRepository");
        LOCAL_REPO_ARG = localRepository != null ? "-Dmaven.repo.local=" + localRepository : null;
    }

    @Test
    public void ignoreFakePomFileTest() throws URISyntaxException {
        Path pom = getCurrentPom();
        Path testFile = Paths.get("src/test/resources/pomTests/withoutMain/src/test.txt");
        Path resolvedPom = MavenSupport.resolvePom(testFile);
        assertThat(pom, is(resolvedPom));
    }

    @Test
    public void getPomFileForCorrectMavenStructureFolderTest() {
        Path pom = Paths.get("src/test/resources/pomTests/withMain/pom.xml");
        Path testFile = Paths.get("src/test/resources/pomTests/withMain/src/main/test.txt");
        Path resolvedPom = MavenSupport.resolvePom(testFile);
        assertThat(pom.toAbsolutePath(), is(resolvedPom));
    }

    @Test
    public void getPomForFileTest() throws URISyntaxException {
        Path pom = getCurrentPom();
        assertThat(pom.getFileName().toString(), is("pom.xml"));
    }

    @Test
    public void getDependenciesTest() throws URISyntaxException {
        Path pom = getCurrentPom();
        Set<Dependency> dependencies = MavenSupport.instance().dependencies(pom, 10000,  LOCAL_REPO_ARG);
        assertThat(dependencies.isEmpty(), is(false));
    }

    private Path getCurrentPom() throws URISyntaxException {
        URI uri = MavenSupportTest.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        return MavenSupport.resolvePom(FileUtils.pathOf(uri));
    }
}
