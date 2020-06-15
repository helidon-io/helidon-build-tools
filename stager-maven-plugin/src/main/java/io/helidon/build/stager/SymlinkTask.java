/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.stager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.unix.Symlink;

/**
 * Create a symlink.
 */
final class SymlinkTask extends StagingTask {

    private final String source;

    SymlinkTask(List<Map<String, List<String>>> iterators, String source, String target) {
        super(iterators, target);
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("source is required");
        }
        this.source = source;
    }

    /**
     * Get the symlink source.
     * @return source, never {@code null}
     */
    String source() {
        return source;
    }

    @Override
    protected void doExecute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
        String resolvedTarget = resolveVar(target(), variables);
        String resolvedSource = resolveVar(source, variables);
        context.logInfo("Creating symlink source: %s, target: %s", resolvedSource, resolvedTarget);
        Project antProject = new Project();
        antProject.setBaseDir(dir.toFile());
        antProject.addBuildListener(new AntBuildListener(context));
        Symlink symlink = new Symlink();
        symlink.setProject(antProject);
        symlink.setResource(resolvedSource);
        symlink.setLink(resolvedTarget);
        symlink.execute();
    }

    /**
     * {@code BuilderListener} implementation to log Ant events.
     */
    private static final class AntBuildListener implements BuildListener {

        private final StagingContext context;

        private AntBuildListener(final StagingContext context) {
            this.context = context;
        }

        @Override
        public void buildStarted(final BuildEvent event) {
        }

        @Override
        public void buildFinished(final BuildEvent event) {
        }

        @Override
        public void targetStarted(final BuildEvent event) {
        }

        @Override
        public void targetFinished(final BuildEvent event) {
        }

        @Override
        public void taskStarted(final BuildEvent event) {
        }

        @Override
        public void taskFinished(final BuildEvent event) {
        }

        @Override
        public void messageLogged(final BuildEvent event) {
            context.logDebug("[symlink] %s", event.getMessage());
        }
    }
}
