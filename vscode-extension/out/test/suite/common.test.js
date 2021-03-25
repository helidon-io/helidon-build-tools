"use strict";
/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) {
        return value instanceof P ? value : new P(function (resolve) {
            resolve(value);
        });
    }

    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) {
            try {
                step(generator.next(value));
            } catch (e) {
                reject(e);
            }
        }

        function rejected(value) {
            try {
                step(generator["throw"](value));
            } catch (e) {
                reject(e);
            }
        }

        function step(result) {
            result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected);
        }

        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", {value: true});
const vscode = require("vscode");
const assert = require("assert");
const common_1 = require("../../common");
const path = require("path");
suite('Common functions Test Suite', () => {
    test('Content of the file should be loaded', () => __awaiter(void 0, void 0, void 0, function* () {
        var _a;
        let extensionPath = (_a = vscode.extensions.getExtension("Oracle.helidon")) === null || _a === void 0 ? void 0 : _a.extensionPath;
        common_1.getPageContent(path.join(extensionPath ? extensionPath : '', 'assets', 'start_page.html'))
            .then(content => assert(content, "File is not found"));
    }));
});
//# sourceMappingURL=common.test.js.map