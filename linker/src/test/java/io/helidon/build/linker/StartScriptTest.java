/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates.
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

import io.helidon.build.linker.util.Constants;
import org.hamcrest.Matcher;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.ensureFile;
import static io.helidon.build.common.FileUtils.lastModifiedSeconds;
import static io.helidon.build.common.OSType.Linux;
import static io.helidon.build.common.OSType.Windows;
import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Unit test for class {@link StartScript}.
 */
@Order(4)
class StartScriptTest {

    private static final Path INSTALL_DIR = ensureDirectory(targetDir(StartScriptTest.class).resolve("script-home"));
    private static final Path BIN_DIR = ensureDirectory(INSTALL_DIR.resolve("bin"));
    private static final Path LIB_DIR = ensureDirectory(INSTALL_DIR.resolve("lib"));
    private static final Path APP_DIR = ensureDirectory(INSTALL_DIR.resolve("app"));
    private static final Path INSTALLED_JAR_FILE = ensureFile(APP_DIR.resolve("main.jar"));
    private static final Path INSTALLED_MODULES_FILE = ensureFile(LIB_DIR.resolve("modules"));
    private static final String JAR_NAME = INSTALLED_JAR_FILE.getFileName().toString();
    private static final String EXIT_ON_STARTED_VALUE = "!";
    private static final String EXIT_ON_STARTED = "-Dexit.on.started=" + EXIT_ON_STARTED_VALUE;
    private static final String NOT_EQUAL = Constants.OS == Windows ? "-ne" : "!=";

    private StartScript.Builder builder() {
        return StartScript.builder()
                          .mainJar(INSTALLED_JAR_FILE)
                          .installHomeDirectory(INSTALL_DIR)
                          .exitOnStartedValue(EXIT_ON_STARTED_VALUE);
    }

    private static String modulesTimeStampComparison() {
        return timeStampComparison("modules", INSTALLED_MODULES_FILE);
    }

    private static String jarTimeStampComparison() {
        return timeStampComparison("jar", INSTALLED_JAR_FILE);
    }

    private static String timeStampComparison(String name, Path file) {
        final String timestamp = Long.toString(lastModifiedSeconds(file));
        return "${" + name + "TimeStamp} " + NOT_EQUAL + " \"" + timestamp + "\"";
    }

    public static Matcher<String> containsString(String substring) {
        if (Constants.OS == Windows) {
            substring = substring.replace(Linux.escapedQuote(), Windows.escapedQuote());
        }
        return StringContains.containsString(substring);
    }

    @Test
    void testExitOnStarted() {
        String script = builder().build().toString();
        assertThat(script, containsString(EXIT_ON_STARTED));
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
        assertThat(script, containsString("defaultDebug=\"" + Configuration.Builder.DEFAULT_DEBUG + "\""));

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

        assertThat(script, containsString(EXIT_ON_STARTED));
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

        assertThat(script, containsString(EXIT_ON_STARTED));
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

        assertThat(script, containsString(EXIT_ON_STARTED));
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

        assertThat(script, containsString(EXIT_ON_STARTED));
    }

    @Test
    void testInstall() throws Exception {
        Path installedScript = BIN_DIR.resolve(Constants.OS.withScriptExtension("start"));
        Files.deleteIfExists(installedScript);
        StartScript script = builder().build();
        Path scriptFile = script.install();
        assertThat(Files.exists(scriptFile), is(true));
        assertExecutable(scriptFile);
        assertThat(scriptFile, is(installedScript));
        String onDisk = InputStreams.toString(new FileInputStream(scriptFile.toFile()));
        assertThat(onDisk, is(script.toString()));
    }

    private static void assertExecutable(Path file) throws IOException {
        if (Constants.OS.isPosix()) {
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
}
