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
package io.helidon.build.stager;

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Configuration converter for {@link StagedDirectory}.
 */
public class StagedDirectoryConverter implements ConfigurationConverter {

    private static final NoopListener NOOP_LISTENER = new NoopListener();

    @Override
    public boolean canConvert(Class<?> type) {
        return StagedDirectory.class.getName().equals(type.getName());
    }

    @Override
    public Object fromConfiguration(ConverterLookup lookup,
                                    PlexusConfiguration configuration,
                                    Class<?> type,
                                    Class<?> enclosingType,
                                    ClassLoader loader,
                                    ExpressionEvaluator evaluator) throws ComponentConfigurationException {

        return fromConfiguration(lookup, configuration, type, enclosingType, loader, evaluator, NOOP_LISTENER);
    }

    @Override
    public Object fromConfiguration(ConverterLookup lookup,
                                    PlexusConfiguration configuration,
                                    Class<?> type,
                                    Class<?> enclosingType,
                                    ClassLoader loader,
                                    ExpressionEvaluator evaluator,
                                    ConfigurationListener listener) throws ComponentConfigurationException {

        try {
            ConfigNode parent = new ConfigNode(configuration, null);
            parent.visit((node) -> node.parent().addMappedChild(node, node.convert()));
            return parent.asStagedDirectory();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new ComponentConfigurationException(ex.getMessage(), ex);
        }
    }

    /**
     * Empty implementation of {@link ConfigurationListener}.
     */
    private static final class NoopListener implements ConfigurationListener {

        @Override
        public void notifyFieldChangeUsingReflection(String name, Object value, Object component) {
        }

        @Override
        public void notifyFieldChangeUsingSetter(String name, Object value, Object component) {
        }
    }
}
