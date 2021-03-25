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
exports.stopHelidonDev = exports.startHelidonDev = exports.getLaunchedServers = exports.addLaunchedServers = void 0;
const path = require("path");
const VSCodeAPI_1 = require("./VSCodeAPI");
const FileSystemAPI_1 = require("./FileSystemAPI");
const ChildProcessAPI_1 = require("./ChildProcessAPI");
const OutputFormatter_1 = require("./OutputFormatter");
const POM_XML_FILE = 'pom.xml';
const SRC_DIR = 'src';
const EXCLUDE_DIRS = [/target/i, /^\./i];
const launchedServers = new Map();

function addLaunchedServers(servers) {
    for (let [key, value] of servers) {
        launchedServers.set(key, value);
    }
}

exports.addLaunchedServers = addLaunchedServers;

function getLaunchedServers() {
    return launchedServers;
}

exports.getLaunchedServers = getLaunchedServers;

function startHelidonDev() {
    return __awaiter(this, void 0, void 0, function* () {
        try {
            if (!ChildProcessAPI_1.ChildProcessAPI.isCommandExist('helidon')) {
                VSCodeAPI_1.VSCodeAPI.showInformationMessage('Helidon CLI is not installed');
                return new Map();
            }
            let helidonProjectDirs = getHelidonProjectDirs();
            let helidonProjectDir;
            if (helidonProjectDirs.length == 0) {
                return new Map();
            } else if (helidonProjectDirs.length == 1) {
                helidonProjectDir = helidonProjectDirs[0];
            } else {
                let directory = yield obtainHelidonProjectDirToStart(helidonProjectDirs);
                if (!directory) {
                    return new Map();
                }
                helidonProjectDir = directory.description;
            }
            let helidonServer = obtainHelidonServerInstance(helidonProjectDir);
            launchedServers.set(path.basename(helidonProjectDir), helidonServer);
            return launchedServers;
        } catch (e) {
            VSCodeAPI_1.VSCodeAPI.showErrorMessage(e);
            return new Map();
        }
    });
}

exports.startHelidonDev = startHelidonDev;

function obtainHelidonProjectDirToStart(helidonProjectDirs) {
    return __awaiter(this, void 0, void 0, function* () {
        let helidonProjectDirItems = [];
        helidonProjectDirs.forEach((value) => {
            helidonProjectDirItems.push({
                label: path.basename(value),
                description: value
            });
        });
        return yield VSCodeAPI_1.VSCodeAPI.showPickOption({
            title: "Choose a Helidon project that you want to start.",
            totalSteps: 1,
            currentStep: 1,
            placeholder: "Project name",
            items: helidonProjectDirItems
        });
    });
}

