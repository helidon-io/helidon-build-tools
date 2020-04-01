/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

package io.helidon.build.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

import org.eclipse.aether.version.Version;

import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.Maven.LATEST_RELEASE;

/**
 * Generator for a quickstart project.
 */
public class QuickstartGenerator extends SimpleQuickstartGenerator {
    private static final String HELIDON_GROUP_ID = "io.helidon";
    private static final String HELIDON_PROJECT_ID = "helidon-project";

    private Predicate<Version> versionSelector;

    /**
     * Returns a new generator.
     *
     * @return The generator.
     */
    public static QuickstartGenerator generator() {
        return new QuickstartGenerator();
    }

    private QuickstartGenerator() {
        this.versionSelector = LATEST_RELEASE;
    }

    /**
     * Sets a selector for the Helidon version to use. The latest release is selected if not set.
     *
     * @param helidonVersionSelector The version. May be {@code null}.
     * @return This instance, for chaining.
     */
    public QuickstartGenerator helidonVersion(Predicate<Version> helidonVersionSelector) {
        this.versionSelector = helidonVersionSelector == null ? LATEST_RELEASE : helidonVersionSelector;
        return this;
    }

    /**
     * Sets the Helidon variant to use.
     *
     * @param helidonVariant The variant.
     * @return This instance, for chaining.
     */
    public QuickstartGenerator helidonVariant(HelidonVariant helidonVariant) {
        super.helidonVariant(helidonVariant);
        return this;
    }

    /**
     * Sets whether or not log messages should be suppressed. Default is {@code false}.
     *
     * @param quiet {@code true} if log messages should not be written.
     * @return This instance, for chaining.
     */
    public QuickstartGenerator quiet(boolean quiet) {
        super.quiet(quiet);
        return this;
    }

    /**
     * Sets the directory in which to generate the project.
     *
     * @param parentDirectory The parent directory.
     * @return This instance, for chaining.
     */
    public QuickstartGenerator parentDirectory(Path parentDirectory) {
        super.parentDirectory(assertDir(parentDirectory));
        return this;
    }

    @Override
    protected void initialize() {
        if (variant() == null) {
            throw new IllegalStateException("helidonVariant required.");
        }
        if (parentDirectory() == null) {
            throw new IllegalStateException("projectDirectory required.");
        }
        if (groupId() == null) {
            groupId("test");
        }
        if (artifactId() == null) {
            artifactId(HELIDON_QUICKSTART_PREFIX + variant().toString());
        }
        final Path projectDir = parentDirectory().resolve(artifactId());
        if (Files.exists(projectDir)) {
            throw new IllegalStateException(projectDir + " already exists");
        } else {
            final Maven maven = Maven.instance();
            Version version = maven.latestVersion(HELIDON_GROUP_ID, HELIDON_PROJECT_ID, versionSelector);
            helidonVersion(version.toString());
        }
    }
}
