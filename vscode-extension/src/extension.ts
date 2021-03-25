/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

import * as vscode from 'vscode';
import {showHelidonGenerator} from './generator';
import {startHelidonDev} from "./helidonDev";
import {stopHelidonDev} from "./helidonDev";
import {VSCodeHelidonCommands} from "./common";
import {openStartPage} from "./startPage";
import {updateWorkspaceDocuments} from "./propertiesSupport";
import {commands, WorkspaceFoldersChangeEvent} from 'vscode';

export function activate(context: vscode.ExtensionContext) {

    vscode.workspace.onDidChangeWorkspaceFolders((event: WorkspaceFoldersChangeEvent) => {
        if (event.added.length > 0) {
            commands.executeCommand('setContext', 'helidonProjectsExist', true);
            return;
        }
        if (event.removed.length > 0) {
            commands.executeCommand('setContext', 'helidonProjectsExist', false);
            return;
        }
    });

    context.subscriptions.push(vscode.commands.registerCommand(VSCodeHelidonCommands.GENERATE_PROJECT, () => {
        showHelidonGenerator();
    }));

    context.subscriptions.push(vscode.commands.registerCommand(VSCodeHelidonCommands.START_PAGE, () => {
        openStartPage(context);
    }));

    context.subscriptions.push(vscode.commands.registerCommand(VSCodeHelidonCommands.DEV_SERVER_START, () => {
        startHelidonDev();
    }));

    context.subscriptions.push(vscode.commands.registerCommand(VSCodeHelidonCommands.DEV_SERVER_STOP, () => {
        stopHelidonDev();
    }));

    updateWorkspaceDocuments(context);
}

export function deactivate() {
}
