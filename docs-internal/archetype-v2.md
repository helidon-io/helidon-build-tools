# Archetype Engine v2

This document describes the design of Helidon archetype engine V2.

## Table of Contents

 * [Introduction](#introduction)
 * [Archetype scripts](#archetype-scripts)
    * [File Paths](#file-paths)
 * [Flow](#flow)
    * [Input](#input)
    * [Step](#step)
    * [Optional Steps](#optional-steps)
 * [Flow Context](#flow-context)
    * [Flow Context Path](#flow-context-path)
    * [Flow Context Directive](#flow-context-directive)
    * [Flow Context Path Expressions](#flow-context-path-expressions)
    * [External Flow Context Values](#external-flow-context-values)
 * [Help text](#help-text)
 * [Output](#output)
    * [Static files](#static-files)
    * [Templates](#templates)
    * [Transformations](#transformations)
    * [Template model](#template-model)
        * [Pre-processed values](#pre-processed-values)
        * [Inline values](#inline-values)
        * [External values](#external-values)
        * [Merge order](#merge-order)
 * [Build time processing](#build-time-processing)
    * [Maven properties](#maven-properties)
 * [Archive](#archive)
 * [Maven compatibility](#maven-compatibility)
    * [Post generation script](#post-generation-script)
    * [Supported archetypes](#supported-archetypes)
 * [Mock-ups](#mock-ups)
    * [UI wizard](#ui-wizard)
    * [CLI](#cli)

## Introduction

This new version of the archetype engine will provide the low-level archetype support needed for the project
 [starter](https://github.com/batsatt/helidon-build-tools/blob/starter/starter-proposal/starter-proposal.md).

V1 had a concept of input flow that models user inputs, V2 expands that concept into an advanced graph of inputs
 (referred to as "flow) that are logically grouped as steps and eventually infer output files.

V2 archetypes are "scripts" that define the flow, and the V2 archetype engine is a simplistic "interpreter".
 The generation of a new project has two phases:
 - Execute the scripts to resolve the output files
 - Render the output files

V1 has a concept of a catalog that aggregates multiple standalone archetypes, instead V2 uses a "main" archetype
 that encapsulates all possible choices.

Fine-grained "features" can be modeled by creating logical groups of inputs and templates that are re-usable throughout
 the flow. Scripts can be split to fit those features and encapsulate related definitions.

A V2 archetype project is a set of scripts and output files. Helidon will use a single "main" archetype:
 - all "scripts" are co-located, making it easy to maintain
 - all "scripts" are co-located, making it easy to understand
 - all "scripts" are co-located, making it easy to re-use

## Archetype scripts

Archetype scripts are XML descriptors. Scripts are composed with the following set of directives:
 - `<exec>`: execute a script, files are resolved relative to the script
 - `<source>`: execute a script, files are resolved relative to the current directory
 - `<context>`: set a read-only value in the context
 - `<step>`: define a step
 - `<input>`: define an input
 - `<output>`: define output files
 - `<help>`: define rich help text

Since the concepts of V2 are more advanced, the descriptor is more complex and requires more understanding from the
 archetype maintainers. An XML schema will be provided for IDE documentation and auto-completion. See below a skeleton
 of the new XML descriptor:

```xml
<archetype-script>
    <help>
        <!-- rich help for the archetype or invoking element goes here ... -->
    </help>
    <!-- Set a read-only value for foo -->
    <context path="foo">
        <value>bar</value>
    </context>
    <exec src="path-to-script.xml" />
    <source src="path-to-script.xml" />
    <step label="My Step" if="${bar} == 'foo'">
        <help>
            <!-- rich help for the step goes here ... -->
        </help>
        <!-- text input -->
        <input name="your-name" type="text" label="Your Name" prompt="Type your name"/>
    </step>
    <input name="select-items" type="select" label="Select an item" prompt="Please select an item">
        <help>
            <!-- rich help for the step -->
        </help>
        <option value="foo" label="Foo">
            <help>
                <!-- rich help for the selectable option -->
            </help>
            <exec src="path-to-script.xml" />
            <source src="path-to-script.xml" />
            <step label="Nested foo step">
                <!-- ... -->
            </step>
            <input name="nested foo input" type="option" label="Foo option" prompt="Do you want foo option?">
                <!-- ... -->
            </input>
            <output />
        </option>
        <exec src="path-to-script.xml" />
        <source src="path-to-script.xml" />
        <output>
            <!-- ... -->
        </output>
    </input>
    <help>
        <!-- see below -->
    </help>
    <output if="${bar} == 'foo'">
        <transformation id="mustache">
            <replace regex="\.mustache$" replacement=""/>
        </transformation>
        <transformation id="foo">
            <replace regex="\.foo$" replacement="\.bar"/>
        </transformation>
        <templates transformations="mustache" if="" engine="mustache">
            <directory>files</directory>
            <includes>
                <include>**/*.mustache</include>
            </includes>
        </templates>
        <files transformations="foo" if="">
            <directory>files</directory>
            <excludes>
                <exclude>**/*.mustache</exclude>
            </excludes>
        </files>
        <model>
            <value key="bob" if="${bar} == 'foo'">alice</value>
            <list key="names" order="30">
                <map>
                    <value key="foo">bar</value>
                    <value key="bar">foo</value>
                </map>
            <map key="dependencies">
                <value key="groupId">com.example</value>
                <value key="artifactId">my-project</value>
            </map>
        </model>
    </output>
</archetype-script>
```

### File Paths

All paths are relative to their current directory, a leading `/` can be used to refer to the archetype root directory.

## Flow

The flow is the graph of inputs.

### Input

The flow is formed by way of nesting `<input>` elements. `<input>` requires a `name` attribute that must be unique
 among siblings.

An input can be of different types:
- `type="option"`: an option (`true` or `false`) ; opt-in by default but can be declared as opt-out (`default="false"`)
- `type="select"`: pick and choose ; single optional choice by default, but can be required (`required="true"`) and or multiple (`multiple="true"`)
- `type="text"`: text value ; may have a default value (`placeholder="my-default-value"`)

An input can also be declared as hidden. Hidden inputs are not exposed in UIs. The only way to set a value of hidden
 inputs is via preset (`<context>`, CLI options, URL query params etc).

Example of a selection:
```xml
<input name="tracing"
       type="select"
       label="Select a tracing provider">

    <option value="zipkin" label="Zipkin" />
    <option value="jaeger" label="Jaeger" />
</input>
```

Example of an option:
```xml
<input name="kubernetes"
       type="option"
       label="Kubernetes Support"
       prompt="Do you want support for Kubernetes"/>
```

Example of a text value:
```xml
<input name="name"
       type="text"
       label="Project name"
       placeholder="myproject"/>
```

### Step

A step represents a UX pane, or a window that contains a certain set of inputs.
Steps are required by default and can be made optional using the optional attribute (`optional="true`).

```xml
<step label="Application Type">
    <!-- ... -->
</step>
```

### Optional Steps

Optional steps must have inputs with defaults. An optional step with non default inputs is invalid and should be
 validated as an error.

Customization of features can be modeled using an optional step declared as the last child of the step to be
 customized. The order is the declaration order, the enclosing steps must declare the optional step used for
 customization carefully.

E.g.
- `se/se.xml` executes `se/hello-world/hello-world-se.xml` AND THEN executes `common/customize-project.xml`
- `se/hello-world/hello-world-se.xml` executes `se/hello-world/customize-hello-world.xml` as the very last step.

The declaration order for the two customization steps is first `customize-hello-world` and then `customize-project`.

When interpreting the flow, the next steps are always computed. If the next steps are all optional, it is possible to
 skip them and generate the project.

An optional step requires no action other than continuing to the next step (unless this is the very last step).

## Flow Context

The flow context is a tree of resolved inputs that represents the choices. The tree nodes represent choices that
 are a pair of an input path its resolved value.

The context may be populated with various mechanisms:
- `<context>` directive
- query parameters
- CLI option

E.g.
```
|- flavor (select input) // value=se
    |- base (select input) // value=secure-hello-world
        |- media-support (option input) // read-only
            |- provider (select input) // value=[jackson,jaxb] ; read-only
        |- security (option input) // read-only
            |- authentication (option input) // read-only
                |- provider (select input) // value=[basic-auth,digest] ; read-only
        |- hello-phrase (text input) // value="Bonjour Monde"
```

The Java API for the flow context might look like the following:
```java
interface ContextNode {
    String name();
    Optional<ContextValue>();
    List<ContextNode> children();
}
interface ContextValue {
    // Flags this value as declared externally prior to any flow invocation.
    // E.g. passed-in with query parameter or CLI option
    boolean external();
    // Flags this value as set by a <context> directive
    boolean readOnly();
}
interface TextContextValue extends ContextValue {}
interface SelectContextValue extends ContextValue {
    List<SelectionOption> options();
}
interface SelectionOption extends ContextValue {}
```

### Flow Context Path

A path can be used to point at a node in the flow context. Path members are separated with a dot.

E.g.
- `flavor.base`
- `media-support.provider`
- `security.authentication.provider`

If all nodes share a common parent hierarchy, a common prefix can be configured to indicate the prefix of absolute
 paths that may be omitted.

E.g. Consider the following absolute paths with a common prefix `flavor.base`:
```
flavor // resolved as flavor
favor // resolved as flavor.base.favor
flavor.base // resolved as flavor.base
flavor.foo // INVALID
flavor.base.media-type.provider // resolved flavor.base.media-type.provider
security.authentication.provider // resolved flavor.base.security.authentication.provider
```

When used within the descriptors, paths are always relative to their current context, unless they start with
 `ROOT.` or `PARENT.`:
```
${ROOT.flavor.base.security.authentication.provider} // absolute, resolved as flavor.base.security.authentication.provider
${ROOT.security.authentication.provider} // absolute, resolved as flavor.base.security.authentication.provider
${provider} // resolved as flavor.base.security.provider if current is flavor.base
${PARENT.security.provider} // resolved as flavor.base.security.provider if the parent of current is flavor.base
```

Invalid paths must be validated at build time and reported as errors.

### Flow Context Directive

The `<context>` directive can be used to pre-set the value of a given input in the flow context. The values set
 by this directive are flagged as read-only, and the inputs rendered by the UIs will be disabled.

The `path` attribute is used to specify the path of the flow context node whose value is to be set.

The example choices below are effectively "presets".

```xml
<archetype-script>
    <context path="media-support.json.provider"> <!-- example of a single value choice -->
        <value>jackson</value>
    </context>
    <context path="security.authentication.provider"> <!-- example of a multi-value choice (i.e a select with multiple values) -->
        <value>basic-auth</value>
        <value>digest-auth</value>
    </context>
    <context path="health"> <!-- example of opting out of an option that defaults to true -->
        <value>false</value>
    </context>
    <context path="project-name"> <!-- example of a text choice -->
        <value>my-super-project</value>
    </context>
</archetype-script>
```

### Flow Context Path Values

A property like syntax can be used to resolve the value of an input.

E.g. 
- `${media-support.json.provider}`
- `${security.authentication.provider}`
- `${security}`

Inline search and replace regular expressions are also supported:
- `${package/\./\/}`

This can be used to add data in the template data model, or to set default values for other inputs.

### Flow Context Path Expressions

Flow context path expressions are boolean expressions that can be used to query the flow context.

Values can be either flow context path values or literal, and are only of the following types: 
 - Boolean: `true` or `false`
 - Text: `'foo'`
 - Array: `['foo', 'bar']`

The following operators are supported:
 - `&&`: logical AND
 - `||`: logical OR
 - `!`: logical negation
 - `contains`: array contains
 - `==`: equality

```
${security}
${media-support.json.provider} == 'jackson'
${security} && ${media-support}
${security} || ${media-support}
${security.authentication.provider} contains 'basic-auth'
!(${security} && ${media-support}) || ${health}
```

Flow context path expressions are supported in the following elements:
 - anything under `<output if="expr">`
 - `<step if="expr">`

### External Flow Context Values

Values in the flow context can also be set externally using CLI options or URI query parameters. The flow context
 nodes are identified using absolute paths, thus the common prefix can be omitted (`PARENT.` and `ROOT.` are not
 allowed).

When declaring external flow context values, gating input option values can be inferred.
 E.g. `media-support.json.provider=jackson` implies both `media-support=true` and `media-support.json=true`.

Query parameters:
```
?media-support.json.provider=jackson&security.authentication.provider=basic-auth,digest-auth&health=false&project-name=my-super-project
```

Properties:
```properties
media-support.json.provider=jackson
security.authentication.provider=basic-auth,digest
health=false
project-name=my-super-project
```

The `.helidon` file generated with the project will include the user's choices. A prefix need to be used to identify the
 choices properties, e.g. `FLOW.media-support.json.provider`.

CLI options:
```
helidon init \
    --input media-support.provider=jackson \
    --input security.authentication.provider=basic-auth,digest
    --input health=false
    --input project-name=my-super-project
```

## Help text

The step, input and option elements support a `label` attribute that is used for description purpose. Label is meant
 to be inline and short. Larger multi-line description text can be provided with a nested `<help>` element.

The `<help>` element supports a limited markdown format:
 - `**bold text**`
 - `_italic_`
 - paragraphs
 - `` `code` ``
 - `[Links](https://example.com)`

Non standard markdown syntax should follow [kramdown's extension](https://kramdown.gettalong.org/syntax.html#extensions).

For instance, we can add support for colors like this:
 - `{::color-info}This is an info colored text{:/}`

The actual colors would be abstracted away with names that can be implemented with various backends:
 - `primary`
 - `secondary`
 - `accent`
 - `error`
 - `info`
 - `success`
 - `warning`

E.g.
````xml
<step label="Media Type Support">
    <help><![CDATA[
Configure support for a specific Media Type. E.g. **JSON** or **XML**.
This is used to consume requests payload or attach payload to responses.

```java
request.content().as(JsonObject.class).thenAccept(json -> {
    System.output.println(json);
    response.send("OK");
});
```
]]></help>
</step>
````

This requires a basic markdown parser. The text above is converted to simple HTML for the web scenario:

```html
<p>
Configure support for a specific Media Type. E.g. <b>JSON</b> or <b>XML</b>.
This is used to consume requests payload or attach payload to responses.
</p>
<pre><code class="language-java">
request.content().as(JsonObject.class).thenAccept(json -> {
    System.output.println(json);
    response.send("OK");
});
</code></pre>
```

The same text can formatted using ANSI escapes. Code formatting with syntax highlighting is do-able with ANSI,
 using a simplistic approach. See the logic in [JavaHtmlConverter](https://github.com/eclipse-ee4j/glassfish-woodstock/blob/master/woodstock-example/src/main/java/com/sun/webui/jsf/example/util/JavaHtmlConverter.java)
 and [highlight.js](https://github.com/highlightjs/highlight.js/blob/master/src/languages/java.js) for reference.

Help text should be implemented as tooltips. The web UI could show an icon `(?)` next to an input that
 expands a help pane.
 
The CLI could implement help by adding an extra option, or by providing a special input that displays help.
E.g.
```
Helidon flavor
  (1) SE
  (2) MP
  (3) Help
Enter selection (Default: 1): 3
```

```
Use 'help' or 'Help OPTION' to display the help text.

Helidon flavor
  (1) SE
  (2) MP
Enter selection (Default: 1): help
```

## Output

The `<output>` element contains configuration for the files and template to be included in the processing.
It can be declared under the following elements:
- `<archetype-script>`
- `<step>`
- `<input>`
- `<option>`

Output is always global, nesting is used to conditionally add to the output based on choices. The children of `<output>`
 also support choices expressions using the `if` attribute to do the same.

E.g.
```xml
<output>
    <templates if="${kubernetes}">
        <directory>files</directory>
        <sources>
            <source>app.yaml</include>
        </includes>
    </templates>
</output>
```

### Static files

Files can be declared in the output using `<files>`.

```xml
<files>
    <directory>files/src/main/java</directory>
    <sources>
        <source>**/*.java</include>
    </includes>
</files>
```

### Templates

Templates can be declared in the output using `<templates>`. The attribute `engine` defines the template engine
 used to process the templates.

```xml
<templates engine="mustache">
    <directory>files/src/test/java</directory>
    <sources>
        <source>**/*.mustache</include>
    </includes>
</templates>
```

### Transformations

Transformations are used to modify the files paths for included files and templates.

E.g.
- A mustache template `pom.xml.mustache` needs to create a file called `pom.xml`
- A java file need to be expanded with the package name as its directory structure

A transformation is basically a named set of search and replace regular expressions:

```xml
<output>
    <transformation id="packaged">
        <replace regex="__pkg__" replacement="${package/\./\/}"/>
    </transformation>
    <files transformations="packaged">
        <directory>files/src/main/java</directory>
        <sources>
            <source>**/*.java</include>
        </includes>
    </files>
</output>
```

### Template model

The `<model>` element is used to configure the data model for the templates in a given `<output>`.

The following data types are supported: `value`, `list`, `map`. Data entries are declared with a key except for list
 elements. Data entries also support the `if` attributes to express choices expressions.

E.g.
```xml
<model>
    <value key="readme-title">My Project</value>
    <list key="dependencies">
        <map if="${media-support.json.provider} == jackson">
            <value key="groupId">io.helidon.media</value>
            <value key="artifactId">helidon-media-jackson</value>
        </map>
        <map if="${media-support.json.provider} == jsonb">
            <value key="groupId">io.helidon.media</value>
            <value key="artifactId">helidon-media-jsonb</value>
        </map>
        <map if="${media-support.json.provider} == jsonp">
            <value key="groupId">io.helidon.media</value>
            <value key="artifactId">helidon-media-jsonp</value>
        </map>
        <map if="${media-support.xml.provider} == jaxp">
            <value key="groupId">io.helidon.media</value>
            <value key="artifactId">helidon-media-jaxp</value>
        </map>
        <map if="${media-support.xml.provider} == jaxb">
            <value key="groupId">io.helidon.media</value>
            <value key="artifactId">helidon-media-jaxb</value>
        </map>
    </list>
</model>
```

#### Pre-processed values

Values may need to be processed by a template engine. This can be done using the `template` attribute.

```xml
<model>
    <value key="name" template="mustache">{{artifactId}}</value>
</model>
```

#### Inline values

Pre-formatted values can be declared using CDATA.

E.g.
```xml
<model>
    <list id="readme-section">
        <item>
            <![CDATA[
## Build the Docker Image

`docker build -t my-image .`
]]>
        </item>
    </list>
</model>
```

#### External values

Values may be defined in a separate file entirely. The file is specified using the `file` attribute.
 Processing using the `template` attribute can also be used.

E.g.

```xml
<model>
    <list key="config">
        <value file="config.yaml.mustache" template="mustache"/>
    </list>
</model>
```

#### Merge order

The template data model is shared across the flow. Keys are not unique so that inputs from various level can contribute
 to the same data. This means that the data for the same keys needs to be merged.

 The default merge order is based on the declaration order, however the order attribute is provided to indicate how to
  merge a particular element. Lower order is resolved with higher priority.

E.g.
```xml
<model>
    <list id="readme-section">
        <value order="50" template="mustache">
            <![CDATA[
## Build the Docker Image

`docker build -t {{artifactId}} .`
]]>
        </value>
    </list>
</model>
```

## Build time processing

The `helidon-archetype-maven-plugin` exposes configuration that takes an `<archetype-script>` that defines the
 entry-point. The content of the entry-point is written out to a reserved file: `/helidon-archetype.xml`.

The `helidon-archetype-maven-plugin` will also expose configuration for `<archetype-script>`:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.helidon.build-tools.archetype</groupId>
            <artifactId>helidon-archetype-maven-plugin</artifactId>
            <configuration>
                <sourceDirectory>src/main/archetype</sourceDirectory>
                <archetype-script>
                    <exec src="my-entry-point.xml" />
                </archetype-script>
            </configuration>
        </plugin>
    </plugins>
</build>
```

The maven plugin checks that all XML files are valid against the schema, and performs a dry-run execution to:
- enforce optional steps contain only optional inputs
- validate flow context paths
- validate flow context expressions
- detect expressions type mismatch

### Maven properties

Archetype may need to inject values derived from Maven properties, e.g. `${project.version}`. This can be done by
 adding to entry-point script since Maven properties are automatically expanded in plugin configuration.

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.helidon.build-tools.archetype</groupId>
            <artifactId>helidon-archetype-maven-plugin</artifactId>
            <configuration>
                <archetype-script>
                    <output>
                        <model>
                            <value key="helidon.version">${project.version}</value>
                        </model>
                    </output>
                </archetype-script>
            </configuration>
        </plugin>
    </plugins>
</build>
```

With the configuration above, mustache templates can use `{{helidon.version}}` to substitute the value of
 `${project.version}`.

## Archive

The archetype archive is a JAR archive that contains the `src/main/archetype` directory as well the root descriptor
 generated by the `helidon-archetype-maven-plugin` (`helidon-archetype.xml`).

The archive could be optimized in the future to contain serialized objects instead of the XML, this would remove the
 need to parse all descriptors from XML.

## Maven compatibility

### Post generation script

The `maven-archetype-plugin` supports a post generation script: `archetype-post-generate.groovy`.
 Helidon archetypes are empty Maven archetypes that bundle such script to defer the work to the Helidon archetype
 engine.

The class-loader used to execute the groovy script is the plugin class-loader for `mvn archetype:generate`: it has no
 parent class-loader and `ClassLoader.getSystemClassLoader()` is not usable within a groovy script. This means that
 the script has to be standalone and create class-loaders manually.

The Helidon archetypes declares the required dependencies so that `mvn archetype:generate` resolves them transitively
 when resolving the archetype. The post generation script resolves the Maven installation to create a class-loader
 that can invoke `aether` in order to resolve the archetype transitive dependencies and create a class-loader
 that can be used to invoke the Helidon archetype engine.

Since the `aether` API is very sensitive and `aether` is resolved from the Maven installation, the script must validate
 Maven `<= 3.2.5` to ensure a version compatible with the post generation script.

The Helidon engine also requires Java 11, so the script must check and enforce a Java version `>= 11`.

### Supported archetypes

The main archetype will be compatible, however we do want to keep publishing the current set of Maven compatible
 archetypes such as `bare` and `quickstart`. These consist of an entry-point that defines presets and invokes the
 main archetype.

Support for a custom URL handler `mvn://` will be added to reference the main archetype from the local repository:
 - `<exec url="mvn:groupId:artifactId:version/helidon-archetype.xml"/>`

E.g. for `quickstart-se`:
```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.helidon.build-tools.archetype</groupId>
                <artifactId>helidon-archetype-maven-plugin</artifactId>
                <configuration>
                    <archetype-script>
                        <!-- presets for quickstart-se -->
                        <context path="flavor">
                            <value>se</value>
                        </context>
                        <context path="base">
                            <value>quickstart</value>
                        </context>
                        <!-- execute the main archetype -->
                        <exec url="mvn://io.helidon.archetypes:helidon-archetype:${project.version}/helidon-archetype.xml"/>
                    </archetype-script>
                </configuration>
            </plugin>
        </plugins>
    </build>
    <dependencies>
        <dependencies>
            <!-- main archetype -->
            <dependency>
                <groupId>io.helidon.archetypes</groupId>
                <artifactId>helidon-archetype</artifactId>
            </dependency>
            <!-- archetype engine -->
            <dependency>
                <groupId>io.helidon.build-tools.archetype</groupId>
                <artifactId>helidon-archetype-engine</artifactId>
            </dependency>
            <!-- mvn:// URL support -->
            <dependency>
                <groupId>io.helidon.build-tools.archetype</groupId>
                <artifactId>helidon-archetype-maven-url-handler</artifactId>
            </dependency>
        </dependencies>
    </dependencies>
</project>
``` 

## Mock-ups

### UI wizard

- Select:

```
(1) Application Type
 |
 | Select a type of application:
 |  (   ) Bare
 |  ( X ) Hello World
 |  (   ) Database
 |
 | [CONTINUE]
-----------------------------------------
```

- Option gating a step:

```
(2) Kubernetes
 |
 | [  ] Kubernetes support
 |
 | [CONTINUE]
-----------------------------------------
```

Such a step requires no user action. Clicking the check box expands nested inputs:

```
(2) Kubernetes
 |
 | [ x ] Kubernetes support
 |   |- [ x ] add a service
 |   |- [ x ] add an Istio sidecar
 |
 | [CONTINUE]
-----------------------------------------
```

The `GENERATE` button is always visible and is active if there are only optional inputs remaining.

```
(3) Media Support
 |
 | [ x ] Media type support
 |   |- [ x ] JSON
 |        |- [ x ] Jackson
 |        |- [ x ] JSON-B
 |        |- [ x ] JSON-P
 |   |- [ x ] XML
 |        |- [ x ] JAX-B
 |        |- [ x ] JAX-P
 |
 | [CONTINUE]
-----------------------------------------
[GENERATE!]
```

### CLI

```
Helidon flavor
  (1) SE
  (2) MP
Enter selection (Default: 1): 
Select a type of application
  (1) bare | Minimal Helidon SE project suitable to start from scratch 
  (2) quickstart | Sample Helidon SE project that includes multiple REST operations 
  (3) database | Helidon SE application that uses the dbclient API with an in-memory H2 database 
Enter selection (Default: 1):
Do you want to configure media type support (yes/no): yes
Do you want to use JSON (yes/no): yes
Select a JSON provider
  (1) jackson | Jackson
  (2) json-b | JSON Binding
  (3) json-p | JSON Processing
Enter selection (Default: 1):
```

Consider doing a "curses" like UI, see `nano` as an example.