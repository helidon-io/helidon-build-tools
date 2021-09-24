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

package io.helidon.build.archetype.maven.url.mvn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Directory/File Visitor.
 */
public class Visitor {

    private File currentFile;
    private File[] files;

    /**
     * Constructor to set the root directory/file.
     *
     * @param directory root
     */
    Visitor(File directory) {
        this.currentFile = directory;
        files = resolveFiles(directory);
    }

    /**
     * Visit the target file or directory.
     *
     * @param targets       path to target file to be resolved
     * @return              target file
     * @throws IOException  if wrong path
     */
    public File visit(String[] targets) throws IOException {
        for (String directory : targets) {
            visit(directory);
        }
        return currentFile;
    }

    private File visit(String target) throws IOException {
        AtomicReference<File> resolved = new AtomicReference<>();
        if (files == null)  {
            throw new IOException("Empty file at path : " + currentFile.getAbsolutePath());
        }
        Arrays.stream(files).forEach(file -> {
            if (file.getName().equals(target)) {
                resolved.set(file);
            }
        });
        if (resolved.get() == null) {
            throw new IOException("File or directory not found : " + target);
        }
        currentFile = resolved.get();
        files = resolveFiles(currentFile);
        return resolved.get();
    }

    /**
     * Visit a jar file.
     *
     * @param jarName       Jar file name.
     * @param path          path to a file into the jar file.
     * @return              the file targeted by the path.
     * @throws IOException  if file is not present.
     */
    public File visitJar(String jarName, String path) throws IOException {
        File jar = visit(jarName);
        File outDirectory = new File(jar.getParent());
        JarFile jarFile = new JarFile(jar);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            File out = getFileFromArchive(path, outDirectory, jarFile, entry);
            if (out != null) {
                return out;
            }
        }
        throw new IOException("File " + path + " was not found into jar : " + jarName);
    }

    /**
     * Visit a zip file.
     *
     * @param zipName       Jar file name.
     * @param path          path to a file into the jar file.
     * @return              the file targeted by the path.
     * @throws IOException  if file is not present.
     */
    public File visitZip(String zipName, String path) throws IOException {
        File zip = visit(zipName);
        File outDirectory = new File(zip.getParent());
        ZipFile zipFile = new JarFile(zip);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File out = getFileFromArchive(path, outDirectory, zipFile, entry);
            if (out != null) {
                return out;
            }
        }
        throw new IOException("File " + path + " was not found into zip : " + zipName);
    }

    private File getFileFromArchive(String path, File directory, ZipFile zipFile, ZipEntry entry) throws IOException {
        String name = entry.getName();
        if (name.equals(path)) {
            InputStream is = zipFile.getInputStream(entry);
            File out = createFile(directory, name);
            FileOutputStream fos = new FileOutputStream(out);
            int c;
            while ((c = is.read()) != -1) {
                fos.write(c);
            }
            fos.close();
            is.close();
            return out;
        }
        return null;
    }

    private File createFile(File directory, String name) throws IOException {
        Files.createDirectories(directory.toPath().resolve(name).getParent());
        Files.write(directory.toPath().resolve(name), new byte[0], StandardOpenOption.CREATE);
        return new File(directory.toPath().resolve(name).toString());
    }

    private File[] resolveFiles(File directory) {
        return directory.listFiles();
    }

}
