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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.helidon.build.cli.impl.InitCommand.Flavor;
import io.helidon.build.util.Log;

import org.apache.maven.model.Model;

import static io.helidon.build.util.PomUtils.readPomModel;

/**
 * Class ArchetypeBrowser.
 */
class AppTypeBrowser {

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
    private static final String ARCHETYPE_DIRECTORY = "/io/helidon/archetypes";

    /**
     * Prefix for all archetypes.
     */
    private static final String ARCHETYPE_PREFIX = "helidon-archetypes";

    /**
     * Format is helidon-archetype-{flavor}-{apptype}-{version}.jar.
     */
    private static final String APPTYPE_JAR = ARCHETYPE_PREFIX + "-%s-%s-%s.jar";

    /**
     * Format is helidon-archetype-{flavor}-{version}.pom.
     */
    private static final String APPTYPE_POM = ARCHETYPE_PREFIX + "-%s-%s.pom";

    /**
     * Reusable byte buffer.
     */
    private static final byte[] BUFFER = new byte[8 * 1024];

    private final Path localCache;
    private final Flavor flavor;
    private final String helidonVersion;

    AppTypeBrowser(Flavor flavor, String helidonVersion) {
        this.flavor = flavor;
        this.helidonVersion = helidonVersion;
        String userHome = System.getProperty("user.home");
        Objects.requireNonNull(userHome);
        localCache = Path.of(userHome, ".helidon", "cache");        // $HOME/.helidon/cache
        try {
            File file = localCache.toFile();
            if (!file.exists()) {
                file.mkdirs();
                file.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns list of apptypes available. Checks remote repo if local cache
     * does not include a pom file. For convenience, it also checks the local
     * Maven repo to support unreleased versions.
     *
     * @return List of available apptypes.
     */
    List<String> appTypes() {
        List<String> appTypes = appTypesLocalRepo();
        if (appTypes.size() == 0) {
            downloadPom(REMOTE_REPO);
            appTypes = appTypesLocalRepo();
            if (appTypes.size() == 0) {
                downloadPom(LOCAL_REPO);
                appTypes = appTypesLocalRepo();
                return appTypes;
            }
        }
        return appTypes;
    }

    /**
     * Returns path to archetype jar in local cache or {@code null} if not found
     * and not available for download. Checks remote and then local repo to
     * handle unreleased versions.
     *
     * @param appType The application type.
     * @return Path to archetype jar or {@code null} if not found.
     */
    Path archetypeJar(String appType) {
        Path jar = downloadJar(REMOTE_REPO, appType);
        return jar == null ? downloadJar(LOCAL_REPO, appType) : jar;
    }

    /**
     * Downloads jar into the local cache and returns path to it.
     *
     * @param repo The repository to use.
     * @param apptype Application type.
     * @return Path to local jar or {code null} if unable.
     */
    private Path downloadJar(String repo, String apptype) {
        String jar = String.format(APPTYPE_JAR, flavor, apptype, helidonVersion);
        Path localJarPath = localCache.resolve(jar);
        if (!localJarPath.toFile().exists()) {
            String location;
            try {
                // Attempt remote download of artifact
                location = String.format("%s%s/%s-%s-%s/%s/%s", repo, ARCHETYPE_DIRECTORY,
                        ARCHETYPE_PREFIX, flavor, apptype, helidonVersion, jar);
                downloadArtifact(new URL(location), localJarPath);
            } catch (ConnectException | FileNotFoundException e1) {
                Log.warn("Unable to download file %s from %s", jar, repo);
                return null;
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        }
        return localJarPath;
    }

    /**
     * Searches for apptypes in the local cache by inspecting the corresponding POM file.
     *
     * @return List of apptype names in local POM file.
     */
    List<String> appTypesLocalRepo() {
        String pomFile = String.format(APPTYPE_POM, flavor, helidonVersion);
        Path path = localCache.resolve(pomFile);
        if (path.toFile().exists()) {
            Model model = readPomModel(path);
            return model.getModules();
        }
        return Collections.emptyList();
    }

    /**
     * Downloads repository POM into local cache.
     */
    private void downloadPom(String repo) {
        String pom = String.format(APPTYPE_POM, flavor, helidonVersion);
        Path localPomPath = localCache.resolve(pom);
        String location = String.format("%s%s/%s-%s/%s/%s", repo, ARCHETYPE_DIRECTORY,
                ARCHETYPE_PREFIX, flavor, helidonVersion, pom);
        try {
            URL url = new URL(location);
            downloadArtifact(url, localPomPath);
        } catch (ConnectException | FileNotFoundException e) {
            Log.warn("Unable to download file %s from %s", pom, repo);
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
        try (BufferedInputStream bis = new BufferedInputStream(url.openConnection().getInputStream())) {
            try (FileOutputStream fos = new FileOutputStream(localPath.toFile())) {
                int n;
                while ((n = bis.read(BUFFER, 0, BUFFER.length)) >= 0) {
                    fos.write(BUFFER, 0, n);
                }
            }
        }
    }
}
