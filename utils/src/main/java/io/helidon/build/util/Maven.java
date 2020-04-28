/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
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

package io.helidon.build.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.version.Version;

import static io.helidon.build.util.Constants.DIR_SEP;
import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;

/**
 * Maven utilities.
 */
public class Maven {
    private static final String LATEST_VERSION = "[0,)";
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
    private static final Instance<Maven> DEFAULT_INSTANCE = new Instance<>(Maven::defaultInstance);

    private final RepositorySystem system;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repositories;

    /**
     * A version filter that does not allow snapshots.
     */
    public static final Predicate<Version> LATEST_RELEASE = Maven::notSnapshot;

    /**
     * A version filter that allows any version.
     */
    public static final Predicate<Version> LATEST = version -> true;

    private Maven(Builder builder) {
        this.system = builder.system;
        this.session = builder.session;
        this.repositories = builder.repositories;
    }

    /**
     * Returns a new builder.
     *
     * @return The builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns an instance constructed with default configuration.
     *
     * @return The instance.
     */
    public static Maven instance() {
        return DEFAULT_INSTANCE.instance();
    }


    /**
     * Returns the latest version for the given artifact.
     *
     * @param groupId The artifact group id.
     * @param artifactId The artifact id.
     * @param filter The filter. May be {@code null}.
     * @return The latest version.
     */
    public Version latestVersion(String groupId, String artifactId, Predicate<Version> filter) {
        return latestVersion(toCoordinates(groupId, artifactId, LATEST_VERSION), filter);
    }

    /**
     * Returns the latest version for the given artifact.
     *
     * @param coordinates The artifact coordinates.
     * @param filter The filter. May be {@code null}.
     * @return The latest version.
     */
    public Version latestVersion(String coordinates, Predicate<Version> filter) {
        final List<Version> versions = versions(coordinates);
        final int lastIndex = versions.size() - 1;
        final Predicate<Version> predicate = filter == null ? LATEST : filter;
        return IntStream.rangeClosed(0, lastIndex)
                .mapToObj(index -> versions.get(lastIndex - index))
                .filter(predicate)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no matching version found!"));
    }

    /**
     * Returns the versions for the given artifact.
     *
     * @param groupId The artifact group id.
     * @param artifactId The artifact id.
     * @return The versions.
     */
    public List<Version> versions(String groupId, String artifactId) {
        return versions(toCoordinates(groupId, artifactId, LATEST_VERSION));
    }

