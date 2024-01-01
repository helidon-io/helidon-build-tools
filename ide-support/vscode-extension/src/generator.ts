/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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
import { getSubstringBetween, HELIDON_OUTPUT_CHANNEL, QuickPickData, QuickPickItemExt } from "./common";
import { VSCodeAPI } from "./VSCodeAPI";
import { FileSystemAPI } from "./FileSystemAPI";
import { ChildProcessAPI } from "./ChildProcessAPI";
import { BaseCommand, GeneratorData, GeneratorDataAPI, OptionCommand, TextCommand } from "./GeneratorCommand";
import { validateQuickPick, validateText } from "./validationApi";
import fetch from 'node-fetch';

import { logger } from './logger';
import { Interpreter } from "./Interpreter";
import { Context, ContextScope, ContextValueKind } from "./Context";

const PROJECT_READY: string = 'Your new project is ready.';
const NEW_WINDOW: string = 'Open in new window';
const CURRENT_WINDOW: string = 'Open in current window';
const ADD_TO_WORKSPACE: string = 'Add to current workspace';

const SELECT_FOLDER = 'Select folder';
const OVERWRITE_EXISTING = 'Overwrite';
const NEW_DIR = 'Choose new directory';
const EXISTING_FOLDER = ' already exists in selected directory.';

const ARCHETYPE_CACHE: Map<string, any> = new Map();
const VERSIONS_URL: string = 'https://helidon.io/api/versions';
const ARCHETYPES_URL_PREFIX = 'https://helidon.io/api/starter/';

const channel = VSCodeAPI.outputChannel(HELIDON_OUTPUT_CHANNEL);

