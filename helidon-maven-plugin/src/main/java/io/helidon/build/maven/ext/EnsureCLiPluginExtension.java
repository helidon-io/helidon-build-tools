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
import java.util.Map;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelProcessor;
import org.codehaus.plexus.logging.Logger;

import static io.helidon.build.util.Constants.HELIDON_CLI_PROPERTY;

/**
 * Modifies the in memory plugin definition for the {@code helidon-cli-maven-plugin} to ensure it is set as an extension.
 * This is required for uses of the 2.1.1 or later CLI with Helidon 2.0.2 applications: the dev-loop extension was configured
 * in the {@code helidon-cli-maven-plugin}, and will pick
 * <p></p>
 * Requires an xml file like so in ${projectDir}.mvn/extensions.xml:
 * <pre>
 *     &lt;extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *             xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 http://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
 *
 *     &lt;extension>
 *         &lt;groupId>io.helidon.build-tools&lt;/groupId>
 *         &lt;artifactId>helidon-maven-plugin&lt;/artifactId>
 *         &lt;version>2.1.1-SNAPSHOT&lt;/version>
 *     &lt;/extension>
 *
 * &lt;/extensions>
 * </pre>
 */
@Named("core-default")
@Singleton
@Typed(ModelProcessor.class)
public class EnsureCLiPluginExtension extends DefaultModelProcessor {

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
        if (Boolean.parseBoolean(System.getProperty(HELIDON_CLI_PROPERTY))) {
            logger.info("checking for CLI extension");
            // TODO modify Plugin for helidon-cli-maven-plugin to set as an extension
        }
        return model;
    }
}
