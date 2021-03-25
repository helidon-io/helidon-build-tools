"use strict";
/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */
Object.defineProperty(exports, "__esModule", {value: true});
exports.ChildProcessAPI = void 0;

class ChildProcessAPI {
    static spawnProcess(command, args, options) {
        return this.childProcess.spawn(command, args, options);
    }

    static execProcess(command, options, callback) {
        return this.childProcess.exec(command, options, callback);
    }

    static killProcess(processPid) {
        this.kill(processPid);
    }

    static isCommandExist(command) {
        return this.commandExistsSync(command);
    }
}

exports.ChildProcessAPI = ChildProcessAPI;
ChildProcessAPI.childProcess = require('child_process');
ChildProcessAPI.kill = require('tree-kill');
ChildProcessAPI.commandExistsSync = require('command-exists').sync;
//# sourceMappingURL=ChildProcessAPI.js.map