export async function showHelidonGenerator(extensionPath: string) {

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
        const cmd = `java -jar ${extensionPath}/target/cli/helidon-cli.jar --plain init --batch \
            ${archetypeValues}`;

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
                logger.error(error);
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
        for (const [name, value] of propsMap) {
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

    interface HelidonVersions {
        versions: string[];
        latest: string;
    }

    async function initArchetypes() {
        const helidonVersion: string = generatorData.elements[0].selectedValues[0];
        archetype = ARCHETYPE_CACHE.get(helidonVersion);
        if (!archetype) {
            const helidonArchetypeResponse: any = await fetch(ARCHETYPES_URL_PREFIX+helidonVersion);
            archetype = await helidonArchetypeResponse.json();
            ARCHETYPE_CACHE.set(helidonVersion, archetype);
        }

        if (!archetype && !archetype.children) {
            const errorMessage = "Unable to get Helidon archetype."
            logger.error(errorMessage);
            VSCodeAPI.showErrorMessage(errorMessage);
            throw new Error(errorMessage);
        }

        const temp = generatorData.elements[0];
        generatorData.elements = [];
        generatorData.elements.push(temp);
        generatorData.context = new Context();
        interpreter = new Interpreter(archetype, generatorData);

        for (const child of archetype.children) {
            const elements = interpreter.process(child);
            generatorData.elements.push(...elements);
        }
    }

    async function init() {
        try {
            const helidonVersionsResponse: any = await fetch(VERSIONS_URL);
            const helidonVersions: HelidonVersions =await helidonVersionsResponse.json();
            // eslint-disable-next-line eqeqeq
            if (helidonVersions == null || Object.keys(helidonVersions).length === 0) {
                return;
            }

            const options: QuickPickItemExt[] = [];
            const defaultValue: any[] = [helidonVersions.latest];

            options.push(...helidonVersions.versions.map((o: string) => {
                return {
                    label: o,
                    value: o
                }
            }));

            const selectedOptions: QuickPickItemExt[] = [];
            selectedOptions.push(...options.filter(option => defaultValue.includes(option.value)));

            const result = {
                title: "Helidon version",
                placeholder: "Helidon version (optional -false). Press 'Enter' to confirm.",
                items: options,
                selectedItems: selectedOptions,
                kind: "enum",
                id: "helidon.version",
                path: "helidon.version",
                _scope: new ContextScope(null, null),
                _skip: false,
                additionalInstructions: initArchetypes
            };
            result._scope = generatorData.context.newScope(result);

            generatorData.elements.push(result);
            await getInput(generatorData.elements[generatorData.currentElementIndex]);
        } catch (e) {
            if (typeof e === "string") {
                VSCodeAPI.showErrorMessage(`Cannot get information about Helidon archetypes : ${e}`);
                logger.error(e);
                channel.appendLine(e);
            } else if (e instanceof Error) {
                VSCodeAPI.showErrorMessage(`Cannot get information about Helidon archetypes : ${e.message}`);
                logger.error(e.stack);
                channel.appendLine(e.stack ?? e.message);
            }
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
        const result: QuickPickData = element;
        result.currentStep = getCurrentStep();
        result.totalSteps = getTotalSteps();
        return result;
    }

    function processUndoAction() {
        if (commandHistory.length !== 0) {
            do {
                generatorData = commandHistory.pop()!.undo();
            } while (generatorData.elements[generatorData.currentElementIndex]._skip === true);
            interpreter.setGeneratorData(generatorData);
        }
    }

    function getTextInput(element: any, resolve: any, reject: any): InputBox {
        const data = element;
        data.totalSteps = getTotalSteps();
        data.currentStep = getCurrentStep();
        const inputBox = VSCodeAPI.createInputBox(data);
        inputBox.buttons = [QuickInputButtons.Back]

        inputBox.show();
        const textCommand = new TextCommand(generatorData);

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
                processUndoAction();
            }
        });
        inputBox.onDidHide(() => {
            inputBox.dispose();
        });

        return inputBox;
    }

    function processSelected(element: any, selectedValues: any[], childrenOfSelected: any[]): OptionCommand {
        // will be used to evaluate expressions
        const contextValue = element.kind === `list` ? selectedValues : selectedValues[0];
        generatorData.context.setValue(element.path, contextValue, ContextValueKind.USER);

        const optionCommand = new OptionCommand(generatorData);

        const processedChildren: any[] = [];
        for (const child of childrenOfSelected) {
            const elements = interpreter.process(child);
            processedChildren.push(...elements);
        }

        optionCommand.selectedOptionsChildren(processedChildren);
        optionCommand.setContext(generatorData.context);
        generatorData = optionCommand.execute();
        interpreter.setGeneratorData(generatorData);
        return optionCommand;
    }

    function processAccept(element: any) {
        const selectedValues = element.selectedItems.map((item: QuickPickItemExt) => item.value);
        const children: any[] = [];
        for (const item of element.selectedItems) {
            const option = item;
            if (option.children) {
                children.push(...option.children);
            }
        }

        const optionCommand = processSelected(element, selectedValues, children);
        commandHistory.push(optionCommand);
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
            const selectedValues = quickPick.selectedItems.map(item => (<QuickPickItemExt>item).value);
            const validation = validateQuickPick(selectedValues, element);
            if (validation.length > 0) {
                quickPick.placeholder = validation.map(error => error.message).join(' ');
            } else {
                element.selectedValues = selectedValues;
                const children: any[] = [];
                for (const item of quickPick.selectedItems) {
                    const option = <QuickPickItemExt>item;
                    if (option.children) {
                        children.push(...option.children);
                    }
                }

                if (element.additionalInstructions) {
                    await element.additionalInstructions();
                }

                const optionCommand = processSelected(element, selectedValues, children);
                commandHistory.push(optionCommand);

                resolve(quickPick.selectedItems);
            }
        });
        quickPick.onDidChangeSelection(items => {
            if (element.kind !== 'list') {
                const newElements = quickPick.selectedItems.filter(item => !lastSelectedItems.includes(item));
                if (newElements.length !== 0) {
                    quickPick.selectedItems = quickPick.selectedItems.filter(item => !lastSelectedItems.includes(item));
                }
                if (items.length === 0) {
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
                processUndoAction();
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
            const element = generatorData.elements[generatorData.currentElementIndex];
            generatorData.context.pushScope(element._scope);
            if (element._skip === true) {
                processAccept(element);
                continue;
            }
            currentInput = getInput(element);
            // eslint-disable-next-line eqeqeq
            if (currentInput != null) {
                await currentInput;
            }
            currentInput = undefined;
            if (maxIterationCount-- < 0) {
                break;
            }
        }
        const projectData = GeneratorDataAPI.convertProjectDataElements(generatorData);
        if (projectData.size !== 0) {
            await generateProject(projectData);
        }
    } catch (e: any) {
        VSCodeAPI.showErrorMessage(e.message);
        logger.error(e.stack);
        channel.appendLine(e.stack ?? e.message);
    }

}
