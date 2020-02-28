/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
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

package io.helidon.build.maven;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Maven goal to get the directory of the top level module.
 * This plugin will find the top most directory, even if the pom file in that
 * directory is not the top level module.
 */
@Mojo(name = "root-dir",
      defaultPhase = LifecyclePhase.VALIDATE,
      threadSafe = true)
public class RootDirMojo extends AbstractMojo {
    private static final String CONTEXT_KEY = "io.helidon.build.maven.RootDirMojo.RootDir";
    private static final String PROPERTY_NAME = "top.parent.basedir";
    /**
     * Plexus build context used to get the scanner for scanning resources.
     */
    @Component
    private BuildContext buildContext;

    /**
     * The Maven project this mojo executes on.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws MojoFailureException {
        String rootDir = (String) getPluginContext().get(CONTEXT_KEY);
        if (null == rootDir) {
            rootDir = findRootDir();
            getPluginContext().put(CONTEXT_KEY, rootDir);
        }

        project.getProperties().setProperty(PROPERTY_NAME, rootDir);

        if (getLog().isDebugEnabled()) {
            final StringWriter str = new StringWriter();
            project.getProperties().list(new PrintWriter(str));
            getLog().debug("All project properties:\n\n" + str);
        }

        getLog().info("Root directory configured to " + rootDir);
    }

    private String findRootDir() throws MojoFailureException {
        Path currentTopLevel = null;

        Map<Integer, Set<Path>> levelToPaths = new HashMap<>();
        Collection<MavenProject> mavenProjects = gatherWholeReactor();

        for (MavenProject current : mavenProjects) {
            Path basedir = current.getBasedir().toPath().toAbsolutePath().normalize();

            if (basedir == null) {
                getLog().info("Basedir of " + current.getName() + " is null");
                continue;
            }

            if (currentTopLevel == null) {
                currentTopLevel = basedir;
                continue;
            }

            // there is an existing top level, let's see if current is higher
            if (basedir.getNameCount() < currentTopLevel.getNameCount()) {
                currentTopLevel = basedir;
                levelToPaths.computeIfAbsent(basedir.getNameCount(), it -> new HashSet<>())
                        .add(basedir);
            }
        }

        if (null == currentTopLevel) {
            throw new MojoFailureException("Failed to identify root path for module: " + project.getName());
        }

        Set<Path> sameLevel = levelToPaths.get(currentTopLevel.getNameCount());
        if (sameLevel.size() > 1) {
            throw new MojoFailureException("There is more than one root of your reactor: " + sameLevel);
        }

        return currentTopLevel.toString();
    }

    private Collection<MavenProject> gatherWholeReactor() {
        Set<MavenProject> toProcess = Collections.newSetFromMap(new IdentityHashMap<>());
        for (MavenProject reactorProject : reactorProjects) {
            toProcess.add(reactorProject);
            MavenProject parent = reactorProject.getParent();
            while (parent != null) {
                toProcess.add(parent);
                parent = parent.getParent();
            }
        }

        getLog().info("Processing " + toProcess.size() + " project to find root");
        return toProcess;
    }
}

