/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.cli.harness;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.cli.harness.CommandModel.FlagInfo;

/**
 * Global option constants.
 */
public class GlobalOptions {

    /**
     * The --help flag name.
     */
    public static final String HELP_FLAG_NAME = "help";

    /**
     * The --help flag description.
     */
    public static final String HELP_FLAG_DESCRIPTION = "Display help information";

    /**
     * The --help flag info.
     */
    public static final FlagInfo HELP_FLAG_INFO = new FlagInfo(HELP_FLAG_NAME, HELP_FLAG_DESCRIPTION, false);

    /**
     * The --version flag name.
     */
    public static final String VERSION_FLAG_NAME = "version";

    /**
     * The --version flag argument.
     */
    public static final String VERSION_FLAG_ARGUMENT = "--" + VERSION_FLAG_NAME;

    /**
     * The --verbose flag name.
     */
    public static final String VERBOSE_FLAG_NAME = "verbose";

    /**
     * The --verbose flag description.
     */
    public static final String VERBOSE_FLAG_DESCRIPTION = "Produce verbose output";

    /**
     * The --verbose flag info.
     */
    public static final FlagInfo VERBOSE_FLAG_INFO = new FlagInfo(VERBOSE_FLAG_NAME, VERBOSE_FLAG_DESCRIPTION, false);

    /**
     * The --verbose flag argument.
     */
    public static final String VERBOSE_FLAG_ARGUMENT = "--" + VERBOSE_FLAG_NAME;

    /**
     * The --debug flag name.
     */
    public static final String DEBUG_FLAG_NAME = "debug";

    /**
     * The --debug flag description.
     */
    public static final String DEBUG_FLAG_DESCRIPTION = "Produce debug output";

    /**
     * The --debug flag info.
     */
    public static final FlagInfo DEBUG_FLAG_INFO = new FlagInfo(DEBUG_FLAG_NAME, DEBUG_FLAG_DESCRIPTION, false);

    /**
     * The --debug flag argument.
     */
    public static final String DEBUG_FLAG_ARGUMENT = "--" + DEBUG_FLAG_NAME;

    /**
     * The --error flag name.
     */
    public static final String ERROR_FLAG_NAME = "error";

    /**
     * The --error flag description.
     */
    public static final String ERROR_FLAG_DESCRIPTION = "Print error stack traces";

    /**
     * The --error flag info.
     */
    public static final FlagInfo ERROR_FLAG_INFO = new FlagInfo(ERROR_FLAG_NAME, ERROR_FLAG_DESCRIPTION, false);

    /**
     * The --error flag argument.
     */
    public static final String ERROR_FLAG_ARGUMENT = "--" + ERROR_FLAG_NAME;

    /**
     * The --plain flag name.
     */
    public static final String PLAIN_FLAG_NAME = "plain";

    /**
     * The --plain flag description.
     */
    public static final String PLAIN_FLAG_DESCRIPTION = "Do not use color or styles in output";

    /**
     * The --plain flag info.
     */
    public static final FlagInfo PLAIN_FLAG_INFO = new FlagInfo(PLAIN_FLAG_NAME, PLAIN_FLAG_DESCRIPTION, false);

    /**
     * The --plain flag argument.
     */
    public static final String PLAIN_FLAG_ARGUMENT = "--" + PLAIN_FLAG_NAME;

    /**
     * Tests whether the given argument is a global flag.
     *
     * @param argument The argument.
     * @return {@code true} if a global flag.
     */
    public static boolean isGlobalFlag(String argument) {
        return GLOBAL_FLAG_ARGUMENTS.contains(argument);
    }

    /**
     * Global flags.
     */
    static final FlagInfo[] GLOBAL_FLAGS = new FlagInfo[]{
            HELP_FLAG_INFO,
            VERBOSE_FLAG_INFO,
            DEBUG_FLAG_INFO,
            ERROR_FLAG_INFO,
            PLAIN_FLAG_INFO
    };

    private static final Set<String> GLOBAL_FLAG_ARGUMENTS = Stream.of(GLOBAL_FLAGS)
                                                                   .map(f -> "--" + f.name())
                                                                   .collect(Collectors.toSet());

    private GlobalOptions() {
    }
}
