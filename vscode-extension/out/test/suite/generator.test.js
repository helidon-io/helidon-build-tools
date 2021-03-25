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
const assert = require("assert");
const helidonGenerator = require("../../generator");
const ts_mock_imports_1 = require("ts-mock-imports");
const vscodeApi = require("../../VSCodeAPI");
const fsSystemApi = require("../../FileSystemAPI");
const childProcApi = require("../../ChildProcessAPI");
const events = require("events");
const stream = require("stream");
let vsCodeApiMockManager;
let fsSystemApiMockManager;
let childProcessAPIManager;
suite('Helidon Project Generator Test Suite', () => {
    setup(() => {
        vsCodeApiMockManager = ts_mock_imports_1.ImportMock.mockStaticClass(vscodeApi, 'VSCodeAPI');
        fsSystemApiMockManager = ts_mock_imports_1.ImportMock.mockStaticClass(fsSystemApi, 'FileSystemAPI');
        childProcessAPIManager = ts_mock_imports_1.ImportMock.mockStaticClass(childProcApi, 'ChildProcessAPI');
    });
    teardown(() => {
        vsCodeApiMockManager.restore();
        fsSystemApiMockManager.restore();
        childProcessAPIManager.restore();
    });
    test('Correct flow leads to execute mvn command', () => __awaiter(void 0, void 0, void 0, function* () {
        vsCodeApiMockManager.mock('showPickOption', {
            label: "Helidon MP Bare",
            projectName: "helidon-bare-mp",
            packages: "io.helidon.examples.bare.mp"
        });
        vsCodeApiMockManager.mock('showInputBox', "test");
        vsCodeApiMockManager.mock('showOpenFolderDialog', {fsPath: "fsPath"});
        fsSystemApiMockManager.mock('isPathExistsSync', false);
        let childProcessMock = childProcessAPIManager.mock('execProcess', createChildProcess());
        yield helidonGenerator.showHelidonGenerator();
        assert(childProcessMock.calledOnce);
    }));
});

function createChildProcess() {
    const childProcess = new events.EventEmitter();
    childProcess.stdin = new stream.Writable();
    childProcess.stdout = new events.EventEmitter();
    childProcess.stderr = new events.EventEmitter();
    return childProcess;
}

//# sourceMappingURL=generator.test.js.map