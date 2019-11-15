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

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;

import io.helidon.linker.util.FileUtils;
import io.helidon.linker.util.JavaRuntime;
import io.helidon.linker.util.Log;

import static io.helidon.linker.Application.APP_DIR;
import static io.helidon.linker.util.FileUtils.fromWorking;

/**
 * Create a custom runtime image by finding the Java modules required of a Helidon application and linking them via jlink,
 * then adding the jars, a start script and, optionally, a CDS archive. Adds Jandex indices as needed.
 */
public class Linker {
    private static final String JLINK_TOOL_NAME = "jlink";
    private static final String JLINK_DEBUG_PROPERTY = JLINK_TOOL_NAME + ".debug";
    private static final float BYTES_PER_MEGABYTE = 1024F * 1024F;
    private static final String EOL = System.getProperty("line.separator");
    private static final String INDENT = "    ";
    private final ToolProvider jlink;
    private final List<String> jlinkArgs;
    private Configuration config;
    private long startTime;
    private Application application;
    private Set<String> javaDependencies;
    private JavaRuntime jri;
    private Path jriMainJar;
    private StartScript startScript;

    /**
     * Main entry point.
     *
     * @param args Command line arguments.
     * @throws Exception If an error occurs.
     * @see Configuration.Builder#commandLine(String...)
     */
    public static void main(String... args) throws Exception {
        linker(Configuration.builder()
                            .commandLine(args)
                            .build())
            .link();
    }

    /**
     * Returns a new linker with the given configuration.
     *
     * @param config The configuration.
     * @return The linker.
     */
    public static Linker linker(Configuration config) {
        return new Linker(config);
    }

    private Linker(Configuration config) {
        this.jlink = ToolProvider.findFirst(JLINK_TOOL_NAME).orElseThrow();
        this.jlinkArgs = new ArrayList<>();
        this.config = config;
        if (config.verbose()) {
            System.setProperty(JLINK_DEBUG_PROPERTY, "true");
        }
    }

    /**
     * Create the JRI.
     *
     * @return The JRI directory.
     */
    public Path link() {
        begin();
        buildApplication();
        collectJavaDependencies();
        buildJlinkArguments();
        buildJri();
        installJars();
        installCdsArchive();
        installStartScript();
        reportSizes();
        end();
        return config.jriDirectory();
    }

    /**
     * Returns the configuration.
     *
     * @return The configuration.
     */
    public Configuration config() {
        return config;
    }

    private void begin() {
        Log.info("Creating Java Runtime Image from %s", config.mainJar().getFileName());
        this.startTime = System.currentTimeMillis();
    }

    private void buildApplication() {
        Log.info("Loading application jars");
        this.application = new Application(config.mainJar());
    }

    private void collectJavaDependencies() {
        Log.info("Collecting Java module dependencies");
        this.javaDependencies = application.javaDependencies(config.jdk());
        Log.info("Found %d Java module dependencies: %s", javaDependencies.size(), String.join(", ", javaDependencies));
    }

    private void buildJlinkArguments() {

        // Tell jlink which jdk modules to include

        addArgument("--add-modules", String.join(",", javaDependencies));

        // Tell jlink the directory in which to create and write the JRI

        addArgument("--output", config.jriDirectory());

        // Tell jlink to strip out unnecessary stuff

        if (config.stripDebug()) {
            addArgument("--strip-debug");
        }
        addArgument("--no-header-files");
        addArgument("--no-man-pages");
        addArgument("--compress", "2");
    }

    private void buildJri() {
        Log.info("Creating base image: %s", jriDirectory());
        final int result = jlink.run(System.out, System.err, jlinkArgs.toArray(new String[0]));
        if (result != 0) {
            throw new Error("JRI creation failed.");
        }
        jri = JavaRuntime.jri(config.jriDirectory(), config.jdk().version());
    }

    private void installJars() {
        Log.info("Installing %d application jars in %s", application.size(), jriDirectory().resolve(APP_DIR));
        this.jriMainJar = application.install(jri);
    }

    private void installCdsArchive() {
        if (config.cds()) {
            try {
                ClassDataSharing.builder()
                                .jri(jri.path())
                                .applicationJar(jriMainJar)
                                .jvmOptions(config.defaultJvmOptions())
                                .args((config.defaultArgs()))
                                .archiveFile(application.archivePath())
                                .logOutput(config.verbose())
                                .build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void installStartScript() {
        try {
            final Path installDir = jriDirectory().resolve("bin");
            startScript = StartScript.builder()
                                     .installDirectory(installDir)
                                     .defaultJvmOptions(config.defaultJvmOptions())
                                     .defaultDebugOptions(config.defaultDebugOptions())
                                     .mainJar(jriMainJar)
                                     .defaultArgs(config.defaultArgs())
                                     .cdsInstalled(config.cds())
                                     .build();

            Log.info("Installing start script in %s", installDir);
            startScript.install();

            final Path scriptFile = fromWorking(startScript.scriptFile());
            final String description = String.format("Executing %s --help", scriptFile);
            final List<String> output = startScript.execute(description, "--help");
            Log.info(INDENT + String.join(EOL + INDENT, output));
        } catch (IllegalStateException e) {
            final boolean cds = config.cds();
            final Path root = fromWorking(jri.path());
            final String jvm = cds ? " -XX:SharedArchiveFile=lib/start.jsa" : "";
            final String command = String.format("cd %s; bin/java%s -jar app/%s", root, jvm, jriMainJar.getFileName());
            Log.info("Start script cannot be created for this platform, use: %s%s%s%s%s", EOL, EOL, INDENT, command, EOL);
            if (cds) {
                Log.info("Note that for CDS to function, the jar path MUST be relative as shown.");
            }
        }
    }

    private void reportSizes() {
        try {
            final long jars = application.diskSize();
            final long jdk = config.jdk().diskSize();
            final long jri = this.jri.diskSize();
            final long cds = config.cds() ? FileUtils.sizeOf(application.archivePath()) : 0;
            final long initial = jars + jdk;
            final float reduction = (1F - (float) jri / (float) initial) * 100F;
            Log.info("");
            Log.info("Initial disk size: %5.1fM  (%.1f application, %.1f JDK)", mb(initial), mb(jars), mb(jdk));
            if (cds == 0) {
                Log.info("  Image disk size: %5.1fM", mb(jri));
            } else {
                Log.info("  Image disk size: %5.1fM (includes %.1f CDS archive)", mb(jri), mb(cds));
            }
            Log.info("        Reduction: %5.1f%%", reduction);
        } catch (UncheckedIOException e) {
            Log.debug("Could not compute disk size: %s", e.getMessage());
        }
    }

    private void end() {
        final long elapsed = System.currentTimeMillis() - startTime;
        final float startSeconds = elapsed / 1000F;
        Log.info("");
        Log.info("Java Runtime Image completed in %.1f seconds", startSeconds);
    }

    private static float mb(final long bytes) {
        return ((float) bytes) / BYTES_PER_MEGABYTE;
    }

    private Path jriDirectory() {
        return fromWorking(config.jriDirectory());
    }

    private void addArgument(String argument) {
        jlinkArgs.add(argument);
    }

    private void addArgument(String argument, Path path) {
        addArgument(argument, path.normalize().toString());
    }

    private void addArgument(String argument, String value) {
        jlinkArgs.add(argument);
        jlinkArgs.add(value);
    }
}
