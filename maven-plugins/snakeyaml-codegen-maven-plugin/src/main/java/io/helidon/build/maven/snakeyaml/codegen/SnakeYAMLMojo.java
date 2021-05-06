/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package io.helidon.build.maven.snakeyaml.codegen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Goal which generates code to help with parsing YAML and JSON using SnakeYAML.
 * <p>
 *     SnakeYAML does a lot of work converting between JSON or YAML and bean classes, but it needs
 *     some help with information it cannot get from compiled class bytecode. This plug-in
 *     generates that helper code.
 * </p>
 * <p>
 *     It analyzes Java sources in a specified location and generates a SnakeYAML
 *     {@code TypeDefinition} for each class. It also uses a second location to locate implementations of interfaces from the
 *     first area for use in setting the impl classes in those {@code TypeDefinition} instances.
 * </p>
 * <p>
 *     Often, builds will use the maven dependency plug-in to extract sources into those
 *     two locations and then invoke the plug-in to analyze the code and generate the helper class.
 * </p>
 *
 */
@Mojo(name = "generate",
      requiresProject = true,
      defaultPhase = LifecyclePhase.GENERATE_SOURCES,
      requiresDependencyResolution = ResolutionScope.RUNTIME)
public class SnakeYAMLMojo extends AbstractMojo {
//
//<p>
//     Here is an example from the maven dependency plug-in showing how to extract the sources so
//     they are available for this plug-in to consume:
// </p>
// <pre>{@code
// <project>
//     [...]
//     <build>
//         <plugins>
//         <plugin>
//             <groupId>org.apache.maven.plugins</groupId>
//             <artifactId>maven-dependency-plugin</artifactId>
//              <executions>
//                  <execution>
//                      <id>unpack-openapi-interfaces</id>
//                      <goals>
//                          <goal>unpack-dependencies</goal>
//                      </goals>
//                      <phase>generate-sources</phase>
//                      <configuration>
//                          <classifier>sources</classifier>
//                          <failOnMissingClassifierArtifact>true</failOnMissingClassifierArtifact>
//                          <outputDirectory>${openapi-interfaces-dir}</outputDirectory>
//                          <includeGroupIds>org.eclipse.microprofile.openapi</includeGroupIds>
//                          <includeArtifactIds>microprofile-openapi-api</includeArtifactIds>
//                          <includes>org/eclipse/microprofile/openapi/models/**/*.java</includes>
//                      </configuration>
//                  </execution>
//                  <execution>
//                      <id>unpack-openapi-impls</id>
//                      <goals>
//                          <goal>unpack-dependencies</goal>
//                      </goals>
//                      <phase>generate-sources</phase>
//                      <configuration>
//                          <classifier>sources</classifier>
//                          <failOnMissingClassifierArtifact>true</failOnMissingClassifierArtifact>
//                          <outputDirectory>${openapi-impls-dir}</outputDirectory>
//                          <includeGroupIds>io.smallrye</includeGroupIds>
//                          <includeArtifactIds>smallrye-open-api</includeArtifactIds>
//                          <includes>io/smallrye/openapi/api/models/**/*.java</includes>
//                      </configuration>
//                  </execution>
//              </executions>
//          </plugin>
//     </plugins>
//   </build>
//   [...]
// </project>
// }
//
// </pre>
//
//

    private static final String PROPERTY_PREFIX = "snakeyamlgen.";
    private static final String PARAM_DUMP_FORMAT = "Code generation for SnakeYAML parsing helper:%n"
            + "  %s:%n"
            + "    Read from directory %s%n"
            + "    Matching %s%n"
            + "    Excluding %s%n";

    private static final String DEFAULT_OUTPUT_CLASS_NAME = "io.helidon.snakeyaml.SnakeYAMLParserHelper";

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    private final Map<String, Type> types = new HashMap<>();
    private final Map<String, Type> implementations = new HashMap<>();
    private final Set<Import> imports = preloadedImports();
    private final Map<String, List<String>> interfaces = new HashMap<>();

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject mavenProject;

