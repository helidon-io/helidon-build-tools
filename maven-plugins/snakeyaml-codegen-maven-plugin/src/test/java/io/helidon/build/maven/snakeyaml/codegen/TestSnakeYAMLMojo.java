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
package io.helidon.build.maven.snakeyaml.codegen;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.helidon.build.maven.snakeyaml.codegen.TestHelper.getFile;
import static org.junit.jupiter.api.Assertions.*;

class TestSnakeYAMLMojo {

    private static final File OUTPUT_DIR = getFile("target/test/generate-mojo");

    @Test
    void testSimple() throws Exception {
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

    @Test
    void testPropSub() throws Exception {
        SnakeYAMLMojo mojo = MavenPluginHelper.getInstance().getMojo("simpleTest/pom-prop-sub.xml",
                OUTPUT_DIR, "generate", SnakeYAMLMojo.class);
        mojo.execute();

        Type topLevelType = mojo.types().get("org.eclipse.microprofile.openapi.models.TopLevel");
        assertNotNull(topLevelType, "Expected Type for org.eclipse.microprofile.openapi.models.TopLevel not found");

        // Test the ability to set property subs after the type defs have been built.
        topLevelType.propertySubstitution("item", String.class.getName(), "getThing", "setThing");

        Type.PropertySubstitution sub = null;
        for (Type.PropertySubstitution s : topLevelType.propertySubstitutions()) {
            if (s.propertySubName().equals("item")) {
                sub = s;
                break;
            }
        }
        assertNotNull(sub, "Expected property substitution for 'item' not found");
        assertEquals("java.lang.String", sub.propertySubType(),
                "Expected replacement property type 'java.lang.String'; found '" + sub.propertySubType() + "' instead");
        assertEquals("getThing", sub.getter(),
                "Expected replacement getter 'getThing'; found '" + sub.getter() + "' instead");
        assertEquals("setThing", sub.setter(),
                "Expected replacement setter 'getThing'; found '" + sub.setter() + "' instead");
    }

    @Test
    void testPropName() {
        String setterMethodName = "setMyProperty";
        assertEquals("myProperty", SnakeYAMLMojo.EndpointScanner.propertyName(setterMethodName));

        setterMethodName = "setIPAddress";
        assertEquals("IPAddress", SnakeYAMLMojo.EndpointScanner.propertyName(setterMethodName));

    }
}
