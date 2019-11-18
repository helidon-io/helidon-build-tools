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

import io.helidon.linker.util.JavaRuntime;
import io.helidon.linker.util.Log;

import static io.helidon.linker.Application.APP_DIR;
import static io.helidon.linker.util.Constants.DIR_SEP;
import static io.helidon.linker.util.Constants.INDENT;
import static io.helidon.linker.util.FileUtils.fromWorking;
import static io.helidon.linker.util.FileUtils.sizeOf;
import static io.helidon.linker.util.Style.BoldBlue;
import static io.helidon.linker.util.Style.BoldBrightCyan;
import static io.helidon.linker.util.Style.BoldBrightGreen;

/**
 * Create a custom runtime image by finding the Java modules required of a Helidon application and linking them via jlink,
 * then adding the jars, a start script and, optionally, a CDS archive. Adds Jandex indices as needed.
 */
public class Linker {
    private static final String JLINK_TOOL_NAME = "jlink";
    private static final String JLINK_DEBUG_PROPERTY = JLINK_TOOL_NAME + ".debug";
    private static final float BYTES_PER_MEGABYTE = 1024F * 1024F;
    private final ToolProvider jlink;
    private final List<String> jlinkArgs;
    private final Configuration config;
    private final String imageName;
    private long startTime;
    private Application application;
    private Set<String> javaDependencies;
    private JavaRuntime jri;
    private Path jriMainJar;
    private StartScript startScript;
    private String startCommand;

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
        this.imageName = config.jriDirectory().getFileName().toString();

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
        Log.info("Creating Java Runtime Image %s from %s", BoldBrightCyan.apply(imageName), 
                 config.mainJar().getFileName());
        this.startTime = System.currentTimeMillis();
    }

    private void buildApplication() {
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
            startCommand = imageName + DIR_SEP + "bin" + DIR_SEP + startScript.scriptFile().getFileName();
            Log.info("");
            Log.info("Executing %s", BoldBlue.apply(startCommand + " --help"));
            startScript.execute(true, "--help");
        } catch (IllegalStateException e) {
            Log.warn("Start script cannot be created for this platform.");
            final Path root = fromWorking(jri.path());
            final String jvm = config.cds() ? " -XX:SharedArchiveFile=lib" + DIR_SEP + "start.jsa" : "";
            final Path jarName = jriMainJar.getFileName();
            startCommand = String.format("cd %s; bin" + DIR_SEP + "java%s -jar app" + DIR_SEP + "%s", root, jvm, jarName);
        }
    }

    private void reportSizes() {
        try {
            final long jars = application.diskSize();
            final long jdk = config.jdk().diskSize();

            final long jri = this.jri.diskSize();
            final long cds = config.cds() ? sizeOf(this.jri.path().resolve(application.archivePath())) : 0;
            final long jriOnly = jri - cds - jars;

            final long initial = jars + jdk;
            final float reduction = (1F - (float) jri / (float) initial) * 100F;

            final String initialSize = BoldBlue.format("%5.1fM", mb(initial));
            final String imageSize = BoldBrightGreen.format("%5.1fM", mb(jri));
            final String percent = BoldBrightGreen.format("%5.1f%%", reduction);

            Log.info("");
            Log.info("Initial disk size: %s  (%.1f application + %.1f JDK)", initialSize, mb(jars), mb(jdk));
            if (cds == 0) {
                Log.info("  Image disk size: %s  (%.1f application + %.1f JDK)", imageSize, mb(jars), mb(jriOnly));
            } else {
                Log.info("  Image disk size: %s  (%.1f application + %.1f JDK + %.1f CDS)", imageSize, mb(jars),
                         mb(jriOnly), mb(cds));
            }
            Log.info("        Reduction: %s", percent);
        } catch (UncheckedIOException e) {
            Log.debug("Could not compute disk size: %s", e.getMessage());
        }
    }

    private void end() {
        final long elapsed = System.currentTimeMillis() - startTime;
        final float startSeconds = elapsed / 1000F;
        Log.info("");
        Log.info("Execute the following to start:", imageName);
        Log.info("");
        Log.info(INDENT + BoldBlue.apply(startCommand));
        if (startScript == null && config.cds()) {
            Log.info("Note that for CDS to function, the jar path MUST be relative as shown.");
        }
        Log.info("");
        Log.info("Java Runtime Image %s completed in %.1f seconds", BoldBrightCyan.apply(imageName), startSeconds);
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
