/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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

import { ChildProcess } from "child_process";

export class ChildProcessAPI {

    static childProcess = require('child_process');
    static kill = require('tree-kill');
    static commandExistsSync = require('command-exists').sync;

    public static spawnProcess(command: String, args: string[], options: Object): ChildProcess {
        return this.childProcess.spawn(command, args, options);
    }

    public static spawnSyncProcess(command: String, args: string[], options: Object): ChildProcess {
        return this.childProcess.spawnSync(command, args, options);
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