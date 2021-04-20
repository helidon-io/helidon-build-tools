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

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class ValidatorProperties extends ValidatorBase {
    protected ValidatorProperties(ValidatorConfig validatorConfig, List<TemplateLine> templateLines) {
        super(validatorConfig, templateLines);
    }

    @Override
    public Set<String> supportedSuffixes() {
        return Set.of(".properties",
                      ".prefs",
                      ".py",
                      ".sh",
                      ".ksh");
    }

    @Override
    public boolean supports(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.startsWith("Makefile")) {
            return true;
        }
        if (fileName.startsWith("GNUmakefile")) {
            return true;
        }
        if (fileName.startsWith("Rakefile")) {
            return true;
        }
        if (fileName.startsWith("Dockerfile")) {
            return true;
        }
        if (fileName.equals("osgi.bundle")) {
            return true;
        }

        return startsWith(path, "#");
    }

    @Override
    protected boolean isPreamble(String line) {
        return line.startsWith("#!");
    }

    @Override
    protected boolean copyrightBeforePreamble() {
        return false;
    }

    @Override
    protected Optional<String> lineCommentStart() {
        return Optional.of("#");
    }
}
