# Helidon Archetype Maven Plugin

This plugin provides Maven goals specific to Helidon archetypes.

#### Goals

* [jar](#goal-jar)
* [integration-test](#goal-integration-test)

## Goal: `jar`

Maven goal to package archetypes.

This goal binds to the `package` phase by default.

### Optional Parameters

| Property                 | Type    | Default<br/>Value                       | Description                                                                          |
|--------------------------|---------|-----------------------------------------|--------------------------------------------------------------------------------------|
| sourceDirectory          | File    | `${project.basedir}/src/main/archetype` | The archetype source directory                                                       |
| outputDirectory          | File    | `${project.build.directory}`            | The project build output directory. (e.g. {@code target/})                           |
| finalName                | String  | `${project.build.finalName}`            | Name of the generated JAR                                                            |
| mavenArchetypeCompatible | boolean | `true`                                  | Indicate if the generated JAR should be compatible with the `maven-archetype-plugin` |

### General usage

The plugin includes a custom packaging `helidon-archetype`. You need to declare the plugin as an extension to enable
 the use of the custom packaging.

```xml
<project>
    <!-- ... -->
    <packaging>helidon-archetype</packaging>
    <build>
        <plugins>
            <plugin>
                <groupId>io.helidon.build-tools</groupId>
                <artifactId>helidon-archetype-maven-plugin</artifactId>
                <extensions>true</extensions>
            </plugin>
        </plugins>
    </build>
</project>
```

## Goal: `integration-test`

Maven goal to test Helidon archetypes.

| Property                 | Type    | Default<br/>Value                               | Description                                                                                                                |
|--------------------------|---------|-------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| invokerEnvVars           | Map     | `{}`                                            | Invoker environment variables                                                                                              |
| permutation              | String  | `null`                                          | Indices (comma separated) of the permutations to process                                                                   |
| permutationStartIndex    | Integer | `1`                                             | Permutation start index (resume-from)                                                                                      |
| permutationsOnly         | boolean | `false`                                         | Whether to only generate input permutations                                                                                |
| generatePermutations     | boolean | `true`                                          | Whether to auto-compute input permutations                                                                                 |
| permutationFilters       | List    | `[]`                                            | Permutation filters to filter the computed permutations.                                                                   |
| permutationFiltersFile   | File    | `null`                                          | Properties file that contains filters to filter the computed permutations.                                                 |
| inputFilters             | List    | `[]`                                            | Input filters to use when computing permutations                                                                           |
| inputFiltersFile         | File    | `null`                                          | Properties file that contains filters to use when computing permutations                                                   |
| externalDefaults         | boolean | `false`                                         | External defaults to use when generating archetypes                                                                        |
| externalValues           | boolean | `false`                                         | External values to use when generating archetypes                                                                          |
| testGoal                 | String  | `package`                                       | The goal to use when building archetypes.                                                                                  |
| mavenArchetypeCompatible | boolean | `60`                                            | Indicate if the project should be generated with the maven-archetype-plugin or with the Helidon archetype engine directly. |
| debug                    | boolean | `false`                                         | Whether to show debug statements in the build output                                                                       |
| showVersion              | boolean | `false`                                         | flag to show the maven version used.                                                                                       |
| streamLogs               | boolean | `true`                                          | Flag used to determine whether the build logs should be output to the normal mojo log.                                     |
| noLog                    | boolean | `false`                                         | Suppress logging to the `build.log` file                                                                                   |
| testProjectsDirectory    | File    | `${project.build.testOutputDirectory}/projects` | Directory of test projects                                                                                                 |
| skip                     | boolean | `false`                                         | Skip the integration test                                                                                                  |

The above parameters are mapped to user properties of the form `archetype.test.PROPERTY`, e.g. `-Darchetype.test.skip=true`.