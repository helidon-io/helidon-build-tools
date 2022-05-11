/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

import java.nio.file.FileSystems
import java.nio.file.Files

static void assertExists(file) {
    if (!Files.exists(file)) {
        throw new AssertionError((Object) "${file.toString()} does not exist")
    }
}

static void assertEqual(expected, actual) {
    if (actual != expected) {
        throw new AssertionError((Object) "Expected '${expected}' but got '${actual}'")
    }
}

static newZipFileSystem(zip) {
    String uriPrefix = "jar:file:"
    if (File.pathSeparator == ";") {
        uriPrefix += "/"
    }
    URI uri = URI.create("${uriPrefix}${zip.toString().replace("\\", "/")}")
    return FileSystems.newFileSystem(uri, [:])
}

def stageDir = basedir.toPath().resolve("target/stage")
assertExists(stageDir)
def archive = stageDir.resolve("cli-data/2.0.0-RC1/cli-data.zip")
assertExists(archive)

def archiveRoot = newZipFileSystem(archive).getPath("/")
def file1 = archiveRoot.resolve("versions.json")
assertExists(file1)
assertEqual("""{
    "versions": [
            "3.0.0-SNAPSHOT",
            "2.5.0",
            "2.4.2",
            "2.4.0",
            "2.0.1",
            "2.0.0"
    ],
    "latest": "3.0.0-SNAPSHOT"
}
""", Files.readString(file1))
