/*
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

import io.helidon.build.archetype.engine.v1.ArchetypeCatalog.ArchetypeEntry;
import io.helidon.build.common.Maps;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.RichTextRenderer;
import io.helidon.build.common.SubstitutionVariables;
import io.helidon.build.common.logging.LogRecorder;
import io.helidon.build.common.maven.MavenVersion;
import io.helidon.build.common.test.utils.HttpServer;
import io.helidon.build.common.test.utils.HttpServer.Filter;
import io.helidon.build.common.test.utils.HttpServer.FilterChain;
import io.helidon.build.common.test.utils.HttpServer.Request;
import io.helidon.build.common.test.utils.HttpServer.Response;
import io.helidon.build.common.test.utils.LogRecordExtension;
import io.helidon.build.common.test.utils.WorkDirExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.helidon.build.common.FileUtils.zip;
import static io.helidon.build.common.PrintStreams.STDOUT;
import static io.helidon.build.common.ansi.AnsiTextStyle.strip;
import static io.helidon.build.common.test.utils.FunctionalMatchers.hasProperty;
import static io.helidon.build.common.test.utils.LogMatchers.matches;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for class {@link Metadata}.
 */
@ExtendWith(LogRecordExtension.class)
@ExtendWith(WorkDirExtension.class)
class MetadataTest {

    private final LogRecorder recorder;
    private final Path workDir;
    private final SubstitutionVariables variables;

    MetadataTest(LogRecorder recorder, Path workDir) {
        this.recorder = recorder;
        this.workDir = workDir;
        this.variables = SubstitutionVariables.of(Map.of("workDir", workDir.toString()));
    }

    @Test
    void testCacheMissWithZeroClock() throws Metadata.UpdateFailed, IOException {
        var filter = new FilterImpl();
        try (var server = new HttpServer(routing -> routing
                .filter(filter)
                .get("/versions.xml", (req, res) -> res
                        .send("""
                                <data>
                                    <archetypes>
                                        <version default="true">1.0.0</version>
                                        <version>2.0.0</version>
                                    </archetypes>
                                </data>
                                """))
                .get("/2.0.0/cli-data.zip", (req, res) -> res
                        .send(zip(Map.of(
                                "metadata.properties", """
                                        cli.version=2.3.4
                                        source=cache-miss
                                        """,
                                "payload.bin", new byte[] {1, 2, 3})))))) {

            var metadata = metadata(server.url(), () -> 0L);

            assertThat(Maps.from(metadata.propertiesOf(v("2.0.0")).entrySet()), is(Map.of(
                    "cli.version", "2.3.4",
                    "source", "cache-miss")));

            assertThat(filter.paths(), is(List.of("/2.0.0/cli-data.zip", "/versions.xml")));
            assertThat(filter.ifNoneMatch("/2.0.0/cli-data.zip"), is(List.of(List.of())));

            assertThat(Files.readAllBytes(workDir.resolve("2.0.0/payload.bin")), is(new byte[] {1, 2, 3}));
            assertThat(Files.readString(workDir.resolve("2.0.0/.lastUpdate")), is("<no-etag>"));
            assertThat(Files.exists(workDir.resolve("2.0.0/cli-data.zip")), is(false));

            var output = strip(recorder.output());
            assertThat(output, matches(this::substitutionValue, """
                    stale check forced (not found) for ${path:${workDir}/2.0.0/.lastUpdate}
                    Updating metadata for Helidon version 2.0.0
                    ~...
                    updated ${path:${workDir}/2.0.0/.lastUpdate} with etag <no-etag>
                    ~...
                    wrote versions.xml to ${path:${workDir}/versions.xml}
                    ~...
                    """));
        }
    }

