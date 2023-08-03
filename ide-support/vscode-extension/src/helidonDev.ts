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

import { OutputChannel, QuickPickItem } from "vscode";
import * as path from 'path';
import { ChildProcess } from "child_process";
import { VSCodeAPI } from "./VSCodeAPI";
import { FileSystemAPI } from "./FileSystemAPI";
import { ChildProcessAPI } from "./ChildProcessAPI";
import { OutputFormatter } from "./OutputFormatter";
import * as vscode from "vscode";
import { logger } from "./logger";
import { HELIDON_OUTPUT_CHANNEL } from "./common";

const POM_XML_FILE: string = 'pom.xml';
const SRC_DIR: string = 'src';
const EXCLUDE_DIRS: RegExp[] = [/target/i, /^\./i];
const launchedServers: Map<string, HelidonServerInstance> = new Map();

const logChannel = VSCodeAPI.outputChannel(HELIDON_OUTPUT_CHANNEL);

export interface HelidonServerInstance {
    serverProcess: ChildProcess;
    outputChannel: OutputChannel;
    projectFolder: string;
    isActive: boolean;
}

export function addLaunchedServers(servers: Map<string, HelidonServerInstance>) {
    for (const [key, value] of servers) {
        launchedServers.set(key, value);
    }
}

export function getLaunchedServers(): Map<string, HelidonServerInstance> {
    return launchedServers;
}

export async function startHelidonDev(extensionPath: string): Promise<Map<string, HelidonServerInstance>> {
    try {
        const helidonProjectDirs = getHelidonProjectDirs();
        let helidonProjectDir: string;

        if (helidonProjectDirs.length === 0) {
            VSCodeAPI.showInformationMessage("Helidon projects have not been found in the workspace folders.");
            return new Map();
        } else if (helidonProjectDirs.length === 1) {
            helidonProjectDir = helidonProjectDirs[0];
        } else {
            const directory = await obtainHelidonProjectDirToStart(helidonProjectDirs);

            if (!directory) {
                return new Map();
            }
            helidonProjectDir = directory.description!;
        }

        const helidonServer = obtainHelidonServerInstance(helidonProjectDir, extensionPath);
        launchedServers.set(path.basename(helidonProjectDir), helidonServer);
        return launchedServers;

    } catch (e: any) {
        VSCodeAPI.showErrorMessage(e.message);
        logger.error(e.stack);
        logChannel.appendLine(e.stack ?? e.message);
        return new Map();
    }
}

async function obtainHelidonProjectDirToStart(helidonProjectDirs: string[]): Promise<QuickPickItem | undefined> {

    const helidonProjectDirItems: QuickPickItem[] = [];
    helidonProjectDirs.forEach((value: string) => {

        helidonProjectDirItems.push({
            label: path.basename(value),
            description: value
        });
    });

    return await VSCodeAPI.showPickOption({
        title: "Choose a Helidon project that you want to start.",
        totalSteps: 1,
        currentStep: 1,
        placeholder: "Project name",
        items: helidonProjectDirItems
    });
}

function obtainHelidonServerInstance(helidonProjectDir: string, extensionPath: string): HelidonServerInstance {

    const helidonDirName = path.basename(helidonProjectDir);
    refreshLaunchedServers();

    if (launchedServers.has(helidonDirName)) {
        const helidonServer = launchedServers.get(helidonDirName)!;
        if (helidonServer.isActive) {
            helidonServer.outputChannel.show();
            VSCodeAPI.showInformationMessage(`Dev Loop for the project in the folder '${helidonDirName}' is already running`);
            return helidonServer;
        }
        // change existing instance
        helidonServer.serverProcess = obtainNewServerProcess(helidonProjectDir, extensionPath);
        helidonServer.outputChannel.show();
        configureServerOutput(helidonServer.serverProcess, helidonServer.outputChannel);
        helidonServer.isActive = true;

        return helidonServer;
    }

    // create new instance
    const outputChannel = VSCodeAPI.outputChannel(helidonDirName);
    outputChannel.show();
    const serverProcess = obtainNewServerProcess(helidonProjectDir, extensionPath);
    configureServerOutput(serverProcess, outputChannel);

    return {
        serverProcess: serverProcess,
        outputChannel: outputChannel,
        projectFolder: helidonProjectDir,
        isActive: true
    };
}

