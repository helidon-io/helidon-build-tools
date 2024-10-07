# Helidon Stager Maven Plugin

A plugin that provides an "ant" like feature to stage a directory.

#### Goals

* [stage](#goal-stage)

## Goal: `stage`

Maven goal to stage a directory.

This goal binds to the `package` phase by default.

### Parameters

| Property       | Type                   | Default Value | Description                                               |
|----------------|------------------------|---------------|-----------------------------------------------------------|
| skip           | boolean                | `false`       | Skip execution of this goal                               |
| directories    | List<StagingDirectory> |               | see [StagingDirectory](#StagingDirectory)                 |
| maxRetries     | int                    | `5`           | configuration for the tasks that support retries          |
| taskTimeout    | int                    | `-1.`         | configuration (in ms) for the tasks that support timeouts |
| connectTimeout | int                    | `-1`          | configuration (in ms) for the download task               |
| executor       | ExecutorConfig         |               | see [ExecutorConfig](#ExecutorConfig)                     |

The above parameters are mapped to user properties of the form `stager.PROPERTY`, e.g. `-Dstager.skip=true`.

#### StagingDirectory

```xml
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
                target="docs/{version}"
        >
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
        <unpack-artifact
                url="https://download.acme.com/{version}/acme-site.jar"
                target="docs/{version}"
        >
            <!-- iterators can be nested under any task to create a loop -->
            <iterators>
                <variables>
                    <!-- the version variable is referenced using {version} in the task definition -->
                    <variable name="version">
                        <value>1.2.3</value>
                    </variable>
                </variables>
            </iterators>
        </unpack-artifact>
        <!-- ... -->
    </unpacks>

    <!-- downloads is a container of download tasks -->
    <downloads>

        <!-- a task to download files -->
        <download
                url="https://download.acme.com/{version}/cli-{platform}-amd64"
                target="cli/{version}/{platform}/acme"
        >
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
        <template
                source="index.html.hbs"
                target="docs/index.html"
        >
            <!-- variables can be used to define variables referenced in the template -->
            <variables>
                <variable name="location" value="./latest/index.html"/>
                <variable name="title" value="Acme Documentation"/>
                <variable name="description" value="Acme Documentation"/>
                <variable name="og-url" value="https://acme.com/docs"/>
                <variable name="og-description" value="Documentation"/>
            </variables>
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
</directory>
```

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

```xml

<project>
    <!-- ... -->
    <plugin>
        <groupId>io.helidon.build-tools</groupId>
        <artifactId>helidon-stager-maven-plugin</artifactId>
        <version>${project.version}</version>
        <configuration>
            <directories>
                <directory target="${project.build.directory}/stage">
                    <archives>
                        <archive target="data/${version}/data.zip">
                            <templates>
                                <template source="versions.hbs" target="versions.json">
                                    <variables>
                                        <variable name="versions">
                                            <value>2.5.0</value>
                                            <value>2.4.2</value>
                                            <value>2.4.0</value>
                                            <value>2.0.1</value>
                                            <value>2.0.0</value>
                                        </variable>
                                    </variables>
                                </template>
                            </templates>
                        </archive>
                    </archives>
                </directory>
            </directories>
        </configuration>
    </plugin>
</project>
```