    /**
     * Directory to contain the generated class source file.
     */
    @Parameter(property = PROPERTY_PREFIX + "outputDirectory",
                defaultValue = "${project.build.directory}/generated-sources",
                required = true)
    private File outputDirectory;

    /**
     * Fully-qualified name for the class to generate.
     */
    @Parameter(property = PROPERTY_PREFIX + "outputClass", defaultValue = DEFAULT_OUTPUT_CLASS_NAME, required = true)
    private String outputClass;

    /**
     * Configuration for compiling the interfaces for which SnakeYAML will need to parse.
     */
    @Parameter(property = PROPERTY_PREFIX + "interfacesConfig", required = true)
    private CompilerConfig interfacesConfig;

    /**
     * Configuration for compiling the implementations SnakeYAML will use to instantiate parsed interfaces.
     */
    @Parameter(property = PROPERTY_PREFIX + "implementationsConfig", required = true)
    private CompilerConfig implementationsConfig;

    /**
     * Controls debug output from the plug-in.
     */
    @Parameter(property = PROPERTY_PREFIX + "debug", defaultValue = "false")
    private boolean debug;

    /**
     * Prefix common to all implementation classes submitted for analysis. Used for grouping import statements in the generated
     * code.
     */
    @Parameter(property = PROPERTY_PREFIX + "implementationPrefix", required = true)
    private String implementationPrefix;

    /**
     * Prefix common to all interfaces submitted for analysis. Used for grouping import statements in the generated code.
     */
    @Parameter(property = PROPERTY_PREFIX + "interfacePrefix", required = true)
    private String interfacePrefix;

    /**
     * Prescribes how the plug-in finds Java classes to analyze for either the interfaces or the
     * implementations.
     */
    public static class CompilerConfig {

        /**
         * Directory where the source files to be compiled are located.
         */
        private String inputDirectory = null; // defaults to "interfaces" or "implementations"

        /**
         * Java file path expressions indicating which files to include in analysis.
         */
        private List<String> includes = defaultIncludes();

        /**
         * Java file path expressions indicating which files to exclude from analysis.
         */
        private List<String> excludes = Collections.emptyList();

        private static List<String> defaultIncludes() {
            final List<String> result = new ArrayList<>();
            result.add("**/*.java");
            return result;
        }
    }

    /**
     * Sets the compiler config for analyzing the interfaces.
     *
     * @param config CompilerConfig for analyzing interfaces
     */
    public void setInterfacesConfig(CompilerConfig config) {
        this.interfacesConfig = fillInConfig(config, "interfaces");
    }

    /**
     * Sets the compiler config for analyzing the implementations.
     *
     * @param config CompilerConfig for analyzing implementations
     */
    public void setImplementationsConfig(CompilerConfig config) {
        this.implementationsConfig = fillInConfig(config, "implementations");
    }

    private CompilerConfig fillInConfig(CompilerConfig config, String defaultInputDirectory) {
        if (config.inputDirectory == null) {
            config.inputDirectory = mavenProject.getBuild().getOutputDirectory().concat("/").concat(defaultInputDirectory);
        }
        return config;
    }

    /**
     * Runs the goal, analyzing interfaces and implementations and generating the helper class.
     *
     * @throws MojoExecutionException in case of errors while executing the goal
     */
    public void execute() throws MojoExecutionException {
        validateParameters();

        dumpParams();

        File f = outputDirectory;
        if (!f.exists()) {
            if (f.mkdirs()) {
                debugLog(() -> "Created output directory " + f.getAbsolutePath());
            } else {
                debugLog(() -> "Using existing output directory " + f.getAbsolutePath());
            }
        }

        try {
            analyzeInterfaces(types, imports);

            addImportsForTypes(types, imports);

            analyzeImplementations(implementations, imports, interfaces);

            associateImplementationsWithInterfaces(types, interfaces);

            generateHelperClass(types, imports);

            addGeneratedCodeToCompilation();

        } catch (Throwable e) {
            throw new MojoExecutionException("Error compiling and analyzing source files", e);
        }
    }

