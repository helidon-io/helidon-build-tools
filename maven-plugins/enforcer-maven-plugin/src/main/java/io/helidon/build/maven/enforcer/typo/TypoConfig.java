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

package io.helidon.build.maven.enforcer.typo;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Configuration of typos rule, used by {@link io.helidon.build.maven.enforcer.EnforcerMojo}.
 */
public class TypoConfig {
    /**
     * Fail if typos found.
     */
    @Parameter(defaultValue = "false")
    private boolean failOnError;

    /**
     * List of typos to look for.
     */
    @Parameter(required = true)
    private String[] typos;

    /**
     * List of suffixes (such as {@code .js}) and file names (such as {@code Dockerfile}) to include.
     */
    @Parameter
    private String[] includes;

    /**
     * List of suffixes (such as {@code .js}) and file names (such as {@code Dockerfile}) to include.
     */
    @Parameter
    private String[] excludes;

    /**
     * Whether to fail on error.
     *
     * @return fail on error
     */
    public boolean failOnError() {
        return failOnError;
    }

    @Override
    public String toString() {
        return "TypoConfig{"
                + "failOnError=" + failOnError
                + ", typos=" + Arrays.toString(typos)
                + ", includes=" + Arrays.toString(includes)
                + ", excludes=" + Arrays.toString(excludes)
                + '}';
    }

    Set<String> typos() {
        return Arrays.stream(typos)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    Set<String> excludes() {
        if (excludes == null) {
            return Set.of();
        }
        return Set.of(excludes);
    }

    Set<String> includes() {
        if (includes == null) {
            return Set.of();
        }
        return Set.of(includes);
    }
}
