/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype.postgenerate;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import static java.util.Collections.emptyMap;

/**
 * A utility to download the Helidon archetype engine with Aether and invoke it.
 */
@SuppressWarnings("unused")
public final class EngineFacade {

    private EngineFacade() {
    }

    private static final String MAVEN_URL_REPO_PROPERTY = "io.helidon.build.common.maven.url.localRepo";
    private static final String MAVEN_CORE_POM_PROPERTIES = "META-INF/maven/org.apache.maven/maven-core/pom.properties";

    /**
     * Generate a project.
     *
     * @param request   archetype generation request
     * @param dependencies Maven coordinates to resolve for running the engine
     */
    @SuppressWarnings("unused")
    public static void generate(ArchetypeGenerationRequest request, List<String> dependencies) {
        checkMavenVersion();
        checkJavaVersion();

        // getRepositorySession only exists in archetype-common >= 3.3.0
        RepositorySystemSession repoSession = EngineFacade.<RepositorySystemSession>invoke(request, "getRepositorySession")
                // getProjectBuildingRequest was removed in archetype-common == 3.3.0
                .or(() -> EngineFacade.<ProjectBuildingRequest>invoke(request, "getProjectBuildingRequest")
                        .map(ProjectBuildingRequest::getRepositorySession))
                .orElseThrow(() -> new IllegalStateException("Unable to get repository system session"));

        // getRemoteRepositories only exists in archetype-common >= 3.3.1
        Aether aether = EngineFacade.<List<RemoteRepository>>invoke(request, "getRemoteRepositories")
                .map(remoteRepos -> new Aether(repoSession, remoteRepos))
                .or(() -> EngineFacade.<List<?>>invoke(request, "getRemoteArtifactRepositories")
                        .map(repos -> {
                            Object repo = repos.isEmpty() ? null : repos.get(0);
                            if (repo instanceof ArtifactRepository) {
                                // getRemoteArtifactRepositories returns List<ArtifactRepository> in archetype-common != 3.3.0
                                return new Aether(repoSession, asListOf(repos, ArtifactRepository.class), true);
                            } else if (repo instanceof RemoteRepository) {
                                // getRemoteArtifactRepositories returns List<RemoteRepository> in archetype-common == 3.3.0
                                return new Aether(repoSession, asListOf(repos, RemoteRepository.class));
                            } else {
                                // empty repository, or unsupported repository type
                                return new Aether(repoSession, List.of());
                            }
                        }))
                .orElseThrow(() -> new IllegalStateException("Unable to initialize aether"));

        File localRepo = aether.repoSession().getLocalRepository().getBasedir();

        // enable mvn:// URL support
        System.setProperty(MAVEN_URL_REPO_PROPERTY, localRepo.getAbsolutePath());

        // create a class-loader with the engine dependencies
        URL[] urls = aether.resolveDependencies(dependencies)
                           .stream()
                           .map(f -> {
                               try {
                                   return f.toURI().toURL();
                               } catch (MalformedURLException e) {
                                   throw new UncheckedIOException(e);
                               }
                           }).toArray(URL[]::new);

        URLClassLoader ecl = new URLClassLoader(urls, EngineFacade.class.getClassLoader());

        Properties archetypeProps = request.getProperties();
        Map<String, String> props = new HashMap<>();
        archetypeProps.stringPropertyNames().forEach(k -> props.put(k, archetypeProps.getProperty(k)));

        // resolve the archetype JAR from the local repository
        File archetypeFile = aether.resolveArtifact(request.getArchetypeGroupId(), request.getArchetypeArtifactId(),
                "jar", request.getArchetypeVersion());

        try {
            FileSystem fileSystem = FileSystems.newFileSystem(archetypeFile.toPath(), EngineFacade.class.getClassLoader());
            Path projectDir = Paths.get(request.getOutputDirectory()).resolve(request.getArtifactId());
            Files.delete(projectDir.resolve("pom.xml"));
            boolean interactiveMode = !"false".equals(System.getProperty("interactiveMode"));
            new ReflectedEngine(ecl, fileSystem, interactiveMode, props, emptyMap(), () -> projectDir).generate();
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    private static String getMavenVersion() {
        // see org.apache.maven.rtinfo.internal.DefaultRuntimeInformation
        Properties props = new Properties();
        try {
            props.load(EngineFacade.class.getClassLoader().getResourceAsStream(MAVEN_CORE_POM_PROPERTIES));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        String version = props.getProperty("version", "").trim();
        return version.startsWith("${") ? "" : version;
    }

    private static void checkMavenVersion() {
        String mavenVersion = getMavenVersion();
        ComparableVersion minMavenVersion = new ComparableVersion("3.2.5");
        if (!mavenVersion.isEmpty() && new ComparableVersion(mavenVersion).compareTo(minMavenVersion) < 0) {
            throw new IllegalStateException("Requires Maven >= 3.2.5");
        }
    }

    private static void checkJavaVersion() {
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

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> invoke(Object object, String methodName) {
        try {
            Method method =  object.getClass().getMethod(methodName);
            return Optional.ofNullable((T) method.invoke(object));
        } catch (NoSuchMethodException ex) {
            return Optional.empty();
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> asListOf(List<?> list, Class<T> type) {
        return (List<T>) list;
    }
}
