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
    private final String currentDirectory;
    private ASTNode parent;
    private Iterator<Visitable> iterator;

    ASTNode(ASTNode parent, String currentDirectory) {
        this.currentDirectory = currentDirectory;
        this.parent = parent;
    }

    public boolean hasNext() {
        if (iterator == null) {
            iterator = children.iterator();
        }
        return iterator.hasNext();
    }

    public Visitable next() {
        if (iterator == null) {
            iterator = children.iterator();
        }
        return iterator.next();
    }

    /**
     * Path associated with the current node (resolved relative to the archetype root directory).
     *
     * @return currentDirectory
     */
    public String currentDirectory() {
        return currentDirectory;
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
}
