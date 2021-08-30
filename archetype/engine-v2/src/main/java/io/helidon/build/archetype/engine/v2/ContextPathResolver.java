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
package io.helidon.build.archetype.engine.v2;

import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for resolving Context Path.
 */
public class ContextPathResolver {

    private static String[] prefix;
    private ContextNode currentNode;

    /**
     * Default constructor.
     */
    public ContextPathResolver() {
    }

    /**
     * Resolve path to specific absolute path.
     *
     * @param prefix    common prefix
     * @param path      relative path
     * @return          path from the root
     */
    public static String resolvePathWithPrefix(String prefix, String path) {
        if (path == null || path.isEmpty())  {
            throw new InvalidPathException("", "path is null or empty");
        }
        setPrefixPath(prefix);
        String[] rootPath = buildAbsolutePathWithPrefix(path.split("\\."));
        return convertArrayToStringPath(rootPath);
    }

    /**
     * Resolve path with specified prefix.
     *
     * @param rootNode  Root context node
     * @param prefix    Common prefix
     * @param path      Path to desired node
     * @return          The desired node
     */
    public ContextNode resolvePath(ContextNode rootNode, String prefix, String path) {
        if (rootNode == null || path == null) {
            throw new InvalidPathException("", "context node or path is null");
        }
        setRoot(rootNode);
        setPrefixPath(prefix);
        return resolvePath(path);
    }

    /**
     * Resolve path without prefix.
     *
     * @param rootNode  Starting point for node search.
     * @param path      Path to desired node.
     * @return          The desired node.
     */
    public ContextNode resolvePath(ContextNode rootNode, String path) {
        return resolvePath(rootNode, null, path);
    }

    /**
     * Resolve path to targeted node.
     *
     * @param path  path to the node
     * @return      the context node pointed by the path
     */
    private ContextNode resolvePath(String path) {
        if (path == null || path.isEmpty())  {
            throw new InvalidPathException("", "path is null or empty");
        }
        if (currentNode == null) {
            throw new NullPointerException("The root node is not set.");
        }
        String[] rootPath = buildRelativePath(path.split("\\."));
        return seekContextNode(rootPath, currentNode);
    }

    private static ContextNode seekContextNode(String[] path, ContextNode node) {
        int pathIx = 0;
        int childrenIx = 0;
        boolean found = false;

        if (!path[pathIx++].equals(node.name())) {
            throw new InvalidPathException(path[pathIx - 1], "Invalid path from root : " + node.name());
        }

        while (pathIx < path.length) {
            while (childrenIx < node.children().size()) {
                if (path[pathIx].equals(node.children().get(childrenIx).name())) {
                    node = node.children().get(childrenIx);
                    found = true;
                    break;
                }
                childrenIx++;
            }
            childrenIx = 0;
            if (!found || (node.children() == null && pathIx < path.length - 1)) {
                throw new InvalidPathException(
                        convertArrayToStringPath(path, pathIx + 1), "Invalid path, cannot find children");
            }
            found = false;
            pathIx++;
        }
        return node;
    }

    private static String[] buildAbsolutePathWithPrefix(String[] path) {
        if (path[0].equals("ROOT") || path[0].equals("PARENT")) {
            throw new InvalidPathException(path[0], "Cannot resolve these keyword: ");
        }

        if (prefix == null) {
            return path;
        }

        int prefixIx = 0;
        int pathIx = 0;
        int limitPrefix = prefix.length;
        int limitPath = path.length;

        if (!prefix[0].equals(path[0])) {
            return mergeArray(prefix, path);
        }

        while (prefixIx < limitPrefix && pathIx < limitPath) {
            if (!path[pathIx].equals(prefix[prefixIx]) && prefixIx != 0){
                throw new InvalidPathException(convertArrayToStringPath(path), "Invalid path");
            }
            prefixIx++;
            pathIx++;
        }
        return path;
    }

    private String[] buildRelativePath(String[] path) {
        if (path[0].equals("ROOT")) {
            path = Arrays.copyOfRange(path, 1, path.length);
            String[] rootPath = lookForRoot();
            if (rootPath == null) {
                return path;
            }
            return mergeArray(rootPath, path);
        }

        if (path[0].equals("PARENT")) {
            path = Arrays.copyOfRange(path, 1, path.length);
            String[] parentPath = lookForParent();
            currentNode = currentNode.parent();
            return mergeArray(parentPath, path);
        }
        return path;
    }

    /**
     * Look for root from current node and return its path.
     *
     * @return path from root to current node
     */
    private String[] lookForRoot() {
        if (currentNode.parent() == null) {
            return null;
        }

        ContextNode node = currentNode;
        List<String> path = new ArrayList<>();
        while (node.parent() != null) {
            path.add(node.parent().name());
            node = node.parent();
        }
        currentNode = node;
        Collections.reverse(path);
        return path.toArray(new String[0]);
    }

    /**
     * Return parent name.
     *
     * @return Path as String array
     */
    private String[] lookForParent() {
        if (currentNode.parent() == null)  {
            throw new NullPointerException("Current node has no parent, wrong path");
        }
        return new String[]{currentNode.parent().name()};
    }

    private static String convertArrayToStringPath(String[] array, int limit) {
        if (array == null || limit < 1) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(limit, array.length); i++) {
            if (array[i] == null) {
                continue;
            }
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(".");
            }
        }
        return sb.toString();
    }

    private static String convertArrayToStringPath(String[] array) {
        return convertArrayToStringPath(array, array.length);
    }

    /**
     * Set the prefix path from which relative path will be resolved on.
     *
     * @param path prefix path
     */
    private static void setPrefixPath(String path) {
        if (path != null) {
            prefix = path.split("\\.");
        }
    }

    /**
     * Set the root node.
     *
     * @param node node to be considered as root
     */
    private void setRoot(ContextNode node) {
        this.currentNode = node;
    }

    private static String[] mergeArray(String[] first, String[] second) {
        String[] result = new String[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
