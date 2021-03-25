/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
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