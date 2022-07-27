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

package io.helidon.lsp.server.management;

import com.google.inject.Module;
import io.helidon.build.common.maven.MavenCommand;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.cli.CliRequest;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DefaultProjectBuilder;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.DefaultProjectDependenciesResolver;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectDependenciesResolver;
//import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.launcher.Launcher;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static io.helidon.build.common.FileUtils.USER_HOME_DIR;
import static io.helidon.build.common.FileUtils.requireDirectory;
import static org.eclipse.aether.util.artifact.JavaScopes.COMPILE;
import static org.eclipse.aether.util.artifact.JavaScopes.PROVIDED;
import static org.eclipse.aether.util.artifact.JavaScopes.RUNTIME;
import static org.eclipse.aether.util.artifact.JavaScopes.SYSTEM;
import static org.eclipse.aether.util.artifact.JavaScopes.TEST;
import static org.eclipse.aether.util.filter.DependencyFilterUtils.classpathFilter;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MavenSupportTest {

    private static final DependencyFilter DEPENDENCY_FILTER = classpathFilter(COMPILE, RUNTIME, TEST, PROVIDED, SYSTEM);

    private PlexusContainer container;

    @Test
    public void testMainCLI() throws Exception {
        String[] args = {"io.helidon.ide-support:helidon-lsp-maven-plugin:3.0.0-SNAPSHOT:build-classpath"};
        String pomPath = "/home/aserkes/IdeaProjects/helidon-build-tools/ide-support/lsp/io.helidon.lsp.server";

        String CLASSWORLDS_CONF = "classworlds.conf";
        String UBERJAR_CONF_DIR = "WORLDS-INF/conf/";
        String classworldsConf = System.getProperty( CLASSWORLDS_CONF );
        InputStream is;
        Launcher launcher = new Launcher();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        launcher.setSystemClassLoader( cl );
        if ( classworldsConf != null ) {
            is = new FileInputStream( classworldsConf );
        } else {
            if ( "true".equals( System.getProperty( "classworlds.bootstrapped" ) ) ) {
                is = cl.getResourceAsStream( UBERJAR_CONF_DIR + CLASSWORLDS_CONF );
            } else {
                is = cl.getResourceAsStream( CLASSWORLDS_CONF );
            }
        }

        if ( is == null ) {
            throw new Exception( "classworlds configuration not specified nor found in the classpath" );
        }

        launcher.configure( is );
        ClassWorld world = launcher.getWorld();
        is.close();

        MavenCli cli = new MavenCli();
        Class c = Class.forName("org.apache.maven.cli.CliRequest");
        Constructor ct = //c.getDeclaredConstructors()[1];
                c.getDeclaredConstructor(new Class[]{
                        Arrays.class,
                        ClassWorld.class});
        ct.setAccessible(true);
        CliRequest cliRequest = (CliRequest) ct.newInstance(args, world);
        int result = cli.doMain(cliRequest);

        System.out.println(result);
    }

    @Deprecated
    @Test
    public void testResolveDependency() throws Exception {
        String pomPath = "/home/aserkes/IdeaProjects/helidon-build-tools/ide-support/lsp/io.helidon.lsp.server/pom.xml";
        setupContainer();
        List<Dependency> dependencies;
        DefaultProjectDependenciesResolver dependenciesResolver =
                (DefaultProjectDependenciesResolver) container.lookup(ProjectDependenciesResolver.class, "resolver");
        try (final FileReader reader = new FileReader(pomPath)) {
            final Model model = new MavenXpp3Reader().read(reader);
            MavenProject project = new MavenProject(model);
            project.setPomFile(new File(pomPath));
            DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
            LocalRepositoryManager localRepositoryManager = newLocalRepositoryManager(localRepoDir());
            session.setLocalRepositoryManager(localRepositoryManager);

            DependencyResolutionResult resolutionResult;
            try {
                DefaultDependencyResolutionRequest resolution = new DefaultDependencyResolutionRequest(project, session);
                resolutionResult = dependenciesResolver.resolve(resolution);
            } catch (DependencyResolutionException e) {
                resolutionResult = e.getResult();
            }

            dependencies = resolutionResult.getDependencies();
        }

        dependencies.forEach(dependency -> System.out.println(dependency.getArtifact().getFile().getAbsolutePath()));
        System.out.println(container);
    }

    @Test
    public void testCallMojoBackup1() throws Exception {
        setupContainer();
        DefaultModelBuilder modelBuilder = (DefaultModelBuilder) container.lookup(ModelBuilder.class, "builder");
        ModelResolver modelResolver = makeModelResolver();
        Model model = resolveEffectiveModel(
                new File("/home/aserkes/IdeaProjects/helidon-build-tools/ide-support/lsp/io.helidon.lsp.server/pom.xml"),
                modelBuilder,
                modelResolver
        );

        List<org.apache.maven.model.Dependency> dependencies = model.getDependencies();
//        dependencies.forEach(dep->dep.get);
        System.out.println();
    }

    public Model resolveEffectiveModel(File pomfile, DefaultModelBuilder modelBuilder, ModelResolver modelResolver) {
        try {
            return modelBuilder.build(makeModelBuildRequest(pomfile, modelResolver)).getEffectiveModel();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ModelBuildingRequest makeModelBuildRequest(File artifactFile, ModelResolver modelResolver) {
        DefaultModelBuildingRequest mbr = new DefaultModelBuildingRequest();
        mbr.setPomFile(artifactFile);
        mbr.setModelResolver(modelResolver); // <-- the hard-to-get modelResolver
        return mbr;
    }

    private Object invoke(Object object, String method)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return object.getClass().getMethod(method).invoke(object);
    }

    private org.apache.maven.model.resolution.ModelResolver makeModelResolver() throws MojoExecutionException {
        try (FileReader reader = new FileReader("/home/aserkes/IdeaProjects/helidon-build-tools/ide-support/lsp/io.helidon.lsp" +
                ".server/pom.xml")) {
            final Model model = new MavenXpp3Reader().read(reader);
            MavenProject project = new MavenProject(model);
            ProjectBuildingRequest projectBuildingRequest =
                    (ProjectBuildingRequest) invoke(project, "getProjectBuildingRequest");

            Class c = Class.forName("org.apache.maven.repository.internal.DefaultModelResolver");
            Constructor ct = //c.getDeclaredConstructors()[1];
                    c.getDeclaredConstructor(new Class[]{
                    RepositorySystemSession.class,
                    RequestTrace.class,
                    String.class,
                    ArtifactResolver.class,
                    VersionRangeResolver.class,
                    RemoteRepositoryManager.class,
                    List.class});
            ct.setAccessible(true);
            DefaultArtifactResolver artifactResolver = (DefaultArtifactResolver) container.lookup(ArtifactResolver.class,
                    "artifactResolver");
            DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
            LocalRepositoryManager localRepositoryManager = newLocalRepositoryManager(localRepoDir());
            session.setLocalRepositoryManager(localRepositoryManager);
            return (org.apache.maven.model.resolution.ModelResolver) ct.newInstance(new Object[]{
                    session,
                    null,
                    null,
                    artifactResolver,
                    new DefaultVersionRangeResolver(),
                    new DefaultRemoteRepositoryManager(),
                    List.of()});
        } catch (Exception e) {
            throw new MojoExecutionException("Error instantiating DefaultModelResolver", e);
        }
    }

    @Test
    public void testCallMojoBackup() throws Exception {
        setupContainer();
        DefaultProjectDependenciesResolver dependenciesResolver =
                (DefaultProjectDependenciesResolver) container.lookup(ProjectDependenciesResolver.class, "resolver");
//        DefaultProjectBuilder builder = (DefaultProjectBuilder)container.lookup(ProjectBuilder.class, "projectBuilder");
        try (final FileReader reader = new FileReader("/home/aserkes/IdeaProjects/helidon-build-tools/ide-support/lsp/io" +
                ".helidon.lsp.server/pom.xml")) {
            final Model model = new MavenXpp3Reader().read(reader);

            MavenProject project = new MavenProject(model);
            project.setPomFile(new File("/home/aserkes/IdeaProjects/helidon-build-tools/ide-support/lsp/io.helidon.lsp" +
                    ".server/pom.xml"));
            DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
            LocalRepositoryManager localRepositoryManager = newLocalRepositoryManager(localRepoDir());
            session.setLocalRepositoryManager(localRepositoryManager);

            MavenProject project1 = newMavenProject(Path.of("/home/aserkes/IdeaProjects/helidon-build-tools/ide-support/lsp/io" +
                    ".helidon.lsp.server/pom" +
                    ".xml"), localRepositoryManager);

            DependencyResolutionResult resolve = dependenciesResolver.resolve(new DefaultDependencyResolutionRequest(project,
                    session)
            );//.setResolutionFilter(DEPENDENCY_FILTER)
            List<Dependency> dependencies = resolve.getDependencies();
            DependencyNode dependencyGraph = resolve.getDependencyGraph();

            DependencyResolutionResult resolutionResult;
            try {
                DefaultDependencyResolutionRequest resolution = new DefaultDependencyResolutionRequest(project, session);
                resolutionResult = dependenciesResolver.resolve(resolution);
            } catch (DependencyResolutionException e) {
                resolutionResult = e.getResult();
            }
            Set<org.apache.maven.artifact.Artifact> artifacts = new LinkedHashSet<>();
            if (resolutionResult.getDependencyGraph() != null) {
                RepositoryUtils.toArtifacts(artifacts, resolutionResult.getDependencyGraph().getChildren(),
                        Collections.singletonList(project.getModel().getArtifactId()), null);

                // Maven 2.x quirk: an artifact always points at the local repo, regardless whether resolved or not
                LocalRepositoryManager lrm = session.getLocalRepositoryManager();
                for (org.apache.maven.artifact.Artifact artifact : artifacts) {
                    if (!artifact.isResolved()) {
                        String path = lrm.getPathForLocalArtifact(RepositoryUtils.toArtifact(artifact));
                        artifact.setFile(new File(lrm.getRepository().getBasedir(), path));
                    }
                }
            }
            project.setResolvedArtifacts(artifacts);
            project.setArtifacts(artifacts);

////////////////////
            var result = new HashSet<String>();
            List<org.apache.maven.model.Dependency> dependencies1 = project.getDependencies();
            for (org.apache.maven.model.Dependency dependency : dependencies1) {
                Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(),
                        dependency.getType(), dependency.getVersion());
                Dependency dep = new Dependency(artifact, dependency.getScope());
                getDependencies(dep, session, result);
            }
//            for (Dependency dependency : dependencies) {
//                getDependencies(dependency, session, result);
//            }
///////////////////////
//            dependencies.forEach(dependency -> dependency);
            System.out.println(container);
        }

        System.out.println(container);
    }

    MavenProject newMavenProject(Path pomFile,
                                 LocalRepositoryManager localRepositoryManager) throws IOException, XmlPullParserException {
        final FileReader reader = new FileReader(pomFile.toFile());
        File pom = pomFile.toFile();
        Model model = new MavenXpp3Reader().read(reader);
        MavenProject project = new MavenProject(model);
        project.setFile(pom);
        return addParent(project, localRepositoryManager);
    }

    private MavenProject addParent(MavenProject childProject, LocalRepositoryManager repoManager) throws IOException,
            XmlPullParserException {
        Parent parent = childProject.getModel().getParent();
        if (parent != null) {
            String parentPom = repoManager.getPathForLocalArtifact(new DefaultArtifact(parent.getGroupId(),
                    parent.getArtifactId(),
                    null,
                    "pom",
                    parent.getVersion()));
            Path pomFile = localRepoDir().resolve(parentPom);
            MavenProject parentProject = newMavenProject(pomFile, repoManager);
            childProject.setParent(parentProject);
        }
        return childProject;
    }

    private void getDependencies(Dependency dependency, DefaultRepositorySystemSession session,
                                 HashSet<String> result) throws DependencyCollectionException,
            org.eclipse.aether.resolution.DependencyResolutionException {
        RepositorySystem repositorySystem = MavenRepositorySystemUtils
                .newServiceLocator()
                .getService(RepositorySystem.class);
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
//            collectRequest.addRepository( repository );
        DependencyNode node = repositorySystem.collectDependencies(session, collectRequest).getRoot();

        DependencyRequest dependencyRequest = new DependencyRequest();
        dependencyRequest.setRoot(node);

        DependencyResult dependencyResult = repositorySystem.resolveDependencies(session, dependencyRequest);

        Set<String> collect = dependencyResult.getArtifactResults().stream().map(r -> r.getArtifact().getFile().getName())
                                              .collect(Collectors.toSet());
        collect.add(dependency.getArtifact().getFile().getName());
        result.addAll(collect);
    }

    private static final String LOCAL_REPO_PROPERTY = "maven.repo.local";
    private static final String DEFAULT_LOCAL_REPO_DIR = ".m2/repository";
    private static final String LOCAL_REPO_MANAGER_CLASS = "org.eclipse.aether.internal.impl.SimpleLocalRepositoryManager";

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

    void setupContainer() {
        ContainerConfiguration cc = setupContainerConfiguration();
        try {
            List<Module> modules = new ArrayList<Module>();
            addGuiceModules(modules);
            container = new DefaultPlexusContainer(cc, modules.toArray(new Module[modules.size()]));
        } catch (PlexusContainerException e) {
            e.printStackTrace();
            fail("Failed to create plexus container.");
        }
    }

    void addGuiceModules(List<Module> modules) {
        // no custom guice modules by default
    }

    ContainerConfiguration setupContainerConfiguration() {
        ClassWorld classWorld = new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());

        ContainerConfiguration cc = new DefaultContainerConfiguration()
                .setClassWorld(classWorld)
                .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                .setAutoWiring(true)
                .setName("maven");

        return cc;
    }

    @Disabled
    @Test//IT SEEMS THAT IT DOES NOT WORK
    public void testMavenEmbedder() {
        MavenCli cli = new MavenCli();
        System.setProperty("maven.multiModuleProjectDirectory", "true");
        cli.doMain(new String[]{"dependency"}, "/home/aserkes/IdeaProjects/helidon-build-tools/ide-support/lsp/io.helidon.lsp" +
                ".server", System.out, System.out);
    }

    @Disabled
    @Test
    public void testPlexus() throws PlexusContainerException, ComponentLookupException, DependencyResolutionException {
        MavenProject project = new MavenProject();
        project.setPomFile(new File("/home/aserkes/IdeaProjects/helidon-build-tools/ide-support/lsp/io.helidon.lsp.server/pom" +
                ".xml"));
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();

        PlexusContainer container = new DefaultPlexusContainer();
        DefaultProjectDependenciesResolver lookup =
                (DefaultProjectDependenciesResolver) container.lookup(ProjectDependenciesResolver.class, "resolver");

        List<Dependency> dependencies = lookup.resolve(new DefaultDependencyResolutionRequest(project, session)
                                                      .setResolutionFilter(DEPENDENCY_FILTER))
                                              .getDependencies();
        dependencies.size();

    }

    @Test
    public void test() throws DependencyResolutionException, MavenInvocationException {
        long time = System.currentTimeMillis();
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBatchMode(true);
        request.setPomFile(new File("/home/aserkes/IdeaProjects/helidon-build-tools/ide-support/lsp/pom.xml"));
        request.setGoals(Collections.singletonList("dependency:list"));

        Invoker invoker = new DefaultInvoker();
        Path path1 = MavenCommand.mavenExecutable();
        Path path2 = MavenCommand.mavenHome();
        invoker.setMavenExecutable(path1.toFile());
        System.setProperty("maven.home", path2.toString());
        List<String> results = new ArrayList<>();
        request.setOutputHandler(list -> {
            results.add(list);
        });
//        invoker.setOutputHandler(list-> {
//            results.add(list);
//        });

        Properties properties = new Properties();
//        properties.setProperty("outputFile", "dependencies.txt"); // redirect output to a file
        properties.setProperty("silent", "true");
        properties.setProperty("outputAbsoluteArtifactFilename", "true"); // with paths
        properties.setProperty("includeScope", "runtime"); // only runtime (scope compile + runtime)
// if only interested in scope runtime, you may replace with excludeScope = compile
        request.setProperties(properties);


        InvocationResult result1 = invoker.execute(request);
        System.out.println(System.currentTimeMillis() - time);
        result1.getExitCode();

//        DefaultProjectBuildingRequest request = new DefaultProjectBuildingRequest();
//        MavenProject project = new MavenProject();
//        project.setPomFile(new File("pom.xml"));
//        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
//        MavenRepositorySystemSession session1 = new MavenRepositorySystemSession();
//        org.sonatype.aether.repository.LocalRepository localRepository1 = session1.getLocalRepository();
//        LocalRepository localRepository = session.getLocalRepository();
//        DefaultProjectDependenciesResolver resolver = new DefaultProjectDependenciesResolver();
//        List<Dependency> dependencies = resolver.resolve(new DefaultDependencyResolutionRequest(project, session)
//                                                        .setResolutionFilter(DEPENDENCY_FILTER))
//                                                .getDependencies();
//        dependencies.size();

//        InvocationRequest request = new DefaultInvocationRequest();
//        request.setPomFile(new File("/home/aserkes/IdeaProjects/helidon-build-tools/ide-support/lsp/pom.xml"));
//        request.setGoals(Arrays.asList("dependency:list"));

        String mavenHomePath = System.getProperty("maven.home");

        if (mavenHomePath == null) {
            Map<String, String> filteredMap = new HashMap<>();
            final List<String> searchEnv = Arrays.asList(new String[]{"m3_home", "m3home", "m2_home", "m2home", "maven_home",
                    "mavenhome", "path"});
            for (final Map.Entry<String, String> entry : System.getenv().entrySet()) {
                final String key = entry.getKey().toLowerCase();
                if (searchEnv.contains(key)) {
                    filteredMap.put(key, entry.getValue());
                }
            }
            for (String key : searchEnv) {
                for (final String pathEnv : filteredMap.values()) {
                    for (final String path : pathEnv.split(File.pathSeparator)) {
                        mavenHomePath = "";//checkForMavenHomeIn(new File(path));
                        if (mavenHomePath != null) break;
                    }
                    if (mavenHomePath != null) break;
                }
                if (mavenHomePath != null) break;
            }
        }

        System.setProperty("maven.home", mavenHomePath);

//        Invoker invoker = new DefaultInvoker();
// the Maven home can be omitted if the "maven.home" system property is set
//        invoker.setMavenHome(new File("/path/to/maven/home"));
//        invoker.setOutputHandler(null); // not interested in Maven output itself
//        InvocationResult result = invoker.execute(request);
//        if (result.getExitCode() != 0) {
//            throw new IllegalStateException("Build failed.");
//        }
    }

    @Test
    public void getPomForFileTest() throws URISyntaxException, IOException {
        String pomForFile = getPomForCurrentClass();
        assertTrue(pomForFile.endsWith("pom.xml"));
    }

    @Test
    public void getDependenciesTest() throws URISyntaxException, IOException {
        String pomForFile = getPomForCurrentClass();
        List<String> dependencies = MavenSupport.getInstance().getDependencies(pomForFile);
        assertTrue(dependencies.size() > 0);
    }

    @Test
    public void getAllDependenciesTest() throws URISyntaxException, IOException {
        String pomForFile = getPomForCurrentClass();
        List<String> dependencies = MavenSupport.getInstance().getAllDependencies(pomForFile);
        assertTrue(dependencies.size() > 0);
    }

    private String getPomForCurrentClass() throws IOException, URISyntaxException {
        URI uri = MavenSupportTest.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        return MavenSupport.getInstance().getPomForFile(uri.getPath());
    }

    @Test
    public void clientSocketTest() throws IOException {
         ServerSocket serverSocket;
         Socket clientSocket;
         PrintWriter out;
         BufferedReader in;


            serverSocket = new ServerSocket(33133);
            clientSocket = serverSocket.accept();
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String greeting = in.readLine();
        System.out.println(greeting);
    }
}