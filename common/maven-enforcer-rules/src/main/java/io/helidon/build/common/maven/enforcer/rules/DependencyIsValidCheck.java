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

package io.helidon.build.common.maven.enforcer.rules;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * A function that will return {@code true} if the given maven coordinate is valid.
 */
public class DependencyIsValidCheck implements Function<Gav, Boolean> {
    static final String JAKARTA_RENAMED = "jakarta-renamed.properties";
    static final String JAKARTA_VERSIONS = "jakarta-versions.properties";
    static final String JAKARTA_GROUPS = "jakarta-groups.properties";

    private static final Map<String, VersionRange> PACKAGE_TO_VERSIONS = loadVersions();
    private static final Map<String, String> PACKAGE_TO_RENAMED = loadRenamed();
    private static final Map<String, String> GROUP_TO_PACKAGE = loadGroups();

    /**
     * Default constructor.
     */
    protected DependencyIsValidCheck() {
    }

    /**
     * Validates the provided maven GAVs. If any are invalid an exception is thrown.
     *
     * @param gavs the array of maven GAVs
     * @throws ViolationException if a passed GAV is in violation of Helidon's usage policy
     */
    public void validate(String... gavs) {
        List<String> violations = new ArrayList<>();
        for (String gav : gavs) {
            if (!apply(gav)) {
                violations.add(gav);
            }
        }

        if (!violations.isEmpty()) {
            throw new ViolationException("Violations detected: " + violations, violations);
        }
    }

    /**
     * Checks the given maven GAV.
     *
     * @param gav the maven GAV
     * @return true if the GAV is not in violation
     */
    public boolean apply(String gav) {
        return apply(Gav.create(gav));
    }

    @Override
    public Boolean apply(Gav gav) {
        String groupPackageName = toPackage(gav.group());
        if (groupPackageName.equals("javax.servlet")
                || groupPackageName.equals("jakarta.servlet")) {
            return false;
        }

        if (groupPackageName.startsWith("javax.")) {
            String renamedPackage = PACKAGE_TO_RENAMED.get(groupPackageName);
            return (renamedPackage == null);
        } else if (groupPackageName.startsWith("jakarta.")) {
            VersionRange versionRange = PACKAGE_TO_VERSIONS.get(groupPackageName);
            if (versionRange != null) {
                return versionRange.containsVersion(gav.toArtifactVersion());
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
    public String toPackage(String group) {
        String packageName = GROUP_TO_PACKAGE.get(group);
        return (packageName == null) ? group : packageName;

    }

    /**
     * Creates a new instance.
     *
     * @return a new instance
     */
    public static DependencyIsValidCheck create() {
        return new DependencyIsValidCheck();
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
