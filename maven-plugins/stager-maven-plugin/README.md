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

| Property                       | Applies to         | Executor parameter |
|--------------------------------|--------------------|--------------------|
| `stager.executor.kind`         | all executors      | `kind`             |
| `stager.executor.nThreads`     | `FIXED`            | `nThreads`         |
| `stager.executor.corePoolSize` | `SCHEDULED`        | `corePoolSize`     |
| `stager.executor.parallelism`  | `WORKSTEALINGPOOL` | `parallelism`      |

For example, run a standalone stager configuration with a fixed pool of four
threads using `-Dstager.executor.kind=FIXED -Dstager.executor.nThreads=4`.

#### Stager Configuration

A stager XML configuration file is rooted at `<stager>` and supports top-level
children in this order: `<include src="..."/>`, `<properties>`, then
`<directories>`.

```xml
<stager xmlns="https://helidon.io/stager/1.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="https://helidon.io/stager/1.0 https://helidon.io/xsd/stager-1.0.xsd">
    <include src="stager-common.xml"/>
    <properties>
        <property name="docs.1.version" value="1.2.3"/>
    </properties>
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
                            <!-- the version variable is substituted as {version} in the task definition -->
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
                            <!-- the version variable is substituted as {version} in the task definition -->
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

Use `<include src="..."/>` as a top-level stager configuration directive before
`<properties>` and `<directories>`. The included file is another stager XML
configuration file. Its `<stager>` children are inserted where the directive
appears.

Include paths are resolved relative to the declaring file unless the path is absolute.
An include declared directly in the POM is resolved relative to the project base
directory; nested includes from XML files are resolved relative to the XML file that declares them.

The `src` value is treated as a literal path during include expansion.

##### Iterator variables

Variables are supported only inside a task's `<iterators>` element. Each
`<variable>` requires a `name` and is an ordered list. An unattributed `<value>`
is a nonblank string row, and an attributed `<value>` is a nonempty flat string
map. A singleton still uses one `<value>`, while a variable without values is
an empty list:

```xml
<iterators>
    <variables>
        <variable name="channels">
            <value>stable</value>
            <value>preview</value>
        </variable>
    </variables>
    <variables>
        <variable name="releases">
            <value version="4.2.0" channel="stable"/>
            <value version="4.3.0" channel="preview"/>
        </variable>
    </variables>
    <variables>
        <variable name="empty"/>
    </variables>
</iterators>
```

Rows in one variable must all have the same shape. Text rows bind the variable
name to each value during iteration. Map rows expose their attributes directly
as iterator substitutions. Values cannot be empty, combine text and
attributes, or contain child elements.

Multiple variables in one `<variables>` block form a matrix of value
combinations. Multiple `<variables>` blocks create separate iterators in XML
order. An empty variable causes its iterator to run zero times. The `join`
attribute on `<iterators>` or `<variables>` retains the usual sequential
execution behavior.

Variables cannot use references, aliases, nested variable declarations, or
shared declarations at the root, directory, or action level. Declare every
iterator's values inline.

### Template models

Each `<template>` can contain one optional free-form `<model>`. The model is
interpreted as ordered XML rather than as Java maps and lists. Leaf values
render their element text, and elements with children retain their XML child
order.

Descendant attributes are addressed with an `@` prefix. Attributes do not
participate in child iteration and cannot shadow same-named child elements:

```xml
<model>
    <release version="4.2.0">
        <version>child-version</version>
    </release>
</model>
```

```mustache
{{release.version}}   <!-- child-version -->
{{release.@version}}  <!-- 4.2.0 -->
```

At every XML scope, an exact child name is resolved before dotted traversal.
For example, `{{properties.cli.version}}` selects a literal
`<cli.version>` child of `<properties>` when present. Once a dotted path
selects an XML value, a missing remainder does not fall back to another scope.
Java fields and methods are never exposed, so an expression such as
`{{title.length}}` cannot invoke `String.length()`.

When a template model does not declare a root-level `properties` field,
preprocessing adds one populated from the effective top-level stager
`<properties>`. This includes templates without an explicit `<model>`. Each
effective property name is materialized as a free-form model element whose
value is `${property.name}`. The ordinary configuration interpolation pass
then resolves those references.

Iterator variables remain available for task iteration and `{name}` task
placeholders, but are not added to the Mustache model automatically.

With `cli.version` and `schemas.version` declared in the top-level stager
`<properties>`, the template only needs to declare its other local fields:

```xml
<template source="properties.mustache" target="properties.txt">
    <model>
        <docs><current>4.2.0</current></docs>
    </model>
