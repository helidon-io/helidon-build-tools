/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.build.maven.enforcer.inclusivenaming;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Copyright configuration to be used with Maven plugin.
 *
 * @see io.helidon.build.maven.enforcer.EnforcerMojo
 */
public class InlusiveNamingConfig {
    /**
     * Fail if copyright is invalid.
     */
    @Parameter(defaultValue = "false")
    private boolean failOnError;

    /**
     * List of suffixes (such as {@code .js}) and file names (such as {@code Dockerfile}) to include.
     */
    @Parameter
    private String[] includes;

    /**
     * List of suffixes (such as {@code .js}) and file names (such as {@code Dockerfile}) to exclude.
     */
    @Parameter
    private String[] excludes;

    /**
     * List of words (such as {@code slave}) to exclude.
     */
    @Parameter
    private String[] excludeTerms;

    @Override
    public String toString() {
        return "InlusiveNamingConfig{"
                + "failOnError=" + failOnError
                + ", includes=" + Arrays.toString(includes)
                + ", excludes=" + Arrays.toString(excludes)
                + ", excludeTerms=" + Arrays.toString(excludeTerms)
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

    Set<String> excludeTerms() {
        if (excludeTerms == null) {
            return Set.of();
        }
        return Stream.of(excludeTerms).map(s -> s.toLowerCase()).collect(Collectors.toSet());
    }

}
