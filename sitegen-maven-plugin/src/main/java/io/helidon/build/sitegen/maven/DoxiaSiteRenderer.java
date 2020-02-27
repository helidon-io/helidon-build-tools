/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import io.helidon.build.sitegen.RenderingException;
import io.helidon.build.sitegen.Site;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.siterenderer.DocumentContent;
import org.apache.maven.doxia.siterenderer.DocumentRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;
import org.apache.maven.doxia.siterenderer.RenderingContext;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * Doxia site renderer.
 */
@Component(role = Renderer.class)
public class DoxiaSiteRenderer extends AbstractLogEnabled implements Renderer {

    @Override
    public void render(Collection<DocumentRenderer> documents, SiteRenderingContext context, File outputDirectory)
            throws RendererException, IOException {

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
    public void generateDocument(Writer writer, SiteRendererSink sink, SiteRenderingContext siteRenderingContext)
            throws RendererException {
    }

    @Override
    public void mergeDocumentIntoSite(Writer writer, DocumentContent content, SiteRenderingContext siteRenderingContext)
            throws RendererException {
    }

    private SiteRenderingContext createSiteRenderingContext(Map<String, ?> attributes, DecorationModel decoration,
            String defaultWindowTitle, Locale locale) {

        SiteRenderingContext context = new SiteRenderingContext();
        context.setTemplateProperties(attributes);
        context.setLocale(locale);
        context.setDecoration(decoration);
        context.setDefaultWindowTitle(defaultWindowTitle);
        return context;
    }

    @Override
    public SiteRenderingContext createContextForSkin(Artifact skin, Map<String, ?> attributes, DecorationModel decoration,
            String defaultWindowTitle, Locale locale) throws RendererException, IOException {

        return createSiteRenderingContext(attributes, decoration, defaultWindowTitle, locale);
    }

    @Override
    public SiteRenderingContext createContextForTemplate(File templateFile, Map<String, ?> attributes, DecorationModel decoration,
            String defaultWindowTitle, Locale locale) throws MalformedURLException {

        SiteRenderingContext context = createSiteRenderingContext(attributes, decoration, defaultWindowTitle, locale);
        context.setTemplateName(templateFile.getName());
        context.setTemplateClassLoader(new URLClassLoader(new URL[]{templateFile.getParentFile().toURI().toURL()}));
        return context;
    }

    @Override
    public void copyResources(SiteRenderingContext siteRenderingContext, File resourcesDirectory, File outputDirectory)
            throws IOException {
    }

    @Override
    public void copyResources(SiteRenderingContext siteRenderingContext, File outputDirectory) throws IOException {
    }

    @Override
    public Map<String, DocumentRenderer> locateDocumentFiles(SiteRenderingContext siteRenderingContext)
            throws IOException, RendererException {

        return Collections.emptyMap();
    }

    @Override
    public Map<String, DocumentRenderer> locateDocumentFiles(SiteRenderingContext siteRenderingContext, boolean editable)
            throws IOException, RendererException {

        return Collections.emptyMap();
    }

    @Override
    public void renderDocument(Writer writer, RenderingContext docRenderingContext, SiteRenderingContext siteContext)
            throws RendererException, FileNotFoundException, UnsupportedEncodingException {
    }
}
