# Helidon Build Cache Maven Plugin

This plugin provides build cache related Maven goals.

#### Goals

* [go-offline](#goal-go-offline)

## Goal: `go-offline`

A goal similar to `dependency:go-offline` but more aggressive and resilient.

This goal requires a direct execution.

### Optional Parameters

| Property                    | Type    | Default<br/>Value       | Description                                                                                             |
|-----------------------------|---------|-------------------------|---------------------------------------------------------------------------------------------------------|
| pomScanningIdentity         | List    | `pom.xml`               | List of relative paths that must exist for a directory to be resolved as a Maven module                 |
| pomScanningIncludes         | List    | `**/*`                  | List of glob expressions used as an include filter for directories that may contain pom.xml files       |
| pomScanningExcludes         | List    | `**/target/**,**/src**` | List of glob expressions used as an exclude filter for directories that may contain pom.xml files       |
| pomIncludes                 | List    | `*:*:*`                 | List of include filters (`groupId:artifactId:packaging` with wildcard support) of scanned pom.xml files |
| pomExcludes                 | List    | `[]`                    | List of exclude filters (`groupId:artifactId:packaging` with wildcard support) of scanned pom.xml files |
| includeSnapshots            | boolean | `false`                 | Specifies if `-SNAPSHOT` artifacts should be processed                                                  |
| includeDependencies         | boolean | `true`                  | Specifies if dependencies should be processed                                                           |
| includeDependencyManagement | boolean | `false`                 | Specifies if dependency management should be processed                                                  |
| includePlugins              | boolean | `true`                  | Specifies if plugins should be processed                                                                |
| includePluginManagement     | boolean | `false`                 | Specifies if plugin management should be processed                                                      |
| traverse                    | boolean | `true`                  | Specifies if the resolution should traverse                                                             |
| profileIncludes             | List    | `*`                     | Profile include patterns (with wildcard support)                                                        |
| profileExcludes             | List    | `[]`                    | Profile exclude patterns (with wildcard support)                                                        |
| scopeIncludes               | List    | `*`                     | Transitive scope exclude patterns (with wildcard support)                                               |
| scopeExcludes               | List    | `test`                  | Transitive scope exclude patterns (with wildcard support)                                               |
| includeOptional             | boolean | `true`                  | Specifies if optional transitive dependencies should be processed                                       |
| failOnError                 | boolean | `false`                 | Specifies if the build will fail if there are errors during execution or not                            |
| skip                        | boolean | `false`                 | Skip this goal execution                                                                                |

The above parameters are mapped to user properties of the form `cache.offline.PROPERTY`.
 `List` values must be comma separated.

### General usage

Define a plugin management entry in your parent pom.
```xml
<project>
    <!-- ... -->
    <pluginManagement>
        <plugin>
            <groupId>io.helidon.build-tools</groupId>
            <artifactId>helidon-build-cache-maven-plugin</artifactId>
            <version>${version.plugin.helidon-build-tools}</version>
        </plugin>
    </pluginManagement>
</project>
```

```shell
mvn build-cache:go-offline
```
