/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.maven.sitegen.freemarker;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import freemarker.cache.URLTemplateLoader;

import static io.helidon.build.common.FileUtils.resourceAsPath;

/**
 * A Freemarker template loader used for loading templates from classpath.
 */
public final class TemplateLoader extends URLTemplateLoader {

    private static final String TEMPLATES_RESOURCE = "/templates/";
    private static final String TEMPLATE_FILE_EXT = ".ftl";

    private final Path templatesDir = resourceAsPath(TEMPLATES_RESOURCE, TemplateLoader.class);

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
