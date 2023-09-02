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
import java.util.LinkedHashMap;
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
    static final String JAKARTA_MODULES = "jakarta-modules.properties";

    private static final Map<String, VersionRange> packageToVersions = loadVersions();
    private static final Map<String, String> packageToRenamed = loadRenamed();
    private static final Map<String, String> groupToPackage = loadModules();

    protected DependencyIsValidCheck() {
    }

    public void validate(String... gavs) {
        for (String gav : gavs) {
            if (!apply(gav)) {
                throw new IllegalStateException(gav + " is a violation.");
            }
        }
    }

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
            String renamedPackage = packageToRenamed.get(groupPackageName);
            return (renamedPackage == null);
        } else if (groupPackageName.startsWith("jakarta.")) {
            VersionRange versionRange = packageToVersions.get(groupPackageName);
            if (versionRange != null) {
                return versionRange.containsVersion(gav.toArtifactVersion());
            }
        }

        return true;
    }

    public String toPackage(String group) {
        String packageName = groupToPackage.get(group);
        return (packageName == null) ? group : packageName;

    }

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

    static Map<String, String> loadModules() {
        Map<String, String> result = new LinkedHashMap<>();
        load(JAKARTA_MODULES).forEach((k, v) -> result.put(k.toString(), v.toString()));
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
