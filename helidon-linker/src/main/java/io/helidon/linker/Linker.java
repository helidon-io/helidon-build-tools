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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;

import io.helidon.linker.util.JavaRuntime;
import io.helidon.linker.util.Log;

import static io.helidon.linker.util.FileUtils.fromWorking;

/**
 * Create a custom JRE by finding the Java modules required of a Helidon application and linking them via jlink,
 * then adding the jars, a start script and, optionally, a CDS archive. Adds Jandex indices as needed.
 */
public class Linker {
    private static final String JLINK_TOOL_NAME = "jlink";
    private static final String JLINK_DEBUG_PROPERTY = JLINK_TOOL_NAME + ".debug";
    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private final ToolProvider jlink;
    private final List<String> jlinkArgs;
    private Configuration config;
    private Application application;
    private Set<String> javaDependencies;
    private JavaRuntime jre;
    private Path jreMainJar;

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
     * Create the JRE.
     *
     * @return The JRE directory.
     */
    public Path link() {
        final long startTime = System.currentTimeMillis();
        buildApplication();
        collectJavaDependencies();
        buildJlinkArguments();
        buildJre();
        installJars();
        installCdsArchive();
        installStartScript();
        complete(startTime);
        return config.jreDirectory();
    }

    /**
     * Returns the configuration.
     *
     * @return The configuration.
     */
    public Configuration config() {
        return config;
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

        // Tell jlink the directory in which to create and write the JRE

        addArgument("--output", config.jreDirectory());

        // Tell jlink to strip out unnecessary stuff

        if (config.stripDebug()) {
            addArgument("--strip-debug");
        }
        addArgument("--no-header-files");
        addArgument("--no-man-pages");
        addArgument("--compress", "2");
    }

    private void buildJre() {
        Log.info("Building Helidon JRE: %s", jreDirectory());
        final int result = jlink.run(System.out, System.err, jlinkArgs.toArray(new String[0]));
        if (result != 0) {
            throw new Error("Helidon JRE creation failed.");
        }
        jre = JavaRuntime.jre(config.jreDirectory(), config.jdk().version());
    }

    private void installJars() {
        Log.info("Copying %d application jars to %s", application.size(), jreDirectory());
        this.jreMainJar = application.install(jre);
    }

    private void installCdsArchive() {
        if (config.cds()) {
            try {
                ClassDataSharing.builder()
                                .jre(jre.path())
                                .applicationJar(jreMainJar)
                                .jvmOptions(config.jvmOptions())
                                .args((config.args()))
                                .archiveFile(application.archivePath())
                                .logOutput(config.verbose())
                                .build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void installStartScript() {
        if (!WINDOWS) {
            StartScript.builder()
                       .jvmOptions(config.jvmOptions())
                       .mainJar(jreMainJar)
                       .args(config.args())
                       .build()
                       .install(jre.path());
        }
    }

    private void complete(long startTime) {
        final long elapsed = System.currentTimeMillis() - startTime;
        final float startSeconds = elapsed / 1000F;
        Log.info("Helidon JRE completed in %.1f seconds", startSeconds);
        if (!WINDOWS) {
            Log.info("See %s/start --help", jreDirectory().resolve("bin"));
        }
    }

    private Path jreDirectory() {
        return fromWorking(config.jreDirectory());
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
