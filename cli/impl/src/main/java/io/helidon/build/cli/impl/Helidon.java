/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.cli.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.helidon.build.cli.harness.CommandLineInterface;
import io.helidon.build.cli.harness.CommandRunner;

import org.graalvm.nativeimage.ImageInfo;
import sun.misc.Signal;

/**
 * Helidon CLI definition and entry-point.
 */
@CommandLineInterface(
        name = "helidon",
        description = "Helidon command line tool",
        commands = {
                BuildCommand.class,
                DevCommand.class,
                InfoCommand.class,
                InitCommand.class,
                VersionCommand.class
        })
public final class Helidon {

    private Helidon() {
    }

    /**
     * Execute the command. Will call {@link System#exit(int)}.
     *
     * @param args raw command line arguments
     */
    public static void main(String... args) {
        execute(args, false);
    }

    /**
     * Execute the command in embedded mode. Will not call {@link System#exit(int)} but
     * return normally on success or throw an exception on failure. This entry point is not
     * intended for use within a native image (e.g. via JNI).
     *
     * @param args raw command line arguments
     * @throws Error if the command fails.
     */
    public static void execute(String... args) {
        execute(args, true);
    }

    /**
     * Execute the command.
     *
     * @param args raw command line arguments
     * @param embedded {@code true} if embedded mode.
     * @throws Error if the command fails and in embedded mode.
     */
    private static void execute(String[] args, boolean embedded) {

        if (ImageInfo.inImageRuntimeCode()) {
            // Register a signal handler for Ctrl-C that calls System.exit in order to trigger
            // the shutdown hooks
            Signal.handle(new Signal("INT"), sig -> System.exit(0));
        }

        String[] processedArgs = preProcessArgs(args);

        CommandRunner.builder()
                     .args(processedArgs)
                     .optionLookup(Config.userConfig()::property)
                     .cliClass(Helidon.class)
                     .embedded(embedded)
                     .build()
                     .initProxy()
                     .execute()
                     .runExitAction();
    }

    private static String[] preProcessArgs(String[] args) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if ("--args-file".equals(args[i]) && (i < args.length - 1)) {
                String argsFile = args[i + 1];
                result.addAll(readArgsFile(argsFile));
                i++;
            } else {
                result.add(args[i]);
            }
        }
        return result.toArray(new String[0]);
    }

    private static List<String> readArgsFile(String argsFile) {
        List<String> result = new ArrayList<>();
        try {
            URL fileURL = new URL(argsFile);
            fileURL.toURI();
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileURL.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    result.addAll(Arrays.asList(line.split("\\s+")));
                }
            }
            reader.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Option argsFile is incorrect.", e);
        }
        return result;
    }
}
