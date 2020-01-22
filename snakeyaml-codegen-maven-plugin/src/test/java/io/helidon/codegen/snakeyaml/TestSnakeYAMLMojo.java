/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package io.helidon.codegen.snakeyaml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.helidon.codegen.snakeyaml.TestHelper.getFile;

public class TestSnakeYAMLMojo {

    private static final File OUTPUT_DIR = getFile("target/test/generate-mojo");

    @Test
    public void testSimple() throws Exception {
        SnakeYAMLMojo mojo = MavenPluginHelper.getInstance().getMojo("simpleTest/pom-one-class.xml",
                OUTPUT_DIR, "generate", SnakeYAMLMojo.class);
        mojo.execute();


        Map<String, List<String>> interfaces = mojo.interfaces();
        Set<SnakeYAMLMojo.Import> imports = mojo.imports();

        assertTrue(interfaces.containsKey("TopLevel"), "Implementations does not contain expected entry for TopLevel");
        assertEquals("TopLevelImpl", interfaces.get("TopLevel").get(0), "Unexpected implementation for TopLevel found");

        SnakeYAMLMojo.Import referenceImport = new SnakeYAMLMojo.Import("io.helidon.codegen.snakeyaml.test.Reference", false);
        SnakeYAMLMojo.Import topLevelImport = new SnakeYAMLMojo.Import("org.eclipse.microprofile.openapi.models.TopLevel", false);
        SnakeYAMLMojo.Import topLevelImplImport = new SnakeYAMLMojo.Import("io.smallrye.openapi.api.models.TopLevelImpl", false);
        assertTrue(imports.contains(referenceImport), "Missing Reference import");
        assertTrue(imports.contains(topLevelImplImport), "Missing TopLevelImpl import");
        assertTrue(imports.contains(topLevelImport), "Missing TopLevel import");
    }
}
