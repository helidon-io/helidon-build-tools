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

package io.helidon.build.copyright;

interface Exclude {
    /**
     *
     * @param file file information
     * @return true if the file matches current exclude
     */
    boolean exclude(FileRequest file);

    static Exclude create(String line) {
        // if starts with ., it is a suffix
        if (line.startsWith(".")) {
            // .ico
            return new SuffixExclude(line);
        }
        if (line.startsWith("/")) {
            // /etc/txt - from repo root
            return new StartsWithExclude(line.substring(1));
        }
        if (line.endsWith("/")) {
            // src/main/proto/
            return new DirectoryExclude(line);
        }
        if (line.contains(".") && !line.contains("/")) {
            // jaxb.index
            return new NameExclude(line);
        }
        return new ContainsExclude(line);
    }

    class StartsWithExclude implements Exclude {
        // exact path from repository root, such as /etc/copyright.txt
        private final String exclude;

        StartsWithExclude(String exclude) {
            this.exclude = exclude;
        }

        @Override
        public boolean exclude(FileRequest file) {
            return file.relativePath().startsWith(exclude);
        }
    }

    class DirectoryExclude implements Exclude {
        private final ContainsExclude contains;
        private final StartsWithExclude startWith;

        DirectoryExclude(String directory) {
            // either the directory is within the tree
            this.contains = new ContainsExclude("/" + directory);
            // or the tree starts with it
            this.startWith = new StartsWithExclude(directory);
        }

        @Override
        public boolean exclude(FileRequest file) {
            return contains.exclude(file) || startWith.exclude(file);
        }
    }

    class ContainsExclude implements Exclude {
        // such as /src/main/proto/
        private final String contains;

        ContainsExclude(String contains) {
            this.contains = contains;
        }

        @Override
        public boolean exclude(FileRequest file) {
            return file.relativePath().contains(contains);
        }
    }

    class NameExclude implements Exclude {
        private final String name;

        NameExclude(String name) {
            this.name = name;
        }

        @Override
        public boolean exclude(FileRequest file) {
            return file.fileName().equals(name);
        }
    }

    class SuffixExclude implements Exclude {
        private final String suffix;

        SuffixExclude(String suffix) {
            this.suffix = suffix;
        }

        @Override
        public boolean exclude(FileRequest file) {
            return file.suffix().equals(suffix);
        }
    }
}
