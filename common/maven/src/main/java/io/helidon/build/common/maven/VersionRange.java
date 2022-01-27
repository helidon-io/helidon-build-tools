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
package io.helidon.build.common.maven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import static io.helidon.build.common.maven.MavenVersion.toMavenVersion;

/**
 * Version Range.
 * Copied and modified from {@code org.apache.maven.artifact.versioning.VersionRange}.
 */
public class VersionRange {

    private static final Map<String, VersionRange> CACHE_SPEC = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<String, VersionRange> CACHE_VERSION = Collections.synchronizedMap(new WeakHashMap<>());

    private final MavenVersion recommendedVersion;
    private final List<Restriction> restrictions;

    private VersionRange(MavenVersion recommendedVersion, List<Restriction> restrictions) {
        this.recommendedVersion = recommendedVersion;
        this.restrictions = restrictions;
    }

    /**
     * <p>
     * Create a version range from a string representation.
     * </p>
     * Some spec examples are:
     * <ul>
     * <li><code>1.0</code> version 1.0 as a recommended version</li>
     * <li><code>[1.0]</code> version 1.0 explicitly only</li>
     * <li><code>[1.0,2.0)</code> versions 1.0 (included) to 2.0 (not included)</li>
     * <li><code>[1.0,2.0]</code> versions 1.0 to 2.0 (both included)</li>
     * <li><code>[1.5,)</code> versions 1.5 and higher</li>
     * <li><code>(,1.0],[1.2,)</code> versions up to 1.0 (included) and 1.2 or higher</li>
     * </ul>
     *
     * @param spec string representation of a version or version range
     * @return a new {@link VersionRange} object that represents the spec
     * @throws IllegalArgumentException if invalid version specification
     */
    public static VersionRange createFromVersionSpec(String spec) {
        if (spec == null) {
            return null;
        }

        VersionRange cached = CACHE_SPEC.get(spec);
        if (cached != null) {
            return cached;
        }

        List<Restriction> restrictions = new ArrayList<>();
        String process = spec;
        MavenVersion version = null;
        MavenVersion upperBound = null;
        MavenVersion lowerBound = null;

        while (process.startsWith("[") || process.startsWith("(")) {
            int index1 = process.indexOf(')');
            int index2 = process.indexOf(']');

            int index = index2;
            if (index2 < 0 || index1 < index2) {
                if (index1 >= 0) {
                    index = index1;
                }
            }

            if (index < 0) {
                throw new IllegalArgumentException("Unbounded range: " + spec);
            }

            Restriction restriction = parseRestriction(process.substring(0, index + 1));
            if (lowerBound == null) {
                lowerBound = restriction.getLowerBound();
            }
            if (upperBound != null) {
                if (restriction.getLowerBound() == null || restriction.getLowerBound().compareTo(upperBound) < 0) {
                    throw new IllegalArgumentException("Ranges overlap: " + spec);
                }
            }
            restrictions.add(restriction);
            upperBound = restriction.getUpperBound();

            process = process.substring(index + 1).trim();

            if (process.startsWith(",")) {
                process = process.substring(1).trim();
            }
        }

        if (process.length() > 0) {
            if (restrictions.size() > 0) {
                throw new IllegalArgumentException("Only fully-qualified sets allowed in multiple set scenario: " + spec);
            } else {
                version = toMavenVersion(process);
                restrictions.add(Restriction.EVERYTHING);
            }
        }

        cached = new VersionRange(version, restrictions);
        CACHE_SPEC.put(spec, cached);
        return cached;
    }

    /**
     * Select the greatest matching version from the given list, if any.
     *
     * @param versions The versions.
     * @return The version, {@code null} if none match.
     */
    public MavenVersion matchVersion(List<MavenVersion> versions) {
        return versions.stream()
                       .filter(this::containsVersion)
                       .max(Comparator.naturalOrder())
                       .orElse(null);
    }

