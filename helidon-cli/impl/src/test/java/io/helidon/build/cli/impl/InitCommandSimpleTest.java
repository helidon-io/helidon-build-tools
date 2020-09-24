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

import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.cli.harness.Config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Simple sequence of commands that starts with {@code helidon init}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InitCommandSimpleTest extends InitCommandTestBase {

    private static final AtomicReference<InitCommandInvoker> INVOKER_REF = new AtomicReference<>();

    @AfterAll
    public static void afterAll() {
        MetadataAccessTestBase.stopMetadataAccess();
    }

    @Test
    @Order(1)
    public void testInit() throws Exception {
        INVOKER_REF.set(initCommandInvoker().invoke().validateProject());
    }

    @Test
    @Order(2)
    public void testBuild() throws Exception {
        INVOKER_REF.get().invokeBuildCommand().assertJarExists();
    }

    @Test
    @Order(3)
    public void testInfo() throws Exception {
        Config.userConfig().clearPlugins();
        assertThat(INVOKER_REF.get().invokeInfoCommand().output(), containsString("plugin"));
    }

    @Test
    @Order(4)
    public void testVersion() throws Exception {
        String output = INVOKER_REF.get().invokeVersionCommand().output();
        assertThat(output, containsString("version"));
        assertThat(output, containsString("helidon.version"));
    }
}
