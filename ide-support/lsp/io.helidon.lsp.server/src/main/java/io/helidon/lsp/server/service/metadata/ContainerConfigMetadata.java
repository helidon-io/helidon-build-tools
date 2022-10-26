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

/**
 * Container for other metadata.
 */
public class ContainerConfigMetadata extends ConfigMetadata {

    /**
     * Create a new instance.
     *
     * @param key         key
     * @param type        type
     * @param kind        kind
     * @param description description
     * @param level       level
     * @param content     content
     */
    public ContainerConfigMetadata(String key, String type, ConfiguredOption.Kind kind, String description, int level,
                                   Set<ConfigMetadata> content) {
        super(key, type, kind, description, level, content);
    }
}
