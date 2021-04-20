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
import java.nio.file.Paths;
import java.util.List;

import io.helidon.build.maven.enforcer.FileFinder;
import io.helidon.build.maven.enforcer.RuleFailure;
import io.helidon.build.util.Log;
import io.helidon.build.util.SystemLogWriter;

/**
 * Main class for copyright checking. The path to check must be a git repository (or a path within a git repository),
 * otherwise you can only use mode that scans all files and ignores SCM ({@code -a -G}).
 *
 * This file will use Java Util Logging, so verbosity can be controlled through it.
 * <p>
 * Usage: {@code java -jar this-library.jar [options] directory?}
 * <p>
 * Options:
 * <ul>
 *     <li>{@code -C file.txt} - path to copyright template with YYYY placeholder for line containing licensor and copyright
 *     year</li>
 *     <li>{@code -X file.txt} - path to excludes file, defaults to no excludes</li>
 *     <li>{@code -Y "-"} - year separator, defaults to {@code ", "} - quotes are stripped</li>
 *     <li>{@code -G} - do not use git</li>
 *     <li>{@code -v} - verbose output</li>
 *     <li>{@code -d} - debug output</li>
 *     <li>directory - if not defined, current directory is used</li>
 * </ul>
 *
 * Example template file (very simple):
 * <code><pre>
 * Copyright (c) YYYY Oracle and/or its affiliates.
 *
 * This program is made available under the Apache 2.0 license.
 * </pre></code>
 */
public final class Main {
    private Main() {
    }

    /**
     * Invoke the copyright check.
     *
     * @param args options as documented on {@link Main}
     */
    public static void main(String[] args) {
        SystemLogWriter.install(Log.Level.INFO);

        FileFinder.Builder filesBuilder = FileFinder.builder();
        Copyright.Builder copyrightBuilder = Copyright.builder();
        boolean verbose = false;
        boolean debug = false;
        Path baseDirectory = Paths.get(".").toAbsolutePath();

        for (int optionIndex = 0; optionIndex < args.length; optionIndex++) {
            String option = args[optionIndex];

            switch (option) {
            case "-C":
                // copyright template
                String templatePath = nextArg("-C", ++optionIndex, args, "Copyright template path");
                copyrightBuilder.templateFile(Paths.get(templatePath));
                break;
            case "-X":
                // matches file
                String excludeFile = nextArg("-X", ++optionIndex, args, "Copyright matches file path");
                copyrightBuilder.excludesFile(Paths.get(excludeFile));
                break;
            case "-Y":
                // year separator
                String yearSeparator = nextArg("-Y", ++optionIndex, args, "year separator");
                copyrightBuilder.yearSeparator(yearSeparator);
                break;
            case "-G":
                filesBuilder.useGit(false);
                break;
            case "-v":
                verbose = true;
                break;
            case "-d":
                debug = true;
                break;
            case "--help":
            case "-h":
            case "/?":
                help();
                return;
            default:
                if (option.startsWith("-")) {
                    // unknown option
                    unknownOption(option);
                }
                // path (but only if last option)
                if (optionIndex == args.length - 1) {
                    baseDirectory = Paths.get(option).toAbsolutePath();
                } else {
                    unknownOption(option);
                }
                break;
            }
        }

        if (debug) {
            SystemLogWriter.install(Log.Level.DEBUG);
        } else if (verbose) {
            SystemLogWriter.install(Log.Level.VERBOSE);
        }

        Copyright copyright = copyrightBuilder.build();

        List<RuleFailure> errors = copyright.check(filesBuilder.build().findFiles(baseDirectory));
        if (errors.isEmpty()) {
            Log.info("Copyright OK");
            return;
        }

        Log.error("Copyright failures (" + errors.size() + "):");
        for (RuleFailure failure : errors) {
            Log.error(failure.fr().relativePath() + ":" + failure.line() + " " + failure.message());
        }

        System.exit(147);
    }

    private static void unknownOption(String option) {
        System.err.println("Unknown option " + option);
        help();
        System.exit(12);
    }

    private static void help() {
        System.out.println("Usage: copyright [options] directory?");
        System.out.println("Options:");
        helpOption("-C", "path", "Copyright template file path");
        helpOption("-X", "path", "Copyright excludes file path");
        helpOption("-Y", "text", "Year separator in copyright statement");
        helpOption("-G", "Do not use git, only with -a");
        helpOption("-d", "Enable debug output");
        helpOption("-v", "Enable verbose output (less than debug)");
        helpOption("-h", "Print this help information");
    }

    private static void helpOption(String flag, String description) {
        System.out.println("  " + flag + "        " + description);
    }

    private static void helpOption(String flag, String value, String description) {
        String filler = " ".repeat(7 - value.length());
        System.out.println("  " + flag + " " + value + filler + description);
    }

    private static String nextArg(String flag, int index, String[] args, String description) {
        if (args.length <= index) {
            System.err.println("Option " + flag + " requires a value: " + description);
            System.exit(13);
        }
        return args[index];
    }
}
