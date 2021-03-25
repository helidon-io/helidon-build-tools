"use strict";
/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */
Object.defineProperty(exports, "__esModule", {value: true});
exports.OutputFormatter = void 0;

/**
 * Formats and sends string to OutputChannel.
 */
class OutputFormatter {
    constructor(outputChannel) {
        this.outputChannel = outputChannel;
        this.tempString = '';
        this.stripAnsi = require('strip-ansi');
    }

    /**
     * Formats string for OutputChannel.
     * Splits input string using new line character as a separator and send to OutputChannel only those strings that finished with new line character.
     * Other strings are stored in temporary string until method receive the string with new line character.
     *
     * @param data Input string
     */
    formatInputString(data) {
        var _a, _b;
        data = data.toString();
        let countFinishedLines = (_b = (_a = data.match(/[\n\r]/g)) === null || _a === void 0 ? void 0 : _a.length) !== null && _b !== void 0 ? _b : 0;
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

    outputLines(lines, lastIndex) {
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

exports.OutputFormatter = OutputFormatter;
//# sourceMappingURL=OutputFormatter.js.map