/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package io.helidon.build.common;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static io.helidon.build.common.FileUtils.javaExecutableInJavaHome;
import static io.helidon.build.common.FileUtils.javaExecutableInPath;
import static java.io.File.pathSeparatorChar;

/**
 * A factory for {@link ProcessBuilder} instances used to execute java.
 */
public class JavaProcessBuilder {

    private static final String PATH_VAR = "PATH";

    /**
     * Returns a new instance.
     *
     * @return The instance.
     */
    public static ProcessBuilder newInstance() {
        final ProcessBuilder builder = new ProcessBuilder();

        // If java is not in the PATH and we have a valid JAVA_HOME set, prepend it to the PATH

        if (javaExecutableInPath().isEmpty()) {
            final Optional<Path> javaHomeExecutable = javaExecutableInJavaHome();
            if (javaHomeExecutable.isPresent()) {
                final Map<String, String> env = builder.environment();
                final String javaBinDir = javaHomeExecutable.get().getParent().toString();
                final String pathVar = javaBinDir + pathSeparatorChar + env.get(PATH_VAR);
                env.put(PATH_VAR, pathVar);
            }
        }
        return builder;
    }

    private JavaProcessBuilder() {
    }
}
