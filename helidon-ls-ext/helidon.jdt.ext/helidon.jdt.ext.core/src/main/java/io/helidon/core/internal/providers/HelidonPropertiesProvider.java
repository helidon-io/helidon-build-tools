/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.core.internal.providers;

import com.google.gson.Gson;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.lsp4mp.commons.metadata.ConfigurationMetadata;
import org.eclipse.lsp4mp.jdt.core.BuildingScopeContext;
import org.eclipse.lsp4mp.jdt.core.IPropertiesProvider;
import org.eclipse.lsp4mp.jdt.core.SearchContext;
import org.eclipse.lsp4mp.jdt.core.utils.JDTTypeUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HelidonPropertiesProvider implements IPropertiesProvider {

    private static final Logger LOGGER = Logger.getLogger(HelidonPropertiesProvider.class.getName());
    private static final String HELIDON_PROPERTIES_FILE = "helidon-configuration-metadata.json";

    private IJavaProject javaProject;
    private IClasspathEntry[] resolvedClasspath;
    private boolean excludeTestCode;
    private final Gson gson;

    public HelidonPropertiesProvider() {
        gson = new Gson();
    }

    public void endSearch(SearchContext context, IProgressMonitor monitor) {
        // Loop for each JAR and try to load the /META-INF/helidon-configuration-metadata.json
        for (IClasspathEntry entry : resolvedClasspath) {
            if (excludeTestCode && entry.isTest()) {
                continue;
            }
            switch (entry.getEntryKind()) {
                case IClasspathEntry.CPE_LIBRARY:
                    String jarPath = entry.getPath().toOSString();
                    IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(jarPath);
                    if (root != null) {
                        if (!root.getElementName().contains("helidon")) {
                            continue;
                        }
                        ConfigurationMetadata metadata = getMetadata(root);
                        if (metadata != null) {
                            context.getCollector().merge(metadata);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public void contributeToClasspath(BuildingScopeContext context, IProgressMonitor monitor) {
        javaProject = context.getJavaProject();
        resolvedClasspath = context.getResolvedClasspath();
        excludeTestCode = context.isExcludeTestCode();
    }

    private ConfigurationMetadata getMetadata(IPackageFragmentRoot root) {
        try {
            final IJarEntryResource resource = JDTTypeUtils.findPropertiesResource(
                    root,
                    HELIDON_PROPERTIES_FILE
            );
            if (resource == null) {
                return null;
            }
            return gson.fromJson(
                    new InputStreamReader(resource.getContents(), StandardCharsets.UTF_8),
                    ConfigurationMetadata.class
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while downloading deployment JAR '" + root.getElementName() + "'.", e);
            return null;
        }
    }

    public SearchPattern createSearchPattern() {
        return null;
    }

    @Override
    public void collectProperties(SearchMatch searchMatch, SearchContext searchContext, IProgressMonitor iProgressMonitor) {
    }
}
