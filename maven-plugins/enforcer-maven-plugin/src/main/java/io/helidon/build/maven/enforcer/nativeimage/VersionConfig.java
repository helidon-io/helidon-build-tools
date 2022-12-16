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

import java.util.List;

import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.maven.enforcer.RuleFailure;

import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;

/**
 * Version configuration.
 */
public class VersionConfig {

    private String version;
    private String matcher;

    public String getVersion() {
        return version;
    }

    public String getMatcher() {
        return matcher;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    private final List<String> availableMatcher = List.of(
            "greaterThan",
            "lessThan",
            "greaterThanOrEqualTo",
            "lessThanOrEqualTo");

    /**
     * Check versions against rule configuration.
     *
     * @param nativeVersion native-image version
     * @param failures      list of errors
     */
    void checkVersion(MavenVersion nativeVersion, List<RuleFailure> failures) {
        MavenVersion ruleVersion = toMavenVersion(getVersion());
        boolean success;
        switch (matcher.toLowerCase()) {
            case "greaterthan":
                success = nativeVersion.isGreaterThan(ruleVersion);
                break;
            case "lessthan":
                success = nativeVersion.isLessThan(ruleVersion);
                break;
            case "greaterthanorequalto":
                success = nativeVersion.isGreaterThanOrEqualTo(ruleVersion);
                break;
            case "lessthanorequalto":
                success = nativeVersion.isLessThanOrEqualTo(ruleVersion);
                break;
            default:
                throw new EnforcerNativeImageException(matcher, availableMatcher);
        }
        if (!success) {
            failures.add(RuleFailure.create(errorMessage(matcher, nativeVersion, ruleVersion)));
        }
    }

    private String errorMessage(String match, MavenVersion nativeVersion, MavenVersion ruleVersion) {
        return String.format("native image version %s is not %s %s rule version.", nativeVersion, match, ruleVersion);
    }

    @Override
    public String toString() {
        return "VersionConfig{"
                + "version=" + version
                + ", matcher=" + matcher
                + "}";
    }
}
