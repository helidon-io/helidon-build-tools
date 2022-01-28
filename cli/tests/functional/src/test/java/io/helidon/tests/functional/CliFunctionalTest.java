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

package io.helidon.tests.functional;

import io.helidon.build.cli.impl.CommandInvoker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class CliFunctionalTest {

    private static final String HELIDON_VERSION = System.getProperty("helidon.test.version", "please-set-helidon-test-version");
    private static final String CUSTOM_GROUP_ID = "mygroupid";
    private static final String CUSTOM_ARTIFACT_ID = "myartifactid";
    private static final String CUSTOM_PROJECT = "myproject";
    private static final String CUSTOM_PACKAGE_NAME = "custom.pack.name";
    private static Path workDir;
    private static Path inputFile;

    @BeforeAll
    static void setup() throws IOException {
        workDir = Files.createTempDirectory("generated");
        inputFile = Files.createTempFile("input","txt");
        Files.writeString(inputFile, "\n\n\n");
    }

    @AfterAll
    static void cleanTempFile() {
        inputFile.toFile().delete();
    }

    @AfterEach
    public void cleanUp() throws IOException {
        Files.walk(workDir)
                .sorted(Comparator.reverseOrder())
                .filter(it -> !it.equals(workDir))
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void batchTest(String flavor, String archetype) throws Exception {
        runBatchTest(flavor, HELIDON_VERSION, archetype, null, null, null, null, true);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void interactiveTest(String flavor, String archetype) throws Exception {
        runInteractiveTest(flavor, null, archetype, null, null, null, null, true);
    }

    @ParameterizedTest
    @CsvSource({
            "se,bare,2.3.0",
            "se,database,2.3.0",
            "se,quickstart,2.3.0",
            "mp,bare,2.3.0",
            "mp,database,2.3.0",
            "mp,quickstart,2.3.0"})
    void batchVersionTest(String flavor, String archetype, String version) throws Exception {
        runBatchTest(flavor, version, archetype, null, null, null, null, false);
    }

    @ParameterizedTest
    @CsvSource({
            "se,bare,2.3.0",
            "se,database,2.3.0",
            "se,quickstart,2.3.0",
            "mp,bare,2.3.0",
            "mp,database,2.3.0",
            "mp,quickstart,2.3.0"})
    void interactiveVersionTest(String flavor, String archetype, String version) throws Exception {
        runInteractiveTest(flavor, version, archetype, null, null, null, null, false);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void batchAllTest(String flavor, String archetype) throws Exception {
        runBatchTest(flavor, "2.3.0", archetype, CUSTOM_GROUP_ID, CUSTOM_ARTIFACT_ID, CUSTOM_PACKAGE_NAME, CUSTOM_PROJECT, false);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void interactiveAllTest(String flavor, String archetype) throws Exception {
        runInteractiveTest(flavor, "2.3.0", archetype, CUSTOM_GROUP_ID, CUSTOM_ARTIFACT_ID, CUSTOM_PACKAGE_NAME, CUSTOM_PROJECT, false);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void customPackageNameBatchTest(String flavor, String archetype) throws Exception {
        runBatchTest(flavor, HELIDON_VERSION, archetype, null, null, CUSTOM_PACKAGE_NAME, null, false);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void customPackageNameInteractiveTest(String flavor, String archetype) throws Exception {
        runInteractiveTest(flavor, null, archetype, null, null, CUSTOM_PACKAGE_NAME, null, false);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void customGroupIdBatchTest(String flavor, String archetype) throws Exception {
        runBatchTest(flavor, HELIDON_VERSION, archetype, CUSTOM_GROUP_ID, null, null, null, false);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void customGroupIdInteractiveTest(String flavor, String archetype) throws Exception {
        runInteractiveTest(flavor, null, archetype, CUSTOM_GROUP_ID, null, null, null, false);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void customArtifactIdBatchTest(String flavor, String archetype) throws Exception {
        runBatchTest(flavor, HELIDON_VERSION, archetype, null, CUSTOM_ARTIFACT_ID, null, null, false);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void customArtifactIdInteractiveTest(String flavor, String archetype) throws Exception {
        runInteractiveTest(flavor, null, archetype, CUSTOM_ARTIFACT_ID, null, null, null, false);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void customProjectNameBatchTest(String flavor, String archetype) throws Exception {
        runBatchTest(flavor, HELIDON_VERSION, archetype, null, null, null, CUSTOM_PROJECT, false);
    }

    @ParameterizedTest
    @CsvSource({"se,bare", "se,database", "se,quickstart", "mp,bare", "mp,database", "mp,quickstart"})
    void customProjectNameInteractiveTest(String flavor, String archetype) throws Exception {
        runInteractiveTest(flavor, null, archetype, null, null, null, CUSTOM_PROJECT, false);
    }

    private CommandInvoker.Builder commandInvoker(String version) {
        return CommandInvoker.builder()
                .helidonVersion(version)
                .metadataUrl("https://helidon.io/cli-data")
                .workDir(workDir);
    }

    private void runBatchTest(String flavor,
                              String version,
                              String archetype,
                              String groupId,
                              String artifactId,
                              String packageName,
                              String name,
                              boolean startApp) throws Exception {
        commandInvoker(version)
                .buildProject(startApp)
                .flavor(flavor)
                .archetypeName(archetype)
                .groupId(groupId)
                .artifactId(artifactId)
                .packageName(packageName)
                .projectName(name)
                .invokeInit()
                .validateProject();
    }

    private void runInteractiveTest(String flavor,
                                    String version,
                                    String archetype,
                                    String groupId,
                                    String artifactId,
                                    String packageName,
                                    String name,
                                    boolean startApp) throws Exception {
        commandInvoker(version)
                .buildProject(startApp)
                .flavor(flavor)
                .archetypeName(archetype)
                .groupId(groupId)
                .artifactId(artifactId)
                .packageName(packageName)
                .projectName(name)
                .input(inputFile.toUri().toURL())
                .invokeInit()
                .validateProject();
    }

}
