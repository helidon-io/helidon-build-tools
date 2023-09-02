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

package io.helidon.build.common.maven.enforcer.rules;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import static java.util.function.Predicate.not;

/**
 * This rule will apply the same basic "eclipse transformer" checks, but applied as an enforcer rule. See
 * <a href="https://projects.eclipse.org/projects/technology.transformer"/>.
 */
// see https://maven.apache.org/enforcer/enforcer-api/writing-a-custom-rule.html
@Named("helidonJakartaDependenciesRule") // rule name - must start from lowercase character
@SuppressWarnings("unused")
public class HelidonJakartaDependenciesRule extends AbstractEnforcerRule {
    @Inject
    private MavenProject project;

    @Inject
    private MavenSession session;

    @Inject
    private RuntimeInformation runtimeInformation;

    /**
     * Rule parameter as list of items. Reflectively injected from pom.xml
     */
    //    @Inject
    private List<String> excludedGavs;

    @Override
    public void execute() throws EnforcerRuleException {
        ConfigurableMavenResolverSystem resolver = Maven.configureResolver()
                .workOffline()
                .withClassPathResolution(false);
        List<Gav> deps = resolver
//                .resolve(project.getGroupId()+":"+project.getArtifactId()+":"+project.getVersion())
                .loadPomFromFile(session.getCurrentProject().getFile()).importCompileAndRuntimeDependencies().resolve()
                .withTransitivity()
                .asList(MavenCoordinate.class).stream()
                .map(Gav::create)
                .filter(gav -> gav.group().startsWith("javax.") || gav.group().startsWith("jakarta."))
                .filter(gav -> !excludedGavs.contains(gav.toCanonicalName()))
                .collect(Collectors.toList());
        System.out.println("DEPS: " + deps);

        DependencyIsValidCheck isValid = DependencyIsValidCheck.create();
        List<Gav> badDeps = deps.stream()
                .filter(not(isValid::apply))
                .collect(Collectors.toList());

        if (!badDeps.isEmpty()) {
            throw new EnforcerRuleException("Bad dependencies spotted: " + badDeps);
        }
    }

    @Override
    public String toString() {
        return String.format(getClass().getSimpleName() + "[excludedGavs=%s]", excludedGavs);
    }

}
