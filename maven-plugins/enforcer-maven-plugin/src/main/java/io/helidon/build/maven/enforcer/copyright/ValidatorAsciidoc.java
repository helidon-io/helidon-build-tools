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

package io.helidon.build.maven.enforcer.copyright;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class ValidatorAsciidoc extends ValidatorBase {
    private static final Optional<String> BLOCK_COMMENT_PREFIX = Optional.of("    ");

    protected ValidatorAsciidoc(ValidatorConfig validatorConfig, List<TemplateLine> templateLines) {
        super(validatorConfig, templateLines);
    }

    @Override
    public Set<String> supportedSuffixes() {
        return Set.of(".adoc");
    }

    @Override
    protected boolean supportsBlockComments() {
        return true;
    }

    @Override
    protected String blockCommentStart() {
        return "/".repeat(79);
    }

    @Override
    protected String blockCommentEnd() {
        return "/".repeat(79);
    }

    @Override
    protected boolean allowLeadEmptyLine() {
        return true;
    }

    @Override
    protected boolean allowTrailEmptyLine(String modifiedYear) {
        return true;
    }

    @Override
    protected Optional<String> blockCommentPrefix() {
        return BLOCK_COMMENT_PREFIX;
    }

    @Override
    protected boolean isPreamble(String line) {
        return line.startsWith("=");
    }
}
