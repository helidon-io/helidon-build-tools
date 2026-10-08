/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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
import io.helidon.build.common.xml.XMLElement;

/**
 * Create a symlink.
 */
final class SymlinkTask extends StagingTask {

    private final String source;
    private final String target;

    SymlinkTask(XMLElement element) {
        super(element);
        this.source = Strings.requireValid(element.attribute("source", null), "source is required");
        this.target = Strings.requireValid(element.attribute("target", null), "target is required");
    }

    @Override
    public String toString() {
        return "SymlinkTask{"
               + "source='" + source + '\''
               + ", target='" + target + '\''
               + '}';
    }

    @Override
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) throws IOException {
        var link = dir.resolve(resolveVar(target, vars));
        var linkTarget = link.getParent().relativize(dir.resolve(resolveVar(source, vars)));
        ctx.logInfo("Creating symlink source: %s, target: %s", link, linkTarget);
        ctx.ensureDirectory(link.getParent());
        Files.createSymbolicLink(link, linkTarget);
    }
}
