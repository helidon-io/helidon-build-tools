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

export class FileSystemAPI {

    static fs = require('fs');
    static path = require('path');
    static os = require('os');

    constructor() {
    }

    public static mkDir(path: string) {
        return this.fs.mkdirSync( path, { recursive: true } );
    }

    public static tempDir() {
        return this.os.tmpdir();
    }

    public static resolvePath(paths: string[]) {
        return this.path.resolve(...paths);
    }

    public static isPathExistsSync(path: string): boolean {
        return this.fs.existsSync(path);
    }

    public static readDirSync(path: string): string[] {
        return this.fs.readdirSync(path);
    }

    public static readTextFileSync(path: string, charset: string): string {
        return this.fs.readFileSync(path, charset);
    }

    public static isDirectorySync(filePath: string): boolean {
        return this.fs.lstatSync(filePath).isDirectory();
    }
}