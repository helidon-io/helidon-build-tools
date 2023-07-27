/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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
import * as assert from "assert";
import * as helidonGenerator from "../../generator";
import { ImportMock } from 'ts-mock-imports';
import * as vscodeApi from "../../VSCodeAPI";
import * as fsSystemApi from "../../FileSystemAPI";
import { ChildProcess } from 'child_process';
import * as childProcApi from "../../ChildProcessAPI";
import * as events from "events";
import * as stream from "stream";
import * as generatorAPI from "../../GeneratorCommand";
import fetch from 'node-fetch';

let vsCodeApiMockManager: any;
let fsSystemApiMockManager: any;
let childProcessAPIManager: any;
let generatorAPIManager: any;
const sinon = require('sinon');

suite('Helidon Project Generator Test Suite', () => {

    setup(() => {
        vsCodeApiMockManager = ImportMock.mockStaticClass(vscodeApi, 'VSCodeAPI');
        fsSystemApiMockManager = ImportMock.mockStaticClass(fsSystemApi, 'FileSystemAPI');
        childProcessAPIManager = ImportMock.mockStaticClass(childProcApi, 'ChildProcessAPI');
        generatorAPIManager = ImportMock.mockStaticClass(generatorAPI, "GeneratorDataAPI");
    });

    teardown(() => {
        vsCodeApiMockManager.restore();
        fsSystemApiMockManager.restore();
        childProcessAPIManager.restore();
    });

    test('Correct flow leads to execute mvn command', async () => {
        const generatorData = new Map();
        generatorData.set("artifactId", "value");
        generatorAPIManager.mock('convertProjectDataElements', generatorData);
        vsCodeApiMockManager.mock('showOpenFolderDialog', <vscode.Uri>{fsPath: "fsPath"});
        vsCodeApiMockManager.mock('createOutputChannel', <vscode.OutputChannel>{appendLine(str: string) {}});
        fsSystemApiMockManager.mock('isPathExistsSync', false);
        const stub = sinon.stub(fetch, 'Promise').resolves({ json: () => Promise.resolve({}) });
        const childProcessMock = childProcessAPIManager.mock('execProcess', createChildProcess());
        await helidonGenerator.showHelidonGenerator("helidonJarFolder");
        stub.restore();
        assert(childProcessMock.calledOnce);
    });
});

function createChildProcess(): ChildProcess {
    const childProcess = <ChildProcess>new events.EventEmitter();
    childProcess.stdin = new stream.Writable();
    childProcess.stdout = <stream.Readable>new events.EventEmitter();
    childProcess.stderr = <stream.Readable>new events.EventEmitter();
    return childProcess;
}
