/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
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
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.helidon.build.cli.common.ProjectConfig;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.SubstitutionVariables;

import io.helidon.build.common.maven.MavenModel;

import static io.helidon.build.cli.common.ProjectConfig.DOT_HELIDON;
import static io.helidon.build.cli.impl.InitOptions.DEFAULT_ARCHETYPE_NAME;
import static io.helidon.build.cli.impl.InitOptions.DEFAULT_FLAVOR;
import static io.helidon.build.cli.impl.TestUtils.exec;
import static io.helidon.build.common.FileUtils.requireDirectory;
import static io.helidon.build.common.FileUtils.requireFile;
import static io.helidon.build.common.FileUtils.unique;
import static io.helidon.build.common.SubstitutionVariables.systemPropertyOrEnvVarSource;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Test utility to invoke {@code helidon}.
 */
public interface CommandInvoker {

    enum InvokerMode {
        CLASSPATH, EMBEDDED, EXECUTABLE
    }

    /**
     * Create a new init command invoker builder.
     *
     * @return Builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Get the Helidon version.
     *
     * @return Helidon version, may be {@code null}
     */
    String helidonVersion();

    /**
     * Get the groupId.
     *
     * @return groupId, never {@code null}
     */
    String groupId();

    /**
     * Get the artifactId.
     *
     * @return artifactId, never {@code null}
     */
    String artifactId();

    /**
     * Get the package name.
     *
     * @return package name, never {@code null}
     */
    String packageName();

    /**
     * Get the project name.
     *
     * @return project name, never {@code null}
     */
    String projectName();

    /**
     * Get the flavor.
     *
     * @return flavor, may be {@code null}
     */
    String flavor();

    /**
     * Get the archetype name.
     *
     * @return archetype name, may be {@code null}
     */
    String archetypeName();

    /**
     * Get the project directory
     *
     * @return project directory, never {@code null}
     */
    Path projectDir();

    /**
     * Get the metadata URL
     *
     * @return metadata URL, may be {@code null}
     */
    String metadataUrl();

    /**
     * Get the batch input file.
     *
     * @return input file, may be {@code null}
     */
    File input();

    /**
     * Get the working directory.
     *
     * @return working directory, never {@code null}
     */
    Path workDir();

    /**
     * Get the user config.
     *
     * @return user config, never {@code null}
     */
    UserConfig config();

    /**
     * Invoke the init command.
     *
     * @return invocation result
     * @throws Exception if an error occurs
     */
    InvocationResult invokeInit() throws Exception;

    /**
     * Invoke the dev command.
     *
     * @return invocation result
     * @throws Exception if an error occurs
     */
    InvocationResult invokeDev() throws Exception;

    /**
     * Stop the dev command.
     *
     * @return invocation result
     */
    InvocationResult stopMonitor();

    /**
     * Invoke the given command on the project.
     *
     * @param command command to invoke
     * @return invocation result
     * @throws Exception if an error occurs
     */
    InvocationResult invokeCommand(String command) throws Exception;

    /**
     * Invoke the {@code helidon build} command on the project.
     *
     * @return invocation result
     * @throws Exception if an error occurs
     */
    default InvocationResult invokeBuildCommand() throws Exception {
        return invokeCommand("build");
    }

    /**
     * Invoke the {@code helidon info} command on the project.
     *
     * @return invocation result
     * @throws Exception if an error occurs
     */
    default InvocationResult invokeInfoCommand() throws Exception {
        return invokeCommand("info");
    }

    /**
     * Invoke the {@code helidon version} command on the project.
     *
     * @return invocation result
     * @throws Exception if an error occurs
     */
    default InvocationResult invokeVersionCommand() throws Exception {
        return invokeCommand("version");
    }

    /**
     * Assert the generated project, and build it if {@code buildProject} is {@code true}.
     *
     * @return this invoker
     * @throws Exception if an error occurs
     */
    CommandInvoker validateProject() throws Exception;

    /**
     * Assert that the JAR file exists.
     *
     * @return this invoker
     */
    CommandInvoker assertJarExists();

    /**
     * Assert that the project directory exists.
     *
     * @return this invoker
     */
    CommandInvoker assertProjectExists();

