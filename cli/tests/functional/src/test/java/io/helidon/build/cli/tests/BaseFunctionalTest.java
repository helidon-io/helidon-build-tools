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

package io.helidon.build.cli.tests;

import io.helidon.build.cli.impl.Helidon;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.Strings;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

abstract class BaseFunctionalTest {

    static final String HELIDON_VERSION = helidonVersion();
    static final String ARCHETYPE_URL = String.format("file:///%s/cli-data", targetDir(BaseFunctionalTest.class));

    protected static Builder cliProcessBuilder() {
        return new Builder();
    }

    protected static String helidonVersion() {
        String version = System.getProperty("helidon.version");
        if (version != null) {
            return version;
        } else {
            throw new IllegalStateException("helidon.version is not set");
        }
    }

    protected void validateSeProject(Path wd) {
        Path projectDir = wd.resolve("bare-se");
        assertThat(Files.exists(projectDir.resolve("pom.xml")), is(true));
        assertThat(Files.exists(projectDir.resolve("src/main/resources/application.yaml")), is(true));
    }

    protected void validateMpProject(Path wd) {
        Path projectDir = wd.resolve("bare-mp");
        assertThat(Files.exists(projectDir.resolve("pom.xml")), is(true));
        assertThat(Files.exists(projectDir.resolve("src/main/resources/META-INF/microprofile-config.properties")), is(true));
    }

    static class Builder {

        private Path workDir;
        private File input;
        private Path executable;
        private ExecutionMode mode = ExecutionMode.CLASSPATH;
        private final List<String> args = new LinkedList<>();

        public Builder executable(Path executable) {
            this.executable = executable;
            this.mode = ExecutionMode.EXECUTABLE;
            return this;
        }

        public Builder input(File input) {
            this.input = input;
            return this;
        }

        public Builder init() {
            args.add(0, "init");
            return this;
        }

        public Builder info() {
            args.add(0, "info");
            return this;
        }

        public Builder version() {
            args.add(0, "version");
            return this;
        }

        public Builder workDirectory(Path workDir) {
            this.workDir = workDir;
            return this;
        }

        public Builder addArg(String option, String value) {
            addOption(option);
            addValue(value);
            return this;
        }

        public Builder addOption(String option) {
            Strings.requireValid(option, "Provided option is not valid");
            if (!option.startsWith("--")) {
                option = String.format("--%s", option);
            }
            args.add(option);
            return this;
        }

        private void addValue(String value) {
            args.add(Strings.requireValid(value, "Provided value is not valid"));
        }

        public ProcessMonitor start() {
            List<String> cmdArgs = new LinkedList<>();
            if (mode == ExecutionMode.CLASSPATH) {
                cmdArgs.addAll(FunctionalUtils.buildJavaCommand());
            }
            if (mode == ExecutionMode.EXECUTABLE) {
                cmdArgs.add(executable.normalize().toString());
            }
            cmdArgs.addAll(args);
            if (cmdArgs.contains("--init")) {
                cmdArgs.add("--reset");
                cmdArgs.add("--url");
                cmdArgs.add(ARCHETYPE_URL);
            }
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(cmdArgs);

            if (workDir != null) {
                pb.directory(workDir.toFile());
            }

            try {
                return ProcessMonitor.builder()
                        .processBuilder(pb)
                        .stdIn(input)
                        .stdOut(System.out)
                        .stdErr(System.err)
                        .capture(true)
                        .build()
                        .start();
            } catch (IOException e) {
                throw new UncheckedIOException("Could not run command", e);
            }
        }

        public String start(long timeout, TimeUnit unit) {
            try {
                return start().waitForCompletion(timeout, unit).output();
            } catch (ProcessMonitor.ProcessTimeoutException toe) {
                throw new RuntimeException("Execution timeout", toe);
            } catch (ProcessMonitor.ProcessFailedException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public void execute(Path wd) {
            args.add("--project");
            args.add(wd.toString());
            Helidon.execute(args.toArray(new String[args.size()]));
        }

    }

    enum ExecutionMode {
        CLASSPATH, EXECUTABLE
    }
}
