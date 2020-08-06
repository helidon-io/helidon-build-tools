/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.cli.impl;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class InitDefaultTest.
 */
public class InitDefaultTest extends InitBaseTest {

    @BeforeEach
    public void beforeEach() {
        super.beforeEach();
        build(true);
    }

    @AfterEach
    public void afterEach() throws IOException {
        super.afterEach();
    }

    @Test
    public void testDefaults() throws Exception {
        generate();
        assertValid();
    }

    @Test
    public void testFlavor() throws Exception {
        flavor("MP");
        generate();
        assertValid();
    }

    @Test
    public void testGroupId() throws Exception {
        groupId("io.helidon.basicapp");
        generate();
        assertValid();

    }

    @Test
    public void testArtifactId() throws Exception {
        artifactId("basicapp");
        generate();
        assertValid();

    }

    @Test
    public void testPackage() throws Exception {
        packageName("io.helidon.mypackage");
        generate();
        assertValid();
    }

    @Test
    public void testName() throws Exception {
        projectName("mybasicproject");
        generate();
        assertValid();
    }
}
