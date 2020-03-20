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

package io.helidon.build.dev.clidemo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import io.helidon.build.util.Constants;
import io.helidon.build.util.HelidonVariant;
import io.helidon.build.util.Log;
import io.helidon.build.util.ProcessMonitor;
import io.helidon.build.util.SimpleQuickstartGenerator;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import static io.helidon.build.dev.clidemo.CliConfig.FEATURE_PREFIX;
import static io.helidon.build.dev.clidemo.CliConfig.HELIDON_VARIANT;
import static io.helidon.build.dev.clidemo.CliConfig.PROJECT_DIRECTORY;

/**
 * Class HelidonCli.
 */
public class HelidonCliDemo {
    private static final String USAGE = "\nUsage: helidon <command>\n" +
            "\ncommands:\n" +
            "    init [se | mp] [--version <version>]\n" +
            "    dev [--clean]\n" +
            "    feature (list | add <name>)\n";

    private static final String DOT_HELIDON = ".helidon";
    private static final String HELIDON_PROPERTIES = "helidon.properties";
    private static final String DEFAULT_VERSION = "1.4.1";
    private static final Path CWD = Path.of(".");
    private static final CliConfig cliConfig = new CliConfig(new File(DOT_HELIDON));

    private static final String MAVEN_EXEC = Constants.OS.mavenExec();
    private static final String MAVEN_PLUGIN_GOAL = "io.helidon.build-tools:helidon-maven-plugin:1.1.2-SNAPSHOT:dev";
    private static final String JAVA_HOME = Constants.javaHome();
    private static final String JAVA_HOME_BIN = JAVA_HOME + File.separator + "bin";
    private static final long SECONDS_PER_YEAR = 365 * 24 * 60 * 60;
    private static final boolean FORK_MAVEN = "true".equals(System.getProperty("dev.fork"));

    private static long pid;
    private static ProcessMonitor processMonitor;

