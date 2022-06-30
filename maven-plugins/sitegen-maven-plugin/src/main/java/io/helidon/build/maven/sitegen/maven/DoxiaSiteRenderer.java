/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.build.maven.sitegen.maven;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.helidon.build.common.maven.plugin.PlexusLoggerHolder;
import io.helidon.build.maven.sitegen.Config;
import io.helidon.build.maven.sitegen.RenderingException;
import io.helidon.build.maven.sitegen.Site;

import org.apache.maven.doxia.siterenderer.DefaultSiteRenderer;
import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.RenderingContext;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.plugins.site.render.ReportDocumentRenderer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;

/**
 * Doxia site renderer.
 */
@Component(role = Renderer.class)
public class DoxiaSiteRenderer extends DefaultSiteRenderer {

    @Requirement
    @SuppressWarnings("unused")
    private PlexusLoggerHolder plexusLogHolder;

    @Override
    public void render(Collection<DocumentRenderer> documents, SiteRenderingContext context, File outputDirectory)
            throws RendererException, IOException {

        Path outputDir = outputDirectory.toPath();
        for (DocumentRenderer docRenderer : documents) {
            if (!(docRenderer instanceof ReportDocumentRenderer)) {
                continue;
            }
            RenderingContext renderingContext = docRenderer.getRenderingContext();
            Path outputFile = outputDir.resolve(docRenderer.getOutputName());
            Path inputFile = renderingContext.getBasedir().toPath().resolve(renderingContext.getInputName());
            FileTime lastModifiedTime = Files.getLastModifiedTime(outputFile);
            boolean modified = !Files.exists(outputFile)
                    || (Files.getLastModifiedTime(inputFile).compareTo(lastModifiedTime) > 0)
                    || (context.getDecoration().getLastModified() > lastModifiedTime.toMillis());

            if (modified || docRenderer.isOverwrite()) {
                if (!Files.exists(outputFile)) {
                    Files.createDirectories(outputFile.getParent());
                }

                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Generating " + outputFile);
                }

                Writer writer = null;
                try {
                    if (!docRenderer.isExternalReport()) {
                        writer = Files.newBufferedWriter(outputFile, Charset.forName(context.getOutputEncoding()));
                    }
                    docRenderer.renderDocument(writer, this, context);
                } finally {
                    IOUtil.close(writer);
                }
            } else {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug(inputFile + " unchanged, not regenerating...");
                }
            }
        }

        Map<String, String> properties = new HashMap<>();
        Map<String, ?> templateProps = context.getTemplateProperties();
        if (templateProps != null) {
            templateProps.forEach((k, v) -> properties.put(k, v.toString()));
            MavenProject project = (MavenProject) templateProps.get("project");
            if (project != null) {
                properties.put("project.groupId", project.getGroupId());
                properties.put("project.artifactId", project.getArtifactId());
                properties.put("project.version", project.getVersion());
                properties.put("project.basedir", project.getBasedir().getAbsolutePath());
            }
        }

        Path siteDir = context.getSiteDirectories().iterator().next().toPath();
        Path configFile = siteDir.resolve("sitegen.yaml");
        Config config = Config.create(configFile, properties);
        Site site = Site.create(config);

        // enable jruby verbose mode on debugging
        if (getLogger().isDebugEnabled()) {
            System.setProperty("jruby.cli.verbose", "true");
        }

        try {
            site.generate(siteDir, outputDir);
        } catch (RenderingException ex) {
            throw new RendererException("Rendering error", ex);
        }
    }

    @Override
    public void copyResources(SiteRenderingContext context, File resourcesDir, File outputDir) {
    }

    @Override
    public void copyResources(SiteRenderingContext context, File outputDirectory) {
    }
}
