/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
package io.helidon.build.javadoc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.helidon.build.common.FileUtils;
import io.helidon.build.common.Lists;
import io.helidon.build.common.OSType;
import io.helidon.build.common.PrintStreams;
import io.helidon.build.common.ProcessMonitor;
import io.helidon.build.common.ProcessMonitor.ProcessFailedException;
import io.helidon.build.common.ProcessMonitor.ProcessTimeoutException;
import io.helidon.build.common.RingBuffer;
import io.helidon.build.common.Strings;
import io.helidon.build.common.logging.Log;
import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.maven.MavenModel;
import io.helidon.build.common.maven.plugin.PlexusLoggerHolder;
import io.helidon.build.javadoc.JavadocModule.CompositeJavadocModule;
import io.helidon.build.javadoc.JavadocModule.JarModule;
import io.helidon.build.javadoc.JavadocModule.SourceModule;
import io.helidon.build.javadoc.JavadocModule.SourceRoot;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Organization;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;

import static io.helidon.build.common.FileUtils.ensureDirectory;
import static io.helidon.build.common.FileUtils.fileName;
import static io.helidon.build.common.FileUtils.findExecutableInJavaHome;
import static io.helidon.build.common.FileUtils.findExecutableInPath;
import static io.helidon.build.common.PrintStreams.DEVNULL;
import static io.helidon.build.common.Strings.normalizePath;
import static java.io.File.pathSeparator;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * A goal to produce javadocs.
 * Provides a <strong>simple</strong> way to produce aggregated javadocs.
 * <br/>
 * Project dependencies can be mapped to project modules, or downloaded via "sources" jar.
 * Only supports JDK >= 17.
 */
