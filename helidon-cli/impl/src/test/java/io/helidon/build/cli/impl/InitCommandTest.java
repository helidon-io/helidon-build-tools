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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Class InitDefaultTest.
 */
public class InitCommandTest extends InitCommandTestBase {

    @Override
    protected CommandInvoker.Builder commandInvoker() {
        return super.commandInvoker()
                .buildProject(true);
    }

    @Test
    public void testDefaults() throws Exception {
        commandInvoker()
                .invokeInit()
                .validateProject();
    }

    @Test
    public void testFlavor() throws Exception {
        commandInvoker()
                .flavor("MP")
                .invokeInit()
                .validateProject();
    }

    @Test
    public void testGroupId() throws Exception {
        commandInvoker()
                .groupId("io.helidon.basicapp")
                .invokeInit()
                .validateProject();
    }

    @Test
    public void testArtifactId() throws Exception {
        CommandInvoker invoker = commandInvoker()
                .artifactId("foo-artifact")
                .invokeInit()
                .validateProject();
        assertThat(invoker.projectDir().getFileName().toString(), is("foo-artifact"));
    }

    @Test
    public void testPackage() throws Exception {
        commandInvoker()
                .packageName("io.helidon.mypackage")
                .invokeInit()
                .validateProject();
    }

    @Test
    public void testName() throws Exception {
        commandInvoker()
                .projectName("mybasicproject")
                .invokeInit()
                .validateProject();
    }

    @Test
    public void testInteractiveSe() throws Exception {
        commandInvoker()
                .input("input.txt")
                .invokeInit()
                .validateProject();
    }

    @Test
    public void testInteractiveMp() throws Exception {
        commandInvoker()
                .input("input.txt")
                .flavor("MP")
                .invokeInit()
                .validateProject();
    }
}
