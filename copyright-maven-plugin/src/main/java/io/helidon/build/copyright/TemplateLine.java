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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.helidon.build.util.Log;

import static io.helidon.build.copyright.Copyright.logBad;
import static io.helidon.build.copyright.Copyright.logGood;

/**
 * Represents a single line in a template to easily validate files.
 * The line may be blank, text, or copyright line with year information
 */
public abstract class TemplateLine {
    /**
     * Validate an actual line against this template line.
     *
     * @param actualLine line to validate
     * @param expectedYear expected year in copyright (unless validating only format)
     * @param lineNumber line number within the copyright comment
     * @return problem with copyright if discovered, empty if OK
     */
    public abstract Optional<String> validate(String actualLine, String expectedYear, int lineNumber);

    /**
     * Parse template lines. If the template does not contain a line with year, it will be added as
     * the first line.
     *
     * @param config validator configuration
     * @param lines lines of the copyright template
     * @return template lines
     */
    public static List<TemplateLine> parseTemplate(Validator.ValidatorConfig config, List<String> lines) {
        List<TemplateLine> templateLines = new ArrayList<>(lines.size() + 1);

        boolean hasCopyright = false;

        for (String line : lines) {
            if (line.contains("YYYY")) {
                templateLines.add(new CopyrightLine(config, line));
                hasCopyright = true;
                continue;
            }
            if (line.isBlank()) {
                templateLines.add(new BlankLine());
                continue;
            }

            templateLines.add(new TextLine(line));
        }

        if (!hasCopyright) {
            templateLines.add(0, new CopyrightLine(config));
        }

        return templateLines;
    }

    private static class TextLine extends TemplateLine {
        private final String line;

        private TextLine(String line) {
            this.line = line;
        }

        @Override
        public Optional<String> validate(String actualLine, String expectedYear, int lineNumber) {
            if (line.equals(actualLine)) {
                return Optional.empty();
            }

            return Optional.of(lineNumber + ": Invalid copyright line. Expected / actual. \n" + line + "\n" + actualLine);
        }
    }

    private static class BlankLine extends TemplateLine {
        private BlankLine() {
        }

        @Override
        public Optional<String> validate(String actualLine, String expectedYear, int lineNumber) {
            if (actualLine.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(lineNumber + ": Expected empty line, but got \"" + actualLine + "\"");
        }
    }

    private static class CopyrightLine extends TemplateLine {
        private final String prefix;
        private final String licensor;
        private final String suffix;
        private final String yearSeparator;
        private final String expectedLine;
        private final String logExpectedLine;

        private final int minLength;
        private final int prefixLength;
        private final int suffixLength;
        private final int licensorLength;
        private final int yearSeparatorLength;
        private final boolean checkFormatOnly;

        private CopyrightLine(CopyrightLineSetup setup) {
            this.prefix = setup.prefix;
            this.licensor = setup.licensor;
            this.suffix = setup.suffix;
            this.yearSeparator = setup.yearSeparator;
            this.expectedLine = prefix + "YYYY[" + yearSeparator + "YYYY] " + licensor + suffix;
            this.logExpectedLine = logGood(expectedLine);
            this.checkFormatOnly = setup.checkFormatOnly;

            this.prefixLength = prefix.length();
            this.licensorLength = licensor.length();
            this.suffixLength = suffix.length();
            this.yearSeparatorLength = yearSeparator.length();
            this.minLength = prefixLength + licensorLength + suffixLength + 5;
        }

        CopyrightLine(Validator.ValidatorConfig config) {
            this(new CopyrightLineSetup("Copyright (c) ",
                                        config.licensor(),
                                        ".",
                                        config.yearSeparator(),
                                        config.checkFormatOnly()));
        }

        CopyrightLine(Validator.ValidatorConfig config, String line) {
            this(parse(config, line));
        }

        private static CopyrightLineSetup parse(Validator.ValidatorConfig config, String line) {
            // line in template
            // Copyright (c) YYYY Oracle and/or its affiliates.
            int yyyy = line.indexOf("YYYY");

            int dot = line.indexOf(".", yyyy);
            if (dot < 0) {
                dot = line.length();
            }

            return new CopyrightLineSetup(
                    line.substring(0, yyyy),
                    line.substring(yyyy + 5, dot),
                    line.substring(dot),
                    config.yearSeparator(),
                    config.checkFormatOnly());
        }

        @Override
        public Optional<String> validate(String actualLine, String expectedYear, int lineNumber) {
            try {
                doValidate(actualLine, expectedYear);
                return Optional.empty();
            } catch (CopyrightException e) {
                return Optional.of(lineNumber + ": " + e.getMessage());
            }
        }

