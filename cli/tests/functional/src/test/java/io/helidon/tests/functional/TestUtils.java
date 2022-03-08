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
package io.helidon.tests.functional;

import io.helidon.build.common.FileUtils;
import io.helidon.build.common.OSType;
import io.helidon.build.common.maven.MavenCommand;
import io.helidon.build.common.maven.MavenVersion;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

public class TestUtils {

    private static final Logger LOGGER = Logger.getLogger(TestUtils.class.getName());

    static void downloadMavenDist(Path destination, String version) throws IOException {

        Path zipPath = destination.resolve("maven-" + version + ".zip");
        URL mavenUrl = new URL(
                String.format("http://archive.apache.org/dist/maven/maven-3/%s/binaries/apache-maven-%s-bin.zip", version, version)
        );

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

        Files.walk(destination)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.equals("mvn"))
                .findFirst().orElseThrow();
    }

    static void downloadFileFromUrl(Path destination, URL url) {
        try {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("www-proxy.us.oracle.com", 80));
            URLConnection connection = url.openConnection(proxy);
            connection.setConnectTimeout(100*60*1000);
            connection.setReadTimeout(100*60*1000);
            ReadableByteChannel readableByteChannel = Channels.newChannel(connection.getInputStream());
            FileOutputStream fileOutputStream = new FileOutputStream(Files.createFile(destination).toString());
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            fileOutputStream.close();
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
                //Assertions.fail("Application failed to start on port :" + port);
                throw new Exception();
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

    static void generateBareSe(Path wd, String mavenHome) throws Exception {
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

        MavenCommand.builder()
                .mvnExecutable(Path.of(mavenHome, "apache-maven-3.8.4", "bin", TestUtils.mvnExecutable("3.8.4")))
                .directory(wd)
                .addArguments(mavenArgs)
                .build()
                .execute();
    }

    static String mvnExecutable(String mavenVersion) {
        if (OSType.currentOS().equals(OSType.Windows)) {
            if (MavenVersion.toMavenVersion(mavenVersion).isLessThanOrEqualTo(MavenVersion.toMavenVersion("3.2.5"))) {
                return "mvn.bat";
            }
            return "mvn.cmd";
        }
        return "mvn";
    }

}
