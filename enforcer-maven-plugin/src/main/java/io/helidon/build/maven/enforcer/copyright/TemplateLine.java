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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import io.helidon.build.common.Log;
import io.helidon.build.maven.enforcer.FileRequest;
import io.helidon.build.maven.enforcer.RuleFailureException;

import static io.helidon.build.maven.enforcer.copyright.Copyright.logBad;
import static io.helidon.build.maven.enforcer.copyright.Copyright.logGood;

/**
 * Represents a single line in a template to easily validate files.
 * The line may be blank, text, or copyright line with year information
 */
public abstract class TemplateLine {
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

    /**
     * Validate an actual line against this template line.
     *
     *
     * @param file file request
     * @param actualLine line to validate
     * @param lineNumber line number within the copyright comment
     * @throws io.helidon.build.maven.enforcer.RuleFailureException for validation failures
     */
    public abstract void validate(FileRequest file,
                                  String actualLine,
                                  int lineNumber) throws RuleFailureException;

    private static class TextLine extends TemplateLine {
        private final String line;

        private TextLine(String line) {
            this.line = line;
        }

        @Override
        public void validate(FileRequest file,
                             String actualLine,
                             int lineNumber) {
            if (line.equals(actualLine)) {
                return;
            }

            throw new RuleFailureException(file,
                                           lineNumber,
                                           "Invalid line. Expected " + logGood(line)
                                                   + ", is " + logBad(actualLine));
        }
    }

    private static class BlankLine extends TemplateLine {
        private BlankLine() {
        }

        @Override
        public void validate(FileRequest file,
                             String actualLine,
                             int lineNumber) {
            if (actualLine.isBlank()) {
                return;
            }
            throw new RuleFailureException(file,
                                           lineNumber,
                                           "Expected empty line, is " + logBad(actualLine));
        }
    }

    private static class CopyrightLine extends TemplateLine {
        private static final Pattern YEAR = Pattern.compile("\\d\\d\\d\\d ");

        private final String prefix;
        private final String licensor;
        private final String suffix;
        private final String yearSeparator;
        private final String logExpectedLine;

        private final int minLength;
        private final int prefixLength;
        private final int suffixLength;
        private final int licensorLength;
        private final int yearSeparatorLength;
        private final boolean checkFormatOnly;
        private final Pattern yearsPattern;
        private final CopyrightLine backup;
        private final String currentYear;

        private CopyrightLine(CopyrightLineSetup setup, CopyrightLineSetup backup) {
            this.prefix = setup.prefix;
            this.licensor = setup.licensor;
            this.suffix = setup.suffix;
            this.yearSeparator = setup.yearSeparator;
            String expectedLine = prefix + "YYYY[" + yearSeparator + "YYYY] " + licensor + suffix;
            this.logExpectedLine = logGood(expectedLine);
            this.checkFormatOnly = setup.checkFormatOnly;

            this.prefixLength = prefix.length();
            this.licensorLength = licensor.length();
            this.suffixLength = suffix.length();
            this.yearSeparatorLength = yearSeparator.length();
            this.minLength = prefixLength + licensorLength + suffixLength + 5;
            this.yearsPattern = Pattern.compile("\\d\\d\\d\\d" + yearSeparator + "\\d\\d\\d\\d ");
            this.currentYear = setup.currentYear;

            if (setup.licensor.contains("Oracle")) {
                this.backup = (backup == null ? null : new CopyrightLine(backup, null));
            } else {
                this.backup = null;
            }
        }

        CopyrightLine(Validator.ValidatorConfig config) {
            this(new CopyrightLineSetup(config,
                                        "Copyright (c) ",
                                        config.licensor(),
                                        "."),
                 null);
        }

        CopyrightLine(Validator.ValidatorConfig config, String line) {
            this(parse(config, line), parse(config, line + " All rights reserved."));
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
                    config,
                    line.substring(0, yyyy),
                    line.substring(yyyy + 5, dot),
                    line.substring(dot));
        }

        @Override
        public void validate(FileRequest file,
                             String actualLine,
                             int lineNumber) {
            try {
                doValidate(file, actualLine, file.lastModifiedYear(), lineNumber);
            } catch (RuleFailureException e) {
                // for older files, we allow "All rights reserved." suffix
                if (backup != null && !file.lastModifiedYear().equals(currentYear)) {
                    try {
                        backup.validate(file, actualLine, lineNumber);
                    } catch (RuleFailureException ignored) {
                        throw e;
                    }
                } else {
                    // current year - cannot have this
                    throw e;
                }
            }
        }

