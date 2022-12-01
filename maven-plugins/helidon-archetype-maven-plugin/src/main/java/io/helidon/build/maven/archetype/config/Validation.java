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
package io.helidon.build.maven.archetype.config;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import io.helidon.build.common.SourcePath;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Generated files validation.
 */
@SuppressWarnings("unused")
public class Validation {

    private Set<String> patterns;
    private String match;
    private boolean fail;

    /**
     * Get patterns.
     *
     * @return patterns
     */
    public Set<String> getPatterns() {
        return patterns;
    }

    /**
     * Get match.
     *
     * @return match
     */
    public String getMatch() {
        return match;
    }

    /**
     * Get fail.
     *
     * @return fail
     */
    public boolean getFail() {
        return fail;
    }

    /**
     * Set patterns.
     *
     * @param patterns patterns
     */
    public void setPatterns(Set<String> patterns) {
        this.patterns = patterns;
    }

    /**
     * Set match.
     *
     * @param match match
     */
    public void setMatch(String match) {
        this.match = match;
    }

    /**
     * Set fail.
     *
     * @param fail fail
     */
    public void setFail(boolean fail) {
        this.fail = fail;
    }

    /**
     * Validate generated files using this validation configuration.
     *
     * @param basedir base directory
     * @throws MojoExecutionException if validation mismatch
     */
    public void validate(File basedir) throws MojoExecutionException {
        List<SourcePath> paths = SourcePath.scan(basedir);
        String error = String.format("Validation failed in directory %s", basedir);
        boolean isMatch;
        Predicate<SourcePath> matches = path -> path.matches(patterns);
        switch (match) {
            case "all":
                isMatch = paths.stream().allMatch(matches);
                break;
            case "any":
                isMatch = paths.stream().anyMatch(matches);
                break;
            case "none":
                isMatch = paths.stream().noneMatch(matches);
                break;
            default:
                throw new MojoExecutionException("Wrong validation match value: " + match);
        }
        if (isMatch == fail) {
            throw new MojoExecutionException(
                    String.format("%s with patterns: %s match: %s, fail: %s", error, patterns, match, fail));
        }
    }
}
