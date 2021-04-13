/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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

import {OutputChannel} from "vscode";

/**
 * Formats and sends string to OutputChannel.
 */
export class OutputFormatter {

    tempString: string = '';
    stripAnsi = require('strip-ansi');

    constructor(readonly outputChannel: OutputChannel) {
    }

    /**
     * Formats string for OutputChannel.
     * Splits input string using new line character as a separator and send to OutputChannel only those strings that finished with new line character.
     * Other strings are stored in temporary string until method receive the string with new line character.
     *
     * @param data Input string
     */
    public formatInputString(data: string) {
        data = data.toString();

        let countFinishedLines = data.match(/[\n\r]/g)?.length ?? 0;
        if (countFinishedLines === 0) {
            this.tempString += data;
            return;
        }
        let splitData = data.split(/[\n\r]/g);
        if (splitData[splitData.length - 1] === '') {
            splitData.splice(splitData.length - 1, 1);
        }
        let lastOutputLineIndex = 0;
        if (splitData.length === countFinishedLines) {
            lastOutputLineIndex = countFinishedLines;
            this.outputLines(splitData, lastOutputLineIndex);
        } else {
            lastOutputLineIndex = countFinishedLines;
            this.outputLines(splitData, lastOutputLineIndex);
            this.tempString = splitData[splitData.length - 1];
        }
    }

    outputLines(lines: string[], lastIndex: number) {
        for (let i = 0; i < lastIndex; i++) {
            if (i === 0) {
                this.outputChannel.appendLine(this.stripAnsi(this.tempString + lines[i]));
                this.tempString = '';
            } else {
                this.outputChannel.appendLine(this.stripAnsi(lines[i]));
            }
        }
    }
}