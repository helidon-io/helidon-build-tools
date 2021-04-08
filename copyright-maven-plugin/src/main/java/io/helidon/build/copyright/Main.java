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

import java.nio.file.Paths;
import java.util.List;

import io.helidon.build.common.Log;
import io.helidon.build.common.Log.Level;
import io.helidon.build.common.SystemLogWriter;

/**
 * Main class for copyright checking. The path to check must be a git repository (or a path within a git repository),
 * otherwise you can only use mode that scans all files and ignores SCM ({@code -y -a}).
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
 *     <li>{@code -b master} - name of the git branch to find changes from, defaults to {@code master}</li>
 *     <li>{@code -Y "-"} - year separator, defaults to {@code ", "} - quotes are stripped</li>
 *     <li>{@code -y} - check format only (ignore last modification timestamp), by default the copyright year is checked</li>
 *     <li>{@code -a} - check all files ignoring if modified, by default only modified files are checked</li>
 *     <li>{@code -S} - check all files, even files not under source control, by default only files known by git are checked</li>
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
     * @param args options as documented on {@link io.helidon.build.copyright.Main}
     */
    public static void main(String[] args) {
        Log.writer(SystemLogWriter.create(Level.INFO));

        Copyright.Builder builder = Copyright.builder();
        boolean verbose = false;
        boolean debug = false;
        builder.path(Paths.get(".").toAbsolutePath());

        for (int optionIndex = 0; optionIndex < args.length; optionIndex++) {
            String option = args[optionIndex];

            switch (option) {
            case "-C":
                // copyright template
                String templatePath = nextArg("-C", ++optionIndex, args, "Copyright template path");
                builder.templateFile(Paths.get(templatePath));
                break;
            case "-X":
                // exclude file
                String excludeFile = nextArg("-X", ++optionIndex, args, "Copyright exclude file path");
                builder.excludesFile(Paths.get(excludeFile));
                break;
            case "-b":
                // branch name
                String branchName = nextArg("-b", ++optionIndex, args, "git branch name");
                builder.masterBranch(branchName);
                break;
            case "-Y":
                // year separator
                String yearSeparator = nextArg("-Y", ++optionIndex, args, "year separator");
                builder.yearSeparator(yearSeparator);
                break;
            case "-S":
                builder.scmOnly(false);
                break;
            case "-y":
                builder.checkFormatOnly(true);
                break;
            case "-a":
                builder.checkAll(true);
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
                    builder.path(Paths.get(option).toAbsolutePath());
                } else {
                    unknownOption(option);
                }
                break;
            }
        }

        if (debug) {
            Log.writer(SystemLogWriter.create(Level.DEBUG));
        } else if (verbose) {
            Log.writer(SystemLogWriter.create(Level.VERBOSE));
        }

        Copyright copyright = builder.build();

        List<String> errors = copyright.check();
        if (errors.isEmpty()) {
            Log.info("Copyright OK");
            return;
        }

        Log.error("Copyright failures:");
        for (String error : errors) {
            Log.error(error);
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
        helpOption("-b", "branch", "Git branch to find changed since");
        helpOption("-Y", "text", "Year separator in copyright statement");
        helpOption("-S", "Check all files, not just Git tracked, only with -a");
        helpOption("-y", "Only check format, ignore last modified year");
        helpOption("-a", "Check all files, not just modified ones");
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
