"use strict";
/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */
Object.defineProperty(exports, "__esModule", {value: true});
exports.FileSystemAPI = void 0;

class FileSystemAPI {
    constructor() {
    }

    static isPathExistsSync(path) {
        return this.fs.existsSync(path);
    }

    static readDirSync(path) {
        return this.fs.readdirSync(path);
    }

    static readTextFileSync(path, charset) {
        return this.fs.readFileSync(path, charset);
    }

    static isDirectorySync(filePath) {
        return this.fs.lstatSync(filePath).isDirectory();
    }
}

exports.FileSystemAPI = FileSystemAPI;
FileSystemAPI.fs = require('fs');
//# sourceMappingURL=FileSystemAPI.js.map