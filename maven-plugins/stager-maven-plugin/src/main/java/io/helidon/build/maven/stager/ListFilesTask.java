/*
 * Copyright (c) 2023, 2026 Oracle and/or its affiliates.
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

import java.nio.file.FileVisitOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import io.helidon.build.common.Lists;
import io.helidon.build.common.SourcePath;
import io.helidon.build.common.xml.XMLElement;

import static io.helidon.build.common.FileUtils.walk;
import static io.helidon.build.common.Strings.normalizePath;

/**
 * List files in a directory.
 */
final class ListFilesTask extends StagingTask implements TextTask {

    private static final Set<FileVisitOption> FILE_VISIT_OPTIONS = Set.of(FileVisitOption.FOLLOW_LINKS);

    private final List<String> includes;
    private final List<String> excludes;
    private final List<Substitution> substitutions;
    private final List<BiFunction<String, Map<String, String>, String>> chain;
    private final String dirName;
    private final Map<Map<String, String>, String> results = new ConcurrentHashMap<>();

    ListFilesTask(XMLElement element) {
        super(element);
        this.includes = Lists.map(elements(element, "include", "includes"), XMLElement::value);
        this.excludes = Lists.map(elements(element, "exclude", "excludes"), XMLElement::value);
        this.substitutions = Lists.map(elements(element, "substitution", "substitutions"), Substitution::new);
        this.chain = Lists.map(substitutions, Substitution::function);
        this.dirName = element.attribute("dir", ".");
    }

    @Override
    public String text(Map<String, String> vars) {
        return results.getOrDefault(vars, "");
    }

    @Override
    public String toString() {
        return "ListFilesTask{"
               + "includes=" + includes
               + ", excludes=" + excludes
               + ", substitutions=" + substitutions
               + ", chain=" + chain
               + ", dirName='" + dirName + '\''
               + ", results=" + results
               + '}';
    }

    @Override
    protected void doExecute(StagingContext ctx, Path dir, Map<String, String> vars) {
        StringBuilder sb = new StringBuilder();
        Path resolved = dir.resolve(resolveVar(dirName, vars));
        List<Path> files = walk(resolved, FILE_VISIT_OPTIONS, this::filter);
        for (Path file : files) {
            String entry = normalizePath(dir.relativize(file));
            for (var function : chain) {
                entry = function.apply(entry, vars);
            }
            sb.append(entry).append("\n");
        }
        results.put(vars, sb.toString());
    }

    private boolean filter(Path p, BasicFileAttributes attrs) {
        return attrs.isDirectory() || new SourcePath(p).matches(includes, excludes);
    }
}
