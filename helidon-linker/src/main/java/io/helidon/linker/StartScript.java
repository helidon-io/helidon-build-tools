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
import java.util.List;
import java.util.Set;

import io.helidon.linker.util.StreamUtils;

import static io.helidon.linker.util.FileUtils.assertDir;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Installs a start script for a main jar.
 */
public class StartScript {
    private static final String INSTALL_PATH = "bin/start";
    private final String script;

    /**
     * Returns a new builder.
     *
     * @return The builder.
     */
    static Builder builder() {
        return new Builder();
    }

    private StartScript(String script) {
        this.script = script;
    }

    /**
     * Install the script in the given JRE path.
     *
     * @param jrePath The path.
     * @return The path to the installed script.
     */
    Path install(Path jrePath) {
        try {
            final Path scriptFile = assertDir(jrePath).resolve(INSTALL_PATH);
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
        static final String DEFAULT_DEBUG = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005";
        private static final String TEMPLATE_PATH = "start-template.sh";
        private static final String MAIN_JAR_NAME = "<MAIN_JAR_NAME>";
        private static final String MAIN_ARGS = "<MAIN_ARGS>";
        private static final String JVM_OPTIONS = "<JVM_OPTIONS>";
        private static final String DEBUG_OPTIONS = "<DEBUG_OPTIONS>";
        private static final String MAIN_ARGS_DESC = "<MAIN_ARGS_DESC>";
        private static final String JVM_OPTIONS_DESC = "<JVM_OPTIONS_DESC>";
        private static final String DEBUG_OPTIONS_DESC = "<DEBUG_OPTIONS_DESC>";
        private static final String UNRECOGNIZED_DESC = "<UNRECOGNIZED_DESC>";
        private static final String JVM_SOME = "Overrides default: ${defaultJvmOptions}";
        private static final String JVM_NONE = "Sets JVM options.";
        private static final String ARGS_SOME = "Overrides default: ${defaultMainArgs}";
        private static final String ARGS_NONE = "Sets arguments.";
        private static final String DEBUG_SOME = "Overrides default: ${defaultDebugOptions}";
        private static final String DEBUG_NONE = "Sets debug options.";
        private static final String UNRECOGNIZED_SOME = "added to the default arguments and passed";
        private static final String UNRECOGNIZED_NONE = "passed as an argument";

        private static final String TEMPLATE = template();
        private Path mainJar;
        private List<String> jvmOptions;
        private List<String> args;
        private List<String> debugOptions;

        private Builder() {
            jvmOptions = emptyList();
            args = emptyList();
            debugOptions = List.of(DEFAULT_DEBUG);
        }

        /**
         * Sets the path to the main jar.
         *
         * @param mainJar The path. May not be {@code null}.
         * @return The builder.
         */
        public Builder mainJar(Path mainJar) {
            this.mainJar = requireNonNull(mainJar);
            return this;
        }

        /**
         * Sets the JVM options.
         *
         * @param jvmOptions The options.
         * @return The builder.
         */
        public Builder jvmOptions(List<String> jvmOptions) {
            this.jvmOptions = jvmOptions;
            return this;
        }

        /**
         * Sets the arguments.
         *
         * @param args The arguments.
         * @return The builder.
         */
        public Builder args(List<String> args) {
            this.args = args;
            return this;
        }

        /**
         * Sets the debug arguments used when starting the application with the {@code --debug} flag.
         *
         * @param debugOptions The options.
         * @return The builder.
         */
        public Builder debugOptions(List<String> debugOptions) {
            this.debugOptions = debugOptions;
            return this;
        }

        /**
         * Builds and returns the instance.
         *
         * @return The instance.
         */
        public StartScript build() {
            final String name = mainJar.getFileName().toString();

            final String jvmOptions = String.join(" ", this.jvmOptions);
            final String jvmDesc = jvmOptions.isEmpty() ? JVM_NONE : JVM_SOME.replace(JVM_OPTIONS, jvmOptions);

            final String args = String.join(" ", this.args);
            final String argsDesc = args.isEmpty() ? ARGS_NONE : ARGS_SOME.replace(MAIN_ARGS, args);

            final String debugOptions = String.join(" ", this.debugOptions);
            final String debugDesc = debugOptions.isEmpty() ? DEBUG_NONE : DEBUG_SOME.replace(DEBUG_OPTIONS, debugOptions);

            final String unrecognizedDesc = args.isEmpty() ? UNRECOGNIZED_NONE : UNRECOGNIZED_SOME;

            final String script = TEMPLATE.replace(MAIN_JAR_NAME, name)
                                          .replace(JVM_OPTIONS, jvmOptions)
                                          .replace(JVM_OPTIONS_DESC, jvmDesc)
                                          .replace(UNRECOGNIZED_DESC, unrecognizedDesc)
                                          .replace(MAIN_ARGS, args)
                                          .replace(MAIN_ARGS_DESC, argsDesc)
                                          .replace(DEBUG_OPTIONS, debugOptions)
                                          .replace(DEBUG_OPTIONS_DESC, debugDesc);

            return new StartScript(script);
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