        private void doValidate(String actualLine, String expectedYear) {
            //Copyright (c) 2019, 2021 Oracle and/or its affiliates.
            //Copyright (c) 2021 Oracle and/or its affiliates.
            if (actualLine.length() < minLength) {
                throw new CopyrightException("line is too short: " + logBad(actualLine) + ", expected: " + logExpectedLine);
            }

            int lineLength = actualLine.length();

            String actualPrefix = actualLine.substring(0, prefixLength);
            int suffixStart = lineLength - suffixLength;
            String actualSuffix = actualLine.substring(suffixStart, lineLength);
            int licensorStart = suffixStart - licensorLength;
            String actualLicensor = actualLine.substring(licensorStart, suffixStart);
            String yearSection = actualLine.substring(prefixLength, licensorStart).trim();

            // start validation
            if (!actualPrefix.equals(prefix)) {
                throw new CopyrightException("line starts with "
                                                     + logBad(actualPrefix)
                                                     + ", but its format should be "
                                                     + logExpectedLine);
            }

            if (!actualSuffix.equals(suffix)) {
                throw new CopyrightException("line ends with "
                                                     + logBad(actualSuffix)
                                                     + ", but its format should be "
                                                     + logExpectedLine);
            }

            if (!actualLicensor.equals(licensor)) {
                throw new CopyrightException("wrong format. Is "
                                                     + logBad(actualLine)
                                                     + ", should be "
                                                     + logExpectedLine);
            }

            if (yearSection.length() == 4) {
                // this is the year - make sure all numbers

                validateYearFormat(actualLine, yearSection);

                if (!checkFormatOnly) {
                    Log.debug("Expected year: " + expectedYear + ", actual year: " + yearSection);
                    validateYear(yearSection, expectedYear);
                }
            } else {
                if (yearSection.length() == (8 + yearSeparatorLength)) {
                    // expected length
                    String firstYear = yearSection.substring(0, 4);
                    String actualSeparator = yearSection.substring(4, 4 + yearSeparatorLength);
                    String secondYear = yearSection.substring(4 + yearSeparatorLength);
                    validateYearFormat(actualLine, firstYear);
                    validateYearFormat(actualLine, secondYear);
                    if (!actualSeparator.equals(this.yearSeparator)) {
                        throw new CopyrightException("Year separator is "
                                                             + logBad(actualSeparator)
                                                             + ", but should be "
                                                             + logGood(yearSeparator)
                                                             + " on line " + logBad(actualLine));
                    }
                    if (!checkFormatOnly) {
                        Log.debug("Expected year: " + expectedYear + ", actual years: " + yearSection);
                        validateYear(firstYear, secondYear, expectedYear);
                    }
                } else {
                    throw new CopyrightException("copyright year is "
                                                         + logBad(yearSection)
                                                         + ", but should be "
                                                         + logGood("yyyy" + yearSeparator + expectedYear));
                }
            }
        }

        private void validateYear(String actualFrom, String actualTo, String expectedYear) {
            if (!actualTo.equals(expectedYear)) {
                throw new CopyrightException("copyright year is "
                                                     + logBad(actualFrom + yearSeparator + actualTo)
                                                     + ", but should be "
                                                     + logGood(actualFrom + yearSeparator + expectedYear));
            }
        }

        private void validateYear(String actualYear, String expectedYear) {
            if (!actualYear.equals(expectedYear)) {
                throw new CopyrightException("copyright year is "
                                                     + logBad(actualYear)
                                                     + ", but should be "
                                                     + logGood(actualYear + yearSeparator + expectedYear));
            }
        }

        void validateYearFormat(String line, String year) {
            try {
                Integer.parseInt(year);
            } catch (NumberFormatException e) {
                throw new CopyrightException("year should be 4 digits, but is "
                                                     + logBad(year)
                                                     + ", on line "
                                                     + logBad(line));
            }
        }

        private static class CopyrightLineSetup {
            private final String prefix;
            private final String licensor;
            private final String suffix;
            private final String yearSeparator;
            private final boolean checkFormatOnly;

            private CopyrightLineSetup(String prefix,
                                       String licensor,
                                       String suffix,
                                       String yearSeparator,
                                       boolean checkFormatOnly) {
                this.prefix = prefix;
                this.licensor = licensor;
                this.suffix = suffix;
                this.yearSeparator = yearSeparator;
                this.checkFormatOnly = checkFormatOnly;
            }
        }
    }
}
