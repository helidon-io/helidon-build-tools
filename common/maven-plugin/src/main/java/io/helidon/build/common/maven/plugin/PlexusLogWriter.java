/*
 * Copyright (c) 2019, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.common.maven.plugin;

import io.helidon.build.common.RichTextRenderer;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.logging.LogWriter;
import io.helidon.build.common.logging.SystemLogWriter;

import org.codehaus.plexus.logging.Logger;

/**
 * {@link LogWriter} that writes to a maven log.
 */
public class PlexusLogWriter extends LogWriter {

    @Override
    public void writeEntry(LogLevel level, Throwable thrown, String message, Object... args) {
        Logger logger = PlexusLoggerHolder.REF.get();
        if (logger == null) {
            // fallback to the default writer
            SystemLogWriter.INSTANCE.writeEntry(level, thrown, message, args);
            return;
        }
        String entry;
        switch (level) {
            case DEBUG:
            case VERBOSE:
                if (logger.isDebugEnabled()) {
                    entry = renderEntry(message, args);
                    if (thrown == null) {
                        logger.debug(entry);
                    } else {
                        logger.debug(entry, thrown);
                    }
                }
                break;
            case INFO:
                if (logger.isInfoEnabled()) {
                    entry = renderEntry(message, args);
                    if (thrown == null) {
                        logger.info(entry);
                    } else {
                        logger.info(entry, thrown);
                    }
                }
                break;
            case WARN:
                if (logger.isWarnEnabled()) {
                    entry = renderEntry(message, args);
                    if (thrown == null) {
                        logger.warn(entry);
                    } else {
                        logger.warn(entry, thrown);
                    }
                }
                break;
            case ERROR:
                if (logger.isErrorEnabled()) {
                    entry = renderEntry(message, args);
                    if (thrown == null) {
                        logger.error(entry);
                    } else {
                        logger.error(entry, thrown);
                    }
                }
                break;
            default:
                throw new Error();
        }
    }

    private String renderEntry(String message, Object... args) {
        String entry = RichTextRenderer.render(message, args);
        recordEntry(entry);
        return entry;
    }
}
