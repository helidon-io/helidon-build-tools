/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.tests.functional;

import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.console.PipedApplicationConsole;
import com.oracle.bedrock.runtime.options.Arguments;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.WorkingDirectory;
import io.helidon.build.common.FileUtils;
import io.helidon.common.http.Http;
import io.helidon.webclient.WebClient;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class CliFunctionalTest {

    private static String jarCliPath = System.getProperty("jar.cli.path", "please-set-cli.jar.path");
    private static final LocalPlatform localPlatform = LocalPlatform.get();
    private static final String CUSTOM_GROUP_ID = "mygroupid";
    private static final String CUSTOM_ARTIFACT_ID = "myartifactid";
    private static final String CUSTOM_PROJECT = "myproject";
    private static final String CUSTOM_PACKAGE_NAME = "custom.pack.name";
    private static final String HELIDON_VERSION = "2.4.0";
    private static Path workDir;

    @BeforeAll
    static void setup() throws IOException {
        jarCliPath = Paths.get(jarCliPath).normalize().toString();
        workDir = Files.createTempDirectory("generated");
    }

    @AfterEach
    public void cleanUp() throws IOException {
        Files.walk(workDir)
                .sorted(Comparator.reverseOrder())
                .filter(it -> !it.equals(workDir))
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void batchTest(String flavor, String archetype) throws Exception {
        runBatchTest(flavor, HELIDON_VERSION, archetype, null, null, null, null, true);
    }

    @ParameterizedTest
    @CsvSource({"1,1", "1,2", "1,3", "2,1", "2,2", "2,3"})
    void interactiveTest(String flavor, String archetype) throws Exception {
        runInteractiveTest(flavor, null, archetype, null, null, null, null, true);
    }

    @ParameterizedTest
    @CsvSource({
            "se,bare,2.3.0",
            "se,database,2.3.0",
            "se,quickstart,2.3.0",
            "mp,bare,2.3.0",
            "mp,database,2.3.0",
            "mp,quickstart,2.3.0"})
    void batchVersionTest(String flavor, String archetype, String version) throws Exception {
        runBatchTest(flavor, version, archetype, null, null, null, null, false);
        checkIntoPom("2.3.0");
    }

    @ParameterizedTest
    @CsvSource({"1,1,2.3.0", "1,2,2.3.0", "1,3,2.3.0", "2,1,2.3.0", "2,2,2.3.0", "2,3,2.3.0"})
    void interactiveVersionTest(String flavor, String archetype, String version) throws Exception {
        runInteractiveTest(flavor, null, null, null, version, null, archetype, false);
        checkIntoPom("2.3.0");
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void batchAllTest(String flavor, String archetype) throws Exception {
        runBatchTest(flavor, "2.3.0", archetype, CUSTOM_GROUP_ID, CUSTOM_ARTIFACT_ID, CUSTOM_PACKAGE_NAME, CUSTOM_PROJECT, false);
        checkIntoPom("2.3.0");
        checkIntoPom(CUSTOM_GROUP_ID);
        checkPackageName();
    }

    @ParameterizedTest
    @CsvSource({"1,1", "1,2", "1,3", "2,1", "2,2", "2,3"})
    void interactiveAllTest(String flavor, String archetype) throws Exception {
        runInteractiveTest(flavor, "2.3.0", archetype, CUSTOM_GROUP_ID, CUSTOM_ARTIFACT_ID, CUSTOM_PACKAGE_NAME, CUSTOM_PROJECT, false);
        checkIntoPom("2.3.0");
        checkIntoPom(CUSTOM_GROUP_ID);
        checkPackageName();
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void customPackageNameBatchTest(String flavor, String archetype) throws Exception {
        runBatchTest(flavor, HELIDON_VERSION, archetype, null, null, CUSTOM_PACKAGE_NAME, null, false);
        checkPackageName();
    }

    @ParameterizedTest
    @CsvSource({"1,1", "1,2", "1,3", "2,1", "2,2", "2,3"})
    void customPackageNameInteractiveTest(String flavor, String archetype) throws Exception {
        runInteractiveTest(flavor, null, archetype, null, null, CUSTOM_PACKAGE_NAME, null, false);
        checkPackageName();
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void customGroupIdBatchTest(String flavor, String archetype) throws Exception {
        runBatchTest(flavor, HELIDON_VERSION, archetype, CUSTOM_GROUP_ID, null, null, null, false);
        checkIntoPom(CUSTOM_GROUP_ID);
    }

    @ParameterizedTest
    @CsvSource({"1,1", "1,2", "1,3", "2,1", "2,2", "2,3"})
    void customGroupIdInteractiveTest(String flavor, String archetype) throws Exception {
        runInteractiveTest(flavor, null, archetype, CUSTOM_GROUP_ID, null, null, null, false);
        checkIntoPom(CUSTOM_GROUP_ID);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void customArtifactIdBatchTest(String flavor, String archetype) throws Exception {
        runBatchTest(flavor, HELIDON_VERSION, archetype, null, CUSTOM_ARTIFACT_ID, null, null, false);
        checkIntoPom(CUSTOM_ARTIFACT_ID);
    }

    @ParameterizedTest
    @CsvSource({"1,1", "1,2", "1,3", "2,1", "2,2", "2,3"})
    void customArtifactIdInteractiveTest(String flavor, String archetype) throws Exception {
        runInteractiveTest(flavor, null, archetype, CUSTOM_ARTIFACT_ID, null, null, null, false);
        checkIntoPom(CUSTOM_ARTIFACT_ID);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void customProjectNameBatchTest(String flavor, String archetype) throws Exception {
        runBatchTest(flavor, HELIDON_VERSION, archetype, null, null, null, CUSTOM_PROJECT, false);
        Assertions.assertTrue(workDir.resolve(CUSTOM_PROJECT).toFile().exists());
    }

    @ParameterizedTest
    @CsvSource({"1,1", "1,2", "1,3", "2,1", "2,2", "2,3"})
    void customProjectNameInteractiveTest(String flavor, String archetype) throws Exception {
        runInteractiveTest(flavor, null, archetype, null, null, null, CUSTOM_PROJECT, false);
        Assertions.assertTrue(workDir.resolve(CUSTOM_PROJECT).toFile().exists());
    }

    private void checkIntoPom(String expected) throws IOException {
        Path pom = Path.of(workDir.toString(), CUSTOM_PROJECT, "pom.xml");
        Assertions.assertTrue(Files.readString(pom).contains(expected));
    }

    private void checkPackageName() {
        Path packageInfo = Path.of(workDir.toString(), CUSTOM_PROJECT, "src", "main", "java", "custom", "pack", "name", "package-info.java");
        Assertions.assertTrue(packageInfo.toFile().exists());
    }

    private void runBatchTest(String flavor,
                              String version,
                              String archetype,
                              String groupId,
                              String artifactId,
                              String packageName,
                              String name,
                              boolean startApp) throws Exception {

        PipedApplicationConsole console = new PipedApplicationConsole();
        ArgList<String> args = new ArgList<>();
        args.add("--batch");
        args.add("--build", "maven");
        args.add("--version", version);
        args.add("--flavor", flavor);
        args.add("--archetype", archetype);
        args.add("--groupid", groupId);
        args.add("--artifactid", artifactId);
        args.add("--package", packageName);
        args.add("--name", name);
        Arguments arguments = toArguments(jarCliPath, buildArgs(args), -1);
        Application app = localPlatform.launch("java", arguments, Console.of(console));
        CliApplication cli = new CliApplication(app, console);
        waitForGeneratedProject();

        if (startApp) {
            testGeneratedProject();
        }

        cli.close();
    }

    private void runInteractiveTest(String flavor,
                                    String version,
                                    String archetype,
                                    String groupId,
                                    String artifactId,
                                    String packageName,
                                    String name,
                                    boolean startApp) throws Exception {

        PipedApplicationConsole console = new PipedApplicationConsole();
        Arguments arguments = toArguments(jarCliPath, buildArgs(null), -1);
        Application app = localPlatform.launch("java", arguments, Console.of(console));
        CliApplication cli = new CliApplication(app, console);
        TimeUnit.SECONDS.sleep(3);

        cli.writeInput(version == null ? HELIDON_VERSION : version);
        cli.writeInput(flavor);
        cli.writeInput(archetype);
        cli.writeInput(name == null ? System.lineSeparator() : name);
        cli.writeInput(groupId == null ? System.lineSeparator() : groupId);
        cli.writeInput(artifactId == null ? System.lineSeparator() : artifactId);
        cli.writeInput(System.lineSeparator());
        cli.writeInput(packageName == null ? System.lineSeparator() : packageName);
        cli.writeInput("n");
        waitForGeneratedProject();

        if (startApp) {
            testGeneratedProject();
        }
        cli.close();
    }

    private void testGeneratedProject() throws Exception {
        int port = localPlatform.getAvailablePorts().next();
        Arguments args = toArguments(jarCliPath, new ArrayList<>(List.of("dev")), port);
        Application app = localPlatform.launch("java", args, WorkingDirectory.at(workDir.resolve(CUSTOM_PROJECT)));
        DevApplication dev = new DevApplication(app, port);
        dev.waitForApplication();

        WebClient webClient = WebClient.builder()
                .baseUri(dev.getBaseUrl())
                .build();

        webClient.get()
                .path("/health")
                .request()
                .thenAccept(it -> MatcherAssert.assertThat("HTTP response", it.status(), CoreMatchers.is(Http.Status.OK_200)))
                .toCompletableFuture()
                .get();

        dev.close();
    }

    private void waitForGeneratedProject() throws InterruptedException {
        long timeout = 10 * 1000;
        long now = System.currentTimeMillis();

        while (!Path.of(workDir.toString(), CUSTOM_PROJECT, "pom.xml").toFile().exists()) {
            TimeUnit.MILLISECONDS.sleep(500);

            if ((System.currentTimeMillis() - now) > timeout) {
                Assertions.fail("Generated project is not created");
            }
        }
    }

    private Arguments toArguments(String appJarPath, List<String> javaArgs, int port) {
        List<String> args = new LinkedList<>();
        if (port != -1) {
            javaArgs.add("--app-jvm-args");
            javaArgs.add("-Dserver.port=" + port);
        }
        args.add("-jar");
        args.add(appJarPath);
        args.addAll(javaArgs);
        return Arguments.of(args);
    }

    private List<String> buildArgs(List<String> additionalArgs) {
        List<String> args = new LinkedList<>(List.of(
                "init",
                "--project",
                workDir.resolve(CUSTOM_PROJECT).toString()));
        if (additionalArgs != null) {
            args.addAll(additionalArgs);
        }
        return args;
    }

    static class ArgList<E> extends LinkedList<E> {

        public void add(E key, E value) {
            if (value == null) {
                return;
            }
            this.add(key);
            this.add(value);
        }
    }

    static class CliApplication  {

        Application application;
        PipedApplicationConsole console;

        CliApplication(Application application, PipedApplicationConsole console) {
            this.application = application;
            this.console = console;
        }

        void writeInput(Object obj) throws Exception {
            this.console.getInputWriter().println(obj);
            this.console.getInputWriter().flush();
            TimeUnit.MILLISECONDS.sleep(200);
        }

        void close() throws InterruptedException {
            application.close();
            while (application.isOperational()) {
                TimeUnit.MILLISECONDS.sleep(500);
            }
        }

    }

    static class DevApplication {
        Application application;
        int port;

        DevApplication(Application application, int port) {
            this.application = application;
            this.port = port;
        }

        void waitForApplication() throws Exception {
            long timeout = 25 * 1000;
            long now = System.currentTimeMillis();
            URL url = new URL("http://localhost:" + port + "/health");

            HttpURLConnection conn = null;
            int responseCode;
            do {
                Thread.sleep(500);
                if ((System.currentTimeMillis() - now) > timeout) {
                    Assertions.fail("Application failed to start");
                }
                try {
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(500);
                    responseCode = conn.getResponseCode();
                } catch (Exception ex) {
                    responseCode = -1;
                }
                if (conn != null) {
                    conn.disconnect();
                }
            } while (responseCode != 200);
        }

        URL getBaseUrl() throws MalformedURLException {
            return new URL("http://localhost:" + this.port);
        }

        void close() throws InterruptedException {
            application.close();
            while (application.isOperational()) {
                TimeUnit.MILLISECONDS.sleep(500);
            }
        }
    }

}
