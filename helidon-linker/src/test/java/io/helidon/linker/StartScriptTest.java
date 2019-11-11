/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.linker;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import io.helidon.linker.util.FileUtils;
import io.helidon.linker.util.StreamUtils;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for class {@link StartScript}.
 */
class StartScriptTest {

    private static final Path MAIN_JAR = TestFiles.helidonSeJar();
    private static final String MAIN_JAR_NAME = MAIN_JAR.getFileName().toString();

    private StartScript.Builder builder() {
        return StartScript.builder().mainJar(MAIN_JAR);
    }

    @Test
    void testMainJarName() {
        String script = builder().build().toString();
        assertThat(script, containsString("Start " + MAIN_JAR_NAME));
        assertThat(script, containsString("passed to " + MAIN_JAR_NAME));
        assertThat(script, containsString("mainJarName=\"" + MAIN_JAR_NAME + "\""));
    }

    @Test
    void testDefaultJvmOptions() {
        String script = builder().build().toString();
        assertThat(script, containsString("JVM_OPTIONS    Sets JVM options."));
        assertThat(script, containsString("defaultJvmOptions=\"\""));

        script = builder().jvmOptions(List.of("-verbose:class", "-Xms32")).build().toString();
        assertThat(script, containsString("JVM_OPTIONS    Overrides default: ${defaultJvmOptions}"));
        assertThat(script, containsString("defaultJvmOptions=\"-verbose:class -Xms32\""));
    }

    @Test
    void testDefaultDebugOptions() {
        String script = builder().build().toString();
        assertThat(script, containsString("DEBUG_OPTIONS  Overrides default: ${defaultDebugOptions}"));
        assertThat(script, containsString("defaultDebugOptions=\"" + StartScript.Builder.DEFAULT_DEBUG + "\""));

        script = builder().debugOptions(List.of("-Xdebug", "-Xnoagent")).build().toString();
        assertThat(script, containsString("DEBUG_OPTIONS  Overrides default: ${defaultDebugOptions}"));
        assertThat(script, containsString("defaultDebugOptions=\"-Xdebug -Xnoagent\""));
    }

    @Test
    void testDefaultArguments() {
        String script = builder().build().toString();
        assertThat(script, containsString("MAIN_ARGS      Sets arguments."));
        assertThat(script, containsString("defaultMainArgs=\"\""));

        script = builder().args(List.of("--foo", "bar")).build().toString();
        assertThat(script, containsString("MAIN_ARGS      Overrides default: ${defaultMainArgs}"));
        assertThat(script, containsString("defaultMainArgs=\"--foo bar\""));
    }

    @Test
    void testInstall() throws Exception {
        Path targetDir = TestFiles.targetDir();
        Path binDir = FileUtils.ensureDirectory(targetDir.resolve("scripts/bin"));
        Files.deleteIfExists(binDir.resolve("start"));
        StartScript script = builder().build();
        Path scriptFile = script.install(binDir.getParent());
        assertThat(Files.exists(scriptFile), is(true));
        assertExecutable(scriptFile);
        String onDisk = StreamUtils.toString(new FileInputStream(scriptFile.toFile()));
        assertThat(onDisk, is(script.toString()));
    }

    private static void assertExecutable(Path file) throws IOException {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
        assertThat(file.toString(), perms, is(Set.of(PosixFilePermission.OWNER_READ,
                                                     PosixFilePermission.OWNER_EXECUTE,
                                                     PosixFilePermission.OWNER_WRITE,
                                                     PosixFilePermission.GROUP_READ,
                                                     PosixFilePermission.GROUP_EXECUTE,
                                                     PosixFilePermission.OTHERS_READ,
                                                     PosixFilePermission.OTHERS_EXECUTE)));
    }
}
