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
package com.acme.cli;

import java.util.Collection;

import io.helidon.build.cli.harness.*;

@CommandFragment
class TestOptions {

    @Creator
    TestOptions(
            @Option.KeyValue(name = "name", description = "The name") String name,
            @Option.KeyValues(name = "talents", description = "The talents") Collection<String> talents,
            @Option.Flag(name = "record", description = "Record flag") boolean buffoon,
            @Option.Flag(name = "export", description = "Export flag", visible = false) boolean export) {

    }
}