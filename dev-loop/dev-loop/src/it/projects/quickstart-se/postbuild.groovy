/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

JUnitLauncher.builder()
        .selectPackage("io.helidon.build.devloop")
        .selectPackage("io.helidon.build.devloop.maven")
        .reportsDir(basedir)
        .parameter("basedir", basedir.getAbsolutePath())
        .parameter("junit.jupiter.testclass.order.default", 'org.junit.jupiter.api.ClassOrderer$OrderAnnotation')
        .outputFile(new File(basedir, "target/test.log"))
        .suiteId("devloop-it")
        .suiteDisplayName("Devloop Integration Test")
        .build()
        .launch()
