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

import io.helidon.build.cli.impl.Helidon;
import io.helidon.build.common.FileUtils;
import io.helidon.build.common.NetworkConnection;
import io.helidon.build.common.OSType;
import io.helidon.build.common.Proxies;

import java.io.ByteArrayOutputStream;
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
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;

public class FunctionalUtils {

    private static final Logger LOGGER = Logger.getLogger(FunctionalUtils.class.getName());
    private static final String MAVEN_DIST_URL = "https://archive.apache.org/dist/maven/maven-3/%s/binaries/apache-maven-%s-bin.zip";

    static final String ARCHETYPE_URL = String.format("file://%s/cli-data", targetDir(FunctionalUtils.class));
    static final String CLI_VERSION = getProperty("cli.version");

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
        try (InputStream is = NetworkConnection.builder()
                .url(url)
                .connectTimeout(100*60*1000)
                .readTimeout(100*60*1000)
                .open();
             OutputStream os = Files.newOutputStream(destination)) {
            is.transferTo(os);
        } catch (IOException e) {
            throw new UncheckedIOException("Download failed at URL : " + url, e);
        }
    }

    static void waitForApplication(int port, ByteArrayOutputStream os) throws Exception {
        long timeout = 60 * 1000;
        long now = System.currentTimeMillis();
        URL url = new URL("http://localhost:" + port + "/greet");

        HttpURLConnection conn = null;
        int responseCode;
        do {
            Thread.sleep(1000);
            if ((System.currentTimeMillis() - now) > timeout) {
                Path failingTestReport = writeReport(os);
                throw new Exception(String.format("Application failed to start on port : %s. \nCheck %s file\n",
                        port,
                        failingTestReport));
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

    static void generateBareSe(Path wd) {
        Helidon.execute(
                "init",
                "--reset",
                "--url", ARCHETYPE_URL,
                "--batch",
                "--project", wd.resolve("bare-se").toString(),
                "--version", CLI_VERSION,
                "--artifactId", "bare-se",
                "--package", "custom.pack.name",
                "--flavor", "se");
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

    static String getProperty(String key) {
        String version = System.getProperty(key);
        if (version != null) {
            return version;
        } else {
            throw new IllegalStateException("helidon.version is not set");
        }
    }

    static List<String> buildJavaCommand() {
        Path jar = Path.of(getProperty("helidon.executable.directory")).resolve("target/helidon.jar");
        return new ArrayList<>(List.of(javaPath(), "-Xmx128M", "-jar", jar.toString()));
    }

    static String javaPath() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            File javaHomeBin = new File(javaHome, "bin");
            if (javaHomeBin.exists() && javaHomeBin.isDirectory()) {
                File javaBin = new File(javaHomeBin, "java");
                if (javaBin.exists() && javaBin.isFile()) {
                    return javaBin.getAbsolutePath();
                }
            }
        }
        return "java";
    }

    private static Path writeReport(ByteArrayOutputStream os) {
        try {
            Path failingTestReport = FileUtils.ensureFile(targetDir(FunctionalUtils.class)
                    .resolve("surefire-reports/failing-test-output.txt"));
            Files.writeString(failingTestReport,
                    os.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.WRITE);
            return failingTestReport;
        } catch (IOException ioe) {
            throw new UncheckedIOException("Could not create failing test file containing Console output", ioe);
        }
    }

}
