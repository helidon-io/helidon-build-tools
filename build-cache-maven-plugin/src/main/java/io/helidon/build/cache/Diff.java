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
package io.helidon.build.cache;

/**
 * Diff.
 */
class Diff {

    private final Object orig;
    private final Object actual;
    private final String path;

    /**
     * Create a new diff instance.
     *
     * @param orig   removed object may be {@code null}
     * @param actual added object may be {@code null}
     * @param path   path
     * @throws IllegalArgumentException if both orig and actual are {@code null}
     */
    Diff(Object orig, Object actual, String path) {
        if (orig == null && actual == null) {
            throw new IllegalArgumentException("Both orig and actual are {@code null}");
        }
        this.orig = orig;
        this.actual = actual;
        this.path = path;
    }

    /**
     * Get the diff description.
     *
     * @return diff description
     */
    String asString() {
        StringBuilder sb = new StringBuilder(path);
        if (orig != null && actual != null) {
            sb.append(" was '").append(orig).append("' but is now '").append(actual).append("'");
        } else if (actual != null) {
            sb.append(" has been added");
        } else {
            sb.append(" has been removed");
        }
        return sb.toString();
    }
}
