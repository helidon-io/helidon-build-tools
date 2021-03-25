/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

import {ImportMock} from 'ts-mock-imports';
import * as assert from "assert";
import {
    addLaunchedServers,
    getLaunchedServers,
    HelidonServerInstance,
    startHelidonDev,
    stopHelidonDev
} from "../../helidonDev";
import * as vscodeApi from "../../VSCodeAPI";
import * as fsSystemApi from "../../FileSystemAPI";
import * as childProcApi from "../../ChildProcessAPI";
import {ChildProcess} from 'child_process';
import events = require('events');
import stream = require('stream');
import * as vscode from "vscode";

let expect = require('chai').expect;
let vsCodeApiMockManager: any;
let fsSystemApiMockManager: any;
let childProcessAPIManager: any;
let sandbox: any;

suite('HelidonDev Test Suite', () => {

    setup(() => {
        vsCodeApiMockManager = ImportMock.mockStaticClass(vscodeApi, 'VSCodeAPI');
        fsSystemApiMockManager = ImportMock.mockStaticClass(fsSystemApi, 'FileSystemAPI');
        childProcessAPIManager = ImportMock.mockStaticClass(childProcApi, 'ChildProcessAPI');
        sandbox = require('sinon').createSandbox();
    });

    teardown(() => {
        vsCodeApiMockManager.restore();
        fsSystemApiMockManager.restore();
        childProcessAPIManager.restore();
        sandbox.restore();
        getLaunchedServers().clear();
    });

    test('No helidon server stops when the workspace is empty', async () => {
        childProcessAPIManager.mock('isCommandExist', true);
        let killProcessMock = childProcessAPIManager.mock('killProcess');

        await stopHelidonDev();

        assert(killProcessMock.notCalled);
    });

    test('No helidon server stops when there is no running Helidon project exists in the workspace', async () => {
        childProcessAPIManager.mock('isCommandExist', true);
        let launchedServers = new Map([
            ["helidonDir1", <HelidonServerInstance>{
                isActive: false,
                outputChannel: {},
                projectFolder: '/dir/helidonDir1/',
                serverProcess: {}
            }]
        ]);
        addLaunchedServers(launchedServers);
        let killProcessMock = childProcessAPIManager.mock('killProcess');

        await stopHelidonDev();
        let resultServers = getLaunchedServers();
        expect(resultServers.values().next().value.isActive).is.false;
        assert(killProcessMock.notCalled);
    });

    test('A helidon server stops when a running Helidon project exists in the workspace', async () => {
        childProcessAPIManager.mock('isCommandExist', true);
        let launchedServers = new Map([
            ["helidonDir1", <HelidonServerInstance>{
                isActive: true,
                outputChannel: {},
                projectFolder: '/dir/helidonDir1/',
                serverProcess: {}
            }]
        ]);
        addLaunchedServers(launchedServers);
        childProcessAPIManager.mock('killProcess');

        await stopHelidonDev();
        let resultServers = getLaunchedServers();
        expect(resultServers.values().next().value.isActive).is.false;
    });

    test('A helidon server is choosen and stops when a few running Helidon projects exist in the workspace', async () => {
        childProcessAPIManager.mock('isCommandExist', true);
        let launchedServers = new Map([
            ["helidonDir1", <HelidonServerInstance>{
                isActive: true,
                outputChannel: {},
                projectFolder: '/dir/helidonDir1/',
                serverProcess: {}
            }],
            ["helidonDir2", <HelidonServerInstance>{
                isActive: false,
                outputChannel: {},
                projectFolder: '/dir/helidonDir2/',
                serverProcess: {}
            }],
            ["helidonDir3", <HelidonServerInstance>{
                isActive: true,
                outputChannel: {},
                projectFolder: '/dir/helidonDir3/',
                serverProcess: {}
            }]
        ]);
        addLaunchedServers(launchedServers);
        vsCodeApiMockManager.mock('showPickOption').withArgs(
            {
                title: "Choose a server that you want to stop.",
                totalSteps: 1,
                currentStep: 1,
                placeholder: "Project name",
                items: [
                    {label: 'helidonDir1'},
                    {label: 'helidonDir3'}
                ]
            }
        ).returns({label: 'helidonDir3'});
        childProcessAPIManager.mock('killProcess');

        await stopHelidonDev();
        let resultServers = getLaunchedServers();
        expect(resultServers.get('helidonDir3')!.isActive).is.false;
    });

    test('A helidon server is choosen and starts when a few Helidon projects exist in the workspace', async () => {
        childProcessAPIManager.mock('isCommandExist', true);
        vsCodeApiMockManager.mock('getWorkspaceFolders', [
            {uri: {fsPath: '/dir/helidonDir1/'}},
            {uri: {fsPath: '/dir/helidonDir2/'}}
        ]);
        vsCodeApiMockManager.mock('showPickOption').withArgs(
            {
                title: "Choose a Helidon project that you want to start.",
                totalSteps: 1,
                currentStep: 1,
                placeholder: "Project name",
                items: [
                    {label: 'helidonDir1', description: '/dir/helidonDir1/'},
                    {label: 'helidonDir2', description: '/dir/helidonDir2/'}
                ]
            }
        ).returns({description: '/dir/helidonDir2/'});
        fsSystemApiMockManager.mock('isPathExistsSync', true);
        fsSystemApiMockManager.mock('readDirSync', []);
        fsSystemApiMockManager.mock('readTextFileSync', '<dependencies><dependency>\n<groupId>helidon-group<\/groupId>\n');
        childProcessAPIManager.mock('spawnProcess', createServerProcess());
        vsCodeApiMockManager.mock('createOutputChannel', createOutputChannel());

        const launchedServers = await startHelidonDev();
        expect(launchedServers.size).is.equal(1);
        expect(launchedServers.keys().next().value).is.equal('helidonDir2');
    });

    test('No helidon servers start when there are no helidon projects in workspace', async () => {
        childProcessAPIManager.mock('isCommandExist', true);
        vsCodeApiMockManager.mock('getWorkspaceFolders', [
            {uri: {fsPath: '/dir/notHelidonDir1/'}},
            {uri: {fsPath: '/dir/notHelidonDir2/'}}
        ]);
        fsSystemApiMockManager.mock('isPathExistsSync', true);
        fsSystemApiMockManager.mock('readDirSync', []);
        fsSystemApiMockManager.mock('readTextFileSync', '<dependencies><dependency>\n<groupId>other-group<\/groupId>\n');
        childProcessAPIManager.mock('spawnProcess', createServerProcess());
        vsCodeApiMockManager.mock('createOutputChannel', createOutputChannel());

        const launchedServers = await startHelidonDev();
        expect(launchedServers.size).is.equal(0);
    });

    test('A helidon server starts when an open folder with Helidon project exists in the workspace', async () => {
        childProcessAPIManager.mock('isCommandExist', true);
        vsCodeApiMockManager.mock('getWorkspaceFolders', [
            {uri: {fsPath: '/dir/helidonDir/'}}
        ]);
        fsSystemApiMockManager.mock('isPathExistsSync', true);
        fsSystemApiMockManager.mock('readDirSync', []);
        fsSystemApiMockManager.mock('readTextFileSync', '<dependencies><dependency>\n<groupId>helidon-group<\/groupId>\n');
        childProcessAPIManager.mock('spawnProcess', createServerProcess());
        vsCodeApiMockManager.mock('createOutputChannel', createOutputChannel());

        const launchedServers = await startHelidonDev();
        expect(launchedServers.size).is.equal(1);
    });

    test('No helidon servers start when there are no open folder in workspace', async () => {
        childProcessAPIManager.mock('isCommandExist', true);
        vsCodeApiMockManager.mock('getWorkspaceFolders', undefined);
        const launchedServers = await startHelidonDev();
        expect(launchedServers.size).is.equal(0);
    });

});

function createOutputChannel(): vscode.OutputChannel {
    let outputChannel = <vscode.OutputChannel>{};
    outputChannel.show = sandbox.stub();
    return outputChannel;
}

function createServerProcess(): ChildProcess {
    const serverProcess = <ChildProcess>new events.EventEmitter();
    serverProcess.stdin = new stream.Writable();
    serverProcess.stdout = <stream.Readable>new events.EventEmitter();
    serverProcess.stderr = <stream.Readable>new events.EventEmitter();
    return serverProcess;
}