    /**
     * Assert that the generated {@code pom.xml} exists and corresponds to the invocation parameters.
     *
     * @return this invoker
     */
    CommandInvoker assertExpectedPom();

    /**
     * Assert that the root directory of the Java package exists under the given "source root" directory.
     *
     * @param sourceRoot source root directory
     * @return this invoker
     */
    CommandInvoker assertPackageExists(Path sourceRoot) throws IOException;

    /**
     * Assert that there is at least one {@code .java} file in the given "source root" directory.
     *
     * @param sourceRoot source root directory
     * @return this invoker
     * @throws IOException if an IO error occurs
     */
    CommandInvoker assertSourceFilesExist(Path sourceRoot) throws IOException;

    /**
     * Assert that the {@code .helidon} file exists under the project directory.
     * If {@code buildProject} is {@code true}, check that the last successful build timestamp is {@code >0}.
     *
     * @return this invoker
     */
    CommandInvoker assertProjectConfig();

    /**
     * Invoker implementation.
     */
    class InvokerImpl implements CommandInvoker {

        private final String groupId;
        private final String artifactId;
        private final String packageName;
        private final String projectName;
        private final String flavor;
        private final String archetypeName;
        private final Path projectDir;
        private final String metadataUrl;
        private final File input;
        private final Path workDir;
        private final UserConfig config;
        private final String helidonVersion;
        private final Path executable;
        private final boolean buildProject;
        private final String appJvmArgs;
        private final boolean verbose;
        private final boolean debug;
        private ProcessMonitor devMonitor;
        private final boolean useProjectOption;
        private final InvokerMode invokerMode;

