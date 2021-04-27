/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.maven.postgenerate;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import io.helidon.build.archetype.engine.v1.ArchetypeEngine;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.project.ProjectBuildingRequest;

import static java.util.stream.Collectors.toMap;

/**
 * A utility to download the Helidon archetype engine with Aether and invoke it.
 */
@SuppressWarnings("unused")
public final class EngineFacade {

    private EngineFacade() {
    }

    private static final String MAVEN_CORE_POM_PROPERTIES = "META-INF/maven/org.apache.maven/maven-core/pom.properties";

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
            if (Runtime.class.getMethod("version") != null
                    && Runtime.Version.class.getMethod("feature") != null
                    && Runtime.getRuntime().version().feature() < 11) {
                throw new IllegalStateException();
            }
        } catch (NoSuchMethodException | IllegalStateException ex) {
            throw new IllegalStateException("Requires Java >= 11");
        } catch (Throwable ex) {
            throw new IllegalStateException("Unable to verify Java version", ex);
        }
    }

    /**
     * Generate a project.
     *
     * @param request   archetype generation request
     * @param engineGAV Helidon archetype engine Maven coordinates
     * @param propNames names of the archetype properties to pass to the Helidon archetype engine
     */
    @SuppressWarnings("unused")
    public static void generate(ArchetypeGenerationRequest request, String engineGAV, List<String> propNames) {
        checkMavenVersion();
        checkJavaVersion();

        ProjectBuildingRequest mavenRequest = request.getProjectBuildingRequest();
        File localRepo = mavenRequest.getRepositorySession().getLocalRepository().getBasedir();
        List<ArtifactRepository> remoteRepos = mavenRequest.getRemoteRepositories();
        Aether aether = new Aether(localRepo, remoteRepos);

        // resolve the helidon engine libs from remote repository

        // create a class-loader with the engine dependencies
        URL[] urls = aether.resolveDependencies(engineGAV)
                           .stream()
                           .map(f -> {
                               try {
                                   return f.toURI().toURL();
                               } catch (MalformedURLException e) {
                                   throw new UncheckedIOException(e);
                               }
                           }).toArray(URL[]::new);

        System.out.println(Arrays.toString(urls));

        URLClassLoader ecl = new URLClassLoader(urls, EngineFacade.class.getClassLoader());
        Properties archetypeProps = request.getProperties();
        Map<String, String> props = new HashMap<>(propNames
                .stream()
                .collect(toMap(Function.identity(), archetypeProps::getProperty)));
        props.put("maven", "true");

        // resolve the archetype JAR from the local repository
        File archetypeFile = aether.resolveArtifact(request.getArchetypeGroupId(), request.getArchetypeArtifactId(),
                "jar", request.getArchetypeVersion());

        // instantiate the engine
        ArchetypeEngine engine;
        try {
            Class<?> engineClass = ecl.loadClass("io.helidon.build.archetype.engine.v1.ArchetypeEngine");
            Constructor<?> engineConstructor = engineClass.getConstructor(File.class, Map.class);
            engine = (ArchetypeEngine) engineConstructor.newInstance(archetypeFile, props);
        } catch (InstantiationException
                | IllegalAccessException
                | NoSuchMethodException
                | ClassNotFoundException ex) {
            throw new IllegalStateException(ex);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(e.getCause());
        }

        File projectDir = new File(request.getOutputDirectory() + "/" + request.getArtifactId());

        // delete place place-holder pom
        new File(projectDir, "pom.xml").delete();

        engine.generate(projectDir);
    }
}
