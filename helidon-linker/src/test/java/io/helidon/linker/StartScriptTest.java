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
import io.helidon.test.util.TestFiles;

import org.junit.jupiter.api.Test;

import static io.helidon.linker.util.Constants.DIR_SEP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Unit test for class {@link StartScript}.
 */
class StartScriptTest {

    private static final Path JAR = TestFiles.helidonSeJar();
    private static final String JAR_NAME = JAR.getFileName().toString();
    private static final Path BIN_DIR = FileUtils.ensureDirectory(TestFiles.targetDir().resolve("scripts" + DIR_SEP + "bin"));

    private StartScript.Builder builder() {
        return StartScript.builder().mainJar(JAR).installDirectory(BIN_DIR);
    }

    @Test
    void testJarName() {
        String script = builder().build().toString();
        assertThat(script, containsString("Start " + JAR_NAME));
        assertThat(script, containsString("passed as arguments to " + JAR_NAME));
        assertThat(script, containsString("jarName=\"" + JAR_NAME + "\""));
    }

    @Test
    void testDefaultJvmOptions() {
        String script = builder().build().toString();
        assertThat(script, containsString("DEFAULT_JVM     Sets default JVM options."));
        assertThat(script, containsString("defaultJvm=\"\""));

        script = builder().defaultJvmOptions(List.of("-verbose:class", "-Xms32")).build().toString();
        assertThat(script, containsString("DEFAULT_JVM     Overrides "));
        assertThat(script, containsString("defaultJvm=\"-verbose:class -Xms32\""));
    }

    @Test
    void testDefaultDebugOptions() {
        String script = builder().build().toString();
        assertThat(script, containsString("DEFAULT_DEBUG   Overrides "));
        assertThat(script, containsString("defaultDebug=\"" + Configuration.Builder.DEFAULT_DEBUG + "\""));

        script = builder().defaultDebugOptions(List.of("-Xdebug", "-Xnoagent")).build().toString();
        assertThat(script, containsString("DEFAULT_DEBUG   Overrides "));
        assertThat(script, containsString("defaultDebug=\"-Xdebug -Xnoagent\""));
    }

    @Test
    void testDefaultArguments() {
        String script = builder().build().toString();
        assertThat(script, containsString("DEFAULT_ARGS    Sets default arguments."));
        assertThat(script, containsString("defaultArgs=\"\""));

        script = builder().defaultArgs(List.of("--foo", "bar")).build().toString();
        assertThat(script, containsString("DEFAULT_ARGS    Overrides "));
        assertThat(script, containsString("defaultArgs=\"--foo bar\""));
    }

    @Test
    void testDefaultConditionals() {
        String script = builder().build().toString();
        assertThat(script, containsString("--noCds         Do not use CDS."));
        assertThat(script, containsString("--debug         Add JVM debug options."));
        assertThat(script, containsString("DEFAULT_DEBUG"));
 
        assertThat(script, containsString("local -r defaultDebug="));
        assertThat(script, containsString("local -r cdsOption="));
        assertThat(script, containsString("local useCds="));
        assertThat(script, containsString("local debug"));

        assertThat(script, containsString("--noCds)"));
        assertThat(script, containsString("--debug)"));

        assertThat(script, containsString("${useCds}"));
        assertThat(script, containsString("${debug}"));
   }

    @Test
    void testConditionalsNoCDS() {
        String script = builder().cdsInstalled(false).build().toString();
        assertThat(script, not(containsString("--noCds         Do not use CDS.")));
        assertThat(script, containsString("--debug         Add JVM debug options."));
        assertThat(script, containsString("DEFAULT_DEBUG"));

        assertThat(script, containsString("local -r defaultDebug="));
        assertThat(script, not(containsString("local -r cdsOption=")));
        assertThat(script, not(containsString("local useCds=")));
        assertThat(script, containsString("local debug"));

        assertThat(script, not(containsString("--noCds)")));
        assertThat(script, containsString("--debug)"));

        assertThat(script, not(containsString("${useCds}")));
        assertThat(script, containsString("${debug}"));
    }

    @Test
    void testConditionalsNoDebug() {
        String script = builder().debugInstalled(false).build().toString();
        assertThat(script, containsString("--noCds         Do not use CDS."));
        assertThat(script, not(containsString("--debug         Add JVM debug options.")));
        assertThat(script, not(containsString("DEFAULT_DEBUG")));

        assertThat(script, not(containsString("local -r defaultDebug=")));
        assertThat(script, containsString("local -r cdsOption="));
        assertThat(script, containsString("local useCds="));
        assertThat(script, not(containsString("local debug")));

        assertThat(script, containsString("--noCds)"));
        assertThat(script, not(containsString("--debug)")));

        assertThat(script, containsString("${useCds}"));
        assertThat(script, not(containsString("${debug}")));
    }

    @Test
    void testConditionalsNoCDsNoDebug() {
        String script = builder().cdsInstalled(false).debugInstalled(false).build().toString();
        assertThat(script, not(containsString("--noCds         Do not use CDS.")));
        assertThat(script, not(containsString("--debug         Add JVM debug options.")));
        assertThat(script, not(containsString("DEFAULT_DEBUG")));

        assertThat(script, not(containsString("local -r defaultDebug=")));
        assertThat(script, not(containsString("local -r cdsOption=")));
        assertThat(script, not(containsString("local useCds=")));
        assertThat(script, not(containsString("local debug")));

        assertThat(script, not(containsString("--noCds)")));
        assertThat(script, not(containsString("--debug)")));

        assertThat(script, not(containsString("${useCds}")));
        assertThat(script, not(containsString("${debug}")));
    }

    @Test
    void testInstall() throws Exception {
        Files.deleteIfExists(BIN_DIR.resolve("start"));
        StartScript script = builder().build();
        Path scriptFile = script.install();
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
