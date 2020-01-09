# Helidon Maven Plugin

This plugin provides common utilities for Maven based Helidon applications.

* [Goal: native-image](#goal-native-image)
* [Goal: java-image](#goal-java-image)

## Goal: `native-image`

Maven goal to invoke GraalVM `native-image` command.

This plugin binds to the `package` phase by default.

### Optional Parameters

| Property | Type | Default<br/>Value | Description |
| --- | --- | --- | --- |
| graalVMHome | File | `${env.GRAALVM_HOME}` | GraalVM home |
| reportExceptionStackTraces | Boolean | `true` | Show exception stack traces for exceptions during image building |
| buildShared | Boolean | `false` | Build shared library |
| buildStatic | Boolean | `false` | Build statically linked executable (requires static `libc` and `zlib` |
| noServer | Boolean | `true` | Do not use image-build server |
| addProjectResources | Boolean | `true` | Indicates if project build resources should be added to the image |
| includeResources | List | [] | List of regexp matching names of resources to be included in the image |
| additionalArgs | List | [] | Additional command line arguments |
| skipNativeImage | Boolean | `false` | Skip this goal execution |

The parameters `reportExceptionStackTraces`, `noServer`, `buildShared`,
 `buildStatic` and `skipNativeImage` are mapped to user properties of the form:
 `native.image.PROPERTY`. The parameter `siteArchiveSkip` is mapped to:
 `native.image.skip`.

### Specifying the path to `native-image`

There are 3 ways to specify the path to the `native-image` command:
* export `GRAALVM_HOME` in your environment
* set the `graalVMHome` Maven property (either in the pom or with -D on the
 command line)
* add the GraalVM `bin` folder to your PATH environment variable

If the plugin fails to determine the path to `native-image`, the build will
 fail with an error.

### Adding build resources

When `addProjectResources` is `true` (the default), the plugin will automatically
 add the processed project resources to the image. I.e the files from
 `src/main/resources` processed under the `target/classes` directory.

You can manually include additional files with the `includeResources` parameter.

### General usage

 A good practice would be to define an execution for this goal under a profile
 named `native-image`.

```xml
    <profiles>
        <profile>
            <id>native-image</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>io.helidon.build-tools</groupId>
                        <artifactId>helidon-maven-plugin</artifactId>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>native-image</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
```

You then build your native image with the following command:

```bash
mvn package -Pnative-image
```

You can also execute this plugin outside of a configured life-cycle, however
 it requires the project jar to be present:

```bash
mvn package
mvn helidon:native-image
```

## Goal: `java-image`

Maven goal to create a custom Java Runtime Image containing the application jars and the JDK modules 
on which they depend. Enables Class Data Sharing by default to reduce startup time. Generates a 
custom `start` script.

This plugin binds to the `package` phase by default.

### Optional Parameters

| Property | Type | Default<br/>Value | Description |
| --- | --- | --- | --- |
| defaultJvmOptions | List | [] | JVM options to use if none are passed to the `start` script |
| defaultArgs | List | [] | Application arguments to use if none are passed to the `start` script |
| defaultDebugOptions | List | [] | JVM debug options to use if the `--debug` flag is passed to the `start` script |
| addClassDataSharingArchive | Boolean | `true` | Add a Class Data Sharing archive to reduce startup time |
| testImage | Boolean | `true` | Start the application after the image is built |
| stripDebug | Boolean | `false` | Remove all debug support from the image, including within `.class` files |
| skipJavaImage | Boolean | `false` | Skip this goal execution |



### General usage

A good practice would be to define an execution for this goal under a profile named `java-image`.

```xml
    <profiles>
        <profile>
            <id>java-image</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>io.helidon.build-tools</groupId>
                        <artifactId>helidon-maven-plugin</artifactId>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>java-image</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
```

You then build your image with the following command:

```bash
mvn package -Pjava-image
```

You can also execute this plugin outside of a configured life-cycle, however
 it requires the project jar to be present and will use only _default_ configuration:

```bash
mvn package
mvn helidon:java-image
```
