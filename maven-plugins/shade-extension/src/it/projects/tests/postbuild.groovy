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
import io.helidon.build.maven.shade.HelidonTransformerTestIT

JUnitLauncher.builder()
        .select(HelidonTransformerTestIT.class, "testServiceRegistry", String.class)
        .select(HelidonTransformerTestIT.class, "testConfigMetadata", String.class)
        .select(HelidonTransformerTestIT.class, "testFeatureRegistry", String.class)
        .select(HelidonTransformerTestIT.class, "testServiceLoader", String.class)
        .select(HelidonTransformerTestIT.class, "testSerialConfig", String.class)
        .parameter("basedir", basedir.getAbsolutePath())
        .reportsDir(basedir)
        .outputFile(new File(basedir, "test.log"))
        .suiteId("tests")
        .suiteDisplayName("Helidon Shade Maven Plugin Extensions Tests")
        .build()
        .launch()

