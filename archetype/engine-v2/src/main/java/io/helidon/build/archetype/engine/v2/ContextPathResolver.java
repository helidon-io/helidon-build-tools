package io.helidon.build.archetype.engine.v2;

import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.LinkedList;

public class ContextPathResolver {

    private static String[] prefix;
    private static ContextNode root;
    private static final LinkedList<NodeWrapper> stackNode = new LinkedList<>();
    private static final StringBuilder pathBuilder = new StringBuilder();

    /**
     * Resolve path to specific absolute path.
     *
     * @param path  relative path
     * @return      path from the root
     */
    public static String resolveRelativePath(String path) {
        if (path == null || path.isEmpty())  {
            throw new InvalidPathException("", "path is null or empty");
        }
        String[] rootPath = buildAbsolutePath(path.split("\\."));
        return convertArrayToStringPath(rootPath);
    }

    /**
     * Resolve path to targeted node.
     *
     * @param path  path to the node
     * @return      the context node pointed by the path
     */
    public static ContextNode resolvePath(String path) {
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
            if (!found) {
                throw new InvalidPathException(
                        convertArrayToStringPath(path, pathIx+1), "Invalid path, cannot find children");
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
            //TODO: Implementation of MergeTwoArray method
            String[] result = new String[path.length + prefix.length];
            System.arraycopy(prefix, 0, result, 0, prefix.length);
            System.arraycopy(path, 0, result, prefix.length, path.length);
            return result;
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
            System.arraycopy(path, 1,  result, 0, path.length-1);
            return result;
        }
        if (path[0].equals("PARENT")) {
            String[] rootPath = lookForParent(path);
            if (rootPath == null) {
                throw new InvalidPathException(convertArrayToStringPath(path), "Cannot find parent :");
            }
            System.out.println(Arrays.toString(rootPath));
            String[] result = new String[path.length-2 + rootPath.length];
            System.arraycopy(rootPath, 0, result, 0, rootPath.length);
            System.arraycopy(path, 2, result, rootPath.length, path.length-2);
            System.out.println(Arrays.toString(result));
            return result;
        }
        return path;
    }

    //Return path from root to parent.
    private static String[] lookForParent(String[] array) {
        if (!array[0].equals("PARENT")) {
            return null;
        }
        String[] path = new String[array.length-1];
        System.arraycopy(array, 1,  path, 0, array.length-1);

        NodeWrapper wrapper = new NodeWrapper(root);
        pathBuilder.append(wrapper.name()).append(".");
        stackNode.add(wrapper);

        while(true) {
            if (wrapper.name().equals(path[0])) {
                try {
                    seekContextNode(path, wrapper.node);
                    return pathBuilder.toString().split("\\.");
                } catch (InvalidPathException ignored) {
                }
                break;
            }

            wrapper = stackNode.getLast().nextNode();

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
        for(int i = 0; i < Math.min(limit, array.length); i++) {
            sb.append(array[i]);
            if (i < array.length-1) {
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
    public static void setPrefixPath(String path) {
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
    public static void setRoot(ContextNode node) {
        if (node != null) {
            root = node;
        }
    }

    static class NodeWrapper {

        private int branchIx;
        private final ContextNode node;

        NodeWrapper(ContextNode node) {
            this.branchIx = 0;
            this.node = node;
        }

        private NodeWrapper nextNode() {
            if (node.children() == null || node.children().size() <= branchIx) {
                pathBuilder.delete(pathBuilder.length() - stackNode.getLast().name().length(), pathBuilder.length());
                stackNode.pop();
                //TODO: optimise to .nextNode() to avoid double check name, save one loop iteration.
                return stackNode.getLast();
            }
            stackNode.add(new NodeWrapper(node.children().get(branchIx++)));
            pathBuilder.append(stackNode.getLast().name()).append(".");
            return stackNode.getLast();
        }

        private String name() {
            return node.name();
        }
    }

}
