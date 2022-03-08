/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Class InitDefaultTest.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InitCommandTest extends InitCommandTestBase {

    @Override
    protected CommandInvoker.Builder commandInvoker() {
        return super.commandInvoker()
                .buildProject(true);
    }

    @Test
    @Order(1)
    public void testProjectOption() throws Exception {
        System.out.println("TEST: testProjectOption");
        CommandInvoker.InvocationResult result = commandInvoker()
                .projectName("project-option")
                .packageName("io.helidon.mypackage")
                .useProjectOption(true)
                .invokeInit();
        System.out.println("\n\nOUTPUT: " + result.output);

               result.validateProject();
    }

    @Test
    @Order(2)
    public void testProjectArgument() throws Exception {
        System.out.println("TEST: testProjectArgument");
        CommandInvoker.InvocationResult result = commandInvoker()
                .projectName("project-argument")
                .packageName("io.helidon.mypackage")
                .useProjectOption(false)
                .invokeInit();
        System.out.println("\n\nOUTPUT: " + result.output);

        result.validateProject();
    }

    @Test
    @Disabled
    public void testDefaults() throws Exception {
        commandInvoker()
                .invokeInit()
                .validateProject();
    }

    @Test
    @Disabled
    public void testFlavor() throws Exception {
        commandInvoker()
                .flavor("MP")
                .invokeInit()
                .validateProject();
    }

    @Test
    @Disabled
    public void testGroupId() throws Exception {
        commandInvoker()
                .groupId("io.helidon.basicapp")
                .invokeInit()
                .validateProject();
    }

    @Test
    @Disabled
    public void testArtifactId() throws Exception {
        CommandInvoker invoker = commandInvoker()
                .artifactId("foo-artifact")
                .invokeInit()
                .validateProject();
        assertThat(invoker.projectDir().getFileName().toString(), is("foo-artifact"));
    }

    @Test
    @Disabled
    public void testPackage() throws Exception {
        commandInvoker()
                .packageName("io.helidon.mypackage")
                .invokeInit()
                .validateProject();
    }

    @Test
    @Disabled
    public void testName() throws Exception {
        commandInvoker()
                .projectName("mybasicproject")
                .invokeInit()
                .validateProject();
    }

    @Test
    @Disabled
    public void testInteractiveSe() throws Exception {
        commandInvoker()
                .input(getClass().getResource("input.txt"))
                .invokeInit()
                .validateProject();
    }

    @Test
    @Disabled
    public void testInteractiveMp() throws Exception {
        commandInvoker()
                .input(getClass().getResource("input.txt"))
                .flavor("MP")
                .invokeInit()
                .validateProject();
    }
}
