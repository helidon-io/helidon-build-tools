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
import java.util.LinkedList;

/**
 * Utility class for resolving Context Path.
 */
public class ContextPathResolver {

    private static String[] prefix;
    private static ContextNode root;
    private static final LinkedList<NodeWrapper> STACK_NODE = new LinkedList<>();
    private static final StringBuilder PATH_BUILDER = new StringBuilder();

    private ContextPathResolver() {
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
        String[] rootPath = buildAbsolutePath(path.split("\\."));
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
    public static ContextNode resolvePath(ContextNode rootNode, String prefix, String path) {
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
    public static ContextNode resolvePath(ContextNode rootNode, String path) {
        return resolvePath(rootNode, null, path);
    }

    /**
     * Resolve path to targeted node.
     *
     * @param path  path to the node
     * @return      the context node pointed by the path
     */
    private static ContextNode resolvePath(String path) {
        if (path == null || path.isEmpty())  {
            throw new InvalidPathException("", "path is null or empty");
        }
        String[] rootPath = buildAbsolutePath(path.split("\\."));
        return seekContextNode(rootPath, root);
    }

    private static ContextNode seekContextNode(String[] path, ContextNode node) {
        if (path == null || path.length == 0) {
            throw new InvalidPathException("", "path is null or empty");
        }

        int pathIx = 0;
        int childrenIx = 0;
        boolean found = false;

        if (!path[pathIx++].equals(node.name())) {
            throw new InvalidPathException(path[pathIx], "Invalid path");
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

    private static String[] buildAbsolutePath(String[] path) {
        path = parsePath(path);

        if (prefix == null) {
            return path;
        }

        int prefixIx = 0;
        int pathIx = 0;
        int limitPrefix = prefix.length;
        int limitPath = path.length;

        if (!prefix[0].equals(path[0])) {
            return mergeArray(path, prefix);
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

    private static String[] parsePath(String[] path) {
        if (path == null || path.length == 0) {
            return null;
        }
        if (path[0].equals("ROOT")) {
            String[] result = new String[path.length - 1];
            System.arraycopy(path, 1,  result, 0, path.length - 1);
            return result;
        }
        if (path[0].equals("PARENT")) {
            String[] rootPath = lookForParent(path);
            if (rootPath == null) {
                throw new InvalidPathException(convertArrayToStringPath(path), "Cannot find parent :");
            }
            String[] result = new String[path.length - 2 + rootPath.length];
            System.arraycopy(rootPath, 0, result, 0, rootPath.length);
            System.arraycopy(path, 2, result, rootPath.length, path.length - 2);
            return result;
        }
        return path;
    }

    //Return path from root to parent.
    private static String[] lookForParent(String[] array) {
        if (!array[0].equals("PARENT")) {
            return null;
        }
        String[] path = new String[array.length - 1];
        System.arraycopy(array, 1,  path, 0, array.length - 1);

        NodeWrapper wrapper = new NodeWrapper(root);
        PATH_BUILDER.append(wrapper.name()).append(".");
        STACK_NODE.add(wrapper);

        while (true) {
            if (wrapper.name().equals(path[0])) {
                try {
                    seekContextNode(path, wrapper.node);
                    return PATH_BUILDER.toString().split("\\.");
                } catch (InvalidPathException ignored) {
                }
                break;
            }

            wrapper = STACK_NODE.getLast().nextNode();

            if (wrapper == null) {
                break;
            }
        }
        return null;
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
        if (path == null) {
            return;
        }
        prefix = path.split("\\.");
    }

    /**
     * Set the root node.
     *
     * @param node node to be considered as root
     */
    private static void setRoot(ContextNode node) {
        if (node != null) {
            root = node;
        }
    }

    private static String[] mergeArray(String[] first, String[] second) {
        String[] result = new String[first.length + second.length];
        System.arraycopy(second, 0, result, 0, second.length);
        System.arraycopy(first, 0, result, second.length, first.length);
        return result;
    }

    /**
     * ContextNode wrapper used to reach ContextNode tree.
     * branchIx is a counter to keep track of the next branch to be read.
     * NodeWrapper is currently used to resolve parent paths.
     */
    static class NodeWrapper {

        private int branchIx;
        private final ContextNode node;

        NodeWrapper(ContextNode node) {
            this.branchIx = 0;
            this.node = node;
        }

        private NodeWrapper nextNode() {
            if (node.children() == null || node.children().size() <= branchIx) {
                PATH_BUILDER.delete(PATH_BUILDER.length() - STACK_NODE.getLast().name().length(), PATH_BUILDER.length());
                STACK_NODE.pop();
                return STACK_NODE.getLast().nextNode();
            }
            STACK_NODE.add(new NodeWrapper(node.children().get(branchIx++)));
            PATH_BUILDER.append(STACK_NODE.getLast().name()).append(".");
            return STACK_NODE.getLast();
        }

        private String name() {
            return node.name();
        }
    }

}
