# Helidon SnakeYAML Helper Maven Plugin

This plugin generates code that gives SnakeYAML additional information for parsing JSON and YAML that is unavailable 
from compiled bytecode at runtime.

* [Goal: generate](#goal-generate
## Goal: `generate`

Generates a class containing SnakeYAML helper code for particular interfaces to be parsed

### Required Parameters

| Property | Type | User Property | Description |
| --- | --- | --- | --- |
| `outputDirectory` | `File` | `snakeyamlgen.outputDirectory` | Directory to contain the generated source file |
| `outputClass` | `String` | `snakeyamlgen.outputClass` | Fully-qualfied class name for the generated class

### Optional Parameters

| Property | Type | Default<br/>Value | Description |
| --- | --- | --- | --- |
| `interfacesConfig` | `CompilerConfig` (see below) | `**/*.java` from `${project.build.dir}/interfaces` | Where to find interface classes to analyze
| `implementationsConfig` | `CompilerConfig` (see below) | `**/*.java` from `${project.build.dir}/implementations` | Where to find implementation classes to analyze
| `debug` | `boolean` | `false` | turns on debug output from the plug-in 

All parameters are mapped to user properties of the form `snakeyamlgen.PROPERTY`.

### `CompilerConfig`
Describes which Java classes to compile for analysis as either interfaces or implementations.

`inputDirectory` - directory from which to read the `.java` files
`includes` - zero or more expressions selecting files to process (default: `**/*.java`)
`excludes` - zero or more expressions selecting files to exclude from processing
  
## Life-cycle Mapping: `generate-sources`

The `generate` goal runs by default during `generate-sources`. 

