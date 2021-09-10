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

import io.helidon.build.archetype.engine.v2.descriptor.ValueType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

class TemplateValue {

    private final String value;
    private URL url = null;
    private final File file;
    private final String template;
    private final int order;

    TemplateValue(ValueType keyValue) throws IOException {
        this.value = keyValue.value();
        this.file = resolveFile(keyValue);
        this.template = keyValue.template();
        this.order = keyValue.order();
        try {
            this.url = new URL(keyValue.url());
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

    public String value() {
        return value;
    }

    public URL url() {
        return url;
    }

    public File file() {
        return file;
    }

    public String template() {
        return template;
    }

    public int order() {
        return order;
    }
}
