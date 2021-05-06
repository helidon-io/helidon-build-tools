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
package io.helidon.build.maven.enforcer.copyright;

import java.nio.file.Path;
import java.util.Set;

import io.helidon.build.maven.enforcer.FileRequest;
import io.helidon.build.maven.enforcer.RuleFailureException;

/**
 * A validator for a certain type of files.
 */
public interface Validator {
    /**
     * Return a set of suffixes this validator supports. If a suffix is supported, no further checks are done.
     *
     * @return set of suffixes including the {@code .}, such as {@code .java}
     * @see #supports(java.nio.file.Path)
     */
    Set<String> supportedSuffixes();

    /**
     * Return {@code true} if this validator supports the provided path. This may be used for more
     * complex analysis of the file content when a suffix is not sufficient.
     *
     * @param path path of the file
     * @return {@code true} if this validator can process the file
     */
    default boolean supports(Path path) {
        return false;
    }

    /**
     * Validate copyright of the file.
     *
     *
     * @param file the file request
     * @param path path of the file
     * @throws io.helidon.build.maven.enforcer.RuleFailureException (such as copyright year should be 2019, 2021, but was 2019)
     */
    void validate(FileRequest file, Path path) throws RuleFailureException;

    /**
     * Validator configuration.
     */
    interface ValidatorConfig {
        /**
         * A new builder.
         * @return new fluent API builder
         */
        static Builder builder() {
            return new Builder();
        }

        /**
         * Separator to use between years in copyright (when using 2).
         *
         * @return year separator
         */
        String yearSeparator();

        /**
         * Validator should only check format, not validity of years.
         *
         * @return whether to check only format
         */
        boolean checkFormatOnly();

        /**
         * Expected licensor (the company) in copyright header.
         *
         * @return name of the expected licensor
         */
        String licensor();

        /**
         * Current year.
         *
         * @return this year
         */
        String currentYear();

        /**
         * Fluent API builder to build {@link Validator.ValidatorConfig}.
         */
        class Builder {
            private String yearSeparator = ", ";
            private boolean checkFormatOnly;
            private String defaultLicensor = "Oracle and/or its affiliates";
            private String currentYear;

            private Builder() {
            }

            public ValidatorConfig build() {
                return new ValidatorConfigImpl(this);
            }

            /**
             * String to separate years in copyright header.
             *
             * @param yearSeparator separator to use, such as {@code ", "}
             * @return updated builder
             */
            public Builder yearSeparator(String yearSeparator) {
                this.yearSeparator = yearSeparator;
                return this;
            }

            /**
             * Whether to check only format and ignore last modified year.
             *
             * @param checkFormatOnly {@code true} to only check format
             * @return updated builder
             */
            public Builder checkFormatOnly(boolean checkFormatOnly) {
                this.checkFormatOnly = checkFormatOnly;
                return this;
            }

            /**
             * Configure the default licensor.
             *
             * @param defaultLicensor default licensor in copyright header
             * @return updated builder
             */
            public Builder defaultLicensor(String defaultLicensor) {
                this.defaultLicensor = defaultLicensor;
                return this;
            }

            public Builder currentYear(String currentYear) {
                this.currentYear = currentYear;
                return this;
            }

            private static class ValidatorConfigImpl implements ValidatorConfig {
                private final String yearSeparator;
                private final boolean checkFormatOnly;
                private final String licensor;
                private final String currentYear;

                private ValidatorConfigImpl(Builder builder) {
                    this.yearSeparator = builder.yearSeparator;
                    this.checkFormatOnly = builder.checkFormatOnly;
                    this.licensor = builder.defaultLicensor;
                    this.currentYear = builder.currentYear;
                }

                @Override
                public String yearSeparator() {
                    return yearSeparator;
                }

                @Override
                public boolean checkFormatOnly() {
                    return checkFormatOnly;
                }

                @Override
                public String licensor() {
                    return licensor;
                }

                @Override
                public String currentYear() {
                    return currentYear;
                }
            }
        }
    }
}
