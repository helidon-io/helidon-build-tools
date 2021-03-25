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
exports.ProjectInformation = void 0;
const vscode_1 = require("vscode");
const common_1 = require("./common");

class ProjectInformation {
    constructor(uri, markers, name) {
        this.uri = uri;
        this.name = name;
        this.markers = markers;
    }

    static getInformation(uri) {
        return __awaiter(this, void 0, void 0, function* () {
            const projectInformation = yield vscode_1.commands.executeCommand("java.execute.workspaceCommand", common_1.VSCodeJavaCommands.JAVA_MARKERS_COMMAND, {uri});
            return new ProjectInformation(projectInformation ? projectInformation.uri : uri, projectInformation ? projectInformation.markers : [], projectInformation ? projectInformation.name : undefined);
        });
    }

    isHelidonProject() {
        return this.markers.includes(ProjectMarker.HELIDON);
    }
}

exports.ProjectInformation = ProjectInformation;
var ProjectMarker;
(function (ProjectMarker) {
    ProjectMarker["JAVA"] = "java";
    ProjectMarker["HELIDON"] = "helidon";
    ProjectMarker["Maven"] = "maven";
})(ProjectMarker || (ProjectMarker = {}));
//# sourceMappingURL=ProjectInformation.js.map