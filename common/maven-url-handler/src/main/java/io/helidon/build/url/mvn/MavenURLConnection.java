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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Maven URL Connection.
 */
public class MavenURLConnection extends URLConnection {

    private final MavenFileResolver resolver = new MavenFileResolver();
    private final MavenUrlParser parser;
    private final Path artifactFile;

    /**
     * Constructs a URL connection to the specified URL. A connection to
     * the object referenced by the URL is not created.
     *
     * @param url the specified URL.
     */
    public MavenURLConnection(URL url) throws IOException {
        super(url);
        Objects.requireNonNull(url, "URL provided is null");
        this.parser = new MavenUrlParser(url.toExternalForm());
        this.artifactFile = resolver.resolveArtifactPath(parser);
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return resolver.getInputStream(artifactFile, parser.pathFromArchive());
    }

    /**
     * Get the absolute Path of the file targeted by url.
     *
     * @return Path
     */
    public Path artifactFile() {
        return artifactFile;
    }

    /**
     * Get the path of the file targeted by url.
     *
     * @return Path
     */
    public String pathFromArchive() {
        return parser.pathFromArchive();
    }

    /**
     * Get groupId.
     *
     * @return groupId
     */
    public String groupId() {
        return parser.groupId();
    }

    /**
     * Get artifactId.
     *
     * @return artifactId
     */
    public String artifactId() {
        return  parser.artifactId();
    }

    /**
     * Get classifier.
     *
     * @return classifier
     */
    public Optional<String> classifier() {
        return parser.classifier();
    }

    /**
     * Get version.
     *
     * @return version
     */
    public String version() {
        return parser.version();
    }

    /**
     * Get type.
     *
     * @return type
     */
    public String type() {
        return parser.type();
    }

    static class MavenFileResolver {

        private static final String SYSTEM_PROPERTY_LOCAL_REPO = "io.helidon.mvn.local.repository";

        MavenFileResolver() {
        }

        private Path resolveArtifactPath(MavenUrlParser parser) throws IOException {
            String fileName = parser.artifactId() + "-" + parser.version() + "." + parser.type();
            Path filePath = Path.of(getLocalRepository().getAbsolutePath());

            for (String element : parser.archivePath()) {
                filePath = filePath.resolve(element);
            }
            filePath = filePath.resolve(fileName);

            if (!filePath.toFile().exists()) {
                throw new IOException(String.format("File %s does not exist.", filePath));
            }

            return filePath;
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
         * Return {@link InputStream} of the target file.
         *
         * @param archivePath   Archive file path
         * @param path          relative path from archive
         * @return              the file targeted by the path
         * @throws IOException  if file is not present
         */
        private InputStream getInputStream(Path archivePath, String path) throws IOException {
            ZipFile zip = new ZipFile(archivePath.toFile());
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equals(path)) {
                    return zip.getInputStream(entry);
                }
            }
            throw new IOException(String.format("File %s was not found into archive : %s.", path, zip));
        }
    }
}
