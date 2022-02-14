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

import org.junit.jupiter.api.Assertions;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TestUtils {

    private static final int BUFFER_SIZE = 4096;
    private static final Logger LOGGER = Logger.getLogger(TestUtils.class.getName());

    static void downloadMavenDist(Path destination, String version) throws IOException {

        Path zipPath = destination.resolve("maven.zip");
        URL mavenUrl = new URL(
                String.format("http://archive.apache.org/dist/maven/maven-3/%s/binaries/apache-maven-%s-bin.zip", version, version)
        );

        LOGGER.info("Downloading maven from URL : " + mavenUrl);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("www-proxy.us.oracle.com", 80));
        URLConnection connection = mavenUrl.openConnection(proxy);
        connection.setReadTimeout(10*60*1000);
        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
        }

        LOGGER.info("Maven download done.");
        LOGGER.info("Unzip Maven started ...");

        unzip(zipPath, destination);

        LOGGER.info("Unzip Maven done.");

        Files.walk(destination)
                .map(Path::toFile)
                .filter(File::isFile)
                .forEach(file -> file.setExecutable(true));

        if (!zipPath.toFile().delete()) {
            LOGGER.info("Could not clean zip file");
        }

        Optional<String> mvnFile = Files.walk(destination)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.equals("mvn"))
                .findFirst();

        if (mvnFile.isEmpty()) {
            throw new IOException("Maven downloading failed. Test can not be processed");
        }
    }

    private static void unzip(Path zipFilePath, Path destDirectory) throws IOException {
        String destination = destDirectory.toString();
        File destDir = new File(destination);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath.toString()));
        ZipEntry entry = zipIn.getNextEntry();
        while (entry != null) {
            String filePath = destination + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                extractFile(zipIn, filePath);
            } else {
                File dir = new File(filePath);
                dir.mkdirs();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    static void waitForApplication(int port) throws Exception {
        long timeout = 30 * 1000;
        long now = System.currentTimeMillis();
        URL url = new URL("http://localhost:" + port + "/greet");

        HttpURLConnection conn = null;
        int responseCode;
        do {
            Thread.sleep(1000);
            if ((System.currentTimeMillis() - now) > timeout) {
                Assertions.fail("Application failed to start on port :" + port);
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

    static int getAvailablePort() throws IOException {
        ServerSocket s = new ServerSocket(0);
        s.close();
        return s.getLocalPort();
    }

}
