/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype;

import java.io.File;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.archetype.ArchetypeGenerationRequest;
import org.apache.maven.archetype.ArchetypeGenerationResult;
import org.apache.maven.archetype.exception.ArchetypeNotConfigured;
import org.apache.maven.archetype.generator.ArchetypeGenerator;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * Generate a project using {@link ArchetypeGenerator}.
 */
class MavenArchetypeGenerator {

    private MavenArchetypeGenerator() {
    }

    /**
     * Generate a new project.
     *
     * @param container           plexus container
     * @param archetypeGroupId    archetype groupId
     * @param archetypeArtifactId archetype artifactId
     * @param archetypeVersion    archetype version
     * @param archetypeFile       archetype file
     * @param properties          archetype properties
     * @param basedir             project base directory
     * @param session             Maven session
     */
    @SuppressWarnings({"unused", "checkstyle:ParameterNumber"})
    static void generate(PlexusContainer container,
                         String archetypeGroupId,
                         String archetypeArtifactId,
                         String archetypeVersion,
                         File archetypeFile,
                         Properties properties,
                         Path basedir,
                         MavenSession session) {

        ArchetypeGenerationRequest request = new ArchetypeGenerationRequest()
                .setArchetypeGroupId(archetypeGroupId)
                .setArchetypeArtifactId(archetypeArtifactId)
                .setArchetypeVersion(archetypeVersion)
                .setGroupId(properties.getProperty("groupId"))
                .setArtifactId(properties.getProperty("artifactId"))
                .setVersion(properties.getProperty("version"))
                .setPackage(properties.getProperty("package"))
                .setOutputDirectory(basedir.toString())
                .setProperties(properties)
                .setRepositorySession(session.getRepositorySession());

        ArchetypeGenerationResult result = new ArchetypeGenerationResult();

        Thread currentThread = Thread.currentThread();
        ClassLoader ccl = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader(container.getLookupRealm());
            ArchetypeGenerator generator = container.lookup(ArchetypeGenerator.class);
            generator.generateArchetype(request, archetypeFile, result);
            if (result.getCause() != null) {
                if (result.getCause() instanceof ArchetypeNotConfigured) {
                    ArchetypeNotConfigured anc = (ArchetypeNotConfigured) result.getCause();
                    throw new RuntimeException(
                            "Missing required properties in archetype.properties: "
                                    + String.join(", ", anc.getMissingProperties()), anc);
                }
            }
        } catch (ComponentLookupException e) {
            throw new RuntimeException(e);
        } finally {
            currentThread.setContextClassLoader(ccl);
        }
    }
}
