"use strict";
/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */
Object.defineProperty(exports, "__esModule", {value: true});
exports.deactivate = exports.activate = void 0;
const vscode = require("vscode");
const generator_1 = require("./generator");
const helidonDev_1 = require("./helidonDev");
const helidonDev_2 = require("./helidonDev");
const common_1 = require("./common");
const startPage_1 = require("./startPage");
const propertiesSupport_1 = require("./propertiesSupport");
const vscode_1 = require("vscode");

function activate(context) {
    vscode.workspace.onDidChangeWorkspaceFolders((event) => {
        if (event.added.length > 0) {
            vscode_1.commands.executeCommand('setContext', 'helidonProjectsExist', true);
            return;
        }
        if (event.removed.length > 0) {
            vscode_1.commands.executeCommand('setContext', 'helidonProjectsExist', false);
            return;
        }
    });
    context.subscriptions.push(vscode.commands.registerCommand(common_1.VSCodeHelidonCommands.GENERATE_PROJECT, () => {
        generator_1.showHelidonGenerator();
    }));
    context.subscriptions.push(vscode.commands.registerCommand(common_1.VSCodeHelidonCommands.START_PAGE, () => {
        startPage_1.openStartPage(context);
    }));
    context.subscriptions.push(vscode.commands.registerCommand(common_1.VSCodeHelidonCommands.DEV_SERVER_START, () => {
        helidonDev_1.startHelidonDev();
    }));
    context.subscriptions.push(vscode.commands.registerCommand(common_1.VSCodeHelidonCommands.DEV_SERVER_STOP, () => {
        helidonDev_2.stopHelidonDev();
    }));
    propertiesSupport_1.updateWorkspaceDocuments(context);
}

exports.activate = activate;

function deactivate() {
}

exports.deactivate = deactivate;
//# sourceMappingURL=extension.js.map