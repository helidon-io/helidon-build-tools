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
package io.helidon.build.util;

import static io.helidon.build.util.StyleFunction.Bold;

/**
 * Constants supporting coordination between {@code DevLoop} and {@code DevCommand}.
 */
public class DevLoopMessages {

    /**
     * Dev loop message prefix.
     */
    public static final String DEV_LOOP_MESSAGE_PREFIX = "|";

    /**
     * Styled dev loop message prefix.
     */
    public static final String DEV_LOOP_STYLED_MESSAGE_PREFIX = Bold.apply(DEV_LOOP_MESSAGE_PREFIX);

    /**
     * Message content when the dev loop has started.
     */
    public static final String DEV_LOOP_START = "loop started";

    /**
     * Message content when the dev loop clears the screen and writes the header.
     */
    public static final String DEV_LOOP_HEADER = "helidon dev";

    /**
     * Message content when a project has changed.
     */
    public static final String DEV_LOOP_PROJECT_CHANGED = "changed";

    /**
     * Message content when a build is starting.
     */
    public static final String DEV_LOOP_BUILD_STARTING = "building";

    /**
     * Message content when a the dev loop has failed.
     */
    public static final String DEV_LOOP_FAILED = "loop failed";

    /**
     * Message content when a build has failed.
     */
    public static final String DEV_LOOP_BUILD_FAILED = "build failed";

    /**
     * Message content when a build has completed successfully.
     */
    public static final String DEV_LOOP_BUILD_COMPLETED = "completed";

    /**
     * Message content when the application is starting.
     */
    public static final String DEV_LOOP_APPLICATION_STARTING = "starting";

    /**
     * Message content when the application has failed.
     */
    public static final String DEV_LOOP_APPLICATION_FAILED = "failed";

    /**
     * Message content when the application is stopping.
     */
    public static final String DEV_LOOP_APPLICATION_STOPPING = "stopping";

    /**
     * Message content when the application has stopped.
     */
    public static final String DEV_LOOP_APPLICATION_STOPPED = "stopped";

    private DevLoopMessages() {
    }
}
