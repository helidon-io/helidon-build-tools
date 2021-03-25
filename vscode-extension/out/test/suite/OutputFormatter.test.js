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
const OutputFormatter_1 = require("../../OutputFormatter");
let sandbox;
const sinon = require("sinon");
suite('OutputFormatter functions Test Suite', () => {
    setup(() => {
        sandbox = sinon.createSandbox();
    });
    teardown(() => {
        sandbox.restore();
    });
    test('Input string does not contain new line character - no data send', () => __awaiter(void 0, void 0, void 0, function* () {
        const inputString = "string without new line character";
        const outputChannel = getOutputChannel();
        const appendLineSpy = sinon.spy(outputChannel, 'appendLine');
        const outputFormatter = new OutputFormatter_1.OutputFormatter(outputChannel);
        outputFormatter.formatInputString(inputString);
        assert(appendLineSpy.notCalled);
    }));
    test('Input string finished with newline character - split input string is sent to outputChannel', () => __awaiter(void 0, void 0, void 0, function* () {
        const inputString1 = "string with newline character\n";
        const inputString2 = "string with combined newline characters\r\n";
        const expectedString1 = "string with newline character";
        const expectedString2 = "string with combined newline characters";
        const outputChannel = getOutputChannel();
        const appendLineSpy = sinon.spy(outputChannel, 'appendLine');
        const outputFormatter = new OutputFormatter_1.OutputFormatter(outputChannel);
        outputFormatter.formatInputString(inputString1);
        outputFormatter.formatInputString(inputString2);
        assert(appendLineSpy.withArgs(expectedString1).calledOnce);
        assert(appendLineSpy.withArgs(expectedString2).calledOnce);
    }));
    test('Input string has with a few newline characters - split input string is sent to outputChannel without last part', () => __awaiter(void 0, void 0, void 0, function* () {
        const inputString1 = "string \n\r with newline\r\n character\n is sent to outputChannel";
        const expectedString1 = "string ";
        const expectedString2 = " with newline";
        const expectedString3 = " character";
        const outputChannel = getOutputChannel();
        const appendLineSpy = sinon.spy(outputChannel, 'appendLine');
        const outputFormatter = new OutputFormatter_1.OutputFormatter(outputChannel);
        outputFormatter.formatInputString(inputString1);
        assert(appendLineSpy.withArgs(expectedString1).calledOnce);
        assert(appendLineSpy.withArgs(expectedString2).calledOnce);
        assert(appendLineSpy.withArgs(expectedString3).calledOnce);
    }));
    test('Input string has with a few newline characters - split input string is sent to outputChannel', () => __awaiter(void 0, void 0, void 0, function* () {
        const inputString1 = "string \n\r with newline\r\n character\n is sent to outputChannel\n";
        const expectedString1 = "string ";
        const expectedString2 = " with newline";
        const expectedString3 = " character";
        const expectedString4 = " is sent to outputChannel";
        const outputChannel = getOutputChannel();
        const appendLineSpy = sinon.spy(outputChannel, 'appendLine');
        const outputFormatter = new OutputFormatter_1.OutputFormatter(outputChannel);
        outputFormatter.formatInputString(inputString1);
        assert(appendLineSpy.withArgs(expectedString1).calledOnce);
        assert(appendLineSpy.withArgs(expectedString2).calledOnce);
        assert(appendLineSpy.withArgs(expectedString3).calledOnce);
        assert(appendLineSpy.withArgs(expectedString4).calledOnce);
    }));
});

function getOutputChannel() {
    return {
        appendLine(value) {
        }
    };
}

//# sourceMappingURL=OutputFormatter.test.js.map