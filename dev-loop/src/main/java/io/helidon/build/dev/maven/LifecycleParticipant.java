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

package io.helidon.build.dev.maven;

import java.util.List;
import java.util.stream.Collectors;

import io.helidon.build.util.ConfigProperties;
import io.helidon.build.util.Log;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

import static io.helidon.build.dev.maven.MavenProjectSupplier.DOT_HELIDON;
import static io.helidon.build.dev.maven.MavenProjectSupplier.PROJECT_CLASSESDIRS;
import static io.helidon.build.dev.maven.MavenProjectSupplier.PROJECT_CLASSPATH;
import static io.helidon.build.dev.maven.MavenProjectSupplier.PROJECT_MAINCLASS;
import static io.helidon.build.dev.maven.MavenProjectSupplier.PROJECT_RESOURCESDIRS;
import static io.helidon.build.dev.maven.MavenProjectSupplier.PROJECT_SOURCEDIRS;

/**
 * Collects settings from a maven project and stores them in the a config file for later use
 * by {@link MavenProjectSupplier}. Must be installed as a maven extension to run.
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class LifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final String MAINCLASS_PROPERTY = "mainClass";

    private final ConfigProperties properties = new ConfigProperties(DOT_HELIDON);

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        Log.debug("LifecycleParticipant: session end");

        List<MavenProject> projects = session.getProjects();
        if (projects.size() != 1) {
            throw new RuntimeException("Unable to process Maven project(s)");
        }

        MavenProject mavenProject = projects.get(0);
        try {
            properties.property(PROJECT_CLASSPATH, mavenProject.getRuntimeClasspathElements());
            properties.property(PROJECT_SOURCEDIRS, mavenProject.getCompileSourceRoots());
            List<String> classesDirs = mavenProject.getCompileClasspathElements()
                    .stream()
                    .filter(d -> !d.endsWith(".jar"))
                    .collect(Collectors.toList());
            properties.property(PROJECT_CLASSESDIRS, classesDirs);
            List<String> resourceDirs = mavenProject.getResources()
                    .stream()
                    .map(Resource::getDirectory)
                    .collect(Collectors.toList());
            properties.property(PROJECT_RESOURCESDIRS, resourceDirs);
            String mainClass = mavenProject.getProperties().getProperty(MAINCLASS_PROPERTY);
            if (mainClass == null) {
                throw new RuntimeException("Unable to find property " + MAINCLASS_PROPERTY);
            }
            properties.property(PROJECT_MAINCLASS, mainClass);
            properties.store();
        } catch (DependencyResolutionRequiredException e) {
            throw new RuntimeException(e);
        }
    }
}
