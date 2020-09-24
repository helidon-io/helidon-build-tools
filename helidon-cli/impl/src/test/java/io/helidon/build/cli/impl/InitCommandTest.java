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
    protected InitCommandInvoker.Builder initCommandInvoker() {
        return super.initCommandInvoker()
                .buildProject(true);
    }

    @Test
    public void testDefaults() throws Exception {
        initCommandInvoker()
                .invoke()
                .validateProject();
    }

    @Test
    public void testFlavor() throws Exception {
        initCommandInvoker()
                .flavor("MP")
                .invoke()
                .validateProject();
    }

    @Test
    public void testGroupId() throws Exception {
        initCommandInvoker()
                .groupId("io.helidon.basicapp")
                .invoke()
                .validateProject();
    }

    @Test
    public void testArtifactId() throws Exception {
        InitCommandInvoker invoker = initCommandInvoker()
                .artifactId("foo-artifact")
                .invoke()
                .validateProject();
        assertThat(invoker.projectDir().getFileName().toString(), is("foo-artifact"));
    }

    @Test
    public void testPackage() throws Exception {
        initCommandInvoker()
                .packageName("io.helidon.mypackage")
                .invoke()
                .validateProject();
    }

    @Test
    public void testName() throws Exception {
        initCommandInvoker()
                .projectName("mybasicproject")
                .invoke()
                .validateProject();
    }

    @Test
    public void testInteractiveSe() throws Exception {
        initCommandInvoker()
                .input("input.txt")
                .invoke()
                .validateProject();
    }

    @Test
    public void testInteractiveMp() throws Exception {
        initCommandInvoker()
                .input("input.txt")
                .flavor("MP")
                .invoke()
                .validateProject();
    }
}
