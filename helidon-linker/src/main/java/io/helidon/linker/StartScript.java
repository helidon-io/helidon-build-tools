/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.linker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import io.helidon.linker.util.StreamUtils;

/**
 * Installs a start script for a main jar.
 */
public class StartScript {
    private static final String SCRIPT_TEMPLATE_PATH = "start-template.sh";
    private static final String SCRIPT_MAIN_JAR_NAME = "<MAIN_JAR_NAME>";
    private static final String SCRIPT_PATH = "bin/start";
    private static final String TEMPLATE = template();
    private final String script;

    /**
     * Returns a new instance for the given main jar.
     *
     * @param mainJarFile The main jar that the script should start.
     * @return The instance.
     */
    static StartScript newScript(Path mainJarFile) {
        return new StartScript(mainJarFile.getFileName().toString());
    }

    private StartScript(String mainJarName) {
        this.script = TEMPLATE.replace(SCRIPT_MAIN_JAR_NAME, mainJarName);
    }

    /**
     * Install the script in the given JRE path.
     *
     * @param jrePath The path.
     * @return The path to the installed script.
     */
    Path install(Path jrePath) {
        try {
            final Path scriptFile = jrePath.resolve(SCRIPT_PATH);
            Files.copy(new ByteArrayInputStream(script.getBytes()), scriptFile);
            Files.setPosixFilePermissions(scriptFile, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_EXECUTE
            ));
            return scriptFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the script.
     *
     * @return The script.
     */
    @Override
    public String toString() {
        return script;
    }

    private static String template() {
        try {
            return StreamUtils.toString(StartScript.class.getClassLoader().getResourceAsStream(SCRIPT_TEMPLATE_PATH));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
