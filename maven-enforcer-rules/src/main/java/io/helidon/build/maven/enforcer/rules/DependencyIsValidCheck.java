/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.build.maven.enforcer.rules;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * A function that will return {@code true} if the given maven coordinate is valid.
 */
class DependencyIsValidCheck implements Function<Artifact, Boolean> {
    static final String JAKARTA_RENAMED = "jakarta-renamed.properties";
    static final String JAKARTA_VERSIONS = "jakarta-versions.properties";
    static final String JAKARTA_GROUPS = "jakarta-groups.properties";

    private static final Map<String, VersionRange> PACKAGE_TO_VERSIONS = loadVersions();
    private static final Map<String, String> PACKAGE_TO_RENAMED = loadRenamed();
    private static final Map<String, String> GROUP_TO_PACKAGE = loadGroups();

    private final String namespace;
    private final List<Pattern> excludedGavRegExs;

    DependencyIsValidCheck(String namespace,
                           List<Pattern> excludedGavRegExs) {
        this.namespace = namespace;
        this.excludedGavRegExs = excludedGavRegExs;
    }

    @Override
    public Boolean apply(Artifact gav) {
        String groupPackageName = toPackage(gav.getGroupId());
        if (isExcluded(groupPackageName)) {
            return true;
        }

        if (groupPackageName.equals("javax.servlet")
                || groupPackageName.equals("jakarta.servlet")) {
            return false;
        }

        try {
            if (HelidonDependenciesRule.JAKARTA.equals(namespace)) {
                return applyJakartaRule(groupPackageName, gav.getSelectedVersion());
            } else if (HelidonDependenciesRule.JAVAX.equals(namespace)) {
                return applyJavaxRule(groupPackageName, gav.getSelectedVersion());
            } else {
                throw new IllegalStateException("Invalid namespace: " + namespace);
            }
        } catch (OverConstrainedVersionException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Checks the given maven GAV.
     *
     * @param gav the maven GAV
     * @return true if the GAV is not in violation
     */
    public boolean apply(String gav) {
        return apply(toArtifact(gav));
    }

    /**
     * Validates the provided maven GAVs. If any are invalid an exception is thrown.
     *
     * @param gavs the array of maven GAVs
     * @throws ViolationException if a passed GAV is in violation of Helidon's usage policy
     */
    public void validate(String... gavs) throws ViolationException {
        validate(Arrays.stream(gavs).map(DependencyIsValidCheck::toArtifact).collect(Collectors.toList()));
    }

    /**
     * Validates the provided maven GAVs. If any are invalid an exception is thrown.
     *
     * @param gavs the collection of maven GAVs
     * @throws ViolationException if a passed GAV is in violation of Helidon's usage policy
     */
    public void validate(Collection<Artifact> gavs) throws ViolationException {
        List<String> violations = new ArrayList<>();
        for (Artifact gav : gavs) {
            if (!apply(gav)) {
                violations.add(gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion());
            }
        }

        if (!violations.isEmpty()) {
            throw new ViolationException("Bad dependencies spotted (review with mvn dependency:tree):   " + violations, violations);
        }
    }

    boolean applyJakartaRule(String groupPackageName,
                             ArtifactVersion version) {
        if (groupPackageName.startsWith("javax.")) {
            String renamedPackage = PACKAGE_TO_RENAMED.get(groupPackageName);
            return (renamedPackage == null);
        } else if (groupPackageName.startsWith("jakarta.")) {
            VersionRange versionRange = PACKAGE_TO_VERSIONS.get(groupPackageName);
            if (versionRange != null) {
                return versionRange.containsVersion(version);
            }
        }

        return true;
    }

    boolean applyJavaxRule(String groupPackageName,
                           ArtifactVersion version) {
        if (groupPackageName.startsWith("jakarta.")) {
            VersionRange versionRange = PACKAGE_TO_VERSIONS.get(groupPackageName);
            if (versionRange != null) {
                return !versionRange.containsVersion(version);
            }
        }
        return true;
    }

    /**
     * Converts a group name to a package name.
     *
     * @param group the group name
     * @return the associated package name
     */
    String toPackage(String group) {
        String packageName = GROUP_TO_PACKAGE.get(group);
        return (packageName == null) ? group : packageName;

    }

    boolean isExcluded(String groupPackageName) {
        for (Pattern pattern : excludedGavRegExs) {
            if (pattern.matcher(groupPackageName).matches()) {
                return true;
            }
        }
        return false;
    }

    static DefaultArtifact toArtifact(String gav) {
        String[] split = gav.split(":");
        DefaultArtifact artifact = new DefaultArtifact(split[0],
                                                       split.length > 1 ? split[1] : "", // artifact
                                                       split.length > 2 ? split[2] : null, // version
                                                       split.length > 3 ? split[3] : "compile", // scope
                                                       split.length > 4 ? split[4] : "jar", // type
                                                       split.length > 5 ? split[5] : "", // classifier
                                                       null); // handler
        return artifact;
    }

    static Map<String, VersionRange> loadVersions() {
        Map<String, VersionRange> result = new LinkedHashMap<>();
        load(JAKARTA_VERSIONS).forEach((k, v) -> {
            try {
                result.put(k.toString(), VersionRange.createFromVersionSpec(v.toString()));
            } catch (InvalidVersionSpecificationException e) {
                throw new IllegalStateException(e);
            }
        });
        return result;
    }

    static Map<String, String> loadRenamed() {
        Map<String, String> result = new LinkedHashMap<>();
        load(JAKARTA_RENAMED).forEach((k, v) -> result.put(k.toString(), v.toString()));
        return result;
    }

    static Map<String, String> loadGroups() {
        Map<String, String> result = new LinkedHashMap<>();
        load(JAKARTA_GROUPS).forEach((k, v) -> result.put(k.toString(), v.toString()));
        return result;
    }

    static Properties load(String name) {
        Properties props = new Properties();
        try (InputStream is = DependencyIsValidCheck.class.getClassLoader()
                .getResourceAsStream(name)) {
            props.load(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return props;
    }

}
