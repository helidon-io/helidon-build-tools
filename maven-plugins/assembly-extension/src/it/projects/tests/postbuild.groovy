/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

import io.helidon.build.common.test.utils.JUnitLauncher
import io.helidon.build.maven.plugins.tests.assembly.extensions.HelidonHandlerTestIT

JUnitLauncher.builder()
        .select(HelidonHandlerTestIT.class, "testServiceRegistry", String.class)
        .select(HelidonHandlerTestIT.class, "testConfigMetadata", String.class)
        .select(HelidonHandlerTestIT.class, "testFeatureRegistry", String.class)
        .select(HelidonHandlerTestIT.class, "testServiceLoader", String.class)
        .select(HelidonHandlerTestIT.class, "testSerialConfig", String.class)
        .parameter("basedir", basedir.getAbsolutePath())
        .reportsDir(basedir)
        .outputFile(new File(basedir, "test.log"))
        .suiteId("tests")
        .suiteDisplayName("Helidon Assembly Maven Plugin Extensions Tests")
        .build()
        .launch()
