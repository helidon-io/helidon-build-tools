/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
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
        vsCodeApiMockManager.mock('showPickOption',
            {label: "Helidon MP Bare", projectName: "helidon-bare-mp", packages: "io.helidon.examples.bare.mp"});
        vsCodeApiMockManager.mock('showInputBox', "test");
        vsCodeApiMockManager.mock('showOpenFolderDialog', <vscode.Uri>{fsPath: "fsPath"});
        fsSystemApiMockManager.mock('isPathExistsSync', false);
        let childProcessMock = childProcessAPIManager.mock('execProcess', createChildProcess());

        await helidonGenerator.showHelidonGenerator();

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