/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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
package io.helidon.build.sitegen.maven;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import io.helidon.build.sitegen.RenderingException;
import io.helidon.build.sitegen.Site;

import org.apache.maven.doxia.siterenderer.DefaultSiteRenderer;
import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.RenderingContext;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.plugins.site.render.ReportDocumentRenderer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;

/**
 * Doxia site renderer.
 */
@Component(role = Renderer.class)
public class DoxiaSiteRenderer extends DefaultSiteRenderer {

    @Override
    public void render(Collection<DocumentRenderer> documents, SiteRenderingContext context, File outputDirectory)
            throws RendererException, IOException {

        for (DocumentRenderer docRenderer : documents) {
            if (!(docRenderer instanceof ReportDocumentRenderer)) {
                continue;
            }
            RenderingContext renderingContext = docRenderer.getRenderingContext();
            File outputFile = new File(outputDirectory, docRenderer.getOutputName());
            File inputFile = new File(renderingContext.getBasedir(), renderingContext.getInputName());
            boolean modified = !outputFile.exists()
                    || (inputFile.lastModified() > outputFile.lastModified())
                    || (context.getDecoration().getLastModified() > outputFile.lastModified());

            if (modified || docRenderer.isOverwrite()) {
                if (!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }

                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Generating " + outputFile);
                }

                Writer writer = null;
                try {
                    if (!docRenderer.isExternalReport()) {
                        writer = WriterFactory.newWriter(outputFile, context.getOutputEncoding());
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

        Properties properties = new Properties();
        Map<String, ?> templateProps = context.getTemplateProperties();
        if (templateProps != null) {
            properties.putAll(templateProps);
            MavenProject project = (MavenProject) templateProps.get("project");
            if (project != null) {
                properties.setProperty("project.groupId", project.getGroupId());
                properties.setProperty("project.artifactId", project.getArtifactId());
                properties.setProperty("project.version", project.getVersion());
                properties.setProperty("project.basedir", project.getBasedir().getAbsolutePath());
            }
        }

        File siteDirectory = context.getSiteDirectories().iterator().next();
        File siteConfigFile = new File(siteDirectory, "sitegen.yaml");
        Site site = Site.builder()
                .config(siteConfigFile, properties)
                .build();

        // enable jruby verbose mode on debugging
        if (getLogger().isDebugEnabled()) {
            System.setProperty("jruby.cli.verbose", "true");
        }

        try {
            site.generate(siteDirectory, outputDirectory);
        } catch (RenderingException ex) {
            throw new RendererException(ex.getMessage(), ex);
        }
    }

    @Override
    public void copyResources(SiteRenderingContext siteRenderingContext, File resourcesDirectory, File outputDirectory)
            throws IOException {
    }

    @Override
    public void copyResources(SiteRenderingContext siteRenderingContext, File outputDirectory) throws IOException {
    }
}
