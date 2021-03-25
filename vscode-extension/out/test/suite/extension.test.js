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
const assert = require("assert");
const vscode = require("vscode");
const common_1 = require("../../common");
const chai_1 = require("chai");
suite('Extension Test Suite', () => {
    test('Extension should be present', () => __awaiter(void 0, void 0, void 0, function* () {
        const extension = vscode.extensions.getExtension("Oracle.helidon");
        assert(extension, "Extension is not present");
    }));
    test('Extension has commands', () => __awaiter(void 0, void 0, void 0, function* () {
        var _a;
        const extensionJson = (_a = vscode.extensions.getExtension("Oracle.helidon")) === null || _a === void 0 ? void 0 : _a.packageJSON;
        const commands = extensionJson === null || extensionJson === void 0 ? void 0 : extensionJson.activationEvents.filter((s) => s.startsWith('onCommand:')).map((s) => s.substring('onCommand:'.length));
        const HELIDON_COMMANDS = [
            common_1.VSCodeHelidonCommands.GENERATE_PROJECT,
            common_1.VSCodeHelidonCommands.DEV_SERVER_START,
            common_1.VSCodeHelidonCommands.START_PAGE,
            common_1.VSCodeHelidonCommands.DEV_SERVER_STOP
        ];
        chai_1.expect(commands).to.have.members(HELIDON_COMMANDS);
    }));
});
//# sourceMappingURL=extension.test.js.map