/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Profile;
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
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.version.Version;

import static io.helidon.build.util.Constants.DIR_SEP;
import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;
import static java.util.Objects.requireNonNull;

/**
 * Maven utilities.
 */
public class Maven {
    private static final String LATEST_VERSION = "[0,)";
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
    private final RepositorySystem system;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repositories;

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
     * Returns the latest version for the given artifact.
     *
     * @param groupId The artifact group id.
     * @param artifactId The artifact id.
     * @param allowSnapshot {@code true} if snapshot versions are allowed.
     * @return The latest version.
     */
    public Version latestVersion(String groupId, String artifactId, boolean allowSnapshot) {
        return latestVersion(toCoordinates(groupId, artifactId, LATEST_VERSION), allowSnapshot);
    }


    /**
     * Returns the latest version for the given artifact.
     *
     * @param coordinates The artifact coordinates.
     * @param allowSnapshot {@code true} if snapshot versions are allowed.
     * @return The latest version.
     */
    public Version latestVersion(String coordinates, boolean allowSnapshot) {
        final Artifact artifact = new DefaultArtifact(coordinates);
        final VersionRangeRequest request = new VersionRangeRequest(artifact, repositories, null);
        try {
            final VersionRangeResult result = system.resolveVersionRange(session, request);
            if (allowSnapshot) {
                return result.getHighestVersion();
            } else {
                final List<Version> versions = result.getVersions();
                final int lastIndex = versions.size() - 1;
                return IntStream.rangeClosed(0, lastIndex)
                                .mapToObj(index -> versions.get(lastIndex - index))
                                .filter(version -> !version.toString().endsWith(SNAPSHOT_SUFFIX))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("no non-snapshot version found!"));
            }
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

    /**
     * Maven builder.
     */
    public static class Builder {
        private static final String PATH_VAR = "PATH";
        private static final String USER_HOME_PROPERTY = "user.home";
        private static final String MVN_BINARY_NAME = "mvn";
        private static final String GLOBAL_SETTINGS_PATH = "conf" + DIR_SEP + "settings.xml";
        private static final String USER_SETTINGS_PATH = ".m2" + DIR_SEP + "settings.xml";
        private static final String LOCAL_REPOSITORY_PATH = ".m2" + DIR_SEP + "repository";
        private static final String CENTRAL_ID = "central";
        private static final String REPOSITORY_TYPE = "default";
        private static final String CENTRAL_URL = "https://repo.maven.apache.org/maven2/";
        private static final String LOCAL_REPOSITORY_KEY = "localRepository";
        private static final String MAVEN_HOME_VAR = "MAVEN_HOME";
        private static final String MVN_HOME_VAR = "MVN_HOME";

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
                mavenHome = envVarPath(MAVEN_HOME_VAR);
                if (mavenHome == null) {
                    mavenHome = envVarPath(MVN_HOME_VAR);
                    if (mavenHome == null) {
                        mavenHome = findExecutable(MVN_BINARY_NAME);
                    }
                }
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

        private List<RemoteRepository> repositories() {
            final Map<String, Profile> profiles = settings.getProfilesAsMap();
            final List<RemoteRepository> result = new ArrayList<>();
            for (String profileName : settings.getActiveProfiles()) {
                for (Repository repo : profiles.get(profileName).getRepositories()) {
                    result.add(new RemoteRepository.Builder(repo.getId(), REPOSITORY_TYPE, repo.getUrl()).build());
                }
            }
            if (result.isEmpty()) {
                result.add(new RemoteRepository.Builder(CENTRAL_ID, REPOSITORY_TYPE, CENTRAL_URL).build());
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

        private static Path envVarPath(String var) {
            final String path = System.getenv(var);
            return path == null ? null : Paths.get(path);
        }

        private static Path findExecutable(String executableName) {
            return Arrays.stream(requireNonNull(System.getenv(PATH_VAR)).split(File.pathSeparator))
                         .map(dir -> Paths.get(dir))
                         .filter(path -> Files.isExecutable(path.resolve(executableName)))
                         .findFirst()
                         .orElseThrow(() -> new IllegalStateException(executableName + " not found in " + PATH_VAR));
        }
    }
}
