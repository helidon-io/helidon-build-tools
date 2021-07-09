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

package io.helidon.lsp.server.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.helidon.lsp.server.model.ConfigurationHint;
import io.helidon.lsp.server.model.ConfigurationMetadata;
import io.helidon.lsp.server.model.ConfigurationProperty;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionParams;

/**
 * Provides the method to propose completion for the given position in the meta configuration file.
 */
public interface TextDocumentHandler {

    /**
     * Provides the list of the CompletionItems for the given position in the meta configuration file.
     *
     * @param position position in the meta configuration file.
     * @return list CompletionItem objects.
     */
    List<CompletionItem> completion(CompletionParams position);

    /**
     * Get the documentation for the property attribute.
     *
     * @param property ConfigurationProperty object.
     * @return documentation.
     */
    default String getDocumentation(ConfigurationProperty property) {
        if (property.getDefaultValue() != null && !property.getDefaultValue().trim().isEmpty()) {
            return String.format(
                    "Default value = %s\n\n%s",
                    property.getDefaultValue(),
                    property.getDescription()
            );
        }
        return property.getDescription();
    }

    /**
     * Get the value from the Hint attribute for the appropriate property attribute from the given ConfigurationMetadata object.
     *
     * @param property property attribute.
     * @param metadata ConfigurationMetadata object.
     * @return the value from the Hint attribute for the appropriate property attribute.
     */
    default String getValue(ConfigurationProperty property, final ConfigurationMetadata metadata) {
        if (metadata.getHints() == null || metadata.getHints().isEmpty()) {
            if (property.getDefaultValue() == null || property.getDefaultValue().isEmpty()) {
                return "";
            }
            return property.getDefaultValue();
        }
        return metadata
                .getHints().stream()
                .filter(hint -> hint.getName().equals(property.getName()))
                .findFirst()
                .map(ConfigurationHint::getValues)
                .map(values -> values.stream()
                        .map(ConfigurationHint.Value::getValue)
                        .collect(Collectors.joining(",")))
                .map(value -> String.format("${1|%s|}", value))
                .orElseGet(() -> Optional.ofNullable(property.getDefaultValue()).orElse(""));
    }
}
