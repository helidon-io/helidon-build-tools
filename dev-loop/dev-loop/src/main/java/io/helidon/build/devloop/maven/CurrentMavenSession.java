/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.build.devloop.maven;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.execution.MavenSession;

import static java.util.Objects.requireNonNull;

/**
 * A holder for the current {@code MavenSession} which is initialized during startup to the session used
 * by our mojo.
 * <p></p>
 * Since full/prime builds use a separate pathway to build (see {@link EmbeddedMavenExecutor}), changes to the pom.xml
 * (e.g. dependencies) will not be reflected in the initial {@code MavenSession} instance held here. To ensure that
 * such changes are seen by builds using this class, the session must be updated accordingly. To do this, we capture
 * the session used during the full build (see {@link MavenProjectConfigCollector}) and re-use it here.
 * <p></p>
 * To capture and pass the session, we must use an ugly hack. Using a static method to store the value does not work,
 * as the class loaders are isolated and therefore different class instances are seen by {@link MavenProjectConfigCollector})
 * and our plugin classes. Therefore, the only way to pass the session is to store it in the system properties:
 * see {@link #set(MavenSession)}.
 */
class CurrentMavenSession {
    private static final String MAVEN_SESSION_PROPERTY = "dev-loop:" + MavenSession.class.getName();
    private static final AtomicReference<MavenSession> CURRENT_SESSION = new AtomicReference<>();

    /**
     * Initialize the current session if not already done.
     *
     * @param session The session.
     */
    static void initialize(MavenSession session) {
        if (CURRENT_SESSION.get() == null) {
            CURRENT_SESSION.set(session);
        }
    }

    /**
     * Store the session in the system properties for extraction by {@link #get()}. Used only by
     * {@link MavenProjectConfigCollector}.
     *
     * @param session The session.
     */
    static void set(MavenSession session) {
        System.getProperties().put(MAVEN_SESSION_PROPERTY, requireNonNull(session));
    }

    /**
     * Return the current session, updating it if a new one was passed via {@link #set(MavenSession)}.
     *
     * @return The session.
     */
    static MavenSession get() {

        // Do we have a new session set via a full build?

        Properties systemProperties = System.getProperties();
        Object fullBuildSession = systemProperties.get(MAVEN_SESSION_PROPERTY);
        if (fullBuildSession != null) {

            // Yes. Fix it up, store it as the current session and return it

            MavenSession session = (MavenSession) fullBuildSession;
            session.setCurrentProject(session.getAllProjects().get(0));
            CURRENT_SESSION.set(session);
            systemProperties.remove(MAVEN_SESSION_PROPERTY);
            return session;

        } else {

            // No, just use the current

            return CURRENT_SESSION.get();
        }
    }

    private CurrentMavenSession() {
    }
}
