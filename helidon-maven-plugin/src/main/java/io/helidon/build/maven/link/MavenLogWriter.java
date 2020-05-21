/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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

package io.helidon.build.maven.link;


import io.helidon.build.util.Log.Level;
import io.helidon.build.util.Style;

import org.apache.maven.plugin.logging.Log;

/**
 * {@link io.helidon.build.util.Log.Writer} that writes to a maven log.
 */
public class MavenLogWriter implements io.helidon.build.util.Log.Writer {
    private final Log log;

    /**
     * Binds the given maven log to the {@code io.helidon.build.util.Log} so that
     * calls to the latter will be written to the maven log.
     * @param mavenLog The maven log.
     */
    public static void bind(Log mavenLog) {
        io.helidon.build.util.Log.writer(new MavenLogWriter(mavenLog));
    }

    /**
     * Constructor.
     *
     * @param log The maven log.
     */
    MavenLogWriter(Log log) {
        this.log = log;
    }

    @Override
    public boolean isDebug() {
        return log.isDebugEnabled();
    }

    @Override
    public boolean isVerbose() {
        return isDebug();
    }

    @Override
    @SuppressWarnings("checkstyle:AvoidNestedBlocks")
    public void write(Level level, Throwable thrown, String message, Object... args) {
        switch (level) {

            case DEBUG:
            case VERBOSE:
                if (log.isDebugEnabled()) {
                    final String msg = Style.render(message, args);
                    if (thrown == null) {
                        log.debug(msg);
                    } else {
                        log.debug(msg, thrown);
                    }
                }
                break;

            case INFO: {
                if (log.isInfoEnabled()) {
                    final String msg = Style.render(message, args);
                    if (thrown == null) {
                        log.info(msg);
                    } else {
                        log.info(msg, thrown);
                    }
                }
                break;
            }

            case WARN: {
                if (log.isWarnEnabled()) {
                    final String msg = Style.render(message, args);
                    if (thrown == null) {
                        log.warn(msg);
                    } else {
                        log.warn(msg, thrown);
                    }
                }
                break;
            }

            case ERROR: {
                if (log.isErrorEnabled()) {
                    final String msg = Style.render(message, args);
                    if (thrown == null) {
                        log.error(msg);
                    } else {
                        log.error(msg, thrown);
                    }
                }
                break;
            }

            default: {
                throw new Error();
            }
        }
    }
}
