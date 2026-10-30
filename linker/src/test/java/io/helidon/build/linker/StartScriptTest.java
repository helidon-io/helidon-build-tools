/*
 * Copyright (c) 2019, 2026 Oracle and/or its affiliates.
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
package io.helidon.build.linker;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import io.helidon.build.common.InputStreams;
import io.helidon.build.common.test.utils.WorkDirExtension;

import org.hamcrest.Matcher;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.helidon.build.common.FileUtils.lastModifiedSeconds;
import static io.helidon.build.common.OSType.CURRENT_OS;
import static io.helidon.build.common.OSType.Linux;
import static io.helidon.build.common.OSType.Windows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Unit test for class {@link StartScript}.
 */
@Order(4)
@ExtendWith(WorkDirExtension.class)
class StartScriptTest {

    private final Path cwd;
    private final Path binDir;
    private final Path jarFile;
    private final Path modulesFile;

    StartScriptTest(Path cwd) {
        this.cwd = cwd;
        try {
            binDir = Files.createDirectories(cwd.resolve("bin"));
            Path appDir = Files.createDirectories(cwd.resolve("app"));
            jarFile = Files.createFile(appDir.resolve("main.jar"));
            Path libDir = Files.createDirectories(cwd.resolve("lib"));
            modulesFile = Files.createFile(libDir.resolve("modules"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testExitOnStarted() {
        String script = builder().build().toString();
        assertThat(script, containsString("-Dexit.on.started=!"));
    }

    @Test
    void testJarName() {
        String script = builder().build().toString();
        String jarName = jarFile.getFileName().toString();
        assertThat(script, containsString("Start " + jarName));
        assertThat(script, containsString("passed as arguments to " + jarName));
        assertThat(script, containsString("jarName=\"" + jarName + "\""));
    }

    @Test
    void testDefaultJvmOptions() {
        String script = builder().build().toString();
        assertThat(script, containsString("DEFAULT_APP_JVM     Sets default JVM options."));
        assertThat(script, containsString("defaultJvm=\"\""));

        script = builder().defaultJvmOptions(List.of("-verbose:class", "-Xms32")).build().toString();
        assertThat(script, containsString("DEFAULT_APP_JVM     Overrides "));
        assertThat(script, containsString("defaultJvm=\"-verbose:class -Xms32\""));
    }

    @Test
    void testDefaultDebugOptions() {
        String script = builder().build().toString();
        assertThat(script, containsString("DEFAULT_APP_DEBUG   Overrides "));
        assertThat(script, containsString("defaultDebug=\"" + Configuration.DEFAULT_DEBUG + "\""));

        script = builder().defaultDebugOptions(List.of("-Xdebug", "-Xnoagent")).build().toString();
        assertThat(script, containsString("DEFAULT_APP_DEBUG   Overrides "));
        assertThat(script, containsString("defaultDebug=\"-Xdebug -Xnoagent\""));
    }

    @Test
    void testDefaultArguments() {
        String script = builder().build().toString();
        assertThat(script, containsString("DEFAULT_APP_ARGS    Sets default arguments."));
        assertThat(script, containsString("defaultArgs=\"\""));

        script = builder().defaultArgs(List.of("--foo", "bar")).build().toString();
        assertThat(script, containsString("DEFAULT_APP_ARGS    Overrides "));
        assertThat(script, containsString("defaultArgs=\"--foo bar\""));
    }

    @Test
    void testDefaultConditionals() {
        String script = builder().build().toString();
        assertThat(script, containsString("--noCds         Do not use CDS."));
        assertThat(script, containsString("--debug         Add JVM debug options."));
        assertThat(script, containsString("DEFAULT_APP_DEBUG"));

        assertThat(script, containsString("defaultDebug="));
        assertThat(script, containsString("cdsOption="));
        assertThat(script, containsString("useCds="));
        assertThat(script, containsString("debug"));

        assertThat(script, containsString("--noCds"));
        assertThat(script, containsString("--debug"));

        assertThat(script, containsString("${useCds}"));
        assertThat(script, containsString("${debug}"));

        assertThat(script, containsString(modulesTimeStampComparison()));
        assertThat(script, containsString(jarTimeStampComparison()));
        assertThat(script, containsString("TimeStamp"));

        assertThat(script, containsString("-Dexit.on.started=!"));
    }

    @Test
    void testConditionalsNoCDS() {
        String script = builder().cdsInstalled(false).build().toString();
        assertThat(script, not(containsString("--noCds         Do not use CDS.")));
        assertThat(script, containsString("--debug         Add JVM debug options."));
        assertThat(script, containsString("DEFAULT_APP_DEBUG"));

        assertThat(script, containsString("defaultDebug="));
        assertThat(script, not(containsString("cdsOption=")));
        assertThat(script, not(containsString("useCds=")));
        assertThat(script, containsString("debug"));

        assertThat(script, not(containsString("--noCds")));
        assertThat(script, containsString("--debug"));

        assertThat(script, not(containsString("${useCds}")));
        assertThat(script, containsString("${debug}"));

        assertThat(script, not(containsString(modulesTimeStampComparison())));
        assertThat(script, not(containsString(jarTimeStampComparison())));
        assertThat(script, not(containsString("timeStamp")));

        assertThat(script, containsString("-Dexit.on.started=!"));
    }

    @Test
    void testConditionalsNoDebug() {
        String script = builder().debugInstalled(false).build().toString();
        assertThat(script, containsString("--noCds         Do not use CDS."));
        assertThat(script, not(containsString("--debug         Add JVM debug options.")));
        assertThat(script, not(containsString("DEFAULT_APP_DEBUG")));

        assertThat(script, not(containsString("defaultDebug=")));
        assertThat(script, containsString("cdsOption="));
        assertThat(script, containsString("useCds="));
        assertThat(script, not(containsString("debug")));

        assertThat(script, containsString("--noCds"));
        assertThat(script, not(containsString("--debug")));

        assertThat(script, containsString("${useCds}"));
        assertThat(script, not(containsString("${debug}")));

        assertThat(script, containsString(modulesTimeStampComparison()));
        assertThat(script, containsString(jarTimeStampComparison()));
        assertThat(script, containsString("TimeStamp"));

        assertThat(script, containsString("-Dexit.on.started=!"));
    }

    @Test
    void testConditionalsNoCDsNoDebug() {
        String script = builder().cdsInstalled(false).debugInstalled(false).build().toString();
        assertThat(script, not(containsString("--noCds         Do not use CDS.")));
        assertThat(script, not(containsString("--debug         Add JVM debug options.")));
        assertThat(script, not(containsString("DEFAULT_APP_DEBUG")));

        assertThat(script, not(containsString("defaultDebug=")));
        assertThat(script, not(containsString("cdsOption=")));
        assertThat(script, not(containsString("useCds=")));
        assertThat(script, not(containsString("debug")));

        assertThat(script, not(containsString("--noCds")));
        assertThat(script, not(containsString("--debug")));

        assertThat(script, not(containsString("${useCds}")));
        assertThat(script, not(containsString("${debug}")));

        assertThat(script, not(containsString(modulesTimeStampComparison())));
        assertThat(script, not(containsString(jarTimeStampComparison())));
        assertThat(script, not(containsString("timeStamp")));

        assertThat(script, containsString("-Dexit.on.started=!"));
    }

    @Test
    void testInstall() throws Exception {
        Path installedScript = binDir.resolve(CURRENT_OS.withScriptExtension("start"));
        Files.deleteIfExists(installedScript);
        StartScript script = builder().build();
        Path scriptFile = script.install();
        assertThat(Files.exists(scriptFile), is(true));
        assertExecutable(scriptFile);
        assertThat(scriptFile, is(installedScript));
        String onDisk = InputStreams.toString(new FileInputStream(scriptFile.toFile()));
        assertThat(onDisk, is(script.toString()));
    }

    StartScript.Builder builder() {
        return StartScript.builder()
                .mainJar(jarFile)
                .installHomeDirectory(cwd)
                .exitOnStartedValue("!");
    }

    String modulesTimeStampComparison() {
        return timeStampComparison("modules", modulesFile);
    }

    String jarTimeStampComparison() {
        return timeStampComparison("jar", jarFile);
    }

    static void assertExecutable(Path file) throws IOException {
        if (CURRENT_OS.isPosix()) {
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

    static String timeStampComparison(String name, Path file) {
        String timestamp = Long.toString(lastModifiedSeconds(file));
        String notEqual = CURRENT_OS == Windows ? "-ne" : "!=";
        return String.format("${%sTimeStamp} %s \"%s\"", name, notEqual, timestamp);
    }

    static Matcher<String> containsString(String substring) {
        if (CURRENT_OS == Windows) {
            substring = substring.replace(Linux.escapedQuote(), Windows.escapedQuote());
        }
        return StringContains.containsString(substring);
    }
}
