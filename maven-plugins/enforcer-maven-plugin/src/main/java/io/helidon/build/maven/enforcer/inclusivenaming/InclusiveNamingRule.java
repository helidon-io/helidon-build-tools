/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.build.maven.enforcer.inclusivenaming;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import io.helidon.build.common.logging.Log;
import io.helidon.build.maven.enforcer.EnforcerException;
import io.helidon.build.maven.enforcer.FileMatcher;
import io.helidon.build.maven.enforcer.FileRequest;
import io.helidon.build.maven.enforcer.FoundFiles;
import io.helidon.build.maven.enforcer.RuleFailure;

/**
 * Rule for Inclusive Naming checking.
 */
public class InclusiveNamingRule {
    private static final String INCLUSIVE_NAMING_XML = "inclusive-names.xml";
    private static final Set<String> ALLOWED_TIERS = Set.of("0");
    private static final Set<String> DEFAULT_INCLUDES = Set.of(".java", ".xml", ".adoc",
                                                               ".txt", ".md", ".html",
                                                               ".css", ".properties", ".yaml",
                                                               ".sh", ".sql", ".conf",
                                                               ".json", ".kt", ".groovy",
                                                               ".mustache", ".yml", ".graphql",
                                                               ".proto", "Dockerfile.native", "Dockerfile.jlink",
                                                               ".gradle", ".MF");
    private final List<FileMatcher> excludes;
    private final List<FileMatcher> includes;
    private final List<XmlData> terms;

    private InclusiveNamingRule(Builder builder) {
        this.excludes = builder.excludes();
        this.includes = builder.includes();
        this.terms = builder.terms();
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
            Log.info("Inclusive naming processed " + count.get() + " files.");
        } else {
            Log.info("Inclusive naming processed " + count.get() + " files, found " + failures.size() + " errors");
        }
        return failures;
    }

    private void processFile(FileRequest fr, List<RuleFailure> errors) {
        try (BufferedReader bufferedReader = Files.newBufferedReader(fr.path(), StandardCharsets.UTF_8)) {
            String line;
            int lineNum = 0;
            List<XmlData> found = new LinkedList<>();
            while ((line = bufferedReader.readLine()) != null) {
                // we want to start from 1
                lineNum++;
                for (XmlData term : terms) {
                    if (line.toLowerCase().contains(term.getTerm())) {
                        found.add(term);
                    }
                }
                if (!found.isEmpty()) {
                    errors.add(RuleFailure.create(fr, lineNum, "inclusive naming found: " + String.join(", ", found.toString())));
                }
                found.clear();
            }

        } catch (IOException e) {
            throw new EnforcerException("Failed to process file for inclusive naming: " + fr.relativePath());
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
     * Fluent API builder for {@link io.helidon.build.maven.enforcer.inclusivenaming.InclusiveNamingRule}.
     */
    public static class Builder {
        private InlusiveNamingConfig inclusiveNamingConfig;

        private Builder() {
        }

        /**
         * Update builder from maven configuration.
         *
         * @param inclusiveNamingConfig maven config
         * @return updated builder
         */
        public Builder config(InlusiveNamingConfig inclusiveNamingConfig) {
            this.inclusiveNamingConfig = inclusiveNamingConfig;
            return this;
        }

        /**
         * Build a new instance of {@link io.helidon.build.maven.enforcer.inclusivenaming.InclusiveNamingRule}.
         *
         * @return new rule
         */
        public InclusiveNamingRule build() {
            return new InclusiveNamingRule(this);
        }

        private List<XmlData> readTerms(Supplier<InputStream> supplier){
            List<XmlData> terms = new ArrayList<>();
            try (InputStream is = supplier.get()) {
                XmlInclusiveNaming xml = xmlInclusiveNaming(is);
                for (XmlData data : xml.getData()) {
                    String tier = data.getTier();
                    String term = data.getTerm();
                    if (!ALLOWED_TIERS.contains(tier)) {
                        boolean exclude = false;
                        for (Pattern pattern : inclusiveNamingConfig.excludeTermsRegExps()) {
                            if (pattern.matcher(term).matches()) {
                                exclude = true;
                                break;
                            }
                        }
                        if (!exclude) {
                            terms.add(data);
                        }
                    }
                }
            } catch (IOException | JAXBException e) {
                throw new EnforcerException("Failed to read inclusive naming file", e);
            }
            terms.addAll(inclusiveNamingConfig.additionalTerms());
            return terms;
        }

        private XmlInclusiveNaming xmlInclusiveNaming(InputStream is) throws JAXBException {
            JAXBContext contextObj = JAXBContext.newInstance(XmlInclusiveNaming.class);
            Unmarshaller unmarshaller = contextObj.createUnmarshaller();
            return (XmlInclusiveNaming) unmarshaller.unmarshal(is);
        }

        List<XmlData> terms() {
            if (inclusiveNamingConfig.inclusiveNamingFile().isPresent()) {
                return readTerms(() -> {
                    try {
                        return new FileInputStream(inclusiveNamingConfig.inclusiveNamingFile().get());
                    } catch (FileNotFoundException e) {
                        throw new EnforcerException(inclusiveNamingConfig.inclusiveNamingFile().get() + " was not found");
                    }
                });
            } else {
                return readTerms(() -> InclusiveNamingRule.class.getResourceAsStream(INCLUSIVE_NAMING_XML));
            }
        }

        List<FileMatcher> excludes() {
            return inclusiveNamingConfig.excludes()
                    .stream()
                    .map(FileMatcher::create)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }

        List<FileMatcher> includes() {
            Set<String> includes = new HashSet<>(DEFAULT_INCLUDES);
            includes.addAll(inclusiveNamingConfig.includes());
            return includes.stream()
                    .map(FileMatcher::create)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }
    }

}
