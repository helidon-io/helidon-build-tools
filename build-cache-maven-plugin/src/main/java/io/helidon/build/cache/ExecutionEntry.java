/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.build.cache;

import java.util.List;
import java.util.Objects;

import io.helidon.build.common.SourcePath;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Execution entry.
 */
final class ExecutionEntry {

    private static final List<String> DEFAULT_INCLUDES = List.of("*");
    static final String XML_ELEMENT_NAME = "execution";

    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String goal;
    private final String executionId;
    private final ConfigNode configuration;

    /**
     * Create a new execution entry instance.
     *
     * @param groupId       plugin groupId
     * @param artifactId    plugin artifactId
     * @param version       plugin version
     * @param goal          goal
     * @param executionId   executionId
     * @param configuration configuration
     */
    ExecutionEntry(String groupId,
                   String artifactId,
                   String version,
                   String goal,
                   String executionId,
                   ConfigNode configuration) {

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.goal = goal;
        this.executionId = executionId;
        this.configuration = configuration;
    }

    /**
     * Get the groupId.
     *
     * @return groupId
     */
    String groupId() {
        return groupId;
    }

    /**
     * Get the artifactId.
     *
     * @return artifactId
     */
    String artifactId() {
        return artifactId;
    }

    /**
     * Get the version.
     *
     * @return version
     */
    String version() {
        return version;
    }

    /**
     * Get the executionId.
     *
     * @return executionId
     */
    String executionId() {
        return executionId;
    }

    /**
     * Get the goal.
     *
     * @return goal
     */
    String goal() {
        return goal;
    }

    /**
     * Get the execution configuration.
     *
     * @return configuration
     */
    ConfigNode config() {
        return configuration;
    }

    /**
     * Test if this execution matches an other execution.
     * Matching executions are identical except for the configuration.
     *
     * @param execution execution
     * @return {@code true} if matches, {@code false} otherwise
     */
    boolean matches(ExecutionEntry execution) {
        return groupId.equals(execution.groupId)
                && artifactId.equals(execution.artifactId)
                && version.equals(execution.version)
                && goal.equals(execution.goal)
                && executionId.equals(execution.executionId);
    }

    /**
     * Test if this execution matches a plugin execution.
     *
     * @param plugin plugin
     * @param goal   plugin execution goal
     * @param id     plugin execution id
     * @return {@code true} if matches, {@code false} otherwise
     */
    boolean matches(Plugin plugin, String goal, String id) {
        return groupId.equals(plugin.getGroupId())
                && artifactId.equals(plugin.getArtifactId())
                && version.equals(plugin.getVersion())
                && this.goal.equals(goal)
                && executionId.equals(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionEntry execution = (ExecutionEntry) o;
        return groupId.equals(execution.groupId)
                && artifactId.equals(execution.artifactId)
                && version.equals(execution.version)
                && goal.equals(execution.goal)
                && executionId.equals(execution.executionId)
                && Objects.equals(configuration, execution.configuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, goal, executionId, configuration);
    }

    /**
     * Get the name of this execution.
     *
     * @return name
     */
    String name() {
        return groupId + ":" + artifactId + ":" + version + ":" + goal + "@" + executionId;
    }

    /**
     * Match the execution name with the given include and exclude patterns.
     *
     * @param includes include patterns
     * @param excludes exclude patterns
     * @return {@code true} if matched, {@code false} otherwise
     */
    boolean match(List<String> includes, List<String> excludes) {
        if (includes == null || includes.isEmpty()) {
            includes = DEFAULT_INCLUDES;
        }
        String execName = name();
        for (String include : includes) {
            if (!SourcePath.wildcardMatch(execName, include)) {
                return false;
            }
        }
        if (excludes != null) {
            for (String exclude : excludes) {
                if (SourcePath.wildcardMatch(execName, exclude)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "ExecutionEntry{"
                + "groupId='" + groupId + '\''
                + ", artifactId='" + artifactId + '\''
                + ", version='" + version + '\''
                + ", goal='" + goal + '\''
                + ", executionId='" + executionId + '\''
                + ", configuration='" + configuration + '\''
                + '}';
    }

    /**
     * Convert this instance to an XML DOM element.
     *
     * @return Xpp3Dom
     */
    Xpp3Dom toXml() {
        Xpp3Dom elt = new Xpp3Dom(XML_ELEMENT_NAME);
        elt.setAttribute("groupId", groupId);
        elt.setAttribute("artifactId", artifactId);
        elt.setAttribute("version", version);
        elt.setAttribute("goal", goal);
        elt.setAttribute("id", executionId);
        if (configuration != null) {
            elt.addChild(configuration.toXpp3Dom());
        }
        return elt;
    }

    /**
     * Create an instance from an XML DOM element.
     *
     * @param elt XML DOM element
     * @return ExecutionEntry
     */
    static ExecutionEntry fromXml(Xpp3Dom elt) {
        ConfigNode config = new ConfigNode(ConfigAdapters.create(elt.getChild("configuration")), null);
        return new ExecutionEntry(
                elt.getAttribute("groupId"),
                elt.getAttribute("artifactId"),
                elt.getAttribute("version"),
                elt.getAttribute("goal"),
                elt.getAttribute("id"),
                config);
    }

    /**
     * Create an execution entry from a mojo execution.
     *
     * @param execution     mojo execution
     * @param configuration config node root
     * @return ExecutionEntry
     */
    static ExecutionEntry create(MojoExecution execution, ConfigNode configuration) {
        return new ExecutionEntry(execution.getGroupId(), execution.getArtifactId(), execution.getVersion(),
                execution.getGoal(), execution.getExecutionId(), configuration);
    }
}
