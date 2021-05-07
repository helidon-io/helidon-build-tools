/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.build.maven.enforcer.typo;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.helidon.build.common.Log;
import io.helidon.build.maven.enforcer.EnforcerException;
import io.helidon.build.maven.enforcer.FileMatcher;
import io.helidon.build.maven.enforcer.FileRequest;
import io.helidon.build.maven.enforcer.FoundFiles;
import io.helidon.build.maven.enforcer.RuleFailure;

/**
 * Rule for typos checking.
 */
public class TyposRule {
    private static final Set<String> DEFAULT_INCLUDES = Set.of(".java", ".xml", ".adoc",
                                                               ".txt", ".md", ".html",
                                                               ".css", ".properties", ".yaml",
                                                               ".sh", ".sql", ".conf",
                                                               ".json", ".kt", ".groovy",
                                                               ".mustache", ".yml", ".graphql",
                                                               ".proto", "Dockerfile.native", "Dockerfile.jlink",
                                                               ".gradle", ".MF");
    private final Set<String> typos;
    private final List<FileMatcher> excludes;
    private final List<FileMatcher> includes;

    private TyposRule(Builder builder) {
        this.typos = builder.typosConfig.typos();
        this.excludes = builder.excludes();
        this.includes = builder.includes();
    }

    /**
     * Fluent API builder for typos rule.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Check the files for typos.
     *
     * @param filesToCheck files to check
     * @return list of identified failures
     */
    public List<RuleFailure> check(FoundFiles filesToCheck) {
        List<RuleFailure> failures = new LinkedList<>();

        AtomicInteger count = new AtomicInteger();

        filesToCheck.fileRequests()
                .stream()
                .filter(this::include)
                .forEach(fr -> {
                    count.incrementAndGet();
                    processFile(fr, failures);
                });

        if (failures.size() == 0) {
            Log.info("Typos processed " + count.get() + " files.");
        } else {
            Log.info("Typos processed " + count.get() + " files, found " + failures.size() + " errors");
        }
        return failures;
    }

    private void processFile(FileRequest fr, List<RuleFailure> errors) {
        try (BufferedReader bufferedReader = Files.newBufferedReader(fr.path(), StandardCharsets.UTF_8)) {
            String line;
            int lineNum = 0;
            List<String> foundTypos = new LinkedList<>();
            while ((line = bufferedReader.readLine()) != null) {
                // we want to start from 1
                lineNum++;
                for (String typo : typos) {
                    if (line.toLowerCase().contains(typo)) {
                        // we have to exclude the definition of the typo itself in plugin config
                        if (fr.fileName().equals("pom.xml")) {
                            // this may be a typo definition
                            if (line.toLowerCase().contains("<typo>" + typo + "</typo>")) {
                                // yep, this is it
                                continue;
                            }
                        }
                        foundTypos.add(typo);
                    }
                }
                if (!foundTypos.isEmpty()) {
                    errors.add(RuleFailure.create(fr, lineNum, "typos found: " + String.join(", ", foundTypos)));
                }
                foundTypos.clear();
            }

        } catch (IOException e) {
            throw new EnforcerException("Failed to process file for typos: " + fr.relativePath());
        }
    }

    private boolean include(FileRequest fr) {
        for (FileMatcher exclude : excludes) {
            if (exclude.matches(fr)) {
                return false;
            }
        }

        for (FileMatcher include : includes) {
            if (include.matches(fr)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Fluent API builder for {@link io.helidon.build.maven.enforcer.typo.TyposRule}.
     */
    public static class Builder {
        private TypoConfig typosConfig;

        private Builder() {
        }

        /**
         * Update builder from maven configuration.
         *
         * @param typosConfig maven config
         * @return updated builder
         */
        public Builder config(TypoConfig typosConfig) {
            this.typosConfig = typosConfig;
            return this;
        }

        /**
         * Build a new instance of {@link io.helidon.build.maven.enforcer.typo.TyposRule}.
         *
         * @return new rule
         */
        public TyposRule build() {
            return new TyposRule(this);
        }

        List<FileMatcher> excludes() {
            return typosConfig.excludes()
                    .stream()
                    .map(FileMatcher::create)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }

        List<FileMatcher> includes() {
            Set<String> includes = new HashSet<>(DEFAULT_INCLUDES);
            includes.addAll(typosConfig.includes());
            return includes.stream()
                    .map(FileMatcher::create)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
    }
}
