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

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import io.helidon.build.common.xml.XMLElement;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleNotFoundException;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.internal.LifecycleExecutionPlanCalculator;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.logging.Logger;

/**
 * Configuration resolver.
 */
@Named
@SessionScoped
public class ConfigResolver {

    @Inject
    private LifecycleExecutionPlanCalculator executionPlanCalculator;

    @Inject
    private Logger logger;

    @Inject
    private MavenSession session;

    /**
     * Resolver the configuration of a mojo execution.
     *
     * @param execution mojo execution
     * @param project   Maven project
     * @return ConfigNode or {@code null}
     */
    public XMLElement resolve(MojoExecution execution, MavenProject project) {
        try {
            MojoExecution executionCopy = copyExecution(execution);
            MavenSession sessionCopy = session.clone();
            sessionCopy.setCurrentProject(project);
            executionPlanCalculator.setupMojoExecution(sessionCopy, project, executionCopy);
            return resolve0(executionCopy, sessionCopy);
        } catch (PluginNotFoundException
                 | PluginResolutionException
                 | PluginDescriptorParsingException
                 | MojoNotFoundException
                 | InvalidPluginDescriptorException
                 | NoPluginFoundForPrefixException
                 | LifecyclePhaseNotFoundException
                 | LifecycleNotFoundException
                 | PluginVersionResolutionException ex) {
            logger.error("Unable to configure mojo: " + execution, ex);
            return null;
        }
    }

    private MojoExecution copyExecution(MojoExecution execution) {
        MojoExecution copy = new MojoExecution(
                execution.getPlugin(),
                execution.getGoal(),
                execution.getExecutionId());
        copy.setLifecyclePhase(execution.getLifecyclePhase());
        copy.setMojoDescriptor(execution.getMojoDescriptor());
        execution.getForkedExecutions().forEach(copy::setForkedExecutions);
        return copy;
    }

    private XMLElement resolve0(MojoExecution execution, MavenSession session) {
        ExpressionEvaluator evaluator = new ExpressionEvaluator(session, execution);
        XMLElement config = ConfigHelper.toXMLElement(execution.getConfiguration());
        config.visit(new XMLElement.Visitor() {
            @Override
            public void visitElement(XMLElement elt) {
                elt.attributes().entrySet().forEach(e -> e.setValue(evaluateExpression(evaluator, e.getValue())));
                String value = elt.value();
                if (value != null) {
                    elt.value(evaluateExpression(evaluator, value));
                }
            }
        });
        return config;
    }

    private String evaluateExpression(ExpressionEvaluator evaluator, String value) {
        return Optional.ofNullable(value).map(v -> {
            try {
                Object result = evaluator.evaluate(v);
                if (result instanceof String) {
                    return (String) result;
                }
                return v;
            } catch (ExpressionEvaluationException e) {
                logger.error("Unable to evaluate expression: " + v, e);
                return v;
            }
        }).orElse("");
    }

    private static final class ExpressionEvaluator extends PluginParameterExpressionEvaluator {

        private final String rootDir;

        ExpressionEvaluator(MavenSession session, MojoExecution mojoExecution) {
            super(session, mojoExecution);
            rootDir = session.getRequest().getMultiModuleProjectDirectory().toPath().toString();
        }

        @Override
        public Object evaluate(String expr, Class<?> type) throws ExpressionEvaluationException {
            Object val = super.evaluate(expr, type);
            if (val instanceof String) {
                return ((String) val).replace(rootDir, "#{root.dir}");
            }
            return val;
        }
    }
}
