/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.build;

/**
 * A build type.
 */
public enum BuildType  {

    /**
     * Java source files.
     */
    JavaSources(DirectoryType.JavaSources, FileType.JavaSource),

    /**
     * Java classes.
     */
    JavaClasses(DirectoryType.Classes, FileType.JavaClass),

    /**
     * Resource source files.
     */
    Resources(DirectoryType.Resources, FileType.NotJavaClass);

    private final DirectoryType directoryType;
    private final FileType fileType;

    BuildType(DirectoryType directoryType, FileType fileType) {
        this.directoryType = directoryType;
        this.fileType = fileType;
    }

    /**
     * Returns the associated directory type.
     *
     * @return The directory type.
     */
    public DirectoryType directoryType() {
        return directoryType;
    }

    /**
     * Returns the associated file type.
     *
     * @return The file type.
     */
    public FileType fileType() {
        return fileType;
    }
}
