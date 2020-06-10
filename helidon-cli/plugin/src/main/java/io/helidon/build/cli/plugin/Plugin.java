/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
            if (args.length > 0) {
                final Plugin plugin = Plugin.newInstance(args[0]);
                plugin.parse(args).execute();
            }
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    /**
     * Returns an new instance from the given name, where the class name is constructed by
     * prepending this package name.
     *
     * @param simpleClassName The unqualified plugin class name.
     * @return The instance.
     * @throws Exception If an error occurs.
     */
    public static Plugin newInstance(String simpleClassName) throws Exception {
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
     * @param allArgs All arguments.
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
     * Parse the arguments.
     *
     * @param args The arguments.
     * @return This instance, for chaining.
     */
    Plugin parse(String[] args) {
        for (int index = 1; index < args.length; index++) {
            final String arg = args[index];
            if (arg.equalsIgnoreCase("--verbose")) {
                Log.verbosity(Log.Verbosity.VERBOSE);
            } else if (arg.equalsIgnoreCase("--debug")) {
                Log.verbosity(Log.Verbosity.DEBUG);
            } else {
                index = parseArg(arg, index, args);
            }
        }
        return this;
    }

    /**
     * Parse an argument.
     *
     * @param arg The argument.
     * @param argIndex The argument index.
     * @param allArgs All arguments.
     * @return The new index.
     */
    int parseArg(String arg, int argIndex, String[] allArgs) {
        throw new IllegalArgumentException("unknown arg: " + arg);
    }

    /**
     * Execute the plugin.
     */
    abstract void execute();
}
