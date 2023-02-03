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

import { QuickPickItem } from "vscode";
import * as vscode from "vscode";

export const HELIDON_OUTPUT_CHANNEL = "Helidon support";

export interface QuickPickItemExt extends QuickPickItem {
    children?: any;
    value?: string;
    description?: string;
}

export interface QuickPickData {
    title: string;
    placeholder: string;
    totalSteps: number;
    currentStep: number;
    items: QuickPickItemExt[];
    selectedItems?: QuickPickItemExt[];
    kind?: string;
    path?: string;
    id?: string;
}

// VS Code Helidon extension commands
export namespace VSCodeHelidonCommands {
    export const GENERATE_PROJECT = 'helidon.generate';
    export const DEV_SERVER_START = 'helidon.startDev';
    export const DEV_SERVER_STOP = 'helidon.stopDev';
    export const START_PAGE = 'helidon.startPage';
}

export namespace VSCodeJavaCommands {
    export const JAVA_MARKERS_COMMAND = 'microprofile/java/projectLabels';
}

export function getPageContent(pagePath: string): Thenable<string> {
    return vscode.workspace.openTextDocument(vscode.Uri.file(pagePath).fsPath)
        .then(doc => doc.getText());
}

export function validateUserInput(userInput: string, pattern: RegExp, errorMessage: string): string | undefined {
    if (!pattern.test(userInput)) {
        return errorMessage;
    }
    return undefined;
}

export function getSubstringBetween(initString: string, startSubstring: string, endSubstring: string): string {
    const startPosition = initString.lastIndexOf(startSubstring) + startSubstring.length;
    const endPosition = initString.lastIndexOf(endSubstring);
    return initString.substring(startPosition, endPosition).trim();
}

/**
 * Test array equality.
 * @param array1 {*}
 * @param array2 {*}
 * @return {boolean}
 */
export function arrayEquals(array1: any, array2: any): boolean {
    return Array.isArray(array1) &&
        Array.isArray(array2) &&
        array1.length === array2.length &&
        array1.every((value, index) => value === array2[index])
}