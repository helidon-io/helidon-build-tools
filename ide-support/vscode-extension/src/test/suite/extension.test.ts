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
