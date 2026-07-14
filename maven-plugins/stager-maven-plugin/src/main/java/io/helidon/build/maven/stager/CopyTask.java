/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import io.helidon.build.common.Lists;
import io.helidon.build.common.SourcePath;
import io.helidon.build.common.Strings;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Copy project directory contents to a target directory.
 */
final class CopyTask extends StagingTask {

    private final String source;
    private final List<String> includes;
    private final List<String> excludes;

    CopyTask(ActionIterators iterators, List<Include> includes, List<Exclude> excludes, Map<String, String> attrs) {
        super("copy", null, iterators, attrs);
        this.source = Strings.requireValid(attrs.get("source"), "source is required");
        Strings.requireValid(target(), "target is required");
        this.includes = Lists.map(includes, Include::value);
        this.excludes = Lists.map(excludes, Exclude::value);
    }

    @Override
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
        String resolvedSource = resolveVar(source, vars);
        String resolvedTarget = resolveVar(target(), vars);
        Path sourceDir = ctx.resolve(resolvedSource).normalize();
        Path targetDir = dir.resolve(resolvedTarget).normalize();
        List<String> resolvedIncludes = Lists.map(includes, pattern -> resolveVar(pattern, vars));
        List<String> resolvedExcludes = Lists.map(excludes, pattern -> resolveVar(pattern, vars));

        if (!Files.exists(sourceDir)) {
            throw new IllegalStateException(sourceDir + " does not exist");
        }
        if (!Files.isDirectory(sourceDir)) {
            throw new IllegalStateException(sourceDir + " is not a directory");
        }

        try (Stream<SourcePath> paths = SourcePath.stream(sourceDir)) {
            paths.filter(p -> p.matches(resolvedIncludes, resolvedExcludes))
                    .map(p -> p.asString(false))
                    .forEach(p -> {
                        Path sourceFile = sourceDir.resolve(p);
                        Path targetFile = targetDir.resolve(p);
                        ctx.ensureDirectory(targetFile.getParent());
                        try {
                            Files.copy(sourceFile, targetFile, REPLACE_EXISTING, COPY_ATTRIBUTES, NOFOLLOW_LINKS);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    String source() {
        return source;
    }

    List<String> includes() {
        return includes;
    }

    List<String> excludes() {
        return excludes;
    }
}
