/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.logging.Log;

import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.unique;

final class CliInvocation extends ProcessInvocation {

    private Path bin;
    private Path inputFile;

    CliInvocation() {
    }

    CliInvocation bin(Path bin) {
        this.bin = bin;
        return this;
    }

    CliInvocation inputFile(Path inputFile) {
        this.inputFile = inputFile;
        return this;
    }

    @Override
    Monitor start() {
        if (bin == null) {
            throw new IllegalStateException("bin is null");
        }
        List<String> cmd = new ArrayList<>();
        cmd.add(bin.toString());
        cmd.addAll(args);
        ensureDirectory(cwd);
        Recorder recorder = new Recorder();
        Path logFile = unique(logDir != null ? logDir : cwd, "cli", ".log");
        try {
            PrintStream printStream = new PrintStream(Files.newOutputStream(logFile));
            ProcessMonitor processMonitor = ProcessMonitor.builder()
                    .processBuilder(new ProcessBuilder()
                            .command(cmd)
                            .directory(cwd.toFile()))
                    .stdIn(inputFile != null ? inputFile.toFile() : null)
                    .stdOut(PrintStreams.accept(printStream, recorder::record))
                    .stdErr(PrintStreams.accept(printStream, recorder::record))
                    .capture(true)
                    .build();
            Log.debug("Executing: " + String.join(" ", cmd));
            return new Monitor(processMonitor.start(), recorder, cwd);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (Exception ex) {
            throw new MonitorException(recorder.sb.toString(), ex);
        }
    }
}
