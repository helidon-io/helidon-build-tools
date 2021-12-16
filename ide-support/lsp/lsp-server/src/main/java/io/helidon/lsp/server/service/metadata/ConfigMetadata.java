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

/**
 * Configuration metadata for Helidon application.
 */
public abstract class ConfigMetadata {

    private final String key;
    private final String type;
    private final ConfiguredOptionKind kind;
    private final String description;
    private final int level;
    private final Set<ConfigMetadata> content;

    /**
     * Create a new instance.
     *
     * @param key         key.
     * @param type        type.
     * @param kind        kind of metadata.
     * @param description short description.
     * @param level       level of metadata in hierarchy.
     * @param content     child metadata set.
     */
    public ConfigMetadata(String key, String type, ConfiguredOptionKind kind, String description, int level,
                          Set<ConfigMetadata> content) {
        this.key = key;
        this.type = type;
        this.kind = kind;
        this.description = description;
        this.level = level;
        this.content = content;
    }

    /**
     * Kind of metadata.
     *
     * @return ConfiguredOptionKind object.
     */
    public ConfiguredOptionKind kind() {
        return kind;
    }

    /**
     * Child metadata set.
     *
     * @return Collection of Child metadata.
     */
    public Set<ConfigMetadata> content() {
        return content;
    }

    /**
     * Level of this metadata in hierarchy.
     *
     * @return level.
     */
    public int level() {
        return level;
    }

    /**
     * Key string.
     *
     * @return key.
     */
    public String key() {
        return key;
    }

    /**
     * Short description.
     *
     * @return short description.
     */
    public String description() {
        return description;
    }

    /**
     * Type of metadata.
     *
     * @return type of metadata.
     */
    public String type() {
        return type;
    }
}
