"use strict";
/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) {
        return value instanceof P ? value : new P(function (resolve) {
            resolve(value);
        });
    }

    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) {
            try {
                step(generator.next(value));
            } catch (e) {
                reject(e);
            }
        }

        function rejected(value) {
            try {
                step(generator["throw"](value));
            } catch (e) {
                reject(e);
            }
        }

        function step(result) {
            result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected);
        }

        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", {value: true});
const ts_mock_imports_1 = require("ts-mock-imports");
const assert = require("assert");
const helidonDev_1 = require("../../helidonDev");
const vscodeApi = require("../../VSCodeAPI");
const fsSystemApi = require("../../FileSystemAPI");
const childProcApi = require("../../ChildProcessAPI");
const events = require("events");
const stream = require("stream");
let expect = require('chai').expect;
let vsCodeApiMockManager;
let fsSystemApiMockManager;
let childProcessAPIManager;
let sandbox;
suite('HelidonDev Test Suite', () => {
    setup(() => {
        vsCodeApiMockManager = ts_mock_imports_1.ImportMock.mockStaticClass(vscodeApi, 'VSCodeAPI');
        fsSystemApiMockManager = ts_mock_imports_1.ImportMock.mockStaticClass(fsSystemApi, 'FileSystemAPI');
        childProcessAPIManager = ts_mock_imports_1.ImportMock.mockStaticClass(childProcApi, 'ChildProcessAPI');
        sandbox = require('sinon').createSandbox();
    });
    teardown(() => {
        vsCodeApiMockManager.restore();
        fsSystemApiMockManager.restore();
        childProcessAPIManager.restore();
        sandbox.restore();
        helidonDev_1.getLaunchedServers().clear();
    });
    test('No helidon server stops when the workspace is empty', () => __awaiter(void 0, void 0, void 0, function* () {
        childProcessAPIManager.mock('isCommandExist', true);
        let killProcessMock = childProcessAPIManager.mock('killProcess');
        yield helidonDev_1.stopHelidonDev();
        assert(killProcessMock.notCalled);
    }));
    test('No helidon server stops when there is no running Helidon project exists in the workspace', () => __awaiter(void 0, void 0, void 0, function* () {
        childProcessAPIManager.mock('isCommandExist', true);
        let launchedServers = new Map([
            ["helidonDir1", {
                isActive: false,
                outputChannel: {},
                projectFolder: '/dir/helidonDir1/',
                serverProcess: {}
            }]
        ]);
        helidonDev_1.addLaunchedServers(launchedServers);
        let killProcessMock = childProcessAPIManager.mock('killProcess');
        yield helidonDev_1.stopHelidonDev();
        let resultServers = helidonDev_1.getLaunchedServers();
        expect(resultServers.values().next().value.isActive).is.false;
        assert(killProcessMock.notCalled);
    }));
    test('A helidon server stops when a running Helidon project exists in the workspace', () => __awaiter(void 0, void 0, void 0, function* () {
        childProcessAPIManager.mock('isCommandExist', true);
        let launchedServers = new Map([
            ["helidonDir1", {
                isActive: true,
                outputChannel: {},
                projectFolder: '/dir/helidonDir1/',
                serverProcess: {}
            }]
        ]);
        helidonDev_1.addLaunchedServers(launchedServers);
        childProcessAPIManager.mock('killProcess');
        yield helidonDev_1.stopHelidonDev();
        let resultServers = helidonDev_1.getLaunchedServers();
        expect(resultServers.values().next().value.isActive).is.false;
    }));
    test('A helidon server is choosen and stops when a few running Helidon projects exist in the workspace', () => __awaiter(void 0, void 0, void 0, function* () {
        childProcessAPIManager.mock('isCommandExist', true);
        let launchedServers = new Map([
            ["helidonDir1", {
                isActive: true,
                outputChannel: {},
                projectFolder: '/dir/helidonDir1/',
                serverProcess: {}
            }],
            ["helidonDir2", {
                isActive: false,
                outputChannel: {},
                projectFolder: '/dir/helidonDir2/',
                serverProcess: {}
            }],
            ["helidonDir3", {
                isActive: true,
                outputChannel: {},
                projectFolder: '/dir/helidonDir3/',
                serverProcess: {}
            }]
        ]);
        helidonDev_1.addLaunchedServers(launchedServers);
        vsCodeApiMockManager.mock('showPickOption').withArgs({
            title: "Choose a server that you want to stop.",
            totalSteps: 1,
            currentStep: 1,
            placeholder: "Project name",
            items: [
                {label: 'helidonDir1'},
                {label: 'helidonDir3'}
            ]
        }).returns({label: 'helidonDir3'});
        childProcessAPIManager.mock('killProcess');
        yield helidonDev_1.stopHelidonDev();
        let resultServers = helidonDev_1.getLaunchedServers();
        expect(resultServers.get('helidonDir3').isActive).is.false;
    }));
    test('A helidon server is choosen and starts when a few Helidon projects exist in the workspace', () => __awaiter(void 0, void 0, void 0, function* () {
        childProcessAPIManager.mock('isCommandExist', true);
        vsCodeApiMockManager.mock('getWorkspaceFolders', [
            {uri: {fsPath: '/dir/helidonDir1/'}},
            {uri: {fsPath: '/dir/helidonDir2/'}}
        ]);
        vsCodeApiMockManager.mock('showPickOption').withArgs({
            title: "Choose a Helidon project that you want to start.",
            totalSteps: 1,
            currentStep: 1,
            placeholder: "Project name",
            items: [
                {label: 'helidonDir1', description: '/dir/helidonDir1/'},
                {label: 'helidonDir2', description: '/dir/helidonDir2/'}
            ]
        }).returns({description: '/dir/helidonDir2/'});
        fsSystemApiMockManager.mock('isPathExistsSync', true);
        fsSystemApiMockManager.mock('readDirSync', []);
        fsSystemApiMockManager.mock('readTextFileSync', '<dependencies><dependency>\n<groupId>helidon-group<\/groupId>\n');
        childProcessAPIManager.mock('spawnProcess', createServerProcess());
        vsCodeApiMockManager.mock('createOutputChannel', createOutputChannel());
        const launchedServers = yield helidonDev_1.startHelidonDev();
        expect(launchedServers.size).is.equal(1);
        expect(launchedServers.keys().next().value).is.equal('helidonDir2');
    }));
    test('No helidon servers start when there are no helidon projects in workspace', () => __awaiter(void 0, void 0, void 0, function* () {
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
        const launchedServers = yield helidonDev_1.startHelidonDev();
        expect(launchedServers.size).is.equal(0);
    }));
    test('A helidon server starts when an open folder with Helidon project exists in the workspace', () => __awaiter(void 0, void 0, void 0, function* () {
        childProcessAPIManager.mock('isCommandExist', true);
        vsCodeApiMockManager.mock('getWorkspaceFolders', [
            {uri: {fsPath: '/dir/helidonDir/'}}
        ]);
        fsSystemApiMockManager.mock('isPathExistsSync', true);
        fsSystemApiMockManager.mock('readDirSync', []);
        fsSystemApiMockManager.mock('readTextFileSync', '<dependencies><dependency>\n<groupId>helidon-group<\/groupId>\n');
        childProcessAPIManager.mock('spawnProcess', createServerProcess());
        vsCodeApiMockManager.mock('createOutputChannel', createOutputChannel());
        const launchedServers = yield helidonDev_1.startHelidonDev();
        expect(launchedServers.size).is.equal(1);
    }));
    test('No helidon servers start when there are no open folder in workspace', () => __awaiter(void 0, void 0, void 0, function* () {
        childProcessAPIManager.mock('isCommandExist', true);
        vsCodeApiMockManager.mock('getWorkspaceFolders', undefined);
        const launchedServers = yield helidonDev_1.startHelidonDev();
        expect(launchedServers.size).is.equal(0);
    }));
});

function createOutputChannel() {
    let outputChannel = {};
    outputChannel.show = sandbox.stub();
    return outputChannel;
}

function createServerProcess() {
    const serverProcess = new events.EventEmitter();
    serverProcess.stdin = new stream.Writable();
    serverProcess.stdout = new events.EventEmitter();
    serverProcess.stderr = new events.EventEmitter();
    return serverProcess;
}

//# sourceMappingURL=helidonDev.test.js.map