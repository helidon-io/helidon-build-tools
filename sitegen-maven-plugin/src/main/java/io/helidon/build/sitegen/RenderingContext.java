/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
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

package io.helidon.build.sitegen;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.helidon.build.sitegen.freemarker.TemplateSession;
import io.helidon.build.util.SourcePath;

import static io.helidon.build.sitegen.Helper.checkNonNull;
import static io.helidon.build.sitegen.Helper.checkNonNullNonEmpty;
import static io.helidon.build.sitegen.Helper.checkValidDir;
import static io.helidon.build.sitegen.Helper.copyResources;

/**
 * Represents a site processing invocation.
 */
public class RenderingContext {

    private final Site site;
    private final TemplateSession templateSession;
    private final Map<String, Page> pages;
    private final File sourcedir;
    private final File outputdir;
    private final List<SourcePath> sourcePaths;

    RenderingContext(Site site, File sourcedir, File outputdir) {
        checkNonNull(site, "site");
        checkValidDir(sourcedir, "sourcedir");
        checkNonNull(outputdir, "outputdir");
        this.site = site;
        this.sourcedir = sourcedir;
        this.outputdir = outputdir;
        this.templateSession = new TemplateSession();
        this.sourcePaths = SourcePath.scan(sourcedir);
        this.pages = Page.create(
                sourcePaths, site.getPages(), sourcedir, site.getBackend());
    }

    /**
     * Get the source directory of the site.
     * @return the source directory, never {@code null}
     */
    public File getSourcedir() {
        return sourcedir;
    }

    /**
     * Get the output directory of this site processing invocation.
     * @return the source directory, never {@code null}
     */
    public File getOutputdir() {
        return outputdir;
    }

    /**
     * Get the {@link TemplateSession} of this site processing invocation.
     * @return the template session, never {@code null}
     */
    public TemplateSession getTemplateSession() {
        return templateSession;
    }

    /**
     * Get all scanned pages.
     *
     * @return the scanned pages indexed by source path, never {@code null}
     */
    public Map<String, Page> getPages() {
        return pages;
    }

    /**
     * Get the configured {@link Site} instance.
     * @return the {@link Site} instance
     */
    public Site getSite() {
        return site;
    }

    /**
     * Find a page with the given target path.
     *
     * @param route the target path to search
     * @return the {@link Page} instance if found, {@code null} otherwise
     */
    public Page getPageForRoute(String route) {
        checkNonNullNonEmpty(route, "route");
        for (Page page : pages.values()) {
            if (route.equals(page.getTargetPath())) {
                return page;
            }
        }
        return null;
    }

    /**
     * Copy the scanned static assets in the output directory.
     */
    public void copyStaticAssets() {
        for (StaticAsset asset : site.getAssets()) {
            for (SourcePath path : SourcePath.filter(
                    sourcePaths, asset.getIncludes(), asset.getExcludes())) {
                File targetDir = new File(outputdir, asset.getTarget());
                targetDir.mkdirs();
                try {
                    copyResources(new File(sourcedir, path.asString()).toPath(),
                            new File(targetDir, path.asString()));
                } catch (IOException ex) {
                    throw new RenderingException(
                            "An error occurred while copying resource: " + path.asString(), ex);
                }
            }
        }
    }

    /**
     * Process the rendering of all pages.
     *
     * @param pagesdir the directory where to generate the rendered files
     * @param ext the file extension to use for the rendered files
     */
    public void processPages(File pagesdir, String ext) {
        for (Page page : pages.values()) {
            PageRenderer renderer = site.getBackend().getPageRenderer(page.getSourceExt());
            renderer.process(page, this, pagesdir, ext);
        }
    }
}
