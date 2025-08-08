# Helidon Assembly Maven Plugin Extension

Allows assembly of Helidon artefacts with [Maven Assembly plugin](https://maven.apache.org/plugins/maven-assembly-plugin/)
by providing handler for aggregating service registry files.

Aggregated files:
 * `META-INF/helidon/config-metadata.json`
 * `META-INF/helidon/feature-registry.json`
 * `META-INF/helidon/service-registry.json`
 * `META-INF/helidon/service.loader`
 * `META-INF/helidon/serial-config.properties`

### General usage

Following example shows configuration of the `maven-assembly-plugin` with `helidon-assembly-extension` module
and `helidon` as configured `containerDescriptorHandler`.

#### Plugin configuration in `pom.xml`

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <configuration>
        <descriptors>
            <descriptor>src/main/assembly/assembly.xml</descriptor>
        </descriptors>
        <archive>
            <manifest>
                <mainClass>${mainClass}</mainClass>
            </manifest>
        </archive>
    </configuration>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>single</goal>
            </goals>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>io.helidon.build-tools</groupId>
            <artifactId>helidon-assembly-extension</artifactId>
            <version>${tools.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

#### Assembly descriptor

```xml
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/ASSEMBLY/2.2.0 http://maven.apache.org/xsd/assembly-2.2.0.xsd">
    <id>bundle</id>
    <formats>
        <format>jar</format>
    </formats>
    <includeBaseDirectory>false</includeBaseDirectory>

    <dependencySets>
        <dependencySet>
            <outputDirectory>/</outputDirectory>
            <useProjectArtifact>true</useProjectArtifact>
            <unpack>true</unpack>
            <scope>runtime</scope>
        </dependencySet>
    </dependencySets>

    <containerDescriptorHandlers>
        <containerDescriptorHandler>
            <handlerName>helidon</handlerName>
        </containerDescriptorHandler>
        <containerDescriptorHandler>
            <handlerName>metaInf-services</handlerName>
        </containerDescriptorHandler>
    </containerDescriptorHandlers>

</assembly>
```
