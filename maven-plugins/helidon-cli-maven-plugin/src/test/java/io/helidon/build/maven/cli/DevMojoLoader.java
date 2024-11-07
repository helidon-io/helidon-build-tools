/*
 * Copyright (c) 2018, 2024 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cli;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.build.common.test.utils.TestFiles;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepositoryManager;

import static io.helidon.build.common.FileUtils.USER_HOME_DIR;
import static io.helidon.build.common.FileUtils.requireDirectory;
import static java.util.Objects.requireNonNull;

/**
 * Loader for {@link DevMojo} instances configured from a given project pom file.
 * Based on MavenPluginHelper.
 */
@SuppressWarnings({"UnconstructableJUnitTestCase", "JUnitMalformedDeclaration"})
public final class DevMojoLoader extends AbstractMojoTestCase {
    private static final AtomicReference<DevMojoLoader> INSTANCE = new AtomicReference<>();
    private static final Path TARGET_DIR = TestFiles.targetDir(DevMojoLoader.class);
    private static final Path TEST_TARGET_DIR = TARGET_DIR.resolve("test");
    private static final Path RESOURCES_DIR = TARGET_DIR.resolve("../src/test/resources").normalize();
    private static final String LOCAL_REPO_PROPERTY = "maven.repo.local";
    private static final String DEFAULT_LOCAL_REPO_DIR = ".m2/repository";
    private static final String PROJECT_BUILD_SOURCE_ENCODING = "project.build.sourceEncoding";
    private static final String LOCAL_REPO_MANAGER_CLASS = "org.eclipse.aether.internal.impl.SimpleLocalRepositoryManager";
    private static final String DEV_MOJO_NAME = "dev";
    private static final String POM_FILE_SUFFIX = "-pom.xml";
    private static final String UTF_8 = "UTF-8";

    private final LocalRepositoryManager repoManager;
    private final ModelReader reader;
    private final Map<String, ?> readOptions;
    private final Path localRepoDir;

    /**
     * Returns a new {@link DevMojo} instance configured by the pom file named {@code ${pomFilePrefix}-pom.xml} in
     * the {@code src/test/resources} directory. Does not include parent projects.
     *
     * @param pomFilePrefix The pom file name prefix.
     * @return The configured instance.
     * @throws Exception If an error occurs.
     */
    public static DevMojo configuredMojoFor(String pomFilePrefix) throws Exception {
        return configuredMojoFor(pomFilePrefix, false);
    }

    /**
     * Returns a new {@link DevMojo} instance configured by the pom file named {@code ${pomFilePrefix}-pom.xml} in
     * the {@code src/test/resources} directory.
     *
     * @param pomFilePrefix The pom file name prefix.
     * @param includeParentProjects Include parent projects when constructing the project.
     * @return The configured instance.
     * @throws Exception If an error occurs.
     */
    public static DevMojo configuredMojoFor(String pomFilePrefix, boolean includeParentProjects) throws Exception {
        String pomFileName = pomFilePrefix + POM_FILE_SUFFIX;
        Path pomFile = RESOURCES_DIR.resolve(pomFileName);
        Path targetDir = TEST_TARGET_DIR.resolve(pomFilePrefix);
        return instance().configuredMojoFor(pomFile, targetDir, includeParentProjects);
    }

    /**
     * Returns a new {@link MavenProject} constructed by reading the given pom file.
     *
     * @param pomFile The pom file.
     * @param targetDir The target directory.
     * @param includeParentProjects {@code true} if parent projects should be read and added.
     * @return The project.
     * @throws Exception If an error occurs.
     */
    public static MavenProject newMavenProject(Path pomFile, Path targetDir, boolean includeParentProjects) throws Exception {
        MavenProject project = instance().newMavenProject(pomFile, includeParentProjects);
        project.getModel().getBuild().setDirectory(targetDir.toString());
        return project;
    }

    private static DevMojoLoader instance() throws Exception {
        DevMojoLoader result = INSTANCE.get();
        if (result == null) {
            result = new DevMojoLoader();
            INSTANCE.set(result);
        }
        return result;
    }

