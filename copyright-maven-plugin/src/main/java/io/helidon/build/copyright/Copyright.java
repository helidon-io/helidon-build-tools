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

package io.helidon.build.copyright;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.build.util.Log;

/**
 * Copyright checking.
 *
 * @see io.helidon.build.copyright.Copyright.Builder
 * @see #builder()
 * @see #check()
 */
public class Copyright {
    // quick lookup of validators that handle a file suffix
    private final Map<String, Validator> suffixToValidator = new HashMap<>();
    // list of all validators for files not handled by a specific one
    private final List<Validator> allValidators = new LinkedList<>();

    private final boolean checkFormatOnly;
    private final Path rootPath;
    private final Path checkPath;
    private final List<Exclude> excludes;
    private final String masterBranch;
    private final boolean checkAll;
    private final boolean scmOnly;
    private final String currentYear;
    private final Validator textValidator;

    private Copyright(Builder builder) {
        this.checkFormatOnly = builder.checkFormatOnly;
        this.rootPath = builder.root;
        this.checkPath = builder.checkPath;
        this.excludes = builder.excludes;
        this.masterBranch = builder.masterBranch;
        this.scmOnly = builder.scmOnly;
        this.checkAll = builder.checkAll;
        this.currentYear = builder.currentYear;

        // add all validators
        ServiceLoader<ValidatorProvider> loader = ServiceLoader.load(ValidatorProvider.class);
        for (ValidatorProvider validatorProvider : loader) {
            Validator validator = validatorProvider.validator(builder.validatorConfig, builder.templateLines);
            allValidators.add(validator);
            for (String suffix : validator.supportedSuffixes()) {
                suffixToValidator.putIfAbsent(suffix, validator);
            }
        }
        // now add built-in validators
        List<Validator> builtIns = new LinkedList<>();

        builtIns.add(new ValidatorJava(builder.validatorConfig, builder.templateLines));
        builtIns.add(new ValidatorProperties(builder.validatorConfig, builder.templateLines));
        builtIns.add(new ValidatorXml(builder.validatorConfig, builder.templateLines));
        builtIns.add(new ValidatorAsciidoc(builder.validatorConfig, builder.templateLines));
        builtIns.add(new ValidatorBat(builder.validatorConfig, builder.templateLines));
        this.textValidator = new ValidatorText(builder.validatorConfig, builder.templateLines);
        builtIns.add(this.textValidator);

        for (Validator validator : builtIns) {
            allValidators.add(validator);
            for (String suffix : validator.supportedSuffixes()) {
                suffixToValidator.putIfAbsent(suffix, validator);
            }
        }

    }

    static String logGood(String good) {
        return "\"$(GREEN " + good.replace(")", "\\)") + ")\"";
    }

    static String logBad(String bad) {
        return "\"$(YELLOW " + bad.replace(")", "\\)") + ")\"";
    }

    /**
     * Check copyrights of files and return a list of found problems.
     *
     * @return list of problems
     */
    public List<String> check() {
        List<String> messages = new LinkedList<>();

        // find files to check and their last modified year (if checking year as well)
        List<ValidPath> validPaths = findFilesToCheck(messages);

        if (!messages.isEmpty()) {
            return messages;
        }

        // perform copyright check on all files
        for (ValidPath validPath : validPaths) {
            checkCopyright(validPath, messages);
        }

        return messages;
    }

    private void checkCopyright(ValidPath validPath, List<String> messages) {
        Path path = validPath.file.path();
        String relativePath = validPath.file.relativePath();

        if (!Files.isReadable(path)) {
            messages.add(relativePath + ": not readable");
            return;
        }
        if (FileSystem.size(path) == 0L) {
            Log.debug(relativePath + ": ignoring empty file");
            return;
        }

        Validator validator = suffixToValidator.get(validPath.file.suffix());
        if (validator == null) {
            for (Validator candidate : allValidators) {
                if (candidate.supports(path)) {
                    validator = candidate;
                    break;
                }
            }
        }
        if (validator == null) {
            Log.debug(relativePath + ": using fallback text validator.");
            validator = textValidator;
        }

        Log.verbose(relativePath + " checking copyright with " + validator.getClass().getName());
        Optional<String> validate = validator.validate(path, validPath.modifiedYear);
        validate.ifPresent(s -> messages.add("$(CYAN " + relativePath + "): " + s));
    }

