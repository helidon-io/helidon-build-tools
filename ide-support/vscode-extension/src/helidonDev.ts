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

import { OutputChannel, QuickPickItem } from "vscode";
import * as path from 'path';
import { ChildProcess } from "child_process";
import { VSCodeAPI } from "./VSCodeAPI";
import { FileSystemAPI } from "./FileSystemAPI";
import { ChildProcessAPI } from "./ChildProcessAPI";
import { OutputFormatter } from "./OutputFormatter";

const POM_XML_FILE: string = 'pom.xml';
const SRC_DIR: string = 'src';
const EXCLUDE_DIRS: RegExp[] = [/target/i, /^\./i];
const launchedServers: Map<string, HelidonServerInstance> = new Map();

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
    const outputChannel = VSCodeAPI.createOutputChannel(helidonDirName);
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

function obtainNewServerProcess(helidonProjectDir: string, extensionPath: string): ChildProcess {
    let cmdSpan = "java";
    const args = ['-jar', `${extensionPath}/target/cli/helidon.jar`, 'dev'];

    // let cmdSpan = "mvn";
    // const args = ['-v'];
    process.env.M2_HOME="/home/aserkes/.sdkman/candidates/maven/current";
    process.env.MAVEN_HOME="/home/aserkes/.sdkman/candidates/maven/current";
    process.env.JAVA_HOME='/home/aserkes/.sdkman/candidates/java/current';
    process.env.PATH=`${process.env.JAVA_HOME}/bin:${process.env.MAVEN_HOME}/bin:${process.env.PATH}`;
    // process.env.PATH=`/home/aserkes/.sdkman/candidates/java/current/bin:${process.env.MAVEN_HOME}/bin`;
console.log("SHELL - "+process.env.M2_HOME);
console.log("PATH - "+process.env.PATH);
    


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
        console.error(data);
        VSCodeAPI.showErrorMessage(data);
    });

    serverProcess.on('close', (code: string) => {
        outputChannel.appendLine("Server stopped");
    });
}

export async function stopHelidonDev() {
    try {
        if (!ChildProcessAPI.isCommandExist('helidon')) {
            return;
        }
        let currentHelidonServer: HelidonServerInstance;
        const activeServerNames = getActiveServerNames();
        if (activeServerNames.length === 0) {
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
            deactivateServer(currentHelidonServer);
        }
    } catch (e: any) {
        VSCodeAPI.showErrorMessage(e.message);
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
