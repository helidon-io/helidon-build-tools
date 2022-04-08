/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.build.cli.tests;

import io.helidon.build.common.FileUtils;
import io.helidon.build.common.NetworkConnection;
import io.helidon.build.common.OSType;
import io.helidon.build.common.Proxies;
import io.helidon.build.common.maven.MavenCommand;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

public class TestUtils {

    private static final Logger LOGGER = Logger.getLogger(TestUtils.class.getName());
    private static final String MAVEN_DIST_URL = "https://archive.apache.org/dist/maven/maven-3/%s/binaries/apache-maven-%s-bin.zip";

    static {
        Proxies.setProxyPropertiesFromEnv();
    }

    static void downloadMavenDist(Path destination, String version) throws IOException {

        Path zipPath = destination.resolve("maven-" + version + ".zip");
        URL mavenUrl = new URL(String.format(MAVEN_DIST_URL, version, version));

        LOGGER.info("Downloading maven from URL : " + mavenUrl);

        downloadFileFromUrl(zipPath, mavenUrl);

        LOGGER.info("Maven download done.");
        LOGGER.info("Unzip Maven started ...");

        FileUtils.unzip(zipPath, destination);

        LOGGER.info("Unzip Maven done.");

        Files.walk(destination)
                .map(Path::toFile)
                .filter(File::isFile)
                .forEach(file -> file.setExecutable(true));

        FileUtils.delete(zipPath);
    }

    static void downloadFileFromUrl(Path destination, URL url) {
        try {
            InputStream is = NetworkConnection.builder()
                    .url(url)
                    .connectTimeout(100*60*1000)
                    .readTimeout(100*60*1000)
                    .open();
            OutputStream os = Files.newOutputStream(destination);
            is.transferTo(os);
        } catch (IOException e) {
            throw new UncheckedIOException("Download failed at URL : " + url, e);
        }
    }

    static void waitForApplication(int port) throws Exception {
        long timeout = 60 * 1000;
        long now = System.currentTimeMillis();
        URL url = new URL("http://localhost:" + port + "/greet");

        HttpURLConnection conn = null;
        int responseCode;
        do {
            Thread.sleep(1000);
            if ((System.currentTimeMillis() - now) > timeout) {
                throw new Exception("Application failed to start on port :" + port);
            }
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(500);
                responseCode = conn.getResponseCode();
            } catch (Exception ex) {
                responseCode = -1;
            }
            if (conn != null) {
                conn.disconnect();
            }
        } while (responseCode != 200);
    }

    static int getAvailablePort() {
        try {
            ServerSocket s = new ServerSocket(0);
            s.close();
            return s.getLocalPort();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    static void generateBareSe(Path wd, Path mavenBinDir) {
        List<String> mavenArgs = List.of(
                "archetype:generate",
                "-DinteractiveMode=false",
                "-DarchetypeGroupId=io.helidon.archetypes",
                "-DarchetypeArtifactId=helidon",
                "-DarchetypeVersion=3.0.0-M1",
                "-DgroupId=groupid",
                "-DartifactId=artifactid",
                "-Dpackage=custom.pack.name",
                "-Dflavor=se",
                "-Dbase=bare");

        try {
            MavenCommand.builder()
                    .executable(mavenBinDir.resolve(TestUtils.getMvnExecutable(mavenBinDir)))
                    .directory(wd)
                    .addArguments(mavenArgs)
                    .build()
                    .execute();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot generate bare-se project", e);
        }
    }

    static String getMvnExecutable(Path mavenBinDir) {
        List<String> executables = OSType.currentOS() == OSType.Windows
                ? List.of("mvn.bat", "mvn.cmd")
                : List.of("mvn");
        for (String executable : executables) {
            if (Files.exists(mavenBinDir.resolve(executable))) {
                return executable;
            }
        }
        throw new IllegalStateException(String.format("mvn executable not found in %s directory.", mavenBinDir));
    }

}