        private void doValidate(FileRequest file, String actualLine, String expectedYear, int lineNumber) {
            //Copyright (c) 2019, 2021 Oracle and/or its affiliates.
            //Copyright (c) 2021 Oracle and/or its affiliates.
            if (actualLine.length() < minLength) {
                throw new RuleFailureException(file,
                                               lineNumber,
                                               "Line too short. Expected " + logExpectedLine + ", is " + logBad(actualLine));
            }

            int lineLength = actualLine.length();

            // Copyright line:
            //      Copyright (c) 2017, 2021 Oracle and/or its affiliates.

            // "Copyright (c) "
            String actualPrefix = actualLine.substring(0, prefixLength);

            // expected starts
            int suffixStart = lineLength - suffixLength;
            int licensorStart = suffixStart - licensorLength;
            String yearSection;
            // after prefix, we should have the year section - either yyyy or yyyy, yyyy
            if (isYears(actualLine, prefixLength)) {
                // year section without the trailing space
                yearSection = actualLine.substring(prefixLength, prefixLength + 10);
                licensorStart = prefixLength + 11;
            } else if (isYear(actualLine, prefixLength)) {
                // year section without the trailing space
                yearSection = actualLine.substring(prefixLength, prefixLength + 4);
                licensorStart = prefixLength + 5;
            } else {
                yearSection = actualLine.substring(prefixLength, licensorStart).trim();
            }

            String actualSuffix = actualLine.substring(suffixStart, lineLength);
            String actualLicensor = actualLine.substring(licensorStart, suffixStart);

            // start validation
            if (!actualPrefix.equals(prefix)) {
                throw new RuleFailureException(file,
                                               lineNumber,
                                               "Wrong prefix. Expected " + logExpectedLine
                                                       + ", is " + logBad(actualPrefix));
            }

            if (!actualSuffix.equals(suffix)) {
                throw new RuleFailureException(file,
                                               lineNumber,
                                               "Wrong suffix. Expected " + logExpectedLine
                                                       + ", is " + logBad(actualSuffix));
            }

            if (!actualLicensor.equals(licensor)) {
                throw new RuleFailureException(file,
                                               lineNumber,
                                               "Wrong format. Expected "
                                                       + logGood(licensor)
                                                       + ", is "
                                                       + logBad(actualLicensor));
            }

            if (yearSection.length() == 4) {
                // this is the year - make sure all numbers

                validateYearFormat(file, lineNumber, actualLine, yearSection);

                if (!checkFormatOnly) {
                    Log.debug("Expected year: " + expectedYear + ", actual year: " + yearSection);
                    validateYear(file, lineNumber, yearSection, expectedYear);
                }
            } else {
                if (yearSection.length() == (8 + yearSeparatorLength)) {
                    // expected length
                    String firstYear = yearSection.substring(0, 4);
                    String actualSeparator = yearSection.substring(4, 4 + yearSeparatorLength);
                    String secondYear = yearSection.substring(4 + yearSeparatorLength);
                    validateYearFormat(file, lineNumber, actualLine, firstYear);
                    validateYearFormat(file, lineNumber, actualLine, secondYear);
                    if (!actualSeparator.equals(this.yearSeparator)) {
                        throw new RuleFailureException(file,
                                                       lineNumber,
                                                       "Wrong year separator. Expected  "
                                                               + logGood(yearSeparator)
                                                               + ", is "
                                                               + logBad(actualSeparator)
                                                               + ", on line "
                                                               + logBad(actualLine));
                    }
                    if (!checkFormatOnly) {
                        Log.debug("Expected year: " + expectedYear + ", actual years: " + yearSection);
                        validateYear(file, lineNumber, firstYear, secondYear, expectedYear);
                    }
                } else {
                    throw new RuleFailureException(file,
                                                   lineNumber,
                                                   "Wrong year. Expected "
                                                           + logGood("yyyy" + yearSeparator + expectedYear)
                                                           + ", is "
                                                           + logBad(yearSection));
                }
            }
        }

        private boolean isYear(String actualLine, int prefixLength) {
            // 2019
            if (actualLine.length() >= prefixLength + 5) {
                String years = actualLine.substring(prefixLength, prefixLength + 5);
                return YEAR.matcher(years).matches();
            }

            return false;
        }

        private boolean isYears(String actualLine, int prefixLength) {
            // 2019, 2021
            if (actualLine.length() >= prefixLength + 11) {
                String years = actualLine.substring(prefixLength, prefixLength + 11);
                return yearsPattern.matcher(years).matches();
            }

            return false;
        }

        private void validateYear(FileRequest file, int lineNumber, String actualFrom, String actualTo, String expectedYear) {
            if (!actualTo.equals(expectedYear)) {
                throw new RuleFailureException(file,
                                               lineNumber,
                                               "Wrong year. Expected "
                                                       + logGood(actualFrom + yearSeparator + expectedYear)
                                                       + ", is "
                                                       + logBad(actualFrom + yearSeparator + actualTo));
            }
        }

        private void validateYear(FileRequest file, int lineNumber, String actualYear, String expectedYear) {
            if (!actualYear.equals(expectedYear)) {
                throw new RuleFailureException(file,
                                               lineNumber,
                                               "Wrong year. Expected "
                                                       + logGood(actualYear + yearSeparator + expectedYear)
                                                       + ", is "
                                                       + logBad(actualYear));
            }
        }

        void validateYearFormat(FileRequest file, int lineNumber, String line, String year) {
            try {
                Integer.parseInt(year);
            } catch (NumberFormatException e) {
                throw new RuleFailureException(file,
                                               lineNumber,
                                               "Wrong year format. Expected "
                                                       + logGood("4 digits")
                                                       + ", is "
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
            private final String currentYear;

            private CopyrightLineSetup(Validator.ValidatorConfig config,
                                       String prefix,
                                       String licensor,
                                       String suffix) {
                this.prefix = prefix;
                this.licensor = licensor;
                this.suffix = suffix;
                this.yearSeparator = config.yearSeparator();
                this.checkFormatOnly = config.checkFormatOnly();
                this.currentYear = config.currentYear();
            }
        }
    }
}
