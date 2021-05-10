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

import * as vscode from 'vscode';
import {showHelidonGenerator} from './generator';
import {startHelidonDev} from "./helidonDev";
import {stopHelidonDev} from "./helidonDev";
import {VSCodeHelidonCommands} from "./common";
import {openStartPage} from "./startPage";
import {commands, WorkspaceFoldersChangeEvent} from 'vscode';
import * as path from 'path';
import {LanguageClient, LanguageClientOptions, ServerOptions, StreamInfo} from 'vscode-languageclient';

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

    startLangServer(context);
}

export function deactivate() {
}

function startLangServer(context: vscode.ExtensionContext) {
    // Get the java home from the process environment.
    const {JAVA_HOME} = process.env;

    // If java home is available continue.
    if (JAVA_HOME) {
        // Java execution path.
        let excecutable: string = path.join(JAVA_HOME, 'bin', 'java');

        // path to the launcher.jar
        let classPath = path.join(__dirname, '..', 'target', 'server', 'io.helidon.lsp.server.jar');
        const args: string[] = ['-cp', classPath];

        // Set the server options
        // -- java execution path
        // -- argument to be pass when executing the java command
        let serverOptions: ServerOptions = {
            command: excecutable,
            args: [...args, 'io.helidon.lsp.server.HelidonLanguageServerLauncher'],
            options: {}
        };

        // Options to control the language client
        let clientOptions: LanguageClientOptions = {
            // Register the server for plain text documents
            documentSelector: [{scheme: 'file', language: 'quarkus-properties'}, {
                scheme: 'file',
                language: 'helidon-properties'
            }]
        };

        // Create the language client and start the client.
        let disposable = new LanguageClient('helidonLS', 'Helidon Language Server', serverOptions, clientOptions).start();

        // Disposables to remove on deactivation.
        context.subscriptions.push(disposable);
    }
}
