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

import java.util.Optional;

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
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.logging.Logger;

/**
 * Configuration resolver.
 */
@Component(role = ConfigResolver.class, hint = "default")
public class ConfigResolver {

    @Requirement
    private LifecycleExecutionPlanCalculator executionPlanCalculator;

    @Requirement
    private Logger logger;

    /**
     * Resolver the configuration of a mojo execution.
     *
     * @param execution mojo execution
     * @param session   Maven session
     * @param project   Maven project
     * @return ConfigNode or {@code null}
     */
    public ConfigNode resolve(MojoExecution execution, MavenSession session, MavenProject project) {
        MavenProject currentProject = session.getCurrentProject();
        try {
            executionPlanCalculator.setupMojoExecution(session, project, execution);
            session.setCurrentProject(project);
            ExpressionEvaluator evaluator = new ExpressionEvaluator(session, execution);
            ConfigNode config = new ConfigNode(ConfigAdapters.create(execution.getConfiguration()), null);
            config.visit(node -> {
                node.attributes().entrySet().forEach(e -> e.setValue(evaluateExpression(evaluator, e.getValue())));
                String value = node.value();
                if (value != null) {
                    node.value(evaluateExpression(evaluator, value));
                }
            });
            return config;
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
        } finally {
            session.setCurrentProject(currentProject);
        }
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

        private final String execRootDir;

        ExpressionEvaluator(MavenSession session, MojoExecution mojoExecution) {
            super(session, mojoExecution);
            execRootDir = session.getExecutionRootDirectory();
        }

        @Override
        public Object evaluate(String expr, Class<?> type) throws ExpressionEvaluationException {
            Object val = super.evaluate(expr, type);
            if (val instanceof String) {
                return ((String) val).replace(execRootDir, "#{exec.root.dir}");
            }
            return val;
        }
    }
}