@Mojo(name = "javadoc", requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
@Execute(phase = LifecyclePhase.NONE)
public class JavadocMojo extends AbstractMojo {

    private static final ArtifactHandler JAR_HANDLER = new DefaultArtifactHandler("jar");
    private static final String JAVADOC_EXE = OSType.currentOS() == OSType.Windows ? "javadoc.exe" : "javadoc";

    @Component
    @SuppressWarnings("unused")
    private PlexusLoggerHolder plexusLogHolder;

    /**
     * The entry point to Aether.
     */
    @Component
    private RepositorySystem repoSystem;

    /**
     * Manager used to look up Archiver/UnArchiver implementations.
     */
    @Component
    private ArchiverManager archiverManager;

    /**
     * Maven Project Builder component.
     */
    @Component
    private ProjectBuilder projectBuilder;

    /**
     * Toolchain manager use to look up the {@code javadoc} executable.
     */
    @Component
    private ToolchainManager toolchainManager;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * The current repository/network configuration of Maven.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project remote repositories to use.
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepos;

    /**
     * The project build output directory. (e.g. {@code target/})
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    private File buildDirectory;

    /**
     * The destination directory where javadoc saves the generated files.
     */
    @Parameter(property = "helidon.javadoc.outputDirectory",
               defaultValue = "${project.build.directory}/apidocs",
               required = true)
    private File outputDirectory;

    /**
     * The project root directory.
     */
    @Parameter(property = "helidon.javadoc.projectRoot",
               defaultValue = "${maven.multiModuleProjectDirectory}",
               required = true)
    private File projectRoot;

    /**
     * Project dependencies include patterns.
     * Format is {@code groupId:artifactId} with wildcard support.
     */
    @Parameter(property = "helidon.javadoc.dependencyIncludes", defaultValue = "*:*")
    private List<String> dependencyIncludes = List.of();

    /**
     * Project dependencies exclude pattern.
     * Format is {@code groupId:artifactId} with wildcard support.
     */
    @Parameter(property = "helidon.javadoc.dependencyExcludes")
    private List<String> dependencyExcludes = List.of();

    /**
     * Pom identity.
     * List of relative paths that must exist for a directory to be resolved as a Maven module.
     */
    @Parameter(property = "helidon.javadoc.pomScanningIdentity", defaultValue = "pom.xml")
    private List<String> pomScanningIdentity = List.of();

    /**
     * Pom scanning includes.
     * List of glob expressions used as an include filter for directories that may contain {@code pom.xml} files.
     */
    @Parameter(property = "helidon.javadoc.pomScanningIncludes", defaultValue = "**/*")
    private List<String> pomScanningIncludes = List.of();

    /**
     * Pom scanning excludes.
     * List of glob expressions used as an exclude filter for directories that may contain {@code pom.xml} files.
     */
    @Parameter(property = "helidon.javadoc.pomScanningExcludes", defaultValue = "**/target/**")
    private List<String> pomScanningExcludes = List.of();

    /**
     * Pom include patterns.
     * List of include filters (format is {@code groupId:artifactId:packaging} with wildcard support)
     * of scanned {@code pom.xml} files.
     */
    @Parameter(property = "helidon.javadoc.pomIncludes", defaultValue = "*:*:*")
    private List<String> pomIncludes = List.of();

    /**
     * Pom exclude patterns.
     * List of exclude filters (format is {@code groupId:artifactId:packaging} with wildcard support)
     * of scanned {@code pom.xml} files.
     */
    @Parameter(property = "helidon.javadoc.pomExcludes")
    private List<String> pomExcludes = List.of();

    /**
     * Whether to fall back to {@code sources-jar} when unable to resolve dependency sources from workspace.
     */
    @Parameter(property = "helidon.javadoc.sourcesJarFallback", defaultValue = "false")
    private boolean sourcesJarFallback;

    /**
     * Whether to resolve the module descriptor for sources by parsing {@code module-info.java}.
     * If {@code false} the module descriptor is resolved from the artifact using {@code ModuleFinder}.
     */
    @Parameter(property = "helidon.javadoc.parseModuleInfo", defaultValue = "true")
    private boolean parseModuleInfo;

    /**
     * Include patterns for unpacking {@code sources-jar}.
     */
    @Parameter(property = "helidon.javadoc.sourcesJarIncludes")
    private List<String> sourcesJarIncludes = List.of();

    /**
     * Excludes patterns for unpacking {@code sources-jar}.
     */
    @Parameter(property = "helidon.javadoc.sourcesJarExcludes")
    private List<String> sourcesJarExcludes = List.of();

    /**
     * Source directory include patterns.
     * List of glob expressions used as an include filter.
     */
    @Parameter(property = "helidon.javadoc.sourceIncludes", defaultValue = "**/*")
    private List<String> sourceIncludes = List.of();

    /**
     * Source directory exclude patterns.
     * List of glob expressions used as an exclude filter.
     */
    @Parameter(property = "helidon.javadoc.sourceExcludes", defaultValue = "**/src/test/java,**/generated-test-sources")
    private List<String> sourceExcludes = List.of();

    /**
     * Java module include patterns.
     * List of Java module names to include, wildcards are supported.
     */
    @Parameter(property = "helidon.javadoc.moduleIncludes", defaultValue = "*")
    private List<String> moduleIncludes = List.of();

    /**
     * Java modules exclude patterns.
     * List of Java module names to exclude, wildcards are supported.
     */
    @Parameter(property = "helidon.javadoc.moduleExcludes")
    private List<String> moduleExcludes = List.of();

    /**
     * Java packages include patterns.
     * List of Java package names to include, wildcards are supported.
     */
    @Parameter(property = "helidon.javadoc.packageIncludes", defaultValue = "*")
    private List<String> packageIncludes = List.of();

    /**
     * Java packages exclude patterns.
     * List of Java package names to exclude, wildcards are supported.
     */
    @Parameter(property = "helidon.javadoc.packageExcludes")
    private List<String> packageExcludes = List.of();

    /**
     * Set an additional option(s) on the command line. All input will be passed as-is to the
     * {@code @options} file. You must take care of quoting and escaping. Useful for a custom doclet.
     */
    @Parameter
    private String[] additionalOptions = new String[0];

    /**
     * See {@code javadoc --source}.
     */
    @Parameter(property = "helidon.javadoc.source", defaultValue = "${maven.compiler.source}")
    private String source;

    /**
     * See {@code javadoc --release}.
     */
    @Parameter(defaultValue = "${maven.compiler.release}")
    private String release;

    /**
     * See {@code javadoc -charset}. Defaults to the value of {@link #docencoding}.
     */
    @Parameter(property = "helidon.javadoc.charset")
    private String charset;

    /**
     * See {@code javadoc -docencoding}.
     */
    @Parameter(property = "helidon.javadoc.docencoding", defaultValue = "UTF-8")
    private String docencoding;

    /**
     * See {@code javadoc -encoding}.
     * If not specified, the encoding value will be the value of the {@code file.encoding} system property.
     */
    @Parameter(property = "helidon.javadoc.encoding", defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    /**
     * See {@code javadoc -bottom}.
     */
    @Parameter(property = "bottom",
               defaultValue = "Copyright &#169; {inceptionYear}&#x2013;{currentYear} {organizationName}. All rights reserved.")
    private String bottom;

    /**
     * See {@code javadoc -doctitle}.
     */
    @Parameter(property = "doctitle", defaultValue = "${project.name} ${project.version} API")
    private String doctitle;

    /**
     * See {@code javadoc -windowtitle}.
     */
    @Parameter(property = "helidon.javadoc.windowtitle", defaultValue = "${project.name} ${project.version} API")
    private String windowtitle;

    /**
     * See {@code javadoc --link}.
     */
    @Parameter(property = "helidon.javadoc.links")
    private ArrayList<String> links;

    /**
     * See {@code --linkoffline}.
     */
    @Parameter(property = "helidon.javadoc.offlineLinks")
    private OfflineLink[] offlineLinks;

    /**
     * See {@code -author}.
     */
    @Parameter(property = "helidon.javadoc.author", defaultValue = "true")
    private boolean author;

    /**
     * See {@code -use}.
     */
    @Parameter(property = "helidon.javadoc.use", defaultValue = "true")
    private boolean use;

    /**
     * See {@code -version}.
     */
    @Parameter(property = "helidon.javadoc.version", defaultValue = "true")
    private boolean version;

    /**
     * See {@code -Xdoclint}.
     */
    @Parameter(property = "helidon.javadoc.doclint")
    private String doclint;

    /**
     * See {@code -quiet}.
     */
    @Parameter(property = "helidon.javadoc.quiet", defaultValue = "false")
    private boolean quiet;

    /**
     * Skip this goal execution.
     */
    @Parameter(property = "helidon.javadoc.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Specifies if the build will fail if there are errors during javadoc execution or not.
     */
    @Parameter(property = "helidon.javadoc.failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * Specifies if the build will fail if there are warnings during javadoc execution or not.
     */
    @Parameter(property = "helidon.javadoc.failOnWarnings", defaultValue = "false")
    private boolean failOnWarnings;

    private Predicate<Artifact> dependencyFilter;
    private Predicate<MavenModel> pomFilter;
    private Predicate<Path> pomIdentityFilter;
    private Predicate<Path> pomScanningFilter;
    private Predicate<Path> sourceFilter;
    private Predicate<String> moduleFilter;
    private Predicate<String> packageFilter;
    private IncludeExcludeFileSelector[] sourcesJarSelectors;
    private Map<String, Path> workspace;
    private Path workDir;

    private final Map<String, JavadocModule> jars = new HashMap<>();
    private final Map<String, JavadocModule> sources = new HashMap<>();
    private final Map<String, Set<JavadocModule>> unresolved = new HashMap<>();
    private final Map<String, Set<JavadocModule>> resolved = new HashMap<>();
    private final Set<String> modulePath = new HashSet<>();
    private final Set<String> classPath = new HashSet<>();

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            Log.info("processing is skipped.");
            return;
        }

        // init filters
        sourcesJarSelectors = selectors(sourcesJarIncludes, sourcesJarExcludes);
        dependencyFilter = Filters.artifactFilter(dependencyIncludes, dependencyExcludes);
        pomFilter = Filters.pomFilter(pomIncludes, pomExcludes);
        sourceFilter = Filters.pathFilter(sourceIncludes, sourceExcludes, projectRoot.toPath());
        pomIdentityFilter = Filters.dirFilter(pomScanningIdentity);
        pomScanningFilter = Filters.pathFilter(pomScanningIncludes, pomScanningExcludes, projectRoot.toPath());
        moduleFilter = Filters.stringFilter(moduleIncludes, moduleExcludes);
        packageFilter = Filters.stringFilter(packageIncludes, packageExcludes);
        workDir = ensureDirectory(buildDirectory.toPath().resolve("javadoc-maven-plugin"));
        workspace = scanWorkspace();

        resolveJavadocModules();
        resolveJavaModules();
        resolvePaths();

        Path optionsFile = writeOptionsFile();
        Path argsFile = writeArgsFile();

        Log.info("Generated options file at %s", optionsFile);
        Log.info("Generated args file at %s", argsFile);

        String exe = javadocExecutable();
        List<String> cmd = List.of(exe, "@" + optionsFile, "@" + argsFile);

        try {
            RingBuffer<String> lines = new RingBuffer<>(10);
            ProcessMonitor.builder()
                    .processBuilder(new ProcessBuilder(cmd))
                    .autoEol(false)
                    .stdOut(PrintStreams.accept(DEVNULL, Log::info))
                    .stdErr(PrintStreams.accept(DEVNULL, Log::warn))
                    .filter(lines::add)
                    .build()
                    .execute(1, TimeUnit.DAYS);
            if (failOnWarnings) {
                for (String line : lines) {
                    if (line.matches("\\d+ warnings?")) {
                        throw new MojoExecutionException("Javadoc execution completed with " + line);
                    }
                }
            }
        } catch (IOException
                 | ProcessTimeoutException
                 | InterruptedException ex) {
            throw new RuntimeException(ex);
        } catch (ProcessFailedException ex) {
            if (failOnError) {
                throw new RuntimeException("Javadoc execution failed", ex);
            } else {
                Log.error("Javadoc execution failed");
            }
        }
    }

    private void resolveJavadocModules() {
        for (Artifact artifact : project.getArtifacts()) {
            if (!"jar".equals(artifact.getType())) {
                Log.debug("Dependency ignored (not a jar type): " + artifact);
                continue;
            }
            JavadocModule module;
            try {
                if (dependencyFilter.test(artifact)) {
                    Path dir = workspace.get(gav(artifact));
                    if (dir != null) {
                        Log.debug("Resolving source roots in directory: %s", dir);
                        Set<SourceRoot> sourceRoots = sourceRootsFromProjectFiles(dir);
                        module = new SourceModule(artifact, sourceRoots, moduleDescriptor(sourceRoots, artifact));
                    } else {
                        if (sourcesJarFallback) {
                            Log.info("Resolving source roots from sources-jar for: %s", artifact);
                            Set<SourceRoot> sourceRoots = sourceRootsFromSourceJar(artifact);
                            module = new SourceModule(artifact, sourceRoots, moduleDescriptor(sourceRoots, artifact));
                        } else {
                            Log.warn("Unable to resolve source roots for: %s", artifact);
                            module = new JarModule(artifact, moduleDescriptor(artifact), true);
                        }
                    }
                } else {
                    Log.debug("Dependency not included as a source module: %s", artifact);
                    module = new JarModule(artifact, moduleDescriptor(artifact), true);
                }
            } catch (Throwable ex) {
                Log.error(ex, "Unable to resolve javadoc module: %s (class-path only)", artifact);
                if (LogLevel.isDebug()) {
                    // Logging the full exception for troubleshooting
                    Log.log(LogLevel.DEBUG, ex, "Unable to resolve javadoc module");
                }
                module = new JarModule(artifact, null, true);
            }

            JavadocModule computed;
            if (module instanceof SourceModule) {
                computed = sources.compute(module.name(), module::merge);
            } else {
                computed = jars.compute(module.name(), module::merge);
            }
            if (computed instanceof CompositeJavadocModule cm) {
                if (!cm.name().equals(JavadocModule.INVALID)) {
                    Log.debug("Found module '%s' in multiple locations: %s",
                              cm.name(), Lists.join(cm.artifacts(), Artifact::getFile, " "));
                }
            }
        }
    }

    private void resolveJavaModules() {
        Map<String, Set<JavadocModule>> required = new HashMap<>();

        // system modules
        // those are not required to be on --module-path
        Set<String> systemModules = ModuleFinder.ofSystem()
                .findAll()
                .stream()
                .map(ModuleReference::descriptor)
                .map(ModuleDescriptor::name)
                .collect(toSet());

        // start with direct requires
        sources.forEach((name, module) -> module.requires(true).forEach(it -> {
            if (!sources.containsKey(it) && !systemModules.contains(it)) {
                required.computeIfAbsent(it, n -> new HashSet<>()).add(module);
            }
        }));

        // depth-first traversal
        Deque<String> stack = new ArrayDeque<>(required.keySet());
        while (!stack.isEmpty()) {
            String name = stack.pop();
            Set<JavadocModule> edges = required.getOrDefault(name, Set.of());
            JavadocModule module = jars.computeIfAbsent(name, n -> {
                Log.debug("Resolving %s from provided/optional dependencies", name);
                return resolveMissing(name, edges);
            });
            if (module != null) {
                resolved.computeIfAbsent(name, n -> new HashSet<>()).addAll(edges);
                module.requires(false).forEach(it -> {
                    if (!sources.containsKey(it) && !systemModules.contains(it)) {
                        required.computeIfAbsent(it, n -> new HashSet<>()).add(module);
                        stack.push(it);
                    }
                });
            } else {
                unresolved.computeIfAbsent(name, n -> new HashSet<>()).addAll(edges);
            }
        }

        if (!unresolved.isEmpty()) {
            unresolved.forEach((name, modules) -> Log.warn(
                    "Unresolved module: %s required by %s", name, Lists.join(modules, JavadocModule::name, ", ")));
        }
    }

    private void resolvePaths() {
        // we create a dummy src directory to put on the --module-source-path
        // for all the patched modules, layout is src/{module-name}
        Path moduleSrcDir = workDir.resolve("src");
        sources.forEach((name, it) -> {
            modulePath.add(normalizePath(it.artifact().getFile()));
            ensureDirectory(moduleSrcDir.resolve(name));
        });

        jars.forEach((name, it) -> {
            String path = normalizePath(it.artifact().getFile());
            if (resolved.containsKey(name)) {
                modulePath.add(path);
            } else {
                classPath.add(path);
            }
        });

        if (LogLevel.isDebug()) {
            List<SourceRoot> sourceRoots = Lists.flatMap(sources.values(), JavadocModule::sourceRoots);
            Log.debug("Resolved source roots: %s", Lists.join(sourceRoots, s -> normalizePath(s.dir()), ", "));
            Log.debug("Resolved module-path: %s", modulePath);
            Log.debug("Resolved class-path: %s", classPath);
        }
    }

    private Path writeOptionsFile() {
        List<String> options = new ArrayList<>();
        if (Strings.isValid(doclint)) {
            options.add("-Xdoclint:" + doclint);
        }
        addOption(options, "-d", normalizePath(outputDirectory));
        if (!addOption(options, "--release", release)) {
            addOption(options, "--source", source);
        }
        addOption(options, "--module-source-path", normalizePath(workDir.resolve("src")));
        addOption(options, "--module-path", String.join(pathSeparator, modulePath));
        addOption(options, "--class-path", String.join(pathSeparator, classPath));
        sources.forEach((name, it) -> {
            Set<SourceRoot> sourceRoots = it.sourceRoots();
            if (!sourceRoots.isEmpty()) {
                String value = Lists.join(sourceRoots, s -> normalizePath(s.dir()), pathSeparator);
                addOption(options, "--patch-module", name + "=" + value);
            }
        });
        addOption(options, "-docencoding", docencoding);
        if (!addOption(options, "-charset", charset)) {
            addOption(options, "-charset", docencoding);
        }
        addOption(options, "-bottom", bottomText());
        addOption(options, "-doctitle", doctitle);
        addOption(options, "-windowtitle", windowtitle);
        for (String link : links) {
            addOption(options, "-link", link);
        }
        for (OfflineLink offlineLink : offlineLinks) {
            addOption(options, "-linkoffline", offlineLink.getUrl(), normalizePath(offlineLink.getLocation()));
        }
        if (author) {
            options.add("-author");
        }
        if (use) {
            options.add("-use");
        }
        if (version) {
            options.add("-version");
        }
        if (quiet) {
            options.add("-quiet");
        }
        for (String option : additionalOptions) {
            options.add(option.replaceAll("(?<!\\\\)\\\\(?![\\\\:])", "\\\\"));
        }
        return writeLines("options", options);
    }

    private Path writeArgsFile() {
        List<String> args = new ArrayList<>();
        sources.forEach((name, module) -> {
            if (!moduleFilter.test(name)) {
                Log.debug("Excluding module: '%s' (provided by %s)", name, module.artifact());
                return;
            }
            for (SourceRoot sourceRoot : module.sourceRoots()) {
                sourceRoot.files().forEach((pkg, files) -> {
                    if (packageFilter.test(pkg) || pkg.isEmpty() && files.stream()
                            .allMatch(file -> "module-info.java".equals(fileName(file)))) {
                        for (Path file : files) {
                            args.add(normalizePath(file.toAbsolutePath()));
                        }
                    } else {
                        Log.debug("Excluding package: '%s' (provided by %s)", pkg, module.artifact());
                    }
                });
            }
        });
        return writeLines("argsfile", args);
    }

    private JavadocModule resolveMissing(String name, Set<JavadocModule> edges) {
        // forced resolution, module is likely in a provided/optional dependency
        // search in the "requiring" modules, so-called "edges".
        return edges.stream()
                .flatMap(JavadocModule::stream)
                .flatMap(it -> {
                    MavenProject pom = effectivePom(it.artifact());
                    return pom.getDependencies()
                            .stream()
                            // all the transitive dependencies of a visible edge have already been processed
                            // thus we only look in provided/optional dependencies
                            // otherwise, we look at all of them
                            .filter(dep -> !it.visible() || (dep.isOptional() || "provided".equals(dep.getScope())))
                            .map(this::resolveDependencies)
                            .flatMap(Collection::stream);
                })
                .flatMap(it -> {
                    try {
                        return Stream.of(new JarModule(it, moduleDescriptor(it), false));
                    } catch (Throwable ex) {
                        Log.error(ex, ex.getMessage());
                        return Stream.empty();
                    }
                })
                .filter(it -> it.name().equals(name))
                .findFirst()
                .orElse(null);
    }

    private Set<SourceRoot> sourceRootsFromSourceJar(Artifact dep) {
        File sourcesJar = resolveSourcesJar(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
        Path sourceRoot = workDir.resolve("source-jars").resolve(dep.toString());
        try {
            Log.debug("Unpacking %s to %s", sourcesJar, sourceRoot);
            Files.createDirectories(sourceRoot);
            UnArchiver unArchiver = archiverManager.getUnArchiver(sourcesJar);
            unArchiver.setSourceFile(sourcesJar);
            unArchiver.setDestDirectory(sourceRoot.toFile());
            unArchiver.setFileSelectors(sourcesJarSelectors);
            unArchiver.extract();
            List<Path> sourceFiles = FileUtils.walk(
                    sourceRoot, (path, attrs) -> attrs.isDirectory() || fileName(path).endsWith(".java"));
            return sourceRoots(sourceFiles);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (NoSuchArchiverException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<SourceRoot> sourceRootsFromProjectFiles(Path dir) {
        List<Path> nested = Lists.filter(workspace.values(), it -> !it.equals(dir) && it.startsWith(dir));
        List<Path> moduleSources = FileUtils.walk(
                dir, (path, attrs) -> attrs.isDirectory() && !nested.contains(path) || fileName(path).endsWith(".java"));
        return sourceRoots(moduleSources);
    }

    private Set<SourceRoot> sourceRoots(List<Path> sourceFiles) {
        Map<Path, SourceRoot> sourceRoots = new HashMap<>();
        Map<Path, String> packages = new HashMap<>();
        for (Path sourceFile : sourceFiles) {
            String pkg = packages.computeIfAbsent(sourceFile.getParent(), ignored -> {
                Log.debug("Parsing package name of %s", sourceFile);
                return JavaParser.packge(sourceFile);
            });
            Path dir = sourceRoot(sourceFile, pkg);
            if (sourceFilter.test(dir)) {
                sourceRoots.computeIfAbsent(dir, d -> new SourceRoot(d, new HashMap<>()))
                        .files()
                        .computeIfAbsent(pkg, p -> new HashSet<>())
                        .add(sourceFile);
            } else {
                Log.debug("Excluding source root: %s", dir);
            }
        }
        return new HashSet<>(sourceRoots.values());
    }

    private static Path sourceRoot(Path sourceFile, String pkg) {
        // walk up the package hierarchy
        Path path = sourceFile.getParent();
        long count = pkg.chars().filter(it -> it == '.').count();
        if (count > 0) {
            while (count >= 0) {
                path = path.getParent();
                count--;
            }
        }
        return path;
    }

    private ModuleDescriptor moduleDescriptor(Set<SourceRoot> sourceRoots, Artifact artifact) {
        return sourceRoots.stream()
                .filter(s -> parseModuleInfo)
                .map(s -> s.dir().resolve("module-info.java"))
                .filter(Files::exists)
                .findFirst()
                .map(this::moduleDescriptor)
                .orElseGet(() -> moduleDescriptor(artifact));
    }

    private ModuleDescriptor moduleDescriptor(Path path) {
        Log.debug("Parsing %s", path);
        return JavaParser.module(path);
    }

    private ModuleDescriptor moduleDescriptor(Artifact artifact) {
        Log.debug("Resolving module descriptor for %s", artifact);
        ModuleFinder mf = ModuleFinder.of(artifact.getFile().toPath());
        return mf.findAll().iterator().next().descriptor();
    }

    private File resolveSourcesJar(String groupId, String artifactId, String version) {
        return resolveArtifact(groupId, artifactId, version, "sources", "jar");
    }

    private File resolveArtifact(String groupId, String artifactId, String version, String classifier, String type) {
        try {
            ArtifactRequest request = new ArtifactRequest();
            request.setArtifact(new org.eclipse.aether.artifact.DefaultArtifact(
                    groupId, artifactId, classifier, type, version));
            request.setRepositories(remoteRepos);
            return repoSystem.resolveArtifact(repoSession, request).getArtifact().getFile();
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Artifact> resolveDependencies(Dependency dep) {
        try {
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(new org.eclipse.aether.graph.Dependency(
                    new org.eclipse.aether.artifact.DefaultArtifact(
                            dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(), dep.getType(), dep.getVersion()),
                    "compile"));
            DependencyNode node = repoSystem.collectDependencies(repoSession, collectRequest).getRoot();
            DependencyRequest dependencyRequest = new DependencyRequest();
            dependencyRequest.setRoot(node);
            return repoSystem.resolveDependencies(repoSession, dependencyRequest).getArtifactResults()
                    .stream()
                    .map(ArtifactResult::getArtifact)
                    .filter(it -> "jar".equals(it.getExtension()))
                    .map(it -> {
                        Artifact artifact = new DefaultArtifact(
                                it.getGroupId(), it.getArtifactId(), it.getVersion(),
                                "compile", it.getExtension(), it.getClassifier(), JAR_HANDLER);
                        artifact.setFile(it.getFile());
                        return artifact;
                    })
                    .toList();
        } catch (DependencyCollectionException
                 | DependencyResolutionException e) {
            throw new RuntimeException(e);
        }
    }

    private MavenProject effectivePom(Artifact dep) {
        try {
            ProjectBuildingRequest pbr = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
            pbr.setRemoteRepositories(project.getRemoteArtifactRepositories());
            pbr.setPluginArtifactRepositories(project.getPluginArtifactRepositories());
            pbr.setProject(null);
            pbr.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
            pbr.setResolveDependencies(true);
            File pomFile = resolveArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), null, "pom");
            return projectBuilder.build(pomFile, pbr).getProject();
        } catch (ProjectBuildingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Map<String, Path> scanWorkspace() {
        try (Stream<Path> stream = Files.walk(projectRoot.toPath())) {
            return stream
                    .filter(pomIdentityFilter)
                    .filter(pomScanningFilter)
                    .map(it -> {
                        Path file = it.resolve("pom.xml");
                        Log.debug("Reading model %s", file);
                        return Map.entry(MavenModel.read(file), it);
                    })
                    .filter(it -> pomFilter.test(it.getKey()))
                    .collect(toMap(e -> gav(e.getKey()), Map.Entry::getValue));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String bottomText() {
        String inceptionYear = project.getInceptionYear();
        LocalDate localDate = LocalDate.now();
        String currentYear = Integer.toString(localDate.getYear());
        String text = bottom;
        if (Strings.isValid(currentYear)) {
            text = text.replace("{currentYear}", currentYear);
        }
        if (Strings.isValid(inceptionYear)) {
            text = text.replace("{inceptionYear}", currentYear);
        } else {
            text = text.replace("{inceptionYear}&#x2013;", currentYear);
        }
        Organization organization = project.getOrganization();
        if (organization != null && Strings.isValid(organization.getName())) {
            if (Strings.isValid(organization.getUrl())) {
                text = text.replace(
                        "{organizationName}",
                        String.format("<a href=\"%s\">%s</a>",
                                      organization.getUrl(),
                                      organization.getName()));
            } else {
                text = text.replace("{organizationName}", organization.getName());
            }
        } else {
            text = text.replace(" {organizationName}", "");
        }
        return text;
    }

    private Path writeLines(String filename, List<String> lines) {
        Path file = workDir.resolve(filename);
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String javadocExecutable() {
        return Optional.ofNullable(toolchainManager.getToolchainFromBuildContext("jdk", session))
                .map(tc -> {
                    Log.debug("Searching for javadoc executable in toolchain: %s", tc.getType());
                    return tc.findTool("javadoc");
                })
                .or(() -> {
                    Log.debug("Unable to find javadoc from toolchain");
                    return findExecutableInPath(JAVADOC_EXE).map(Path::toString);
                })
                .or(() -> {
                    Log.debug("Searching for javadoc executable in JAVA_HOME");
                    return findExecutableInJavaHome(JAVADOC_EXE).map(Path::toString);
                }).orElseThrow(() -> new IllegalStateException("Unable to find javadoc executable"));
    }

    private static String gav(Artifact dep) {
        return String.format("%s:%s:%s", dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
    }

    private static String gav(MavenModel pom) {
        return String.format("%s:%s:%s", pom.getGroupId(), pom.getArtifactId(), pom.getVersion());
    }

    private static boolean addOption(List<String> args, String option, Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value != null) {
                String str = value.toString();
                if (!str.isEmpty()) {
                    str = str.replace("'", "\\'");
                    str = str.replace("\n", " ");
                    str = "'" + str + "'";
                    sb.append(str);
                    if (i < values.length - 1) {
                        sb.append(" ");
                    }
                }
            }
        }
        if (!sb.isEmpty()) {
            args.add(option);
            args.add(sb.toString());
            return true;
        }
        return false;
    }

    private static IncludeExcludeFileSelector[] selectors(List<String> includes, List<String> excludes) {
        if (!excludes.isEmpty() || !includes.isEmpty()) {
            IncludeExcludeFileSelector selector = new IncludeExcludeFileSelector();
            selector.setExcludes(excludes.toArray(new String[0]));
            selector.setIncludes(includes.toArray(new String[0]));
            return new IncludeExcludeFileSelector[] {selector};
        }
        return null;
    }
}
