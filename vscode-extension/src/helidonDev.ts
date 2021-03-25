/**
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

import {OutputChannel, QuickPickItem} from "vscode";
import * as path from 'path';
import {ChildProcess} from "child_process";
import {VSCodeAPI} from "./VSCodeAPI";
import {FileSystemAPI} from "./FileSystemAPI";
import {ChildProcessAPI} from "./ChildProcessAPI";
import {OutputFormatter} from "./OutputFormatter";

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
    for (let [key, value] of servers) {
        launchedServers.set(key, value);
    }
}

export function getLaunchedServers(): Map<string, HelidonServerInstance> {
    return launchedServers;
}

export async function startHelidonDev(): Promise<Map<string, HelidonServerInstance>> {
    try {
        if (!ChildProcessAPI.isCommandExist('helidon')) {
            VSCodeAPI.showInformationMessage('Helidon CLI is not installed');
            return new Map();
        }
        let helidonProjectDirs = getHelidonProjectDirs();
        let helidonProjectDir: string;

        if (helidonProjectDirs.length == 0) {
            return new Map();
        } else if (helidonProjectDirs.length == 1) {
            helidonProjectDir = helidonProjectDirs[0];
        } else {
            let directory = await obtainHelidonProjectDirToStart(helidonProjectDirs);

            if (!directory) {
                return new Map();
            }
            helidonProjectDir = directory.description!;
        }

        let helidonServer = obtainHelidonServerInstance(helidonProjectDir);
        launchedServers.set(path.basename(helidonProjectDir), helidonServer);
        return launchedServers;

    } catch (e) {
        VSCodeAPI.showErrorMessage(e);
        return new Map();
    }
}

async function obtainHelidonProjectDirToStart(helidonProjectDirs: string[]): Promise<QuickPickItem | undefined> {

    let helidonProjectDirItems: QuickPickItem[] = [];
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

function obtainHelidonServerInstance(helidonProjectDir: string): HelidonServerInstance {

    let helidonDirName = path.basename(helidonProjectDir);
    refreshLaunchedServers();

    if (launchedServers.has(helidonDirName)) {
        let helidonServer = launchedServers.get(helidonDirName)!;
        if (helidonServer.isActive) {
            helidonServer.outputChannel.show();
            return helidonServer;
        }
        //change existing instance
        helidonServer.serverProcess = obtainNewServerProcess(helidonProjectDir);
        helidonServer.outputChannel.show();
        configureServerOutput(helidonServer.serverProcess, helidonServer.outputChannel);
        helidonServer.isActive = true;

        return helidonServer;
    }

    //create new instance
    let outputChannel = VSCodeAPI.createOutputChannel(helidonDirName);
    outputChannel.show();
    let serverProcess = obtainNewServerProcess(helidonProjectDir);
    configureServerOutput(serverProcess, outputChannel);

    return {
        serverProcess: serverProcess,
        outputChannel: outputChannel,
        projectFolder: helidonProjectDir,
        isActive: true
    };
}

function refreshLaunchedServers() {
    let helidonProjectDirs = getHelidonProjectDirs();
    launchedServers.forEach((server, name) => {
        if (!helidonProjectDirs.includes(server.projectFolder)) {
            launchedServers.delete(name);
        }
    });
}

function obtainNewServerProcess(helidonProjectDir: string): ChildProcess {
    let cmdSpan = "helidon";
    let args = ['dev'];
    let opts = {
        cwd: helidonProjectDir //cwd means -> current working directory (where this maven command will by executed)
    };
    let serverProcess = ChildProcessAPI.spawnProcess(cmdSpan, args, opts);
    return serverProcess;
}

function configureServerOutput(serverProcess: ChildProcess, outputChannel: OutputChannel) {

    let outputFormatter = new OutputFormatter(outputChannel);

    serverProcess!.stdout!.on('data', function (data: string) {
        outputFormatter.formatInputString(data);
    });

    serverProcess!.stderr!.on('data', (data: string) => {
        console.error(data);
        VSCodeAPI.showErrorMessage(data)
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
        let activeServerNames = getActiveServerNames();
        if (activeServerNames.length == 0) {
            return;
        }
        if (activeServerNames.length == 1) {
            currentHelidonServer = launchedServers.get(activeServerNames[0])!;
            deactivateServer(currentHelidonServer);
            return;
        }

        let stopServerName = await obtainStopServerName();

        if (stopServerName) {
            currentHelidonServer = launchedServers.get(stopServerName)!;
            deactivateServer(currentHelidonServer);
        }
    } catch (e) {
        VSCodeAPI.showErrorMessage(e)
        return;
    }
}

async function obtainStopServerName(): Promise<string | undefined> {

    let runningProjectNames: QuickPickItem[] = [];
    getActiveServerNames().forEach(name => runningProjectNames.push({label: name}));

    let stopServer = await VSCodeAPI.showPickOption({
        title: "Choose a server that you want to stop.",
        totalSteps: 1,
        currentStep: 1,
        placeholder: "Project name",
        items: runningProjectNames
    });

    return stopServer?.label;
}

function getActiveServerNames(): string[] {
    let runningProjectNames: string[] = [];
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
    let dirPaths: string[] = [];
    recursiveSearch(inputDirPaths, searchFileName);
    return dirPaths;

    function recursiveSearch(inputDirs: string[], searchFile: string) {
        for (let inputDir of inputDirs) {
            let searchFilePath = path.join(inputDir, searchFile);
            let srcDirPath = path.join(inputDir, SRC_DIR);
            if (FileSystemAPI.isPathExistsSync(searchFilePath) && FileSystemAPI.isPathExistsSync(srcDirPath)) {
                dirPaths.push(inputDir);
            }
            FileSystemAPI.readDirSync(inputDir).forEach((file: string) => {
                let filePath = path.join(inputDir, file);
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
    for (let pattern of patterns) {
        if (pattern.test(path.basename(dirName))) {
            return true;
        }
    }
    return false;
}

function getHelidonProjectDirs(): string[] {
    let helidonProjectDirs: string[] = [];
    let rootDirPaths = getRootDirPaths();
    let MavenProjectDirs = getDirsByFileName(rootDirPaths, POM_XML_FILE);
    for (let mavenProject of MavenProjectDirs) {
        let mavenProjectPomPath = path.join(mavenProject, POM_XML_FILE);
        let isHelidonProject = isPomFileContainsHelidonDependency(mavenProjectPomPath);
        if (isHelidonProject) {
            helidonProjectDirs.push(mavenProject);
        }
    }
    return helidonProjectDirs;
}

function isPomFileContainsHelidonDependency(pomFilePath: string): boolean {
    let regex = /.*<dependency>[^<>]*<groupId>[^<>]*helidon[^<>]*<\/groupId>.*/isg
    let pomContent = FileSystemAPI.readTextFileSync(pomFilePath, 'utf8');
    if (pomContent) {
        return regex.test(pomContent);
    }
    return false;
}

/**
 * Find full paths of the root directories for the current workspace
 */
function getRootDirPaths(): string[] {
    let dirs = VSCodeAPI.getWorkspaceFolders();
    if (!dirs) {
        return [];
    }
    let dirPaths: string[] = [];
    for (let dir of dirs) {
        dirPaths.push(dir.uri.fsPath);
    }
    return dirPaths;
}
