/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

package io.helidon.build.cli.impl;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.build.archetype.engine.ArchetypeCatalog;
import io.helidon.build.cli.impl.InitCommand.Flavor;
import io.helidon.build.util.Log;
import io.helidon.build.util.NetworkConnection;
import io.helidon.build.util.Requirements;

import static io.helidon.build.cli.impl.CommandRequirements.requireSupportedHelidonVersion;
import static io.helidon.build.cli.impl.Config.userConfig;

/**
 * Class ArchetypeBrowser.
 */
class ArchetypeBrowser {

    /**
     * Maven remote repo.
     */
    static final String REMOTE_REPO = "https://repo.maven.apache.org/maven2";

    /**
     * Maven local repo. Mostly a convenience to find unreleased versions; if not
     * found or not available a warning is displayed.
     */
    static final String LOCAL_REPO = "file://" + System.getProperty("user.home") + "/.m2/repository";

    /**
     * Archetype directory.
     */
    private static final String CATALOG_GROUP_ID = "io.helidon.archetypes";

    /**
     * The catalog artifactId.
     */
    private static final String CATALOG_ARTIFACT_ID = "helidon-archetype-catalog";

    /**
     * Helidon version not found message.
     */
    private static final String HELIDON_VERSION_NOT_FOUND = "$(red Helidon version) $(RED %s) $(red not found.)";

    /**
     * Archetype not found message.
     */
    static final String ARCHETYPE_NOT_FOUND = "$(red Archetype \")$(RED %s)$(red \" not found.)";

    /**
     * Download failed message.
     */
    private static final String DOWNLOAD_FAILED = "Unable to download %s from %s";

    /**
     * Snapshot version suffix.
     */
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    /**
     * Reusable byte buffer.
     */
    private static final byte[] BUFFER = new byte[8 * 1024];

    private final Path localCacheDir;
    private final Flavor flavor;
    private final String helidonVersion;

    ArchetypeBrowser(Flavor flavor, String helidonVersion) {
        this.flavor = flavor;
        this.helidonVersion = requireSupportedHelidonVersion(helidonVersion);
        this.localCacheDir = userConfig().cacheDir();  // $HOME/.helidon/cache
    }

    /**
     * Returns list of archetypes available. Checks remote repo if local cache
     * does not include a catalog file. For convenience, it also checks the local
     * Maven repo to support unreleased versions.
     *
     * @return List of available archetype.
     */
    List<ArchetypeCatalog.ArchetypeEntry> archetypes() {
        List<ArchetypeCatalog.ArchetypeEntry> archetypes = archetypesLocalCache();
        if (archetypes.isEmpty()) {
            downloadCatalog(REMOTE_REPO);
            archetypes = archetypesLocalCache();
            if (archetypes.isEmpty()) {
                downloadCatalog(LOCAL_REPO);
                archetypes = archetypesLocalCache();
                return archetypes;
            }
        }
        return archetypes;
    }

    /**
     * Returns path to archetype jar in local cache or {@code null} if not found
     * and not available for download. Checks remote and then local repo to
     * handle unreleased versions.
     *
     * @param archetype The archetype.
     * @return Path to archetype jar or {@code null} if not found.
     */
    Path archetypeJar(ArchetypeCatalog.ArchetypeEntry archetype) {
        Path jar = downloadJar(REMOTE_REPO, archetype);
        return jar == null ? downloadJar(LOCAL_REPO, archetype) : jar;
    }

    /**
     * Downloads jar into the local cache and returns path to it.
     *
     * @param repo The repository to use.
     * @param archetype archetype to download.
     * @return Path to local jar or {code null} if unable.
     */
    private Path downloadJar(String repo, ArchetypeCatalog.ArchetypeEntry archetype) {
        String filename = String.format("%s-%s.jar", archetype.artifactId(), archetype.version());
        Path localJarPath = localCacheDir.resolve(filename);
        if (!localJarPath.toFile().exists()) {
            String location;
            try {
                // Attempt download of artifact
                location = String.format("%s/%s/%s/%s/%s", repo, archetype.groupId().replaceAll("\\.", "/"),
                        archetype.artifactId(), helidonVersion, filename);
                downloadArtifact(new URL(location), localJarPath);
            } catch (ConnectException | FileNotFoundException e) {
                downloadFailed(archetype.name(), filename, repo, e);
                return null;
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        }
        return localJarPath;
    }

    /**
     * Searches for archetypes in the local cache by inspecting the corresponding POM file.
     *
     * @return List of archetype entries from the local catalog.
     */
    List<ArchetypeCatalog.ArchetypeEntry> archetypesLocalCache() {
        String catalogFilename = String.format("%s-%s.xml", CATALOG_ARTIFACT_ID, helidonVersion);
        Path path = localCacheDir.resolve(catalogFilename);
        if (Files.exists(path)) {
            try {
                return ArchetypeCatalog.read(Files.newInputStream(path))
                                       .entries()
                                       .stream()
                                       .filter(a -> a.tags().contains(flavor.toString().toLowerCase()))
                                       .collect(Collectors.toList());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Downloads the catalog into local cache.
     */
    private void downloadCatalog(String repo) {
        String catalogFilename = String.format("%s-%s.xml", CATALOG_ARTIFACT_ID, helidonVersion);
        Path localCatalogPath = localCacheDir.resolve(catalogFilename);
        String location = String.format("%s/%s/%s/%s/%s", repo, CATALOG_GROUP_ID.replaceAll("\\.", "/"),
                CATALOG_ARTIFACT_ID, helidonVersion, catalogFilename);
        try {
            URL url = new URL(location);
            downloadArtifact(url, localCatalogPath);
        } catch (ConnectException | FileNotFoundException e) {
            downloadFailed(null, catalogFilename, repo, e);
            // Falls through
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Downloads a repository artifact into the local cache.
     *
     * @param url Location of remote artifact.
     * @param localPath Local path.
     * @throws IOException If error occurs.
     */
    void downloadArtifact(URL url, Path localPath) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(open(url))) {
            try (FileOutputStream fos = new FileOutputStream(localPath.toFile())) {
                int n;
                while ((n = bis.read(BUFFER, 0, BUFFER.length)) >= 0) {
                    fos.write(BUFFER, 0, n);
                }
            }
        }
    }

    /**
     * Open the connect for the given URL.
     *
     * @param url the URL
     * @return InputStream
     * @throws IOException if an IO error occurs
     */
    private InputStream open(URL url) throws IOException {
        return NetworkConnection.builder()
                                .url(url)
                                .open();
    }

    /**
     * Handle download failed error.
     *
     * @param archetypeId The archetype id.
     * @param file The file.
     * @param repo The repo.
     * @param error The error
     */
    private void downloadFailed(String archetypeId, String file, String repo, Throwable error) {
        boolean local = repo.equals(LOCAL_REPO);
        boolean notFound = error instanceof FileNotFoundException;
        boolean snapshot = helidonVersion.endsWith(SNAPSHOT_SUFFIX);
        Log.Level level = local || snapshot ? Log.Level.DEBUG : Log.Level.WARN;
        Log.log(level, error, DOWNLOAD_FAILED, file, repo);
        if (notFound && local) {
            if (archetypeId == null) {
                Requirements.failed(HELIDON_VERSION_NOT_FOUND, helidonVersion);
            } else {
                Requirements.failed(ARCHETYPE_NOT_FOUND, archetypeId);
            }
        }
    }
}
