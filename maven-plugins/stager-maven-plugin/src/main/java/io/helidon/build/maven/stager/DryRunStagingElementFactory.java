/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.build.maven.stager;

import java.util.List;
import java.util.Map;

/**
 * Custom staging element factory that wraps the staging tasks to override the execution to do a no-op.
 */
final class DryRunStagingElementFactory extends StagingElementFactory {

    private final StagerMojo stagerMojo;

    DryRunStagingElementFactory(StagerMojo stagerMojo) {
        this.stagerMojo = stagerMojo;
    }

    @Override
    StagingAction createAction(String name,
                               Map<String, String> attrs,
                               Map<String, List<StagingElement>> children,
                               String text) {

        StagingAction action = super.createAction(name, attrs, children, text);
        return new DryRunAction(stagerMojo, action);
    }
}
