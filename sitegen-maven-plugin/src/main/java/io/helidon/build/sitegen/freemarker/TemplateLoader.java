/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.sitegen.freemarker;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import io.helidon.build.sitegen.Helper;

import freemarker.cache.URLTemplateLoader;

/**
 * A Freemarker template loader used for loading templates from classpath.
 *
 * @author rgrecour
 */
public class TemplateLoader extends URLTemplateLoader {

    private static final String TEMPLATES_RESOURCE =
            "/helidon-sitegen-templates/";
    private static final String TEMPLATE_FILE_EXT = ".ftl";

    private final Path templatesDir;

    /**
     * Create a new instance of {@link TemplateLoader}.
     */
    public TemplateLoader(){
        try {
            templatesDir = Helper
                    .loadResourceDirAsPath(TEMPLATES_RESOURCE);
        } catch (URISyntaxException
                | IOException
                | IllegalStateException  ex) {
            throw new IllegalStateException(
                    "Unable to get templates directory", ex);
        }
    }

    @Override
    protected URL getURL(String name) {
        String tplName = name;
        if (!tplName.endsWith(TEMPLATE_FILE_EXT)) {
            tplName += TEMPLATE_FILE_EXT;
        }
        Path tpl = templatesDir.resolve(tplName);
        if (!Files.exists(tpl)) {
            return null;
        }
        try {
            return tpl.toUri().toURL();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
