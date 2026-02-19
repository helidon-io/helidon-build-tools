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

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.spi.ToolProvider;

import io.helidon.build.common.FileUtils;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.Strings;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogFormatter;
import io.helidon.build.common.logging.LogLevel;

import static io.helidon.build.common.FileUtils.fileName;
import static io.helidon.build.common.FileUtils.fromWorking;
import static io.helidon.build.common.FileUtils.measuredSize;
import static io.helidon.build.common.FileUtils.sizeOf;
import static io.helidon.build.common.PrintStreams.STDERR;
import static io.helidon.build.common.PrintStreams.STDOUT;
import static io.helidon.build.linker.JavaRuntime.CURRENT_JDK;

/**
 * Create a custom runtime image by finding the Java modules required of a Helidon application and linking them via jlink,
 * then adding the jars, a start script and, optionally, a CDS archive. Adds Jandex indices as needed.
 */
public final class Linker {
    private static final String DEBUGGER_MODULE = "jdk.jdwp.agent";
    private final ToolProvider jlink;
    private final List<String> jlinkArgs;
    private final Configuration config;
    private final String imageName;
    private long startTime;
    private Application application;
    private String exitOnStarted;
    private Set<String> javaDependencies;
    private Path jriMainJar;
    private StartScript startScript;
    private List<String> startCommand;
    private long appSize;
    private long jdkSize;
    private long jriSize;
    private long cdsSize;
    private long jriAppSize;
    private long initialSize;
    private long imageSize;
    private float reduction;

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
        this.jlink = ToolProvider.findFirst("jlink").orElseThrow(() -> new IllegalStateException("jlink not found"));
        this.jlinkArgs = new ArrayList<>();
        this.config = config;
        this.imageName = fileName(config.jriDirectory());

        if (config.verbose()) {
            System.setProperty("jlink.debug", "true");
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
        Log.info("Creating Java Runtime Image $(cyan %s) from $(cyan JDK %s) and $(cyan %s), built with Helidon $(cyan %s)",
                imageName, CURRENT_JDK.version(), config.mainJar().getFileName(), version);
    }

    private void collectJavaDependencies() {
        Log.info("Collecting Java module dependencies...");
        javaDependencies = application.dependencies();
        javaDependencies.addAll(config.additionalModules());
        List<String> sorted = new ArrayList<>(javaDependencies);
        sorted.sort(null);
        Log.info("Including %d Java dependencies: %s", sorted.size(), String.join(", ", sorted));
        if (config.stripDebug()) {
            Log.info("Excluding debug support: jdk.jdwp.agent", DEBUGGER_MODULE);
        } else {
            javaDependencies.add(DEBUGGER_MODULE);
            Log.info("Including debug support: %s", DEBUGGER_MODULE);
        }
    }

    private void buildJlinkArguments() {

        // Tell jlink which jdk modules to include
        jlinkArgs.add("--add-modules");
        jlinkArgs.add(String.join(",", javaDependencies));

        // Tell jlink the directory in which to create and write the JRI
        jlinkArgs.add("--output");
        jlinkArgs.add(config.jriDirectory().normalize().toString());

        // Tell jlink to strip out unnecessary stuff
        if (config.stripDebug()) {
            jlinkArgs.add("--strip-debug");
        }
        jlinkArgs.add("--no-header-files");
        jlinkArgs.add("--no-man-pages");
        jlinkArgs.add("--compress");

        // The options used with --compress changed in 21
        if (CURRENT_JDK.version().feature() >= 21) {
            jlinkArgs.add("zip-6");
        } else {
            jlinkArgs.add("2");
        }

        // user provided args
        jlinkArgs.addAll(config.additionalJlinkArgs());
    }

    private void buildJri() {
        Log.info("Creating base image: %s", jriDirectory());
        final int result = jlink.run(System.out, System.err, jlinkArgs.toArray(new String[0]));
        if (result != 0) {
            throw new Error("JRI creation failed.");
        }
    }

    private void installJars() {
        boolean stripDebug = config.stripDebug();
        Path appDir = jriDirectory().resolve(Application.APP_DIR);
        String message = stripDebug ? ", stripping debug information from all classes" : "";
        Log.info("Installing %d application jars in %s%s", application.size(), appDir, message);
        this.jriMainJar = application.install(config.jriDirectory(), stripDebug);
    }

    private Path archiveFile() {
        if (config().cacheType() == Configuration.CacheType.AOT) {
            return application.aotCachePath();
        } else {
            return application.archivePath();
        }
    }

