/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import io.helidon.build.common.Lists;
import io.helidon.build.common.Strings;
import io.helidon.build.common.xml.XMLElement;

import static io.helidon.build.common.SourcePath.wildcardMatch;

/**
 * Cache config.
 */
public final class CacheConfig {

    private static final Properties EMPTY_PROPS = new Properties();
    static final CacheConfig EMPTY = new CacheConfig(null, EMPTY_PROPS, EMPTY_PROPS);

    private final boolean enabled;
    private final boolean record;
    private final String recordSuffix;
    private final List<String> loadSuffixes;
    private final String reactorRule;
    private final String moduleSet;
    private final boolean enableChecksums;
    private final boolean includeAllChecksums;
    private final List<LifecycleConfig> lifecycleConfig = new ArrayList<>();
    private final List<ReactorRule> reactorRules = new ArrayList<>();

    /**
     * Lifecycle configuration to control executions.
     */
    public static final class LifecycleConfig {

        static final LifecycleConfig EMPTY = new LifecycleConfig(null, null, null, true, List.of(), List.of(), List.of());

        private final String path;
        private final String glob;
        private final String regex;
        private final boolean enabled;
        private final List<String> executionsIncludes;
        private final List<String> executionsExcludes;
        private final List<String> projectFilesExcludes;

        /**
         * Create a new instance.
         *
         * @param path                 project path
         * @param glob                 project glob expression
         * @param regex                project regex
         * @param enabled              enabled
         * @param executionsExcludes   execution excludes
         * @param executionsIncludes   execution includes
         * @param projectFilesExcludes project files excludes
         */
        public LifecycleConfig(String path,
                               String glob,
                               String regex,
                               boolean enabled,
                               List<String> executionsIncludes,
                               List<String> executionsExcludes,
                               List<String> projectFilesExcludes) {
            this.path = path;
            this.glob = glob;
            this.regex = regex;
            this.enabled = enabled;
            this.executionsIncludes = executionsIncludes;
            this.executionsExcludes = executionsExcludes;
            this.projectFilesExcludes = projectFilesExcludes;
        }

        /**
         * Match a project.
         *
         * @param project project to match
         * @return {@code true} if the project matches, {@code false} otherwise
         */
        boolean matches(String project) {
            return project.equals(path)
                   || (glob != null && wildcardMatch("/" + project, glob))
                   || (regex != null && project.matches(regex));
        }

        /**
         * Get the project path.
         *
         * @return path may be {@code null}
         */
        public String path() {
            return path;
        }

        /**
         * Get the project glob.
         *
         * @return glob, may be {@code null}
         */
        public String glob() {
            return glob;
        }

        /**
         * Get the project regex.
         *
         * @return regex, may be {@code null}
         */
        public String regex() {
            return regex;
        }

        /**
         * Get the enabled flag.
         *
         * @return enabled flag
         */
        public boolean enabled() {
            return enabled;
        }

        /**
         * Get the executions includes.
         *
         * @return list of include patterns
         */
        public List<String> executionsIncludes() {
            return executionsIncludes;
        }

        /**
         * Get the executions excludes.
         *
         * @return list of exclude patterns
         */
        public List<String> executionsExcludes() {
            return executionsExcludes;
        }

        /**
         * Get the project files excludes.
         *
         * @return list of exclude patterns
         */
        public List<String> projectFilesExcludes() {
            return projectFilesExcludes;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (LifecycleConfig) obj;
            return Objects.equals(this.path, that.path)
                   && Objects.equals(this.glob, that.glob)
                   && Objects.equals(this.regex, that.regex)
                   && Objects.equals(this.executionsIncludes, that.executionsIncludes)
                   && Objects.equals(this.executionsExcludes, that.executionsExcludes)
                   && Objects.equals(this.projectFilesExcludes, that.projectFilesExcludes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path,
                    glob,
                    regex,
                    executionsIncludes,
                    executionsExcludes,
                    projectFilesExcludes);
        }

        @Override
        public String toString() {
            return "LifecycleConfig["
                   + "path=" + path + ", "
                   + "glob=" + glob + ", "
                   + "regex=" + regex + ", "
                   + "executionsIncludes=" + executionsIncludes + ", "
                   + "executionsExcludes=" + executionsExcludes + ", "
                   + "projectFilesExcludes=" + projectFilesExcludes + ']';
        }
    }

