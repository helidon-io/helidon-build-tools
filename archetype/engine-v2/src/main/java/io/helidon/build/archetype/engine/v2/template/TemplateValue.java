/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.archetype.engine.v2.template;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import io.helidon.build.archetype.engine.v2.descriptor.ValueType;

/**
 * Template value used in {@link TemplateModel}.
 */
public class TemplateValue {

    private String value;
    private URL url = null;
    private final File file;
    private final String template;
    private final int order;

    /**
     * TemplateValue constructor.
     *
     * @param value Value containing xml descriptor data
     *
     * @throws IOException if file attribute has I/O issues
     */
    public TemplateValue(ValueType value) throws IOException {
        this.value = value.value();
        this.file = resolveFile(value);
        this.template = value.template();
        this.order = value.order();
        try {
            this.url = new URL(value.url());
        } catch (MalformedURLException ignored) {
        }
    }

    TemplateValue(String value, URL url, File file, String template, int order) {
        this.value = value;
        this.url = url;
        this.file = file;
        this.template = template;
        this.order = order;
    }

    private File resolveFile(ValueType value) throws IOException {
        if (value.file() == null) {
            return null;
        }
        Path filepath = Path.of(value.file());
        if (!filepath.toFile().exists()) {
            throw new FileNotFoundException(String.format("File %s does not exist", filepath));
        }
        return filepath.toFile();
    }

    /**
     * Get its value.
     *
     * @return value
     */
    public String value() {
        return value;
    }

    /**
     * Set its value.
     *
     * @param value value
     */
    public void value(String value) {
        this.value = value;
    }

    /**
     * Get its URL.
     *
     * @return {@link URL} URL attribute
     */
    public URL url() {
        return url;
    }

    /**
     * Get its resolved file.
     *
     * @return {@link File} file attribute
     */
    public File file() {
        return file;
    }

    /**
     * Get its template.
     *
     * @return template name attribute
     */
    public String template() {
        return template;
    }

    /**
     * Get its order.
     *
     * @return  order attribute
     */
    public int order() {
        return order;
    }
}
