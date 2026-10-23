/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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

import java.nio.file.Files;
import java.nio.file.Path;

import io.helidon.build.cli.common.ProjectConfig;
import io.helidon.build.common.RequirementFailure;
import io.helidon.build.common.Requirements;
import io.helidon.build.common.RichTextRenderer;
import io.helidon.build.common.maven.MavenCommand;
import io.helidon.build.common.maven.MavenModel;
import io.helidon.build.common.maven.MavenModel.Parent;
import io.helidon.build.common.maven.MavenVersion;

import static io.helidon.build.cli.common.ProjectConfig.ensureProjectConfig;
import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.FileUtils.requireFile;
import static io.helidon.build.common.FileUtils.requireJavaExecutable;
import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;

/**
 * Command assertions with message strings formatted via {@link RichTextRenderer#render(String, Object...)}.
 */
public class CommandRequirements {

    private static final MavenVersion MINIMUM_HELIDON_VERSION = toMavenVersion("2.0.0-M4");
    private static final MavenVersion ALLOWED_HELIDON_SNAPSHOT_VERSION = toMavenVersion("2.0.0-SNAPSHOT");
    private static final MavenVersion MINIMUM_REQUIRED_MAVEN_VERSION = toMavenVersion("3.6.0");

    /**
     * Assert that the given Helidon version is supported.
     *
     * @param helidonVersion The version.
     * @return The version, for chaining.
     * @throws RequirementFailure If the version does not meet the requirement.
     */
    static String requireSupportedHelidonVersion(String helidonVersion) {
        requireSupportedHelidonVersion(toMavenVersion(helidonVersion));
        return helidonVersion;
    }

    /**
     * Assert that the given Helidon version is supported.
     *
     * @param helidonVersion The version.
     * @return The version, for chaining.
     * @throws RequirementFailure If the version does not meet the requirement.
     */
    static MavenVersion requireSupportedHelidonVersion(MavenVersion helidonVersion) {
        Requirements.require(helidonVersion.equals(ALLOWED_HELIDON_SNAPSHOT_VERSION)
                        || helidonVersion.isGreaterThanOrEqualTo(MINIMUM_HELIDON_VERSION),
                "$(red Helidon version) $(RED %s) $(red is not supported.)", helidonVersion);
        return helidonVersion;
    }

    /**
     * An unsupported Java version was found.
     */
    static void unsupportedJavaVersion() {
        throw new RequirementFailure(
                "$(red Java version at %s is not supported: 11 or later is required.)",
                requireJavaExecutable());
    }

    /**
     * Assert that the installed Maven version is at least the required minimum.
     *
     * @throws RequirementFailure If the installed version does not meet the requirement.
     */
    static void requireMinimumMavenVersion() {
        MavenCommand.requireMavenVersion(MINIMUM_REQUIRED_MAVEN_VERSION);
    }

    /**
     * Require that a valid Maven project configuration exists.
     *
     * @param commonOptions The options.
     */
    static void requireValidMavenProjectConfig(CommonOptions commonOptions) {
        try {
            Path projectDir = commonOptions.project();
            Path pomFile = requireFile(requireDirectory(projectDir).resolve("pom.xml"));
            Path projectConfigFile = ProjectConfig.toDotHelidon(projectDir);
            if (!Files.exists(projectConfigFile)) {
                // Find the helidon version if we can and create the config file
                MavenModel model = MavenModel.read(pomFile);
                Parent parent = model.parent();
                String helidonVersion = null;
                if (parent != null && parent.groupId().startsWith("io.helidon.")) {
                    helidonVersion = parent.version();
                }
                ensureProjectConfig(projectDir, helidonVersion);
            }
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("does not exist")) {
                message = "$(italic Please cd to a project directory.)";
            }
            throw new RequirementFailure(message);
        }
    }

    /**
     * Assert that the given version directory exists.
     *
     * @throws RequirementFailure If the directory does not exist.
     */
    static Path requireHelidonVersionDir(Path versionDir) {
        try {
            return requireDirectory(versionDir);
        } catch (Exception e) {
            throw new RequirementFailure("$(italic Version %s not found.)", versionDir.getFileName());
        }
    }

    private CommandRequirements() {
    }
}
