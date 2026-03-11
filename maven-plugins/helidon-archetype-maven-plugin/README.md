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

| Property          | Type    | Default<br/>Value                     | Description                                                                                       |
|-------------------|---------|---------------------------------------|---------------------------------------------------------------------------------------------------|
| invokerEnvVars    | Map     | `{}`                                  | Invoker environment variables                                                                     |
| test              | String  | `null`                                | Indices (comma separated) of the variations to process                                            |
| startIndex        | int     | `1`                                   | Variation start index                                                                             |
| endIndex          | int     | `-1`                                  | Variation end index                                                                               |
| generateOnly      | boolean | `false`                               | Whether to only generate input variations                                                         |
| generateTests     | boolean | `true`                                | Whether to auto-compute input variations                                                          |
| failOnUnbounded   | boolean | `false`                               | Whether to fail when computed variations include unbounded inputs                                 |
| maxVariations     | long    | `-1`                                  | Maximum projected variation count to allow during computation, use `-1` for no limit              |
| plansFile         | File    | `null`                                | XML file that defines the named plans used to generate the test projects                          |
| externalDefaults  | Map     | `null`                                | External defaults to use when generating archetypes                                               |
| externalValues    | Map     | `null`                                | External values to use when generating archetypes                                                 |
| testGoal          | String  | `package`                             | The goal to use when building archetypes.                                                         |
| testProfiles      | List    | `[]`                                  | The profiles to use when building archetypes.                                                     |
| invokerId         | String  | `maven`                               | Specify the invoker used to generate the test projects. See [invokerId](#InvokerId).              |
| cliDataDirectory  | File    | `${project.build.directory}/cli-data` | Directory that contains the staged `cli-data` used when the invoker is a Helidon CLI distribution |
| debug             | boolean | `false`                               | Whether to show debug statements in the build output                                              |
| showVersion       | boolean | `false`                               | flag to show the maven version used.                                                              |
| streamLogs        | boolean | `true`                                | Flag used to determine whether the build logs should be output to the normal mojo log.            |
| noLog             | boolean | `false`                               | Suppress logging to the `build.log` file                                                          |
| projectsDirectory | File    | `${project.build.directory}/tests`    | Directory of test projects                                                                        |
| skip              | boolean | `false`                               | Skip this goal                                                                                    |

The above parameters are mapped to user properties of the form `archetype.test.PROPERTY`, e.g. `-Darchetype.test.skip=true`.

### Variation Plans

`plansFile` is the supported way to bound archetype variation generation. Each `<plan>` pins a coherent
scenario using `<values>` and `<defaults>`, then the plugin computes variations for each plan
independently and merges the unique results. Use each plan's `<rules>` block for any exclusions needed inside
that scenario. Use `<fragment>` for reusable values, defaults, and rules; a `<plan>` or `<fragment>` can
inherit one or more fragments via `extends="fragment-a,fragment-b"`. Inherited values/defaults merge
left-to-right, and local rules are appended after inherited rules.

```xml
<plans xmlns="https://helidon.io/archetype-plans/1.0"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="https://helidon.io/archetype-plans/1.0 https://helidon.io/xsd/archetype-plans-1.0.xsd">
    <fragment id="custom/base">
        <values>
            <app-type>custom</app-type>
            <media></media>
        </values>
    </fragment>

    <fragment id="custom/security" extends="custom/base">
        <values>
            <security>true</security>
            <extra></extra>
        </values>
        <rules>
            <rule if="${security.atz} == []">
                <exclude if="sizeof ((list) ${security.atn}) != 1"/>
            </rule>
        </rules>
    </fragment>

    <plan id="custom/security" extends="custom/security"/>

    <plan id="custom/observability" extends="custom/base">
        <values>
            <metrics>true</metrics>
            <health>true</health>
            <tracing>true</tracing>
        </values>
    </plan>
</plans>
```

If `plansFile` is not configured, the plugin computes the full variation set from the archetype inputs and
the configured external values/defaults. Use plans when you need a curated set of relevant scenarios instead of
the full Cartesian product.

### InvokerId

The supported values are:

- `maven` Use the Maven Archetype Engine to generate the test projects
- `helidon` Use the Helidon Archetype Engine to generate the test projects
- `groupId:artifactId[:extension[:classifier]]:version` Use a specific version of the Helidon CLI to generate the test projects,
  requires a `cli-data` directory, see [stage](#goal-stage).

Note that when using the Helidon CLI requires `<cliData>true</cliData>`.
