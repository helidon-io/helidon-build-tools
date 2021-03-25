/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

export class FileSystemAPI {

    static fs = require('fs');

    constructor() {
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