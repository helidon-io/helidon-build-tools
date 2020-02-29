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

package io.helidon.dev.build.maven;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

/**
 * Class LifecycleParticipant.
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class LifecycleParticipant extends AbstractMavenLifecycleParticipant {

    /**
     * Constructor.
     */
    public LifecycleParticipant() {
        System.out.println("LifecycleParticipant: ctor");
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        super.afterProjectsRead(session);
        System.out.println("LifecycleParticipant: projects read");
    }

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        super.afterSessionStart(session);
        System.out.println("LifecycleParticipant: session start");
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        final MavenProject project = session.getCurrentProject();
        // TODO Store project configuration file
        System.out.println("LifecycleParticipant: session end, project " + project.getArtifactId() + "cached");
        super.afterSessionEnd(session);
    }
}
