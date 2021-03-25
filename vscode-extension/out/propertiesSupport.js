"use strict";
/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) {
        return value instanceof P ? value : new P(function (resolve) {
            resolve(value);
        });
    }

    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) {
            try {
                step(generator.next(value));
            } catch (e) {
                reject(e);
            }
        }

        function rejected(value) {
            try {
                step(generator["throw"](value));
            } catch (e) {
                reject(e);
            }
        }

        function step(result) {
            result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected);
        }

        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", {value: true});
exports.updateWorkspaceDocuments = void 0;
const vscode_1 = require("vscode");
const path = require("path");
const ProjectInformation_1 = require("./ProjectInformation");

function updateWorkspaceDocuments(context) {
    let updatedDocumentsCache = [];
    // When extension is started
    vscode_1.workspace.textDocuments.forEach(document => {
        updateDocumentLanguageId(document, updatedDocumentsCache);
    });
    // When a text document is opened
    context.subscriptions.push(vscode_1.workspace.onDidOpenTextDocument((document) => {
        updateDocumentLanguageId(document, updatedDocumentsCache);
    }));
}

exports.updateWorkspaceDocuments = updateWorkspaceDocuments;

function updateDocumentLanguageId(document, documentCache) {
    return __awaiter(this, void 0, void 0, function* () {
        const fileName = path.basename(document.fileName);
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
    });
}

function setDocumentLanguageId(document, fileName, documentCache, languageId) {
    const projectInfo = ProjectInformation_1.ProjectInformation.getInformation(document.uri.toString());
    return projectInfo.then(project => {
        if (!project.isHelidonProject()) {
            //it is not a Helidon project.
            return;
        }
        const oldLanguageId = document.languageId;
        vscode_1.languages.setTextDocumentLanguage(document, languageId);
        if (!documentCache.includes(document.fileName)) {
            vscode_1.languages.setTextDocumentLanguage(document, oldLanguageId);
            documentCache.push(document.fileName);
        }
    });
}

//# sourceMappingURL=propertiesSupport.js.map