/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.maven.enforcer.copyright;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.helidon.build.common.logging.Log;
import io.helidon.build.maven.enforcer.FileRequest;

class ValidatorJava extends ValidatorBase {
    private static final Optional<String> BLOCK_COMMENT_PREFIX = Optional.of("*");

    protected ValidatorJava(ValidatorConfig validatorConfig, List<TemplateLine> templateLines) {
        super(validatorConfig, templateLines);
    }

    @Override
    public Set<String> supportedSuffixes() {
        return Set.of(".java",
                      ".g",
                      ".c",
                      ".h",
                      ".css",
                      ".js");
    }

    @Override
    public boolean supports(Path path) {
        return super.startsWith(path, "/*\n");
    }

    @Override
    protected boolean supportsBlockComments() {
        return true;
    }

    @Override
    protected String blockCommentStart() {
        return "/*";
    }

    @Override
    protected String blockCommentEnd() {
        return "*/";
    }

    @Override
    protected Optional<String> blockCommentPrefix() {
        return BLOCK_COMMENT_PREFIX;
    }

    @Override
    protected boolean isPreamble(String line) {
        return line.startsWith("package ");
    }

    @Override
    protected boolean allowTrailEmptyLine(String modifiedYear) {
        // we have a leeway here - for files modified before current year, we allow a trailing line
        //return !currentYear().equals(modifiedYear);
        return true;
    }

    @Override
    protected void containsTrailingEmptyLine(FileRequest file) {
        Log.warn("File " + file.relativePath() + " contains trailing empty line in copyright comment.");
    }
}
