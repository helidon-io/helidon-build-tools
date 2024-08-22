# Helidon Build Cache Maven Extension

This extension provides features designed to help parallelize Maven builds CI pipelines.

* [Lifecycle Fast Forwarding](#lifecycle-fast-forwarding)
* [Reactor Splitting](#reactor-splitting)
* [Extension Configuration](#extension-configuration)
* [General usage](#general-usage)

## Lifecycle Fast Forwarding

The main focus is fast-forwarding of subsequent Maven invocations without source code changes. Invocations don't have
 to be identical and can differ in life-cycle and configurations. It can be used to "split" the life-cycle into
 distinct invocations without duplicating executions.

E.g. Invoke `mvn compile` and then `mvn package` without running the compilation twice.

### Principles

This extension provides a Maven extension that manages the state of projects in the reactor and uses recorded information
 to detect plugin executions that can be skipped and removed from the life-cycle.

The state of a project consists of the few things that Maven plugins can update in the build context:
- properties
- "main" source files
- "test" sources files
- project artifact
- attached artifacts
- recorded executions with their effective configuration

The state is saved to `target/state.xml`. If the file exists at the start of a build, the state is loaded and used
 to restore the "build context" and to detect execution duplicates. A combined "cache archive file" may also be created
 to save and load all the files needed to fast-forward builds.

The state is invalidated when the project files are changed. Changes are detected by looking at the most recent
 `last-modified` file attribute, or by using MD5 checksums. When the state is not available, the life-cycle is not
 modified.

### Limitations

Maven does not maintain inputs and outputs for plugin executions, thus it is not possible to support fine-grained
 incremental life-cycle changes. I.e. when a project has file changes, the cache features are not available.

Plugin executions can be excluded using `<executionExcludes>`. This means they won't be recorded in the state and
won't be fast-forwarded.

A given plugin executions should be excluded when:
- it always needs to run
- it uses files not located within the project directory
- it generates files in other locations than the build directory

Plugin executions may require prior executions in the life-cycle, however the extension does not invalidate cached
 executions based on the life-cycle order. I.e. if an execution has differences and is planned for execution, all
 subsequent executions are still eligible for fast-forwarding.

The states of modules with inter-project dependencies rely on the dependency graph for indirect state invalidation.
 A module that requires another must declare a dependency, otherwise it will fast-forward even if the required module
 has changed.

Values recorded in the state may not be portable, E.g. file paths. Values that contain the execution root directory
 are recorded in a portable way. However, any other system specific value that is recorded will not be portable and
 cause the cache to be un-usable on a different system.

## Reactor Splitting

The main focus is to parallelize CI builds.
This extension accepts configuration to define named rules that split the reactor into named module sets.

At runtime given a rule name and a module set name, the extension will filter the reactor.

### Extension Configuration

The configuration resides in `.mvn/cache-config.xml`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<cacheConfig>
    <!-- 
        Disable the extension.
        Can be overridden with -Dcache.enabled=true
     -->
    <enabled>false</enabled>
    <!--
        Enable state recording (target/state.xml).
        Can be overridden with -Dcache.record=false
    -->
    <record>true</record>
    <!--
        Load additional state files (target/state-{suffix}.xml).
        Can be overridden with -Dcache.loadSuffixes=foo,bar
    -->
    <loadSuffixes>
        <suffix>foo</suffix>
    </loadSuffixes>
    <!--
        Add a suffix to the state file name used for recording (target/state-{suffix}.xml).
        Can be overridden with -Dcache.recordSuffix=bar
    -->
    <recordSuffix>foo</recordSuffix>
    <!-- Per project configuration -->
    <lifecycleConfig>
        <!-- Indicate if the project files checksum should be computed -->
        <enableChecksums>true</enableChecksums>
        <!-- Indicate if the all the individual project file checksums should be computed -->
        <includeAllChecksums>true</includeAllChecksums>

        <!-- Match projects with a glob -->
        <project glob="**/*">
            <projectFilesExcludes>
                <!-- Glob expressions relative to each project base directory -->
                <exclude>.*/**</exclude>
                <exclude>etc/**</exclude>
            </projectFilesExcludes>
            <executionsIncludes>
                <!-- Wildcard match of executions (groupId:artifactId:version:goal@executionId) -->
                <include>*</include>
            </executionsIncludes>
            <executionsExcludes>
                <!-- Wildcard match of executions (groupId:artifactId:version:goal@executionId) -->
                <exclude>com.acme*</exclude>
            </executionsExcludes>
        </project>

        <!-- Exact match -->
        <project path="foo/bar/pom.xml">
            <!-- ... -->
        </project>

        <!-- Match with a regex -->
        <project regex=".*">
            <!-- ... -->
        </project>
    </lifecycleConfig>

    <!-- 
        All the reactor rules.
        The current rule is specified with -DreactorRule=XXX
    -->
    <reactorRules>
        <!-- An example rule used to parallelize the tests -->
        <reactorRule name="tests">
            <profiles>
                <!-- Activate the tests profile automatically -->
                <profile>tests</profile>
            </profiles>
            <!--
                The module sets in this rule.
                The active one is specified with -DmoduleSet=XXX
             -->
            <moduleSets>
                <moduleSet name="core">
                    <includes>
                        <!-- Glob expressions of project paths relative to the multimodule root directory -->
                        <include>webserver/**</include>
                    </includes>
                </moduleSet>
                <!-- Subsequent modulesets only match against the remaining modules -->
                <moduleSet name="others">
                    <includes>
                        <!-- I.e. everything else -->
                        <include>**/*</include>
                    </includes>
                    <excludes>
                        <!-- Except benchmarks -->
                        <exclude>tests/benchmark/**</exclude>
                    </excludes>
                </moduleSet>
            </moduleSets>
        </reactorRule>
    </reactorRules>
</cacheConfig>
```

### Properties

| Property           | Type    | Default<br/>Value | Description                                    |
|--------------------|---------|-------------------|------------------------------------------------|
| cache.enabled      | Boolean | `false`           | Enables the extension                          |
| cache.record       | Boolean | `false`           | Update the recorded cache state                |
| cache.loadSuffixes | List    | `[]`              | List of additional state file suffixes to load |
| cache.recordSuffix | String  | `null`            | State file suffix to use                       |
| reactorRule        | String  | `null`            | The reactor rule to use                        |
| moduleSet          | String  | `null`            | The moduleset in the reactor rule to use       |

## General usage

Define the extension under `.mvn/extensions.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<extensions xmlns="https://maven.apache.org/EXTENSIONS/1.1.0"
        xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="https://maven.apache.org/EXTENSIONS/1.1.0 https://maven.apache.org/xsd/core-extensions-1.1.0.xsd">
    <extension>
        <groupId>io.helidon.build-tools</groupId>
        <artifactId>helidon-build-cache-maven-extension</artifactId>
        <version>X.Y.Z</version>
    </extension>
</extensions>
```

Define the extension in your pom.xml:

```xml
<project>
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>io.helidon.build-tools</groupId>
                    <artifactId>helidon-build-cache-maven-extension</artifactId>
                    <version>X.Y.Z</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

Do a prime build:

```shell
mvn -Dcache.enabled=true -DskipTests package
```

Run the tests with fast-forwarding for a subset of the reactor:

```shell
mvn -Dcache.enabled=true -DreactorRule=tests -DmoduleSet=core test
```