    private void installCdsArchive() {
        if (config.cacheType() != Configuration.CacheType.NONE) {
            try {
                ClassDataSharing cds = ClassDataSharing.builder()
                        .jri(config.jriDirectory())
                        .applicationJar(jriMainJar)
                        .jvmOptions(config.defaultJvmOptions())
                        .args((config.defaultArgs()))
                        .archiveFile(archiveFile())
                        .aot(config.cacheType() == Configuration.CacheType.AOT)
                        .exitOnStartedValue(exitOnStarted)
                        .maxWaitSeconds(config.maxAppStartSeconds())
                        .logOutput(config.verbose())
                        .build();

                // Get the archive size
                cdsSize = sizeOf(config.jriDirectory().resolve(archiveFile()));

                if (cds.aot()) {
                    // For aot we do not have the class list so just report archive size.
                    Log.info("AOT Cache %s is %s", cds.archiveFile(), measuredSize(cdsSize));
                } else {
                    // Count how many classes in the archive are from the JDK vs the app. Note that we cannot
                    // just count one and subtract since some classes in the class list may not have been
                    // put in the archive (see verbose output for examples).

                    Application app = application;
                    int jdkCount = 0;
                    int appCount = 0;
                    for (String name : cds.classList()) {
                        String resourcePath = name + ".class";
                        if (CURRENT_JDK.containsResource(resourcePath)) {
                            jdkCount++;
                        } else if (app.containsResource(resourcePath)) {
                            appCount++;
                        }
                    }

                    // Report the stats
                    if (appCount == 0) {
                        if (!CURRENT_JDK.cdsRequiresUnlock()) {
                            Log.warn("CDS archive does not contain any application classes, but should!");
                        }
                        Log.info("CDS archive is $(bold,blue %6s) for $(bold,blue %d) JDK classes",
                                measuredSize(cdsSize), jdkCount);
                    } else {
                        Log.info("CDS archive %s is $(bold,blue %6s) for $(bold,blue %d) classes:"
                                        + " $(bold,blue %d) JDK and $(bold,blue %d) application",
                                cds.archiveFile(), measuredSize(cdsSize), jdkCount + appCount, jdkCount, appCount);
                    }
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
                    .cdsInstalled(config.cacheType() == Configuration.CacheType.CDS)
                    .aotInstalled(config.cacheType() == Configuration.CacheType.AOT)
                    .debugInstalled(!config.stripDebug())
                    .exitOnStartedValue(exitOnStarted)
                    .build();

            Log.info("Installing start script in %s", startScript.installDirectory());
            startScript.install();
            startCommand = List.of(imageName + File.separator + "bin" + File.separator + startScript.scriptFile().getFileName());
        } catch (StartScript.PlatformNotSupportedError e) {
            if (config.cacheType() != Configuration.CacheType.NONE) {
                Log.warn("Start script cannot be created for this platform;"
                         + " for CDS/AOTCach to function, the jar path $(bold,yellow must) be relative as shown below.");
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
                Log.info("Executing $(cyan %s)", startCommand());
                Log.info();
                List<String> command = new ArrayList<>(startCommand);
                command.add(command.indexOf("-jar"), "-Dexit.on.started=!");
                File root = config.jriDirectory().toFile();
                try {
                    ProcessMonitor.builder()
                            .processBuilder(new ProcessBuilder().command(command).directory(root))
                            .stdOut(PrintStreams.apply(STDOUT, LogFormatter.of(LogLevel.INFO)))
                            .stdErr(PrintStreams.apply(STDERR, LogFormatter.of(LogLevel.WARN)))
                            .transform(line -> "    " + line)
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
            Log.info("Executing $(cyan %s)", startCommand() + " " + option);
            Log.info();
            startScript.execute(line -> "    " + line, option);
        }
    }

    private void computeSizes() {
        try {
            appSize = application.diskSize();
            jdkSize = FileUtils.sizeOf(CURRENT_JDK.path());
            imageSize = FileUtils.sizeOf(config.jriDirectory());
            jriAppSize = config.stripDebug() ? application.installedSize(config.jriDirectory()) : appSize;
            jriSize = imageSize - cdsSize - jriAppSize;
            initialSize = appSize + jdkSize;
            reduction = (1F - (float) imageSize / (float) initialSize) * 100F;
        } catch (UncheckedIOException e) {
            Log.debug("Could not compute disk size: %s", e.getMessage());
        }
    }

    private void end() {
        float startSeconds = (System.currentTimeMillis() - startTime) / 1000F;
        Log.info();
        Log.info("Java Runtime Image $(cyan %s) completed in %.1f seconds", imageName, startSeconds);
        Log.info();

        String measuredInitialSize = measuredSize(initialSize);
        String measuredJdkSize = measuredSize(jdkSize);
        String measuredAppSize = measuredSize(appSize);
        Log.info("Initial size: $(bold,blue %s) (JDK: %s, application: %s)",
                measuredInitialSize,
                measuredJdkSize,
                measuredAppSize);
        if (config.cacheType() != Configuration.CacheType.NONE) {
            Log.info("  Image size: $(bold,bright,green %s) (JDK: %s, application: %s, %s: %s)",
                    Strings.padded(" ", measuredInitialSize.length(), measuredSize(imageSize)),
                    Strings.padded(" ", measuredJdkSize.length(), measuredSize(jriSize)),
                    Strings.padded(" ", measuredAppSize.length(), measuredSize(jriAppSize)),
                    config.cacheType(),
                    measuredSize(cdsSize));
        } else {
            Log.info("  Image size: $(bold,bright,green %s) (JDK: %s, application: %s)",
                    Strings.padded(" ", measuredInitialSize.length(), measuredSize(imageSize)),
                    Strings.padded(" ", measuredJdkSize.length(), measuredSize(jriSize)),
                    Strings.padded(" ", measuredAppSize.length(), measuredSize(jriAppSize)));
        }
        Log.info("   Reduction: $(bold,bright,green %5.1f%%)", reduction);
        Log.info();
    }

    private Path jriDirectory() {
        return fromWorking(config.jriDirectory());
    }
}
