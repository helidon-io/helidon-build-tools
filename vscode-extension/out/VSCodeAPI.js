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
exports.VSCodeAPI = void 0;
const vscode_1 = require("vscode");

class VSCodeAPI {
    constructor() {
    }

    static showOpenFolderDialog(customOptions) {
        return __awaiter(this, void 0, void 0, function* () {
            const openDialogOptions = {
                canSelectFolders: true,
                canSelectFiles: false,
                canSelectMany: false,
            };
            const result = yield vscode_1.window.showOpenDialog(Object.assign(openDialogOptions, customOptions));
            if (result && result.length > 0) {
                return Promise.resolve(result[0]);
            } else {
                return Promise.resolve(undefined);
            }
        });
    }

    static getWorkspaceFolders() {
        return vscode_1.workspace.workspaceFolders;
    }

    static createOutputChannel(name) {
        return vscode_1.window.createOutputChannel(name);
    }

    static createInputBox() {
        return vscode_1.window.createInputBox();
    }

    static showPickOption(data) {
        return __awaiter(this, void 0, void 0, function* () {
            return yield new Promise((resolve, reject) => {
                let quickPick = vscode_1.window.createQuickPick();
                quickPick.title = data.title;
                quickPick.totalSteps = data.currentStep;
                quickPick.step = data.currentStep;
                quickPick.items = data.items;
                quickPick.ignoreFocusOut = true;
                quickPick.canSelectMany = false;
                quickPick.placeholder = data.placeholder;
                quickPick.show();
                quickPick.onDidAccept(() => __awaiter(this, void 0, void 0, function* () {
                    if (quickPick.selectedItems[0]) {
                        resolve(quickPick.selectedItems[0]);
                        quickPick.dispose();
                    }
                }));
                quickPick.onDidHide(() => {
                    quickPick.dispose();
                });
            });
        });
    }

    static showErrorMessage(message) {
        vscode_1.window.showErrorMessage(message);
    }

    static showInformationMessage(message, ...items) {
        return vscode_1.window.showInformationMessage(message, ...items);
    }

    static showWarningMessage(message, ...items) {
        return vscode_1.window.showWarningMessage(message, ...items);
    }

    static updateWorkspaceFolders(start, deleteCount, ...workspaceFoldersToAdd) {
        return vscode_1.workspace.updateWorkspaceFolders(start, deleteCount, ...workspaceFoldersToAdd);
    }

    static executeCommand(command, ...rest) {
        return vscode_1.commands.executeCommand(command, ...rest);
    }

    static getVisibleTextEditors() {
        return vscode_1.window.visibleTextEditors;
    }

    static showInputBox(data) {
        return __awaiter(this, void 0, void 0, function* () {
            return yield new Promise((resolve, rejects) => {
                // let inputBox = window.createInputBox();
                let inputBox = vscode_1.window.createInputBox();
                inputBox.title = data.title;
                inputBox.placeholder = data.placeholder;
                inputBox.prompt = data.prompt;
                inputBox.value = data.value;
                inputBox.totalSteps = data.totalSteps;
                inputBox.step = data.currentStep;
                inputBox.ignoreFocusOut = true;
                inputBox.show();
                inputBox.onDidAccept(() => __awaiter(this, void 0, void 0, function* () {
                    let t = inputBox.value;
                    let validation = yield data.messageValidation(t);
                    if (validation) {
                        inputBox.validationMessage = validation;
                    } else {
                        inputBox.dispose();
                        resolve(t);
                    }
                }));
                inputBox.onDidChangeValue((text) => __awaiter(this, void 0, void 0, function* () {
                    let validation = yield data.messageValidation(text);
                    inputBox.validationMessage = validation;
                }));
                inputBox.onDidHide(() => {
                    inputBox.dispose();
                });
            });
        });
    }
}

exports.VSCodeAPI = VSCodeAPI;
//# sourceMappingURL=VSCodeAPI.js.map