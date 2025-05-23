/*
 * Copyright (c) 2019, 2025 Oracle and/or its affiliates.
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
package io.helidon.build.common.maven.logging;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Maven life-cycle participant that captures the plexus logger.
 */
@Component(role = AbstractMavenLifecycleParticipant.class, hint = "plexus-logger-initializer")
public class PlexusLoggerInitializer extends AbstractMavenLifecycleParticipant {

    @Requirement
    @SuppressWarnings("unused")
    private PlexusLoggerHolder holder;
}
