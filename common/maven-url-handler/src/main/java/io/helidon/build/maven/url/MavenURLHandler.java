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

package io.helidon.build.maven.url;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Maven Url Stream Handler.
 */
class MavenURLHandler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        MavenFileResolver resolver = new MavenFileResolver();
        return new MavenURLConnection(url, resolver);
    }

    @Override
    protected void parseURL(URL u, String spec, int start, int limit) {
        super.setURL(u, null, null, 0,  null, null, spec.substring(4), null, null);
    }

    static class MavenURLConnection extends URLConnection {

        private final MavenFileResolver resolver;

        /**
         * Constructs a URL connection to the specified URL. A connection to
         * the object referenced by the URL is not created.
         *
         * @param url       the specified URL.
         * @param resolver  resolver to use to resolve url.
         */
        protected MavenURLConnection(URL url, MavenFileResolver resolver) {
            super(url);
            Objects.requireNonNull(url, "URL provided is null");
            Objects.requireNonNull(resolver, "Maven resolver provided is null");
            this.resolver = resolver;
        }

        @Override
        public void connect() throws IOException {
        }

        /**
         * Get the file input stream targeted by the url.
         */
        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            File file = resolver.resolve(url.toExternalForm());
            return new FileInputStream(file);
        }
    }

    static class MavenFileResolver {

        private static final String PROTOCOL = "mvn";
        private static final String SYSTEM_PROPERTY_LOCAL_REPO = "io.helidon.mvn.local.repository";

        /**
         *  Default Constructor.
         */
        MavenFileResolver() {
        }

        /**
         * Resolve maven artifact as file in local repository.
         *
         * @param url               url pointing at target file
         * @return                  targeted file
         * @throws IOException      throw IOException if not present or wrong path
         */
        File resolve(String url) throws IOException {
            if (!url.startsWith(PROTOCOL + ":")) {
                throw new IllegalArgumentException("url should be a mvn based url");
            }
            url = url.substring((PROTOCOL + "://").length());
            MavenURLParser parser = new MavenURLParser(url);

            return resolve(parser, parser.getType());
        }

        private File resolve(MavenURLParser parser, String type) throws IOException {
            String fileName = parser.getArtifactId() + "-" + parser.getVersion() + "." + parser.getType();
            FileVisitor visitor = new FileVisitor(getLocalRepository());
            visitor.visit(parser.getArchivePath());
            return type.equals("jar")
                    ? visitor.visitJar(fileName, parser.getPathFromArchive())
                    : visitor.visitZip(fileName, parser.getPathFromArchive());
        }

        private File getLocalRepository() throws IOException {
            String local = System.getProperty(SYSTEM_PROPERTY_LOCAL_REPO);
            if (local == null) {
                throw new IOException(String.format("System property %s is not set.", SYSTEM_PROPERTY_LOCAL_REPO));
            }
            return new File(local);
        }

        static class FileVisitor {

            private File currentFile;
            private File[] files;

            /**
             * Constructor to set the root directory/file.
             *
             * @param file root
             */
            FileVisitor(File file) {
                this.currentFile = file;
                files = resolveFiles(file);
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
                    throw new IOException(String.format("Empty file at path : %s.", currentFile.getAbsolutePath()));
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
                throw new IOException(String.format("File %s was not found into jar : %s.", path, jarName));
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
                throw new IOException(String.format("File %s was not found into zip : %s.", path, zipName));
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

            private File[] resolveFiles(File file) {
                return file.listFiles();
            }
        }

    }

    static class MavenURLParser {

        /**
         * Syntax for the url to be shown on exception messages.
         */
        private static final String SYNTAX =
                "mvn://groupId:artifactId:[version]:[classifier (optional)]:[type (optional)]/.../[file]";

        /**
         * Final artifact path separator.
         */
        private static final String FILE_SEPARATOR = "/";

        /**
         * Default artifact type.
         */
        private static final String DEFAULT_TYPE = "jar";

        /**
         * List of supported artifact types.
         */
        private static final List<String> SUPPORTED_TYPE = Arrays.asList(DEFAULT_TYPE, "zip");

        /**
         * Artifact group id.
         */
        private String groupId;

        /**
         * Artifact id.
         */
        private String artifactId;

        /**
         * Artifact version.
         */
        private String version = "LATEST";

        /**
         * Artifact classifier.
         */
        private String classifier = null;

        /**
         * Artifact type.
         */
        private String type = "jar";

        /**
         * Path from version directory to target file.
         */
        private String pathFromArchive;

        /**
         * Creates a new protocol parser.
         *
         * @param path                   the path part of the url (without starting mvn://)
         *
         * @throws MalformedURLException if provided path does not comply to expected syntax or an malformed repository URL
         */
        MavenURLParser(String path) throws MalformedURLException {
            Objects.requireNonNull(path, "Maven url provided to Parser is null");
            parseArtifactPart(path);
        }

        /**
         * Parses the artifact part of the url.
         *
         * @param part                   url part without protocol and repository.
         *
         * @throws MalformedURLException if provided path does not comply to syntax.
         */
        private void parseArtifactPart(String part) throws MalformedURLException {
            String[] segments = part.split(":");

            if (segments.length > 2) {
                groupId = segments[0];
                checkStringHealth(groupId, "groupId");

                artifactId = segments[1];
                checkStringHealth(artifactId, "artifactId");
            } else {
                throw new MalformedURLException("Missing element in maven URL. Syntax " + SYNTAX);
            }

            switch (segments.length) {
                case 3:
                    parseNoClassifierOrType(segments);
                    break;
                case 4:
                    parseWithClassifierOrType(segments);
                    break;
                case 5:
                    parseCompleteUrl(segments);
                    break;
                default:
                    throw new MalformedURLException("Invalid path. Syntax " + SYNTAX);
            }
        }

        private void parseCompleteUrl(String[] segments) throws MalformedURLException {
            if (segments.length != 5) {
                throw new MalformedURLException("Invalid. Syntax " + SYNTAX);
            }
            version = segments[2];
            checkStringHealth(version, "version");

            classifier = segments[3];
            checkStringHealth(classifier, "classifier");

            segments = segments[4].split(FILE_SEPARATOR);

            type = checkTypeHealth(segments[0]) ? segments[0] : DEFAULT_TYPE;
            checkStringHealth(type, "type");

            buildPathFromArchive(segments);
        }

        private void parseWithClassifierOrType(String[] segments) throws MalformedURLException {
            if (segments.length != 4) {
                throw new MalformedURLException("Invalid. Syntax " + SYNTAX);
            }

            version = segments[2];
            checkStringHealth(version, "version");

            segments = segments[3].split(FILE_SEPARATOR);

            if (checkTypeHealth(segments[0])) {
                type = segments[0];
            } else {
                classifier = segments[0];
            }

            buildPathFromArchive(segments);
        }

        private void parseNoClassifierOrType(String[] segments) throws MalformedURLException {
            if (segments.length != 3) {
                throw new MalformedURLException("Invalid. Syntax " + SYNTAX);
            }
            segments = segments[2].split(FILE_SEPARATOR);

            version = segments[0];
            checkStringHealth(version, "version");

            buildPathFromArchive(segments);
        }

        private void buildPathFromArchive(String[] segments) {
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i < segments.length - 1; i++) {
                builder.append(segments[i]).append("/");
            }
            builder.append(segments[segments.length - 1]);
            pathFromArchive = builder.toString();
        }

        private void checkStringHealth(String patient, String patientName) throws MalformedURLException {
            if (patient.trim().length() == 0) {
                throw new MalformedURLException(String.format("Invalid %s. Syntax  %s. ", patientName, SYNTAX));
            }
        }

        private boolean checkTypeHealth(String type) {
            return SUPPORTED_TYPE.contains(type);
        }

        /**
         * Returns the group id of the artifact.
         *
         * @return group Id
         */
        public String getGroupId() {
            return groupId;
        }

        /**
         * Returns the artifact id.
         *
         * @return artifact id
         */
        public String getArtifactId() {
            return artifactId;
        }

        /**
         * Returns the artifact version.
         *
         * @return version
         */
        public String getVersion() {
            return version;
        }

        /**
         * Returns the artifact classifier.
         *
         * @return classifier
         */
        public String getClassifier() {
            return classifier;
        }

        /**
         * Returns the artifact type.
         *
         * @return type
         */
        public String getType() {
            return type;
        }

        /**
         * Return file path from version directory.
         *
         * @return file path
         */
        public String getPathFromArchive() {
            return pathFromArchive;
        }

        /**
         * Get full path from groupId to file.
         *
         * @return full path
         */
        public String[] getArchivePath() {
            ArrayList<String> path = new ArrayList<>(Arrays.asList(groupId.split("\\.")));
            path.add(artifactId);
            path.add(version);
            if (classifier != null) {
                path.add(classifier);
            }
            String[] pathArray = new String[path.size()];
            return path.toArray(pathArray);
        }

    }

}
