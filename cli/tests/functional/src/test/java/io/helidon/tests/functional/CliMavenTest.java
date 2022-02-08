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
import com.oracle.bedrock.runtime.console.CapturingApplicationConsole;
import com.oracle.bedrock.runtime.options.Arguments;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.EnvironmentVariable;
import com.oracle.bedrock.runtime.options.WorkingDirectory;
import io.helidon.webclient.WebClient;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CliMavenTest {

    private static final LocalPlatform platform = LocalPlatform.get();
    private static final String ARCHETYPE_VERSION = helidonArchetypeVersion();
    private static String jarCliPath = cliJarFile();

    private static Path workDir;
    private static Path javaHome;
    private static Path mavenHome;

    enum MAVEN_VERSION {
        V3_1_1("3.1.1"), V3_2_5("3.2.5"), V3_8_1("3.8.1"), V3_8_2("3.8.2"), V3_8_4("3.8.4");

        private final String version;

        MAVEN_VERSION(String version) {
            this.version = version;
        }

        public static List<String> getVersions() {
            return List.of("3.1.1", "3.2.5", "3.8.1", "3.8.2", "3.8.4");
        }
    }

    @BeforeAll
    static void setUp() throws IOException {
        workDir = Files.createTempDirectory("generated");
        javaHome = Files.createTempDirectory("java");
        mavenHome = Files.createTempDirectory("maven");
        jarCliPath =  Paths.get(jarCliPath).normalize().toString();

        for (String version : MAVEN_VERSION.getVersions()) {
            TestUtils.downloadMavenDist(mavenHome, version);
        }
    }

    private static String helidonArchetypeVersion() {
        String version = System.getProperty("helidon.current.test.version");
        if (version != null) {
            return version;
        } else {
            throw new IllegalStateException("Helidon archetype version is not set");
        }
    }

    private static String cliJarFile() {
        String jarPath = System.getProperty("jar.cli.path");
        if (jarPath != null) {
            return jarPath;
        } else {
            throw new IllegalStateException("Cli jar path property is not set");
        }
    }

    @AfterEach
    void cleanUpGeneratedFiles() throws IOException {
        cleanUp(workDir);
    }

    @AfterAll
    static void cleanUp() throws IOException {
        cleanUp(javaHome);
        cleanUp(mavenHome);
    }

    static void cleanUp(Path directory) throws IOException {
        Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .filter(it -> !it.equals(directory))
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @ParameterizedTest
    @CsvSource({"V3_8_4", "V3_2_5"})
    public void testMavenCorrectBehavior(String version) {
        CliApplication app = Builder.builder()
                .mavenVersion(MAVEN_VERSION.valueOf(version))
                .build();
        app.runMavenCommand();
        app.stop();
        assertConsoleOutputContains(app.console(), "BUILD SUCCESS");
    }

    @Test
    public void testInitCorrectBehavior() {
        CliApplication app = Builder.builder()
                .mavenVersion(MAVEN_VERSION.V3_8_4)
                .batch()
                .verbose()
                .build();
        app.runInitCommand();
        app.stop();
        Path generatedProject = app.getGeneratedProjectLocation();
        assertConsoleOutputContains(app.console(), "Switch directory to " + generatedProject + " to use CLI");
        Assertions.assertTrue(generatedProject.resolve("pom.xml").toFile().exists());
    }

    @Test
    public void testWrongMavenVersion() {
        CliApplication app = Builder.builder()
                .mavenVersion(MAVEN_VERSION.V3_1_1)
                .build();
        app.runMavenCommand();
        app.stop();
        CapturingApplicationConsole console = app.console();
        assertConsoleOutputContains(console, "Requires Maven >= 3.2.5");
    }

    @Test
    public void testMissingValues() {
        List<String> mvnArgs = List.of(
                "archetype:generate",
                "-DinteractiveMode=false",
                "-DarchetypeGroupId=io.helidon.archetypes",
                "-DarchetypeArtifactId=helidon",
                "-DarchetypeVersion=" + ARCHETYPE_VERSION);

        CliApplication app = Builder.builder()
                .mavenVersion(MAVEN_VERSION.V3_8_4)
                .build();
        app.runMavenCommand(mvnArgs);
        app.stop();
        CapturingApplicationConsole console = app.console();

        assertConsoleOutputContains(console, "Property groupId is missing. Add -DgroupId=someValue");
        assertConsoleOutputContains(console, "Property artifactId is missing. Add -DartifactId=someValue");
        assertConsoleOutputContains(console, "Property package is missing. Add -Dpackage=someValue");
        assertConsoleOutputContains(console, "BUILD FAILURE");
    }

    @Test //Test issue https://github.com/oracle/helidon-build-tools/issues/499
    public void testIssue499() throws Exception {
        int port = platform.getAvailablePorts().next();
        List<String> cmdArgs = new LinkedList<>(List.of( "--app-jvm-args", "-Dserver.port=" + port));

        CliApplication generateApp = Builder.builder()
                .mavenVersion(MAVEN_VERSION.V3_8_2)
                .batch()
                .build();
        generateApp.runInitCommand();
        generateApp.stop();

        CliApplication app = Builder.builder()
                .mavenVersion(MAVEN_VERSION.V3_8_2)
                .port(port)
                .build();
        app.runDevCommand(cmdArgs);
        app.waitForApplication();

        Files.walk(workDir)
                .filter(p -> p.toString().endsWith("GreetService.java"))
                .findAny()
                .ifPresent(path -> {
                    try {
                        String content = Files.readString(path);
                        content = content.replaceAll("\"World\"", "\"Jhon\"");
                        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException ioException) {
                        Assertions.fail("Unable to modified GreetService file");
                    }
                });

        app.waitForApplication();

        WebClient client = WebClient.builder()
                .baseUri("http://localhost:" + port + "/greet")
                .build();
        client.get().request(String.class)
                .thenAccept(s -> Assertions.assertTrue(s.contains("Jhon")))
                .toCompletableFuture().get();

        app.stop();
    }

    @Test
    public void testVerbose() throws Exception {
        int port = platform.getAvailablePorts().next();
        List<String> cmdArgs = new LinkedList<>(List.of("--app-jvm-args", "-Dserver.port=" + port));

        CliApplication generateApp = Builder.builder()
                .mavenVersion(MAVEN_VERSION.V3_8_4)
                .batch()
                .build();
        generateApp.runInitCommand();
        generateApp.stop();

        CliApplication app = Builder.builder()
                .mavenVersion(MAVEN_VERSION.V3_8_4)
                .port(port)
                .verbose()
                .build();
        app.runDevCommand(cmdArgs);
        app.waitForApplication();
        app.stop();
        CapturingApplicationConsole console = app.console();

        if (console.getCapturedOutputLines()
                .stream()
                .noneMatch(line -> line.contains("Detecting the operating system and CPU architecture"))) {
            Assertions.fail("Verbose mode does not print system information");
        }
    }

    @Test
    public void testDebug() {
        CliApplication app = Builder.builder()
                .mavenVersion(MAVEN_VERSION.V3_8_4)
                .batch()
                .debug()
                .build();
        app.runInitCommand();
        app.stop();
        CapturingApplicationConsole console = app.console();
        console.getCapturedOutputLines().forEach(System.out::println);
        if (console.getCapturedOutputLines()
                .stream()
                .noneMatch(line -> line.contains("Found maven executable"))) {
            Assertions.fail("Debug mode does not print required information");
        }
    }

    @Test
    public void testCliMavenPluginJansiIssue() throws Exception {
        int port = platform.getAvailablePorts().next();

        CliApplication generateApp = Builder.builder()
                .mavenVersion(MAVEN_VERSION.V3_8_1)
                .batch()
                .build();
        generateApp.runInitCommand();
        generateApp.stop();

        setCustomServerPortInPom(generateApp.getGeneratedProjectLocation().resolve("pom.xml"), port);

        CliApplication app = Builder.builder()
                .mavenVersion(MAVEN_VERSION.V3_8_1)
                .port(port)
                .devProcess()
                .workDirectory(workDir.resolve("artifactid"))
                .build();
        app.runMavenCommand(List.of("io.helidon.build-tools:helidon-cli-maven-plugin:3.0.0-M2:dev"));
        app.waitForApplication();
        app.stop();
        CapturingApplicationConsole console = app.console();

        if (console.getCapturedOutputLines()
                .stream()
                .noneMatch(line -> line.contains("BUILD SUCCESS"))) {
            Assertions.fail("Build is Failing when using maven 3.8.+ version");
        }
    }

    @Test
    public void testCliMavenPluginBackwardCompatibility() throws Exception {
        int port = platform.getAvailablePorts().next();
        CliApplication generateApp = Builder.builder()
                .mavenVersion(MAVEN_VERSION.V3_2_5)
                .batch()
                .build();
        generateApp.runInitCommand();
        generateApp.stop();

        setCustomServerPortInPom(generateApp.getGeneratedProjectLocation().resolve("pom.xml"), port);

        CliApplication app = Builder.builder()
                .mavenVersion(MAVEN_VERSION.V3_2_5)
                .devProcess()
                .port(port)
                .workDirectory(workDir.resolve("artifactid"))
                .build();
        app.runMavenCommand(List.of("io.helidon.build-tools:helidon-cli-maven-plugin:2.1.0:dev"));
        app.waitForApplication();
        app.stop();
        CapturingApplicationConsole console = app.console();

        if (console.getCapturedOutputLines()
                .stream()
                .noneMatch(line -> line.contains("BUILD SUCCESS"))) {
            Assertions.fail("Build is failing when using new CLI with old Helidon version");
        }
    }

    //@Test
    public void testCliPluginCompatibility() throws Exception {
        CliApplication generateApp = Builder.builder()
                .mavenVersion(MAVEN_VERSION.V3_8_4)
                .batch()
                .build();
        generateApp.runInitCommand();
        generateApp.stop();

        CliApplication app = Builder.builder()
                .mavenVersion(MAVEN_VERSION.V3_8_4)
                .port(8080)
                .devProcess()
                .artifactId("artifactid")
                .workDirectory(workDir.resolve("artifactid"))
                .build();
        app.runMavenCommand(List.of("helidon:dev", "-Dversion.plugin.helidon=2.0.2"));
        app.waitForApplication();
        app.stop();
        CapturingApplicationConsole console = app.console();

        if (console.getCapturedOutputLines()
                .stream()
                .noneMatch(line -> line.contains("BUILD SUCCESS"))) {
            Assertions.fail("Build is Failing  when using maven 3.8.+ version");
        }
    }

    private void setCustomServerPortInPom(Path pom, int port) throws Exception {
        Plugin plugin = new Plugin();
        plugin.setGroupId("io.helidon.build-tools");
        plugin.setArtifactId("helidon-cli-maven-plugin");
        Xpp3Dom config = new Xpp3Dom("configuration");
        Xpp3Dom appArgs = new Xpp3Dom("appJvmArgs");
        appArgs.setValue("-Dserver.port=" + port);
        config.addChild(appArgs);
        plugin.setConfiguration(config);

        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        Model model = mavenReader.read(new FileReader(pom.toFile()));
        model.getBuild().getPlugins().add(plugin);
        MavenXpp3Writer mavenWriter = new MavenXpp3Writer();
        mavenWriter.write(new FileWriter(pom.toFile()), model);
    }

    private void assertConsoleOutputContains(CapturingApplicationConsole console, String expected) {
        Optional<String> captured = console.getCapturedOutputLines().stream()
                .filter(l -> l.contains(expected))
                .findAny();
        Assertions.assertTrue(captured.isPresent());
    }

    static class CliApplication {

        private Application app;

        private final String flavor;
        private final String archetype;
        private final MAVEN_VERSION mavenVersion;
        private final String mavenExecFile;
        private final String m2Home;
        private List<String> cmdArgs = new LinkedList<>();
        private final CapturingApplicationConsole console;
        private final int port;
        private final String groupId;
        private final String artifactId;
        private final String build;
        private final String version;
        private final String packageName;
        private final boolean isBatch;
        private final boolean isDebug;
        private final boolean isVerbose;
        private boolean isDevProcess;
        private final Path workDirectory;

        private final String DEFAULT_FLAVOR = "se";
        private final String DEFAULT_BASE = "bare";
        private final String DEFAULT_GROUPID = "groupid";
        private final String DEFAULT_ARTIFACTID = "artifactid";
        private final String DEFAULT_BUILD = "maven";
        private final String DEFAULT_VERSION = "2.4.1";
        private final String DEFAULT_PACKAGE_NAME = "my.custom.pack.name";

        private CliApplication(Builder builder) {
            this.flavor = builder.flavor == null ? DEFAULT_FLAVOR : builder.flavor;
            this.archetype = builder.archetype == null ? DEFAULT_BASE : builder.archetype;
            this.mavenVersion = Objects.requireNonNull(builder.mavenVersion, "Maven version is null");
            this.port = builder.port;
            this.mavenExecFile = Path.of(mavenHome.toString(), "apache-maven-" + mavenVersion.version, "bin", "mvn").toString();
            this.groupId = builder.groupId == null ? DEFAULT_GROUPID : builder.groupId;
            this.artifactId = builder.artifactId == null ? DEFAULT_ARTIFACTID : builder.artifactId;
            this.build = builder.build == null ? DEFAULT_BUILD : builder.build;
            this.version = builder.version == null ? DEFAULT_VERSION : builder.version;
            this.packageName = builder.packageName == null ? DEFAULT_PACKAGE_NAME : builder.packageName;
            this.workDirectory = builder.workDirectory == null ? workDir : builder.workDirectory;
            this.isBatch = builder.isBatch;
            this.isDebug = builder.isDebug;
            this.isVerbose = builder.isVerbose;
            this.isDevProcess = builder.isDevProcess;
            this.m2Home = mavenHome.resolve("apache-maven-" + mavenVersion.version).toString();
            this.console = new CapturingApplicationConsole();
        }

        private void parseMavenArgs() {
            cmdArgs = List.of(
                    "-U", //update snapshot
                    "archetype:generate",
                    "-DinteractiveMode=false",
                    "-DarchetypeGroupId=io.helidon.archetypes",
                    "-DarchetypeArtifactId=helidon",
                    "-DarchetypeVersion=" + ARCHETYPE_VERSION,
                    "-DgroupId=" + groupId,
                    "-DartifactId=" + artifactId,
                    "-Dpackage=" + packageName,
                    "-Dflavor=" + flavor,
                    "-Dbase=" + archetype,
                    "-Dname=" + artifactId);
        }

        private void parseJavaArgs() {
            applyMode();
            cmdArgs.add("--build");
            cmdArgs.add(build);
            cmdArgs.add("--version");
            cmdArgs.add(version);
            cmdArgs.add("--flavor");
            cmdArgs.add(flavor);
            cmdArgs.add("--archetype");
            cmdArgs.add(archetype);
            cmdArgs.add("--groupid");
            cmdArgs.add(groupId);
            cmdArgs.add("--artifactid");
            cmdArgs.add(artifactId);
            cmdArgs.add("--package");
            cmdArgs.add(packageName);
            cmdArgs.add("--name");
            cmdArgs.add(artifactId);
        }

        private void applyMode() {
            if (isDebug) {
                cmdArgs.add("--debug");
            }
            if (isVerbose) {
                cmdArgs.add("--verbose");
            }
            if (isBatch) {
                cmdArgs.add("--batch");
            }
        }

        void runInitCommand() {
            parseJavaArgs();
            List<String> args = new LinkedList<>(List.of("-jar", jarCliPath, "init"));
            args.addAll(cmdArgs);

            app = platform.launch(
                    "java",
                    Arguments.of(args),
                    Console.of(console),
                    WorkingDirectory.at(workDirectory));
        }

        void runDevCommand(List<String> extraArgs) {
            applyMode();
            List<String> args = new LinkedList<>(List.of("-jar", jarCliPath, "dev"));
            args.addAll(cmdArgs);
            args.addAll(extraArgs);

            app = platform.launch(
                    "java",
                    Arguments.of(args),
                    Console.of(console),
                    WorkingDirectory.at(workDirectory.resolve(artifactId)));

            isDevProcess = true;
        }

        void runMavenCommand(List<String> args) {
            parseMavenArgs();
            if (args == null) {
                args = cmdArgs;
            }

            app = platform.launch(
                    mavenExecFile,
                    Arguments.of(args),
                    EnvironmentVariable.of("M2_HOME", m2Home),
                    EnvironmentVariable.of("MAVEN_HOME", m2Home),
                    Console.of(console),
                    WorkingDirectory.at(workDirectory));
        }

        void runMavenCommand() {
            runMavenCommand(null);
        }

        void waitForApplication() throws Exception {
            long timeout = 30 * 1000;
            long now = System.currentTimeMillis();
            URL url = new URL("http://localhost:" + port + "/greet");

            HttpURLConnection conn = null;
            int responseCode;
            do {
                Thread.sleep(1000);
                if ((System.currentTimeMillis() - now) > timeout) {
                    stop();
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

        CapturingApplicationConsole console() {
            return console;
        }

        Path getGeneratedProjectLocation() {
            return workDir.resolve(artifactId);
        }

        void stop() {
            if (!isDevProcess) {
                app.waitFor();
            }
            app.close();
            console.getCapturedOutputLines().forEach(System.out::println);
            console.getCapturedErrorLines().forEach(System.out::println);
        }
    }

    static class Builder {

        private MAVEN_VERSION mavenVersion;
        private Path workDirectory;
        private int port;
        private String flavor;
        private String archetype;
        private String groupId;
        private String artifactId;
        private String build;
        private String version;
        private String packageName;
        private boolean isBatch = false;
        private boolean isDebug = false;
        private boolean isVerbose = false;
        private boolean isDevProcess = false;

        static Builder builder() {
            return new Builder();
        }

        public Builder flavor(String flavor) {
            this.flavor = flavor;
            return this;
        }

        public Builder archetype(String archetype) {
            this.archetype = archetype;
            return this;
        }

        public Builder mavenVersion(MAVEN_VERSION mavenVersion) {
            this.mavenVersion = mavenVersion;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder build(String build) {
            this.build = build;
            return this;
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder workDirectory(Path workDirectory) {
            this.workDirectory = workDirectory;
            return this;
        }

        public Builder batch() {
            this.isBatch = true;
            return this;
        }

        public Builder debug() {
            this.isDebug = true;
            return this;
        }

        public Builder verbose() {
            this.isVerbose = true;
            return this;
        }

        public Builder devProcess() {
            this.isDevProcess = true;
            return this;
        }

        public CliApplication build() {
            return new CliApplication(this);
        }

    }

}
