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

import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionParams;

/**
 * Proposes completion for the given position in the meta configuration file if the appropriate TextDocumentHandler for this
 * type of the file is not found.
 */
public class EmptyTextDocumentHandler implements TextDocumentHandler {
    @Override
    public List<CompletionItem> completion(CompletionParams position) {
        return Collections.emptyList();
    }
}
