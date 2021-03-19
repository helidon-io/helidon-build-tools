/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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

import * as vscode from "vscode";
import * as path from "path";
import {getPageContent} from "./common";

export async function openStartPage(context: vscode.ExtensionContext) {

    const RESOURCE_FOLDER: string = 'assets';

    // Create and show panel
    const panel = vscode.window.createWebviewPanel(
        'helidon_generator',
        'helidon',
        vscode.ViewColumn.One,
        {
            enableCommandUris: true,
            enableScripts: true
        }
    );
    panel.iconPath = {
        light: vscode.Uri.file(path.join(context.extensionPath, RESOURCE_FOLDER, 'icons', 'favicon.png')),
        dark: vscode.Uri.file(path.join(context.extensionPath, RESOURCE_FOLDER, 'icons', 'favicon.png'))
    };

    getPageContent(path.join(context.extensionPath, RESOURCE_FOLDER, 'start_page.html'))
        .then(content => panel.webview.html = content);
}

