# Helidon Maven Plugin

This plugin provides Maven goals specific to generate Java API documentation with `javadoc`.

#### Goals

* [javadoc](#goal-javadoc)

## Goal: `javadoc`

This goal binds to the `package` phase by default.

### Optional Parameters

| Property            | Type    | Default<br/>Value                            | Description                                                                                             |
|---------------------|---------|----------------------------------------------|---------------------------------------------------------------------------------------------------------|
| buildDirectory      | File    | `${project.build.directory}`                 | The project build output directory                                                                      |
| outputDirectory     | File    | `${project.build.directory}/apidocs`         | The destination directory where javadoc saves the generated files                                       |
| projectRoot         | File    | `${maven.multiModuleProjectDirectory}`       | The project root directory                                                                              |
| dependencyIncludes  | List    | `*:*`                                        | Project dependencies include patterns (`groupId:artifactId`)                                            |
| dependencyExcludes  | List    | `[]`                                         | Project dependencies exclude pattern (`groupId:artifactId`)                                             |
| pomScanningIdentity | List    | `pom.xml`                                    | List of relative paths that must exist for a directory to be resolved as a Maven module                 |
| pomScanningIncludes | List    | `**/*`                                       | List of glob expressions used as an include filter for directories that may contain pom.xml files       |
| pomScanningExcludes | List    | `**/target/**`                               | List of glob expressions used as an exclude filter for directories that may contain pom.xml files       |
| pomIncludes         | List    | `*:*:*`                                      | List of include filters (`groupId:artifactId:packaging` with wildcard support) of scanned pom.xml files |
| pomExcludes         | List    | `[]`                                         | List of exclude filters (`groupId:artifactId:packaging` with wildcard support) of scanned pom.xml files |
| sourcesJarFallback  | boolean | `false`                                      | Whether to fall back to {@code sources-jar} when unable to resolve dependency sources from workspace    |
| parseModuleInfo     | boolean | `true`                                       | Whether to resolve the module descriptor for sources by parsing {@code module-info.java}                |
| sourcesJarIncludes  | List    | `[]`                                         | Include patterns for unpacking sources-jar                                                              |
| sourcesJarExcludes  | List    | `[]`                                         | Exclude patterns for unpacking sources-jar                                                              |
| sourceIncludes      | List    | `**/*`                                       | Source directory include patterns. List of glob expressions used as an include filter                   |
| sourceExcludes      | List    | `**/src/test/java,**/generated-test-sources` | Source directory exclude patterns. List of glob expressions used as an exclude filter                   |
| moduleIncludes      | List    | `*`                                          | Java module include patterns.  List of Java module names to include, wildcards are supported            |
| moduleExcludes      | List    | `[]`                                         | Java module exclude patterns.  List of Java module names to exclude, wildcards are supported            |
| packageIncludes     | List    | `*`                                          | Java package include patterns.  List of Java package names to include, wildcards are supported          |
| packageExcludes     | List    | `[]`                                         | Java package exclude patterns.  List of Java package names to exclude, wildcards are supported          |
| fileIncludes        | List    | `*.java`                                     | File name include patterns. List of file names/patterns to include, wildcards are supported             |
| fileExcludes        | List    | `[]`                                         | File name exclude patterns. List of file names/patterns to exclude, wildcards are supported             |
| additionalOptions   | List    | `[]`                                         | Set additional options. You must take care of quoting and escaping                                      |
| additionalOptions   | List    | `[]`                                         | Set additional options. You must take care of quoting and escaping                                      |
| source              | String  | `${maven.compiler.source}`                   | See `javadoc --source`                                                                                  |
| release             | String  | `${maven.compiler.release}`                  | See `javadoc --release`                                                                                 |
| charset             | String  |                                              | See `javadoc -charset`, Defaults the value of `docencoding`                                             | 
| docencoding         | String  | `UTF-8`                                      | See `javadoc -docencoding`                                                                              |
| encoding            | String  | `${project.build.sourceEncoding}`            | See `javadoc -encoding`                                                                                 |
| bottom              | String  | See [defaults](#defaults)                    | See `javadoc -bottom`                                                                                   |
| doctitle            | String  | `${project.name} ${project.version} API`     | See `javadoc -doctitle`                                                                                 |
| windowtitle         | String  | `${project.name} ${project.version} API`     | See `javadoc -windowtitle`                                                                              |
| links               | List    | `[]`                                         | See `javadoc --link`                                                                                    |
| offlineLinks        | List    | `[]`                                         | See `javadoc --linkoffline`                                                                             |
| author              | boolean | `true`                                       | See `javadoc -author`                                                                                   |
| use                 | boolean | `true`                                       | See `javadoc -use`                                                                                      |
| version             | boolean | `true`                                       | See `javadoc -version`                                                                                  |
| doclint             | String  |                                              | See `javadoc -Xdoclint`                                                                                 |
| quiet               | boolean | `true`                                       | See `javadoc -quiet`                                                                                    |
| failOnError         | boolean | `true`                                       | Specifies if the build will fail if there are errors during javadoc execution or not                    |
| failOnWarnings      | boolean | `false`                                      | Specifies if the build will fail if there are warnings during javadoc execution or not                  |
| skip                | boolean | `false`                                      | Skip this goal execution                                                                                |

Except for `release`, the above parameters are mapped to user properties of the form `helidon.javadoc.PROPERTY`.
`List` values must be comma separated.

#### Defaults

Bottom:
```
Copyright &#169; {inceptionYear}&#x2013;{currentYear} {organizationName}. All rights reserved.
```

### General usage

A good practice would be to define an execution for this goal under a profile
named `javadoc`.

```xml

<profiles>
    <profile>
        <id>javadoc</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>io.helidon.build-tools</groupId>
                    <artifactId>helidon-javadoc-maven-plugin</artifactId>
                    <executions>
                        <execution>
                            <goals>
                                <goal>javadoc</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```
