/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates. All rights reserved.
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

import * as assert from "assert";
import { OutputFormatter } from "../../OutputFormatter";
import { OutputChannel } from "vscode";

let sandbox: any;
const sinon = require("sinon");

suite('OutputFormatter functions Test Suite', () => {

    setup(() => {
        sandbox = sinon.createSandbox();
    });

    teardown(() => {
        sandbox.restore();
    });

    test('Input string does not contain new line character - no data send', async () => {
        const inputString: string = "string without new line character";
        const outputChannel = getOutputChannel();
        const appendLineSpy = sinon.spy(outputChannel, 'appendLine');
        const outputFormatter = new OutputFormatter(outputChannel);

        outputFormatter.formatInputString(inputString);

        assert(appendLineSpy.notCalled);
    });

    test('Input string finished with newline character - split input string is sent to outputChannel', async () => {
        const inputString1: string = "string with newline character\n";
        const inputString2: string = "string with combined newline characters\r\n";
        const expectedString1: string = "string with newline character";
        const expectedString2: string = "string with combined newline characters";
        const outputChannel = getOutputChannel();
        const appendLineSpy = sinon.spy(outputChannel, 'appendLine');
        const outputFormatter = new OutputFormatter(outputChannel);

        outputFormatter.formatInputString(inputString1);
        outputFormatter.formatInputString(inputString2);

        assert(appendLineSpy.withArgs(expectedString1).calledOnce);
        assert(appendLineSpy.withArgs(expectedString2).calledOnce);
    });

    test('Input string has with a few newline characters - split input string is sent to outputChannel without last part', async () => {
        const inputString1: string = "string \n\r with newline\r\n character\n is sent to outputChannel";
        const expectedString1: string = "string ";
        const expectedString2: string = " with newline";
        const expectedString3: string = " character";
        const outputChannel = getOutputChannel();
        const appendLineSpy = sinon.spy(outputChannel, 'appendLine');
        const outputFormatter = new OutputFormatter(outputChannel);

        outputFormatter.formatInputString(inputString1);

        assert(appendLineSpy.withArgs(expectedString1).calledOnce);
        assert(appendLineSpy.withArgs(expectedString2).calledOnce);
        assert(appendLineSpy.withArgs(expectedString3).calledOnce);
    });

    test('Input string has with a few newline characters - split input string is sent to outputChannel', async () => {
        const inputString1: string = "string \n\r with newline\r\n character\n is sent to outputChannel\n";
        const expectedString1: string = "string ";
        const expectedString2: string = " with newline";
        const expectedString3: string = " character";
        const expectedString4: string = " is sent to outputChannel";
        const outputChannel = getOutputChannel();
        const appendLineSpy = sinon.spy(outputChannel, 'appendLine');
        const outputFormatter = new OutputFormatter(outputChannel);

        outputFormatter.formatInputString(inputString1);

        assert(appendLineSpy.withArgs(expectedString1).calledOnce);
        assert(appendLineSpy.withArgs(expectedString2).calledOnce);
        assert(appendLineSpy.withArgs(expectedString3).calledOnce);
        assert(appendLineSpy.withArgs(expectedString4).calledOnce);
    });

});

function getOutputChannel(): OutputChannel {
    return <OutputChannel>{
        appendLine(value: string) {
        }
    };
}