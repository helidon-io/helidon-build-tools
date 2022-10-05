/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.lsp.server.service.metadata;

import java.util.Set;

import io.helidon.config.metadata.ConfiguredOption;

//TODO add javadocs for every class in this package
public abstract class ConfigMetadata {

    private final String key;
    private final String type;
    private final ConfiguredOption.Kind kind;
    private final String description;
    private final int level;
    private final Set<ConfigMetadata> content;

    public ConfigMetadata(String key, String type, ConfiguredOption.Kind kind, String description, int level, Set<ConfigMetadata> content) {
        this.key = key;
        this.type = type;
        this.kind = kind;
        this.description = description;
        this.level = level;
        this.content = content;
    }

    public ConfiguredOption.Kind kind() {
        return kind;
    }

    public Set<ConfigMetadata> content() {
        return content;
    }

    public int level() {
        return level;
    }

    public String key() {
        return key;
    }

    public String description() {
        return description;
    }

    public String type() {
        return type;
    }
}