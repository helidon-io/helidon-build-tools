/*
 * Copyright (c) 2019, 2023 Oracle and/or its affiliates.
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

import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.common.logging.LogLevel;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * A plexus component that sets the logger.
 */
@Component(role = PlexusLoggerHolder.class)
public class PlexusLoggerHolder {

    /**
     * The plexus logger reference.
     */
    public static final AtomicReference<Logger> REF = new AtomicReference<>();

    /**
     * Set the logger.
     *
     * @param logger logger
     */
    @Requirement
    @SuppressWarnings("unused")
    public void setLogger(Logger logger) {
        if (logger.isDebugEnabled()) {
            LogLevel.set(LogLevel.DEBUG);
        } else if (logger.isWarnEnabled()) {
            LogLevel.set(LogLevel.ERROR);
        } else if (logger.isWarnEnabled()) {
            LogLevel.set(LogLevel.WARN);
        } else if (logger.isInfoEnabled()) {
            LogLevel.set(LogLevel.INFO);
        }
        REF.set(logger);
    }
}
