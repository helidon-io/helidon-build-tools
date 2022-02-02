/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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

/**
 * Helidon CLI implementation.
 */
module io.helidon.build.cli.impl {
    requires io.helidon.build.cli.harness;
    requires io.helidon.build.archetype.engine.v1;
    requires io.helidon.build.archetype.engine.v2;
    requires io.helidon.build.cli.plugin;
    requires org.graalvm.sdk;
    requires io.helidon.build.common.ansi;
    requires io.helidon.build.common;
    requires io.helidon.build.common.maven;
    requires io.helidon.build.cli.common;
    requires io.helidon.build.devloop.common;
    requires jdk.unsupported;
    provides io.helidon.build.cli.harness.CommandRegistry
            with io.helidon.build.cli.impl.HelidonRegistry;
}