    private List<ValidPath> findFilesToCheck(List<String> messages) {

        // we need to find the complete list of files to check
        Set<String> filesToCheck;
        Set<String> locallyModified;
        if (checkAll) {
            if (checkFormatOnly) {
                locallyModified = Set.of();
            } else {
                locallyModified = Git.locallyModified(rootPath, checkPath);
            }
            if (scmOnly) {
                filesToCheck = new HashSet<>();
                filesToCheck.addAll(Git.gitTracked(rootPath, checkPath));
                filesToCheck.addAll(locallyModified);
            } else {
                filesToCheck = findAllFiles(rootPath, checkPath);
            }
        } else {
            locallyModified = Git.locallyModified(rootPath, checkPath);
            Set<String> gitModified = Git.gitModified(rootPath, checkPath, masterBranch);
            filesToCheck = new HashSet<>(locallyModified);
            filesToCheck.addAll(gitModified);
        }

        List<ValidPath> validPaths = new LinkedList<>();

        for (String relativePath : filesToCheck) {
            addValidFile(relativePath, locallyModified, validPaths, messages);
        }

        return validPaths;
    }

    private static Set<String> findAllFiles(Path rootPath,
                                            Path checkPath) {
        Set<String> result = new HashSet<>();

        try {
            Files.walkFileTree(checkPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    result.add(rootPath.relativize(file).toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new CopyrightException("Failed to list files", e);
        }

        return result;
    }

    private void addValidFile(String relativePath,
                              Set<String> modified,
                              List<ValidPath> validPaths,
                              List<String> messages) {

        FileRequest file = FileRequest.create(rootPath, relativePath);

        // file may have been deleted from GIT (or locally)
        if (!Files.exists(file.path())) {
            Log.debug("File " + relativePath + " does not exist, ignoring.");
            return;
        }

        for (Exclude exclude : excludes) {
            if (exclude.exclude(file)) {
                Log.debug("Excluding " + relativePath);
                return;
            }
        }

        if (checkFormatOnly) {
            // we do not care about year
            validPaths.add(new ValidPath(file, currentYear));
            return;
        }

        if (modified.contains(relativePath)) {
            Log.debug("Including " + relativePath + " with current year (" + currentYear
                              + "), as file is locally modified");
            validPaths.add(new ValidPath(file, currentYear));
        } else {
            // now I need to get last modified date from git
            Git.yearModified(rootPath, relativePath, messages)
                    .ifPresent(year -> {
                        Log.debug("Including " + relativePath + " with git modified year (" + year + ")");
                        validPaths.add(new ValidPath(file, year));
                    });
        }
    }

    /**
     * Fluent API builder for {@link io.helidon.build.copyright.Copyright}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link io.helidon.build.copyright.Copyright}.
     */
    public static class Builder {
        private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");

        private final String currentYear = YEAR_FORMATTER.format(ZonedDateTime.now());
        private final Validator.ValidatorConfig.Builder validatorConfigBuilder = Validator.ValidatorConfig.builder();
        // values with defaults
        private String masterBranch = "master";
        private String yearSeparator = ", ";
        private boolean checkFormatOnly = false;
        private boolean checkAll = false;
        private boolean scmOnly = true;

        // values that must be provided
        private Path root;
        private Path checkPath;
        private Path excludesFile;
        private Path templateFile;
        private List<Exclude> excludes;

        private Validator.ValidatorConfig validatorConfig;
        private List<TemplateLine> templateLines;

        private Builder() {
        }

        /**
         * Build a new copyright.
         *
         * @return copyright from this builder
         */
        public Copyright build() {
            if (checkPath == null) {
                throw new CopyrightException("The directory to check must be defined");
            }

            validatorConfig = validatorConfigBuilder
                    .checkFormatOnly(checkFormatOnly)
                    .yearSeparator(yearSeparator)
                    .build();

            if (templateFile == null) {
                Log.verbose("Parsing default template file (Apache 2).");
                this.templateLines = TemplateLine.parseTemplate(validatorConfig, defaultCopyrightTemplate());
            } else {
                Log.verbose("Parsing template file: " + templateFile.toAbsolutePath());
                this.templateLines = TemplateLine.parseTemplate(validatorConfig, FileSystem.toLines(templateFile));
            }

            excludes = parseExcludes();
            root = Git.repositoryRoot(checkPath);
            return new Copyright(this);
        }

        private List<String> defaultCopyrightTemplate() {
            InputStream is = Copyright.class.getResourceAsStream("apache.txt");

            List<String> lines = new LinkedList<>();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                throw new CopyrightException("Failed to read default template from classpath");
            }
            return lines;
        }

        /**
         * Git branch to check from, defaults to {@code master}.
         *
         * @param masterBranch name of the master branch
         * @return updated builder
         */
        public Builder masterBranch(String masterBranch) {
            this.masterBranch = masterBranch;
            return this;
        }

        /**
         * Path to check.
         *
         * @param checkPath directory to check
         * @return updated builder
         */
        public Builder path(Path checkPath) {
            this.checkPath = checkPath;
            return this;
        }

        /**
         * File with excludes.
         *
         * @param excludesFile excludes file
         * @return updated builder
         */
        public Builder excludesFile(Path excludesFile) {
            this.excludesFile = excludesFile;
            return this;
        }

        /**
         * File with copyright template.
         *
         * @param templateFile template file
         * @return updated builder
         */
        public Builder templateFile(Path templateFile) {
            this.templateFile = templateFile;
            return this;
        }

        /**
         * Year separator, defaults to {@code , }.
         * The result is {@code 2019, 2021}.
         * Only the first and last year are used.
         *
         * @param yearSeparator separator of years
         * @return updated builder
         */
        public Builder yearSeparator(String yearSeparator) {
            this.yearSeparator = yearSeparator;
            return this;
        }

        /**
         * Whether to only check format, or also year (default).
         *
         * @param checkFormatOnly whether to check format only, defaults to {@code false}
         * @return updated builder
         */
        public Builder checkFormatOnly(boolean checkFormatOnly) {
            this.checkFormatOnly = checkFormatOnly;
            return this;
        }

        /**
         * Whether to check all files, or only modified (default).
         *
         * @param checkAll whether to check all files, defaults to {@code false}
         * @return updated builder
         */
        public Builder checkAll(boolean checkAll) {
            this.checkAll = checkAll;
            return this;
        }

        /**
         * Only include files that are version managed by git.
         * This is ignored unless {@link #checkAll(boolean)} is set to {@code true}.
         *
         * @param scmOnly only check files that are version managed by git, defaults to {@code true}
         * @return updated builder
         */
        public Builder scmOnly(boolean scmOnly) {
            this.scmOnly = scmOnly;
            return this;
        }

        private List<Exclude> parseExcludes() {
            if (excludesFile == null) {
                return List.of();
            }

            try {
                Log.verbose("Parsing exclude file: " + excludesFile.toAbsolutePath());
                return Files.readAllLines(excludesFile)
                        .stream()
                        .map(String::trim)
                        .filter(it -> !it.startsWith("#"))
                        .filter(it -> !it.isBlank())
                        .map(Exclude::create)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new CopyrightException("Failed to parse excludes file: " + excludesFile, e);
            }
        }
    }

    private static class ValidPath {
        private final FileRequest file;
        private final String modifiedYear;

        ValidPath(FileRequest file, String modifiedYear) {
            this.file = file;
            this.modifiedYear = modifiedYear;
        }
    }
}