    /**
     * Returns whether the given version is within this range.
     *
     * @param version The version.
     * @return {@code true} if in range.
     */
    public boolean containsVersion(MavenVersion version) {
        for (Restriction restriction : restrictions) {
            if (restriction.containsVersion(version)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VersionRange)) {
            return false;
        }
        VersionRange other = (VersionRange) obj;

        return Objects.equals(recommendedVersion, other.recommendedVersion)
               && Objects.equals(restrictions, other.restrictions);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (recommendedVersion == null ? 0 : recommendedVersion.hashCode());
        hash = 31 * hash + (restrictions == null ? 0 : restrictions.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        if (recommendedVersion != null) {
            return recommendedVersion.toString();
        } else {
            StringBuilder buf = new StringBuilder();
            for (Restriction r : restrictions) {
                if (buf.length() > 0) {
                    buf.append(',');
                }
                buf.append(r.toString());
            }
            return buf.toString();
        }
    }

    private static Restriction parseRestriction(String spec) {
        boolean lowerBoundInclusive = spec.startsWith("[");
        boolean upperBoundInclusive = spec.endsWith("]");

        String process = spec.substring(1, spec.length() - 1).trim();

        Restriction restriction;

        int index = process.indexOf(',');

        if (index < 0) {
            if (!lowerBoundInclusive || !upperBoundInclusive) {
                throw new IllegalArgumentException("Single version must be surrounded by []: " + spec);
            }

            MavenVersion version = toMavenVersion(process);

            restriction = new Restriction(version, lowerBoundInclusive, version, upperBoundInclusive);
        } else {
            String lowerBound = process.substring(0, index).trim();
            String upperBound = process.substring(index + 1).trim();
            if (lowerBound.equals(upperBound)) {
                throw new IllegalArgumentException("Range cannot have identical boundaries: " + spec);
            }

            MavenVersion lowerVersion = null;
            if (lowerBound.length() > 0) {
                lowerVersion = toMavenVersion(lowerBound);
            }
            MavenVersion upperVersion = null;
            if (upperBound.length() > 0) {
                upperVersion = toMavenVersion(upperBound);
            }

            if (upperVersion != null && lowerVersion != null && upperVersion.compareTo(lowerVersion) < 0) {
                throw new IllegalArgumentException("Range defies version ordering: " + spec);
            }

            restriction = new Restriction(lowerVersion, lowerBoundInclusive, upperVersion, upperBoundInclusive);
        }

        return restriction;
    }

    private static class Restriction {
        private final MavenVersion lowerBound;
        private final boolean lowerBoundInclusive;
        private final MavenVersion upperBound;
        private final boolean upperBoundInclusive;
        public static final Restriction EVERYTHING = new Restriction(null, false, null, false);

        Restriction(MavenVersion lowerBound, boolean lowerBoundInclusive, MavenVersion upperBound,
                    boolean upperBoundInclusive) {
            this.lowerBound = lowerBound;
            this.lowerBoundInclusive = lowerBoundInclusive;
            this.upperBound = upperBound;
            this.upperBoundInclusive = upperBoundInclusive;
        }

        public MavenVersion getLowerBound() {
            return lowerBound;
        }

        public boolean isLowerBoundInclusive() {
            return lowerBoundInclusive;
        }

        public MavenVersion getUpperBound() {
            return upperBound;
        }

        public boolean isUpperBoundInclusive() {
            return upperBoundInclusive;
        }

        public boolean containsVersion(MavenVersion version) {
            if (lowerBound != null) {
                int comparison = lowerBound.compareTo(version);

                if ((comparison == 0) && !lowerBoundInclusive) {
                    return false;
                }
                if (comparison > 0) {
                    return false;
                }
            }
            if (upperBound != null) {
                int comparison = upperBound.compareTo(version);

                if ((comparison == 0) && !upperBoundInclusive) {
                    return false;
                }
                return comparison >= 0;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = 13;

            if (lowerBound == null) {
                result += 1;
            } else {
                result += lowerBound.hashCode();
            }

            result *= lowerBoundInclusive ? 1 : 2;

            if (upperBound == null) {
                result -= 3;
            } else {
                result -= upperBound.hashCode();
            }

            result *= upperBoundInclusive ? 2 : 3;

            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof Restriction)) {
                return false;
            }

            Restriction restriction = (Restriction) other;
            if (lowerBound != null) {
                if (!lowerBound.equals(restriction.lowerBound)) {
                    return false;
                }
            } else if (restriction.lowerBound != null) {
                return false;
            }

            if (lowerBoundInclusive != restriction.lowerBoundInclusive) {
                return false;
            }

            if (upperBound != null) {
                if (!upperBound.equals(restriction.upperBound)) {
                    return false;
                }
            } else if (restriction.upperBound != null) {
                return false;
            }

            return upperBoundInclusive == restriction.upperBoundInclusive;

        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();

            buf.append(isLowerBoundInclusive() ? '[' : '(');
            if (getLowerBound() != null) {
                buf.append(getLowerBound());
            }
            buf.append(',');
            if (getUpperBound() != null) {
                buf.append(getUpperBound());
            }
            buf.append(isUpperBoundInclusive() ? ']' : ')');

            return buf.toString();
        }
    }
}
