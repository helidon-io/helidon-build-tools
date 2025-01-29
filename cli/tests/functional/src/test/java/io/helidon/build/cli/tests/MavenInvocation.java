/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import io.helidon.build.common.FileUtils;
import io.helidon.build.common.NetworkConnection;
import io.helidon.build.common.OSType;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.Proxies;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.maven.MavenCommand;

import static io.helidon.build.common.FileUtils.delete;
import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.FileUtils.unzip;

class MavenInvocation extends ProcessInvocation {

    static {
        Proxies.setProxyPropertiesFromEnv();
        LogLevel.set(LogLevel.DEBUG);
    }

    private static final boolean IS_WINDOWS = OSType.currentOS() == OSType.Windows;
    private static final Path MAVEN_DOWNLOADS = Path.of(".maven-downloads").toAbsolutePath();
    private static final String LOCAL_REPO_ARG = localRepoArg();

    private final String mavenVersion;

    MavenInvocation(String mavenVersion) {
        this.mavenVersion = Objects.requireNonNull(mavenVersion, "mavenVersion is null");
    }

    Monitor start() {
        if (cwd == null) {
            throw new IllegalStateException("cwd is not set");
        }
        Recorder recorder = new Recorder();
        Path distDir = downloadMaven(mavenVersion);
        Path logFile = unique(logDir != null ? logDir : cwd, "maven", ".log");
        try {
            PrintStream printStream = new PrintStream(Files.newOutputStream(logFile));
            MavenCommand cmd = MavenCommand.builder()
                    .executable(mavenBin(distDir.resolve("bin")))
                    .directory(cwd)
                    .stdOut(PrintStreams.accept(printStream, recorder::record))
                    .stdErr(PrintStreams.accept(printStream, recorder::record))
                    .addOptionalArgument(LOCAL_REPO_ARG)
                    .addArguments(args)
                    .build();

            Log.info("Invoking maven, logs: %s", logFile);
            return new Monitor(cmd.start(), recorder, cwd);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (Exception ex) {
            throw new MonitorException(recorder.sb.toString(), ex);
        }
    }

    static Path downloadMaven(String version) {
        ensureDirectory(MAVEN_DOWNLOADS);
        Path distDir = MAVEN_DOWNLOADS.resolve("apache-maven-" + version);
        if (!Files.exists(distDir)) {
            Path distZip = MAVEN_DOWNLOADS.resolve("apache-maven-" + version + "-bin.zip");
            URL url = downloadUrl(version);

            Log.info("Downloading " + url);
            download(distZip, url);

            Log.info("Unzipping " + distZip + " to " + MAVEN_DOWNLOADS);
            unzip(distZip, MAVEN_DOWNLOADS);

            Log.info("Deleting " + distZip);
            delete(distZip);
        }
        return distDir;
    }

    static URL downloadUrl(String version) {
        try {
            return new URL(String.format(
                    "https://archive.apache.org/dist/maven/maven-3/%s/binaries/apache-maven-%s-bin.zip",
                    version, version));
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void download(Path destination, URL url) {
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

    static Path mavenBin(Path dir) {
        for (String executable : IS_WINDOWS ? List.of("mvn.bat", "mvn.cmd") : List.of("mvn")) {
            Path file = dir.resolve(executable);
            if (Files.exists(dir.resolve(executable))) {
                if (!FileUtils.setExecutable(file, true, true)) {
                    Log.warn("Unable to make %s executable", file);
                }
                return file;
            }
        }
        throw new IllegalStateException(String.format("mvn executable not found in %s directory.", dir));
    }

    static String localRepoArg() {
        String localRepository = System.getProperty("localRepository");
        return localRepository != null ? "-Dmaven.repo.local=" + localRepository : null;
    }
}