    Set<Import> imports() {
        return imports;
    }

    Map<String, List<String>> interfaces() {
        return interfaces;
    }

    Map<String, Type> types() {
        return types;
    }

    private void debugLog(Supplier<String> msgSupplier) {
        if (getLog().isDebugEnabled() || debug) {
            getLog().debug(msgSupplier.get());
        }
    }

    private void dumpParams() {
        if (!getLog().isDebugEnabled() &&  !debug) {
            return;
        }
        debugLog(() -> String.format("Output directory: %s%nOutput class: %s%n", outputDirectory, outputClass));
        debugLog(() -> dumpConfig(interfacesConfig, "interfaces"));
        debugLog(() -> dumpConfig(implementationsConfig, "implementations"));
    }

    private String dumpConfig(CompilerConfig config, String category) {
        return String.format(PARAM_DUMP_FORMAT, category, config.inputDirectory, config.includes, config.excludes);
    }

    /**
     * An import required in the generated source.
     */
    static class Import implements Comparable<Import> {
        private String name;
        private boolean isStatic;

        Import(String name, boolean isStatic) {
            this.name = name;
            this.isStatic = isStatic;
        }

        Import(Class<?> c) {
            this(c.getName(), false);
        }

        String name() {
            return name;
        }

        boolean isStatic() {
            return isStatic;
        }

        @Override
        public String toString() {
            return "Import{"
                    + "name='" + name + '\''
                    + ", isStatic=" + isStatic
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Import anImport = (Import) o;
            return isStatic == anImport.isStatic && name.equals(anImport.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, isStatic);
        }

        @Override
        public int compareTo(Import o) {
            if (this == o) {
                return 0;
            }
            if (o == null) {
                return 1;
            }
            int nameResult = name.compareTo(o.name);
            if (nameResult != 0) {
                return nameResult;
            }
            return isStatic ? (o.isStatic ? 0 : 1) : (o.isStatic ? -1 : 0);
        }
    }

    /**
     * Preloads the set of imports with items needed by the code in the class template so
     * we do not accidentally repeat imports which are triggered by the classes analyzed.
     *
     * @return Set of Import instances used by hard-coded methods in the template
     */
    private static Set<Import> preloadedImports() {
        Set<Import> result = new HashSet<>();
        result.add(new Import(java.util.List.class));
        result.add(new Import(java.util.HashMap.class));
        result.add(new Import(java.util.Map.class));
        result.add(new Import(java.util.Set.class));
        result.add(new Import(java.util.function.Function.class));
        result.add(new Import(java.util.function.BiFunction.class));
        result.add(new Import(java.util.AbstractMap.class));
        return result;
    }

    private void validateParameters() throws MojoExecutionException {
        validateInputDirectory(interfacesConfig.inputDirectory);
        validateInputDirectory(implementationsConfig.inputDirectory);
    }

    private void validateInputDirectory(String dir) throws MojoExecutionException {
        Path inputDirPath = resolveDirectory(dir);
        if (!Files.exists(inputDirPath)) {
            throw new MojoExecutionException("Cannot find specified inputDirectory " + inputDirPath.toString());
        }
    }

    private void analyzeInterfaces(Map<String, Type> types, Set<Import> imports) throws IOException {
        analyzeClasses(types, imports, null, inputs(interfacesConfig), "interfaces");
    }

    private void analyzeImplementations(Map<String, Type> types, Set<Import> imports, Map<String, List<String>> interfaces) throws IOException {
        analyzeClasses(types, imports, interfaces, inputs(implementationsConfig), "implementations");
    }

