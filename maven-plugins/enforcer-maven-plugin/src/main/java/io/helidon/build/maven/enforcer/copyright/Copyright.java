/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.maven.enforcer.copyright;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import io.helidon.build.common.logging.Log;
import io.helidon.build.maven.enforcer.EnforcerException;
import io.helidon.build.maven.enforcer.FileMatcher;
import io.helidon.build.maven.enforcer.FileRequest;
import io.helidon.build.maven.enforcer.FileSystem;
import io.helidon.build.maven.enforcer.FoundFiles;
import io.helidon.build.maven.enforcer.RuleFailure;
import io.helidon.build.maven.enforcer.RuleFailureException;
import io.helidon.build.maven.enforcer.copyright.spi.ValidatorProvider;

/**
 * Copyright checking.
 *
 * @see Copyright.Builder
 * @see #builder()
 * @see #check(io.helidon.build.maven.enforcer.FoundFiles)
 */
public class Copyright {
    // quick lookup of validators that handle a file suffix
    private final Map<String, Validator> suffixToValidator = new HashMap<>();
    // list of all validators for files not handled by a specific one
    private final List<Validator> allValidators = new LinkedList<>();

    private final List<FileMatcher> excludes;
    private final Validator textValidator;

    private Copyright(Builder builder) {
        this.excludes = builder.excludes;

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
        builtIns.add(new ValidatorJsp(builder.validatorConfig, builder.templateLines));
        builtIns.add(new ValidatorHandlebars(builder.validatorConfig, builder.templateLines));
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
     * Fluent API builder for {@link Copyright}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Check copyrights of files and return a list of found problems.
     *
     * @param files files discovered from git and local file system
     * @return list of problems
     */
    public List<RuleFailure> check(FoundFiles files) {
        Log.info("Obtaining last modified year for up to " + files.fileRequests().size() + " files");
        // find files to check and their last modified year (if checking year as well)
        List<FileRequest> validPaths = findFilesToCheck(files);

        List<RuleFailure> failures = new LinkedList<>();

        // perform copyright check on all files
        for (FileRequest fileRequest : validPaths) {
            checkCopyright(fileRequest, failures);
        }

        return failures;
    }

    private void checkCopyright(FileRequest file, List<RuleFailure> messages) {
        Path path = file.path();
        String relativePath = file.relativePath();

        if (!Files.isReadable(path)) {
            messages.add(RuleFailure.create(file, -1, "not readable"));
            return;
        }
        if (FileSystem.size(path) == 0L) {
            Log.debug(relativePath + ": ignoring empty file");
            return;
        }

        Validator validator = suffixToValidator.get(file.suffix());
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
        try {
            validator.validate(file, path);
        } catch (RuleFailureException e) {
            messages.add(e.failure());
        }
    }

    private List<FileRequest> findFilesToCheck(FoundFiles files) {

        List<FileRequest> validPaths = new LinkedList<>();

        for (FileRequest fileRequest : files.fileRequests()) {
            addValidFile(fileRequest, validPaths);
        }

        return validPaths;
    }

    private void addValidFile(FileRequest file,
                              List<FileRequest> validPaths) {

        for (FileMatcher exclude : excludes) {
            if (exclude.matches(file)) {
                Log.debug("Excluding " + file.relativePath());
                return;
            }
        }

        // now I need to get last modified date from git
        Log.debug("Including " + file.relativePath() + " with modified year (" + file.lastModifiedYear() + ")");
        validPaths.add(file);
    }

    /**
     * Builder for {@link Copyright}.
     */
    public static class Builder {
        private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");

        private final String currentYear = YEAR_FORMATTER.format(ZonedDateTime.now());
        private final Validator.ValidatorConfig.Builder validatorConfigBuilder = Validator.ValidatorConfig.builder();
        // values with defaults
        private String yearSeparator = ", ";

        // values that must be provided
        private Path excludesFile;
        private Path templateFile;
        private List<FileMatcher> excludes;

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
            validatorConfig = validatorConfigBuilder
                    .yearSeparator(yearSeparator)
                    .currentYear(currentYear)
                    .build();

            if (templateFile == null) {
                Log.verbose("Parsing default template file (Apache 2).");
                this.templateLines = TemplateLine.parseTemplate(validatorConfig, defaultCopyrightTemplate());
            } else {
                Log.verbose("Parsing template file: " + templateFile.toAbsolutePath());
                this.templateLines = TemplateLine.parseTemplate(validatorConfig, FileSystem.toLines(templateFile));
            }

            excludes = parseExcludes();
            return new Copyright(this);
        }

        /**
         * Update configuration from copyright config (when using Maven).
         *
         * @param config configuration
         * @return updated builder
         */
        public Builder config(CopyrightConfig config) {
            config.excludeFile().map(File::toPath).ifPresent(this::excludesFile);
            config.templateFile().map(File::toPath).ifPresent(this::templateFile);
            config.yearSeparator().ifPresent(this::yearSeparator);
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

        private List<String> defaultCopyrightTemplate() {
            InputStream is = Copyright.class.getResourceAsStream("apache.txt");

            List<String> lines = new LinkedList<>();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                throw new EnforcerException("Failed to read default template from classpath");
            }
            return lines;
        }

        private List<FileMatcher> parseExcludes() {
            if (excludesFile == null) {
                return List.of();
            }

            try {
                Log.verbose("Parsing matches file: " + excludesFile.toAbsolutePath());
                return Files.readAllLines(excludesFile)
                        .stream()
                        .map(String::trim)
                        .filter(it -> !it.startsWith("#"))
                        .filter(it -> !it.isBlank())
                        .map(FileMatcher::create)
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new EnforcerException("Failed to parse excludes file: " + excludesFile, e);
            }
        }
    }
}