</template>
```

`{{#container}}` iterates every direct child in true XML order.
`{{#container.item}}` iterates only the matching children in their relative
order. Each active sequence calculates its own `index`, `first`, and `last`
metadata. Iteration items also provide `name` and `value`; `{{.}}` and
`{{value}}` render the current element text, while a complex `value` remains
usable for dotted child lookup such as `{{value.title}}`.

Exact local XML children take precedence over iteration metadata. When a local
child uses a metadata name, add leading underscores until a name is not occupied
by local XML. Any number of leading underscores can be removed to reach
metadata. For example, with `<name>real</name>`, `{{name}}` renders `real` and
`{{_name}}` renders the iteration item's XML name. If `<_name>` also exists,
use `{{__name}}` for the metadata.

Lookup within an iteration item proceeds through local XML, local metadata,
the containing XML element, and then ordinary outer Mustache scopes. Parent
fields therefore remain visible: `{{cli.version}}` can resolve another child
of `properties` while that container is being iterated. Local fields still win
when heterogeneous repeated elements expose different child shapes at the same
template expression.

An empty leaf renders as an empty string and is false in a section, even when
it has attributes. Inverted sections therefore work for both plain and
attributed empty leaves. Its attributes remain available through expressions
such as `{{empty.@state}}`.

An explicit model `<properties>` field is a complete local override, whether it
is a container, empty, scalar, or repeated. In that case, preprocessing does not
inject any top-level stager properties into the model.

Model leaf text and attribute values can use iterator placeholders. Values are
resolved for each template execution, for example
`<version order="{order}">{version}</version>`. The `<model>` container itself
cannot have attributes, and no model element can mix text and child content.
Errors report the qualified model path.

After includes are expanded, the preprocessor collects the effective
properties and builds the effective `<directories>` tree. It injects property
references into template models that do not declare `properties`, then runs
`${...}` interpolation once across that tree.

Stager `<properties>` are resolved before Maven expressions. For duplicate
stager property names, the last definition in the expanded configuration wins.
This means caller properties declared after includes override included fallback
properties.

The resulting ordered property elements are exposed as `properties` in each
template model that does not provide its own field; no separate property data
is passed to the model parser.

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

The same logical stager children are declared directly under the plugin
`<configuration>` element when configuration is kept in `pom.xml`. Do not wrap
POM configuration in a `<stager>` element. The supported order is
`<include src="..."/>`, `<properties>`, then `<directories>`.

```xml
<configuration>
    <include src="stager-common.xml"/>
    <properties>
        <property name="stage.path" value="${project.build.directory}/stage"/>
    </properties>
    <directories>
        <directory target="${stage.path}">
            <files>
                <file target="{channel}/info.txt">
                    <iterators>
                        <variables>
                            <variable name="channel">
                                <value>stable</value>
                            </variable>
                        </variables>
                    </iterators>
                </file>
            </files>
        </directory>
    </directories>
</configuration>
```

XML attributes and text in stager configuration are interpolated before task
execution. `${...}` expressions are resolved from stager `<properties>` first,
then Maven expressions. Expressions that cannot be resolved are left unchanged.
Recursive property values are supported. Iterator variables such as `{version}`
are resolved during task execution. See [Includes](#includes) for
include-specific property precedence.

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
            <directories>
                <directory target="${stage.path}">
                    <files>
                        <file target="{channel}/info.txt">
                            <iterators>
                                <variables>
                                    <variable name="channel">
                                        <value>stable</value>
                                    </variable>
                                </variables>
                            </iterators>
                        </file>
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
    <directories>
        <directory target="${stage.path}">
            <files>
                <file target="{channel}/info.txt">
                    <iterators>
                        <variables>
                            <variable name="channel">
                                <value>stable</value>
                            </variable>
                        </variables>
                    </iterators>
                </file>
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
