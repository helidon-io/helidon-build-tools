/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

import * as assert from 'assert';
import * as vscode from 'vscode';
import {VSCodeHelidonCommands} from "../../common";
import {expect} from 'chai';

suite('Extension Test Suite', () => {

    test('Extension should be present', async () => {
        const extension = vscode.extensions.getExtension("Oracle.helidon");
        assert(extension, "Extension is not present");
    });

    test('Extension has commands', async () => {
        const extensionJson = vscode.extensions.getExtension("Oracle.helidon")?.packageJSON;
        const commands = extensionJson?.activationEvents.filter((s: string) => s.startsWith('onCommand:'))
            .map((s: string) => s.substring('onCommand:'.length));
        const HELIDON_COMMANDS: string[] = [
            VSCodeHelidonCommands.GENERATE_PROJECT,
            VSCodeHelidonCommands.DEV_SERVER_START,
            VSCodeHelidonCommands.START_PAGE,
            VSCodeHelidonCommands.DEV_SERVER_STOP
        ];

        expect(commands).to.have.members(HELIDON_COMMANDS);
    });

});
