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
package io.helidon.build.common.logging;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.helidon.build.common.RichTextRenderer;

/**
 * Default log formatter implementation.
 */
final class DefaultFormatter extends LogFormatter {

    /**
     * Singleton instance.
     */
    static final DefaultFormatter INSTANCE = new DefaultFormatter();
    private static final String EOL = System.getProperty("line.separator");

    private DefaultFormatter() {
    }

    @Override
    public String formatEntry(LogLevel level, Throwable thrown, String message, Object... args) {
        final String rendered = RichTextRenderer.render(message, args);
        final String trace = trace(level, thrown);
        if (trace == null) {
            return rendered;
        } else if (rendered.isEmpty()) {
            return trace;
        } else {
            return rendered + EOL + trace;
        }
    }

    private static String trace(LogLevel level, Throwable thrown) {
        if (thrown != null) {
            if (isDebug(level)) {
                StringWriter sw = new StringWriter();
                try (PrintWriter pw = new PrintWriter(sw)) {
                    thrown.printStackTrace(pw);
                    return sw.toString();
                } catch (Exception ignored) {
                }
            } else if (isVerbose(level)) {
                return thrown.toString();
            }
        }
        return null;
    }
}