function refreshLaunchedServers() {
    const helidonProjectDirs = getHelidonProjectDirs();
    launchedServers.forEach((server, name) => {
        if (!helidonProjectDirs.includes(server.projectFolder)) {
            launchedServers.delete(name);
        }
    });
}

function preparePathForRegExp(path: string): string {
    return path.replace(/\\/g, "\\\\");
}

function configEnvPath(configPath: string, binPath: string, pathDelimiter: string) {
    // eslint-disable-next-line eqeqeq
    if (process.env.PATH != null) {
        if (!process.env.PATH.includes(configPath)) {
            process.env.PATH = `${binPath}${process.env.PATH}`;
        } else {
            process.env.PATH = process.env.PATH.replace(
                new RegExp(preparePathForRegExp(configPath) + `.?bin.?${pathDelimiter}?`),
                binPath
            );
        }
    }
}

function obtainNewServerProcess(helidonProjectDir: string, extensionPath: string): ChildProcess {
    const cmdSpan = "java";
    const args = ['-jar', `${extensionPath}/target/cli/helidon-cli.jar`, 'dev'];

    const pathDelimiter = path.delimiter;

    const helidonConfig = vscode.workspace.getConfiguration('helidon');

    const configJavaHome: string = helidonConfig.get("javaHomeDir")!;
    const javaHomeBinDir: string = configJavaHome ? `${configJavaHome}/bin${pathDelimiter}` : "";
    const configMavenHome: string = helidonConfig.get("mavenHomeDir")!;
    const mavenBinDir: string = configMavenHome ? `${configMavenHome}/bin${pathDelimiter}` : "";

    // eslint-disable-next-line eqeqeq
    if (process.env.PATH != null) {
        if (mavenBinDir !== "") {
            process.env.M2_HOME = configMavenHome;
            process.env.MAVEN_HOME = configMavenHome;
            configEnvPath(configMavenHome, mavenBinDir, pathDelimiter);
        }

        if (javaHomeBinDir !== "") {
            process.env.JAVA_HOME = configJavaHome;
            configEnvPath(configJavaHome, javaHomeBinDir, pathDelimiter);
        }
    } else {
        process.env.PATH = `${javaHomeBinDir}${mavenBinDir}`;
    }

    const opts = {
        cwd: helidonProjectDir, // cwd means -> current working directory (where this command will by executed)
    };
    const serverProcess = ChildProcessAPI.spawnProcess(cmdSpan, args, opts);

    return serverProcess;
}

function configureServerOutput(serverProcess: ChildProcess, outputChannel: OutputChannel) {

    const outputFormatter = new OutputFormatter(outputChannel);

    serverProcess!.stdout!.on('data', (data: string) => {
        outputFormatter.formatInputString(data);
    });

    serverProcess!.stderr!.on('data', (data: string) => {
        outputFormatter.formatInputString(data);
        console.error(data.toString());
        VSCodeAPI.showErrorMessage(data);
    });

    serverProcess.on('close', (code: string) => {
        outputChannel.appendLine("Server stopped");
        stopHelidonDev(true);
    });
}

export async function stopHelidonDev(flag?: boolean) {
    try {
        let currentHelidonServer: HelidonServerInstance;
        const activeServerNames = getActiveServerNames();
        if (activeServerNames.length === 0) {
            if (flag === undefined){
                VSCodeAPI.showInformationMessage(`Dev Loop is not started`);
            }
            return;
        }
        if (activeServerNames.length === 1) {
            currentHelidonServer = launchedServers.get(activeServerNames[0])!;
            deactivateServer(currentHelidonServer);
            return;
        }

        const stopServerName = await obtainStopServerName();

        if (stopServerName) {
            currentHelidonServer = launchedServers.get(stopServerName)!;
            currentHelidonServer.outputChannel.show();
            deactivateServer(currentHelidonServer);
        } else {
            VSCodeAPI.showInformationMessage(`Dev Loop for the project in the folder '${stopServerName}' is not started`);
        }
    } catch (e: any) {
        VSCodeAPI.showErrorMessage(e.message);
        logger.error(e.stack);
        logChannel.appendLine(e.stack ?? e.message);
        return;
    }
}

