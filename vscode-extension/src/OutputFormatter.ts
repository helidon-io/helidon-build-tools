/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
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