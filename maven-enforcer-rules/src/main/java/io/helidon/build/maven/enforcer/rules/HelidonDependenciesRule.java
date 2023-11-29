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

package io.helidon.build.maven.enforcer.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

/**
 * This rule will apply the same basic "eclipse transformer" checks, but applied as an enforcer rule.
 */
// see https://maven.apache.org/enforcer/enforcer-api/writing-a-custom-rule.html
// see https://projects.eclipse.org/projects/technology.transformer
@Named("helidonDependenciesRule") // rule name - must start from lowercase character
@SuppressWarnings("unused")
public class HelidonDependenciesRule extends AbstractEnforcerRule {
    /**
     * An appropriate value for {@link #namespace} (value={@value}).
     */
    static final String JAVAX = "javax";
    /**
     * An appropriate value for {@link #namespace} (value={@value}).
     */
    static final String JAKARTA = "jakarta";

    @Inject
    private MavenProject project;

    @Inject
    private MavenSession session;

    /**
     * Rule parameter as list of gav regular expressions. Reflectively injected from pom.xml
     */
    // @Inject // injected by maven
    private List<String> excludedGavRegExs;

    /**
     * Rule parameter string. The enforcement policy can either be {@link #JAVAX} or {@link #JAKARTA}.
     * Reflectively injected from pom.xml. The default is {@link #JAKARTA}.
     * When set to {@link #JAKARTA}, only "post javax->jakarta" module names are allowed. When set to {@link #JAVAX}, only
     * "pre javax->jakarta" module names are allowed.
     */
    // @Inject // injected by maven
    private String namespace;

    @Override
    public void execute() throws ViolationException {
        if (this.excludedGavRegExs == null) {
            this.excludedGavRegExs = List.of();
        }

        String namespace = checkNamespace(this.namespace);
        List<Pattern> excludedGavRegExs = this.excludedGavRegExs.stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());
        List<Artifact> artifacts = project.getArtifacts().stream()
                .filter(a -> a.getGroupId().startsWith("javax.") || a.getGroupId().startsWith("jakarta."))
                .filter(a -> a.getScope().equalsIgnoreCase("compile") || a.getScope().equalsIgnoreCase("runtime"))
                .filter(a -> !a.isOptional())
                .collect(Collectors.toList());
        DependencyIsValidCheck check = new DependencyIsValidCheck(namespace, excludedGavRegExs);
        check.validate(artifacts);
    }

    @Override
    public String toString() {
        return String.format(getClass().getSimpleName() + "[namespace=%s, excludedGavRegExs=%s]", namespace, excludedGavRegExs);
    }

    void setProject(MavenProject project) {
        this.project = Objects.requireNonNull(project);
    }

    void setExcludedGavRegExs(List<String> excludedGavRegExs) {
        this.excludedGavRegExs = new ArrayList<>(Objects.requireNonNull(excludedGavRegExs));
    }

    static String checkNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return JAKARTA;
        }

        namespace = namespace.toLowerCase().trim();
        if (namespace.equals(JAVAX) || namespace.equals(JAKARTA)) {
            return namespace;
        }

        throw new IllegalArgumentException("The namespace '" + namespace + "' is invalid. Only valid namespace names are: '"
                                                   + JAKARTA + "' and '" + JAVAX + "'.");
    }

}
