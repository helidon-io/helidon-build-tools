/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.devloop.maven;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.devloop.BuildExecutor;
import io.helidon.build.devloop.BuildMonitor;

/**
 * A {@link BuildExecutor} that executes within the current process. Uses reflection to avoid build time
 * dependencies, so assume usage within a maven plugin or some equivalent that sets up the thread context
 * {@code ClassLoader} correctly.
 */
public class EmbeddedMavenExecutor extends BuildExecutor {
    private static final AtomicReference<Object> MAVEN_CLI = new AtomicReference<>();
    private static final AtomicReference<Method> DO_MAIN_METHOD = new AtomicReference<>();
    private static final String MAVEN_CLI_CLASS_NAME = "org.apache.maven.cli.MavenCli";
    private static final String DO_MAIN_METHOD_NAME = "doMain";

    private final PrintStream out;
    private final PrintStream err;

    /**
     * Constructor.
     *
     * @param projectDir The project directory.
     * @param monitor The build monitor. All output is written to {@link BuildMonitor#stdOut()} and
     * {@link BuildMonitor#stdErr()}.
     */
    public EmbeddedMavenExecutor(Path projectDir, BuildMonitor monitor) {
        super(projectDir, monitor);
        initialize();
        this.out = monitor.stdOut();
        this.err = monitor.stdErr();
    }

    @Override
    public boolean willFork() {
        return false;
    }

    @Override
    public void execute(String... args) throws Exception {
        final Object mavenCli = MAVEN_CLI.get();
        final Method doMain = DO_MAIN_METHOD.get();
        final ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(mavenCli.getClass().getClassLoader());
        try {
            Object result = doMain.invoke(mavenCli, args, projectDirectory().toString(), out, err);
            int exitValue = ((Number) result).intValue();
            if (exitValue != 0) {
                throw new Exception("Build failed");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(origLoader);
        }
    }

    private static void initialize() {
        if (MAVEN_CLI.get() == null) {
            try {
                final ClassLoader loader = Thread.currentThread().getContextClassLoader();
                final Class<?> cliClass = loader.loadClass(MAVEN_CLI_CLASS_NAME);
                MAVEN_CLI.set(cliClass.getDeclaredConstructor().newInstance());
                final Class<?>[] parameterTypes = {String[].class, String.class, PrintStream.class, PrintStream.class};
                DO_MAIN_METHOD.set(cliClass.getMethod(DO_MAIN_METHOD_NAME, parameterTypes));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
