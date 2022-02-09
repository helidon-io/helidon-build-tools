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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TestUtils {

    private static final int BUFFER_SIZE = 4096;
    private static final Logger LOGGER = Logger.getLogger(TestUtils.class.getName());

    static void downloadMavenDist(Path destination, String version) throws IOException {

        Path zipPath = destination.resolve("maven.zip");
        URL mavenUrl = new URL(
                String.format("https://archive.apache.org/dist/maven/maven-3/%s/binaries/apache-maven-%s-bin.zip", version, version)
        );

        LOGGER.info("Downloading maven from URL : " + mavenUrl);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("www-proxy.us.oracle.com", 80));
        try (InputStream in = mavenUrl.openConnection(proxy).getInputStream()) {
            Files.copy(in, zipPath);
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

}
