/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
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

