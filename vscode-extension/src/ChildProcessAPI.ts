/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

import {ChildProcess} from "child_process";

export class ChildProcessAPI {

    static childProcess = require('child_process');
    static kill = require('tree-kill');
    static commandExistsSync = require('command-exists').sync;

    public static spawnProcess(command: String, args: string[], options: Object): ChildProcess {
        return this.childProcess.spawn(command, args, options);
    }

    public static execProcess(command: String, options: Object, callback: Function): ChildProcess {
        return this.childProcess.exec(command, options, callback);
    }

    public static killProcess(processPid: number) {
        this.kill(processPid);
    }

    public static isCommandExist(command: string): boolean {
        return this.commandExistsSync(command);
    }
}