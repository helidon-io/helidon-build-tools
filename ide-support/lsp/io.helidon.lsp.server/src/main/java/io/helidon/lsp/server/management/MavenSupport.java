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

package io.helidon.lsp.server.management;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.build.common.maven.MavenCommand;

/**
 * Support operations with maven.
 */
public class MavenSupport {

    private static final Logger LOGGER = Logger.getLogger(MavenSupport.class.getName());
    private static final String POM_FILE_NAME = "pom.xml";
    private static MavenSupport instance;

    private boolean isMavenInstalled = false;

    private MavenSupport() {
        initialize();
    }

    /**
     * Return instance of the MavenSupport class (singleton pattern).
     *
     * @return Instance of the MavenSupport class.
     */
    public static MavenSupport getInstance() {
        if (instance == null) {
            instance = new MavenSupport();
        }
        return instance;
    }

    private void initialize() {
        Path mavenPath = null;
        try {
            mavenPath = MavenCommand.mavenExecutable();
        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Maven is not installed in the system", e);
            return;
        }
        if (mavenPath != null) {
            isMavenInstalled = true;
        }
    }

    /**
     * Get paths for the dependency jars for the given pom file.
     *
     * @param pomPath Path to the pom file.
     * @return List that contains paths for the dependency jars.
     */
    public List<String> getDependencies(final String pomPath) {
        if (!isMavenInstalled) {
            return null;
        }

        List<String> output = new ArrayList<>();
        List<String> result = new ArrayList<>();
        String mvnCommand = "dependency:build-classpath";
        String dependencyMarker = "Dependencies classpath:";

        try {
            MavenCommand.builder()
                    .addArgument(mvnCommand)
                    .stdOut(output::add)
                    .directory(new File(pomPath).getParentFile())
                    .verbose(false)
                    .build().execute();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error when executing the maven command - " + mvnCommand, e);
            return Collections.emptyList();
        }
        for (int x = 0; x < output.size(); x++) {
            if (output.get(x).contains(dependencyMarker)) {
                String dependencies = output.get(x + 1);
                result.addAll(Arrays.asList(dependencies.split(File.pathSeparator)));
                break;
            }
        }

        return result;
    }

    /**
     * Get pom file for the file from the maven project.
     *
     * @param fileName File name.
     * @return Get pom file for the given file or null if pom.xml is not found.
     * @throws IOException IOException
     */
    public String getPomForFile(final String fileName) throws IOException {
        final Path currentPath = Paths.get(fileName);
        Path currentDirPath;
        if (currentPath.toFile().isDirectory()) {
            currentDirPath = currentPath;
        } else {
            currentDirPath = currentPath.getParent();
        }
        String pomForDir = findPomForDir(currentDirPath);
        while (pomForDir == null && currentDirPath != null) {
            currentDirPath = currentDirPath.getParent();
            pomForDir = findPomForDir(currentDirPath);
        }
        return pomForDir;
    }

    private String findPomForDir(final Path directoryPath) throws IOException {
        if (directoryPath == null) {
            return null;
        }
        File[] listFiles = directoryPath.toFile().listFiles();
        if (listFiles == null) {
            return null;
        }
        return Arrays.stream(listFiles)
                .filter(file ->
                        file.isFile() && file.getName().equals(POM_FILE_NAME))
                .findFirst()
                .map(File::getAbsolutePath).orElse(null);
    }

}
