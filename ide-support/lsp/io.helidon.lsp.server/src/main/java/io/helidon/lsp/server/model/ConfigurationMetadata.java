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

package io.helidon.lsp.server.model;

import java.util.List;

/**
 * Model to represent the object from the helidon-configuration-metadata.json .
 */
public class ConfigurationMetadata {

    private List<ConfigurationGroup> groups;
    private List<ConfigurationProperty> properties;
    private List<ConfigurationHint> hints;

    public List<ConfigurationGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<ConfigurationGroup> groups) {
        this.groups = groups;
    }

    public List<ConfigurationProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<ConfigurationProperty> properties) {
        this.properties = properties;
    }

    public List<ConfigurationHint> getHints() {
        return hints;
    }

    public void setHints(List<ConfigurationHint> hints) {
        this.hints = hints;
    }
}
