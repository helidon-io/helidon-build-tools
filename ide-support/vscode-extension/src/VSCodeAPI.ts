/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates. All rights reserved.
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

import { QuickPickData } from "./common";
import {
    commands, InputBox,
    OpenDialogOptions,
    OutputChannel,
    QuickPickItem,
    TextEditor,
    Uri,
    window,
    workspace,
    WorkspaceFolder
} from "vscode";

export interface InputBoxData {
    title: string;
    placeholder: string;
    value: string;
    prompt: string;
    totalSteps: number;
    currentStep: number;
    messageValidation: (value: string) => string | undefined;
}

export class VSCodeAPI {

    constructor() {
    }

    public static async showOpenFolderDialog(customOptions: OpenDialogOptions): Promise<Uri | undefined> {
        const openDialogOptions: OpenDialogOptions = {
            canSelectFolders: true,
            canSelectFiles: false,
            canSelectMany: false,
        };

        const result: Uri[] | undefined = await window.showOpenDialog(Object.assign(openDialogOptions, customOptions));
        if (result && result.length > 0) {
            return Promise.resolve(result[0]);
        } else {
            return Promise.resolve(undefined);
        }
    }

    public static getWorkspaceFolders(): readonly WorkspaceFolder[] | undefined {
        return workspace.workspaceFolders;
    }

    public static createOutputChannel(name: string): OutputChannel {
        return window.createOutputChannel(name);
    }

    public static createInputBox(): InputBox {
        return window.createInputBox();
    }

    public static async showPickOption(data: QuickPickData) {
        return await new Promise<QuickPickItem | undefined>((resolve, reject) => {
            const quickPick = window.createQuickPick();
            quickPick.title = data.title;
            quickPick.totalSteps = data.currentStep;
            quickPick.step = data.currentStep;
            quickPick.items = data.items;
            quickPick.ignoreFocusOut = true;
            quickPick.canSelectMany = false;
            quickPick.placeholder = data.placeholder;

            quickPick.show();
            quickPick.onDidAccept(async () => {
                if (quickPick.selectedItems[0]) {
                    resolve(quickPick.selectedItems[0]);
                    quickPick.dispose();
                }
            });
            quickPick.onDidHide(() => {
                quickPick.dispose();
            });
        });
    }

    public static showErrorMessage(message: string) {
        window.showErrorMessage(message);
    }

    public static showInformationMessage(message: string, ...items: string[]): Thenable<string | undefined> {
        return window.showInformationMessage(message, ...items);
    }

    public static showWarningMessage(message: string, ...items: string[]): Thenable<string | undefined> {
        return window.showWarningMessage(message, ...items);
    }

    public static updateWorkspaceFolders(start: number, deleteCount: number | undefined | null, ...workspaceFoldersToAdd: { uri: Uri, name?: string }[]): boolean {
        return workspace.updateWorkspaceFolders(start, deleteCount, ...workspaceFoldersToAdd);
    }

    public static executeCommand<T>(command: string, ...rest: any[]): Thenable<T | undefined> {
        return commands.executeCommand(command, ...rest);
    }

    public static getVisibleTextEditors(): readonly TextEditor[] {
        return window.visibleTextEditors;
    }

    public static async showInputBox(data: InputBoxData) {
        return await new Promise<string | undefined>((resolve, rejects) => {
            const inputBox = window.createInputBox();
            inputBox.title = data.title;
            inputBox.placeholder = data.placeholder;
            inputBox.prompt = data.prompt;
            inputBox.value = data.value;
            inputBox.totalSteps = data.totalSteps;
            inputBox.step = data.currentStep;
            inputBox.ignoreFocusOut = true;

            inputBox.show();
            inputBox.onDidAccept(async () => {
                const t = inputBox.value;
                const validation: string | undefined = await data.messageValidation(t);
                if (validation) {
                    inputBox.validationMessage = validation;
                } else {
                    inputBox.dispose();
                    resolve(t);
                }
            });
            inputBox.onDidChangeValue(async text => {
                const validation: string | undefined = await data.messageValidation(text);
                inputBox.validationMessage = validation;
            });
            inputBox.onDidHide(() => {
                inputBox.dispose();
            });
        });
    }
}