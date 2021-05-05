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

import * as path from 'path';
import { Uri, QuickPickItem } from 'vscode';
import { validateUserInput } from "./common";
import { InputBoxData, VSCodeAPI } from "./VSCodeAPI";
import { FileSystemAPI } from "./FileSystemAPI";
import { ChildProcessAPI } from "./ChildProcessAPI";

export interface ProjectData extends QuickPickItem {
    archetype: string;
    flavor: string;
    pkg: string;
}

export async function showHelidonGenerator() {

    const NUMBER_OF_STEPS = 4;

    const DEFAULT_ARCHETYPE_VERSION = "2.0.1";

    const PROJECT_READY: string = 'Your new project is ready.';
    const NEW_WINDOW: string = 'Open in new window';
    const CURRENT_WINDOW: string = 'Open in current window';
    const ADD_TO_WORKSPACE: string = 'Add to current workspace';

    const SELECT_FOLDER = 'Select folder';
    const OVERWRITE_EXISTING = 'Overwrite';
    const NEW_DIR = 'Choose new directory';
    const EXISTING_FOLDER = ' already exists in selected directory.';

    interface GeneratedProjectData {
        projectData: ProjectData;
        groupId: string;
        artifactId: string;
        pkg: string;
        archetypeVersion: string;
    }

    const quickPickItems: ProjectData[] = [
        {label: "Helidon MP Bare", archetype: "bare", flavor: "mp", pkg: "io.helidon.examples.bare.mp"},
        {label: "Helidon MP Database", archetype: "database", flavor: "mp",  pkg: "io.helidon.examples.database.mp"},
        {label: "Helidon MP Quickstart", archetype: "quickstart", flavor: "mp", pkg: "io.helidon.examples.quickstart.mp"},
        {label: "Helidon SE Bare", archetype: "bare", flavor: "se", pkg: "io.helidon.examples.bare.se"},
        {label: "Helidon SE Database", archetype: "database", flavor: "se", pkg: "io.helidon.examples.database.se"},
        {label: "Helidon SE Quickstart", archetype: "quickstart", flavor: "se", pkg: "io.helidon.examples.quickstart.se"}
    ];

    async function showInputBox(data: InputBoxData) {
        return VSCodeAPI.showInputBox(data);
    }

    async function obtainTypeOfProject(state: Partial<GeneratedProjectData>) {
        state.projectData = (<ProjectData>await VSCodeAPI.showPickOption({
            title: "Choose project you want to generate.",
            totalSteps: NUMBER_OF_STEPS,
            currentStep: 1,
            placeholder: "Project type",
            items: quickPickItems
        }));
        await obtainGroupId(state);
    }

    async function obtainGroupId(state: Partial<GeneratedProjectData>) {
        state.groupId = await VSCodeAPI.showInputBox({
            title: "Select your project groupId",
            placeholder: "Project groupId",
            value: "io.helidon.examples",
            prompt: "Type in your project groupId",
            totalSteps: NUMBER_OF_STEPS,
            currentStep: 2,
            messageValidation: groupIdValidator
        });
        return await obtainArtifactId(state);
    }

    function groupIdValidator(value: string): string | undefined {
        const exp = new RegExp("^[a-z\.]*$");
        const errorMessage = "This groupId is not valid. Use only words separated by dots.";
        return validateUserInput(value, exp, errorMessage);
    }

    async function obtainArtifactId(state: Partial<GeneratedProjectData>) {
        state.artifactId = await showInputBox({
            title: "Select your project artifactId",
            placeholder: "Project artifactId",
            value: state.projectData ? `${state.projectData.archetype}-${state.projectData.flavor}` : "my-project",
            prompt: "Type in your project artifactId",
            totalSteps: NUMBER_OF_STEPS,
            currentStep: 3,
            messageValidation: artifactIdValidator
        });
        return await obtainPackage(state);
    }

    function artifactIdValidator(value: string): string | undefined {
        const exp = new RegExp("^[a-z\-]*$");
        const errorMessage = "This artifactId is not valid. Use only words separated by -";
        return validateUserInput(value, exp, errorMessage);
    }

    async function obtainPackage(state: Partial<GeneratedProjectData>) {
        const pkg = state.projectData ? state.projectData.pkg : "io.helidon.examples.quickstart";
        state.pkg = await showInputBox({
            title: "Select your project package structure",
            placeholder: pkg,
            value: pkg,
            prompt: "Type in your project package structure",
            totalSteps: NUMBER_OF_STEPS,
            currentStep: 4,
            messageValidation: packageValidator
        });
        return await generateProject(state as GeneratedProjectData);
    }

    function packageValidator(value: string): string | undefined {
        const exp = new RegExp("^[a-zA-Z\.]*$");
        const errorMessage = "This package structure is not valid. Use only words separated by dots.";
        return validateUserInput(value, exp, errorMessage);
    }

    async function generateProject(projectData: GeneratedProjectData) {
        const targetDir = await obtainTargetFolder(projectData.artifactId);
        if (!targetDir) {
            throw new Error('Helidon project generation has been canceled.');
        }

        VSCodeAPI.showInformationMessage('Your Helidon project is being created...');

        const cmd = `helidon init \
            --batch \
            --plain \
            --flavor ${projectData.projectData.flavor} \
            --build MAVEN \
            --version ${projectData.archetypeVersion} \
            --archetype ${projectData.projectData.archetype} \
            --groupid ${projectData.groupId} \
            --artifactid ${projectData.artifactId}  \
            --package ${projectData.pkg}`;

        const channel = VSCodeAPI.createOutputChannel('helidon');
        channel.appendLine(cmd);

        const opts = {
            cwd: targetDir.fsPath // cwd means -> current working directory (where this maven command will by executed)
        };
        ChildProcessAPI.execProcess(cmd, opts, (error: string, stdout: string, stderr: string) => {
            channel.appendLine(stdout);
            if (stdout.includes("BUILD SUCCESS")) {
                VSCodeAPI.showInformationMessage('Project generated...');
                openPreparedProject(targetDir, projectData.artifactId);
            } else if (stdout.includes("BUILD FAILURE")) {
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

    async function openPreparedProject(targetDir: Uri, artifactId: string): Promise<void> {

        const openFolderCommand = 'vscode.openFolder';
        const newProjectFolderUri = getNewProjectFolder(targetDir, artifactId);

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
                VSCodeAPI.executeCommand(openFolderCommand, newProjectFolderUri, true);
            }
        } else if (VSCodeAPI.getVisibleTextEditors().length > 0) {
            // If VS does not have any project opened, but has some file opened in it.
            const input: string | undefined = await VSCodeAPI.showInformationMessage(PROJECT_READY, NEW_WINDOW, CURRENT_WINDOW);
            if (input) {
                VSCodeAPI.executeCommand(openFolderCommand, newProjectFolderUri, NEW_WINDOW === input);
            }
        } else {
            VSCodeAPI.executeCommand(openFolderCommand, newProjectFolderUri, false);
        }

    }

    function getNewProjectFolder(targetDir: Uri, artifactId: string): Uri {
        return Uri.file(path.join(targetDir.fsPath, artifactId));
    }

    try {
        await obtainTypeOfProject({
            archetypeVersion: DEFAULT_ARCHETYPE_VERSION
        });
    } catch (e) {
        // window.showErrorMessage(e);
        VSCodeAPI.showErrorMessage(e);
    }

}