    private DevMojoLoader() throws Exception {
        this.setUp();
        this.localRepoDir = localRepoDir();
        this.repoManager = newLocalRepositoryManager(localRepoDir);
        this.reader = new DefaultModelReader();
        this.readOptions = Map.of();
    }

    private DevMojo configuredMojoFor(Path pomFile, Path targetDir, boolean includeParentProjects) throws Exception {
        MavenProject project = newMavenProject(pomFile, targetDir, includeParentProjects);
        MavenSession session = newMavenSession(project);
        MojoExecution execution = newMojoExecution(DEV_MOJO_NAME);
        Mojo mojo = lookupConfiguredMojo(session, execution);
        requireNonNull(mojo);
        return (DevMojo) mojo;
    }

    private MavenProject newMavenProject(Path pomFile, boolean includeParentProjects) throws IOException {
        File pom = pomFile.toFile();
        Model model = reader.read(pom, readOptions);
        MavenProject project = new MavenProject(model);
        project.setFile(pom);
        project.getProperties().put(PROJECT_BUILD_SOURCE_ENCODING, UTF_8);
        return includeParentProjects ? addParent(project) : project;
    }

    private MavenProject addParent(MavenProject childProject) throws IOException {
        Parent parent = childProject.getModel().getParent();
        if (parent != null) {
            String parentPom = repoManager.getPathForLocalArtifact(new DefaultArtifact(parent.getGroupId(),
                                                                                       parent.getArtifactId(),
                                                                                       null,
                                                                                       "pom",
                                                                                       parent.getVersion()));
            Path pomFile = localRepoDir.resolve(parentPom);
            MavenProject parentProject = newMavenProject(pomFile, true);
            childProject.setParent(parentProject);
        }
        return childProject;
    }

    @Override
    protected MavenSession newMavenSession(MavenProject project) {
        MavenSession session = super.newMavenSession(project);
        // Set a local repo manager
        ((DefaultRepositorySystemSession) session.getRepositorySession()).setLocalRepositoryManager(repoManager);
        // Resolve build plugin versions
        resolveBuildPluginVersions(project);
        return session;
    }

    private static void resolveBuildPluginVersions(MavenProject project) {
        if (project.getParent() != null) {
            resolveBuildPluginVersions(project.getParent());
        }
        for (Plugin plugin : project.getBuild().getPlugins()) {
            String version = plugin.getVersion();
            if (version == null) {

                // NOTE: This is a temporary hack just to see if solving this problem will get us to
                //       the point that our build config can be resolved. If so, the real way to do
                //       this means parsing project.getPluginManagement().

                switch (plugin.getArtifactId()) {
                    case "maven-dependency-plugin":
                        version = "3.1.2";
                        break;
                    case "maven-compiler-plugin":
                        version = "3.8.1";
                        break;
                    case "exec-maven-plugin":
                        version = "1.6.0";
                        break;
                    case "helidon-cli-maven-plugin":
                        version = projectVersion();
                        break;
                }
                plugin.setVersion(version);
            }
        }
    }

    private static String projectVersion() {
        String version = System.getProperties().getProperty("version");
        if (version == null) {
            throw new IllegalStateException("Unable to resolve project version from system properties");
        }
        return version;
    }

    private static LocalRepositoryManager newLocalRepositoryManager(Path localRepoDir) throws Exception {
        Class<?> repoManagerClass = Class.forName(LOCAL_REPO_MANAGER_CLASS);
        Constructor<?> ctor = repoManagerClass.getDeclaredConstructor(File.class);
        ctor.setAccessible(true);
        return (LocalRepositoryManager) ctor.newInstance(localRepoDir.toFile());
    }

    private static Path localRepoDir() {
        Path dir;
        String prop = System.getProperty(LOCAL_REPO_PROPERTY);
        if (prop != null) {
            dir = Path.of(prop);
        } else {
            dir = USER_HOME_DIR.resolve(DEFAULT_LOCAL_REPO_DIR);
        }
        return requireDirectory(dir);
    }
}
