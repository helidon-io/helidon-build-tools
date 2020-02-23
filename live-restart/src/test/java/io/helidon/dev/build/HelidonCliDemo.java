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
import java.util.Optional;

import io.helidon.dev.mode.DevModeLoop;
import io.helidon.dev.mode.QuickstartGenerator;

import static io.helidon.dev.mode.QuickstartGenerator.HelidonVariant;

/**
 * Class HelidonCli.
 *
 * Usage: helidon (init [se | mp] | dev)
 */
public class HelidonCliDemo {

    private static final String DOT_HELIDON = ".helidon";
    private static final Path CWD = Path.of(".");

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            displayHelpAndExit();
        }
        if (args[0].equalsIgnoreCase("init")) {
            HelidonVariant variant = HelidonVariant.SE;
            if (args.length > 1) {
                try {
                    variant = HelidonVariant.valueOf(args[1]);
                } catch (IllegalArgumentException e) {
                    displayHelpAndExit();
                }
            }
            helidonInit(variant);
        } else if (args[0].equalsIgnoreCase("dev")) {
            helidonDev();
        } else {
            displayHelpAndExit();
        }
    }

    private static void helidonInit(HelidonVariant variant) throws Exception {
        Optional<Path> rootDir = readRootDir();
        if (rootDir.isEmpty()) {
            QuickstartGenerator generator = new QuickstartGenerator(CWD);
            storeRootDir(generator.generate(variant));
        } else {
            System.err.println("Project '" + rootDir.get() + "' already exists");
            System.exit(2);
        }
    }

    private static void helidonDev() throws Exception {
        Optional<Path> rootDir = readRootDir();
        if (rootDir.isPresent()) {
            DevModeLoop loop = new DevModeLoop(rootDir.get());
            loop.start(60 * 60);
        } else {
            System.err.println("Please run init command first");
            System.exit(3);
        }
    }

    private static void displayHelpAndExit() {
        System.out.println("Usage: helidon (init [se | mp] | dev)");
        System.exit(1);
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
