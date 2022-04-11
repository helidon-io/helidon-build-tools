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

package io.helidon.build.archetype.engine.v2.ast;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Source location.
 */
public final class Location {

    private final Path path;
    private final int lineNo;
    private final int charNo;

    private Location(Path path, int lineNo, int charNo) {
        this.path = Objects.requireNonNull(path, "path is null");
        if (lineNo < 0) {
            throw new IllegalArgumentException("Invalid line number: " + lineNo);
        }
        this.lineNo = lineNo;
        if (charNo < 0) {
            throw new IllegalArgumentException("Invalid line character number: " + charNo);
        }
        this.charNo = charNo;
    }

    /**
     * Get the path.
     *
     * @return path
     */
    public Path path() {
        return path;
    }

    /**
     * Get the line number.
     *
     * @return line number
     */
    public int lineNumber() {
        return lineNo;
    }

    /**
     * Get the line character number.
     *
     * @return line character number
     */
    public int charNumber() {
        return charNo;
    }

    @Override
    public String toString() {
        return path + ":" + lineNo + ":" + charNo;
    }

    /**
     * Make a copy.
     *
     * @return copy
     */
    public Location copy() {
        return new Location(path, lineNo, charNo);
    }

    /**
     * Create a new location.
     *
     * @param path   file path
     * @param lineNo line number
     * @param charNo line character number
     * @return location
     */
    public static Location of(Path path, int lineNo, int charNo) {
        return new Location(path, lineNo, charNo);
    }
}
