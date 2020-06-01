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

import org.junit.jupiter.api.Test;

/**
 * Class InitDefaultTest.
 */
public class InitDefaultTest extends BaseCommandTest {

    @Test
    public void testDefaults() throws Exception {
        InitTestHelper helper = new InitTestHelper();
        helper.execute();
    }

    @Test
    public void testFlavor() throws Exception {
        InitTestHelper helper = new InitTestHelper();
        helper.flavor("MP");
        helper.execute();
    }

    @Test
    public void testGroupId() throws Exception {
        InitTestHelper helper = new InitTestHelper();
        helper.groupId("io.helidon.basicapp");
        helper.execute();
    }

    @Test
    public void testArtifactId() throws Exception {
        InitTestHelper helper = new InitTestHelper();
        helper.artifactId("basicapp");
        helper.execute();
    }

    @Test
    public void testPackage() throws Exception {
        InitTestHelper helper = new InitTestHelper();
        helper.packageName("io.helidon.mypackage");
        helper.execute();
    }

    @Test
    public void testName() throws Exception {
        InitTestHelper helper = new InitTestHelper();
        helper.name("mybasicproject");
        helper.execute();
    }
}
