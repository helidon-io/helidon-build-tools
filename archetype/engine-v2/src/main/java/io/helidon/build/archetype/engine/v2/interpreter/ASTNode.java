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

package io.helidon.build.archetype.engine.v2.interpreter;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Base class for AST nodes.
 */
public abstract class ASTNode implements Visitable, Serializable {

    private final LinkedList<Visitable> children = new LinkedList<>();
    private ASTNode parent;
    private final Location location;
    private Iterator<Visitable> iterator;

    ASTNode(ASTNode parent, Location location) {
        this.location = location;
        this.parent = parent;
    }

    /**
     * Returns true if the node has more children.
     *
     * @return true if the node has more children, false otherwise
     */
    public boolean hasNext() {
        if (iterator == null) {
            iterator = children.iterator();
        }
        return iterator.hasNext();
    }

    /**
     * Returns the next child.
     *
     * @return next child
     */
    public Visitable next() {
        if (iterator == null) {
            iterator = children.iterator();
        }
        return iterator.next();
    }

    /**
     * Location associated with the current node.
     *
     * @return location
     */
    public Location location() {
        return location;
    }

    /**
     * Get children nodes of the current node.
     *
     * @return children nodes
     */
    public LinkedList<Visitable> children() {
        return children;
    }

    protected static <T, R> LinkedList<R> transformList(LinkedList<T> list, Function<T, R> mapper) {
        return list.stream().map(mapper).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Parent AST node for the current node.
     *
     * @return parent ASTNode
     */
    public ASTNode parent() {
        return parent;
    }

    void parent(ASTNode parent) {
        this.parent = parent;
    }

    /**
     * Location of the elements in the AST.
     */
    public static class Location {

        private final String currentDirectory;
        private final String scriptDirectory;
        private final String scriptFile;

        private Location(String currentDirectory, String scriptDirectory, String scriptFile) {
            this.currentDirectory = currentDirectory;
            this.scriptDirectory = scriptDirectory;
            this.scriptFile = scriptFile;
        }

        /**
         * Path to the directory used for resolving paths in the {@code OutputAST} (resolved relative to the archetype root
         * directory).
         *
         * @return current directory
         */
        public String currentDirectory() {
            return currentDirectory;
        }

        /**
         * Path to the directory of the current descriptor script (resolved relative to the archetype root directory).
         *
         * @return script directory
         */
        public String scriptDirectory() {
            return scriptDirectory;
        }

        /**
         * Path to the current descriptor script file (resolved relative to the archetype root directory).
         *
         * @return script directory
         */
        public String scriptPath() {
            return scriptFile;
        }

        /**
         * Create a new builder.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        @Override
        public String toString() {
            return "Location{"
                   + "scriptPath='"
                   + scriptFile
                   + '\''
                   + '}';
        }

        /**
         * {@code Location} builder static inner class.
         */
        public static final class Builder {

            private String currentDirectory;
            private String scriptDirectory;
            private String scriptFile;

            private Builder() {
            }

            /**
             * Sets the path to the directory used for resolving paths in the {@code OutputAST} and returns a reference to this
             * Builder so that the methods can be chained together.
             *
             * @param currentDirectory the {@code currentDirectory} to set
             * @return a reference to this Builder
             */
            public Builder currentDirectory(String currentDirectory) {
                this.currentDirectory = currentDirectory;
                return this;
            }

            /**
             * Sets the path to the directory of the current descriptor script and returns a reference to this Builder so that
             * the methods can be chained
             * together.
             *
             * @param scriptDirectory the {@code scriptDirectory} to set
             * @return a reference to this Builder
             */
            public Builder scriptDirectory(String scriptDirectory) {
                this.scriptDirectory = scriptDirectory;
                return this;
            }

            /**
             * Sets the path to the descriptor script file and returns a reference to this Builder so that
             * the methods can be chained
             * together.
             *
             * @param scriptFile the {@code scriptDirectory} to set
             * @return a reference to this Builder
             */
            public Builder scriptFile(String scriptFile) {
                this.scriptFile = scriptFile;
                return this;
            }

            /**
             * Returns a {@code Location} built from the parameters previously set.
             *
             * @return a {@code Location} built with parameters of this {@code Builder}
             */
            public Location build() {
                if (currentDirectory == null) {
                    currentDirectory = "";
                }
                if (scriptDirectory == null) {
                    scriptDirectory = "";
                }
                if (scriptFile == null) {
                    scriptFile = "";
                }
                return new Location(currentDirectory, scriptDirectory, scriptFile);
            }
        }
    }
}
