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
package io.helidon.build.maven.enforcer.nativeimage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import io.helidon.build.common.PathFinder;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.maven.enforcer.EnforcerException;
import io.helidon.build.maven.enforcer.RuleFailure;

import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;

/**
 * Native image version rule.
 */
public class NativeImageRule {

    private static final String NATIVE_IMAGE = "native-image";
    private static final String GRAALVM_HOME = "GRAALVM_HOME";
    private static final String JAVA_HOME = "JAVA_HOME";
    private final List<VersionConfig> versionConfigs;

    /**
     * Constructor.
     *
     * @param config native-image configuration
     */
    public NativeImageRule(NativeImageConfig config) {
        this.versionConfigs = config.rules();
    }

    /**
     * Check native-image version.
     *
     * @return list of errors
     */
    public List<RuleFailure> check() {
        List<RuleFailure> failures = new LinkedList<>();
        Process process = startProcess();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("GraalVM")) {
                    checkVersion(failures, line.split(" "));
                }
            }
        } catch (IOException e) {
            throw new EnforcerException("Fail to run native-image --version");
        }
        return failures;
    }

    private void checkVersion(List<RuleFailure> failures, String[] elements) {
        if (elements.length < 1) {
            return;
        }
        MavenVersion version = toMavenVersion(elements[1]);
        for (VersionConfig rule : versionConfigs) {
            rule.checkVersion(version, failures);
        }
    }

    private Process startProcess() {
        Path nativeImage = findNativeImage();
        if (nativeImage == null
                || !nativeImage.toFile().exists()) {
            throw new EnforcerException("native-image executable not found.");
        }
        ProcessBuilder builder = new ProcessBuilder(nativeImage.toString(), "--version")
                .redirectErrorStream(true);

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new EnforcerException("Fail to run native-image --version");
        }

        try {
            process.getOutputStream().close();
        } catch (IOException ignored) {
        }
        return process;
    }

    private Path findNativeImage() {
        return PathFinder.find(NATIVE_IMAGE,
                        List.of(Optional.ofNullable(System.getenv(GRAALVM_HOME))
                                .map(Path::of)
                                .map(p -> p.resolve("bin"))),
                        List.of(Optional.ofNullable(System.getenv(JAVA_HOME))
                                .map(Path::of)
                                .map(p -> p.resolve("bin"))))
                .orElseThrow(() -> new IllegalStateException("Unable to find " + NATIVE_IMAGE));
    }

}
