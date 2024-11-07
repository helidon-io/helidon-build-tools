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

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.BeforeAll;

/**
 * Class ArchetypeBaseTest.
 */
class ArchetypeBaseTest {

    static private File targetDir;

    @BeforeAll
    static void initialize() {
        URL url = ArchetypeEngineTest.class.getClassLoader().getResource("pom.xml.mustache");
        assert url != null;
        targetDir = new File(url.getFile()).getParentFile();
    }

    static File targetDir() {
        return targetDir;
    }
}