        private InvokerImpl(Builder builder) {
            useProjectOption = builder.useProjectOption;
            buildProject = builder.buildProject;
            helidonVersion = builder.helidonVersion;
            input = builder.input;
            metadataUrl = builder.metadataUrl;
            flavor = builder.flavor == null ? DEFAULT_FLAVOR : builder.flavor;
            archetypeName = builder.archetypeName == null ? DEFAULT_ARCHETYPE_NAME : builder.archetypeName;
            SubstitutionVariables substitutions = SubstitutionVariables.of(key -> {
                switch (key.toLowerCase()) {
                    case "init_flavor":
                        return flavor.toLowerCase();
                    case "init_archetype":
                        return archetypeName;
                    default:
                        return null;
                }
            }, systemPropertyOrEnvVarSource());
            config = builder.config == null ? UserConfig.create() : builder.config;
            projectName = config.projectName(builder.projectName, builder.artifactId, substitutions);
            groupId = builder.groupId == null ? config.defaultGroupId(substitutions) : builder.groupId;
            artifactId = config.artifactId(builder.artifactId, builder.projectName, substitutions);
            packageName = builder.packageName == null ? config.defaultPackageName(substitutions) : builder.packageName;
            executable = builder.executable;
            invokerMode = setInvokerMode(builder.executable, builder.embedded);
            appJvmArgs = builder.appJvmArgs;
            verbose = builder.verbose;
            debug = builder.debug;
            try {
                workDir = builder.workDir == null ? Files.createTempDirectory("helidon-init") : builder.workDir;
                projectDir = unique(workDir, projectName);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        private InvokerMode setInvokerMode(Path executable, boolean embedded) {
            if (executable != null) {
                return InvokerMode.EXECUTABLE;
            }
            if (embedded) {
                return InvokerMode.EMBEDDED;
            }
            return InvokerMode.CLASSPATH;
        }

        @Override
        public String helidonVersion() {
            return helidonVersion;
        }

        @Override
        public String groupId() {
            return groupId;
        }

        @Override
        public String artifactId() {
            return artifactId;
        }

        @Override
        public String packageName() {
            return packageName;
        }

        @Override
        public String projectName() {
            return projectName;
        }

        @Override
        public String flavor() {
            return flavor;
        }

        @Override
        public String archetypeName() {
            return archetypeName;
        }

        @Override
        public Path projectDir() {
            return projectDir;
        }

        @Override
        public String metadataUrl() {
            return metadataUrl;
        }

        @Override
        public File input() {
            return input;
        }

        @Override
        public Path workDir() {
            return workDir;
        }

        @Override
        public UserConfig config() {
            return config;
        }

        @Override
        public InvocationResult invokeInit() throws Exception {
            List<String> args = new ArrayList<>();
            args.add("init");
            args.add("--url");
            args.add(Objects.requireNonNull(metadataUrl, "metadataUrl is null!"));
            if (input == null) {
                args.add("--batch");
            }
            if (debug) {
                args.add("--debug");
            }
            if (helidonVersion != null) {
                args.add("--version");
                args.add(helidonVersion);
            }
            if (!DEFAULT_FLAVOR.equals(flavor)) {
                args.add("--flavor");
                args.add(flavor);
            }
            if (!DEFAULT_ARCHETYPE_NAME.equals(archetypeName)) {
                args.add("--archetype");
                args.add(archetypeName);
            }
            args.add("--groupId");
            args.add(groupId);
            args.add("--artifactId");
            args.add(artifactId);
            args.add("--package");
            args.add(packageName);
            args.add("--name");
            args.add(projectName);
            if (useProjectOption) {
                args.add("--project");
            }
            args.add(projectDir.toString());
            String[] argsArray = args.toArray(new String[]{});
            System.out.print("Executing with args ");
            args.forEach(a -> System.out.print(a + " "));
            System.out.println();

            return new InvocationResult(this, execute(workDir.toFile(), input, argsArray));
        }

        private String execute(File wd, File input, String... args) throws Exception {

            if (invokerMode == InvokerMode.EXECUTABLE) {
                return TestUtils.execWithExecutable(executable, wd, args);
            }

            if (invokerMode == InvokerMode.EMBEDDED) {
                Helidon.execute(args);
                return "Helidon class executed";
            }

            return TestUtils.execWithDirAndInput(workDir.toFile(), input, args);
        }

        @Override
        public InvocationResult invokeDev() throws Exception {
            List<String> args = new ArrayList<>();
            args.add("dev");
            if (verbose) {
                args.add("--verbose");
            }
            if (debug) {
                args.add("--debug");
            }
            if (appJvmArgs != null) {
                args.add("--app-jvm-args");
                args.add(appJvmArgs);
            }
            System.out.print("Executing with args ");
            args.forEach(a -> System.out.print(a + " "));
            System.out.println();

            // Execute and verify process exit code
            devMonitor = TestUtils.startWithDirAndInput(workDir.toFile(), input, args);
            return new InvocationResult(this, devMonitor.output());
        }

        public InvocationResult stopMonitor() {
            devMonitor.stop();
            return new InvocationResult(this, devMonitor.output());
        }

        @Override
        public InvocationResult invokeCommand(String command) throws Exception {
            String output = exec(command, "--project", projectDir.toString());
            return new InvocationResult(this, output);
        }

        @Override
        public CommandInvoker validateProject() throws Exception {
            assertProjectExists();
            assertExpectedPom();
            Path sourceRoot = projectDir.resolve("src/main/java");
            assertPackageExists(sourceRoot);
            assertSourceFilesExist(sourceRoot);
            if (buildProject) {
                invokeBuildCommand();
                assertJarExists();
                assertProjectConfig();
            }
            return this;
        }

        @Override
        public CommandInvoker assertJarExists() {
            requireFile(projectDir.resolve("target").resolve(artifactId + ".jar"));
            return this;
        }

        @Override
        public CommandInvoker assertProjectExists() {
            requireDirectory(projectDir);
            return this;
        }

        @Override
        public CommandInvoker assertExpectedPom() {
            // Check pom and read model
            Path pomFile = requireFile(projectDir().resolve("pom.xml"));
            MavenModel model = MavenModel.read(pomFile);

            // Flavor
            String parentArtifact = model.parent().artifactId();
            assertThat(parentArtifact, containsString(flavor.toLowerCase()));

            // GroupId
            assertThat(model.groupId(), is(groupId));

            // ArtifactId
            assertThat(model.artifactId(), is(artifactId));

            // Project Name
            assertThat(model.name(), is(projectName));

            if (helidonVersion != null) {
                // Project Version
                assertThat(model.parent().version(), is(helidonVersion));
            }

            return this;
        }

        @Override
        public CommandInvoker assertPackageExists(Path sourceRoot) throws IOException {
            long sources = Files.walk(sourceRoot)
                    .filter(file -> file.toString().contains(packageName.replace(".", File.separator)))
                    .count();
            assertThat(sources, is(greaterThan(0L)));
            return this;
        }

        @Override
        public CommandInvoker assertSourceFilesExist(Path sourceRoot) throws IOException {
            long sourceFiles = Files.walk(sourceRoot)
                    .filter(file -> file.getFileName().toString().endsWith(".java"))
                    .count();
            assertThat(sourceFiles, is(greaterThan(0L)));
            return this;
        }

        @Override
        public CommandInvoker assertProjectConfig() {
            Path dotHelidon = projectDir.resolve(DOT_HELIDON);
            ProjectConfig config = new ProjectConfig(dotHelidon);
            assertThat(config.exists(), is(true));
            if (buildProject) {
                assertThat(config.lastSuccessfulBuildTime(), is(greaterThan(0L)));
            }
            return this;
        }
    }

    /**
     * Invoker delegate.
     */
    class InvocationDelegate implements CommandInvoker {

        final CommandInvoker delegate;

        InvocationDelegate(CommandInvoker delegate) {
            this.delegate = Objects.requireNonNull(delegate);
        }

        @Override
        public String helidonVersion() {
            return delegate.helidonVersion();
        }

        @Override
        public String groupId() {
            return delegate.groupId();
        }

        @Override
        public String artifactId() {
            return delegate.artifactId();
        }

        @Override
        public String packageName() {
            return delegate.packageName();
        }

        @Override
        public String projectName() {
            return delegate.projectName();
        }

        @Override
        public String flavor() {
            return delegate.flavor();
        }

        @Override
        public String archetypeName() {
            return delegate.archetypeName();
        }

        @Override
        public Path projectDir() {
            return delegate.projectDir();
        }

        @Override
        public String metadataUrl() {
            return delegate.metadataUrl();
        }

        @Override
        public File input() {
            return delegate.input();
        }

        @Override
        public Path workDir() {
            return delegate.workDir();
        }

        @Override
        public UserConfig config() {
            return delegate.config();
        }

        @Override
        public InvocationResult invokeInit() throws Exception {
            return delegate.invokeInit();
        }

        @Override
        public InvocationResult invokeDev() throws Exception {
            return delegate.invokeDev();
        }

        @Override
        public InvocationResult stopMonitor() {
            return delegate.stopMonitor();
        }

        @Override
        public InvocationResult invokeCommand(String command) throws Exception {
            return delegate.invokeCommand(command);
        }

        @Override
        public CommandInvoker validateProject() throws Exception {
            return delegate.validateProject();
        }

        @Override
        public CommandInvoker assertJarExists() {
            return delegate.assertJarExists();
        }

        @Override
        public CommandInvoker assertProjectExists() {
            return delegate.assertProjectExists();
        }

        @Override
        public CommandInvoker assertExpectedPom() {
            return delegate.assertExpectedPom();
        }

        @Override
        public CommandInvoker assertPackageExists(Path sourceRoot) throws IOException {
            return delegate.assertPackageExists(sourceRoot);
        }

        @Override
        public CommandInvoker assertSourceFilesExist(Path sourceRoot) throws IOException {
            return delegate.assertSourceFilesExist(sourceRoot);
        }

        @Override
        public CommandInvoker assertProjectConfig() {
            return delegate.assertProjectConfig();
        }
    }

    /**
     * Invoker delegate that holds invocation output.
     */
    class InvocationResult extends InvocationDelegate {

        final String output;

        InvocationResult(CommandInvoker delegate, String output) {
            super(delegate);
            this.output = Objects.requireNonNull(output, "output is null");
        }

        /**
         * Get the invocation result output.
         *
         * @return output, never {@code null}
         */
        public String output() {
            return output;
        }
    }

    /**
     * Builder class for {@link CommandInvoker}.
     */
    class Builder {

        private String groupId;
        private String artifactId;
        private String packageName;
        private String projectName;
        private String flavor;
        private String archetypeName;
        private UserConfig config;
        private String metadataUrl;
        private Path workDir;
        private File input;
        private String helidonVersion;
        private Path executable;
        private boolean buildProject;
        private boolean useProjectOption;
        private boolean embedded;
        private boolean verbose;
        private boolean debug;
        private String appJvmArgs;

        /**
         * Use the {@code --project} option instead of the project argument.
         *
         * @param useProjectOption {@code true} if should use {@code --project} option
         * @return this builder
         */
        public Builder useProjectOption(boolean useProjectOption) {
            this.useProjectOption = useProjectOption;
            return this;
        }

        /**
         * Set the build project flag.
         *
         * @param buildProject build project flag
         * @return this builder
         */
        public Builder buildProject(boolean buildProject) {
            this.buildProject = buildProject;
            return this;
        }

        /**
         * Set the Helidon version.
         *
         * @param helidonVersion Helidon version
         * @return this builder
         */
        public Builder helidonVersion(String helidonVersion) {
            this.helidonVersion = helidonVersion;
            return this;
        }

        /**
         * Set the groupId.
         *
         * @param groupId groupId
         * @return this builder
         */
        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        /**
         * Set the artifactId.
         *
         * @param artifactId artifactId
         * @return this builder
         */
        public Builder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        /**
         * Set the Java package name.
         *
         * @param packageName Java package name
         * @return this builder
         */
        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        /**
         * Set the project name.
         *
         * @param projectName project name
         * @return this builder
         */
        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        /**
         * Set the flavor.
         *
         * @param flavor flavor
         * @return this builder
         */
        public Builder flavor(String flavor) {
            this.flavor = flavor.toUpperCase();
            return this;
        }

        /**
         * Set the archetype name.
         *
         * @param archetypeName archetype name
         * @return this builder
         */
        public Builder archetypeName(String archetypeName) {
            this.archetypeName = archetypeName;
            return this;
        }

        /**
         * Set the batch input file.
         *
         * @param inputFileUrl input file
         * @return this builder
         */
        public Builder input(URL inputFileUrl) {
            URL url = Objects.requireNonNull(inputFileUrl, inputFileUrl + "not found");
            this.input = new File(url.getFile());
            return this;
        }

        /**
         * Set the metadata URL.
         *
         * @param metadataUrl metadata URL
         * @return this builder
         */
        public Builder metadataUrl(String metadataUrl) {
            this.metadataUrl = metadataUrl;
            return this;
        }

        /**
         * Set the working directory.
         *
         * @param workDir working directory
         * @return this builder
         */
        public Builder workDir(Path workDir) {
            this.workDir = workDir;
            return this;
        }

        /**
         * Set the user config.
         *
         * @param config user config
         * @return this builder
         */
        public Builder userConfig(UserConfig config) {
            this.config = config;
            return this;
        }

        /**
         * Run cli with helidon.sh script.
         *
         * @return this builder
         */
        public Builder executable(Path executable) {
            this.executable = executable;
            return this;
        }

        /**
         * Run cli with {@code helidon} class.
         *
         * @return this builder
         */
        public Builder embedded() {
            this.embedded = true;
            return this;
        }

        /**
         * Set the application jvm arguments.
         *
         * @param args arguments
         * @return this builder
         */
        public Builder appJvmArgs(String args) {
            this.appJvmArgs = args;
            return this;
        }

        /**
         * Set verbose.
         *
         * @return this builder
         */
        public Builder verbose() {
            this.verbose = true;
            return this;
        }

        /**
         * Set debug.
         *
         * @return this builder
         */
        public Builder debug() {
            this.debug = true;
            return this;
        }

        /**
         * Build the command invoker instance.
         *
         * @return invoker instance
         */
        public CommandInvoker build() {
            return new InvokerImpl(this);
        }

        /**
         * Build the command invoker instance and invoke the init command.
         *
         * @return invoker instance
         * @throws Exception if any error occurs
         */
        public CommandInvoker.InvocationResult invokeInit() throws Exception {
            return new InvokerImpl(this).invokeInit();
        }

        /**
         * Build the command invoker instance and invoke the dev command.
         *
         * @return invoker instance
         * @throws Exception if any error occurs
         */
        public CommandInvoker invokeDev() throws Exception {
            return new InvokerImpl(this).invokeDev();
        }

    }
}
