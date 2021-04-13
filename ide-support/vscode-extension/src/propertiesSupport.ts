/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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

import * as vscode from "vscode";
import {languages, TextDocument, workspace} from "vscode";
import * as path from "path";
import {ProjectInformation} from "./ProjectInformation";

export function updateWorkspaceDocuments(context: vscode.ExtensionContext) {
    let updatedDocumentsCache: string[] = [];
    // When extension is started
    workspace.textDocuments.forEach(document => {
        updateDocumentLanguageId(document, updatedDocumentsCache);
    });
    // When a text document is opened
    context.subscriptions.push(
        workspace.onDidOpenTextDocument((document) => {
            updateDocumentLanguageId(document, updatedDocumentsCache);
        })
    );
}

async function updateDocumentLanguageId(document: TextDocument, documentCache: string[]) {

    const fileName: string = path.basename(document.fileName);
    if (fileName === 'application.properties') {
        if (document.languageId === 'helidon-properties') {
            // the language ID is already helidon-properties do nothing.
            return;
        }
        setDocumentLanguageId(document, fileName, documentCache, 'helidon-properties');
    } else if (fileName === 'application.yaml' || fileName === 'application.yml') {
        if (document.languageId === 'yaml') {
            // the language ID is already yaml do nothing.
            return;
        }
    }
}

function setDocumentLanguageId(document: TextDocument, fileName: string, documentCache: string[], languageId: string): Promise<void> {
    const projectInfo = ProjectInformation.getInformation(document.uri.toString());
    return projectInfo.then(project => {
        if (!project.isHelidonProject()) {
            //it is not a Helidon project.
            return;
        }
        const oldLanguageId: string = document.languageId;
        languages.setTextDocumentLanguage(document, languageId);
        if (!documentCache.includes(document.fileName)) {
            languages.setTextDocumentLanguage(document, oldLanguageId);
            documentCache.push(document.fileName);
        }
    });
}