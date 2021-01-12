# Archetype Engine v2

This document describes the design of Helidon archetype engine V2.

## Table of Contents

 * [Archetype Engine v2](#archetype-engine-v2)
 * [Introduction](#introduction)
 * [Descriptor](#descriptor)
 * [Decoupling descriptors](#decoupling-descriptors)
 * [Flow Input](#flow-input)
 * [Flow Step](#flow-step)
 * [Optional steps](#optional-steps)
 * [Continue action](#continue-action)
 * [Choices graph](#choices-graph)
 * [Choices path](#choices-path)
 * [Choice](#choice)
 * [Choices expressions](#choices-expressions)
 * [Choices intersection](#choices-intersection)
 * [Choices mapping](#choices-mapping)
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
 graph of choices that reflects the current answers to the inputs. Files and templates can be scoped at any level of
 the inputs graph, and can also use conditional expressions that query the choices graph.

V2 enables the modeling of fined grained "features" by creating logical groups of user inputs and templates that can be
 re-used in throughout the inputs graph.

V1 has a concept of a catalog that aggregates multiple standalone archetypes, instead V2 uses a mono archetype
 that encapsulates all possible choices.

The mono archetype is a single project, which provides significant benefits:
- easier to maintain, since all files are co-located
- easier to understand
- easier for sharing across the inputs graph

## Descriptor

V2 will also use an XML descriptor, it may look similar to the V1 descriptors however it is completely different and
 incompatible. The top-level element is changed to reflect that.

Since the concept of V2 are more advanced, the descriptor is more complex and requires more understanding from the
 archetype maintainers. To further allow for logical grouping of features, descriptors can be broken up and "included".

An XML schema will be provided for IDE documentation and auto-completion. Some parts of the descriptors are designed
 specifically so that the schema can indicate what elements can be used.

See below a skeleton of the new XML descriptor:

```xml
<archetype-flow xmlns="https://helidon.io/archetype/2.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="https://helidon.io/archetype/2.0 https://helidon.io/xsd/archetype-flow-2.0.xsd">

    <choice flow="">
        <option value="" />
    </choice>
    <invoke-flow src="" />
    <include src="" />
    <flow-step label="" if="">
        <flow-input />
        <output />
    </flow-step>
    <flow-input name="" type="" label="" prompt="">
        <option value="" label="">
            <invoke-flow src="" />
            <include src="" />
            <flow-step />
            <flow-input />
            <output />
        </option>
        <invoke-flow src="" />
        <include src="" />
        <output />
    </flow-input>
    <output if="">
        <transformation id="" if="">
            <replace regex="" replacement=""/>
        </transformation>
        <templates transformations="" if="">
            <directory></directory>
            <includes>
                <include></include>
            </includes>
        </templates>
        <templates transformations="" if="">
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

## Decoupling descriptors

Two directives elements are provided to decouple descriptors:
 - `<invoke-flow src="path-to-xml" />`
 - `<include src="path-to-xml" />`
 
The value of the path attribute is always relative to the current descriptor.

`<invoke-flow>` invokes the flow declared in a separate descriptor within the current flow step/input.
 It effectively changes the current working directory to the directory that contains the invoked xml file.
All references are resolved relative to their own directory.

`<include>` allows to re-use an XML file in a different working directory.
It includes all non flow-* elements in the current context.

Both directives can be used inside the following elements
 - `<archetype-flow>`
 - `<flow-step>`
 - `<flow-input>`
 - `<option>`

## Flow Input

The inputs graph is a DAG (direct acyclic graph) formed by way of nesting `<flow-input>` elements. `<flow-input>`
 requires a `name` attribute that must be unique among the children of the parent `<flow-input>` element.

An input can be of different types:
- option: opt-in by default, but can be declared as opt-out
- select: single optional choice by default, but can be required and or multiple
- text: text value

Example of a selection:
```xml
<flow-input name="flavor"
            type="select"
            label="Select a flavor">

    <option value="se" label="Helidon SE" />
    <option value="mp" label="Helidon MP" />
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

## Flow Step

A flow step represents a UX pane or windows that contains certain set of inputs.
Steps are required by default and can be made optional using the optional attribute (`optional="true`).

```xml
<flow-step label="Application Type">
    <!-- ... -->
</flow-step>
```

### Optional steps

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

### Continue action

If there is any "next" step, a `CONTINUE` action is provided to navigate to the next step.
If the step is non optional and has non defaulted inputs, the `CONTINUE` action is disabled (greyed out), and enabled
 when the inputs have been filled.
When the current step is optional, the `CONTINUE` action is available and can be used to skip.

## Choices graph

The choices graph represents the current choices made by the user. It is maintained and evolves as the user makes
 choices.

E.g. Parts of the tree may be removed if a user changes a previous choice.

```
|- flavor=se
    |- base=bare
        |- media-support.provider
             |- jackson
        |- security
            |- authentication.provider
                |- basic-auth
```

## Choices path

A path can be used to point at a node in the choices graph. Path members are separated with a `.`.

E.g.
- `flavor.base`
- `media-support.provider`
- `security.authentication.provider`

If all nodes share common parent nodes, then the path of these common nodes is optional when expressing a path.
E.g. In the context of the root node, with a common ancestor `flavor.base`, both are equivalent:
- `flavor.base.media-support.provider`
- `media-support.provider`

Paths are always relative to their current context, unless they start with `...` or `..`:
- `...` refers to the root
- `..` refers to the parent node

E.g. If the context is `flavor.base.security.authentication` and the common ancestor is `flavor.base`, the paths below
 are equivalent:
- `${...flavor.base.security.authentication.provider}`
- `${..security.provider}`
- `${provider}`

Invalid paths should be validated at build time and reported as errors.

## Choice

Choices can be used to pre-fill the choices graph before flow resolution. When resolving flow, entries that exist in
 the choices graph will be skipped, those are immutable and hidden.

Choices can also be passed to the archetype engine "manually", e.g. batch cli, URI query param.

The `flow` attribute is used to specify the path in the choices graph to be filled.

The example choices below are effectively "presets".

```xml
<archetype-flow>
    <choice flow="media-support.json.provider"> <!-- example of a single value choice -->
        <option value="jackson" />
    </choice>
    <choice flow="security.authentication.provider"> <!-- example of a multi-value choice (i.e a select with multiple values) -->
        <option value="basic-auth" />
        <option value="digest-auth" />
    </choice>
    <choice flow="health"> <!-- example of opting out of an option that defaults to true -->
        <option value="false" />
    </choice>
    <choice flow="project-name">my-super-project</choice> <!-- example of a text choice -->
</archetype-flow>
```

## Choices expressions

Choices expression are boolean expressions that can be used to query the choices graph.

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

The operator `!` to negate sub expressions, parenthesis can be used to group expressions:
- `!(${security} && ${media-support}) || ${health}`

Choices expressions are supported in the following elements:
- anything under `<output if="expr">`
- `<flow-step if="expr">`

## Choices intersection

The intersections different parts of the choices graph can be done by using choices expressions on a step.

```xml
<flow-step label="Jersey Security" if="${security}">
    <flow-input name="username"
                type="text"
                label="Username"
                prompt="What username do you want to use"
                placeholder="myuser">
    </flow-input>
</flow-step>
```

### Choices mapping

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
 choices properties, e.g. `input.media-support.json.provider`.

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
Use 'help OPTION' to display the help of any option.

Helidon flavor
  (1) SE
  (2) MP
Enter selection (Default: 1): help 1
```

## Output

The `<output>` element contains configuration for the files and template to be included in the processing.
It can be declared under the following elements:
- `archetype-flow`
- `flow-step`
- `flow-input`
- `option`

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
- A java files needs to be expanded with the package name as its directory structure

A transformation is basically is a named search and replace regular expressions:

```xml
<output>
    <transformation id="packaged">
        <replace regex="__pkg__" replacement="${package@\.@\/}"/>
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
                    <invoke-flow src="my-entry-point.xml" />
                </archetype-flow>
            </configuration>
        </plugin>
    </plugins>
</build>
```

The `<archetype-flow>` element defined in the plugin configuration is required and is effectively the root descriptor.
 It must either have all the required declarations inline or invoke a separate flow descriptor with `<invoke-flow>`.

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

In V1 each standalone archetype was Maven compatible, by way of the `archetype-post-generate.groovy` doing the
 following:
 - check and enforce Maven version
 - check and enforce Java version
 - resolve the Maven installation to use aether
 - use `aether` to resolve the Helidon archetype engine
 - invoke the archetype

In V2 these standalone Helidon archetypes do not exist, instead they are implemented as a top-level choice in the mono
 archetype. In order to keep supporting existing Maven archetypes we will re-create the corresponding artifacts but
 this time dedicated for the Maven use-case.

In V1 since there was a concern about exposing dependencies needed for the Maven use-case only, so both the Helidon
 archetype engine and aether were not declared as dependencies. Instead, `aether` was resolved from the Maven
 installation, and the engine was resolved with `aether`.

Since these old-new artifacts will be dedicated to Maven we can add those dependencies and simplify the groovy logic:
 - check and enforce Maven version
 - check and enforce Java version
 - invoke the engine with groovy

Each Maven compatible archetype is basically a root descriptor that defines presets and invokes the flow of the mono
 archetype. The `<invoke-flow>` directory will support a `url` attribute, and a custom scheme for Maven will be
 provided `mvn://`.

E.g. for `quickstart-se`:
```xml
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>io.helidon.build-tools.archetype</groupId>
                <artifactId>helidon-archetype-maven-plugin</artifactId>
                <configuration>
                    <!-- presets for quickstart-se -->
                    <archetype-flow>
                        <choices>
                            <choice flow="flavor"><option value="se" /></choice>
                            <choice flow="base"><option value="quickstart" /></choice>
                        </choices>
                        <invoke-flow url="mvn://io.helidon.archetypes:helidon-archetype/helidon-archetype.xml"/>
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

Note that it is better to declare the dependencies in the pom rather than resolving them programmatically so that
 they are resolved by Maven before invoking the maven archetype logic.

## Mock-ups

### UI wizard

```
(1) Application Type
 |
 | Select a type of application:
 |  ( ) Bare
 |  ( ) Hello World
 |  ( ) Database
 |
 | [CONTINUE]
-----------------------------------------
(2) Kubernetes
 |
 | [ ] Kubernetes support
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