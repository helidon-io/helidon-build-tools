# Helidon Stager Maven Plugin

A plugin that provides an "ant" like feature to stage a directory.

#### Goals

* [stage](#goal-stage)

## Goal: `stage`

Maven goal to stage a directory.

This goal binds to the `package` phase by default.

### Parameters

| Property       | Type                          | Default Value | Description                                               |
|----------------|-------------------------------|---------------|-----------------------------------------------------------|
| skip           | boolean                       | `false`       | Skip execution of this goal                               |
| properties     | PlexusConfiguration (raw XML) |               | see [POM configuration](#pom-configuration)               |
| variables      | PlexusConfiguration (raw XML) |               | see [POM configuration](#pom-configuration)               |
| include        | PlexusConfiguration (raw XML) |               | see [POM configuration](#pom-configuration)               |
| directories    | PlexusConfiguration (raw XML) |               | see [POM configuration](#pom-configuration)               |
| configFile     | File                          |               | stager XML configuration file                             |
| maxRetries     | int                           | `5`           | configuration for the tasks that support retries          |
| taskTimeout    | int                           | `-1.`         | configuration (in ms) for the tasks that support timeouts |
| connectTimeout | int                           | `-1`          | configuration (in ms) for the download task               |
| executor       | ExecutorConfig                |               | see [ExecutorConfig](#ExecutorConfig)                     |

Scalar parameters are mapped to user properties of the form `stager.PROPERTY`, e.g. `-Dstager.skip=true`.

When the POM does not provide an `<executor>` configuration, executor settings
can be supplied as Maven user properties. A configured POM `<executor>` is
used as a complete object and is not overridden by user properties.

| Property | Applies to | Executor parameter |
|---|---|---|
| `stager.executor.kind` | all executors | `kind` |
| `stager.executor.nThreads` | `FIXED` | `nThreads` |
| `stager.executor.corePoolSize` | `SCHEDULED` | `corePoolSize` |
| `stager.executor.parallelism` | `WORKSTEALINGPOOL` | `parallelism` |

For example, run a standalone stager configuration with a fixed pool of four
threads using `-Dstager.executor.kind=FIXED -Dstager.executor.nThreads=4`.

#### Stager Configuration

A stager XML configuration file is rooted at `<stager>` and supports top-level children in this
order: `<include src="..."/>`, `<properties>`, `<variables>`, then `<directories>`.

```xml
<stager xmlns="https://helidon.io/stager/1.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="https://helidon.io/stager/1.0 https://helidon.io/xsd/stager-1.0.xsd">
    <include src="stager-common.xml"/>
    <properties>
        <property name="docs.1.version" value="1.2.3"/>
    </properties>
    <variables>
        <variable name="channel" value="stable"/>
    </variables>
    <directories>
        <!-- directory is a container of tasks -->
        <directory target="${project.build.directory}/stage">

            <!-- unpack-artifacts is a container of unpack-artifact tasks -->
            <unpack-artifacts>

                <!-- a task to download and unpack an artifact (.jar, .zip) -->
                <unpack-artifact
                        groupId="com.acme"
                        artifactId="acme-docs"
                        version="{version}"
                        excludes="META-INF/**"
                        target="docs/{version}">
                    <!-- iterators can be nested under any task to create a loop -->
                    <iterators>
                        <variables>
                            <!-- the version variable is referenced using {version} in the task definition -->
                            <variable name="version">
                                <value>${docs.1.version}</value>
                            </variable>
                        </variables>
                    </iterators>
                </unpack-artifact>
                <!-- ... -->
            </unpack-artifacts>

            <!-- unpacks is a container of unpack tasks -->
            <unpacks>

                <!-- a task to download and unpack a file (.jar, .zip) -->
                <unpack url="https://download.acme.com/{version}/acme-site.jar"
                        target="docs/{version}">
                    <!-- iterators can be nested under any task to create a loop -->
                    <iterators>
                        <variables>
                            <!-- the version variable is referenced using {version} in the task definition -->
                            <variable name="version">
                                <value>1.2.3</value>
                            </variable>
                        </variables>
                    </iterators>
                </unpack>
                <!-- ... -->
            </unpacks>

            <!-- downloads is a container of download tasks -->
            <downloads>

                <!-- a task to download files -->
                <download url="https://download.acme.com/{version}/cli-{platform}-amd64"
                        target="cli/{version}/{platform}/acme">
                    <iterators>
                        <variables>
                            <!--
                             defining more than one variable creates a matrix
                             I.e. darwin-1.0.0, darwin-2.0.0, linux-1.0.0, linux-2.0.0
                            -->
                            <variable name="platform">
                                <value>darwin</value>
                                <value>linux</value>
                            </variable>
                            <variable name="version">
                                <value>1.0.0</value>
                                <value>2.0.0</value>
                            </variable>
                        </variables>
                    </iterators>
                </download>
                <!-- ... -->
            </downloads>

            <!-- archives is a container of archive tasks -->
            <archives>

                <!--
                    archive is a task to create a zip archive, and a container of tasks (similar to directory)
                 -->
                <archive target="data/{version}/data.zip">

                    <!-- put any task here to add content to the archive -->
                </archive>
                <!-- ... -->
            </archives>

            <!-- templates is a container of template tasks -->
            <templates>

                <!-- a task to render a handlebar template (mustache) -->
                <template source="index.html.hbs" target="docs/index.html">
                    <model>
                        <location>./latest/index.html</location>
                        <title>Acme Documentation</title>
                        <description>Acme Documentation</description>
                        <og-url>https://acme.com/docs</og-url>
                        <og-description>Documentation</og-description>
                    </model>
                </template>
                <!-- ... -->
            </templates>

            <!-- files is a container of file tasks -->
            <files>
                <!-- a file task can be used to create text files -->
                <file target="cli-data/latest"><![CDATA[
This is a text file.
]]></file>
                <!-- ... -->
            </files>

            <!-- symlinks is a container of symlink tasks -->
            <symlinks>

                <!-- symlink is a task to create a symlink -->
                <symlink
                        source="docs/${docs.latest.version}"
                        target="docs/latest"
                />
                <!-- ... -->
            </symlinks>

            <!-- copy-artifacts is a container of copy-artifact tasks -->
            <copy-artifacts>

                <!-- download any Maven artifact to a given location -->
                <copy-artifact
                        groupId="com.acme"
                        artifactId="acme-engine"
                        classifier="schema"
                        type="xsd"
                        version="1.0.0"
                        target="xsd/engine-1.0.xsd"
                />
            </copy-artifacts>

            <!-- copies is a container of copy tasks -->
            <copies>

                <!-- copy directory contents to a staging directory -->
                <copy source="src/docs" target="docs">
                    <includes>
                        <include>**/*.html</include>
                    </includes>
                    <excludes>
                        <exclude>draft/**</exclude>
                    </excludes>
                </copy>
            </copies>
        </directory>
    </directories>
</stager>
```

The `<copy>` task copies the contents of a project directory into the target
staging directory. The `source` value must be a directory path; it does not
copy individual files, rename targets, or expand globs. Use nested
`<include>` and `<exclude>` filters for glob-style selection relative to the
copy source directory.

##### Includes

Use `<include src="..."/>` as a top-level stager configuration directive before `<properties>`,
`<variables>`, and `<directories>`. The referenced file is another stager XML configuration file. Its
`<stager>` children are inserted where the directive appears.

Include paths are resolved relative to the declaring file unless the path is absolute.
An include declared directly in the POM is resolved relative to the project base
directory; nested includes from XML files are resolved relative to the XML file that declares them.

The `src` value is treated as a literal path during include expansion.

Root-level `<variables>` and direct `<directories><variables>` are merged into the effective global
configuration. Root-level variables are collected before direct `<directories><variables>`;
within that collection order, duplicate global `<variable name="...">` entries use the last
definition. Because `<include>` appears before caller `<variables>` and `<directories>`, caller
variables override included fallback variables with the same name.
Iterator-local variables are not part of this global precedence rule.

Variable references retain the referenced name by default. Add `name` to
expose the same value under an alias, which is useful for iterator
placeholders:

```xml
<variable name="version" ref="release.versions"/>
```

An ordinary variable can aggregate previously declared lists by containing
direct variable references. The referenced lists are appended in declaration
order. Values are not deduplicated, and map values are appended as complete
list elements rather than merged:

```xml
<variable name="archetype.versions">
    <variable ref="archetype.v2.versions"/>
    <variable ref="archetype.v3.versions"/>
    <variable ref="archetype.v4.versions"/>
</variable>
```

Each aggregate source must be a list. References may resolve variables in an
enclosing scope or earlier siblings in the same collection; forward references
are invalid. Nested `<value>` and aggregate `<variable>` children cannot be
mixed. When `ref` is present, it takes precedence over inline and nested
content.

### Template models

Each `<template>` can contain one optional free-form `<model>`. Leaf elements
are strings (including empty leaves); elements with children are ordered model
collections. Template-local `<variables>` are no longer supported. Global and
iterator variables remain available for task iteration and `{name}` task
placeholders, but are not added to the Mustache model automatically.

```xml
<template source="properties.mustache" target="properties.txt">
    <model>
        <properties>
            <cli.version>4.2.0</cli.version>
            <schemas.version>2026.07</schemas.version>
        </properties>
        <docs><current>4.2.0</current></docs>
    </model>
</template>
```

`{{#properties}}` iterates its children in XML order. Each item provides
`name`, `value`, `index`, `first`, and `last`; nested fields are available
directly. For example, use `{{name}}={{value}}`, or for a nested child use
`{{title}}` and `{{git.url}}`. Parent fields remain visible in the section,
so `{{cli.version}}` is available while iterating `properties`. Dotted XML
names are literal map keys when resolved within their containing section.

Model leaf text can use iterator placeholders. The value is resolved for each
template execution: `<version>{version}</version>`. Model siblings must have
unique names and model elements cannot have attributes or mixed text and child
content; errors report the qualified model path.

The previous `.entries` decoration has been removed. Iterate the model
collection directly.

Repeated values conflict when scalar strings differ or their types differ.
Conflicts fail the task rather than choosing the first or last value, and
nested conflicts report the qualified variable path, such as
`release.version`. An empty value remains invalid when used as a normal
template value.

Included files may declare optional fallback variables without a value, for example
`<variable name="preview-versions"/>`. These empty variables are intended for iterator inputs:
when an iterator references an empty variable, it contributes no values and the task runs zero
times. Empty variables are not substituted as empty strings; using one as a normal template or
task value fails unless the including configuration already supplied a valued definition.

After includes are expanded, `${...}` interpolation runs once across the expanded configuration.

Stager `<properties>` are resolved before Maven expressions. For duplicate stager property names,
the last definition in the expanded configuration wins. This means caller properties declared after
includes override included fallback properties.

Missing files and circular chains fail the goal and report the path that could not be loaded.

The `list-files` `<include>` element is still a pattern element, not a file inclusion directive:

```xml
<stager xmlns="https://helidon.io/stager/1.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="https://helidon.io/stager/1.0 https://helidon.io/xsd/stager-1.0.xsd">
    <directories>
        <directory target="${stage.path}">
            <files>
                <file target="sitemap.txt">
                    <list-files dir="docs">
                        <include>**/*.html</include>
                    </list-files>
                </file>
            </files>
        </directory>
    </directories>
</stager>
```

##### POM configuration

The same logical stager children are declared directly under the plugin `<configuration>` element
when configuration is kept in `pom.xml`. Do not wrap POM configuration in a `<stager>` element.
The supported order is `<include src="..."/>`, `<properties>`, `<variables>`, then
`<directories>`.

```xml
<configuration>
    <include src="stager-common.xml"/>
    <properties>
        <property name="stage.path" value="${project.build.directory}/stage"/>
    </properties>
    <variables>
        <variable name="channel" value="stable"/>
    </variables>
    <directories>
        <directory target="${stage.path}">
            <files>
                <file target="{channel}/info.txt">Channel: {channel}</file>
            </files>
        </directory>
    </directories>
</configuration>
```

XML attributes and text in stager configuration are interpolated before task execution. `${...}`
expressions are resolved from stager `<properties>` first, then Maven expressions. Expressions that
cannot be resolved are left unchanged. Recursive property values are supported. Task variables such
as `{version}` are resolved during task execution. See [Includes](#includes) for include-specific
property and global variable precedence.

##### ExecutorConfig

| Property   | Type                | Default Value | Description                      |
|------------|---------------------|---------------|----------------------------------|
| kind       | Kind                | DEFAULT       | see [Kind](#ExecutorConfig-Kind) |
| parameters | Map<String, String> |               | see [Kind](#ExecutorConfig-Kind) |

##### ExecutorConfig Kind

| Kind             | Description                                                                                                             |
|------------------|-------------------------------------------------------------------------------------------------------------------------|
| DEFAULT          | Uses the current thread (no parallelism)                                                                                |
| CACHED           | Uses `Executors#newCachedThreadPool`                                                                                    |
| SINGLE           | Uses `Executors#newSingleThreadExecutor`                                                                                |
| FIXED            | Uses `Executors#newFixedThreadPool(int)`. `nThreads` : number of threads, default is `5`.                               |
| SCHEDULED        | Uses `Executors#newScheduledThreadPool(int)`. `corePoolSize`: number of threads to keep in the pool, default is `5`     |
| SINGLESCHEDULED  | Uses `Executors#newSingleThreadScheduledExecutor`                                                                       |
| VIRTUAL          | Not implemented yet                                                                                                     |
| WORKSTEALINGPOOL | Uses `Executors#newWorkStealingPool(int)`. `parallelism`: targeted parallelism level, default is `-1` (max parallelism) |

### General usage

#### pom.xml configuration

```xml
<project>
    <!-- ... -->
    <plugin>
        <groupId>io.helidon.build-tools</groupId>
        <artifactId>helidon-stager-maven-plugin</artifactId>
        <version>${project.version}</version>
        <configuration>
            <include src="stager-common.xml"/>
            <properties>
                <property name="stage.path" value="${project.build.directory}/stage"/>
            </properties>
            <variables>
                <variable name="channel" value="stable"/>
            </variables>
            <directories>
                <directory target="${stage.path}">
                    <files>
                        <file target="{channel}/info.txt">Channel: {channel}</file>
                    </files>
                </directory>
            </directories>
        </configuration>
    </plugin>
</project>
```

#### stager.xml configuration

The `stage` goal can also read stager configuration from `stager.xml`:

```xml
<stager xmlns="https://helidon.io/stager/1.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="https://helidon.io/stager/1.0 https://helidon.io/xsd/stager-1.0.xsd">
    <include src="stager-common.xml"/>
    <properties>
        <property name="stage.path" value="${project.build.directory}/stage"/>
    </properties>
    <variables>
        <variable name="channel" value="stable"/>
    </variables>
    <directories>
        <directory target="${stage.path}">
            <files>
                <file target="{channel}/info.txt">Channel: {channel}</file>
            </files>
        </directory>
    </directories>
</stager>
```

```shell
mvn stager:{version}:stage -Dstager.configFile=stager.xml
```

This short invocation requires the plugin group in Maven `settings.xml`:

```xml
<pluginGroups>
    <pluginGroup>io.helidon.build-tools</pluginGroup>
</pluginGroups>
```

Without the plugin group, use the full plugin coordinate:

```shell
mvn io.helidon.build-tools:helidon-stager-maven-plugin:{version}:stage -Dstager.configFile=stager.xml
```

If both `configFile` and POM stager configuration are configured, `configFile` takes precedence.
If none are configured, the goal does nothing.
