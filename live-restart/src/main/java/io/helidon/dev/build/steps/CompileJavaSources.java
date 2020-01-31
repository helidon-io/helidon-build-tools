/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.build.steps;

import java.util.function.Consumer;

import io.helidon.dev.build.BuildRoot;
import io.helidon.dev.build.BuildStep;
import io.helidon.dev.build.BuildType;

/**
 * A build step that compiles java sources using the ToolProvider API.
 */
public class CompileJavaSources implements BuildStep {

    @Override
    public BuildType inputType() {
        return BuildType.JavaSources;
    }

    @Override
    public BuildType outputType() {
        return BuildType.JavaClasses;
    }

    @Override
    public void incrementalBuild(BuildRoot.Changes changes,
                                 Consumer<String> stdOut,
                                 Consumer<String> stdErr) throws Exception {
    }

    @Override
    public String toString() {
        return "CompileJavaSources{}";
    }
}
