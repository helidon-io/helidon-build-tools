/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.codegen.openapi;

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
import io.smallrye.openapi.api.models.OpenAPIImpl;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Goal which generates code to help with parsing YAML and JSON using SnakeYAML.
 * <p>
 *     SnakeYAML does a lot of work converting between JSON or YAML and bean classes, but it needs
 *     some help with information it cannot get from compiled class bytecode. This plug-in
 *     generates that helper code.
 * </p>
 * <p>
 *     It analyzes Java sources in a specified location and generates a SnakeYAML
 *     {@code TypeDefinition} for each class.
 * </p>
 * <p>
 *     Here is an example from the maven dependency plug-in showing how to extract the sources so
 *     they are available for this plug-in to consume:
 * </p>
 * <pre>{@code
 * <project>
 *   [...]
 *   <build>
 *     <plugins>
 *       <plugin>
 *         <groupId>org.apache.maven.plugins</groupId>
 *         <artifactId>maven-dependency-plugin</artifactId>
 *         <version>3.1.1</version>
 *         <executions>
 *           <execution>
 *             <id>src-dependencies</id>
 *             <phase>package</phase>
 *             <goals>
 *               <!-- use copy-dependencies instead if you don't want to explode the sources -->
 *               <goal>unpack-dependencies</goal>
 *             </goals>
 *             <configuration>
 *               <artifactItems>
 *                 <artifactItem>
 *                   <groupId>junit</groupId>
 *                   <artifactId>junit</artifactId>
 *                   <version>3.8.1</version>
 *                   <type>jar</type>
 *                   <overWrite>false</overWrite>
 *                   <outputDirectory>${project.build.directory}/alternateLocation</outputDirectory>
 *                   <destFileName>optional-new-name.jar</destFileName>
 *                   <includes>**&quot;/*.class,**&quot;/*.xml</includes>
 *                   <excludes>**&quot;/*test.class</excludes>
 *                 </artifactItem>
 *               </artifactItems>
 *               <includes>**&quot;/*.java</includes>
 *               <excludes>**&quot;/*.properties</excludes>
 *               <classifier>sources</classifier>
 *               <failOnMissingClassifierArtifact>false</failOnMissingClassifierArtifact>
 *               <outputDirectory>${project.build.directory}/sources</outputDirectory>
 *             </configuration>
 *           </execution>
 *         </executions>
 *       </plugin>
 *     </plugins>
 *   </build>
 *   [...]
 * </project>
 * }
 *
 * </pre>
 */
@Mojo( name = "generate",
        requiresProject = true,
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.RUNTIME)
public class SnakeYAMLMojo extends AbstractMojo {

    private static final String TYPE_PREFIX = OpenAPI.class.getPackage().getName() + ".";
    private static final String TYPE_PREFIX_REPLACEMENT = OpenAPIImpl.class.getPackage().getName() + ".";

    private static final String PROPERTY_PREFIX = "openapigen.";

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
    @Parameter(property = PROPERTY_PREFIX + "generatedClass", required = true)
    private String generatedClass;

    /**
     * Directory within the current Maven project containing Java sources to analyze.
     */
    @Parameter(property = PROPERTY_PREFIX + "inputDirectory")
    String inputDirectory;

    /**
     * Selector for which Java sources to analyze.
     */
    @Parameter(property= PROPERTY_PREFIX + "includes", defaultValue = "**/*.java")
    List<String> includes;

    /**
     * Deselector to identify Java sources to ignore during the analysis.
     */
    @Parameter(property = PROPERTY_PREFIX + "excludes")
    List<String> excludes;

    public void execute()
        throws MojoExecutionException
    {
        validateParameters();

        File f = outputDirectory;
        if (!f.exists()) {
            if (f.mkdirs()) {
                getLog().debug("Created output directory " + f.getAbsolutePath());
            } else {
                getLog().debug("Using existing output directory " + f.getAbsolutePath());
            }
        }

        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = jc.getStandardFileManager(null, null, null);
        try {
            DiagnosticListener<JavaFileObject> diagListener = new DiagnosticListener<JavaFileObject>() {
                @Override
                public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                    if (getLog().isDebugEnabled()) {
                        getLog().debug(diagnostic.toString());
                    }
                }
            };

            JavaCompiler.CompilationTask task = jc.getTask(null, fm, diagListener, null, null,
                    javaFilesToCompile(fm));
            List<Processor> procs = new ArrayList<>();
            EndpointProcessor ep = new EndpointProcessor(new EndpointScanner());
            procs.add(ep);
            task.setProcessors(procs);
            task.call();

            Map<String, Type> types = ep.types();
            Set<Import> imports = ep.imports();

            /*
             * The parsing builds most of the data model, but we need to add property substitutions explicitly
             * because there is no way to determine them automatically.
             */
            addPropertySubstitutions(types);
            addImportsForTypes(types, imports);
            addImplementationTypes(types, imports);

            getLog().info("Types prepared: " + types.size());
            getLog().debug("Types: " + types);
            getLog().debug("Imports: " + ep.imports().stream().sorted().map(Import::toString).collect(Collectors.joining(",")));

            generateParserClass(types, imports);

        } catch (Throwable e) {
            throw new MojoExecutionException("Error compiling and analyzing source files", e);
        }
    }

    static class Import implements Comparable<Import> {
        String name;
        boolean isStatic;

        Import(String name, boolean isStatic) {
            this.name = name;
            this.isStatic = isStatic;
        }

        Import(Class<?> c) {
            this(c.getName(), false);
        }

        @Override
        public String toString() {
            return "Import{" +
                    "name='" + name + '\'' +
                    ", isStatic=" + isStatic +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Import anImport = (Import) o;
            return isStatic == anImport.isStatic &&
                    name.equals(anImport.name);
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

    private void validateParameters() throws MojoExecutionException {

        Path inputDirPath = resolvedInputDirectory();
        if (!Files.exists(inputDirPath)) {
            throw new MojoExecutionException("Cannot find specified inputDirectory " + inputDirPath.toString());
        }
    }

    private void addPropertySubstitutions(Map<String, Type> types) {
        Type pathItemType = types.get(PathItem.class.getSimpleName());
        if (pathItemType != null) { // might be null for tests
            for (PathItem.HttpMethod m : PathItem.HttpMethod.values()) {
                pathItemType.propertySubstitution(m.name()
                        .toLowerCase(), Operation.class.getSimpleName(), getter(m), setter(m));
            }
        }

        Type schemaType = types.get(Schema.class.getSimpleName());
        if (schemaType != null) {
            schemaType.propertySubstitution("enum", List.class.getSimpleName(), "getEnumeration", "setEnumeration");
            schemaType.propertySubstitution("default", Object.class.getSimpleName(), "getDefaultValue", "setDefaultValue");
        }
    }

    private void addImportsForTypes(Map<String, Type> types, Set<Import> imports) {
        types.forEach((name, type) -> imports.add(new Import(type.fullName, false)));
    }

    private void addImplementationTypes(Map<String, Type> types, Set<Import> imports) {
        types.forEach((name, type) -> {
            String implName = type.simpleName + "Impl";
            type.implementationType(implName);
            if (type.fullName.startsWith(TYPE_PREFIX)) {
                imports.add(new Import(TYPE_PREFIX_REPLACEMENT + type.simpleName + "Impl", false));
            }
        });

    }

    private void generateParserClass(Map<String, Type> types, Set<Import> imports) throws IOException {
        Path outputPath = Paths.get(outputDirectory.getAbsolutePath(), "Parser.java");
        Writer writer = new OutputStreamWriter(new FileOutputStream(outputPath.toFile()));
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache m = mf.compile("typeClassTemplate.mustache");
        CodeGenModel model = new CodeGenModel(types.values(), imports);
        m.execute(writer, model);
        writer.close();
    }

    private static String getter(PathItem.HttpMethod method) {
        return methodName("get", method);
    }

    private static String setter(PathItem.HttpMethod method) {
        return methodName("set", method);
    }

    private static String methodName(String operation, PathItem.HttpMethod method) {
        return operation + method.name();
    }

//    private ClassLoader initClassLoader() {
//        try {
//            Set<URL> urls = new HashSet<>();
//            List<String> elements = mavenProject.getRuntimeClasspathElements();
//            for (String element : elements) {
//                urls.add(new File(element).toURI().toURL());
//            }
//
//            ClassLoader augmentedClassLoader = URLClassLoader.newInstance(
//                    urls.toArray(new URL[0]),
//                    Thread.currentThread().getContextClassLoader());
//            return augmentedClassLoader;
////            Thread.currentThread().setContextClassLoader(contextClassLoader);
//
//        } catch (DependencyResolutionRequiredException | MalformedURLException e) {
//            throw new RuntimeException(e);
//        }
//    }

    private Path resolvedInputDirectory() {
        Path baseDirPath = mavenProject.getBasedir().toPath();
        return (inputDirectory != null) ? baseDirPath.resolve(inputDirectory) : baseDirPath;
    }

    private Iterable<? extends JavaFileObject> javaFilesToCompile(StandardJavaFileManager fm) throws IOException {
        List<? extends File> files =
                inputs(resolvedInputDirectory(), includes, excludes).stream()
                .map(Path::toFile)
                .collect(Collectors.toList());
        getLog().debug("Files to be compiled: " + files.toString());
        return fm.getJavaFileObjectsFromFiles(files);
    }

    /**
     * Computes paths to be processed as inputs, based on an input directory
     * and glob-style include and exclude expressions identifying paths within
     * that input directory.
     * @param inputDirectory the directory within which to search for files
     * @param includes glob-style include expressions
     * @param excludes glob-style exclude expressions
     * @return Paths within the input directory tree that match the includes and
     * are not ruled out by the excludes
     * @throws IOException in case of errors matching candidate paths
     */
    static Collection<Path> inputs(Path inputDirectory, List<String> includes, List<String> excludes) throws IOException {
        Collection<Path> result = Files.find(inputDirectory, Integer.MAX_VALUE, (path, attrs) ->
                matches(path, pathMatchers(inputDirectory, includes))
                        && !matches(path, pathMatchers(inputDirectory, excludes)))
                .collect(Collectors.toSet());
        if (result.isEmpty()) {
            throw new IllegalArgumentException("No input files selected from " + inputDirectory);
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
                .map(glob -> {
                    return FileSystems.getDefault().getPathMatcher("glob:" + inputDirectory + "/" + glob);
                });
    }

    private static boolean matches(Path candidate, Stream<PathMatcher> matchers) {
        return matchers.anyMatch((m) -> (m.matches(candidate)));
    }

    /**
     * Collects information about types analyzed by the compiler, gathering class/interface,
     * enum, and method information.
     */
    private static class EndpointScanner extends TreePathScanner<Type, Type> {

        private final Map<String, Type> types = new TreeMap<>();
        private final Set<Import> imports = preloadedImports();

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
            return result;
        }

        @Override
        public Type visitClass(ClassTree node, Type type) {
            String typeName = fullyQualifiedPath(getCurrentPath());
            if (node.getKind() == Tree.Kind.CLASS || node.getKind() == Tree.Kind.INTERFACE) {
                final Type newType = new Type(typeName, node.getSimpleName().toString());
                type = newType;
                types.put(typeName, newType);
                updateRef(node, newType);
                /*
                 * Define enums now to make sure they are defined before they are referenced in
                 * methods handled by visitMethod.
                 */
                node.getMembers().stream()
                        .filter(member -> member.getKind() == Tree.Kind.ENUM)
                        .map(member -> ((ClassTree) member).getSimpleName().toString())
                        .forEach(type::typeEnumByType);
            }

            return super.visitClass(node, type);
        }

        @Override
        public Type visitImport(ImportTree node, Type type) {
            imports.add(new Import(node.getQualifiedIdentifier().toString(), node.isStatic()));
            return super.visitImport(node, type);
        }

        private void updateRef(ClassTree node, Type type) {
            type.ref(node.getImplementsClause().stream()
                    .filter(tree -> tree.getKind() == Tree.Kind.IDENTIFIER)
                    .anyMatch(tree -> ((IdentifierTree) tree).getName().toString().equals("Reference")));
        }

        @Override
        public Type visitMethod(MethodTree node, Type type) {
            CharSequence methodName = node.getName();
            CharSequence namePrefix = methodName.subSequence(0, Math.min(3, methodName.length()));
            if (namePrefix.equals("set") && methodName.length() > 3 && node.getParameters().size() == 1) {
                String propName = Character.toLowerCase(methodName.charAt(3)) + methodName.subSequence(4, methodName.length()).toString();
                VariableTree propertyTree = node.getParameters().get(0);
                updateEnumIfNeeded(propertyTree, type, propName);
                addPropertyParametersIfNeeded(propertyTree, type, propName);
            }
            return super.visitMethod(node, type);
        }

        Map<String, Type> types() {
            return types;
        }

        Set<Import> imports() {
            return imports;
        }

        private void updateEnumIfNeeded(VariableTree node, Type type, String propName) {
            Tree propertyType = node.getType();
            if (propertyType.getKind() == Tree.Kind.IDENTIFIER) {
                String referencedTypeName = ((IdentifierTree) propertyType).getName().toString();

                type.getTypeEnum(referencedTypeName).ifPresent(typeEnum -> typeEnum.name(propName));
            }
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

        Map<String, Type> types() {
            return scanner.types();
        }

        Set<Import> imports() {
            return scanner.imports();
        }
    }

    private static String fullyQualifiedPath(TreePath path) {
            ExpressionTree packageNameExpr = path.getCompilationUnit().getPackageName();
            MemberSelectTree packageID = packageNameExpr.getKind() == Tree.Kind.MEMBER_SELECT ?
                    ((MemberSelectTree) packageNameExpr) : null;

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
                    Tree parent= path.getParentPath().getLeaf();
                    if (parent.getKind() == Tree.Kind.CLASS || parent.getKind() == Tree.Kind.INTERFACE) {
                        result.append(((ClassTree) parent).getSimpleName().toString()).append(".");
                    }
                    leafName = ((ClassTree) path.getLeaf()).getSimpleName().toString();
                }
            }
            if (leafName != null) {
                result.append(".").append(leafName);
            }
            return leafName != null ? result.toString() : null;
        }
}
