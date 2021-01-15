# Archetype Engine v2

This document describes the design of Helidon archetype engine V2.

## Table of Contents

 * [Introduction](#introduction)
 * [Descriptor](#descriptor)
      * [Decoupling descriptors](#decoupling-descriptors)
 * [Flow](#flow)
      * [Flow Input](#flow-input)
      * [Flow Step](#flow-step)
      * [Optional Flow Steps](#optional-flow-steps)
      * [Continue Action](#continue-action)
 * [Flow Context](#flow-context)
      * [Flow Context Path](#flow-context-path)
      * [Flow Context Directive](#flow-context-directive)
      * [Flow Context Path Expressions](#flow-context-path-expressions)
      * [Choices intersection](#choices-intersection)
      * [External flow context values](#external-flow-context-values)
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
 * [Mock-ups](#mock-ups)
      * [UI wizard](#ui-wizard)
      * [CLI](#cli)

## Introduction

This new version of the archetype engine will provide the low-level archetype support needed for the project
 [starter](https://github.com/batsatt/helidon-build-tools/blob/starter/starter-proposal/starter-proposal.md).

V1 had a concept of input flow that models user inputs.

V2 expands that concept into an advanced graph of inputs, logically grouped as steps. The inputs graph is mirrored by a
 graph of choices that reflects the current answers to the inputs. Output files can be scoped at any level of the
 inputs graph (the "flow") and can also use conditional expressions that query the choices graph.

V2 enables the modeling of fine-grained "features" by creating logical groups of user inputs and templates that can be
 re-used throughout the inputs graph.

V1 has a concept of a catalog that aggregates multiple standalone archetypes, instead V2 uses a single archetype
 that encapsulates all possible choices.

The V2 archetype is a single project, which provides significant benefits:
- easier to maintain, since all files are co-located
- easier to understand
- easier for sharing across the inputs graph

## Descriptor

V2 will also use an XML descriptor, it may look similar to the V1 descriptors however it is completely different and
 incompatible. The top-level element is changed to reflect that.

Since the concepts of V2 are more advanced, the descriptor is more complex and requires more understanding from the
 archetype maintainers. Descriptors can be broken up and "included" to allow for reuse and logical grouping of features.

An XML schema will be provided for IDE documentation and auto-completion. Some parts of the descriptors are designed
 specifically so that the schema can indicate what elements can be used.

See below a skeleton of the new XML descriptor:

```xml
<archetype-flow xmlns="https://helidon.io/archetype/2.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="https://helidon.io/archetype/2.0 https://helidon.io/xsd/archetype-flow-2.0.xsd">

    <flow-context path="">
        <value></value>
    </flow-context>
    <invoke src="" />
    <include src="" />
    <flow-step label="" if="">
        <flow-input />
        <output />
    </flow-step>
    <flow-input name="" type="" label="" prompt="">
        <option value="" label="">
            <invoke src="" />
            <include src="" />
            <flow-step />
            <flow-input />
            <output />
        </option>
        <invoke src="" />
        <include src="" />
        <output />
    </flow-input>
    <output if="">
        <transformation id="" if="">
            <replace regex="" replacement=""/>
        </transformation>
        <templates transformations="" if="" engine="">
            <directory></directory>
            <includes>
                <include></include>
            </includes>
        </templates>
        <files transformations="" if="">
            <directory></directory>
            <excludes>
                <exclude></exclude>
            </excludes>
        </files>
        <model if="">
            <value key="" order="" if=""></value>
            <list key="" order="" if="">
                <map order="" if="">
                    <value key=""></value>
                </map>
            <map key="" order="" if="">
                <value key=""></value>
            </map>
        </model>
    </output>
</archetype-flow>
```

### Decoupling descriptors

Two directive elements are provided to decouple descriptors:
 - `<invoke src="path-to-xml" />`
 - `<include src="path-to-xml" />`

The value of the `src` attribute is a path reference in the archetype directory structure.
Paths are relative to their current directory, `/` can be used to refer to the archetype root directory.

Descriptors are composed of directives and associated `<output>` XML elements.

The `<flow-*>` XML elements ("flow nodes") are input directives:
 - `<flow-context>`
 - `<flow-step>`
 - `<flow-input>`

`<include>` is a directive to use the `<output>` element declared in a separate descriptor within the current
 working directory.

`<invoke>` is a directive to invoke all directives of a separate descriptor in its own directory.

## Flow

### Flow Input

The inputs graph is a DAG (direct acyclic graph) formed by way of nesting `<flow-input>` elements. `<flow-input>`
 requires a `name` attribute that must be unique among the children of the parent `<flow-input>` element.

An input can be of different types:
- option: opt-in by default, but can be declared as opt-out
- select: single optional choice by default, but can be required and or multiple
- text: text value

An input can also be declared as hidden. Hidden inputs are not exposed in UIs. The only way to set a value of hidden
 inputs is via preset (`<flow-context>`, CLI options, URL query params etc).

Example of a selection:
```xml
<flow-input name="tracing"
            type="select"
            label="Select a tracing provider">

    <option value="zipkin" label="Zipkin" />
    <option value="jaeger" label="Jaeger" />
</flow-input>
```

Example of an option:
```xml
<flow-input name="kubernetes"
            type="option"
            label="Kubernetes Support"
            prompt="Do you want support for Kubernetes"/>
```

Example of a text value:
```xml
<flow-input name="name"
            type="text"
            label="Project name"
            placeholder="myproject"/>
```

### Flow Step

A flow step represents a UX pane or windows that contains certain set of inputs.
Steps are required by default and can be made optional using the optional attribute (`optional="true`).

```xml
<flow-step label="Application Type">
    <!-- ... -->
</flow-step>
```

### Optional Flow Steps

Optional steps must have inputs with defaults. An optional step with non default inputs is invalid and should be
 validated as an error.

Customization of features can be modeled using an optional step declared as the last child of the step to be
 customized. The order is the declaration order, the enclosing steps must declare the optional step used for
 customization carefully.

E.g.
- `se/se.xml` invokes `se/hello-world/hello-world-se.xml` AND THEN invokes `common/customize-project.xml`
- `se/hello-world/hello-world-se.xml` invokes `se/hello-world/customize-hello-world.xml` as the very last step.

The declaration order for the two customization steps is first hello-world and then project.

When resolving flow nodes, the next steps are always computed. If next steps are all optional, it must possible to
 skip them and generate the project.

An optional step requires no action other than continuing to the next step (unless this is the very last step).

### Continue Action

If there is any "next" step, a `CONTINUE` action is provided to navigate to the next step.
If the step is non optional and has non defaulted inputs, the `CONTINUE` action is disabled (greyed out), and enabled
 when the inputs have been filled.
When the current step is optional, the `CONTINUE` action is available and can be used to skip.

## Flow Context

The flow context is a tree of resolved inputs that represents the choices. The tree nodes represent choices that
 are a pair of an input path its resolved value.

The context may be populated with various mechanisms:
- `<flow-context>` directive
- query parameters
- CLI option

E.g.
```
|- flavor (select input) // value=se
    |- base (select input) // value=secure-hello-world
        |- media-support (option input) // readonly
            |- provider (select input) // value=[jackson,jaxb] ; readonly=true
        |- security (option input) // readonly
            |- authentication (option input) // readonly
                |- provider (select input) // value=[basic-auth,digest] ; readonly=true
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
    // Flags this value as set by a <flow-context> directive
    boolean readOnly();
}
interface TextContextValue extends ContextValue {}
interface SelectContextValue extends ContextValue {
    List<SelectionOption> options();
}
interface SelectionOption extends ContextValue {}
```

### Flow Context Path

A path can be used to point at a node in the flow context. Path members are separated with a `.`.

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
${ROOT.flavor.base.security.authentication.provider} // absolute!! resolved as flavor.base.security.authentication.provider
${ROOT.security.authentication.provider} // absolute!! resolved as flavor.base.security.authentication.provider
${PARENT.security.provider} // resolved as $(current.parent).security.provider
${PARENT.PARENT.security.provider} // resolved as $(current.parent.parent).security.provider
${provider} // resolved as $(current).provider
```

Invalid paths must be validated at build time and reported as errors.

### Flow Context Directive

The `<flow-context>` can be used to pre-set choice, that is to say a for a given input in the flow context. The
 values set by this directive are flagged as read-only, and the inputs rendered by the UIs will be disabled.

The `path` attribute is used to specify the path of the flow context node whose value is to be set.

The example choices below are effectively "presets".

```xml
<archetype-flow>
    <flow-context path="media-support.json.provider"> <!-- example of a single value choice -->
        <value>jackson</value>
    </flow-context>
    <flow-context path="security.authentication.provider"> <!-- example of a multi-value choice (i.e a select with multiple values) -->
        <value>basic-auth</value>
        <value>digest-auth</value>
    </flow-context>
    <flow-context path="health"> <!-- example of opting out of an option that defaults to true -->
        <value>false</value>
    </flow-context>
    <flow-context path="project-name"> <!-- example of a text choice -->
        <value>my-super-project</value>
    </flow-context>
</archetype-flow>
```

### Flow Context Path Expressions

Flow context path expressions are boolean expressions that can be used to query the flow context.

Operands can use `${}` to specify a path in the choices graph.
E.g. 
- `${media-support.json.provider}`
- `${security.authentication.provider}`
- `${security}`

Inline search and replace regular expressions are also supported:
- `${package/\./\/}`

At build time, the expressions are parsed and validated:
- paths must be valid in the current context
- operators must be valid (E.g. `== true` is invalid be used on a text option or select option)

An expression with a single operand will test if an input is set in the choices graph, regardless of the type of input:
- `${security}`

The operator `==` can be used to test equality:
- `${security} == true`
- `${media-support.json.provider} == jackson`

The operator `!=` can be used to test equality:
- `${media-support.json.provider} != jackson`

The operators `&&` and `||` can be used for logical AND / OR:
- `${security} && ${media-support}`
- `${security} || ${media-support}`

The operator `contains` can be used to test if a multiple select contains a value:
- `${security.authentication.provider} contains basic-auth`

The operator `!` can be used to negate sub expressions, parenthesis can be used to group expressions:
- `!(${security} && ${media-support}) || ${health}`

Choices expressions are supported in the following elements:
- anything under `<output if="expr">`
- `<flow-step if="expr">`

### External flow context values

Values in the flow context can also be set externally using CLI options or URI query parameters. The flow context
 nodes are identified using absolute paths, thus the common prefix can be omitted (`PARENT.` and `ROOT.` are not allowed).

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
 to be inline and short. Larger, multi-line description text can be provided with a nested `<help>` element.

The `<help>` element supports a limited markdown format:
- `**bold text**`
- `_italic_`
- paragraphs
- `` `code` ``
- `[Links](https://example.com)`

We can expand the markdown support over time to provide more rich text features e.g. add support for colors:
- primary
- secondary
- accent
- error
- info
- success
- warning

E.g.
````xml
<flow-step label="Media Type Support">
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
</flow-step>
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
- `<archetype-flow>`
- `<flow-step>`
- `<flow-input>`
- `<option>`

Output is always global, nesting is used to conditionally add to the output based on choices. The children of `<output>`
 also support choices expressions using the `if` attribute to do the same.

E.g.
```xml
<output>
    <templates if="${kubernetes}">
        <directory>files</directory>
        <includes>
            <include>app.yaml</include>
        </includes>
    </templates>
</output>
```

### Static files

Files can be declared in the output using `<files>`.

```xml
<files>
    <directory>files/src/main/java</directory>
    <includes>
        <include>**/*.java</include>
    </includes>
</files>
```

### Templates

Templates can be declared in the output using `<templates>`. The attribute `engine` defines the template engine
 used to process the templates.

```xml
<templates engine="mustache">
    <directory>files/src/test/java</directory>
    <includes>
        <include>**/*.mustache</include>
    </includes>
</templates>
```

### Transformations

Transformations are used to modify the files paths for included files and templates.

E.g.
- A mustache template `pom.xml.mustache` needs to create a file called `pom.xml.mustache
- A java file need to be expanded with the package name as its directory structure

A transformation is basically a named search and replace regular expressions:

```xml
<output>
    <transformation id="packaged">
        <replace regex="__pkg__" replacement="${package/\./\/}"/>
    </transformation>
    <files transformations="packaged">
        <directory>files/src/main/java</directory>
        <includes>
            <include>**/*.java</include>
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

Keys are not unique, data for a given key can be declared at different levels and is effectively merged. The default
 order is based on the declaration order, but it can also be controlled using the `order` attribute.

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

The archetype is processed at build time to validate the descriptors:
- optional steps contain only optional inputs
- expressions are valid
- choices paths are valid

The `helidon-archetype-maven-plugin` will also expose configuration for `<archetype-flow>`:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.helidon.build-tools.archetype</groupId>
            <artifactId>helidon-archetype-maven-plugin</artifactId>
            <configuration>
                <sourceDirectory>src/main/archetype</sourceDirectory>
                <archetype-flow>
                    <invoke src="my-entry-point.xml" />
                </archetype-flow>
            </configuration>
        </plugin>
    </plugins>
</build>
```

The `<archetype-flow>` element defined in the plugin configuration is required and is effectively the root descriptor.
 It must either have all the required declarations inline or invoke a separate flow descriptor with `<invoke>`.

A reserved archive entry name is used to store the root descriptor: `helidon-archetype.xml`.

### Maven properties

Archetype may need to inject values derived from Maven properties, E.g. `${project.version}`. This can be
 done by adding to root descriptor since Maven properties are automatically expanded in plugin configuration.

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.helidon.build-tools.archetype</groupId>
            <artifactId>helidon-archetype-maven-plugin</artifactId>
            <configuration>
                <archetype-flow>
                    <output>
                        <model>
                            <value key="helidon.version">${project.version}</value>
                        </model>
                    </output>
                </archetype-flow>
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

Archetypes are made compatible by providing a `archetype-post-generate.groovy` and doing the following:
 - check and enforce Maven version
 - check and enforce Java version
 - resolve the Maven installation to use aether
 - use `aether` to resolve the Helidon archetype engine
 - invoke the archetype

The new V2 archetype will be compatible, however there is no project to contains the equivalent to the old V1 compatible
 archetypes. We will create projects for the archetypes that we want to keep supporting. Each of these is basically a
 root descriptor that defines presets and invokes the flow of the V2 archetype.

We only want to preserve the current set of Maven compatible archetypes (i.e `quickstart`, `bare` etc), and point at the
 new V2 archetype for anything new.

We should avoid doing any Maven artifact resolution as part of `archetype-post-generate.groovy` executed by the
 groovy script, and instead let Maven resolve the transitive dependencies as part of `mvn archetype:generate`. We can
 use `aether` as part of `archetype-post-generate.groovy` to only resolve from the local repository.

Note that the class-loader of the groovy script does not contain anything, and does not have a parent: this is why
 we have to bootstrap `aether` from the Maven installation.

Support for a custom URL handler `mvn://` will be added to allow root descriptors to reference the V2 archetype from
 the local repository:
 - `<invoke url="mvn:groupId:artifactId:version/helidon-archetype.xml`

E.g. for `quickstart-se`:
```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.helidon.build-tools.archetype</groupId>
                <artifactId>helidon-archetype-maven-plugin</artifactId>
                <configuration>
                    <archetype-flow>
                        <!-- presets for quickstart-se -->
                        <flow-context path="flavor"><value>se</value></flow-context>
                        <flow-context path="base"><value>quickstart</value></flow-context>
                        <!-- invoke the actual V2 archetype -->
                        <invoke url="mvn://io.helidon.archetypes:helidon-archetype:${project.version}/helidon-archetype.xml"/>
                    </archetype-flow>
                </configuration>
            </plugin>
        </plugins>
    </build>
    <dependencies>
        <dependencies>
            <!-- V2 mono archetype -->
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
                <artifactId>helidon-archetype-maven-url</artifactId>
            </dependency>
        </dependencies>
    </dependencies>
</project>
``` 

## Mock-ups

### UI wizard

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
(2) Kubernetes
 |
 | [ X ] Kubernetes support
 |   |- [ x ] add a service
 |   |- [ x ] add an Istio sidecar
 |
 | [CONTINUE]
-----------------------------------------
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
(4) Customize Project
 |
 |   Project name [ my-project ]
 |   Project groupId [ com.example ]
 |   Project artifactId [ my-project ]
 |   Project version [ 1.0-SNAPSHOT ]
 |   Java package [ com.example.myproject ]
 |
-----------------------------------------
[TRY IT!]
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