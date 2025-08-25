# Helidon Archetype Maven Plugin

This plugin provides Maven goals specific to Helidon archetypes.

#### Goals

* [general usage](#general-usage)
* [compile](#goal-compile)
* [jar](#goal-jar)
* [stage](#goal-stage)
* [integration-test](#goal-integration-test)

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

## Goal: `compile`

Maven goal to compile archetypes.

This goal binds to the `package` phase by default.

### Optional Parameters

| Property           | Type    | Default<br/>Value                       | Description                    |
|--------------------|---------|-----------------------------------------|--------------------------------|
| sourceDirectory    | File    | `${project.basedir}/src/main/archetype` | The archetype source directory |
| archetypeDirectory | File    | `${project.build.directory}/archetype`  | The archetype output directory |
| compilerOptions    | List    | `[]`                                    | Compiler options               |
| skip               | boolean | `false`                                 | Skip this goal                 |

The following parameters are mapped to user properties of the form `archetype.compile.PROPERTY`, e.g.
`-Darchetype.compile.skip=true`

- skip

## Goal: `jar`

Maven goal to package archetypes.

This goal binds to the `package` phase by default.

### Optional Parameters

| Property                 | Type    | Default<br/>Value                      | Description                                                                          |
|--------------------------|---------|----------------------------------------|--------------------------------------------------------------------------------------|
| archetypeDirectory       | File    | `${project.build.directory}/archetype` | The archetype directory to archive                                                   |
| outputDirectory          | File    | `${project.build.directory}`           | The project build output directory. (e.g. {@code target/})                           |
| finalName                | String  | `${project.build.finalName}`           | Name of the generated JAR                                                            |
| mavenArchetypeCompatible | boolean | `true`                                 | Indicate if the generated JAR should be compatible with the `maven-archetype-plugin` |
| skip                     | boolean | `false`                                | Skip this goal                                                                       |

The following parameters are mapped to user properties of the form `archetype.jar.PROPERTY`, e.g. `-Darchetype.jar.skip=true`

- skip

## Goal: `stage`

Maven goal to generate a `cli-data` directory to use archetypes with the helidon CLI.

This goal binds to the `package` phase by default.

### Optional Parameters

| Property          | Type    | Default<br/>Value                     | Description                                 |
|-------------------|---------|---------------------------------------|---------------------------------------------|
| stageCliDirectory | boolean | `${project.build.directory}/cli-data` | Path of the `cli-data` directory to create. |
| skip              | boolean | `false`                               | Skip this goal                              |

The above parameters are mapped to user properties of the form `archetype.stage.PROPERTY`, e.g. `-Darchetype.stage.skip=true`.

## Goal: `integration-test`

Maven goal to test Helidon archetypes.

| Property                 | Type    | Default<br/>Value                               | Description                                                                                                                |
|--------------------------|---------|-------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| invokerEnvVars           | Map     | `{}`                                            | Invoker environment variables                                                                                              |
| test                     | String  | `null`                                          | Indices (comma separated) of the variations to process                                                                     |
| startIndex               | Integer | `1`                                             | variation start index                                                                                                      |
| endIndex                 | Integer | `-1`                                            | variation end index                                                                                                        |
| generateOnly             | boolean | `false`                                         | Whether to only generate input variations                                                                                  |
| generateTests            | boolean | `true`                                          | Whether to auto-compute input variations                                                                                   |
| rulesFile                | File    | `null`                                          | Properties file that contains filters to filter the computed variations.                                                   |
| externalDefaults         | boolean | `false`                                         | External defaults to use when generating archetypes                                                                        |
| externalValues           | boolean | `false`                                         | External values to use when generating archetypes                                                                          |
| testGoal                 | String  | `package`                                       | The goal to use when building archetypes.                                                                                  |
| testProfiles             | List    | `[]`                                            | The profiles to use when building archetypes.                                                                              |
| mavenArchetypeCompatible | boolean | `60`                                            | Indicate if the project should be generated with the maven-archetype-plugin or with the Helidon archetype engine directly. |
| invokerId                | String  | `maven`                                         | Specify the invoker used to generate the test projects. See [invokerId](#InvokerId).                                       |
| debug                    | boolean | `false`                                         | Whether to show debug statements in the build output                                                                       |
| showVersion              | boolean | `false`                                         | flag to show the maven version used.                                                                                       |
| streamLogs               | boolean | `true`                                          | Flag used to determine whether the build logs should be output to the normal mojo log.                                     |
| noLog                    | boolean | `false`                                         | Suppress logging to the `build.log` file                                                                                   |
| testProjectsDirectory    | File    | `${project.build.testOutputDirectory}/projects` | Directory of test projects                                                                                                 |
| skip                     | boolean | `false`                                         | Skip this goal                                                                                                             |

The above parameters are mapped to user properties of the form `archetype.test.PROPERTY`, e.g. `-Darchetype.test.skip=true`.

### InvokerId

The supported values are:

- `maven` Use the Maven Archetype Engine to generate the test projects
- `helidon` Use the Helidon Archetype Engine to generate the test projects
- `groupId:artifactId[:extension[:classifier]]:version` Use a specific version of the Helidon CLI to generate the test projects,
  requires a `cli-data` directory, see[stage](#goal-stage).

Note that when using the Helidon CLI requires `<cliData>true</cliData>`.
