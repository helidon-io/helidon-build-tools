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

import { commands } from "vscode";
import { VSCodeJavaCommands } from "./common";

export class ProjectInformation {
    uri: string;
    name: string | undefined;
    markers: ProjectMarker[];

    constructor(uri: string, markers: ProjectMarker[], name?: string) {
        this.uri = uri;
        this.name = name;
        this.markers = markers;
    }

    public static async getInformation(uri: string): Promise<ProjectInformation> {
        const projectInformation: { uri: string; name: string; markers: ProjectMarker[] } | undefined =
            await commands.executeCommand(
                "java.execute.workspaceCommand",
                VSCodeJavaCommands.JAVA_MARKERS_COMMAND,
                {uri}
            );
        return new ProjectInformation(
            projectInformation ? projectInformation.uri : uri,
            projectInformation ? projectInformation.markers : [],
            projectInformation ? projectInformation.name : undefined);
    }

    public isHelidonProject(): boolean {
        return this.markers.includes(ProjectMarker.HELIDON);
    }

}

enum ProjectMarker {
    JAVA = 'java',
    HELIDON = 'helidon',
    Maven = 'maven'
}