    @Test
    void testRetryPeriod() throws Metadata.UpdateFailed, IOException {
        var filter = new FilterImpl();
        try (var server = new HttpServer(routing -> routing
                .filter(filter)
                .get("/versions.xml", (req, res) -> res
                        .send("""
                                <data>
                                    <archetypes>
                                        <version default="true">1.0.0</version>
                                    </archetypes>
                                </data>
                                """))
                .get("/1.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v1\"")
                        .send(zip(Map.of(
                                "metadata.properties", "cli.version=1.2.3\n")))))) {

            var versionDir = Files.createDirectories(workDir.resolve("1.0.0"));
            Files.writeString(versionDir.resolve("metadata.properties"), "cli.version=1.2.3\n");
            var lastUpdate = Files.writeString(versionDir.resolve(".lastUpdate"), "\"v1\"");
            var now = new AtomicLong(10_000);
            Files.setLastModifiedTime(lastUpdate, FileTime.fromMillis(now.get()));

            var metadata = metadata(server.url(), 1, TimeUnit.DAYS, now::get);
            assertThat(metadata.cliVersionOf(v("1.0.0"), false), is(v("1.2.3")));
            assertThat(filter.requests, is(List.of()));
            recorder.output();

            now.addAndGet(1000);
            assertThat(metadata.cliVersionOf(v("1.0.0"), false), is(v("1.2.3")));
            assertThat(filter.requests, is(List.of()));
            var output = strip(recorder.output());
            assertThat(output, matches(this::substitutionValue, """
                    stale check is false (retry) for ${path:${workDir}/1.0.0/.lastUpdate}
                    """));

            now.incrementAndGet();
            assertThat(metadata.cliVersionOf(v("1.0.0"), false), is(v("1.2.3")));
            assertThat(filter.requests, is(List.of()));
            output = strip(recorder.output());
            assertThat(output, matches(this::substitutionValue, """
                    stale check is false for ${path:${workDir}/1.0.0/.lastUpdate} ~.*
                    """));
        }
    }

    @Test
    void testUpdatePeriod() throws Metadata.UpdateFailed, IOException {
        var filter = new FilterImpl();
        try (var server = new HttpServer(routing -> routing
                .filter(filter)
                .get("/versions.xml", (req, res) -> res
                        .send("""
                                <data>
                                    <archetypes>
                                        <version default="true">1.0.0</version>
                                    </archetypes>
                                </data>
                                """))
                .get("/1.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v1\"")
                        .send(zip(Map.of(
                                "metadata.properties", "cli.version=1.2.3\n")))))) {

            var versionDir = Files.createDirectories(workDir.resolve("1.0.0"));
            Files.writeString(versionDir.resolve("metadata.properties"), "cli.version=1.2.3\n");
            var lastUpdate = Files.writeString(versionDir.resolve(".lastUpdate"), "\"v1\"");
            Files.writeString(workDir.resolve("versions.xml"), """
                <data>
                    <archetypes>
                        <version default="true">1.0.0</version>
                    </archetypes>
                </data>
                """);
            Files.setLastModifiedTime(lastUpdate, FileTime.fromMillis(10_000));

            var beforeExpiry = metadata(server.url(), 1000, TimeUnit.MILLISECONDS, () -> 10_999);
            assertThat(beforeExpiry.cliVersionOf(v("1.0.0"), false), is(v("1.2.3")));
            assertThat(filter.requests, is(List.of()));
            var output = strip(recorder.output());
            assertThat(output, matches(this::substitutionValue, """
                    stale check is false for ${path:${workDir}/1.0.0/.lastUpdate} ~.*
                    """));

            var atExpiry = metadata(server.url(), 1000, TimeUnit.MILLISECONDS, () -> 11_000);
            assertThat(atExpiry.cliVersionOf(v("1.0.0"), false), is(v("1.2.3")));
            assertThat(filter.paths(), is(List.of("/1.0.0/cli-data.zip", "/versions.xml")));
            output = strip(recorder.output());
            assertThat(output, matches(this::substitutionValue, """
                    stale check is true for ${path:${workDir}/1.0.0/.lastUpdate} ~.*
                    Updating metadata for Helidon version 1.0.0
                    ~...
                    updated ${path:${workDir}/1.0.0/.lastUpdate} with etag "v1"
                    ~...
                    """));
        }
    }

    @Test
    void testUpdatesDisabled() throws Metadata.UpdateFailed, IOException {
        var filter = new FilterImpl();
        try (var server = new HttpServer(routing -> routing
                .filter(filter)
                .get("/versions.xml", (req, res) -> res
                        .send("""
                                <data>
                                    <archetypes>
                                        <version default="true">1.0.0</version>
                                    </archetypes>
                                </data>
                                """))
                .get("/1.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v1\"")
                        .send(zip(Map.of(
                                "metadata.properties", "cli.version=1.2.3\n")))))) {

            var now = new AtomicLong(10_000);
            var metadata = metadata(server.url(), -1, TimeUnit.MILLISECONDS, now::get);

            assertThat(metadata.cliVersionOf(v("1.0.0"), false), is(v("1.2.3")));

            assertThat(filter.paths(), is(List.of("/1.0.0/cli-data.zip", "/versions.xml")));
            assertThat(Files.readString(workDir.resolve("1.0.0/.lastUpdate")), is("\"v1\""));

            var output = strip(recorder.output());
            assertThat(output, matches(this::substitutionValue, """
                    stale check forced (not found) for ${path:${workDir}/1.0.0/.lastUpdate}
                    Updating metadata for Helidon version 1.0.0
                    ~...
                    """));

            now.addAndGet(1001);
            assertThat(metadata.cliVersionOf(v("1.0.0"), false), is(v("1.2.3")));
            assertThat(filter.paths(), is(List.of("/1.0.0/cli-data.zip", "/versions.xml")));
            output = strip(recorder.output());
            assertThat(output, matches(this::substitutionValue, """
                    stale check is false (disabled) for ${path:${workDir}/1.0.0/.lastUpdate}
                    """));
        }
    }

    @Test
    void testEtagMatch() throws Metadata.UpdateFailed, IOException {
        var filter = new FilterImpl();
        try (var server = new HttpServer(routing -> routing
                .filter(filter)
                .get("/versions.xml", (req, res) -> res
                        .send("""
                                <data>
                                    <archetypes>
                                        <version default="true">1.0.0</version>
                                    </archetypes>
                                </data>
                                """))
                .get("/1.0.0/cli-data.zip", (req, res) -> {
                    if (req.header("If-None-Match").contains("\"v1\"")) {
                        res.header("ETag", "\"v1\"").status(304).send();
                    } else {
                        res.header("ETag", "\"v1\"").send(zip(Map.of(
                                "metadata.properties", """
                                        cli.version=1.2.3
                                        source=cached
                                        """,
                                "preserved.txt", "preserved")));
                    }
                }))) {

            var now = new AtomicLong(10_000);
            var metadata = metadata(server.url(), 0, TimeUnit.MILLISECONDS, now::get);
            assertThat(metadata.cliVersionOf(v("1.0.0"), false), is(v("1.2.3")));
            recorder.output(); // drain output

            var lastUpdate = workDir.resolve("1.0.0/.lastUpdate");
            Files.setLastModifiedTime(lastUpdate, FileTime.fromMillis(1));
            now.addAndGet(1001);
            assertThat(Maps.from(metadata.propertiesOf(v("1.0.0")).entrySet()), is(Map.of(
                    "cli.version", "1.2.3",
                    "source", "cached")));
            assertThat(Files.readString(workDir.resolve("1.0.0/preserved.txt")), is("preserved"));
            assertThat(Files.readString(lastUpdate), is("\"v1\""));
            assertThat(Files.getLastModifiedTime(lastUpdate).toMillis(), greaterThan(1L));
            assertThat(filter.paths(), is(List.of(
                    "/1.0.0/cli-data.zip", "/versions.xml",
                    "/1.0.0/cli-data.zip", "/versions.xml")));
            assertThat(filter.ifNoneMatch("/1.0.0/cli-data.zip"), is(List.of(List.of(), List.of("\"v1\""))));

            var output = strip(recorder.output());
            assertThat(output, matches(this::substitutionValue, """
                    stale check forced (zero delay) for ${path:${workDir}/1.0.0/.lastUpdate}
                    Updating metadata for Helidon version 1.0.0
                    ~...
                    not modified ~.*/1.0.0/cli-data.zip
                    ~...
                    updated ${path:${workDir}/1.0.0/.lastUpdate} with etag "v1"
                    ~...
                    """));
        }
    }

    @Test
    void testEtagChange() throws Metadata.UpdateFailed, IOException {
        var filter = new FilterImpl();
        try (var server = new HttpServer(routing -> routing
                .filter(filter)
                .get("/versions.xml", (req, res) -> res
                        .send("""
                                <data>
                                    <archetypes>
                                        <version default="true">1.0.0</version>
                                    </archetypes>
                                </data>
                                """))
                .get("/1.0.0/cli-data.zip", (req, res) -> {
                    if (req.header("If-None-Match").isEmpty()) {
                        res.header("ETag", "\"v1\"").send(zip(Map.of(
                                "metadata.properties", """
                                        cli.version=1.2.3
                                        source=old
                                        """,
                                "obsolete.txt", "obsolete")));
                    } else {
                        res.header("ETag", "\"v2\"").send(zip(Map.of(
                                "metadata.properties", """
                                        cli.version=2.3.4
                                        source=new
                                        """,
                                "current.txt", "current")));
                    }
                }))) {

            var now = new AtomicLong(10_000);
            var metadata = metadata(server.url(), 0, TimeUnit.MILLISECONDS, now::get);
            assertThat(metadata.cliVersionOf(v("1.0.0"), false), is(v("1.2.3")));
            assertThat(Files.exists(workDir.resolve("1.0.0/obsolete.txt")), is(true));
            recorder.output();

            now.addAndGet(1001);
            assertThat(Maps.from(metadata.propertiesOf(v("1.0.0")).entrySet()), is(Map.of(
                    "cli.version", "2.3.4",
                    "source", "new")));
            assertThat(Files.exists(workDir.resolve("1.0.0/obsolete.txt")), is(false));
            assertThat(Files.readString(workDir.resolve("1.0.0/current.txt")), is("current"));
            assertThat(Files.readString(workDir.resolve("1.0.0/.lastUpdate")), is("\"v2\""));
            assertThat(filter.paths(), is(List.of(
                    "/1.0.0/cli-data.zip", "/versions.xml",
                    "/1.0.0/cli-data.zip", "/versions.xml")));
            assertThat(filter.ifNoneMatch("/1.0.0/cli-data.zip"), is(List.of(List.of(), List.of("\"v1\""))));

            var output = strip(recorder.output());
            assertThat(output, matches(this::substitutionValue, """
                    stale check forced (zero delay) for ${path:${workDir}/1.0.0/.lastUpdate}
                    Updating metadata for Helidon version 1.0.0
                    ~...
                    deleting ${path:${workDir}/1.0.0}
                    ~...
                    updated ${path:${workDir}/1.0.0/.lastUpdate} with etag "v2"
                    ~...
                    """));
        }
    }

    @Test
    void testDefaultChange() throws Metadata.UpdateFailed, IOException {
        var versionsXml = new AtomicReference<>("""
                <data>
                    <archetypes>
                        <version default="true">1.0.0</version>
                        <version>2.0.0</version>
                    </archetypes>
                </data>
                """);
        var filter = new FilterImpl();
        try (var server = new HttpServer(routing -> routing
                .filter(filter)
                .get("/versions.xml", (req, res) -> res.send(versionsXml.get()))
                .get("/1.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v1\"")
                        .send(zip(Map.of(
                                "metadata.properties", "cli.version=1.2.3\n"))))
                .get("/2.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v2\"").send(zip(Map.of(
                                "metadata.properties", "cli.version=2.3.4\n")))))) {

            var now = new AtomicLong(10_000);
            var metadata = metadata(server.url(), 2000, TimeUnit.MILLISECONDS, now::get);
            assertThat(metadata.defaultVersion(), is(v("1.0.0")));
            recorder.output();

            versionsXml.set("""
                    <data>
                        <archetypes>
                            <version>1.0.0</version>
                            <version default="true">2.0.0</version>
                        </archetypes>
                    </data>
                    """);
            Files.setLastModifiedTime(workDir.resolve("versions.xml"), FileTime.fromMillis(now.get()));
            now.addAndGet(2000);

            assertThat(metadata.defaultVersion(), is(v("2.0.0")));
            assertThat(Files.readString(workDir.resolve("2.0.0/metadata.properties")),
                    is("cli.version=2.3.4\n"));
            assertThat(filter.paths(), is(List.of(
                    "/versions.xml", "/1.0.0/cli-data.zip",
                    "/versions.xml", "/2.0.0/cli-data.zip")));

            var output = strip(recorder.output());
            assertThat(output, matches(this::substitutionValue, """
                    stale check is true for ${path:${workDir}/versions.xml} ~.*
                    Looking up default Helidon version
                    ~...
                    wrote versions.xml to ${path:${workDir}/versions.xml}
                    ~...
                    updated ${path:${workDir}/2.0.0/.lastUpdate} with etag "v2"
                    ~...
                    """));
        }
    }

    @Test
    void testLastUpdateTime() throws Metadata.UpdateFailed, IOException {
        try (var server = new HttpServer(routing -> routing
                .get("/versions.xml", (req, res) -> res.send("""
                        <data>
                            <archetypes>
                                <version default="true">1.0.0</version>
                            </archetypes>
                        </data>
                        """))
                .get("/1.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v1\"")
                        .send(zip(Map.of("metadata.properties", "cli.version=1.2.3\n")))))) {

            var metadata = metadata(server.url());
            assertThat(metadata.lastUpdateTime(), is(FileTime.fromMillis(0)));
            assertThat(metadata.defaultVersion(), is(v("1.0.0")));
            assertThat(metadata.lastUpdateTime(), is(Files.getLastModifiedTime(workDir.resolve("versions.xml"))));
        }
    }

    @Test
    void testDefaultVersion() throws Metadata.UpdateFailed {
        try (var server = new HttpServer(routing -> routing
                .get("/versions.xml", (req, res) -> res.send("""
                        <data>
                            <archetypes>
                                <version>1.0.0</version>
                                <version default="true">2.0.0</version>
                                <version>3.0.0</version>
                            </archetypes>
                        </data>
                        """))
                .get("/2.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v2\"")
                        .send(zip(Map.of("metadata.properties", "cli.version=2.3.4\n")))))) {

            assertThat(metadata(server.url()).defaultVersion(), is(v("2.0.0")));
        }
    }

    @Test
    void testLatestVersion() throws Metadata.UpdateFailed {
        try (var server = new HttpServer(routing -> routing
                .get("/versions.xml", (req, res) -> res.send("""
                        <data>
                            <archetypes>
                                <version default="true">1.0.0</version>
                                <version>3.0.0</version>
                                <version>2.0.0</version>
                            </archetypes>
                        </data>
                        """))
                .get("/1.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v1\"")
                        .send(zip(Map.of("metadata.properties", "cli.version=1.2.3\n")))))) {

            assertThat(metadata(server.url()).latestVersion(), is(v("3.0.0")));
        }
    }

    @Test
    void testCliPluginVersion() throws Metadata.UpdateFailed {
        try (var server = new HttpServer(routing -> routing
                .get("/versions.xml", (req, res) -> res.send("""
                        <data>
                            <archetypes>
                                <version>1.0.0</version>
                                <version>1.0.1</version>
                                <version default="true">1.0.2</version>
                            </archetypes>
                        </data>
                        """))
                .get("/1.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v1\"")
                        .send(zip(Map.of("metadata.properties", "cli.version=0.0.1\n"))))
                .get("/1.0.1/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v2\"")
                        .send(zip(Map.of("metadata.properties", """
                                cli.version=0.0.1
                                cli.latest.plugin.version=0.0.99
                                """))))
                .get("/1.0.2/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v3\"")
                        .send(zip(Map.of("metadata.properties", """
                                cli.version=0.0.1
                                cli.0.0.1.plugin.version=77.0.0
                                cli.1.2.3.plugin.version=88.0.0
                                cli.latest.plugin.version=99.0.0
                                """)))))) {

            var metadata = metadata(server.url(), -1, TimeUnit.MILLISECONDS);

            var version = v("1.0.0");
            assertThat(metadata.cliPluginVersion(version, v("99.0.0"), false), is(v("99.0.0")));
            assertThat(metadata.cliPluginVersion(version, v("98.0.0"), false), is(v("98.0.0")));

            version = v("1.0.1");
            assertThat(metadata.cliPluginVersion(version, v("99.0.0"), false), is(v("0.0.99")));
            assertThat(metadata.cliPluginVersion(version, v("0.0.1"), false), is(v("0.0.99")));

            version = v("1.0.2");
            assertThat(metadata.cliPluginVersion(version, v("1.2.2"), false), is(v("77.0.0")));
            assertThat(metadata.cliPluginVersion(version, v("1.2.3"), false), is(v("88.0.0")));
            assertThat(metadata.cliPluginVersion(version, v("1.2.4"), false), is(v("88.0.0")));
            assertThat(metadata.cliPluginVersion(version, v("99.0.0"), false), is(v("99.0.0")));
            assertThat(metadata.cliPluginVersion(version, v("99.0.1"), false), is(v("99.0.0")));
        }
    }

    @Test
    void testCliVersionOf() throws Metadata.UpdateFailed {
        try (var server = new HttpServer(routing -> routing
                .get("/versions.xml", (req, res) -> res.send("""
                        <data>
                            <archetypes>
                                <version default="true">1.0.0</version>
                            </archetypes>
                        </data>
                        """))
                .get("/1.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v1\"")
                        .send(zip(Map.of("metadata.properties", """
                                cli.version=4.5.6
                                ignored=value
                                """)))))) {
            assertThat(metadata(server.url()).cliVersionOf(v("1.0.0"), false), is(v("4.5.6")));
        }
    }

    @Test
    void testCheckForCliUpdate() throws Metadata.UpdateFailed {
        try (var server = new HttpServer(routing -> routing
                .get("/versions.xml", (req, res) -> res.send("""
                        <data>
                            <archetypes>
                                <version default="true">1.0.0</version>
                            </archetypes>
                        </data>
                        """))
                .get("/1.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v1\"")
                        .send(zip(Map.of("metadata.properties", "cli.version=1.2.3\n")))))) {
            var metadata = metadata(server.url());

            assertThat(metadata.checkForCliUpdate(v("1.2.2"), false), is(Optional.of(v("1.2.3"))));
            assertThat(metadata.checkForCliUpdate(v("1.2.3"), false), is(Optional.empty()));
        }
    }

    @Test
    void testCliReleaseNotesOf() throws Metadata.UpdateFailed {
        try (var server = new HttpServer(routing -> routing
                .get("/versions.xml", (req, res) -> res.send("""
                        <data>
                            <archetypes>
                                <version default="true">1.0.0</version>
                            </archetypes>
                        </data>
                        """))
                .get("/1.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v1\"")
                        .send(zip(Map.of("metadata.properties", """
                                cli.version=4.5.6
                                cli.1.2.3.message=Note #1
                                cli.1.2.4.message=Note #2
                                cli.1.2.5.message=Note #3
                                cli.1.2.6.message=Note #4
                                """)))))) {
            var notes = metadata(server.url()).cliReleaseNotesOf(v("1.0.0"), v("1.2.4"));

            assertThat(notes, is(Map.of(
                    v("1.2.6"), "Note #4",
                    v("1.2.5"), "Note #3")));
            assertThat(new ArrayList<>(notes.keySet()), contains(v("1.2.6"), v("1.2.5")));
        }
    }

    @Test
    void testPropertiesOf() throws Metadata.UpdateFailed {
        try (var server = new HttpServer(routing -> routing
                .get("/versions.xml", (req, res) -> res.send("""
                        <data>
                            <archetypes>
                                <version default="true">1.0.0</version>
                            </archetypes>
                        </data>
                        """))
                .get("/1.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v1\"")
                        .send(zip(Map.of("metadata.properties", """
                                cli.version=1.2.3
                                color=blue
                                feature.enabled=true
                                """)))))) {
            var properties = metadata(server.url()).propertiesOf(v("1.0.0"));

            assertThat(Maps.from(properties.entrySet()), is(Map.of(
                    "cli.version", "1.2.3",
                    "color", "blue",
                    "feature.enabled", "true")));
        }
    }

    @Test
    void testCatalogOf() throws Metadata.UpdateFailed {
        try (var server = new HttpServer(routing -> routing
                .get("/versions.xml", (req, res) -> res.send("""
                        <data>
                            <archetypes>
                                <version default="true">1.0.0</version>
                            </archetypes>
                        </data>
                        """))
                .get("/1.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v1\"")
                        .send(zip(Map.of("archetype-catalog.xml", """
                                <archetype-catalog modelVersion="1.0" name="test" groupId="acme" version="1.0.0">
                                    <archetype artifactId="acme-one" name="one" title="One" summary="First" />
                                    <archetype artifactId="acme-two" name="two" title="Two" summary="Second" />
                                </archetype-catalog>
                                """)))))) {
            var catalog = metadata(server.url()).catalogOf(v("1.0.0"));

            assertThat(catalog.name(), is("test"));
            assertThat(catalog.entries(), contains(List.of(
                    allOf(
                            hasProperty("artifactId", ArchetypeEntry::artifactId, is("acme-one")),
                            hasProperty("name", ArchetypeEntry::name, is("one")),
                            hasProperty("title", ArchetypeEntry::title, is("One"))),
                    allOf(
                            hasProperty("artifactId", ArchetypeEntry::artifactId, is("acme-two")),
                            hasProperty("name", ArchetypeEntry::name, is("two")),
                            hasProperty("title", ArchetypeEntry::title, is("Two"))))));
        }
    }

    @Test
    void testArchetypeV1Of() throws Metadata.UpdateFailed, IOException {
        var jarBytes = new byte[] {10, 20, 30, 40};
        try (var server = new HttpServer(routing -> routing
                .get("/versions.xml", (req, res) -> res.send("""
                        <data>
                            <archetypes>
                                <version default="true">1.0.0</version>
                            </archetypes>
                        </data>
                        """))
                .get("/1.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v1\"")
                        .send(zip(Map.of(
                                "archetype-catalog.xml", """
                                        <archetype-catalog modelVersion="1.0" name="test" groupId="acme" version="1.0.0">
                                            <archetype artifactId="acme-one" name="one" title="One" summary="First" />
                                        </archetype-catalog>
                                        """,
                                "acme-one-1.0.0.jar", jarBytes)))))) {
            var metadata = metadata(server.url());
            var entry = metadata.catalogOf(v("1.0.0")).entries().get(0);

            var archetype = metadata.archetypeV1Of(entry);
            assertThat(archetype.getFileName().toString(), is("acme-one-1.0.0.jar"));
            assertThat(Files.readAllBytes(archetype), is(jarBytes));
        }
    }

    @Test
    void testArchetypeV2Of() throws Metadata.UpdateFailed, IOException {
        var jarBytes = new byte[] {50, 60, 70, 80};
        try (var server = new HttpServer(routing -> routing
                .get("/versions.xml", (req, res) -> res.send("""
                        <data>
                            <archetypes>
                                <version default="true">4.0.0</version>
                            </archetypes>
                        </data>
                        """))
                .get("/4.0.0/cli-data.zip", (req, res) -> res
                        .header("ETag", "\"v1\"")
                        .send(zip(Map.of("helidon-4.0.0.jar", jarBytes)))))) {
            var archetype = metadata(server.url()).archetypeV2Of(v("4.0.0"));

            assertThat(archetype.getFileName().toString(), is("helidon-4.0.0.jar"));
            assertThat(Files.readAllBytes(archetype), is(jarBytes));
        }
    }

    private Metadata metadata(String url) {
        return metadata(url, Metadata.DEFAULT_UPDATE_FREQUENCY, Metadata.DEFAULT_UPDATE_FREQUENCY_UNITS);
    }

    private Metadata metadata(String url, LongSupplier timeSource) {
        return metadata(url, Metadata.DEFAULT_UPDATE_FREQUENCY, Metadata.DEFAULT_UPDATE_FREQUENCY_UNITS, timeSource);
    }

    private String substitutionValue(String expression) {
        int separator = expression.indexOf(':');
        if (separator < 0 || !expression.substring(0, separator).equals("path")) {
            return null;
        }
        return variables.resolve(expression.substring(separator + 1)).replace("/", File.separator);
    }

    private Metadata metadata(String url, long updateFrequency, TimeUnit updateFrequencyUnits) {
        return metadata(url, updateFrequency, updateFrequencyUnits, Metadata.DEFAULT_TIME_SOURCE);
    }

    private Metadata metadata(String url, long updateFrequency, TimeUnit updateFrequencyUnits, LongSupplier timeSource) {
        return Metadata.builder()
                .rootDir(workDir)
                .url(url)
                .updateFrequency(updateFrequency)
                .updateFrequencyUnits(updateFrequencyUnits)
                .timeSource(timeSource)
                .debugPlugin(true)
                .pluginStdOut(PrintStreams.apply(STDOUT, it -> {
                    var stripped = strip(RichTextRenderer.render(it));
                    recorder.addEntry(stripped);
                    return stripped;
                }))
                .build();
    }

    private static MavenVersion v(String version) {
        return MavenVersion.toMavenVersion(version);
    }

    private static final class FilterImpl implements Filter {

        final List<Map.Entry<String, Map<String, List<String>>>> requests = new CopyOnWriteArrayList<>();

        @Override
        public void filter(FilterChain chain, Request request, Response response) {
            requests.add(Map.entry(request.path(), request.headers()));
            chain.proceed();
        }

        List<String> paths() {
            return requests.stream()
                    .map(Map.Entry::getKey)
                    .toList();
        }

        List<List<String>> ifNoneMatch(String path) {
            return requests.stream()
                    .filter(e -> path.equals(e.getKey()))
                    .map(e -> e.getValue().entrySet().stream()
                            .filter(header -> "If-None-Match".equalsIgnoreCase(header.getKey()))
                            .flatMap(header -> header.getValue().stream())
                            .toList())
                    .toList();
        }
    }
}