function obtainHelidonServerInstance(helidonProjectDir) {
    let helidonDirName = path.basename(helidonProjectDir);
    refreshLaunchedServers();
    if (launchedServers.has(helidonDirName)) {
        let helidonServer = launchedServers.get(helidonDirName);
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
    let outputChannel = VSCodeAPI_1.VSCodeAPI.createOutputChannel(helidonDirName);
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

function obtainNewServerProcess(helidonProjectDir) {
    let cmdSpan = "helidon";
    let args = ['dev'];
    let opts = {
        cwd: helidonProjectDir //cwd means -> current working directory (where this maven command will by executed)
    };
    let serverProcess = ChildProcessAPI_1.ChildProcessAPI.spawnProcess(cmdSpan, args, opts);
    return serverProcess;
}

function configureServerOutput(serverProcess, outputChannel) {
    let outputFormatter = new OutputFormatter_1.OutputFormatter(outputChannel);
    serverProcess.stdout.on('data', function (data) {
        outputFormatter.formatInputString(data);
    });
    serverProcess.stderr.on('data', (data) => {
        console.error(data);
        VSCodeAPI_1.VSCodeAPI.showErrorMessage(data);
    });
    serverProcess.on('close', (code) => {
        outputChannel.appendLine("Server stopped");
    });
}

function stopHelidonDev() {
    return __awaiter(this, void 0, void 0, function* () {
        try {
            if (!ChildProcessAPI_1.ChildProcessAPI.isCommandExist('helidon')) {
                return;
            }
            let currentHelidonServer;
            let activeServerNames = getActiveServerNames();
            if (activeServerNames.length == 0) {
                return;
            }
            if (activeServerNames.length == 1) {
                currentHelidonServer = launchedServers.get(activeServerNames[0]);
                deactivateServer(currentHelidonServer);
                return;
            }
            let stopServerName = yield obtainStopServerName();
            if (stopServerName) {
                currentHelidonServer = launchedServers.get(stopServerName);
                deactivateServer(currentHelidonServer);
            }
        } catch (e) {
            VSCodeAPI_1.VSCodeAPI.showErrorMessage(e);
            return;
        }
    });
}

exports.stopHelidonDev = stopHelidonDev;

function obtainStopServerName() {
    return __awaiter(this, void 0, void 0, function* () {
        let runningProjectNames = [];
        getActiveServerNames().forEach(name => runningProjectNames.push({label: name}));
        let stopServer = yield VSCodeAPI_1.VSCodeAPI.showPickOption({
            title: "Choose a server that you want to stop.",
            totalSteps: 1,
            currentStep: 1,
            placeholder: "Project name",
            items: runningProjectNames
        });
        return stopServer === null || stopServer === void 0 ? void 0 : stopServer.label;
    });
}

function getActiveServerNames() {
    let runningProjectNames = [];
    launchedServers.forEach((value, key) => {
        if (value.isActive) {
            runningProjectNames.push(key);
        }
    });
    return runningProjectNames;
}

function deactivateServer(currentHelidonServer) {
    if (currentHelidonServer.isActive) {
        killProcess(currentHelidonServer.serverProcess);
        currentHelidonServer.isActive = false;
    }
}

function killProcess(process) {
    ChildProcessAPI_1.ChildProcessAPI.killProcess(process.pid);
}

/**
 * Find folders that contain the specific file and src folder (root folder of the project)
 * @param searchFileName Name of the file for search
 * @param inputDirPaths Directories for search
 */
function getDirsByFileName(inputDirPaths, searchFileName) {
    let dirPaths = [];
    recursiveSearch(inputDirPaths, searchFileName);
    return dirPaths;

    function recursiveSearch(inputDirs, searchFile) {
        for (let inputDir of inputDirs) {
            let searchFilePath = path.join(inputDir, searchFile);
            let srcDirPath = path.join(inputDir, SRC_DIR);
            if (FileSystemAPI_1.FileSystemAPI.isPathExistsSync(searchFilePath) && FileSystemAPI_1.FileSystemAPI.isPathExistsSync(srcDirPath)) {
                dirPaths.push(inputDir);
            }
            FileSystemAPI_1.FileSystemAPI.readDirSync(inputDir).forEach((file) => {
                let filePath = path.join(inputDir, file);
                if (FileSystemAPI_1.FileSystemAPI.isDirectorySync(filePath)) {
                    if (!isDirMatchesPattern(filePath, EXCLUDE_DIRS)) {
                        recursiveSearch([filePath], searchFile);
                    }
                }
            });
        }
        return dirPaths;
    }
}

function isDirMatchesPattern(dirName, patterns) {
    for (let pattern of patterns) {
        if (pattern.test(path.basename(dirName))) {
            return true;
        }
    }
    return false;
}

function getHelidonProjectDirs() {
    let helidonProjectDirs = [];
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

function isPomFileContainsHelidonDependency(pomFilePath) {
    let regex = /.*<dependency>[^<>]*<groupId>[^<>]*helidon[^<>]*<\/groupId>.*/isg;
    let pomContent = FileSystemAPI_1.FileSystemAPI.readTextFileSync(pomFilePath, 'utf8');
    if (pomContent) {
        return regex.test(pomContent);
    }
    return false;
}

/**
 * Find full paths of the root directories for the current workspace
 */
function getRootDirPaths() {
    let dirs = VSCodeAPI_1.VSCodeAPI.getWorkspaceFolders();
    if (!dirs) {
        return [];
    }
    let dirPaths = [];
    for (let dir of dirs) {
        dirPaths.push(dir.uri.fsPath);
    }
    return dirPaths;
}

//# sourceMappingURL=helidonDev.js.map