    private void associateImplementationsWithInterfaces(Map<String, Type> types, Map<String, List<String>> interfaces) {
        types.values()
                .forEach(t -> {
                    List<String> impls = interfaces.get(t.simpleName());
                    if (impls != null) {
                        if (impls.size() > 1) {
                            getLog().warn(String.format("Multiple implementations found for %s: %s",
                                    t.simpleName(), impls));
                        }
                        t.implementationType(impls.get(0));
                    }
                });
    }

    private void analyzeClasses(Map<String, Type> types, Set<Import> imports, Map<String, List<String>> interfaces,
            Collection<Path> pathsToCommpile, String note) {
        /*
         * There should not be compilation errors, but without our own diagnostic listener any compilation errors will
         * appear in the build output. We want to suppress those because we want to gather information about the interfaces
         * and classes, not actually compile them into .class files.
         */
        DiagnosticListener<JavaFileObject> diagListener = diagnostic -> {
            debugLog(() -> diagnostic.toString());
        };

        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = jc.getStandardFileManager(null, null, null);

        JavaCompiler.CompilationTask task = jc.getTask(null, fm, diagListener, null, null,
                javaFilesToCompile(fm, pathsToCommpile));
        List<Processor> procs = new ArrayList<>();
        EndpointProcessor ep = new EndpointProcessor(new EndpointScanner(types, imports, interfaces));
        procs.add(ep);
        task.setProcessors(procs);
        task.call();

        getLog().info(String.format("Types prepared for %s: %d", note, types.size()));
        debugLog(() -> String.format("Types prepared for %s: %s", note, types));
        debugLog(() -> String.format("Imports after analyzing %s: %s", note,
                imports.stream().sorted().map(Import::toString).collect(Collectors.joining(","))));
        debugLog(() -> String.format("Interface impls after analyzing %s: %s", note, interfaces));
    }

    private void addImportsForTypes(Map<String, Type> types, Set<Import> imports) {
        types.forEach((name, type) -> imports.add(new Import(type.fullName(), false)));
    }

    private void generateHelperClass(Map<String, Type> types, Set<Import> imports) throws IOException {
        List<String> pathElementsToGeneratedClass = new ArrayList<>();
        String outputPackage = outputClass.substring(0, outputClass.lastIndexOf('.'));
        for (String segment : outputPackage.split("\\.")) {
            pathElementsToGeneratedClass.add(segment);
        }
        Path outputDir = Paths.get(outputDirectory.getAbsolutePath(), pathElementsToGeneratedClass.toArray(new String[0]));
        Files.createDirectories(outputDir);

        String simpleClassName = outputClass.substring(outputClass.lastIndexOf('.') + 1);
        Path outputPath = outputDir.resolve(simpleClassName + ".java");

        Writer writer = new OutputStreamWriter(new FileOutputStream(outputPath.toFile()));
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache m = mf.compile("typeClassTemplate.mustache");
        CodeGenModel model = new CodeGenModel(outputPackage, simpleClassName, types.values(), imports, interfacePrefix,
                implementationPrefix);
        m.execute(writer, model);
        writer.close();
    }

    private void addGeneratedCodeToCompilation() {
        mavenProject.addCompileSourceRoot(outputDirectory.getPath());
    }

    private Path resolveDirectory(String directoryWithinProject) {
        Path baseDirPath = mavenProject.getBasedir().toPath();
        return (directoryWithinProject != null) ? baseDirPath.resolve(directoryWithinProject) : baseDirPath;
    }

    private Iterable<? extends JavaFileObject> javaFilesToCompile(StandardJavaFileManager fm, Collection<Path> paths) {
        List<? extends File> files =
                paths.stream()
                        .map(Path::toFile)
                        .collect(Collectors.toList());
        debugLog(() -> "Files to be compiled: " + files.toString());
        return fm.getJavaFileObjectsFromFiles(files);
    }

