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

import * as path from 'path';
import { InputBox, QuickInput, QuickInputButtons, QuickPick, Uri } from 'vscode';
import { getSubstringBetween, QuickPickData, QuickPickItemExt } from "./common";
import { VSCodeAPI } from "./VSCodeAPI";
import { FileSystemAPI } from "./FileSystemAPI";
import { ChildProcessAPI } from "./ChildProcessAPI";
import { BaseCommand, GeneratorData, GeneratorDataAPI, OptionCommand, TextCommand } from "./GeneratorCommand";
import { validateQuickPick, validateText } from "./validationApi";
import { getHelidonLangServerClient } from './languageServer';
import { logger } from './logger';
import { Interpreter } from "./Interpreter";
import { Context, ContextValueKind } from "./Context";

export async function showHelidonGenerator(extensionPath: string) {

    const PROJECT_READY: string = 'Your new project is ready.';
    const NEW_WINDOW: string = 'Open in new window';
    const CURRENT_WINDOW: string = 'Open in current window';
    const ADD_TO_WORKSPACE: string = 'Add to current workspace';

    const SELECT_FOLDER = 'Select folder';
    const OVERWRITE_EXISTING = 'Overwrite';
    const NEW_DIR = 'Choose new directory';
    const EXISTING_FOLDER = ' already exists in selected directory.';

    let interpreter: Interpreter;
    let generatorData: GeneratorData;
    let commandHistory: BaseCommand [];
    let archetype: any;

    async function generateProject(projectData: Map<string, string>) {
        const artifactId = <string>projectData.get('artifactId');
        const targetDir = await obtainTargetFolder(artifactId);
        if (!targetDir) {
            throw new Error('Helidon project generation has been canceled.');
        }

        VSCodeAPI.showInformationMessage('Your Helidon project is being created...');

        const archetypeValues = prepareProperties(projectData);
        const cmd = `java -jar ${extensionPath}/target/cli/helidon.jar init --batch \
            --reset --url file:///${extensionPath}/target/cli-data \
            ${archetypeValues}`;

        const channel = VSCodeAPI.createOutputChannel('helidon');
        channel.appendLine(cmd);

        const opts = {
            cwd: targetDir.fsPath
        };
        ChildProcessAPI.execProcess(cmd, opts, (error: string, stdout: string, stderr: string) => {
            channel.appendLine(stdout);
            if (stdout.includes("Switch directory to ")) {
                const projectDir = getSubstringBetween(stdout, "Switch directory to ", "to use CLI");
                VSCodeAPI.showInformationMessage('Project generated...');
                openPreparedProject(projectDir);
            } else {
                console.log(error.toString());
                VSCodeAPI.showInformationMessage('Project generation failed...');
            }
            if (stderr) {
                channel.appendLine(stderr);
            }
            if (error) {
                channel.appendLine(error);
            }
        });
    }

    function prepareProperties(propsMap: Map<string, string>): string {
        let result = "";
        for (let [name, value] of propsMap) {
            result += ` -D${name}=${value}`;
        }
        return result;
    }

    async function obtainTargetFolder(artifactId: string) {

        const specificFolderMessage = `'${artifactId}'` + EXISTING_FOLDER;
        let directory: Uri | undefined = await VSCodeAPI.showOpenFolderDialog({openLabel: SELECT_FOLDER});

        while (directory && FileSystemAPI.isPathExistsSync(path.join(directory.fsPath, artifactId))) {
            const choice: string | undefined = await VSCodeAPI.showWarningMessage(specificFolderMessage, OVERWRITE_EXISTING, NEW_DIR);
            if (choice === OVERWRITE_EXISTING) {
                // Following line deletes target folder recursively
                require("rimraf").sync(path.join(directory.fsPath, artifactId));
                break;
            } else if (choice === NEW_DIR) {
                directory = await VSCodeAPI.showOpenFolderDialog({openLabel: SELECT_FOLDER});
            } else {
                directory = undefined;
                break;
            }
        }

        return directory;
    }

    async function openPreparedProject(projectDir: string): Promise<void> {

        const openFolderCommand = 'vscode.openFolder';
        const newProjectFolderUri = getNewProjectFolder(projectDir);

        if (VSCodeAPI.getWorkspaceFolders()) {
            const input: string | undefined = await VSCodeAPI.showInformationMessage(PROJECT_READY, NEW_WINDOW, ADD_TO_WORKSPACE);
            if (!input) {
                return;
            } else if (input === ADD_TO_WORKSPACE) {
                VSCodeAPI.updateWorkspaceFolders(
                    VSCodeAPI.getWorkspaceFolders() ? VSCodeAPI.getWorkspaceFolders()!.length : 0,
                    undefined,
                    {uri: newProjectFolderUri}
                );
            } else {
                await VSCodeAPI.executeCommand(openFolderCommand, newProjectFolderUri, true);
            }
        } else if (VSCodeAPI.getVisibleTextEditors().length > 0) {
            // If VS does not have any project opened, but has some file opened in it.
            const input: string | undefined = await VSCodeAPI.showInformationMessage(PROJECT_READY, NEW_WINDOW, CURRENT_WINDOW);
            if (input) {
                await VSCodeAPI.executeCommand(openFolderCommand, newProjectFolderUri, NEW_WINDOW === input);
            }
        } else {
            await VSCodeAPI.executeCommand(openFolderCommand, newProjectFolderUri, false);
        }

    }

    function getNewProjectFolder(projectDir: string): Uri {
        return Uri.file(projectDir);
    }

    async function prepareArchetype() {
        try {
            let client = getHelidonLangServerClient();
            await client.sendRequest("workspace/executeCommand", {command: "helidon.archetype.v2.json"})
                .then(data => {
                    if (typeof data === "string") {
                        archetype = JSON.parse(data);
                    }

                });
        } catch (e) {
            if (typeof e === "string") {
                logger.error(e);
            } else if (e instanceof Error) {
                logger.error(e.message);
            }
        }
    }

    async function init() {
        await prepareArchetype();

        if (!archetype && !archetype.children) {
            return;
        }
        interpreter = new Interpreter(archetype, generatorData);


        for (let child of archetype.children) {
            const elements = interpreter.process(child);
            generatorData.elements.push(...elements);
        }
    }

    function getCurrentStep(): number {
        return generatorData.elements
            .filter((element, index) => element._skip === false && index <= generatorData.currentElementIndex)
            .length;
    }

    function getTotalSteps(): number {
        return generatorData.elements.filter((value: any) => value._skip === false).length;
    }

    function prepareQuickPickData(element: any): QuickPickData {
        let result: QuickPickData;
        result = element;
        result.currentStep = getCurrentStep();
        result.totalSteps = getTotalSteps();
        return result;
    }

    function getTextInput(element: any, resolve: any, reject: any): InputBox {
        const data = element;
        data.totalSteps = getTotalSteps();
        data.currentStep = getCurrentStep();
        const inputBox = VSCodeAPI.createInputBox(data);
        inputBox.buttons = [QuickInputButtons.Back]

        inputBox.show();
        let textCommand = new TextCommand(generatorData);

        inputBox.onDidAccept(async () => {
            const insertedValue = inputBox.value;
            const validation = validateText(insertedValue, element);
            if (validation.length > 0) {
                inputBox.validationMessage = validation.map(error => error.message).join(' ');
            } else {
                element.value = insertedValue;
                generatorData = textCommand.execute();
                interpreter.setGeneratorData(generatorData);
                commandHistory.push(textCommand);
                inputBox.dispose();
                resolve(insertedValue);
            }
        });
        inputBox.onDidTriggerButton(item => {
            if (item === QuickInputButtons.Back) {
                resolve(inputBox);
                if (commandHistory.length !== 0) {
                    generatorData = commandHistory.pop()!.undo();
                    interpreter.setGeneratorData(generatorData);
                }
            }
        });
        inputBox.onDidHide(() => {
            inputBox.dispose();
        });

        return inputBox;
    }

    function processSelected(element:any, selectedValues: any[], childrenOfSelected: any[]): OptionCommand {
        //will be used to evaluate expressions
        let contextValue = element.kind === `list` ? selectedValues : selectedValues[0];
        generatorData.context.setValue(element.path, contextValue, ContextValueKind.USER);

        let processedChildren: any[] = [];
        for (let child of childrenOfSelected) {
            const elements = interpreter.process(child);
            processedChildren.push(...elements);
        }

        let optionCommand = new OptionCommand(generatorData);
        optionCommand.selectedOptionsChildren(processedChildren);
        optionCommand.setContext(generatorData.context);
        generatorData = optionCommand.execute();
        interpreter.setGeneratorData(generatorData);
        return optionCommand;
    }

    function processAccept(element: any) {
        let selectedValues = element.selectedItems.map((item: QuickPickItemExt) => item.value);
        let children: any[] = [];
        for (let item of element.selectedItems) {
            let option = item;
            if (option.children) {
                children.push(...option.children);
            }
        }
        processSelected(element, selectedValues, children);
    }

    function getQuickPickInput(element: any, resolve: any, reject: any): QuickPick<any> {
        const data = prepareQuickPickData(element);
        const quickPick = VSCodeAPI.createQuickPick(data);
        quickPick.canSelectMany = true;
        quickPick.buttons = [QuickInputButtons.Back];
        quickPick.selectedItems = data.selectedItems ?? [];
        quickPick.activeItems = data.selectedItems ?? [];
        let lastSelectedItems = quickPick.selectedItems;

        quickPick.show();

        quickPick.onDidAccept(async () => {
            let selectedValues = quickPick.selectedItems.map(item => (<QuickPickItemExt>item).value);
            const validation = validateQuickPick(selectedValues, element);
            if (validation.length > 0) {
                quickPick.placeholder = validation.map(error => error.message).join(' ');
            } else {
                element.selectedValues = selectedValues;
                let children: any[] = [];
                for (let item of quickPick.selectedItems) {
                    let option = <QuickPickItemExt>item;
                    if (option.children) {
                        children.push(...option.children);
                    }
                }

                let optionCommand = processSelected(element, selectedValues, children);
                commandHistory.push(optionCommand);

                resolve(quickPick.selectedItems);
            }
        });
        quickPick.onDidChangeSelection(items => {
            if (element.kind !== 'list') {
                let newElements = quickPick.selectedItems.filter(item => !lastSelectedItems.includes(item));
                if (newElements.length != 0) {
                    quickPick.selectedItems = quickPick.selectedItems.filter(item => !lastSelectedItems.includes(item));
                }
                if (items.length == 0) {
                    quickPick.selectedItems = lastSelectedItems;
                }

                lastSelectedItems = quickPick.selectedItems;
                Object.defineProperty(data, 'selectedItems', quickPick.selectedItems);
                element.selectedItems = quickPick.selectedItems;
            }
        });
        quickPick.onDidTriggerButton(item => {
            if (item === QuickInputButtons.Back) {
                resolve(quickPick);
                if (commandHistory.length !== 0) {
                    generatorData = commandHistory.pop()!.undo();
                    interpreter.setGeneratorData(generatorData);
                }
            }
        });
        quickPick.onDidHide(() => {
            quickPick.dispose();
        });
        return quickPick;
    }


    function getInput(element: any): Promise<QuickInput> | undefined {
        let result: Promise<QuickInput> | undefined;
        if (element.kind === 'enum') {
            result = new Promise<QuickInput>((resolve, reject) => getQuickPickInput(element, resolve, reject));
        }
        if (element.kind === 'list') {
            result = new Promise<QuickInput>((resolve, reject) => getQuickPickInput(element, resolve, reject));
        }
        if (element.kind === 'boolean') {
            result = new Promise<QuickInput>((resolve, reject) => getQuickPickInput(element, resolve, reject));
        }
        if (element.kind === 'text') {
            result = new Promise<QuickInput>((resolve, reject) => getTextInput(element, resolve, reject));
        }
        return result;
    }

    try {
        generatorData = {steps: [], elements: [], currentElementIndex: 0, context: new Context()};
        commandHistory = [];

        await init();
        let maxIterationCount = 100;
        let currentInput: Promise<QuickInput> | undefined;
        while (generatorData.currentElementIndex < generatorData.elements.length) {
            let element = generatorData.elements[generatorData.currentElementIndex];
            generatorData.context.pushScope(element._scope);
            if (element._skip === true) {
                processAccept(element);
                continue;
            }
            currentInput = getInput(element);
            if (currentInput != null) {
                await currentInput;
            }
            currentInput = undefined;
            if (maxIterationCount-- < 0) {
                break;
            }
        }
        if (generatorData.elements.length !== 0) {
            await generateProject(GeneratorDataAPI.convertProjectDataElements(generatorData));
        }
    } catch (e: any) {
        VSCodeAPI.showErrorMessage(e.message);
    }

}
