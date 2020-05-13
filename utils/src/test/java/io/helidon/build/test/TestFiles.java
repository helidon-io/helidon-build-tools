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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.util.FileUtils;
import io.helidon.build.util.HelidonVariant;
import io.helidon.build.util.Instance;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenCommand;
import io.helidon.build.util.QuickstartGenerator;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static io.helidon.build.test.HelidonTestVersions.currentHelidonBuildToolsReleaseVersion;
import static io.helidon.build.test.HelidonTestVersions.currentHelidonReleaseVersion;
import static io.helidon.build.util.Constants.DIR_SEP;
import static io.helidon.build.util.FileUtils.assertFile;
import static io.helidon.build.util.FileUtils.ensureDirectory;
import static io.helidon.build.util.FileUtils.lastModifiedTime;
import static java.util.Objects.requireNonNull;

/**
 * Test file utilities.
 */
public class TestFiles implements BeforeAllCallback {
    private static final String HELIDON_QUICKSTART_PREFIX = "helidon-quickstart-";
    private static final String SIGNED_JAR_COORDINATES = "org.bouncycastle:bcpkix-jdk15on:1.60";
    private static final String VERSION_1_4_1 = "1.4.1";
    private static final AtomicReference<Path> TARGET_DIR = new AtomicReference<>();
    private static final Instance<Path> SE_JAR = new Instance<>(TestFiles::getOrCreateQuickstartSeJar);
    private static final Instance<Path> MP_JAR = new Instance<>(TestFiles::getOrCreateQuickstartMpJar);
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
     * the latest Helidon version.
     *
     * @return The value.
     */
    public static String exitOnStartedValue() {
        return currentHelidonReleaseVersion().equals(VERSION_1_4_1) ? "✅" : "!";
    }

    /**
     * Returns the quickstart SE main jar created from the latest archetype version.
     *
     * @return The jar.
     */
    public static Path helidonSeJar() {
        return SE_JAR.instance();
    }

    /**
     * Returns the quickstart SE project directory created from the latest archetype version.
     *
     * @return The directory.
     */
    public static Path helidonSeProject() {
        helidonSeJar(); // ensure created.
        return targetDir().resolve(quickstartId(HelidonVariant.SE));
    }

    /**
     * Returns the directory of a new copy of the quickstart SE project.
     *
     * @return The directory.
     */
    public static Path helidonSeProjectCopy() {
        return copyQuickstartProject(HelidonVariant.SE, SE_COPY_NUMBER);
    }

    /**
     * Returns the quickstart MP main jar created from the latest archetype version.
     *
     * @return The jar.
     */
    public static Path helidonMpJar() {
        return MP_JAR.instance();
    }

    /**
     * Returns the quickstart MP project directory created from the latest archetype version.
     *
     * @return The directory.
     */
    public static Path helidonMpProject() {
        helidonMpJar(); // ensure created.
        return targetDir().resolve(quickstartId(HelidonVariant.MP));
    }

    /**
     * Returns the directory of a new copy of the quickstart MP project.
     *
     * @return The directory.
     */
    public static Path helidonMpProjectCopy() {
        return copyQuickstartProject(HelidonVariant.MP, MP_COPY_NUMBER);
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
     * Creates the given file (with no content) if it does not already exist.
     *
     * @param file The file.
     * @return The file.
     */
    public static Path ensureFile(Path file) {
        if (!Files.exists(file)) {
            try {
                Files.createFile(file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return file;
    }

    /**
     * Ensure that the given file exists, and update the modified time if it does.
     *
     * @param file The file.
     * @return The file.
     */
    public static Path touch(Path file) {
        if (Files.exists(file)) {
            final long currentTime = System.currentTimeMillis();
            final long lastModified = lastModifiedTime(file);
            final long lastModifiedPlusOneSecond = lastModified + 1000;
            final long newTime = Math.max(currentTime, lastModifiedPlusOneSecond);
            try {
                Files.setLastModifiedTime(file, FileTime.fromMillis(newTime));
                return file;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            return ensureFile(file);
        }
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
     * ID or directory for quickstart.
     *
     * @param variant The Helidon variant.
     */
    public static String quickstartId(HelidonVariant variant) {
        return HELIDON_QUICKSTART_PREFIX + variant;
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

    private static Path getOrCreateQuickstartSeJar() {
        return getOrCreateQuickstartJar(HelidonVariant.SE);
    }

    private static Path getOrCreateQuickstartMpJar() {
        return getOrCreateQuickstartJar(HelidonVariant.MP);
    }

    private static Path getOrCreateQuickstartJar(HelidonVariant variant) {
        final String id = quickstartId(variant);
        final Path sourceDir = targetDir().resolve(id);
        if (Files.exists(sourceDir)) {
            return quickstartJar(sourceDir, id);
        } else {
            return createQuickstartJar(variant);
        }
    }

    private static Path createQuickstartJar(HelidonVariant variant) {
        final Path projectDir = createQuickstartProject(variant);
        return buildQuickstartProject(variant, projectDir);
    }

    private static Path createQuickstartProject(HelidonVariant variant) {
        return QuickstartGenerator.generator()
                                  .helidonVariant(variant)
                                  .parentDirectory(targetDir())
                                  .helidonVersion(currentHelidonReleaseVersion())
                                  .pluginVersion(currentHelidonBuildToolsReleaseVersion())
                                  .generate();
    }

    private static Path buildQuickstartProject(HelidonVariant variant, Path projectDir) {
        final String id = quickstartId(variant);
        Log.info("Building %s", id);

        MavenCommand.builder()
                    .directory(projectDir)
                    .addArgument("clean")
                    .addArgument("package")
                    .addArgument("-DskipTests")
                    .build()
                    .execute();

        return quickstartJar(projectDir, id);
    }

    private static Path quickstartJar(Path projectDir, String id) {
        return assertFile(projectDir.resolve("target" + DIR_SEP + id + ".jar"));
    }

    private static Path directory(HelidonVariant variant, int copyNumber) {
        final String id = quickstartId(variant) + "-" + copyNumber;
        return targetDir().resolve(id);
    }

    private static Path copyQuickstartProject(HelidonVariant variant, AtomicInteger copyNumber) {
        final Path sourceDir = helidonSeProject();
        Path copyDir = directory(variant, copyNumber.getAndIncrement());
        while (Files.exists(copyDir)) {
            copyDir = directory(variant, copyNumber.getAndIncrement());
        }
        return FileUtils.copyDirectory(sourceDir, copyDir);
    }
}
