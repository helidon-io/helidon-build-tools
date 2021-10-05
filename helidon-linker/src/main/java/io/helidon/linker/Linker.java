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

package io.helidon.linker;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.spi.ToolProvider;

import io.helidon.build.util.Log;
import io.helidon.build.util.Log.Level;
import io.helidon.build.util.LogFormatter;
import io.helidon.build.util.PrintStreams;
import io.helidon.build.util.ProcessMonitor;
import io.helidon.linker.util.JavaRuntime;

import static io.helidon.build.util.FileUtils.fileName;
import static io.helidon.build.util.FileUtils.fromWorking;
import static io.helidon.build.util.FileUtils.sizeOf;
import static io.helidon.build.util.PrintStreams.STDERR;
import static io.helidon.build.util.PrintStreams.STDOUT;
import static io.helidon.build.util.StyleFunction.BoldBlue;
import static io.helidon.build.util.StyleFunction.BoldBrightGreen;
import static io.helidon.build.util.StyleFunction.BoldYellow;
import static io.helidon.build.util.StyleFunction.Cyan;
import static io.helidon.linker.Application.APP_DIR;
import static io.helidon.linker.util.Constants.CDS_REQUIRES_UNLOCK_OPTION;
import static io.helidon.linker.util.Constants.DEBUGGER_MODULE;
import static io.helidon.linker.util.Constants.DIR_SEP;
import static io.helidon.linker.util.Constants.INDENT;

/**
 * Create a custom runtime image by finding the Java modules required of a Helidon application and linking them via jlink,
 * then adding the jars, a start script and, optionally, a CDS archive. Adds Jandex indices as needed.
 */
