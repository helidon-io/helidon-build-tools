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

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Staging action that prints information about the action being executed.
 */
final class DryRunAction implements StagingAction {

    private final StagerMojo stagerMojo;
    private final StagingAction delegate;

    DryRunAction(StagerMojo stagerMojo, StagingAction delegate) {
        this.stagerMojo = stagerMojo;
        this.delegate = delegate;
    }

    @Override
    public CompletionStage<Void> execute(StagingContext ctx, Path dir, Map<String, String> vars) {
        stagerMojo.getLog().info(toString(dir, vars));
        return delegate.execute(ctx, dir, vars);
    }

    @Override
    public String elementName() {
        return delegate.elementName();
    }

    @Override
    public String toString(Path dir, Map<String, String> vars) {
        return delegate.toString(dir, vars);
    }
}