    /**
     * Returns the versions for the given artifact.
     *
     * @param coordinates The artifact coordinates.
     * @return The versions.
     */
    public List<Version> versions(String coordinates) {
        final Artifact artifact = new DefaultArtifact(coordinates);
        final VersionRangeRequest request = new VersionRangeRequest(artifact, repositories, null);
        try {
            return system.resolveVersionRange(session, request).getVersions();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the path to the given artifact.
     *
     * @param groupId The artifact group id.
     * @param artifactId The artifact id.
     * @param version The artifact version.
     * @return The path.
     */
    public Path artifact(String groupId, String artifactId, String version) {
        return artifact(toCoordinates(groupId, artifactId, version));
    }

    /**
     * Returns the path to the given artifact.
     *
     * @param coordinates The artifact coordinates.
     * @return The path.
     */
    public Path artifact(String coordinates) {
        final Artifact artifact = new DefaultArtifact(coordinates);
        final ArtifactRequest request = new ArtifactRequest(artifact, repositories, null);
        try {
            ArtifactResult result = system.resolveArtifact(session, request);
            return result.getArtifact().getFile().toPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the given artifact coordinate components as a GAV coordinate.
     *
     * @param groupId The artifact group id.
     * @param artifactId The artifact id.
     * @param version The artifact version.
     * @return The coordinates.
     */
    public static String toCoordinates(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version;
    }

    private static boolean notSnapshot(Version version) {
        return !version.toString().endsWith(SNAPSHOT_SUFFIX);
    }

    /**
     * Maven builder.
     */
    public static class Builder {
        private static final String USER_HOME_PROPERTY = "user.home";
        private static final String GLOBAL_SETTINGS_PATH = "conf" + DIR_SEP + "settings.xml";
        private static final String USER_SETTINGS_PATH = ".m2" + DIR_SEP + "settings.xml";
        private static final String LOCAL_REPOSITORY_PATH = ".m2" + DIR_SEP + "repository";
        private static final String CENTRAL_ID = "central";
        private static final String REPOSITORY_TYPE = "default";
        private static final String CENTRAL_URL = "https://repo.maven.apache.org/maven2/";
        private static final String LOCAL_REPOSITORY_KEY = "localRepository";

        private final Path userHome;
        private Path mavenHome;
        private Path localRepositoryDir;
        private Path globalSettings;
        private Path userSettings;
        private boolean offline;
        private RepositorySystem system;
        private DefaultRepositorySystemSession session;
        private Settings settings;
        private List<RemoteRepository> repositories;

        private Builder() {
            this.userHome = Paths.get(System.getProperty(USER_HOME_PROPERTY));
        }

        /**
         * Sets the path to the maven install directory.
         *
         * @param mavenHome The path.
         * @return This instance.
         */
        public Builder mavenHome(Path mavenHome) {
            this.mavenHome = assertDir(mavenHome);
            return this;
        }

        /**
         * Sets the path to the global {@code settings.xml} file.
         *
         * @param globalSettingsFile The path.
         * @return This instance.
         */
        public Builder globalSettingsFile(Path globalSettingsFile) {
            this.globalSettings = assertFile(globalSettingsFile);
            return this;
        }

        /**
         * Sets the path to the user {@code settings.xml} file.
         *
         * @param userSettingsFile The path.
         * @return This instance.
         */
        public Builder userSettingsFile(Path userSettingsFile) {
            this.userSettings = assertFile(userSettingsFile);
            return this;
        }

        /**
         * Sets the path to the local repository.
         *
         * @param localRepositoryDir The path.
         * @return This instance.
         */
        public Builder localRepositoryDir(Path localRepositoryDir) {
            this.localRepositoryDir = assertDir(localRepositoryDir);
            return this;
        }

        /**
         * Sets offline mode.
         *
         * @param offline {@code true} if offline.
         * @return This instance.
         */
        public Builder offline(boolean offline) {
            this.offline = offline;
            return this;
        }

        /**
         * Builds the {@link Maven} instance.
         *
         * @return The instance.
         */
        public Maven build() {
            system = newRepositorySystem();
            session = MavenRepositorySystemUtils.newSession()
                    .setOffline(offline)
                    .setCache(new DefaultRepositoryCache());
            final LocalRepository localRepo = localRepository();
            session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

            //session.setTransferListener( new ConsoleTransferListener() );
            //session.setRepositoryListener( new ConsoleRepositoryListener() );

            settings = settings();
            repositories = repositories();
            return new Maven(this);
        }

        private Path mavenHome() {
            if (mavenHome == null) {
                mavenHome = MavenCommand.mavenHome();
            }
            return mavenHome;
        }

        private LocalRepository localRepository() {
            if (localRepositoryDir == null) {
                final String localRepo = System.getProperty(LOCAL_REPOSITORY_KEY);
                if (localRepo != null) {
                    final Path dir = Paths.get(localRepo);
                    if (Files.isDirectory(dir)) {
                        localRepositoryDir = dir;
                    }
                }
                if (localRepositoryDir == null) {
                    localRepositoryDir = assertDir(userHome.resolve(LOCAL_REPOSITORY_PATH));
                }
            }
            return new LocalRepository(localRepositoryDir.toFile());
        }

        private Settings settings() {
            if (globalSettings == null) {
                final Path home = mavenHome();
                this.globalSettings = home.resolve(GLOBAL_SETTINGS_PATH);
            }
            if (userSettings == null) {
                this.userSettings = userHome.resolve(USER_SETTINGS_PATH);
            }
            final SettingsBuilder settingsBuilder = new DefaultSettingsBuilderFactory().newInstance();
            final DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
            if (Files.isRegularFile(globalSettings)) {
                request.setGlobalSettingsFile(globalSettings.toFile());
            }
            if (Files.isRegularFile(userSettings)) {
                request.setUserSettingsFile(userSettings.toFile());
            }
            request.setSystemProperties(System.getProperties());
            try {
                return settingsBuilder.build(request).getEffectiveSettings();
            } catch (SettingsBuildingException e) {
                throw new IllegalArgumentException("Could not process settings.xml", e);
            }
        }

        private org.eclipse.aether.repository.Proxy proxy(String repoUrl) {
            for (Proxy proxySetting : settings.getProxies()) {
                if (repoUrl.startsWith(proxySetting.getProtocol())) {
                    return new org.eclipse.aether.repository.Proxy(proxySetting.getProtocol(), proxySetting.getHost(),
                            proxySetting.getPort());
                }
            }
            return null;
        }

        private List<RemoteRepository> repositories() {
            final Map<String, Profile> profiles = settings.getProfilesAsMap();
            final List<RemoteRepository> result = new ArrayList<>();
            for (String profileName : settings.getActiveProfiles()) {
                for (Repository repo : profiles.get(profileName).getRepositories()) {
                    result.add(new RemoteRepository.Builder(repo.getId(), REPOSITORY_TYPE, repo.getUrl())
                            .setProxy(proxy(repo.getUrl()))
                            .build());
                }
            }
            if (result.isEmpty()) {
                result.add(new RemoteRepository.Builder(CENTRAL_ID, REPOSITORY_TYPE, CENTRAL_URL)
                        .setProxy(proxy(CENTRAL_URL))
                        .build());
            }
            return result;
        }

        private RepositorySystem newRepositorySystem() {
            final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
            locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
            locator.addService(TransporterFactory.class, FileTransporterFactory.class);
            locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
            locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
                @Override
                public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                    Log.error("Service creation failed for %s implementation %s: %s",
                            type, impl, exception.getMessage(), exception);
                }
            });
            return locator.getService(RepositorySystem.class);
        }
    }

    private static Maven defaultInstance() {
        if (System.getProperty("dump.env") != null) {
            Log.info("\n--- Environment ---- \n");
            System.getenv().forEach((key, value) -> Log.info("    %s = %s", key, value));
            Log.info("\n--- System Properties ---- \n");
            System.getProperties().forEach((key, value) -> Log.info("    %s = %s", key, value));
        }
        return Maven.builder().build();
    }

}