public final class Linker {
    private static final String JLINK_TOOL_NAME = "jlink";
    private static final String JLINK_DEBUG_PROPERTY = JLINK_TOOL_NAME + ".debug";
    private static final float BYTES_PER_MEGABYTE = 1024F * 1024F;
    private final ToolProvider jlink;
    private final List<String> jlinkArgs;
    private final Configuration config;
    private final String imageName;
    private long startTime;
    private Application application;
    private String exitOnStarted;
    private Set<String> javaDependencies;
    private JavaRuntime jri;
    private Path jriMainJar;
    private long cdsArchiveSize;
    private StartScript startScript;
    private List<String> startCommand;
    private float appSize;
    private float jdkSize;
    private float jriSize;
    private float cdsSize;
    private float jriAppSize;
    private String initialSize;
    private String imageSize;
    private String percent;

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
        this.jlink = ToolProvider.findFirst(JLINK_TOOL_NAME).orElseThrow(() -> new IllegalStateException("jlink not found"));
        this.jlinkArgs = new ArrayList<>();
        this.config = config;
        this.imageName = fileName(config.jriDirectory());

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
        testImage();
        displayStartScriptHelp();
        computeSizes();
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
        Log.info();
        this.startTime = System.currentTimeMillis();
    }

    private void buildApplication() {
        this.application = Application.create(config.mainJar());
        this.exitOnStarted = application.exitOnStartedValue();
        final String version = application.helidonVersion();
        Log.info("Creating Java Runtime Image %s from %s and %s, built with Helidon %s",
                 Cyan.apply(imageName),
                 Cyan.apply("JDK " + config.jdk().version()),
                 Cyan.apply(config.mainJar().getFileName()),
                 Cyan.apply(version));
    }

    private void collectJavaDependencies() {
        Log.info("Collecting Java module dependencies");
        this.javaDependencies = application.javaDependencies(config.jdk());
        this.javaDependencies.addAll(config.additionalModules());
        final List<String> sorted = new ArrayList<>(javaDependencies);
        sorted.sort(null);
        Log.info("Including %d Java dependencies: %s", sorted.size(), String.join(", ", sorted));
        if (config.stripDebug()) {
            Log.info("Excluding debug support: %s", DEBUGGER_MODULE);
        } else {
            javaDependencies.add(DEBUGGER_MODULE);
            Log.info("Including debug support: %s", DEBUGGER_MODULE);
        }
    }

    private void buildJlinkArguments() {

        // On JDK 9, jlink insists on a --module-path so give it the jmods directory

        addArgument("--module-path", config.jdk().jmodsDir());

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
        final boolean stripDebug = config.stripDebug();
        final Path appDir = jriDirectory().resolve(APP_DIR);
        final String message = stripDebug ? ", stripping debug information from all classes" : "";
        Log.info("Installing %d application jars in %s%s", application.size(), appDir, message);
        this.jriMainJar = application.install(jri, stripDebug);
    }

    private void installCdsArchive() {
        if (config.cds()) {
            try {
                final ClassDataSharing cds = ClassDataSharing.builder()
                                                             .jri(jri.path())
                                                             .applicationJar(jriMainJar)
                                                             .jvmOptions(config.defaultJvmOptions())
                                                             .args((config.defaultArgs()))
                                                             .archiveFile(application.archivePath())
                                                             .exitOnStartedValue(exitOnStarted)
                                                             .maxWaitSeconds(config.maxAppStartSeconds())
                                                             .logOutput(config.verbose())
                                                             .build();

                // Get the archive size

                cdsArchiveSize = sizeOf(jri.path().resolve(application.archivePath()));

                // Count how many classes in the archive are from the JDK vs the app. Note that we cannot
                // just count one and subtract since some classes in the class list may not have been
                // put in the archive (see verbose output for examples).

                final JavaRuntime jdk = config.jdk();
                final Application app = application;
                int jdkCount = 0;
                int appCount = 0;
                for (String name : cds.classList()) {
                    final String resourcePath = name + ".class";
                    if (jdk.containsResource(resourcePath)) {
                        jdkCount++;
                    } else if (app.containsResource(resourcePath)) {
                        appCount++;
                    }
                }

                // Report the stats

                final String cdsSize = BoldBlue.format("%.1fM", mb(cdsArchiveSize));
                final String jdkSize = BoldBlue.format("%d", jdkCount);
                final String appSize = BoldBlue.format("%d", appCount);
                if (appCount == 0) {
                    if (!CDS_REQUIRES_UNLOCK_OPTION) {
                        Log.warn("CDS archive does not contain any application classes, but should!");
                    }
                    Log.info("CDS archive is %s for %s JDK classes", cdsSize, jdkSize);
                } else {
                    final String total = BoldBlue.format("%d", jdkCount + appCount);
                    Log.info("CDS archive is %s for %s classes: %s JDK and %s application", cdsSize, total, jdkSize, appSize);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void installStartScript() {
        try {
            startScript = StartScript.builder()
                                     .installHomeDirectory(config.jriDirectory())
                                     .defaultJvmOptions(config.defaultJvmOptions())
                                     .defaultDebugOptions(config.defaultDebugOptions())
                                     .mainJar(jriMainJar)
                                     .defaultArgs(config.defaultArgs())
                                     .cdsInstalled(config.cds())
                                     .debugInstalled(!config.stripDebug())
                                     .exitOnStartedValue(exitOnStarted)
                                     .build();

            Log.info("Installing start script in %s", startScript.installDirectory());
            startScript.install();
            startCommand = List.of(imageName + DIR_SEP + "bin" + DIR_SEP + startScript.scriptFile().getFileName());
        } catch (StartScript.PlatformNotSupportedError e) {
            if (config.cds()) {
                Log.warn("Start script cannot be created for this platform; for CDS to function, the jar path %s"
                         + " be relative as shown below.", BoldYellow.apply("must"));
            } else {
                Log.warn("Start script cannot be created for this platform.");
            }
            startCommand = e.command();
        }
    }

    private String startCommand() {
        return String.join(" ", startCommand);
    }

    private void testImage() {
        if (config.test()) {
            if (startScript != null) {
                executeStartScript("--test");
            } else {
                Log.info();
                Log.info("Executing %s", Cyan.apply(startCommand()));
                Log.info();
                final List<String> command = new ArrayList<>(startCommand);
                command.add(command.indexOf("-jar"), "-Dexit.on.started=!");
                final File root = config.jriDirectory().toFile();
                try {
                    ProcessMonitor.builder()
                                  .processBuilder(new ProcessBuilder().command(command).directory(root))
                                  .stdOut(PrintStreams.apply(STDOUT, LogFormatter.of(Level.INFO)))
                                  .stdErr(PrintStreams.apply(STDERR, LogFormatter.of(Level.WARN)))
                                  .transform(INDENT)
                                  .capture(false)
                                  .build()
                                  .execute(config.maxAppStartSeconds(), TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void displayStartScriptHelp() {
        executeStartScript("--help");
    }

    private void executeStartScript(String option) {
        if (startScript != null) {
            Log.info();
            Log.info("Executing %s", Cyan.apply(startCommand() + " " + option));
            Log.info();
            startScript.execute(INDENT, option);
        }
    }

    private void computeSizes() {
        try {
            final long app = application.diskSize();
            final long jdk = config.jdk().diskSize();
            final long jri = this.jri.diskSize();
            final long cds = cdsArchiveSize;
            final long jriApp = config.stripDebug() ? application.installedSize(this.jri) : app;
            final long jriOnly = jri - cds - jriApp;
            final long initial = app + jdk;
            final float reduction = (1F - (float) jri / (float) initial) * 100F;

            appSize = mb(app);
            jdkSize = mb(jdk);
            jriSize = mb(jriOnly);
            cdsSize = config.cds() ? mb(cds) : 0;
            jriAppSize = mb(jriApp);
            initialSize = BoldBlue.format("%5.1fM", mb(initial));
            imageSize = BoldBrightGreen.format("%5.1fM", mb(jri));
            percent = BoldBrightGreen.format("%5.1f%%", reduction);
        } catch (UncheckedIOException e) {
            Log.debug("Could not compute disk size: %s", e.getMessage());
        }
    }

    private void end() {
        final long elapsed = System.currentTimeMillis() - startTime;
        final float startSeconds = elapsed / 1000F;
        Log.info();
        Log.info("Java Runtime Image %s completed in %.1f seconds", Cyan.apply(imageName), startSeconds);
        Log.info();
        Log.info("     initial size: %s  (%.1f JDK + %.1f application)", initialSize, jdkSize, appSize);
        if (config.cds()) {
            Log.info("       image size: %s  (%5.1f JDK + %.1f application + %.1f CDS)", imageSize, jriSize, jriAppSize, cdsSize);
        } else {
            Log.info("       image size: %s  (%5.1f JDK + %.1f application)", imageSize, jriSize, jriAppSize);
        }
        Log.info("        reduction: %s", percent);
        Log.info();
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
