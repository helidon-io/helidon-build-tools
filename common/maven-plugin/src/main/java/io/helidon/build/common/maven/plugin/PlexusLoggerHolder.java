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

import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Maven life-cycle participant that captures the plexus logger.
 */
@Component(role = AbstractMavenLifecycleParticipant.class, hint = "plexus-logger-holder")
public class PlexusLoggerHolder extends AbstractMavenLifecycleParticipant {

    static final AtomicReference<Logger> LOGGER = new AtomicReference<>();

    @Requirement
    private Logger logger;

    @Override
    public void afterProjectsRead(MavenSession session) {
        LOGGER.set(logger);
    }
}
