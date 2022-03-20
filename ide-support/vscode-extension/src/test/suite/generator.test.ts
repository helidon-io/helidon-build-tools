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

import * as vscode from "vscode";
import * as assert from "assert";
import * as helidonGenerator from "../../generator";
import {ImportMock} from 'ts-mock-imports';
import * as vscodeApi from "../../VSCodeAPI";
import * as fsSystemApi from "../../FileSystemAPI";
import {ChildProcess} from 'child_process';
import * as childProcApi from "../../ChildProcessAPI";
import * as events from "events";
import * as stream from "stream";

let vsCodeApiMockManager: any;
let fsSystemApiMockManager: any;
let childProcessAPIManager: any;

suite('Helidon Project Generator Test Suite', () => {

    setup(() => {
        vsCodeApiMockManager = ImportMock.mockStaticClass(vscodeApi, 'VSCodeAPI');
        fsSystemApiMockManager = ImportMock.mockStaticClass(fsSystemApi, 'FileSystemAPI');
        childProcessAPIManager = ImportMock.mockStaticClass(childProcApi, 'ChildProcessAPI');
    });

    teardown(() => {
        vsCodeApiMockManager.restore();
        fsSystemApiMockManager.restore();
        childProcessAPIManager.restore();
    });

    test('Correct flow leads to execute mvn command', async () => {
        vsCodeApiMockManager.mock('showPickOption', {
          label: "Helidon MP Bare",
          archetype: "bare",
          flavor: "mp",
          pkg: "io.helidon.examples.bare.mp"
        });
        // vsCodeApiMockManager.mock('showInputBox', "test");
        // vsCodeApiMockManager.mock('showOpenFolderDialog', <vscode.Uri>{fsPath: "fsPath"});
        // vsCodeApiMockManager.mock('createOutputChannel', <vscode.OutputChannel>{appendLine(str:string){}});
        // fsSystemApiMockManager.mock('isPathExistsSync', false);
        // let childProcessMock = childProcessAPIManager.mock('execProcess', createChildProcess());
        // await helidonGenerator.showHelidonGenerator("helidonJarFolder");
        // assert(childProcessMock.calledOnce);
    });
});

function createChildProcess(): ChildProcess {
    const childProcess = <ChildProcess>new events.EventEmitter();
    childProcess.stdin = new stream.Writable();
    childProcess.stdout = <stream.Readable>new events.EventEmitter();
    childProcess.stderr = <stream.Readable>new events.EventEmitter();
    return childProcess;
}