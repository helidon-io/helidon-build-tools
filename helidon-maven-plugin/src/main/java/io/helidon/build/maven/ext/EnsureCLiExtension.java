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
package io.helidon.build.maven.ext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelProcessor;
import org.codehaus.plexus.logging.Logger;

import static io.helidon.build.util.Constants.HELIDON_CLI_PROPERTY;

/**
 * Modifies the in-memory plugin definition for the {@code helidon-maven-plugin} to switch it to {@code helidon-cli-maven-plugin}.
 * This is required for uses of the 2.1.2 or later CLI with Helidon 2.0.2 applications: the dev-loop extension was configured
 * in the {@code helidon-maven-plugin}, and will pick the wrong version.
 * <p></p>
 * Requires an xml file like so in ${projectDir}.mvn/extensions.xml:
 * <pre>
 *     &lt;extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *             xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 http://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
 *
 *     &lt;extension>
 *         &lt;groupId>io.helidon.build-tools&lt;/groupId>
 *         &lt;artifactId>helidon-maven-plugin&lt;/artifactId>
 *         &lt;version>2.1.2-SNAPSHOT&lt;/version>
 *     &lt;/extension>
 *
 * &lt;/extensions>
 * </pre>
 */
@Named("core-default")
@Singleton
@Typed(ModelProcessor.class)
public class EnsureCLiExtension extends DefaultModelProcessor {
    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty(HELIDON_CLI_PROPERTY));
    private static final String BUILD_TOOLS_GROUP_ID = "io.helidon.build-tools";
    private static final String HELIDON_PLUGIN_ARTIFACT_ID = "helidon-maven-plugin";
    private static final String HELIDON_CLI_PLUGIN_ARTIFACT_ID = "helidon-cli-maven-plugin";
    private static final String HELIDON_CLI_PLUGIN_VERSION = "${version.plugin.helidon-cli}";

    @Inject
    private Logger logger;

    @Override
    public Model read(File input, Map<String, ?> options) throws IOException {
        return enableCLiExtensionIfRequired(super.read(input, options));
    }

    @Override
    public Model read(Reader input, Map<String, ?> options) throws IOException {
        return enableCLiExtensionIfRequired(super.read(input, options));
    }

    @Override
    public Model read(InputStream input, Map<String, ?> options) throws IOException {
        return enableCLiExtensionIfRequired(super.read(input, options));
    }

    private Model enableCLiExtensionIfRequired(Model model) {
        if (ENABLED && model != null) {

            // Find the helidon CLI profile

            for (Profile profile : model.getProfiles()) {
                final Activation activation = profile.getActivation();
                if (activation != null) {
                    final ActivationProperty activationProperty = activation.getProperty();
                    if (activationProperty != null && activationProperty.getName().equals(HELIDON_CLI_PROPERTY)) {
                        final BuildBase build = profile.getBuild();
                        if (build != null) {
                            final List<Plugin> plugins = build.getPlugins();

                            // See if this profile has the old plugin configured

                            for (int i = 0; i < plugins.size(); i++) {
                                final Plugin plugin = plugins.get(i);
                                if (plugin.getGroupId().equals(BUILD_TOOLS_GROUP_ID)
                                    && plugin.getArtifactId().equals(HELIDON_PLUGIN_ARTIFACT_ID)) {

                                    // It does, so switch it to the new plugin

                                    final Plugin newPlugin = plugin.clone();
                                    newPlugin.setArtifactId(HELIDON_CLI_PLUGIN_ARTIFACT_ID);
                                    newPlugin.setVersion(HELIDON_CLI_PLUGIN_VERSION);
                                    plugins.set(i, newPlugin);

                                    if (logger.isDebugEnabled()) {
                                        logger.debug("replaced " + plugin + " with " + newPlugin);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return model;
    }
}
