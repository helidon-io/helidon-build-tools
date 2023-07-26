/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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
import { VSCodeAPI } from "./VSCodeAPI";
import { FileSystemAPI } from "./FileSystemAPI";

const mpMainClass = "io.helidon.microprofile.cdi.Main";
const typeConfig = "java";
const configRequest = "launch";
const pomFile = "pom.xml";

export function processLaunchConfig(context: vscode.ExtensionContext) {
    const projectsDir = VSCodeAPI.getWorkspaceFolders();
    let workspaceConfigChanged = false;
    let workspaceContainsMpProjects = false;

    if (projectsDir) {
        for (const projectDir of projectsDir) {
            const projectType = getProjectType(projectDir.uri.fsPath);
            if (projectType === ProjectType.MP) {
                const workspaceConfiguration = vscode.workspace.getConfiguration('launch', projectDir.uri);
                const configurations = workspaceConfiguration.configurations;
                let configContainsMpConfig = false;
                for (const config of configurations) {
                    if (
                        /* eslint-disable eqeqeq */
                        (config.type != null && config.type === typeConfig) &&
                        (config.request != null && config.request === configRequest) &&
                        (config.mainClass != null && config.mainClass === mpMainClass) &&
                        (config.cwd != null && config.cwd === projectDir.uri.fsPath)
                        /* eslint-enable eqeqeq */
                    ) {
                        configContainsMpConfig = true;
                        workspaceContainsMpProjects = true;
                        break;
                    }
                }
                if (!configContainsMpConfig) {
                    const defaultLaunchConfig = {
                        type: typeConfig,
                        name: projectDir.name + " - " + "Launch Helidon MP Project",
                        request: configRequest,
                        mainClass: mpMainClass,
                        projectName: projectDir.name,
                        cwd: projectDir.uri.fsPath
                    };
                    configurations.unshift(defaultLaunchConfig);
                    workspaceConfiguration.update('configurations', [...configurations]);
                    workspaceConfigChanged = true;
                }
            }
        }
        if (workspaceConfigChanged) {
            VSCodeAPI.showInformationMessage('New configuration to RUN/DEBUG the Helidon MP project was added. ' +
                'If it does not start automatically select it manually.');
        }
        if (workspaceContainsMpProjects) {
            VSCodeAPI.showInformationMessage('The project contains a configuration to RUN/DEBUG the Helidon MP project. ' +
                'If it does not start automatically select it manually.');
        }
    }
}

function getProjectType(projectPath: string): ProjectType {

    const pomFilePath = FileSystemAPI.resolvePath([projectPath, pomFile]);
    if (!FileSystemAPI.isPathExistsSync(pomFilePath)) {
        return ProjectType.OTHER;
    }
    const pomContent = FileSystemAPI.readTextFileSync(pomFilePath, 'utf8');
    if (pomContent) {
        const mpRegex = /.*<dependency>[^<>]*<groupId>[^<>]*io.helidon.microprofile[^<>]*<\/groupId>.*/isg;
        if (mpRegex.test(pomContent)) {
            return ProjectType.MP;
        }
    }
    return ProjectType.OTHER;
}

enum ProjectType {
    // SE = "se",
    MP = "mp",
    // NIMA = "nima",
    OTHER = "other"
}
