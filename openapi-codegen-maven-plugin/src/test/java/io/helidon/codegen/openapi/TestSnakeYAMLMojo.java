/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.codegen.openapi;

import org.junit.jupiter.api.Test;

import java.io.File;

import static io.helidon.codegen.openapi.TestHelper.getFile;

public class TestSnakeYAMLMojo {

    private static final File OUTPUT_DIR = getFile("target/test/generate-mojo");

    @Test
    public void testSimple() throws Exception {
        SnakeYAMLMojo mojo = MavenPluginHelper.getInstance().getMojo("simpleTest/pom-one-class.xml",
                OUTPUT_DIR, "generate", SnakeYAMLMojo.class);
        mojo.execute();


    }
}
