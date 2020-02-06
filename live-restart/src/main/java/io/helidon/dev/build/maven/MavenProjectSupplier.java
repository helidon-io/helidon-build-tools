/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.build.maven;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.helidon.dev.build.Project;
import io.helidon.dev.build.ProjectSupplier;
import io.helidon.dev.build.util.ConsumerPrintStream;

import org.apache.maven.cli.MavenCli;
import org.apache.maven.project.MavenProject;

import static io.helidon.build.util.FileUtils.assertFile;

/**
 * A {@code ProjectSupplier} for Maven projects.
 */
public class MavenProjectSupplier implements ProjectSupplier {
    private static final String POM_FILE = "pom.xml";
    private static final String PROJECT_DIRECTORY_PROPERTY = "maven.multiModuleProjectDirectory";
    private static final String[] CLEAN_BUILD_ARGS = {"clean", "prepare-package", "-DskipTests"};
    private static final String[] BUILD_ARGS = {"prepare-package", "-DskipTests"};
    private static final AtomicReference<MavenProject> PROJECT = new AtomicReference<>();

    static void setProject(MavenProject project) {
        PROJECT.set(project);
    }

    @Override
    public Project get(Path projectDir, boolean clean, Consumer<String> stdOut, Consumer<String> stdErr) throws Exception {
        try {
            final Path pomFile = assertFile(projectDir.resolve(POM_FILE));
            final MavenCli maven = new MavenCli();
            final String[] args = clean ? CLEAN_BUILD_ARGS : BUILD_ARGS;
            final PrintStream stdOutStream = ConsumerPrintStream.newStream(stdOut);
            final PrintStream stdErrStream = ConsumerPrintStream.newStream(stdErr);
            System.getProperties().put(PROJECT_DIRECTORY_PROPERTY, projectDir.toString());

            // TODO: this fails, looks like it may be a missing dependency (if we're lucky)
            if (maven.doMain(args, projectDir.toString(), stdOutStream, stdErrStream) != 0) {
                throw new Exception("Build failed.");
            }
            final MavenProject mavenProject = PROJECT.get();
            if (mavenProject == null) {
                throw new IllegalStateException("Maven project not found.");
            }

            return null; // TODO use Project.builder()

        } catch (Exception e) {
            stdErr.accept(e.getMessage());
            throw e;
        } catch (Throwable e) {
            stdErr.accept(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
