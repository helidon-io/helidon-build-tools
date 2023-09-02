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

import java.util.Objects;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

/**
 * A simple group-artifact-version.
 */
public class Gav {
    private final String group;
    private final String artifact;
    private final String version;

    protected Gav(String g,
                  String a,
                  String v) {
        group = Objects.requireNonNull(g);
        artifact = a;
        version = v;

        assert (!group.isBlank());
    }

    protected Gav(MavenCoordinate c) {
        this(c.getGroupId(), c.getArtifactId(), c.getVersion());
    }

    @Override
    public String toString() {
        return toCanonicalName();
    }

    /**
     * The group id.
     *
     * @return group id
     */
    public String group() {
        assert (group != null && !group.isBlank());
        return group;
    }

    /**
     * The artifact id.
     *
     * @return artifact id
     */
    public String artifact() {
        assert (artifact != null && !artifact.isBlank());
        return artifact;
    }

    /**
     * The version id.
     *
     * @return version id
     */
    public String version() {
        assert (version != null && !version.isBlank());
        return version;
    }

    String toCanonicalName() {
        return group() + ":" + artifact() + ":" + version();
    }

    ArtifactVersion toArtifactVersion() {
        return new DefaultArtifactVersion(version());
    }

    static Gav create(String gav) {
        String[] split = gav.split(":");
        return new Gav(split[0], split.length > 1 ? split[1] : null, split.length > 2 ? split[2] : null);
    }

    static Gav create(MavenCoordinate gav) {
        return new Gav(gav);
    }

}
