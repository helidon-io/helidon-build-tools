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
 * Utility to convert strings to the {@code StyleRenderer} DSL. Assumes output will be rendered.
 */
public class Style {
    private static final String STYLE_PREFIX = "$(";
    private static final String STYLE_SUFFIX = ")";
    private static final String ESCAPED_STYLE_SUFFIX = "\\)";

    /**
     * Converts the given message to one that will apply the given style name(s).
     *
     * @param style The style name. May be a comma separated list.
     * @param message The message.
     * @param args The message args.
     * @return The formatted message.
     */
    public static String style(String style, Object message, Object... args) {
        final String msg = String.format(message.toString(), args).replace(STYLE_SUFFIX, ESCAPED_STYLE_SUFFIX);
        return STYLE_PREFIX + style + " " + msg + STYLE_SUFFIX;
    }

    /**
     * Converts the given message to one that will apply the {@code italic} style.
     *
     * @param message The message.
     * @return The formatted message.
     */
    public static String italic(Object message) {
        return style("italic", message);
    }

    /**
     * Converts the given message to one that will apply the {@code bold cyan} style.
     *
     * @param message The message.
     * @return The formatted message.
     */
    public static String boldCyan(Object message) {
        return style("CYAN", message);
    }

    /**
     * Converts the given message to one that will apply the {@code bold blue} style.
     *
     * @param message The message.
     * @return The formatted message.
     */
    public static String boldBlue(Object message) {
        return style("BLUE", message);
    }

    private Style() {
    }
}
