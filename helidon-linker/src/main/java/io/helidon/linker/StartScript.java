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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import io.helidon.linker.util.FileUtils;
import io.helidon.linker.util.Log;
import io.helidon.linker.util.ProcessMonitor;
import io.helidon.linker.util.StreamUtils;

import static io.helidon.linker.util.Constants.CDS_REQUIRES_UNLOCK_OPTION;
import static io.helidon.linker.util.Constants.CDS_UNLOCK_OPTIONS;
import static io.helidon.linker.util.Constants.DIR_SEP;
import static io.helidon.linker.util.Constants.EOL;
import static io.helidon.linker.util.Constants.WINDOWS;
import static io.helidon.linker.util.FileUtils.assertDir;
import static java.util.Collections.emptyList;

/**
 * Installs a start script for a main jar.
 */
public class StartScript {
    private static final String INSTALL_PATH = "start";
    private final Path scriptFile;
    private final String script;


    /**
     * Returns a new builder.
     *
     * @return The builder.
     */
    static Builder builder() {
        return new Builder();
    }

    private StartScript(Builder builder) {
        this.scriptFile = builder.scriptFile;
        this.script = builder.script;
    }

    /**
     * Install the script.
     *
     * @return The path to the installed script.
     */
    Path install() {
        try {
            Files.copy(new ByteArrayInputStream(script.getBytes()), scriptFile);
            Files.setPosixFilePermissions(scriptFile, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_EXECUTE
            ));
            return scriptFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Execute the script with the given arguments.
     *
     * @param transform the output transform.
     * @param args The arguments.
     * @throws RuntimeException If the process fails.
     */
    public void execute(Function<String, String> transform, String... args) {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        final List<String> command = new ArrayList<>();
        command.add(scriptFile.toString());
        command.addAll(Arrays.asList(args));
        processBuilder.command(command);
        processBuilder.directory(scriptFile.getParent().getParent().toFile());
        try {
            ProcessMonitor.builder()
                          .processBuilder(processBuilder)
                          .stdOut(Log::info)
                          .stdErr(Log::warn)
                          .transform(transform)
                          .capture(false)
                          .build()
                          .execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the script.
     *
     * @return The script.
     */
    public String script() {
        return script;
    }

    /**
     * Returns the path to the script file.
     *
     * @return The path.
     */
    public Path scriptFile() {
        return scriptFile;
    }

    /**
     * Returns the script.
     *
     * @return The script.
     */
    @Override
    public String toString() {
        return script;
    }

    /**
     * Platform not supported error.
     */
    public static final class PlatformNotSupportedError extends IllegalStateException {
        private final List<String> command;

        private PlatformNotSupportedError(List<String> command) {
            this.command = command;
        }

        /**
         * Returns the Java command to execute on this platform.
         *
         * @return The command.
         */
        List<String> command() {
            return command;
        }
    }

    /**
     * The builder.
     */
    public static class Builder {
        private static final String TEMPLATE_NAME = "start-template";
        private static final String BASH_EXTENSION = ".sh";
        private static final String WINDOWS_EXTENSION = ".bat";
        private static final String JAR_NAME = "<JAR_NAME>";
        private static final String DEFAULT_ARGS = "<DEFAULT_ARGS>";
        private static final String DEFAULT_JVM = "<DEFAULT_JVM>";
        private static final String DEFAULT_DEBUG = "<DEFAULT_DEBUG>";
        private static final String HAS_CDS = "<HAS_CDS>";
        private static final String HAS_DEBUG = "<HAS_DEBUG>";
        private static final String CDS_UNLOCK_OPTION = "<CDS_UNLOCK>";
        private static final String DEFAULT_ARGS_DESC = "<DEFAULT_ARGS_DESC>";
        private static final String DEFAULT_JVM_DESC = "<DEFAULT_JVM_DESC>";
        private static final String DEFAULT_DEBUG_DESC = "<DEFAULT_DEBUG_DESC>";
        private static final String OVERRIDES = "Overrides \\\"${default%s}\\\".";
        private static final String SETS = "Sets default %s.";
        private static final String CDS = "cds";
        private static final String DEBUG = "debug";

        private List<String> template;
        private Path installDirectory;
        private Path mainJar;
        private List<String> defaultJvmOptions;
        private List<String> defaultDebugOptions;
        private List<String> defaultArgs;
        private boolean cdsInstalled;
        private boolean debugInstalled;
        private Path scriptFile;
        private String script;

        private Builder() {
            this.defaultJvmOptions = emptyList();
            this.defaultDebugOptions = List.of(Configuration.Builder.DEFAULT_DEBUG);
            this.cdsInstalled = true;
            this.debugInstalled = true;
            this.defaultArgs = emptyList();
        }

        /**
         * Sets the install directory.
         *
         * @param installDirectory The target.
         * @return The builder.
         */
        public Builder installDirectory(Path installDirectory) {
            this.installDirectory = assertDir(installDirectory);
            return this;
        }

        /**
         * Sets the path to the main jar.
         *
         * @param mainJar The path. May not be {@code null}.
         * @return The builder.
         */
        public Builder mainJar(Path mainJar) {
            this.mainJar = FileUtils.assertFile(mainJar);
            return this;
        }

        /**
         * Sets the default JVM options.
         *
         * @param jvmOptions The options.
         * @return The builder.
         */
        public Builder defaultJvmOptions(List<String> jvmOptions) {
            if (isValid(jvmOptions)) {
                this.defaultJvmOptions = jvmOptions;
            }
            return this;
        }

        /**
         * Sets the default arguments.
         *
         * @param args The arguments.
         * @return The builder.
         */
        public Builder defaultArgs(List<String> args) {
            if (isValid(args)) {
                this.defaultArgs = args;
            }
            return this;
        }

        /**
         * Sets the default debug arguments used when starting the application with the {@code --debug} flag.
         *
         * @param debugOptions The options.
         * @return The builder.
         */
        public Builder defaultDebugOptions(List<String> debugOptions) {
            if (isValid(debugOptions)) {
                this.defaultDebugOptions = debugOptions;
            }
            return this;
        }

        /**
         * Sets whether or not a CDS archive was installed.
         *
         * @param cdsInstalled {@code true} if installed.
         * @return The builder.
         */
        public Builder cdsInstalled(boolean cdsInstalled) {
            this.cdsInstalled = cdsInstalled;
            return this;
        }

        /**
         * Sets whether or not a debug classes and module were installed.
         *
         * @param debugInstalled {@code true} if debug is installed.
         * @return The builder.
         */
        public Builder debugInstalled(boolean debugInstalled) {
            this.debugInstalled = debugInstalled;
            return this;
        }

        /**
         * Builds and returns the instance.
         *
         * @return The instance.
         * @throws PlatformNotSupportedError If a script cannot be created for the current platform.
         */
        public StartScript build() {
            if (installDirectory == null) {
                throw new IllegalStateException("installTarget is required");
            }
            if (mainJar == null) {
                throw new IllegalStateException("mainJar is required");
            }
            this.template = template();
            this.scriptFile = assertDir(installDirectory).resolve(INSTALL_PATH);
            this.script = createScript();

            return new StartScript(this);
        }

        private String createScript() {
            final String name = mainJar.getFileName().toString();

            final String jvm = String.join(" ", this.defaultJvmOptions);
            final String jvmDesc = description(this.defaultJvmOptions, "JVM options", "Jvm");

            final String args = String.join(" ", this.defaultArgs);
            final String argsDesc = description(this.defaultArgs, "arguments", "Args");

            final List<String> debugOptions = debugInstalled ? this.defaultDebugOptions : emptyList();
            final String debug = String.join(" ", debugOptions);
            final String debugDesc = description(debugOptions, "debug options", "Debug");

            final String hasCds = cdsInstalled ? "yes" : "";
            final String hasDebug = debugInstalled ? "yes" : "";
            final String cdsUnlock = requiresUnlock() ? CDS_UNLOCK_OPTIONS + " " : "";

            if (!cdsInstalled) {
                removeTemplateLines(CDS);
            }
            
            if (!debugInstalled) {
                removeTemplateLines(DEBUG);
            }

            return String.join(EOL, template)
                         .replace(JAR_NAME, name)
                         .replace(DEFAULT_JVM, jvm)
                         .replace(DEFAULT_JVM_DESC, jvmDesc)
                         .replace(DEFAULT_ARGS, args)
                         .replace(DEFAULT_ARGS_DESC, argsDesc)
                         .replace(DEFAULT_DEBUG, debug)
                         .replace(DEFAULT_DEBUG_DESC, debugDesc)
                         .replace(HAS_CDS, hasCds)
                         .replace(HAS_DEBUG, hasDebug)
                         .replace(CDS_UNLOCK_OPTION, cdsUnlock);
        }

        private void removeTemplateLines(String containing) {
            for (int i = template.size() -1; i >= 0; i--) {
                if (template.get(i).toLowerCase().contains(containing)) {
                    template.remove(i);
                }
            }
        }

        private List<String> createCommand() {
            final List<String> command = new ArrayList<>();
            command.add("bin" + DIR_SEP + "java");
            if (cdsInstalled) {
                if (requiresUnlock()) {
                    command.add(CDS_UNLOCK_OPTIONS);
                }
                command.add("-XX:SharedArchiveFile=lib" + DIR_SEP + "start.jsa");
                command.add("-Xshare:on");
            }
            command.addAll(defaultJvmOptions);
            command.add("-jar");
            command.add("app" + DIR_SEP + mainJar.getFileName());
            command.addAll(defaultArgs);
            return command;
        }

        private boolean requiresUnlock() {
            return cdsInstalled && CDS_REQUIRES_UNLOCK_OPTION;
        }

        private static String description(List<String> defaults, String description, String varName) {
            if (defaults.isEmpty()) {
                return String.format(SETS, description);
            } else {
                return String.format(OVERRIDES, varName);
            }
        }

        private static boolean isValid(Collection<?> value) {
            return value != null && !value.isEmpty();
        }

        private List<String> template() {
            final String path = TEMPLATE_NAME + (WINDOWS ? WINDOWS_EXTENSION : BASH_EXTENSION);
            final InputStream content = StartScript.class.getClassLoader().getResourceAsStream(path);
            if (content == null) {
                throw new PlatformNotSupportedError(createCommand());
            } else {
                try {
                    return StreamUtils.toLines(content);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }
}