    /**
     * Computes paths to be processed as inputs, based on an input directory
     * and glob-style include and exclude expressions identifying paths within
     * that input directory.
     * @param compilerConfig conveys the directory, includes, and excludes to compile
     * @throws IOException in case of errors matching candidate paths
     */
    Collection<Path> inputs(CompilerConfig compilerConfig) throws IOException {
        Path inputPath = resolveDirectory(compilerConfig.inputDirectory);
        Collection<Path> result = Files.find(inputPath, Integer.MAX_VALUE, (path, attrs) ->
                matches(path, pathMatchers(inputPath, compilerConfig.includes))
                        && !matches(path, pathMatchers(inputPath, compilerConfig.excludes)))
                .collect(Collectors.toSet());
        if (result.isEmpty()) {
            throw new IllegalArgumentException("No input files selected from " + compilerConfig.inputDirectory);
        }
        return result;
    }

    /**
     * Creates a stream of PathMatchers, one for each glob.
     * @param inputDirectory Path within which the globs are applied
     * @param globs the glob patterns
     * @return PathMatchers for the globs
     */
    private static Stream<PathMatcher> pathMatchers(Path inputDirectory, List<String> globs) {
        return globs.stream()
                .map(glob -> FileSystems.getDefault().getPathMatcher("glob:" + sanitizeWindowsPath(inputDirectory) + "/" + glob));
    }

    /**
     * PathMather does not produce correct matching String if directory is not in proper format.
     *
     * @param inputDirectory input directory
     * @return properly escaped formatted path
     */
    private static String sanitizeWindowsPath(Path inputDirectory) {
        if (OS_NAME.contains("win")) {
            return inputDirectory.toString().replaceAll("\\\\", "/");
        }
        return inputDirectory.toString();
    }

    private static boolean matches(Path candidate, Stream<PathMatcher> matchers) {
        return matchers.anyMatch((m) -> (m.matches(candidate)));
    }

    /**
     * Collects information about types analyzed by the compiler, gathering class/interface,
     * enum, and method information.
     */
    static class EndpointScanner extends TreePathScanner<Type, Type> {

        private final Map<String, Type> types;
        private final Set<Import> imports;
        private final Map<String, List<String>> interfacesToImpls;

        EndpointScanner(Map<String, Type> types, Set<Import> imports, Map<String, List<String>> interfacesToImpls) {
            this.types = types;
            this.imports = imports;
            this.interfacesToImpls = interfacesToImpls;
        }
        @Override
        public Type visitClass(ClassTree node, Type type) {
            String typeName = fullyQualifiedPath(getCurrentPath()); // null for anon. inner class
            Tree.Kind kind = node.getKind();
            if ((kind == Tree.Kind.CLASS || kind == Tree.Kind.INTERFACE) && typeName != null) {
                List<String> interfaces = SnakeYAMLMojo.treesToStrings(node.getImplementsClause());
                final Type newType = new Type(typeName, node.getSimpleName().toString(), kind == Tree.Kind.INTERFACE,
                        interfaces);
                if (interfacesToImpls != null && kind == Tree.Kind.CLASS) {
                    // Add this class to our map from each interface to all of its implementations.
                    interfaces.stream()
                            .map(intf -> interfacesToImpls.computeIfAbsent(intf, key -> new ArrayList<>()))
                            .forEach(list -> list.add(newType.simpleName()));
                }
                types.put(typeName, newType);
                imports.add(new Import(newType.fullName(), false));
                type = newType;
            }

            return super.visitClass(node, type);
        }

        @Override
        public Type visitImport(ImportTree node, Type type) {
            imports.add(new Import(node.getQualifiedIdentifier().toString(), node.isStatic()));
            return super.visitImport(node, type);
        }

        @Override
        public Type visitMethod(MethodTree node, Type type) {
            CharSequence methodName = node.getName();
            CharSequence namePrefix = methodName.subSequence(0, Math.min(3, methodName.length()));
            if (namePrefix.equals("set") && methodName.length() > 3 && node.getParameters().size() == 1) {
                VariableTree propertyTree = node.getParameters().get(0);
                addPropertyParametersIfNeeded(propertyTree, type, propertyName(methodName));
            }
            return super.visitMethod(node, type);
        }

