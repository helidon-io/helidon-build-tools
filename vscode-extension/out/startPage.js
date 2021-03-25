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
exports.openStartPage = void 0;
const vscode = require("vscode");
const path = require("path");
const common_1 = require("./common");

function openStartPage(context) {
    return __awaiter(this, void 0, void 0, function* () {
        const RESOURCE_FOLDER = 'assets';
        // Create and show panel
        const panel = vscode.window.createWebviewPanel('helidon_generator', 'helidon', vscode.ViewColumn.One, {
            enableCommandUris: true,
            enableScripts: true
        });
        panel.iconPath = {
            light: vscode.Uri.file(path.join(context.extensionPath, RESOURCE_FOLDER, 'icons', 'favicon.png')),
            dark: vscode.Uri.file(path.join(context.extensionPath, RESOURCE_FOLDER, 'icons', 'favicon.png'))
        };
        common_1.getPageContent(path.join(context.extensionPath, RESOURCE_FOLDER, 'start_page.html'))
            .then(content => panel.webview.html = content);
    });
}

exports.openStartPage = openStartPage;
//# sourceMappingURL=startPage.js.map