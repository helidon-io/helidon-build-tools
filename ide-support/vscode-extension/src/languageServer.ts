/*
 * Copyright (c) 2022 Oracle and/or its affiliates. All rights reserved.
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

import {ExtensionContext} from "vscode";
import * as path from 'path';
import {LanguageClient, LanguageClientOptions, ServerOptions, StreamInfo} from 'vscode-languageclient';
import {VSCodeAPI} from './VSCodeAPI';
import {OutputFormatter} from './OutputFormatter';
import {ChildProcess} from "child_process";
import {ChildProcessAPI} from "./ChildProcessAPI";
import * as vscode from 'vscode';


export async function startSocketLangServer(context: ExtensionContext): Promise<ChildProcess | undefined> {
    // Get the java home from the process environment.
    const {JAVA_HOME} = process.env;
    let langServerProcess: ChildProcess | undefined;

    // If java home is available continue.
    // if (JAVA_HOME) {
        //start Language Server
        let excecutable: string = 'java';
        let jarFileDir = path.join(__dirname, '..', 'server');
        const opts = {
            cwd: jarFileDir
        };
        const getPort = require('get-port');
        let connectionInfo = {
            port: await getPort(),
            host: "localhost"
        };
        
        const args: string[] = [];
        if (debugMode()){
            args.push('-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:10001');
        }
        args.push('-jar', 'io.helidon.lsp.server.jar');
        args.push(connectionInfo.port);
        
        langServerProcess = ChildProcessAPI.spawnProcess(excecutable, args, opts);
        configureLangServer(langServerProcess);

        //connect to the Language Server
        //wait for server start
        await new Promise(resolve => {
            setTimeout(resolve, 3000)
        });
        let net = require("net");
        let serverOptions = () => {
            // Connect to language server via socket
            let socket = net.connect(connectionInfo);
            let result: StreamInfo = {
                writer: socket,
                reader: socket
            };
            return Promise.resolve(result);
        };

        // Options to control the language client
        let clientOptions: LanguageClientOptions = {
            documentSelector: [
                {
                    scheme: 'file',
                    pattern: '**/*.yaml'
                },
                {
                    scheme: 'file',
                    pattern: '**/*.properties'
                }
            ],
            synchronize: {
                fileEvents: vscode.workspace.createFileSystemWatcher("**/pom.xml")
            }
        };

        // Create the language client and start the client.
        let client = new LanguageClient('helidonLS', 'Helidon Language Server', serverOptions, clientOptions);
        let maxCountRestart = 5;
        configureLangClient(client, context, serverOptions, clientOptions, maxCountRestart);
        // Disposables to remove on deactivation.
        context.subscriptions.push(client.start());
    // }

    return langServerProcess;
}

function debugMode() : boolean {
    return process.env['DEBUG_VSCODE_HELIDON'] === 'true';
}

function configureLangClient(
    client: LanguageClient,
    context: ExtensionContext,
    serverOptions: ServerOptions,
    clientOptions: LanguageClientOptions,
    maxCountRestart: number
) {
    //reconnect to the server if it stopped
    client.onDidChangeState(async event => {
        if (event.newState === 1 && maxCountRestart > 0) {
            maxCountRestart = maxCountRestart - 1;
            await new Promise(resolve => {
                setTimeout(resolve, 1000);
            });
            let newClient = new LanguageClient('helidonLS', 'Helidon Language Server', serverOptions, clientOptions);
            context.subscriptions.push(newClient.start());
        }
    });
}

function configureLangServer(langServerProcess: ChildProcess) {
    const outputChannel = VSCodeAPI.createOutputChannel("Helidon LS LOGS");
    const outputFormatter = new OutputFormatter(outputChannel);
    langServerProcess.on('close', (code) => {
        if (code !== 0) {
            outputFormatter.formatInputString(`Helidon LangServer process exited with code ${code}`);
        }

    });
    langServerProcess.stdout!.on('data', (data) => {
        outputFormatter.formatInputString(data);
    });

    langServerProcess.stderr!.on('data', (data) => {
        outputFormatter.formatInputString(data);
    });
}
