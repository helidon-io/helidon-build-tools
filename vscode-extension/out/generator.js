"use strict";
/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) {
        return value instanceof P ? value : new P(function (resolve) {
            resolve(value);
        });
    }

    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) {
            try {
                step(generator.next(value));
            } catch (e) {
                reject(e);
            }
        }

        function rejected(value) {
            try {
                step(generator["throw"](value));
            } catch (e) {
                reject(e);
            }
        }

        function step(result) {
            result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected);
        }

        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", {value: true});
exports.showHelidonGenerator = void 0;
const path = require("path");
const vscode_1 = require("vscode");
const common_1 = require("./common");
const VSCodeAPI_1 = require("./VSCodeAPI");
const FileSystemAPI_1 = require("./FileSystemAPI");
const ChildProcessAPI_1 = require("./ChildProcessAPI");

function showHelidonGenerator() {
    return __awaiter(this, void 0, void 0, function* () {
        const NUMBER_OF_STEPS = 4;
        const DEFAULT_ARCHETYPE_VERSION = "2.0.1";
        const PROJECT_READY = 'Your new project is ready.';
        const NEW_WINDOW = 'Open in new window';
        const CURRENT_WINDOW = 'Open in current window';
        const ADD_TO_WORKSPACE = 'Add to current workspace';
        const SELECT_FOLDER = 'Select folder';
        const OVERWRITE_EXISTING = 'Overwrite';
        const NEW_DIR = 'Choose new directory';
        const EXISTING_FOLDER = ' already exists in selected directory.';
        const quickPickItems = [
            {label: "Helidon MP Bare", projectName: "helidon-bare-mp", packages: "io.helidon.examples.bare.mp"},
            {
                label: "Helidon MP Database",
                projectName: "helidon-database-mp",
                packages: "io.helidon.examples.database.mp"
            },
            {
                label: "Helidon MP Quickstart",
                projectName: "helidon-quickstart-mp",
                packages: "io.helidon.examples.quickstart.mp"
            },
            {label: "Helidon SE Bare", projectName: "helidon-bare-se", packages: "io.helidon.examples.bare.se"},
            {
                label: "Helidon SE Database",
                projectName: "helidon-database-se",
                packages: "io.helidon.examples.database.se"
            },
            {
                label: "Helidon SE Quickstart",
                projectName: "helidon-quickstart-se",
                packages: "io.helidon.examples.quickstart.se"
            }
        ];

        function showInputBox(data) {
            return __awaiter(this, void 0, void 0, function* () {
                return VSCodeAPI_1.VSCodeAPI.showInputBox(data);
            });
        }

        function obtainTypeOfProject(projectState) {
            return __awaiter(this, void 0, void 0, function* () {
                projectState.projectData = (yield VSCodeAPI_1.VSCodeAPI.showPickOption({
                    title: "Choose project you want to generate.",
                    totalSteps: NUMBER_OF_STEPS,
                    currentStep: 1,
                    placeholder: "Project type",
                    items: quickPickItems
                }));
                yield obtainGroupId(projectState);
            });
        }

        function obtainGroupId(projectState) {
            return __awaiter(this, void 0, void 0, function* () {
                projectState.groupId = yield VSCodeAPI_1.VSCodeAPI.showInputBox({
                    title: "Select your project groupId",
                    placeholder: "Project groupId",
                    value: "io.helidon.examples",
                    prompt: "Type in your project groupId",
                    totalSteps: NUMBER_OF_STEPS,
                    currentStep: 2,
                    messageValidation: groupIdValidator
                });
                return yield obtainArtefactId(projectState);
            });
        }

        function groupIdValidator(value) {
            const exp = new RegExp("^[a-z\.]*$");
            const errorMessage = "This groupId is not valid. Use only words separated by dots.";
            return common_1.validateUserInput(value, exp, errorMessage);
        }

        function obtainArtefactId(projectState) {
            return __awaiter(this, void 0, void 0, function* () {
                projectState.artifactId = yield showInputBox({
                    title: "Select your project artefactId",
                    placeholder: "Project artefactId",
                    value: projectState.projectData ? projectState.projectData.projectName : "helidon-project",
                    prompt: "Type in your project artefactId",
                    totalSteps: NUMBER_OF_STEPS,
                    currentStep: 3,
                    messageValidation: artefactIdValidator
                });
                return yield obtainPackages(projectState);
            });
        }

        function artefactIdValidator(value) {
            const exp = new RegExp("^[a-z\-]*$");
            const errorMessage = "This artefactId is not valid. Use only words separated by -";
            return common_1.validateUserInput(value, exp, errorMessage);
        }

        function obtainPackages(projectState) {
            return __awaiter(this, void 0, void 0, function* () {
                let packages = projectState.projectData ? projectState.projectData.packages : "io.helidon.examples.quickstart";
                projectState.packages = yield showInputBox({
                    title: "Select your project package structure",
                    placeholder: packages,
                    value: packages,
                    prompt: "Type in your project package structure",
                    totalSteps: NUMBER_OF_STEPS,
                    currentStep: 4,
                    messageValidation: packagesValidator
                });
                return yield generateProject(projectState);
            });
        }

        function packagesValidator(value) {
            const exp = new RegExp("^[a-zA-Z\.]*$");
            const errorMessage = "This package structure is not valid. Use only words separated by dots.";
            return common_1.validateUserInput(value, exp, errorMessage);
        }

        function generateProject(projectData) {
            return __awaiter(this, void 0, void 0, function* () {
                const targetDir = yield obtainTargetFolder(projectData.artifactId);
                if (!targetDir) {
                    throw new Error('Helidon project generation has been canceled.');
                }
                VSCodeAPI_1.VSCodeAPI.showInformationMessage('Your Helidon project is being created...');
                let cmd = "mvn archetype:generate -DinteractiveMode=false \
            -DarchetypeGroupId=io.helidon.archetypes \
            -DarchetypeArtifactId=" + projectData.projectData.projectName + " \
            -DarchetypeVersion=" + projectData.archetypeVersion + " \
            -DgroupId=" + projectData.groupId + " \
            -DartifactId=" + projectData.artifactId + " \
            -Dpackage=" + projectData.packages;
                let opts = {
                    cwd: targetDir.fsPath //cwd means -> current working directory (where this maven command will by executed)
                };
                ChildProcessAPI_1.ChildProcessAPI.execProcess(cmd, opts, function (error, stdout, stderr) {
                    console.log(stdout);
                    if (stdout.includes("BUILD SUCCESS")) {
                        VSCodeAPI_1.VSCodeAPI.showInformationMessage('Project generated...');
                        openPreparedProject(targetDir, projectData.artifactId);
                    } else if (stdout.includes("BUILD FAILURE")) {
                        VSCodeAPI_1.VSCodeAPI.showInformationMessage('Project generation failed...');
                    }
                    if (stderr) {
                        console.log(stderr);
                    }
                    if (error) {
                        console.log(error);
                    }
                });
            });
        }

        function obtainTargetFolder(projectName) {
            return __awaiter(this, void 0, void 0, function* () {
                const specificFolderMessage = `'${projectName}'` + EXISTING_FOLDER;
                let directory = yield VSCodeAPI_1.VSCodeAPI.showOpenFolderDialog({openLabel: SELECT_FOLDER});
                while (directory && FileSystemAPI_1.FileSystemAPI.isPathExistsSync(path.join(directory.fsPath, projectName))) {
                    const choice = yield VSCodeAPI_1.VSCodeAPI.showWarningMessage(specificFolderMessage, OVERWRITE_EXISTING, NEW_DIR);
                    if (choice === OVERWRITE_EXISTING) {
                        //Following line deletes target folder recursively
                        require("rimraf").sync(path.join(directory.fsPath, projectName));
                        break;
                    } else if (choice === NEW_DIR) {
                        directory = yield VSCodeAPI_1.VSCodeAPI.showOpenFolderDialog({openLabel: SELECT_FOLDER});
                    } else {
                        directory = undefined;
                        break;
                    }
                }
                return directory;
            });
        }

        function openPreparedProject(targetDir, artifactId) {
            return __awaiter(this, void 0, void 0, function* () {
                const openFolderCommand = 'vscode.openFolder';
                const newProjectFolderUri = getNewProjectFolder(targetDir, artifactId);
                if (VSCodeAPI_1.VSCodeAPI.getWorkspaceFolders()) {
                    const input = yield VSCodeAPI_1.VSCodeAPI.showInformationMessage(PROJECT_READY, NEW_WINDOW, ADD_TO_WORKSPACE);
                    if (!input) {
                        return;
                    } else if (input === ADD_TO_WORKSPACE) {
                        VSCodeAPI_1.VSCodeAPI.updateWorkspaceFolders(VSCodeAPI_1.VSCodeAPI.getWorkspaceFolders() ? VSCodeAPI_1.VSCodeAPI.getWorkspaceFolders().length : 0, undefined, {uri: newProjectFolderUri});
                    } else {
                        VSCodeAPI_1.VSCodeAPI.executeCommand(openFolderCommand, newProjectFolderUri, true);
                    }
                } else if (VSCodeAPI_1.VSCodeAPI.getVisibleTextEditors().length > 0) {
                    //If VS does not have any project opened, but has some file opened in it.
                    const input = yield VSCodeAPI_1.VSCodeAPI.showInformationMessage(PROJECT_READY, NEW_WINDOW, CURRENT_WINDOW);
                    if (input) {
                        VSCodeAPI_1.VSCodeAPI.executeCommand(openFolderCommand, newProjectFolderUri, NEW_WINDOW === input);
                    }
                } else {
                    VSCodeAPI_1.VSCodeAPI.executeCommand(openFolderCommand, newProjectFolderUri, false);
                }
            });
        }

        function getNewProjectFolder(targetDir, artifactId) {
            return vscode_1.Uri.file(path.join(targetDir.fsPath, artifactId));
        }

        try {
            yield obtainTypeOfProject({
                archetypeVersion: DEFAULT_ARCHETYPE_VERSION
            });
        } catch (e) {
            // window.showErrorMessage(e);
            VSCodeAPI_1.VSCodeAPI.showErrorMessage(e);
        }
    });
}

exports.showHelidonGenerator = showHelidonGenerator;
//# sourceMappingURL=generator.js.map