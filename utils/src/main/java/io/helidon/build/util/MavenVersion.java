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

import java.util.function.Predicate;

import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * A Maven version number.
 */
public class MavenVersion extends ComparableVersion {
    private final boolean qualified;

    private MavenVersion(String version) {
        super(version);
        this.qualified = version.contains("-");
    }

    /**
     * Returns a new instance from the given version.
     *
     * @param version The version.
     * @return The instance.
     */
    public static MavenVersion toMavenVersion(String version) {
        return new MavenVersion(version);
    }

    /**
     * Returns a predicate that checks if the version being tested is both not qualified and is
     * greater than or equal to the given version.
     *
     * @param minimumVersion The minimum version.
     * @return The predicate.
     */
    public static Predicate<MavenVersion> unqualifiedMinimum(String minimumVersion) {
        return notQualified().and(greaterThanOrEqualTo(minimumVersion));
    }

    /**
     * Returns a predicate that checks if the version being tested is not {@link #isQualified() qualified}.
     *
     * @return The predicate.
     */
    public static Predicate<MavenVersion> notQualified() {
        return v -> !v.isQualified();
    }

    /**
     * Returns a predicate that checks if the version being tested is less than to the given version.
     *
     * @param version The version.
     * @return The predicate.
     */
    public static Predicate<MavenVersion> lessThan(String version) {
        final MavenVersion mavenVersion = new MavenVersion(version);
        return v -> v.isLessThan(mavenVersion);
    }

    /**
     * Returns a predicate that checks if the version being tested is less than or equal to the given version.
     *
     * @param version The version.
     * @return The predicate.
     */
    public static Predicate<MavenVersion> lessThanOrEqualTo(String version) {
        final MavenVersion mavenVersion = new MavenVersion(version);
        return v -> v.isLessThanOrEqualTo(mavenVersion);
    }

    /**
     * Returns a predicate that checks if the version being tested is greater than to the given version.
     *
     * @param version The version.
     * @return The predicate.
     */
    public static Predicate<MavenVersion> greaterThan(String version) {
        final MavenVersion mavenVersion = new MavenVersion(version);
        return v -> v.isGreaterThan(mavenVersion);
    }

    /**
     * Returns a predicate that checks if the version being tested is greater than or equal to the given version.
     *
     * @param version The version.
     * @return The predicate.
     */
    public static Predicate<MavenVersion> greaterThanOrEqualTo(String version) {
        final MavenVersion mavenVersion = new MavenVersion(version);
        return v -> v.isGreaterThanOrEqualTo(mavenVersion);
    }

    /**
     * Tests whether or not this version is qualified, i.e. contains a '-' character.
     *
     * @return {@code true} if qualified.
     */
    public boolean isQualified() {
        return qualified;
    }

    /**
     * Tests whether or not this version is less than the given version.
     *
     * @param other The version to compare to.
     * @return {@code true} if less than.
     */
    public boolean isLessThan(MavenVersion other) {
        return this.compareTo(other) < 0;
    }

    /**
     * Tests whether or not this version is less than or equal to the given version.
     *
     * @param other The version to compare to.
     * @return {@code true} if less than or equal.
     */
    public boolean isLessThanOrEqualTo(MavenVersion other) {
        return this.compareTo(other) <= 0;
    }

    /**
     * Tests whether or not this version is greater than the given version.
     *
     * @param other The version to compare to.
     * @return {@code true} if greater than.
     */
    public boolean isGreaterThan(MavenVersion other) {
        return this.compareTo(other) > 0;
    }

    /**
     * Tests whether or not this version is greater than or equal to the given version.
     *
     * @param other The version to compare to.
     * @return {@code true} if greater than or equal.
     */
    public boolean isGreaterThanOrEqualTo(MavenVersion other) {
        return this.compareTo(other) >= 0;
    }
}
