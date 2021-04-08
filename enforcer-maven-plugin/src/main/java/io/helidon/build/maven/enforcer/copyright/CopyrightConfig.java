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

import java.io.File;
import java.util.Optional;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Copyright configuration to be used with Maven plugin.
 *
 * @see io.helidon.build.maven.enforcer.EnforcerMojo
 */
public class CopyrightConfig {
    /**
     * Fail if copyright is invalid.
     */
    @Parameter(defaultValue = "false")
    private boolean failOnError;

    /**
     * File with the template to use for copyright.
     */
    @Parameter
    private File templateFile;

    /**
     * File with excludes.
     */
    @Parameter
    private File excludeFile;

    /**
     * Copyright year separator.
     */
    @Parameter(defaultValue = ", ")
    private String yearSeparator;

    @Override
    public String toString() {
        return "CopyrightConfig{"
                + ", failOnError=" + failOnError
                + ", templateFile=" + templateFile
                + ", excludeFile=" + excludeFile
                + ", yearSeparator='" + yearSeparator
                + '}';
    }

    /**
     * Whether this rule is configured to fail on error.
     *
     * @return whether to fail on error
     */
    public boolean failOnError() {
        return failOnError;
    }

    Optional<File> templateFile() {
        return Optional.ofNullable(templateFile);
    }

    Optional<File> excludeFile() {
        return Optional.ofNullable(excludeFile);
    }

    Optional<String> yearSeparator() {
        return Optional.ofNullable(yearSeparator);
    }
}
