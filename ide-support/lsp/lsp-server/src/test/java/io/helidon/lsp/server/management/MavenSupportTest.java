/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenSupportTest {

    @Test
    public void getPomForFileTest() throws URISyntaxException, IOException {
        String pomForFile = getPomForCurrentClass();
        assertTrue(pomForFile.endsWith("pom.xml"));
    }

    @Test
    public void getDependenciesTest() throws URISyntaxException, IOException {
        String pomForFile = getPomForCurrentClass();
        Set<io.helidon.lsp.common.Dependency> dependencies = MavenSupport.instance().dependencies(pomForFile, 10000);
        assertTrue(dependencies.size() > 0);
    }

    private String getPomForCurrentClass() throws IOException, URISyntaxException {
        URI uri = MavenSupportTest.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        return MavenSupport.instance().resolvePom(uri.getPath());
    }

}