    /**
     * Reactor rule configuration.
     */
    public static final class ReactorRule {
        private final String name;
        private final List<String> profiles;
        private final List<ModuleSet> moduleSets;

        /**
         * @param name       name
         * @param profiles   profiles to activate
         * @param moduleSets moduleSets
         */
        public ReactorRule(String name, List<String> profiles, List<ModuleSet> moduleSets) {
            this.name = name;
            this.profiles = profiles;
            this.moduleSets = moduleSets;
        }

        /**
         * Get the name.
         *
         * @return name
         */
        public String name() {
            return name;
        }

        /**
         * Get the profiles to activate.
         *
         * @return list of profile ids
         */
        public List<String> profiles() {
            return profiles;
        }

        /**
         * Get the moduleSets.
         *
         * @return list of ModuleSet
         */
        public List<ModuleSet> moduleSets() {
            return moduleSets;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ReactorRule) obj;
            return Objects.equals(this.name, that.name)
                   && Objects.equals(this.profiles, that.profiles)
                   && Objects.equals(this.moduleSets, that.moduleSets);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, profiles, moduleSets);
        }

        @Override
        public String toString() {
            return "ReactorRule["
                   + "name=" + name + ", "
                   + "profiles=" + profiles + ", "
                   + "moduleSets=" + moduleSets + ']';
        }
    }

    /**
     * Modules configuration.
     */
    public static final class ModuleSet {
        private final String name;
        private final List<String> includes;
        private final List<String> excludes;

        /**
         * @param name     name
         * @param includes module includes
         * @param excludes module excludes
         */
        public ModuleSet(String name, List<String> includes, List<String> excludes) {
            this.name = name;
            this.includes = includes;
            this.excludes = excludes;
        }

        /**
         * Get the name.
         *
         * @return name
         */
        public String name() {
            return name;
        }

        /**
         * Get the modules includes.
         *
         * @return list of include patterns
         */
        public List<String> includes() {
            return includes;
        }

        /**
         * Get the modules excludes.
         *
         * @return list of exclude patterns
         */
        public List<String> excludes() {
            return excludes;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ModuleSet) obj;
            return Objects.equals(this.name, that.name)
                   && Objects.equals(this.includes, that.includes)
                   && Objects.equals(this.excludes, that.excludes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, includes, excludes);
        }

        @Override
        public String toString() {
            return "ModuleSet["
                   + "name=" + name + ", "
                   + "includes=" + includes + ", "
                   + "excludes=" + excludes + ']';
        }
    }

    CacheConfig(XMLElement xmlElt, Properties sysProps, Properties userProps) {
        boolean enableChecksums = false;
        boolean includeAllChecksums = false;
        String enabledValue = stringProperty(sysProps, userProps, "cache.enabled");
        boolean enabled = parseBoolean(enabledValue, false);
        String recordValue = stringProperty(sysProps, userProps, "cache.record");
        boolean record = parseBoolean(recordValue, true);
        String loadSuffixesValue = stringProperty(sysProps, userProps, "cache.loadSuffixes");
        List<String> loadSuffixes;
        if (loadSuffixesValue != null) {
            loadSuffixes = Arrays.stream(loadSuffixesValue.split(","))
                    .filter(Strings::isValid)
                    .collect(Collectors.toList());
        } else {
            loadSuffixes = List.of();
        }
        String recordSuffix = stringProperty(sysProps, userProps, "cache.recordSuffix");
        if (xmlElt != null) {
            if (enabledValue == null) {
                enabled = booleanElement(xmlElt, "enabled", false);
            }
            if (recordValue == null) {
                record = booleanElement(xmlElt, "record", true);
            }
            if (loadSuffixesValue == null) {
                loadSuffixes = stringListElement(xmlElt, "loadSuffixes");
            }
            if (recordSuffix == null) {
                recordSuffix = xmlElt.child("recordSuffix").map(XMLElement::value).orElse(null);
            }
            XMLElement lifecycleConfigElt = xmlElt.child("lifecycleConfig").orElse(null);
            if (lifecycleConfigElt != null) {
                enableChecksums = booleanElement(lifecycleConfigElt, "enableChecksums", false);
                includeAllChecksums = booleanElement(lifecycleConfigElt, "includeAllChecksums", false);
                for (XMLElement projectElt : lifecycleConfigElt.children("project")) {
                    String path = projectElt.attribute("path", null);
                    String glob = projectElt.attribute("glob", null);
                    String regex = projectElt.attribute("regex", null);
                    boolean projectEnabled = booleanElement(projectElt, "enabled", true);
                    List<String> executionsIncludes = stringListElement(projectElt, "executionsIncludes");
                    List<String> executionsExcludes = stringListElement(projectElt, "executionsExcludes");
                    List<String> projectFilesExcludes = stringListElement(projectElt, "projectFilesExcludes");
                    lifecycleConfig.add(new LifecycleConfig(path, glob, regex, projectEnabled, executionsIncludes,
                            executionsExcludes, projectFilesExcludes));
                }
            }
            reactorRules.addAll(Lists.map(xmlElt.childrenAt("reactorRules", "reactorRule"), r -> {
                String name = r.attribute("name");
                List<String> profiles = stringListElement(r, "profiles");
                List<ModuleSet> moduleSets = Lists.map(r.childrenAt("moduleSets", "moduleSet"), m -> {
                    String moduleSetName = m.attribute("name");
                    List<String> includes = stringListElement(m, "includes");
                    List<String> excludes = stringListElement(m, "excludes");
                    return new ModuleSet(moduleSetName, includes, excludes);
                });
                return new ReactorRule(name, profiles, moduleSets);
            }));
        }
        this.enableChecksums = enableChecksums;
        this.includeAllChecksums = includeAllChecksums;
        this.enabled = enabled;
        this.record = record;
        this.recordSuffix = recordSuffix;
        this.loadSuffixes = Collections.unmodifiableList(loadSuffixes);
        this.reactorRule = stringProperty(sysProps, userProps, "reactorRule");
        this.moduleSet = stringProperty(sysProps, userProps, "moduleSet");
    }

    /**
     * Indicate if the project files checksum should be computed.
     *
     * @return projectFilesChecksums flag
     */
    boolean enableChecksums() {
        return enableChecksums;
    }

    /**
     * Indicate if the all the individual project file checksums should be computed.
     *
     * @return projectFilesChecksums flag
     */
    boolean includeAllChecksums() {
        return includeAllChecksums;
    }

    /**
     * Indicate if the lifecycle extension is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    boolean enabled() {
        return enabled;
    }

    /**
     * Indicate if the state is recorded.
     *
     * @return {@code true} if state is recorded, {@code false} otherwise
     */
    public boolean record() {
        return record;
    }

    /**
     * Get the state file suffixes to load.
     *
     * @return list, never {@code null}
     */
    public List<String> loadSuffixes() {
        return loadSuffixes;
    }

    /**
     * Get suffix to use for the recorded state files.
     *
     * @return Optional, never {@code null}
     */
    public Optional<String> recordSuffix() {
        return Optional.ofNullable(recordSuffix);
    }

    /**
     * Get the {@link ReactorRule} name.
     *
     * @return {@link ReactorRule} name, may be {@code null}
     */
    public String reactorRule() {
        return reactorRule;
    }

    /**
     * Get the {@link ModuleSet} name.
     *
     * @return {@link ModuleSet} name, may be {@code null}
     */
    public String moduleSet() {
        return moduleSet;
    }

    /**
     * Get the life-cycle config.
     *
     * @return list
     */
    List<LifecycleConfig> lifecycleConfig() {
        return lifecycleConfig;
    }

    /**
     * Get the reactor rules.
     *
     * @return map
     */
    List<ReactorRule> reactorRules() {
        return reactorRules;
    }

    private static List<String> stringListElement(XMLElement xmlElt, String eltName) {
        return xmlElt.child(eltName)
                .map(e -> Lists.map(e.children(), XMLElement::value))
                .map(l -> Lists.filter(l, Strings::isValid))
                .orElse(List.of());
    }

    private static boolean booleanElement(XMLElement xmlElt, String eltName, boolean defaultValue) {
        return xmlElt.child(eltName)
                .map(e -> parseBoolean(e.value(), defaultValue))
                .orElse(defaultValue);
    }

    private static boolean parseBoolean(String str, boolean defaultValue) {
        return str == null || str.isEmpty() ? defaultValue : Boolean.parseBoolean(str);
    }

    private static String stringProperty(Properties sysProps, Properties userProps, String prop) {
        String value = sysProps.getProperty(prop);
        return value != null ? value : userProps.getProperty(prop);
    }
}
