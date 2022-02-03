/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.build.common.ansi;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumMap;
import java.util.Map;

import io.helidon.build.common.RichTextRenderer;
import io.helidon.build.common.logging.LogFormatter;
import io.helidon.build.common.logging.LogLevel;

import static io.helidon.build.common.ansi.AnsiTextStyles.BoldYellow;
import static io.helidon.build.common.ansi.AnsiTextStyles.Italic;
import static io.helidon.build.common.ansi.AnsiTextStyles.ItalicRed;
import static io.helidon.build.common.ansi.AnsiTextStyles.Plain;
import static io.helidon.build.common.ansi.AnsiTextStyles.Red;

/**
 * {@link LogFormatter} implementation that supports ANSI via {@link io.helidon.build.common.RichTextRenderer}
 * substitutions.
 */
final class AnsiLogFormatter extends LogFormatter {

    private static final String EOL = System.getProperty("line.separator");

    private final String warnPrefix;
    private final String errorPrefix;
    private final Map<LogLevel, AnsiTextStyles> stylesByLevel;

    private AnsiLogFormatter() {
        boolean stylesEnabled = AnsiTextProvider.isEnabled();
        warnPrefix = stylesEnabled ? BoldYellow.apply("warning: ") : "WARNING: ";
        errorPrefix = stylesEnabled ? Red.apply("error: ") : "ERROR: ";
        stylesByLevel = new EnumMap<>(LogLevel.class);
        stylesByLevel.put(LogLevel.DEBUG, Italic);
        stylesByLevel.put(LogLevel.VERBOSE, Plain);
        stylesByLevel.put(LogLevel.INFO, Plain);
        stylesByLevel.put(LogLevel.WARN, Plain);
        stylesByLevel.put(LogLevel.ERROR, ItalicRed);
    }

    @Override
    public String formatEntry(LogLevel level, Throwable thrown, String message, Object... args) {
        String entry = toStyled(level, thrown, message, args);
        switch (level) {
            case DEBUG:
            case VERBOSE:
            case INFO:
                return entry;
            case WARN:
                return warnPrefix + entry;
            case ERROR:
                return errorPrefix + entry;
            default:
                throw new Error();
        }
    }

    private String toStyled(LogLevel level, Throwable thrown, String message, Object... args) {
        String rendered = RichTextRenderer.render(message, args);
        String styled = toStyled(level, rendered);
        String trace = toStackTrace(thrown, level);
        if (trace == null) {
            return styled;
        } else if (styled.isEmpty()) {
            return trace;
        } else {
            return styled + EOL + trace;
        }
    }

    private String toStyled(LogLevel level, String message) {
        return AnsiTextStyle.isStyled(message) ? message : styleEntry(level, message);
    }

    private String toStackTrace(Throwable thrown, LogLevel level) {
        if (thrown != null) {
            if (isDebug(level)) {
                final StringWriter sw = new StringWriter();
                try (PrintWriter pw = new PrintWriter(sw)) {
                    thrown.printStackTrace(pw);
                    return styleEntry(LogLevel.DEBUG, sw.toString());
                } catch (Exception ignored) {
                }
            } else if (isVerbose(level)) {
                return styleEntry(LogLevel.DEBUG, thrown.toString());
            }
        }
        return null;
    }

    private String styleEntry(LogLevel level, String entry) {
        AnsiTextStyles style = stylesByLevel.get(level);
        return style == Plain ? entry : style.apply(entry);
    }

    /**
     * Lazy singleton.
     */
    static final class Holder {

        /**
         * Singleton instance.
         */
        static final AnsiLogFormatter INSTANCE = new AnsiLogFormatter();
    }
}
