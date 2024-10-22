# Helidon CLI

The Helidon CLI lets you easily create a Helidon project by picking from
a set of archetypes.

It also supports a developer loop that performs continuous compilation and
application restart, so you can easily iterate over source code changes.

## Create a New Project

```
helidon init
```

Then answer the questions.

## Developer Loop

```
cd myproject
helidon dev
```

As you make source code changes the project will automatically recompile and
restart your application.

## Implementation Notes

* **impl**: implementation of the Helidon CLI. Uses `harness`, `codegen` and `plugin`
* **harness**: a general purpose CLI harness
* **codegen**: annotation processor for `harness`
* **plugin**: code that would normally be in `impl`, but to reduce native image bloat
  it is included as a jar in the native image and executed by extracting
  the jar on disk and spawning a java process.

Issue [#849](https://github.com/helidon-io/helidon-build-tools/issues/849) 
and related PR [#905](https://github.com/helidon-io/helidon-build-tools/pull/905) added 
a prompt with a list of available Helidon versions to choose from 
for Helidon CLI `init` command. As a result the previous approach was changed when 
user had to choose the default version of Helidon or typed some other one.

## For versions 3.0.4 or less

The site release creates a `latest` file containing the latest Helidon version number:
https://helidon.io/cli-data/latest.

User has to choose the default version of Helidon or types some other one when Helidon CLI `init` command is used.

## For versions after 3.0.4

The `latest` file was replaced by `versions.xml` file that contains information about archetypes versions: 
https://helidon.io/cli-data/versions.xml.

User has to choose the version of Helidon from the list of available versions when Helidon CLI `init` command is used.

## cli-data.zip: archetypes, catalog and metadata.properties

The archetypes used by the CLI (for `init` command) are maintained in
the main Helidon repository and published as part of a Helidon release:
https://repo1.maven.org/maven2/io/helidon/archetypes/

The archetypes along with an index are published to Maven as a catalog:
https://repo1.maven.org/maven2/io/helidon/archetypes/helidon-archetype-catalog/

As part of the helidon-site build a file called `metadata.properies` is generated
that contains information about versions:

* build-tools.version: Version of `helidon-maven-plugin` to use. Only used by CLI versions 2.0.2 and earlier.
* cli.version: Version of the latest CLI binaries. Used to determine if there is an upgrade available.
* cli.latest.plugin.version: Version of the `helidon-cli-maven-plugin` to use for versions > 2.0.2
  
Note that the last two can be different if, for example, we choose to release
CLI binaries without doing a full build-tools release.

The site build bundles `metadata.properties` along with the archetype catalog into `cli-data.zip`. 

This file is hosted on helidon.io. For example:
https://helidon.io/cli-data/2.2.2/cli-data.zip

## CLI's use of `cli-data.zip`

If needed the CLI downloads `cli-data.zip` and extracts the data into `~/.helidon/cache`.
It now has access to the archetype catalog from the local disk.

### Before versions 3.x

When the cli starts up it determines the version of Helidon to use. The user
might have passed the version, or the CLI might use the latest by
inspecting https://helidon.io/cli-data/latest.

### Since versions 3.x

When the cli starts up it inspects https://helidon.io/cli-data/versions.xml and 
gets information about all available versions of Helidon. After that it proposes to the user the latest 
major versions of Helidon and the default one. User can choose one of them or look at all available 
versions and choose the required one.

## CLI Configuration

Configuration for the cli is stored in `~/.helidon/config`.

