# Helidon Build Cache Plugin

This plugin provides "fast-forward" features for Maven builds.

The main focus is fast-forwarding of subsequent Maven invocations without source code changes. Invocations don't have
 to be identical and can differ in life-cycle and configurations. It can be used to "split" the life-cycle into
 distinct invocations without duplicating executions.

E.g. Invoke `mvn compile` and then `mvn package` without running the compilation twice.

### Principles

This plugin provides a Maven extension that manages the state of projects in the reactor and uses recorded information
 to detect plugin executions that can be skipped and removed from the life-cycle.

The state of a project consists of the few things that Maven plugins can update in the build context:
- properties
- "main" source files
- "test" sources files
- project artifact
- attached artifacts
- recorded executions with their effective configuration

The state if saved to `target/state.xml`, if present it is loaded by the extension and used to restore the project
 state and detect execution duplicates. The extension also uses a combined "cache archive file" to save and load all the
 files needed to fast-forward builds.

The state is invalidated when the project files are changed. Changes are detected by looking at the most recent
 `last-modified` file attribute, or by using checksums. When the state is not available, the life-cycle is not modified.

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
 sub-sequent executions are still eligible for fast-forwarding.

The states of modules with inter-project dependencies rely on the dependency graph for indirect state invalidation.
 A module that requires another must declare a dependency, otherwise it will fast-forward even if the required module
 has changed.

Values recorded in the state may not be portable, E.g. file paths. Values that contain the execution root directory
 are recorded in a portable way. However, any other system specific value that is recorded will not be portable and
 cause the cache to be un-usable on a different system.

#### Goals

This plugin does not provide any goal at the moment.

### Configuration

| Property | Type | Default<br/>Value | Description |
| --- | --- | --- | --- |
| skip | Boolean | `false` | Disables build cache |
| projectFilesExcludes | List | `[]` | Project files excludes patterns |
| buildFilesExcludes | List | `[]` | Build files excludes patterns |
| enableChecksums | Boolean | `false` | Enables combined checksums for the project files |
| includeAllChecksums | Boolean | `false` | Enables individual checksums for all project files |
| archiveFile | File | `null` | Path a `.tar` file |
| loadArchive | Boolean | `false` | Loads the cache from the archive file |
| saveArchive | Boolean | `false` | Saves the cache to the archive file |
| executionsExcludes | List | `[]` | Execution exclude patterns |
| executionsIncludes | List | `[*]` | Execution include patterns |

All parameters  are mapped to user properties of the form `cache.PROPERTY`. List parameters are passes as comma
 separated values.

The configuration for the reactor uses the execution root. I.e. the module that contains the top-level `<modules>`
 definition.

The configuration is looked-up from the effective pom ; individual modules can override configuration inherited from
 their parent to configure a given module. E.g. skip the cache for a single module, or configure project files
 excludes etc. Configuration passed with properties trumps everything.

### General usage

You need to use `<extensions>true</extensions>` when declaring the plugin. A good practice would be to define it
 under a profile to isolate the usage.

```xml
<profile>
  <id>cache</id>
  <build>
    <plugins>
      <plugin>
        <groupId>io.helidon.build-tools</groupId>
        <artifactId>helidon-build-cache-maven-plugin</artifactId>
        <extensions>true</extensions>
        <configuration>
          <projectFilesExcludes>
            <exclude>.git/**</exclude>
          </projectFilesExcludes>
          <buildFilesExcludes>
            <exclude>classes/io/grpc/**/*.class</exclude>
          </buildFilesExcludes>
          <enableChecksums>true</enableChecksums>
          <includeAllChecksums>true</includeAllChecksums>
          <archiveFile>${project.build.directory}/build-cache.tar</archiveFile>
          <loadArchive>true</loadArchive>
          <createArchive>true</createArchive>
        </configuration>
      </plugin>
    </plugins>
  </build>
</profile>
```


You then build your project using the profile above:

```bash
mvn package -Pcache
```

The first build would generate the state files under `target/state.xml` and generate an archive under
 `/target/build-cache.tar`. Subsequent builds will re-use the cache.
