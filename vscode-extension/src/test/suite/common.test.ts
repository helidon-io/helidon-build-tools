/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

import * as vscode from "vscode";
import * as assert from "assert";
import {getPageContent} from "../../common";
import * as path from "path";

suite('Common functions Test Suite', () => {

    test('Content of the file should be loaded', async () => {
        let extensionPath = vscode.extensions.getExtension("Oracle.helidon")?.extensionPath;
        getPageContent(path.join(
            extensionPath ? extensionPath : '', 'assets', 'start_page.html'))
            .then(content =>
                assert(content, "File is not found")
            )
    });

});