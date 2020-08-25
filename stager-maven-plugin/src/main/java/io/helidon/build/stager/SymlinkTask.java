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
import java.util.Map;

/**
 * Create a symlink.
 */
final class SymlinkTask extends StagingTask {

    static final String ELEMENT_NAME = "symlink";

    private final String source;

    SymlinkTask(ActionIterators iterators, String source, String target) {
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
    public String elementName() {
        return ELEMENT_NAME;
    }

    @Override
    protected void doExecute(StagingContext context, Path dir, Map<String, String> variables) throws IOException {
        Path link = dir.resolve(resolveVar(target(), variables));
        Path linkTarget = link.getParent().relativize(dir.resolve(resolveVar(source, variables)));
        context.logInfo("Creating symlink source: %s, target: %s", link, linkTarget);
        Files.createSymbolicLink(link, linkTarget);
    }

    @Override
    public String describe(Path dir, Map<String, String> variables) {
        String link = resolveVar(target(), variables);
        Path linkTarget = dir.resolve(link).getParent().relativize(dir.resolve(resolveVar(source, variables)));
        return ELEMENT_NAME + "{"
                + "target=" + linkTarget
                + ", source=" + link
                + '}';
    }
}
