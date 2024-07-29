/*
 * Copyright (c) 2022, 2024 Oracle and/or its affiliates.
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
import io.helidon.build.common.SourcePath;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static io.helidon.build.common.test.utils.TestFiles.targetDir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class FunctionalUtils {

    private static final Logger LOGGER = Logger.getLogger(FunctionalUtils.class.getName());
    private static final String MAVEN_DIST_URL = "https://archive.apache.org/dist/maven/maven-3/%s/binaries/apache-maven-%s-bin.zip";
    static final String ARCHETYPE_URL = FileUtils.urlOf(targetDir(FunctionalUtils.class).resolve("cli-data")).toString();
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

        try (Stream<Path> files = Files.walk(destination)) {
            files.map(Path::toFile)
                    .filter(File::isFile)
                    .forEach(file -> {
                        if (!file.setExecutable(true)) {
                            LOGGER.warning(String.format("Unable to make %s executable", file));
                        }
                    });
        }
        FileUtils.delete(zipPath);
    }

    static void downloadFileFromUrl(Path destination, URL url) {
        try (InputStream is = NetworkConnection.builder()
                .url(url)
                .connectTimeout(100 * 60 * 1000)
                .readTimeout(100 * 60 * 1000)
                .open();
             OutputStream os = Files.newOutputStream(destination)) {
            is.transferTo(os);
        } catch (IOException e) {
            throw new UncheckedIOException("Download failed at URL : " + url, e);
        }
    }

    static void waitForApplication(int port, Supplier<String> output) throws Exception {
        long timeout = 60 * 1000;
        long now = System.currentTimeMillis();
        URL url = new URL("http://localhost:" + port + "/greet");

        HttpURLConnection conn = null;
        int responseCode;
        do {
            Thread.sleep(1000);
            if ((System.currentTimeMillis() - now) > timeout) {
                throw new Exception(String.format("Application failed to start on port : %s\nProcess output:\n %s",
                        port,
                        output.get()));
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

    static void validateSeProject(Path wd) {
        List<SourcePath> files = SourcePath.scan(wd.resolve("bare-se"));
        assertThat(files.stream().anyMatch(path -> path.matches("pom.xml")), is(true));
        assertThat(files.stream().anyMatch(path -> path.matches("**/*.java")), is(true));
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
            throw new IllegalStateException(String.format("System property %s is not set", key));
        }
    }

    static List<String> buildJavaCommand() {
        Path jar = Path.of(getProperty("helidon.executable.directory")).resolve("target/helidon-cli.jar");
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

    static void setMavenLocalRepoUrl() {
        try {
            String url = FunctionalUtils.class
                    .getClassLoader()
                    .loadClass("org.hamcrest.MatcherAssert")
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .resolve("../../../..")
                    .getPath();
            System.setProperty("io.helidon.build.common.maven.url.localRepo", url);
        } catch (ClassNotFoundException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
