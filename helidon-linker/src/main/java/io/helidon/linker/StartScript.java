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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import io.helidon.linker.util.FileUtils;
import io.helidon.linker.util.Log;
import io.helidon.linker.util.ProcessMonitor;
import io.helidon.linker.util.StreamUtils;

import static io.helidon.linker.util.FileUtils.assertDir;
import static java.util.Collections.emptyList;

/**
 * Installs a start script for a main jar.
 */
public class StartScript {
    private static final String INSTALL_PATH = "start";
    private static final String EOL = System.getProperty("line.separator");
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
     * @param description A description of what is being executed.
     * @param args The arguments.
     */
    public void execute(String description, String... args) {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        final List<String> command = new ArrayList<>();
        command.add(scriptFile.toString());
        command.addAll(Arrays.asList(args));
        processBuilder.command(command);
        processBuilder.directory(scriptFile.getParent().getParent().toFile());

        try {
            final List<String> output = ProcessMonitor.builder()
                                                      .description(description)
                                                      .processBuilder(processBuilder)
                                                      .capture(true)
                                                      .build()
                                                      .execute()
                                                      .output();
            Log.info(String.join(EOL, output));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
     * The builder.
     */
    public static class Builder {
        private static final String TEMPLATE_PATH = "start-template.sh";
        private static final String JAR_NAME = "<JAR_NAME>";
        private static final String DEFAULT_ARGS = "<DEFAULT_ARGS>";
        private static final String DEFAULT_JVM = "<DEFAULT_JVM>";
        private static final String DEFAULT_DEBUG = "<DEFAULT_DEBUG>";
        private static final String DEFAULT_ARGS_DESC = "<DEFAULT_ARGS_DESC>";
        private static final String DEFAULT_JVM_DESC = "<DEFAULT_JVM_DESC>";
        private static final String DEFAULT_DEBUG_DESC = "<DEFAULT_DEBUG_DESC>";
        private static final String JVM_SOME = "Overrides default JVM options (${defaultJvm})";
        private static final String JVM_NONE = "Sets default JVM options.";
        private static final String ARGS_SOME = "Overrides default arguments (${defaultArgs})";
        private static final String ARGS_NONE = "Sets default arguments.";
        private static final String DEBUG_SOME = "Overrides default debug options (${defaultDebug})";
        private static final String DEBUG_NONE = "Sets debug options.";

        private static final String TEMPLATE = template();
        private Path installDirectory;
        private Path mainJar;
        private List<String> defaultJvmOptions;
        private List<String> defaultDebugOptions;
        private List<String> defaultArgs;
        private Path scriptFile;
        private String script;

        private Builder() {
            defaultJvmOptions = emptyList();
            defaultDebugOptions = List.of(Configuration.Builder.DEFAULT_DEBUG);
            defaultArgs = emptyList();
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
         * Builds and returns the instance.
         *
         * @return The instance.
         */
        public StartScript build() {
            if (installDirectory == null) {
                throw new IllegalStateException("installTarget is required");
            }
            if (mainJar == null) {
                throw new IllegalStateException("mainJar is required");
            }

            this.scriptFile = assertDir(installDirectory).resolve(INSTALL_PATH);
            this.script = createScript();

            return new StartScript(this);
        }

        private String createScript() {
            final String name = mainJar.getFileName().toString();

            final String jvm = String.join(" ", this.defaultJvmOptions);
            final String jvmDesc = jvm.isEmpty() ? JVM_NONE : JVM_SOME.replace(DEFAULT_JVM, jvm);

            final String args = String.join(" ", this.defaultArgs);
            final String argsDesc = args.isEmpty() ? ARGS_NONE : ARGS_SOME.replace(DEFAULT_ARGS, args);

            final String debug = String.join(" ", this.defaultDebugOptions);
            final String debugDesc = debug.isEmpty() ? DEBUG_NONE : DEBUG_SOME.replace(DEFAULT_DEBUG, debug);

            return TEMPLATE.replace(JAR_NAME, name)
                           .replace(DEFAULT_JVM, jvm)
                           .replace(DEFAULT_JVM_DESC, jvmDesc)
                           .replace(DEFAULT_ARGS, args)
                           .replace(DEFAULT_ARGS_DESC, argsDesc)
                           .replace(DEFAULT_DEBUG, debug)
                           .replace(DEFAULT_DEBUG_DESC, debugDesc);
        }

        private static boolean isValid(Collection<?> value) {
            return value != null && !value.isEmpty();
        }

        private static String template() {
            try {
                return StreamUtils.toString(StartScript.class.getClassLoader().getResourceAsStream(TEMPLATE_PATH));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
