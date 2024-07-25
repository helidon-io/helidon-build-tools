/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import io.helidon.build.common.SourcePath;
import io.helidon.build.maven.cache.CacheConfig.ModuleSet;
import io.helidon.build.maven.cache.CacheConfig.ReactorRule;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.graph.DefaultProjectDependencyGraph;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.logging.Logger;

import static java.util.stream.Collectors.toList;

/**
 * Reactor manager.
 */
@Named
@SessionScoped
public class ReactorManager {

    @Inject
    private MavenSession session;

    @Inject
    private CacheConfigManager configManager;

    @Inject
    private Logger logger;

    /**
     * Update the session to filter the reactor.
     */
    public void afterProjectsRead() {
        if (disabled()) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Processing reactor...");
        }

        List<SourcePath> allPaths = session.getAllProjects().stream()
                .map(this::projectPath)
                .collect(Collectors.toList());
        Set<SourcePath> selectPaths = new HashSet<>();
        Map<String, List<SourcePath>> moduleSets = new HashMap<>();
        ReactorRule reactorRule = configManager.reactorRule();
        String moduleSetName = configManager.cacheConfig().moduleSet();

        // process all moduleSets in order
        for (ModuleSet moduleSet : reactorRule.moduleSets()) {
            List<SourcePath> unselected = allPaths.stream().filter(p -> !selectPaths.contains(p)).collect(toList());
            List<SourcePath> paths = SourcePath.filter(unselected, moduleSet.includes(), moduleSet.excludes());
            selectPaths.addAll(paths);
            moduleSets.put(moduleSet.name(), paths);
        }

        // filter the original projects list to retain the ordering
        List<SourcePath> moduleSet = moduleSets.get(moduleSetName);
        List<MavenProject> projects;
        if (moduleSet == null) {
            throw new IllegalStateException("ModuleSet not found: " + moduleSetName);
        } else if (moduleSet.isEmpty()) {
            throw new IllegalStateException("Resolved moduleSet is empty: " + moduleSetName);
        } else {
            projects = session.getAllProjects().stream()
                    .filter(p -> moduleSet.contains(projectPath(p)))
                    .collect(toList());
        }

        // update the session
        try {
            ProjectDependencyGraph graph = new DefaultProjectDependencyGraph(session.getAllProjects(), projects);
            session.setProjects(graph.getSortedProjects());
            session.setAllProjects(graph.getAllProjects());
            session.setProjectDependencyGraph(graph);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Process the session before the models are built.
     */
    public void afterSessionStart() {
        if (disabled()) {
            return;
        }
        ReactorRule reactorRule = configManager.reactorRule();
        String moduleSet = configManager.cacheConfig().moduleSet();
        logger.info(String.format("Filtering modules {reactorRule=%s, moduleSet=%s}", reactorRule.name(), moduleSet));
        if (!reactorRule.profiles().isEmpty()) {
            logger.info("Activating profiles: " + reactorRule.profiles());
            reactorRule.profiles().forEach(session.getRequest()::addActiveProfile);
            ProjectBuildingRequest pbr = session.getProjectBuildingRequest();
            Set<String> profileIds = new HashSet<>(pbr.getActiveProfileIds());
            pbr.setActiveProfileIds(new ArrayList<>(profileIds));
        }
    }

    private boolean disabled() {
        CacheConfig cacheConfig = configManager.cacheConfig();
        ReactorRule reactorRule = configManager.reactorRule();
        String moduleSetName = cacheConfig.moduleSet();
        return !cacheConfig.enabled() || reactorRule == null || moduleSetName == null || moduleSetName.isEmpty();
    }

    private SourcePath projectPath(MavenProject project) {
        Path path = root().relativize(project.getFile().toPath().toAbsolutePath());
        return new SourcePath(path);
    }

    private Path root() {
        return session.getRequest().getMultiModuleProjectDirectory().toPath();
    }
}