    public static void main(String[] args) throws Exception {
        String command = null;
        HelidonVariant variant = HelidonVariant.SE;
        String version = DEFAULT_VERSION;
        boolean clean = false;
        String featureName = null;

        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.startsWith("--")) {
                if ("init".equals(command) && arg.equals("--version")) {
                    version = argAt(++i, args);
                } else if ("dev".equals(command) && arg.equalsIgnoreCase("--clean")) {
                    clean = true;
                } else if (arg.equals("--help")) {
                    displayHelpAndExit(0);
                } else {
                    displayHelpAndExit(1);
                }
            } else if (command == null) {
                command = arg;
            } else if (command.equals("init")) {
                variant = HelidonVariant.parse(argAt(i, args));
            } else if (command.equals("feature")) {
                if (arg.equals("list")) {
                    command = "feature-list";
                } else if (arg.equals("add")) {
                    command = "feature-add";
                    featureName = argAt(++i, args);
                } else {
                    displayHelpAndExit(1);
                }
            } else {
                displayHelpAndExit(1);
            }
        }
        if (command == null) {
            displayHelpAndExit(1);
        } else if (command.equals("init")) {
            helidonInit(variant, version);
        } else if (command.equals("dev")) {
            helidonDev(clean);
        } else if (command.equals("feature-list")) {
            helidonFeatureList();
        } else if (command.equals("feature-add")) {
            helidonFeatureAdd(featureName);
        } else {
            displayHelpAndExit(1);
        }
    }

    private static String argAt(int index, String[] args) {
        if (index < args.length) {
            return args[index];
        } else {
            System.err.println(args[index - 1] + ": missing required argument");
            displayHelpAndExit(1);
            return "";
        }
    }

    private static void helidonInit(HelidonVariant variant, String version) throws Exception {
        Optional<Path> projectDir = cliConfig.projectDir();
        if (projectDir.isEmpty()) {
            // Generate project
            Path dir = null;
            try {
                dir = SimpleQuickstartGenerator.generator()
                        .parentDirectory(CWD)
                        .helidonVariant(variant)
                        .helidonVersion(version)
                        .generate();
            } catch (IllegalStateException e) {
                System.err.println(e.getMessage());
                System.exit(2);
            }
            Objects.requireNonNull(dir);

            // Copy properties files into project directory
            File sourceFile = new File(Objects.requireNonNull(
                    HelidonCliDemo.class.getClassLoader().getResource(HELIDON_PROPERTIES)).getFile());
            File destFile = Path.of(dir.toString(), DOT_HELIDON).toFile();
            try (FileReader fr = new FileReader(sourceFile)) {
                Properties sourceProps = new Properties();
                sourceProps.load(fr);
                try (FileWriter fw = new FileWriter(destFile)) {
                    Properties destProps = new Properties();
                    destProps.setProperty(PROJECT_DIRECTORY, ".");
                    destProps.setProperty(HELIDON_VARIANT, variant.toString());
                    sourceProps.forEach((key, value) -> {
                        String propName = (String) key;
                        if (propName.startsWith(FEATURE_PREFIX)) {      // Applies to both SE or MP
                            destProps.setProperty(propName, (String) value);
                        } else if (propName.startsWith(variant.toString())) {       // Project's variant
                            destProps.setProperty(
                                propName.substring(variant.toString().length() + 1),
                                (String) value);
                        }
                    });
                    destProps.store(fw, "Helidon CLI config");
                }
            }

            System.out.println("Switch directory to ./" + dir.getFileName() + " to use CLI");
        } else {
            System.err.println("Error: A project already exists in this directory");
            System.exit(2);
        }
    }

    private static void helidonDev(boolean clean) {
        // Execute Helidon maven plugin to enter dev loop
        String cleanProp = "-Ddev.clean=" + clean;
        String forkProp = "-Ddev.fork=" + FORK_MAVEN;
        ProcessBuilder processBuilder = new ProcessBuilder()
                .directory(CWD.toFile())
                .command(MAVEN_EXEC, MAVEN_PLUGIN_GOAL, cleanProp, forkProp);
        Map<String, String> env = processBuilder.environment();
        String path = JAVA_HOME_BIN + File.pathSeparatorChar + env.get("PATH");
        env.put("PATH", path);
        env.put("JAVA_HOME", JAVA_HOME);
        try {
            // Fork process and wait for its completion
            processMonitor = ProcessMonitor.builder()
                    .processBuilder(processBuilder)
                    .stdOut(System.out::println)
                    .stdErr(System.err::println)
                    .capture(false)
                    .build()
                    .start();
            pid = processMonitor.toHandle().pid();
            Log.info("Process with PID %d is starting", pid);
            processMonitor.waitForCompletion(SECONDS_PER_YEAR, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void helidonFeatureList() {
        ensureProject();
        cliConfig.listFeatures().forEach(System.out::println);
    }

    private static void helidonFeatureAdd(String featureName) {
        Path projectDir = ensureProject();

        // Get list of deps for this feature -- must be non-empty
        List<CliConfig.Dependency> featureDeps = cliConfig.featureDeps(featureName);
        if (featureDeps.isEmpty()) {
            System.err.println("Error: Feature '" + featureName + "' does not exist");
            System.exit(4);
        }

        try {
            Model model;
            File pomFile = Path.of(projectDir.toString(), "pom.xml").toFile();

            // Read pom and add new deps
            try (FileReader fr = new FileReader(pomFile)) {
                MavenXpp3Reader mvnReader = new MavenXpp3Reader();
                model = mvnReader.read(fr);
                List<Dependency> existingDeps = model.getDependencies();
                featureDeps.forEach(fd -> {
                    // Check if dependency already there
                    Optional<Dependency> found = existingDeps.stream().filter(d ->
                            d.getGroupId().equals(fd.groupId())
                            && d.getArtifactId().equals(fd.artifactId())
                            && Objects.equals(d.getVersion(), fd.version())).findFirst();

                    // Now add dependency if necessary
                    if (found.isEmpty()) {
                        Dependency newDep = new Dependency();
                        newDep.setGroupId(fd.groupId());
                        newDep.setArtifactId(fd.artifactId());
                        newDep.setVersion(fd.version());                        model.addDependency(newDep);
                        System.out.println("Adding '" + fd + "' to the project's pom");
                    } else {
                        System.out.println("Dependency '" + fd + "' already in project's pom");
                    }
                });
            }

            // Write pom with new deps
            try (FileWriter fw = new FileWriter(pomFile)) {
                MavenXpp3Writer mvnWriter = new MavenXpp3Writer();
                mvnWriter.write(fw, model);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Path ensureProject() {
        Optional<Path> projectDir = cliConfig.projectDir();
        if (projectDir.isEmpty()) {
            System.err.println("Error: Cannot find a project, use init command first");
            System.exit(3);
        }
        return projectDir.get();
    }

    private static void displayHelpAndExit(int status) {
        System.out.println(USAGE);
        System.exit(status);
    }
}
