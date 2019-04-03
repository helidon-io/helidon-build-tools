# Helidon Sitegen Maven Plugin

This plugin provides a site generator built on top of AsciiDoctorJ.

* [Goal: generate](#goal-generate)
* [Goal: package](#goal-package)
* [Goal: preprocess-adoc](#goal-preprocess-adoc)
* [Goal: naturalize-adoc](#goal-naturalize-adoc)
* [Site Config File](#site-config-file)
* [Life-cycle Mapping: site](#life-cycle-mapping-site)

## Goal: `generate`

Generates the site files.

### Required Parameters

| Property | Type | User Property | Description |
| --- | --- | --- | --- |
| siteConfigFile | File | helidon.sitegen.siteConfigFile | Site configuration file |

### Optional Parameters

| Property | Type | Default<br/>Value | Description |
| --- | --- | --- | --- |
| siteOutputDirectory | File | `${project.build.directory}/site` | Directory containing the generated site files |
| siteSourceDirectory | File | `${project.basedir}/src/main/site` | Directory containing the site sources |
| siteGenerateSkip | Boolean | `false` | Skip this goal execution |

All parameters are mapped to user properties of the form `sitegen.PROPERTY`.

## Goal: `package`

Creates the site archive.

### Optional Parameters

| Property | Type | Default<br/>Value | Description |
| --- | --- | --- | --- |
| siteArchiveOutputDirectory | File | `${project.build.directory}` | Directory containing the generated JAR |
| siteOutputDirectory | File | `${project.build.directory}/site` | Directory containing the generate site files to archive. |
| siteArchiveFinalName | String | `${project.build.finalName}` | Name of the generated JAR |
| siteArchiveIncludes | List | [] | List of files to include |
| siteArchiveExcludes | List | [] |List of files to exclude |
| siteArchiveSkip | Boolean | `false` | Skip this goal execution |

The parameter `siteArchiveSkip` is mapped to a user property:
 `sitegen.siteArchiveSkip`.

## Goal: `preprocess-adoc`

Pre-includes included text specified by AsciiDoc `include::` directives into
 an `.adoc` file, adding AsciiDoc comments to track where each snippet of
 included content is in the updated file and where it came from.

### Optional Parameters

| Property | Type | Default<br/>Value | Description |
| --- | --- | --- | --- |
| inputDirectory | File | `${project.basedir}` | Directory containing the files to be processed |
| outputDirectory| File | `${project.basedir}` | Directory where the reformatted `.adoc` file should be written |
| checkPreprocess | Boolean | `false` | Check that the input and output files are the same |
| includes | List | [] | List of files to include |
| exclude | List | [] | List of files to exclude |

All parameters are mapped to user properties of the form `sitegen.PROPERTY`.

## Goal: `naturalize-adoc`

Converts a preprocessed `.adoc` file back into natural form with conventional
 `include::` directives.

### Optional Parameters

| Property | Type | Default<br/>Value | Description |
| --- | --- | --- | --- |
| inputDirectory | File | `${project.basedir}` | Directory containing the files to be processed |
| outputDirectory| File | `${project.basedir}` | Directory where the reformatted `.adoc` file should be written |
| includes | List | [] | List of files to include |
| exclude | List | [] | List of files to exclude |

All parameters are mapped to user properties of the form `sitegen.PROPERTY`.

## Site Config File

The site configuration file is used to configure the following:
* Asciidoctor engine: add libraries, declare attributes etc
* Static assets: specify static files to be added to the output directory
* Pages: define matching rules for the pages
* Backend: define the backend configuration

The backend is responsible for the rendering, there are 2 available choices:
* `basic`
* `vuetify`

```yaml
engine:
  asciidoctor:
    images-dir: String
    libraries:
      - String
    attributes:
      key: String
assets:
  - target: String
    includes:
      - String # match pattern
    excludes:
      - String # match pattern
header:
  favicon:
    path: String
    type: String
  stylesheets:
    # local
    - path: String
      type: String
    # remote
    - url: String
      type: String
  scripts:
    # local
    - path: String
      type: String
    # remote
    - url: String
      type: String
  meta:
    key: String
pages:
  - includes:
      - String # match pattern
backend:
  name: String
  # see backend specific configuration below
```

### Match Patterns

A match pattern is a string representing file path segments that may contains
 wildcard `*` to match multiple files at once. 

`*` matches zero or more characters within a path segment, `**` matches zero or
 more characters across path segments.

For example:
* `**/*.adoc` will match nested files ending with `.adoc` at any depth
* `docs/*.adoc` will match files ending with `.adoc` at depth 1

### Basic Backend

The `basic` backend generates static HTML output. It is used mostly for testing.

### Vuetify Backend

The `vuetify` backend generates a single page application using
 https://vuetifyjs.com.

#### Configuration

The backend configuration has the following options:

```yaml
    homePage: String # exact path to a .adoc file
    releases:
        # List of version displayed at the top of the navigation
        - String
    navigation:
      glyph: # image or icon displayed at the top of the navigation
        type: String # "image" or "icon"
        value: String
      items:
        # navigation groups
        - title: String
          items:
            # navigation group item that matches pages
            - title: String
              pathprefix: String # must match path prefix of included pages
              glyph: # image or icon for this item
                type: String # "image" or "icon"
                value: String
              items:
                - includes:
                    - String # match pattern
                - excludes:
                    - String # match pattern
            # navigation group item with an external link
            - title: String
              glyph: # image or icon for this item
                type: String # "image" or "icon"
                value: String
              href: String
```

The value for glyph of type "icon" must be the name of an icon from one of the
 following icon libraries:
* https://material.io/tools/icons
* https://fontawesome.com/icons (add the `fa-` prefix)

## Life-cycle Mapping: `site`

The plugin provides a custom mapping `site`. It is associated with the `.jar`
 file extension and maps the following plugin executions:

* `process-resources`: org.apache.maven.plugins:maven-resources-plugin:resources
* `compile`: io.helidon.build-tools:sitegen-maven-plugin:generate
* `package`: io.helidon.build-tools:sitegen-maven-plugin:package
* `install`: org.apache.maven.plugins:maven-install-plugin:install
* `deploy`: org.apache.maven.plugins:maven-deploy-plugin:deploy

You need to declare the plugin as an extension in order to use it:

```xml
<project>
    <groupId>com.example</groupId>
    <artifactId>example-site</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>site</packaging>
    <build>
        <plugins>
            <plugin>
                <groupId>io.helidon.build-tools</groupId>
                <artifactId>sitegen-maven-plugin</artifactId>
                <extensions>true</extensions>
            </plugin>
        </plugins>
    <build>
</project>
```