        static String propertyName(CharSequence setterMethodName) {
            /*
             * Normally, we force the property name to start with a lower-case character.
             * But, if the SECOND character of the property name is upper-case then we should not force the first character to
             * lower-case. For example, setIPAddress should NOT yield the property name iPAddress but rather it should be
             * IPAddress.
             */
            boolean forceFirstCharOfPropertyNameToLowerCase = setterMethodName.length() <= 4 // just one extra character so
                                                                                             // we cannot look past it
                    || !Character.isUpperCase(setterMethodName.charAt(4));
            return (
                    forceFirstCharOfPropertyNameToLowerCase
                            ? Character.toLowerCase(setterMethodName.charAt(3))
                            : setterMethodName.charAt(3))
                    + setterMethodName.subSequence(4, setterMethodName.length()).toString();
        }

        private static void addPropertyParametersIfNeeded(VariableTree node, Type type, String propName) {
            if (node.getType().getKind() == Tree.Kind.PARAMETERIZED_TYPE) {
                ParameterizedTypeTree pTree = (ParameterizedTypeTree) node.getType();

                List<String> parameterTypeNames = pTree.getTypeArguments().stream()
                        .filter(argTree -> argTree.getKind() == Tree.Kind.IDENTIFIER)
                        .map(argTree -> ((IdentifierTree) argTree).getName().toString())
                        .collect(Collectors.toList());
                type.propertyParameter(propName, parameterTypeNames);
            }
        }
    }

    @SupportedSourceVersion(SourceVersion.RELEASE_8)
    @SupportedAnnotationTypes("*")
    private static class EndpointProcessor extends AbstractProcessor {

        private final EndpointScanner scanner;
        private Trees trees;

        EndpointProcessor(EndpointScanner scanner) {
            this.scanner = scanner;
        }

        @Override
        public synchronized void init(ProcessingEnvironment processingEnv) {
            super.init(processingEnv);
            trees = Trees.instance(processingEnv);
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (!roundEnv.processingOver()) {
                for (Element e : roundEnv.getRootElements()) {
                    scanner.scan(trees.getPath(e).getParentPath(), null);
                }
            }
            return true;
        }
    }

    private static List<String> treesToStrings(List<? extends Tree> trees) {
        return trees.stream()
                .map(Tree::toString)
                .collect(Collectors.toList());
    }

    private static String fullyQualifiedPath(TreePath path) {
        ExpressionTree packageNameExpr = path.getCompilationUnit().getPackageName();
        MemberSelectTree packageID = packageNameExpr.getKind() == Tree.Kind.MEMBER_SELECT
                ? ((MemberSelectTree) packageNameExpr) : null;

        StringBuilder result = new StringBuilder();
        if (packageID != null) {
            result.append(packageID.getExpression().toString())
                    .append(".")
                    .append(packageID.getIdentifier().toString());
        }
        Tree.Kind kind = path.getLeaf().getKind();
        String leafName = null;
        if (kind == Tree.Kind.CLASS || kind == Tree.Kind.INTERFACE) {
            leafName = ((ClassTree) path.getLeaf()).getSimpleName().toString();
        } else if (kind == Tree.Kind.ENUM) {
            if (path.getParentPath() != null) {
                Tree parent = path.getParentPath().getLeaf();
                if (parent.getKind() == Tree.Kind.CLASS || parent.getKind() == Tree.Kind.INTERFACE) {
                    result.append(((ClassTree) parent).getSimpleName().toString()).append(".");
                }
                leafName = ((ClassTree) path.getLeaf()).getSimpleName().toString();
            }
        }

        // leafName can be empty for anonymous inner classes, for example.
        boolean isUsefulLeaf = leafName != null && !leafName.isEmpty();
        if (isUsefulLeaf) {
            result.append(".").append(leafName);
        }
        return isUsefulLeaf ? result.toString() : null;
    }
}
