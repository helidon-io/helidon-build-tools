"use strict";
/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */
Object.defineProperty(exports, "__esModule", {value: true});
exports.validateUserInput = exports.getPageContent = exports.VSCodeJavaCommands = exports.VSCodeHelidonCommands = void 0;
const vscode = require("vscode");
// VS Code Helidon extension commands
var VSCodeHelidonCommands;
(function (VSCodeHelidonCommands) {
    VSCodeHelidonCommands.GENERATE_PROJECT = 'helidon.generate';
    VSCodeHelidonCommands.DEV_SERVER_START = 'helidon.startDev';
    VSCodeHelidonCommands.DEV_SERVER_STOP = 'helidon.stopDev';
    VSCodeHelidonCommands.START_PAGE = 'helidon.startPage';
})(VSCodeHelidonCommands = exports.VSCodeHelidonCommands || (exports.VSCodeHelidonCommands = {}));
var VSCodeJavaCommands;
(function (VSCodeJavaCommands) {
    VSCodeJavaCommands.JAVA_MARKERS_COMMAND = 'microprofile/java/projectLabels';
})(VSCodeJavaCommands = exports.VSCodeJavaCommands || (exports.VSCodeJavaCommands = {}));

function getPageContent(pagePath) {
    return vscode.workspace.openTextDocument(vscode.Uri.file(pagePath).fsPath)
        .then(doc => doc.getText());
}

exports.getPageContent = getPageContent;

function validateUserInput(userInput, pattern, errorMessage) {
    if (!pattern.test(userInput)) {
        return errorMessage;
    }
    return undefined;
}

exports.validateUserInput = validateUserInput;
//# sourceMappingURL=common.js.map