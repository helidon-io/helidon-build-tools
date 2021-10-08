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

package io.helidon.build.url.mvn;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Maven Url Stream Handler.
 */
public class Handler extends URLStreamHandler {

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
        protected MavenURLConnection(URL url, MavenFileResolver resolver) throws IOException {
            super(url);
            Objects.requireNonNull(url, "URL provided is null");
            Objects.requireNonNull(resolver, "Maven resolver provided is null");
            resolver.resolve(url.toExternalForm());
            this.resolver = resolver;
        }

        @Override
        public void connect() throws IOException {
        }

        /**
         * Get the absolute Path of the file targeted by url.
         *
         * @return Path
         */
        public Path artifactFile() {
            return resolver.artifactFile();
        }

        /**
         * Get groupId.
         *
         * @return groupId
         */
        public String groupId() {
            return resolver.groupId();
        }

        /**
         * Get artifactId.
         *
         * @return artifactId
         */
        public String artifactId() {
            return  resolver.artifactId();
        }

        /**
         * Get classifier.
         *
         * @return classifier
         */
        public Optional<String> classifier() {
            return resolver.classifier();
        }

        /**
         * Get version.
         *
         * @return version
         */
        public String version() {
            return resolver.version();
        }

        /**
         * Get type.
         *
         * @return type
         */
        public String type() {
            return resolver.type();
        }
    }

    static class MavenFileResolver {

        private static final String PROTOCOL = "mvn";
        private static final String SYSTEM_PROPERTY_LOCAL_REPO = "io.helidon.mvn.local.repository";
        private MavenUrlParser parser;
        private Path artifactFile;

        /**
         *  Default Constructor.
         */
        MavenFileResolver() {
        }

        /**
         * Resolve maven artifact in local repository.
         *
         * @param url url pointing at the target file
         *
         * @throws MalformedURLException throw MalformedURLException if url disrespect the syntax
         */
        void resolve(String url) throws IOException {
            if (!url.startsWith(PROTOCOL + ":")) {
                throw new IllegalArgumentException("url should be a mvn based url");
            }
            url = url.substring((PROTOCOL + "://").length());
            parser = new MavenUrlParser(url);
            artifactFile = resolve(parser);
        }

        private Path resolve(MavenUrlParser parser) throws IOException {
            String fileName = parser.artifactId() + "-" + parser.version() + "." + parser.type();
            FileVisitor visitor = new FileVisitor(getLocalRepository());
            visitor.visit(parser.archivePath());
            return visitor.visitArchive(fileName, parser.pathFromArchive());
        }

        private File getLocalRepository() throws IOException {
            String local = System.getProperty(SYSTEM_PROPERTY_LOCAL_REPO);
            if (local == null) {
                throw new IOException(String.format("System property %s is not set.", SYSTEM_PROPERTY_LOCAL_REPO));
            }
            File file = new File(local);
            if (file.exists() && file.isDirectory()) {
                return file;
            }
            throw new IOException(String.format("Directory %s does not exist.", local));
        }

        /**
         * Get Path to the url target file.
         *
         * @return path
         */
        Path artifactFile() {
            return artifactFile;
        }

        /**
         * Returns the group id of the artifact.
         *
         * @return group Id
         */
        String groupId() {
            return parser.groupId();
        }

        /**
         * Returns the artifact id.
         *
         * @return artifact id
         */
        String artifactId() {
            return parser.artifactId();
        }

        /**
         * Returns the artifact classifier.
         *
         * @return classifier
         */
        Optional<String> classifier() {
            return parser.classifier();
        }

        /**
         * Returns the artifact version.
         *
         * @return version
         */
        String version() {
            return parser.version();
        }

        /**
         * Returns the artifact type.
         *
         * @return type
         */
        String type() {
            return parser.type();
        }

        static class FileVisitor {

            private Path fromRoot;
            private File currentFile;
            private File[] files;

            /**
             * Constructor to set the root directory/file.
             *
             * @param file root
             */
            FileVisitor(File file) throws IOException {
                if (!file.isDirectory() || !file.exists()) {
                    throw new IOException(String.format("Directory %s is not a directory or it does not exist", file.getName()));
                }
                this.currentFile = file;
                this.fromRoot = file.toPath();
                files = file.listFiles();
            }

            /**
             * Visit the target file or directory.
             *
             * @param targets       path to target file to be resolved
             * @throws IOException  if wrong path
             */
            public void visit(String[] targets) throws IOException {
                for (String directory : targets) {
                    visit(directory);
                }
            }

            private File visit(String target) throws IOException {
                if (files == null)  {
                    throw new IOException(String.format("Empty directory at path : %s.", currentFile.getAbsolutePath()));
                }

                Optional<File> resolved = Arrays.stream(files)
                        .filter(f -> f.getName().equals(target))
                        .findFirst();
                if (resolved.isEmpty()) {
                    throw new IOException("File or directory not found : " + target);
                }

                currentFile = resolved.get();
                files = currentFile.listFiles();
                fromRoot = fromRoot.resolve(resolved.get().getName());
                return resolved.get();
            }

            /**
             * Visit a archive.
             *
             * @param zipName       Jar file name.
             * @param path          path to a file into the jar file.
             * @return              the file targeted by the path.
             * @throws IOException  if file is not present.
             */
            public Path visitArchive(String zipName, String path) throws IOException {
                Enumeration<? extends ZipEntry> entries = new ZipFile(visit(zipName)).entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.equals(path)) {
                        fromRoot = fromRoot.resolve(name);
                        return fromRoot;
                    }
                }
                throw new IOException(String.format("File %s was not found into archive : %s.", path, zipName));
            }
        }
    }
}
