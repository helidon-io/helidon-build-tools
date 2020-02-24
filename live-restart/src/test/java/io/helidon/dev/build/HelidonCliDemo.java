/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.build;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import io.helidon.dev.mode.DevModeLoop;
import io.helidon.dev.mode.QuickstartGenerator;

import static io.helidon.dev.mode.QuickstartGenerator.HelidonVariant;

/**
 * Class HelidonCli.
 */
public class HelidonCliDemo {
    private static final String USAGE = "Usage: helidon init [se | mp] [--version <version>]| dev [--clean]";
    private static final String DOT_HELIDON = ".helidon";
    private static final Path CWD = Path.of(".");

    public static void main(String[] args) throws Exception {
        String command = null;
        HelidonVariant variant = HelidonVariant.SE;
        String version = null;
        boolean clean = false;

        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.startsWith("--")) {
                if ("init".equals(command) && arg.equals("--version")) {
                    version = argAt(++i, args);
                } else if ("dev".equals(command) && arg.equalsIgnoreCase("--clean")) {
                    clean = true;
                } else if (arg.equals("--help")) {
                    displayHelpAndExit(0);
                } else {
                    displayHelpAndExit(1);
                }
            } else if (command == null) {
                command = arg;
            } else if (command.equals("init")) {
                variant = HelidonVariant.valueOf(argAt(++i, args).toUpperCase(Locale.ENGLISH));
            } else {
                displayHelpAndExit(1);
            }
        }
        if (command == null) {
            displayHelpAndExit(1);
        } else if (command.equals("init")) {
            helidonInit(variant, version);
        } else if (command.equals("dev")) {
            helidonDev(clean);
        } else {
            displayHelpAndExit(1);
        }
    }

    private static String argAt(int index, String[] args) {
        if (index < args.length) {
            return args[index];
        } else {
            System.err.println(args[index - 1] + ": missing required argument");
            displayHelpAndExit(1);
            return "";
        }
    }

    private static void helidonInit(HelidonVariant variant, String version) throws Exception {
        Optional<Path> rootDir = readRootDir();
        if (rootDir.isEmpty()) {
            storeRootDir(QuickstartGenerator.generator()
                                            .projectDirectory(CWD)
                                            .helidonVariant(variant)
                                            .helidonVersion(version)
                                            .generate());
        } else {
            System.err.println("Project '" + rootDir.get() + "' already exists");
            System.exit(2);
        }
    }

    private static void helidonDev(boolean clean) throws Exception {
        Optional<Path> rootDir = readRootDir();
        if (rootDir.isPresent()) {
            DevModeLoop loop = new DevModeLoop(rootDir.get(), clean);
            loop.start(60 * 60);
        } else {
            System.err.println("Please run init command first");
            System.exit(3);
        }
    }

    private static void displayHelpAndExit(int status) {
        System.out.println(USAGE);
        System.exit(status);
    }

    private static void storeRootDir(Path rootDir) throws Exception {
        File file = Path.of(DOT_HELIDON).toFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(rootDir.toString());
            writer.newLine();
        }
    }

    private static Optional<Path> readRootDir() throws Exception {
        File file = Path.of(DOT_HELIDON).toFile();
        if (!file.exists()) {
            return Optional.empty();
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Path path = Path.of(reader.readLine());
            // If project directory no longer exists, remove dot file
            if (!path.toFile().exists()) {
                boolean result = file.delete();
                if (!result) {
                    System.out.println("Unable to delete " + DOT_HELIDON);
                    System.exit(4);
                }
                return Optional.empty();
            }
            return Optional.of(path);
        }
    }
}
