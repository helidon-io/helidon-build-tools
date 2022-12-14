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

package io.helidon.lsp.server.service;

import java.util.ArrayList;
import java.util.List;

import io.helidon.lsp.server.service.metadata.ConfigMetadata;
import io.helidon.lsp.server.service.metadata.ConfiguredType;
import io.helidon.lsp.server.service.metadata.ValueConfigMetadata;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.InsertTextFormat;

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
     * Prepare description for the configuration metadata.
     *
     * @param value configuration metadata.
     * @return description for the configuration metadata.
     */
    default String prepareInfoForKey(ConfigMetadata value) {
        StringBuilder details = new StringBuilder(value.type());
        if (value instanceof ValueConfigMetadata) {
            ValueConfigMetadata vValue = (ValueConfigMetadata) value;
            if (vValue.defaultValue() != null && !vValue.defaultValue().isBlank()) {
                details.append("\nDefault value: ").append(vValue.defaultValue());
            }
            if (vValue.allowedValues() != null && vValue.allowedValues().size() > 0) {
                details.append("\nAllowed values: ");
                for (ConfiguredType.AllowedValue allowedValue : vValue.allowedValues()) {
                    details.append("\n  ").append(allowedValue.value());
                    if (allowedValue.description() != null && !allowedValue.description().isBlank()) {
                        details.append(" (").append(allowedValue.description()).append(")");
                    }
                }
            }
        }
        return details.toString();
    }

    /**
     * Prepare list of completion items for the configuration metadata.
     *
     * @param proposedMetadata configuration metadata.
     * @return list of completion items.
     */
    default List<CompletionItem> prepareCompletionForAllowedValues(ConfigMetadata proposedMetadata) {
        if (proposedMetadata == null) {
            return List.of();
        }
        List<CompletionItem> result = new ArrayList<>();
        if (proposedMetadata instanceof ValueConfigMetadata) {
            ValueConfigMetadata vValue = (ValueConfigMetadata) proposedMetadata;
            if (vValue.allowedValues() != null && vValue.allowedValues().size() > 0) {
                for (ConfiguredType.AllowedValue allowedValue : vValue.allowedValues()) {
                    CompletionItem item = new CompletionItem();
                    item.setKind(CompletionItemKind.Snippet);
                    item.setLabel(allowedValue.value());
                    item.setInsertText(allowedValue.value());
                    item.setDocumentation(allowedValue.description());
                    item.setInsertTextFormat(InsertTextFormat.Snippet);
                    result.add(item);
                }
                return result;
            }
            if (vValue.type().equals("java.lang.Boolean")) {
                CompletionItem trueItem = new CompletionItem();
                trueItem.setKind(CompletionItemKind.Value);
                trueItem.setLabel("true");
                trueItem.setInsertText("true");
                result.add(trueItem);
                CompletionItem falseItem = new CompletionItem();
                falseItem.setKind(CompletionItemKind.Value);
                falseItem.setLabel("false");
                falseItem.setInsertText("false");
                result.add(falseItem);
            }
            return result;
        }
        return result;
    }

}
