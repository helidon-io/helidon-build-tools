/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.sitegen;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

import io.helidon.sitegen.asciidoctor.AsciidocConverter;

import org.slf4j.LoggerFactory;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A helper class to help with class-path resources.
 *
 * @author rgrecour
 */
public abstract class Helper {

    private static final org.slf4j.Logger LOGGER =
            LoggerFactory.getLogger(Helper.class);

    /**
     * Load a resource directory as a {@link java.nio.file.Path} instance.
     *
     * @param resourcePath the resource path to load
     * @return the created path instance
     * @throws URISyntaxException if the resource URL cannot be converted to a =
     *  URI
     * @throws IOException if an error occurred during {@link FileSystem}
     *  creation
     * @throws IllegalStateException if the resource path is not found, or if
     * the URI scheme is not <code>jar</code> or <code>file</code>
     */
    public static Path loadResourceDirAsPath(String resourcePath)
            throws URISyntaxException, IOException, IllegalStateException {

        // get classloader resource URL
        URL templatesDirURL = AsciidocConverter.class.getResource(resourcePath);
        if (templatesDirURL == null) {
            throw new IllegalStateException("resource not found: "
                    + resourcePath);
        }

        // convert URL to Path
        URI templatesDirURI = templatesDirURL.toURI();
        Path templatesDir;
        switch (templatesDirURI.getScheme()) {
            case "jar":
                FileSystem fs;
                try {
                    fs = FileSystems.newFileSystem(
                            templatesDirURI, Collections.emptyMap());
                } catch (FileSystemAlreadyExistsException ex) {
                    fs = FileSystems.getFileSystem(templatesDirURI);
                }
                String relativePath = templatesDirURI.getSchemeSpecificPart();
                int idx = relativePath.indexOf("!");
                if (idx > 0) {
                    relativePath = relativePath.substring(idx + 1);
                }
                templatesDir = fs.getPath(relativePath);
                break;
            case "file":
                templatesDir = new File(templatesDirURI).toPath();
                break;
            default:
                throw new IllegalStateException(templatesDirURI.toASCIIString()
                        + " expecting jar: or file:");
        }
        return templatesDir;
    }

    /**
     * Return the {@code String} from an {@link Object} instance.
     *
     * @param obj the instance to convert
     * @return the string value if given a {@code String} instance, or null
     * otherwise
     */
    public static String asString(Object obj) {
        if (obj == null || !(obj instanceof String)) {
            return null;
        } else {
            return (String) obj;
        }
    }

    /**
     * Copy static resources into the given output directory.
     *
     * @param resources the path to the resources
     * @param outputdir the target output directory where to copy the files
     * @throws IOException if an error occurred during processing
     */
    public static void copyResources(Path resources, File outputdir)
            throws IOException {

        try {
            Files.walkFileTree(resources, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(
                        Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(
                        Path file, BasicFileAttributes attrs) throws IOException {
                    if (!Files.isDirectory(file)) {
                        String targetRelativePath = resources
                                .relativize(file).toString();
                        Path targetPath = outputdir
                                .toPath().resolve(targetRelativePath);
                        Files.createDirectories(targetPath.getParent());
                        LOGGER.debug("Copying static resource: {} to {}",
                                targetRelativePath, targetPath.toString());
                        Files.copy(file, targetPath, REPLACE_EXISTING);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(
                        Path file, IOException ex) throws IOException {
                    LOGGER.error("Error while copying static resource: {} - {}",
                            file.getFileName(), ex.getMessage());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(
                        Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            throw new RenderingException(
                    "An error occurred during static resource processing ", ex);
        }
    }

    /**
     * Verify that a given {@code Object} is non null.
     *
     * @param arg the {@code Object} instance to check
     * @param name the name of the instance used for the exception message
     * @throws IllegalArgumentException if arg is null
     */
    public static void checkNonNull(Object arg, String name)
        throws IllegalArgumentException {

        if (arg == null) {
            throw new IllegalArgumentException(name + " is null");
        }
    }

    /**
     * Verify that a given {@code String} is non null and non empty.
     *
     * @param arg the {@code String} instance to check
     * @param name the name of the instance used for the exception message
     * @throws IllegalArgumentException if arg is null or empty
     */
    public static void checkNonNullNonEmpty(String arg, String name){
        if (arg == null || arg.isEmpty()) {
            throw new IllegalArgumentException(name + " is null or empty");
        }
    }

    /**
     * Verify that a given {@code File} is non null and exists.
     *
     * @param arg the @{code File} instance to check
     * @param name the name of the instance used for the exception message
     * @throws IllegalArgumentException if arg is null or does not exist
     */
    public static void checkNonNullExistent(File arg, String name){
        if (arg == null) {
            throw new IllegalArgumentException(name + "is null");
        }
        if (!arg.exists()) {
            throw new IllegalArgumentException(
                    arg.getAbsolutePath() + " does not exist");
        }
    }

    /**
     * Verify that a given {@code File} is non null, exists and is a file.
     *
     * @param arg the @{code File} instance to check
     * @param name the name of the instance used for the exception message
     * @throws IllegalArgumentException if arg is null, does not exist, or is not a file
     */
    public static void checkValidFile(File arg, String name){
        checkNonNullExistent(arg, name);
        if (!arg.isFile()) {
            throw new IllegalArgumentException(
                    arg.getAbsolutePath() + " is not a file");
        }
    }

    /**
     * Verify that a given {@code File} is non null, exists and is a directory.
     *
     * @param arg the @{code File} instance to check
     * @param name the name of the instance used for the exception message
     * @throws IllegalArgumentException if arg is null, does not exist, or is not a directory
     */
    public static void checkValidDir(File arg, String name) {
        checkNonNullExistent(arg, name);
        if (!arg.isDirectory()) {
            throw new IllegalArgumentException(
                    arg.getAbsolutePath() + " is not a directory");
        }
    }

    /**
     * Get the file extension in the given file path.
     *
     * @param filepath the file path with an extension
     * @return the file extension
     */
    public static String getFileExt(String filepath){
        int index = filepath.lastIndexOf(".");
        return index < 0 ? null : filepath.substring(index + 1);
    }

    /**
     * Replace the file extension in the given file path.
     *
     * @param filepath the file path to use
     * @param ext the new file extension
     * @return the filepath with the new extension
     */
    public static String replaceFileExt(String filepath, String ext){
        String path = filepath;
        path = path.substring(0, path.lastIndexOf("."));
        return path + ext;
    }

    /**
     * Get the relative path for a given source file within the source directory.
     *
     * @param sourcedir the source directory
     * @param source the source file
     * @return the relative path of the source file
     */
    public static String getRelativePath(File sourcedir, File source) {
        return sourcedir.toURI().relativize(source.toURI()).toString();
    }
}
