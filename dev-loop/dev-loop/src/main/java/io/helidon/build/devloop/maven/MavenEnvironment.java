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
package io.helidon.build.devloop.maven;

import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.LifecycleMappingDelegate;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;

/**
 * An accessor for various Maven components.
 */
public class MavenEnvironment {
    private final MavenProject project;
    private final MavenSession session;
    private final MojoDescriptorCreator mojoDescriptorCreator;
    private final DefaultLifecycles defaultLifeCycles;
    private final LifecycleMappingDelegate standardLifecycleDelegate;
    private final Map<String, LifecycleMappingDelegate> lifecycleDelegates;
    private final BuildPluginManager buildPluginManager;

    /**
     * Constructor.
     *
     * @param project The project.
     * @param session The session.
     * @param mojoDescriptorCreator The mojo descriptor creator.
     * @param defaultLifeCycles The default lifecycles.
     * @param standardLifecycleDelegate The standard lifecycle mapping delegate.
     * @param lifecycleDelegates The lifecycle delegates, by phase.
     * @param buildPluginManager The build plugin manager.
     */
    public MavenEnvironment(MavenProject project,
                            MavenSession session,
                            MojoDescriptorCreator mojoDescriptorCreator,
                            DefaultLifecycles defaultLifeCycles,
                            LifecycleMappingDelegate standardLifecycleDelegate,
                            Map<String, LifecycleMappingDelegate> lifecycleDelegates,
                            BuildPluginManager buildPluginManager) {
        this.project = project;
        this.session = session;
        this.mojoDescriptorCreator = mojoDescriptorCreator;
        this.defaultLifeCycles = defaultLifeCycles;
        this.standardLifecycleDelegate = standardLifecycleDelegate;
        this.lifecycleDelegates = lifecycleDelegates;
        this.buildPluginManager = buildPluginManager;
    }

    /**
     * Returns the project.
     *
     * @return The project.
     */
    public MavenProject project() {
        return project;
    }

    /**
     * Returns the session.
     *
     * @return The session.
     */
    public MavenSession session() {
        return session;
    }

    /**
     * Returns the mojo descriptor creator.
     *
     * @return The creator.
     */
    public MojoDescriptorCreator mojoDescriptorCreator() {
        return mojoDescriptorCreator;
    }

    /**
     * Returns the default lifecycles.
     *
     * @return The lifecycles.
     */
    public DefaultLifecycles defaultLifeCycles() {
        return defaultLifeCycles;
    }

    /**
     * Returns the standard lifecycle delegate.
     *
     * @return THe delegate.
     */
    public LifecycleMappingDelegate standardLifecycleDelegate() {
        return standardLifecycleDelegate;
    }

    /**
     * Returns the lifecycle delegates, by phase.
     *
     * @return The delegates.
     */
    public Map<String, LifecycleMappingDelegate> lifecycleDelegates() {
        return lifecycleDelegates;
    }

    /**
     * Returns the build plugin manager.
     *
     * @return The manager.
     */
    public BuildPluginManager buildPluginManager() {
        return buildPluginManager;
    }
}
