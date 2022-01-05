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
package io.helidon.build.cli.plugin;

import java.util.function.Consumer;

/**
 * An abstract CLI plugin.
 */
public abstract class Plugin {

    /**
     * Main entry point.
     *
     * @param args The arguments
     */
    public static void main(String[] args) {
        try {
            execute(args, System.out::println);
        } catch (IllegalArgumentException | Failed e) {
            fail(e.getMessage());
        } catch (Throwable e) {
            fail(e.toString());
        }
    }

    /**
     * Execute the plugin without the system exit.
     *
     * @param args The arguments
     * @param logConsumer The log output consumer.
     * @throws Exception if an error occurs
     */
    public static void execute(String[] args, Consumer<String> logConsumer) throws Exception {
        if (args.length > 0) {
            Log.output(logConsumer);
            final Plugin plugin = Plugin.newInstance(args[0]);
            plugin.parse(args).execute();
        }
    }

    /**
     * Exception that is always logged with only the message.
     */
    public static class Failed extends Exception {
        private final String message;

        /**
         * Constructor.
         *
         * @param message The message.
         */
        public Failed(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }

    /**
     * Returns a new instance from the given name, where the class name is constructed by
     * prepending this package name.
     *
     * @param simpleClassName The unqualified plugin class name.
     * @return The instance.
     * @throws Exception If an error occurs.
     */
    static Plugin newInstance(String simpleClassName) throws Exception {
        final String className = Plugin.class.getPackageName() + "." + simpleClassName;
        return (Plugin) Class.forName(className).getDeclaredConstructor().newInstance();
    }

    /**
     * Log an error message and exit the process with status 1.
     *
     * @param message The message.
     */
    static void fail(String message) {
        Log.error(message);
        System.exit(1);
    }

    /**
     * Returns the argument after the current index or fails if missing.
     *
     * @param currentIndex The current index.
     * @param allArgs      All arguments.
     * @return The next argument.
     */
    static String nextArg(int currentIndex, String[] allArgs) {
        final int nextIndex = currentIndex + 1;
        if (nextIndex < allArgs.length) {
            return allArgs[nextIndex];
        } else {
            throw new IllegalArgumentException(allArgs[currentIndex] + ": missing required argument");
        }
    }

    /**
     * Assert that the given argument is valid.
     *
     * @param argName  The argument name.
     * @param argument The argument.
     * @throws IllegalArgumentException If argument is {@code null}
     */
    static void assertRequiredArg(String argName, Object argument) {
        if (argument == null) {
            missingRequiredArg(argName);
        }
    }

    /**
     * Fail with a missing argument exception.
     *
     * @param argName The argument name.
     * @throws IllegalArgumentException always.
     */
    static void missingRequiredArg(String argName) {
        throw new IllegalArgumentException("missing required argument: " + argName);
    }

    /**
     * Parse the arguments.
     *
     * @param args The arguments.
     * @return This instance, for chaining.
     * @throws Exception if an error occurs.
     */
    Plugin parse(String[] args) throws Exception {
        for (int index = 1; index < args.length; index++) {
            final String arg = args[index];
            if (arg.equalsIgnoreCase("--verbose")) {
                Log.verbosity(Log.Verbosity.VERBOSE);
            } else if (arg.equalsIgnoreCase("--debug")) {
                Log.verbosity(Log.Verbosity.DEBUG);
            } else {
                index = parseArg(arg, index, args);
                if (index < 0) {
                    throw new IllegalArgumentException("unknown argument: " + arg);
                }
            }
        }
        validateArgs();
        return this;
    }

    /**
     * Parse an argument.
     *
     * @param arg      The argument.
     * @param argIndex The argument index.
     * @param allArgs  All arguments.
     * @return The new index, or -1 if an unknown argument.
     * @throws Exception if an error occurs.
     */
    int parseArg(String arg, int argIndex, String[] allArgs) throws Exception {
        return -1;
    }

    /**
     * Validate arguments.
     *
     * @throws Exception If an exception occurs.
     */
    abstract void validateArgs() throws Exception;

    /**
     * Execute the plugin.
     *
     * @throws Exception if an error occurs.
     */
    abstract void execute() throws Exception;
}