async function obtainStopServerName(): Promise<string | undefined> {

    const runningProjectNames: QuickPickItem[] = [];
    getActiveServerNames().forEach(name => runningProjectNames.push({label: name}));

    const stopServer = await VSCodeAPI.showPickOption({
        title: "Choose a server that you want to stop.",
        totalSteps: 1,
        currentStep: 1,
        placeholder: "Project name",
        items: runningProjectNames
    });

    return stopServer?.label;
}

function getActiveServerNames(): string[] {
    const runningProjectNames: string[] = [];
    launchedServers.forEach((value: HelidonServerInstance, key: string) => {
        if (value.isActive) {
            runningProjectNames.push(key);
        }
    });
    return runningProjectNames;
}

function deactivateServer(currentHelidonServer: HelidonServerInstance) {
    if (currentHelidonServer.isActive) {
        killProcess(currentHelidonServer.serverProcess);
        currentHelidonServer.isActive = false;
    }
}

function killProcess(process: ChildProcess) {
    ChildProcessAPI.killProcess(process.pid);
}

/**
 * Find folders that contain the specific file and src folder (root folder of the project)
 * @param searchFileName Name of the file for search
 * @param inputDirPaths Directories for search
 */
function getDirsByFileName(inputDirPaths: string[], searchFileName: string): string[] {
    const dirPaths: string[] = [];
    recursiveSearch(inputDirPaths, searchFileName);
    return dirPaths;

    function recursiveSearch(inputDirs: string[], searchFile: string) {
        for (const inputDir of inputDirs) {
            const searchFilePath = path.join(inputDir, searchFile);
            const srcDirPath = path.join(inputDir, SRC_DIR);
            if (FileSystemAPI.isPathExistsSync(searchFilePath) && FileSystemAPI.isPathExistsSync(srcDirPath)) {
                dirPaths.push(inputDir);
            }
            FileSystemAPI.readDirSync(inputDir).forEach((file: string) => {
                const filePath = path.join(inputDir, file);
                if (FileSystemAPI.isDirectorySync(filePath)) {
                    if (!isDirMatchesPattern(filePath, EXCLUDE_DIRS)) {
                        recursiveSearch([filePath], searchFile);
                    }
                }
            });
        }
        return dirPaths;
    }
}

function isDirMatchesPattern(dirName: string, patterns: RegExp[]): boolean {
    for (const pattern of patterns) {
        if (pattern.test(path.basename(dirName))) {
            return true;
        }
    }
    return false;
}

function getHelidonProjectDirs(): string[] {
    const helidonProjectDirs: string[] = [];
    const rootDirPaths = getRootDirPaths();
    const MavenProjectDirs = getDirsByFileName(rootDirPaths, POM_XML_FILE);
    for (const mavenProject of MavenProjectDirs) {
        const mavenProjectPomPath = path.join(mavenProject, POM_XML_FILE);
        const isHelidonProject = isPomFileContainsHelidonDependency(mavenProjectPomPath);
        if (isHelidonProject) {
            if (!helidonProjectDirs.includes(mavenProject)) {
                helidonProjectDirs.push(mavenProject);
            }
        }
    }
    return helidonProjectDirs;
}

function isPomFileContainsHelidonDependency(pomFilePath: string): boolean {
    const regex = /.*<dependency>[^<>]*<groupId>[^<>]*helidon[^<>]*<\/groupId>.*/isg;
    const pomContent = FileSystemAPI.readTextFileSync(pomFilePath, 'utf8');
    if (pomContent) {
        return regex.test(pomContent);
    }
    return false;
}

/**
 * Find full paths of the root directories for the current workspace
 */
function getRootDirPaths(): string[] {
    const dirs = VSCodeAPI.getWorkspaceFolders();
    if (!dirs) {
        return [];
    }
    const dirPaths: string[] = [];
    for (const dir of dirs) {
        dirPaths.push(dir.uri.fsPath);
    }
    return dirPaths;
}
