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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Generate a directory using a set of actions.
 */
class StagingDirectory implements StagingAction {

    static final String ELEMENT_NAME = "directory";

    private final List<StagingAction> actions;
    private final String target;

    StagingDirectory(String target, List<StagingAction> actions) {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("target is required");
        }
        this.target = target;
        this.actions = actions == null ? List.of() : actions;
    }

    @Override
    public String elementName() {
        return ELEMENT_NAME;
    }

    /**
     * Get the target.
     *
     * @return target, never {@code null}
     */
    String target() {
        return target;
    }

    /**
     * Nested actions.
     *
     * @return actions, never {@code null}
     */
    List<StagingAction> actions() {
        return actions;
    }

    @Override
    public void execute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
        Path targetDir = dir.resolve(target());
        Files.createDirectories(targetDir);
        for (StagingAction action : actions()) {
            action.execute(context, targetDir, variables);
        }
    }

    @Override
    public String describe(Path dir, Map<String, String> variables) {
        return ELEMENT_NAME + "{"
                + "target='" + dir.resolve(target()) + '\''
                + '}';
    }
}
