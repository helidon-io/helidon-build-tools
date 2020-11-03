/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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

package io.helidon.build.test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.util.ApplicationGenerator;
import io.helidon.build.util.FileUtils;
import io.helidon.build.util.HelidonVariant;
import io.helidon.build.util.Instance;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenCommand;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static io.helidon.build.test.HelidonTestVersions.helidonBuildToolsTestVersion;
import static io.helidon.build.test.HelidonTestVersions.helidonTestVersion;
import static io.helidon.build.util.Constants.DIR_SEP;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.ensureDirectory;
import static java.util.Objects.requireNonNull;

/**
 * Test file utilities.
 */
public class TestFiles implements BeforeAllCallback {
    private static final String HELIDON_APP_PREFIX = "helidon-app-";
    private static final String VERSION_1_4_1 = "1.4.1";
    private static final AtomicReference<Path> TARGET_DIR = new AtomicReference<>();
    private static final Instance<Path> SE_JAR = new Instance<>(TestFiles::getOrCreateApplicationSeJar);
    private static final Instance<Path> MP_JAR = new Instance<>(TestFiles::getOrCreateApplicationMpJar);
    private static final Instance<Path> SIGNED_JAR = new Instance<>(TestFiles::fetchSignedJar);
    private static final AtomicInteger SE_COPY_NUMBER = new AtomicInteger(1);
    private static final AtomicInteger MP_COPY_NUMBER = new AtomicInteger(1);

    @Override
    public void beforeAll(ExtensionContext ctx) {
        if (TARGET_DIR.get() == null) {
            TARGET_DIR.set(targetDir(requireNonNull(ctx.getRequiredTestClass())));
        }
    }

    /**
     * Returns the target directory, set from the location of the first test class to execute. This approach ensures that
     * each project using this class will have its own target directory used.
     *
     * @return The directory.
     */
    public static Path targetDir() {
        return requireNonNull(TARGET_DIR.get());
    }

    /**
     * Returns the value required for the {@code -Dexit.on.started} property to trigger on
     * the current Helidon test version.
     *
     * @return The value.
     */
    public static String exitOnStartedValue() {
        return helidonTestVersion().equals(VERSION_1_4_1) ? "✅" : "!";
    }

    /**
     * Returns the SE main jar created from the latest archetype version.
     *
     * @return The jar.
     */
    public static Path helidonSeJar() {
        return SE_JAR.instance();
    }

    /**
     * Returns the SE project directory created from the latest archetype version.
     *
     * @return The directory.
     */
    public static Path helidonSeProject() {
        helidonSeJar(); // ensure created.
        return targetDir().resolve(applicationId(HelidonVariant.SE));
    }

    /**
     * Returns the directory of a new copy of the SE project.
     *
     * @return The directory.
     */
    public static Path helidonSeProjectCopy() {
        return copyApplicationProject(HelidonVariant.SE, SE_COPY_NUMBER);
    }

    /**
     * Returns the MP main jar created from the latest archetype version.
     *
     * @return The jar.
     */
    public static Path helidonMpJar() {
        return MP_JAR.instance();
    }

    /**
     * Returns the MP project directory created from the latest archetype version.
     *
     * @return The directory.
     */
    public static Path helidonMpProject() {
        helidonMpJar(); // ensure created.
        return targetDir().resolve(applicationId(HelidonVariant.MP));
    }

    /**
     * Returns the directory of a new copy of the MP project.
     *
     * @return The directory.
     */
    public static Path helidonMpProjectCopy() {
        return copyApplicationProject(HelidonVariant.MP, MP_COPY_NUMBER);
    }

    /**
     * Returns a signed jar.
     *
     * @return The jar.
     */
    public static Path signedJar() {
        return SIGNED_JAR.instance();
    }

    /**
     * Recursively delete a directory.
     *
     * @param dir Directory to delete.
     * @return Outcome of operation.
     */
    public static boolean deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDirectory(file);
            }
        }
        return dir.delete();
    }

    /**
     * ID or directory for application.
     *
     * @param variant The Helidon variant.
     */
    public static String applicationId(HelidonVariant variant) {
        return HELIDON_APP_PREFIX + variant;
    }


    /**
     * Returns the target directory for the given test class.
     *
     * @param testClass The test class.
     * @return The directory.
     */
    public static Path targetDir(Class<?> testClass) {
        try {
            final Path codeSource = Paths.get(testClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            return ensureDirectory(codeSource.getParent());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Path fetchSignedJar() {
        throw new IllegalStateException("not yet implemented, see https://github.com/oracle/helidon-build-tools/issues/110");
    }

    private static Path getOrCreateApplicationSeJar() {
        return getOrCreateApplicationJar(HelidonVariant.SE);
    }

    private static Path getOrCreateApplicationMpJar() {
        return getOrCreateApplicationJar(HelidonVariant.MP);
    }

    private static Path getOrCreateApplicationJar(HelidonVariant variant) {
        final String id = applicationId(variant);
        final Path sourceDir = targetDir().resolve(id);
        if (Files.exists(sourceDir)) {
            return applicationJar(sourceDir, id);
        } else {
            return createApplicationJar(variant);
        }
    }

    private static Path createApplicationJar(HelidonVariant variant) {
        final Path projectDir = createApplicationProject(variant);
        return buildApplicationProject(variant, projectDir);
    }

    private static Path createApplicationProject(HelidonVariant variant) {
        return ApplicationGenerator.generator()
                                   .helidonVariant(variant)
                                   .parentDirectory(targetDir())
                                   .helidonVersion(helidonTestVersion())
                                   .pluginVersion(helidonBuildToolsTestVersion())
                                   .generate();
    }

    private static Path buildApplicationProject(HelidonVariant variant, Path projectDir) {
        final String id = applicationId(variant);
        Log.info("Building %s", id);
        try {
            MavenCommand.builder()
                        .directory(projectDir)
                        .addArgument("clean")
                        .addArgument("package")
                        .addArgument("-DskipTests")
                        .build()
                        .execute();
            return applicationJar(projectDir, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Path applicationJar(Path projectDir, String id) {
        return assertFile(projectDir.resolve("target" + DIR_SEP + id + ".jar"));
    }

    private static Path directory(HelidonVariant variant, int copyNumber) {
        final String id = applicationId(variant) + "-" + copyNumber;
        return targetDir().resolve(id);
    }

    private static Path copyApplicationProject(HelidonVariant variant, AtomicInteger copyNumber) {
        final Path sourceDir = helidonSeProject();
        Path copyDir = directory(variant, copyNumber.getAndIncrement());
        while (Files.exists(copyDir)) {
            copyDir = directory(variant, copyNumber.getAndIncrement());
        }
        return FileUtils.copyDirectory(sourceDir, copyDir);
    }
}
