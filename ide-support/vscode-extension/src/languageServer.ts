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

import { ExtensionContext } from "vscode";
import * as path from 'path';
import { LanguageClient, LanguageClientOptions, StreamInfo } from 'vscode-languageclient';
import { VSCodeAPI } from './VSCodeAPI';
import { OutputFormatter } from './OutputFormatter';
import { ChildProcess } from "child_process";
import { ChildProcessAPI } from "./ChildProcessAPI";
import * as vscode from 'vscode';
import { logger } from "./logger";

let helidonLangServerProcess: ChildProcess;
let deactivatedExtension = false;
const clientName = 'Helidon Language Client';
const serverOutputChannel = VSCodeAPI.createOutputChannel("Helidon LS LOGS");
const clientOutputChannel = VSCodeAPI.createOutputChannel(clientName);
const outputFormatter = new OutputFormatter(serverOutputChannel);
let client : LanguageClient;

export function deactivated(value: boolean){
    deactivatedExtension = value;
}

export function startLangServer(context: ExtensionContext) {
    let maxCountRestart = 5;
    startSocketLangServer(context, maxCountRestart);
}

async function startSocketLangServer(
    context: ExtensionContext,
    maxCountRestart: number
) {

    let langServerProcess: ChildProcess | undefined;

    if (helidonLangServerProcess){
        logger.info("Helidon language server will be restarted. The previous instance of the server will be stopped. Pid - " + helidonLangServerProcess.pid);
        ChildProcessAPI.killProcess(helidonLangServerProcess.pid);
        if (client){
            client.stop();
        }
    }

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
    if (debugMode()) {
        args.push('-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:10001');
    }
    args.push('-jar', 'io.helidon.lsp.server.jar');
    args.push(connectionInfo.port);

    langServerProcess = ChildProcessAPI.spawnProcess(excecutable, args, opts);
    configureLangServer(langServerProcess);

    //connect to the Language Server
    //wait for server start
    await new Promise(resolve => {
        setTimeout(resolve, 5000)
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
        outputChannel: clientOutputChannel,
        outputChannelName: clientName,
        documentSelector: [
            {
                scheme: 'file',
                pattern: '**/*.yaml'
            },
            {
                scheme: 'file',
                pattern: '**/*.properties'
            },
            {
                scheme: 'file',
                pattern: '**/pom.xml'
            }
        ],
        synchronize: {
            fileEvents: vscode.workspace.createFileSystemWatcher("**/pom.xml")
        }
    };

    // Create the language client and start the client.
    client = new LanguageClient('HelidonLanguageClient', clientName, serverOptions, clientOptions);
    configureLangClient(client, context, maxCountRestart);
    logger.info("Helidon Language Server started. Pid - " + langServerProcess.pid);
    // Disposables to remove on deactivation.
    context.subscriptions.push(client.start());

    helidonLangServerProcess = langServerProcess;
}

function debugMode(): boolean {
    return process.env['DEBUG_VSCODE_HELIDON'] === 'true';
}

function configureLangClient(
    client: LanguageClient,
    context: ExtensionContext,
    maxCountRestart: number
) {
    logger.info("Configure Helidon Language Server");
    //reconnect to the server if it stopped
    client.onDidChangeState(async event => {
        logger.info("Client for Helidon Language Server has changed its state from " + event.oldState + " to " + event.newState);
        if (event.newState === 1 && maxCountRestart > 0) {
            if (deactivatedExtension && helidonLangServerProcess){
                logger.info("Helidon Language Server will be stopped because plugin is deactivated. Pid - " + helidonLangServerProcess.pid);
                ChildProcessAPI.killProcess(helidonLangServerProcess.pid);
                client.stop();
                return;
            }
            maxCountRestart = maxCountRestart - 1;
            await new Promise(resolve => {
                setTimeout(resolve, 1000);
            });
            if (!deactivatedExtension){
                logger.info("Helidon Language Server will be restarted, maxCountRestart - " + maxCountRestart);
                startSocketLangServer(context, maxCountRestart);
            }
        }
    });
}

function configureLangServer(langServerProcess: ChildProcess) {
    langServerProcess.on('close', (code) => {
        if (code !== 0) {
            logger.info(`Helidon LangServer process exited with code ${code}`);
            outputFormatter.formatInputString(`Helidon LangServer process exited with code ${code}`);
        }

    });
    langServerProcess.stdout!.on('data', (data) => {
        logger.info(data.toString());
        outputFormatter.formatInputString(data);
    });

    langServerProcess.stderr!.on('data', (data) => {
        logger.error(data.toString());
        outputFormatter.formatInputString(data);
    });
}
