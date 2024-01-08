/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.helidon.build.common.FileUtils.ensureDirectory;

/**
 * Generate a directory using a set of actions.
 */
class StagingDirectory extends StagingTask {

    static final String ELEMENT_NAME = "directory";

    StagingDirectory(List<StagingAction> nested, Map<String, String> attrs) {
        super(ELEMENT_NAME, nested, null, attrs);
    }

    @Override
    public CompletionStage<Void> execute(StagingContext ctx, Path dir, Map<String, String> vars) {
        Path targetDir = dir.resolve(target());
        ctx.logInfo("Staging %s", targetDir);
        try {
            ensureDirectory(targetDir);
        } catch (Throwable ex) {
            return CompletableFuture.failedFuture(ex);
        }
        return super.execute(ctx, targetDir, vars);
    }

    @Override
    protected CompletableFuture<Void> execBody(StagingContext ctx, Path dir, Map<String, String> vars) {
        return CompletableFuture.completedFuture(null);
    }
}
