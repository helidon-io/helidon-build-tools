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
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenSupportTest {

    @Test
    public void ignoreFakePomFileTest() throws URISyntaxException {
        String pomForFile = getCurrentPom();
        String testFile = Paths.get("src", "test", "resources", "pomTests", "withoutMain", "src", "test.txt")
                               .toAbsolutePath()
                               .toString();
        String resolvedPom = MavenSupport.instance().resolvePom(testFile);
        assertEquals(pomForFile, resolvedPom);
    }

    @Test
    public void getPomFileForCorrectMavenStructureFolderTest() {
        String pomForFile = Paths.get("src", "test", "resources", "pomTests", "withMain", "pom.xml")
                                 .toAbsolutePath()
                                 .toString();
        String testFile = Paths.get("src", "test", "resources", "pomTests", "withMain", "src", "main", "test.txt")
                               .toAbsolutePath()
                               .toString();
        String resolvedPom = MavenSupport.instance().resolvePom(testFile);
        assertEquals(pomForFile, resolvedPom);
    }

    @Test
    public void getPomForFileTest() throws URISyntaxException {
        String pomForFile = getCurrentPom();
        assertTrue(pomForFile.endsWith("pom.xml"));
    }

    @Test
    public void getDependenciesTest() throws URISyntaxException {
        String pomForFile = getCurrentPom();
        Set<io.helidon.lsp.common.Dependency> dependencies = MavenSupport.instance().dependencies(pomForFile, 10000);
        assertTrue(dependencies.size() > 0);
    }

    private String getCurrentPom() throws URISyntaxException {
        URI uri = MavenSupportTest.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        return MavenSupport.instance().resolvePom(uri.getPath());
    }

}