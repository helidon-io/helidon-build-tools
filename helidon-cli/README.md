# Helidon CLI

* **impl**: implementation of the Helidon CLI. Uses `harness`, `codegen` and `plugin`
* **harness**: a general purpose CLI harness
* **codegen**: annotation processor for `harness`
* **plugin**: code that would normally be in `impl`, but to reduce native image bloat
  it is included as a jar in the native image and executed by extracting
  the jar on disk and spawning a java process.

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

In addition to cli-data, the site release also creates a `latest` file
containing the latest Helidon version number:
https://helidon.io/cli-data/latest

## CLI's use of `cli-data.zip`

When the cli starts up it determines the version of Helidon to use. The user
might have passed the version, or the CLI might use the latest by 
inspecting https://helidon.io/cli-data/latest.

If needed the CLI downloads `cli-data.zip` and extracts the data into `~/.helidon/cache`.
It now has access to the archetype catalog from the local disk.

## CLI Configuration

Configuration fort he cli is stored in `~/.helidon/config`.


