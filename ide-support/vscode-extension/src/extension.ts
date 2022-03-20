/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates. All rights reserved.
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

import * as vscode from 'vscode';
import { showHelidonGenerator } from './generator';
import { startHelidonDev } from "./helidonDev";
import { stopHelidonDev } from "./helidonDev";
import { VSCodeHelidonCommands } from "./common";
import { openStartPage } from "./startPage";
import { updateWorkspaceDocuments } from "./propertiesSupport";
import { commands, WorkspaceFoldersChangeEvent } from 'vscode';
import { STEPS } from "./steps_data";

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
        showHelidonGenerator(context.extensionPath, STEPS);
    }));

    context.subscriptions.push(vscode.commands.registerCommand(VSCodeHelidonCommands.START_PAGE, () => {
        openStartPage(context);
    }));

    context.subscriptions.push(vscode.commands.registerCommand(VSCodeHelidonCommands.DEV_SERVER_START, () => {
        startHelidonDev(context.extensionPath);
    }));

    context.subscriptions.push(vscode.commands.registerCommand(VSCodeHelidonCommands.DEV_SERVER_STOP, () => {
        stopHelidonDev();
    }));

    updateWorkspaceDocuments(context);
}

export function deactivate() {
}
