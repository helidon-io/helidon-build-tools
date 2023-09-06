/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

class SimpleEngineFacade {
    private static final String MAVEN_URL_REPO_PROPERTY = "io.helidon.build.common.maven.url.localRepo";
    private static final String MAVEN_CORE_POM_PROPERTIES = "META-INF/maven/org.apache.maven/maven-core/pom.properties";

    static void checkMavenVersion() {
        String mavenVersion = getMavenVersion();
        ComparableVersion minMavenVersion = new ComparableVersion("3.2.5");
        if (!mavenVersion.isEmpty() && new ComparableVersion(mavenVersion).compareTo(minMavenVersion) < 0) {
            throw new IllegalStateException("Requires Maven >= 3.2.5");
        }
    }

    static void checkJavaVersion() {
        try {
            // using reflection to run on Java 8
            Method versionMethod = Runtime.class.getMethod("version");
            Runtime runtime = Runtime.getRuntime();
            Object version = versionMethod.invoke(runtime);
            Method featureMethod = version.getClass().getMethod("feature");
            Object feature = featureMethod.invoke(version);
            if (feature instanceof Integer && (int) feature < 11) {
                throw new IllegalStateException();
            }
        } catch (NoSuchMethodException | IllegalStateException ex) {
            throw new IllegalStateException("Requires Java >= 11");
        } catch (Throwable ex) {
            throw new IllegalStateException("Unable to verify Java version", ex);
        }
    }

    static File getLocalRepository(MavenSession session) {
        if (session != null) {
            return new File(session.getLocalRepository().getBasedir());
        }

        File baseDir = new File(System.getProperty("user.home"), "/m2/repository");
        assert (baseDir.isDirectory());
        return baseDir;
    }

    static List<ArtifactRepository> getRemoteArtifactRepositories(MavenProject project) {
        if (project != null) {
            return project.getRemoteArtifactRepositories();
        }

        return List.of();
    }

    private static String getMavenVersion() {
        // see org.apache.maven.rtinfo.internal.DefaultRuntimeInformation
        Properties props = new Properties();
        try {
            props.load(SimpleEngineFacade.class.getClassLoader().getResourceAsStream(MAVEN_CORE_POM_PROPERTIES));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        String version = props.getProperty("version", "").trim();
        return version.startsWith("${") ? "" : version;
    }

}
