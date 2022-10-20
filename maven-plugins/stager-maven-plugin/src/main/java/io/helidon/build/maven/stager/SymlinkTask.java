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
package io.helidon.build.maven.stager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.helidon.build.common.Strings;

/**
 * Create a symlink.
 */
final class SymlinkTask extends StagingTask {

    static final String ELEMENT_NAME = "symlink";

    private final String source;

    SymlinkTask(ActionIterators iterators, Map<String, String> attrs) {
        super(ELEMENT_NAME, null, iterators, attrs);
        this.source = Strings.requireValid(attrs.get("source"), "source is required");
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
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        Path link = dir.resolve(resolveVar(target(), vars));
        Path linkTarget = link.getParent().relativize(dir.resolve(resolveVar(source, vars)));
        ctx.logInfo("Creating symlink source: %s, target: %s", link, linkTarget);
        Files.createDirectories(link.getParent());
        Files.createSymbolicLink(link, linkTarget);
    }

    @Override
    public String describe(Path dir, Map<String, String> vars) {
        String link = resolveVar(target(), vars);
        Path linkTarget = dir.resolve(link).getParent().relativize(dir.resolve(resolveVar(source, vars)));
        return ELEMENT_NAME + "{"
                + "target=" + linkTarget
                + ", source=" + link
                + '}';
    }
}
