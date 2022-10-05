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

import java.nio.file.Files
import java.nio.file.Path

static void assertExists(file) {
    if (!Files.exists(file)) {
        throw new AssertionError((String) "${file.toString()} does not exist")
    }
}

static void assertEqual(expected, actual) {
    if (actual != expected) {
        throw new AssertionError((String) "Expected '${expected}' but got '${actual}'")
    }
}

static String symlinkTarget(Path file) {
    if (!Files.isSymbolicLink(file)) {
        throw new AssertionError((String) "${file.toString()} is not a symbolic link")
    }
    return Files.readSymbolicLink(file).toString()
}

def stageDir = basedir.toPath().resolve("target/stage")
assertExists(stageDir)

def file1 = stageDir.resolve("cli/latest")
assertEqual(symlinkTarget(file1), "2.0.0-RC1")

def file2 = stageDir.resolve("docs/latest")
assertEqual(symlinkTarget(file2), "1.4.4")

def file3 = stageDir.resolve("docs/v1")
assertEqual(symlinkTarget(file3), "1.4.4")

def file4 = stageDir.resolve("docs/v2")
assertEqual(symlinkTarget(file4), "2.0.0-RC1")
