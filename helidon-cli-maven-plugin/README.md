# Helidon CLI Maven Plugin

This plugin provides Maven goals supporting the Helidon CLI.

## Goal: `dev`

Implements the development loop for the `helidon dev` command, executing the following algorithm:

1. Perform a full build.
2. Start the application.
3. On file change: 

   - stop the application
   - if the `pom.xml` file changed, go to step 1; else
   - perform an incremental build and go to step 2

If an error occurs during a build, the loop waits for a file change and tries again.

Any `pom.xml` change triggers a full build, meaning a [phase](https://maven.apache.org/ref/3.6.3/maven-core/lifecycles.html) 
such as `process-classes` is executed. An incremental build executes only the specific goal(s) bound to the watched 
directory, e.g. `resources:resources` for files in the `src/main/resources` directory or `compiler:compile` for files in 
the `src/main/java` directory. 

The actual source/resource directories and their includes/excludes are configured with the `maven-compiler-plugin` and 
`maven-resources-plugin` and are discovered by this plugin; however, the goal(s) to execute on a file change are 
configurable here. Further, custom directories and goals can be added to incremental builds, and the full build phase can 
be changed...  

### Configuration

This example describes all configuration elements:
```xml
<plugin>
    <groupId>io.helidon.build-tools</groupId>
    <artifactId>helidon-cli-maven-plugin</artifactId>
    <configuration>

        <devLoop>

            <!-- This section defines behavior for full builds. -->
           
            <fullBuild>
                <!-- The phase to execute on pom file change. -->
                <!-- This is the default phase, here for illustration only. -->
                <phase>process-classes</phase>

                <!-- The maximum number of full build failures to allow before exiting the loop. -->
                <!-- Defaults to Integer.MAX_VALUE -->
                <maxBuildFailures>1024</maxBuildFailures>
            </fullBuild>

            <!-- This section defines behavior for incremental builds. -->
           
            <incrementalBuild>

                <!-- Specify the goal(s) to execute when any Java source file changes. -->
                <!-- Directories, includes and excludes are specified in maven-compiler-plugin config. -->
                <javaSourceGoals>
                    <!-- This is the default goal, here for illustration only. -->
                    <goal>compiler:compile</goal>
                </javaSourceGoals>

                <!-- Specify the goal(s) to execute when any resource file changes. -->
                <!-- Directories, includes and excludes are specified in maven-resources-plugin config. -->
                <resourceGoals>
                    <!-- This is the default goal, here for illustration only. -->
                    <goal>resources:resources</goal>
                </resourceGoals>

                <!-- Specify custom directories to watch and the goal(s) to execute on change. -->
                <customDirectories>

                    <!-- Specify a custom directory to watch -->
                    <directory>
                        <!-- Specify the directory path -->
                        <path>etc</path>
                        <!-- Specify files to watch in the directory; empty for all files. -->
                        <includes>**/*.hello</includes>
                        <!-- Specify files in the directory not to watch. -->
                        <excludes />
                        <!-- Specify the goal(s) to execute when a watched file changes. -->
                        <goals>
                            <goal>exec:exec@say-hello</goal>
                        </goals>
                    </directory>
               </customDirectories>

                <!-- The maximum number of incremental build failures to allow before exiting the loop. -->
                <!-- Defaults to Integer.MAX_VALUE -->
                <maxBuildFailures>1024</maxBuildFailures>
            </incrementalBuild>
        </devLoop>
    </configuration>
</plugin>
```
If multiple goals are specified, they are executed in declaration order.

Any goal configured in the application pom can be executed. This example assumes that the pom contains a `say-hello` 
execution using the `exec` goal of the `exec-maven-plugin`, e.g.:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>say-hello</id>
            <goals>
                <goal>exec</goal>
            </goals>
            <configuration>
                <executable>bash</executable>
                <arguments>
                    <argument>-c</argument>
                    <argument>echo; echo Hello; echo</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```
This declaration does not specify a phase so is not tied to any lifecycle; it is only executed during the dev loop
whenever any file with the `.hello` suffix in the `etc` directory changes.

Includes and excludes use [Ant path pattern](http://ant.apache.org/manual/dirtasks.html#patterns) syntax.

> **_NOTE:_** Changes to the `devLoop` configuration require a restart to take effect (it is injected by Maven
> at startup).


#### Goal References

Individual goals can be selected using fully qualified references:
```
   groupId:artifactId:version:goal@executionId
```
A [default executionId](http://maven.apache.org/guides/mini/guide-default-execution-ids.html) will be used if not provided, 
e.g. the default executionId for the `compile` goal is `default-compile`.

Note that while the version _can_ be declared, it will be ignored since it can only resolve to the plugin configured in the 
current project. 

A [plugin prefix](https://maven.apache.org/guides/introduction/introduction-to-plugin-prefix-mapping.html) can be used as an
alias for the `groupId` and `artifactId`, for example `compiler` in the following reference:
```
    compiler:compile
```

As a special case, a phase may be used in place of a goal, and will be expanded into the corresponding list of goals.   

##### Example Goals

1. `org.apache.maven.plugins:maven-exec-plugin:3.0.0:exec@compile-sass`
2. `org.apache.maven.plugins:maven-exec-plugin:exec@compile-sass`
3. `exec:exec@compile-sass`
4. `compiler:compile`
5. `compile`

References #1-3 are equivalent, #4 only executes the `compile` goal and #5 executes all goals in the `compile` phase.
