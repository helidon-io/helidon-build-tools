# Helidon Shade Maven Plugin Extensions

Allows shading Helidon artefacts with[ Maven Shade plugin](https://maven.apache.org/plugins/maven-shade-plugin/) 
by providing transformer for aggregating service registry files.

Aggregated files:
 * `META-INF/helidon/service-registry.json`
 * `META-INF/helidon/config-metadata.json`
 * `META-INF/helidon/service.loader`

### General usage

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.1</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <transformers>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                    <transformer implementation="io.helidon.shade.transformers.HelidonServiceTransformer"/>
                </transformers>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>io.helidon.build-tools</groupId>
            <artifactId>helidon-shade-extensions</artifactId>
            <version>4.0.0-SNAPSHOT</version>
        </dependency>
    </dependencies>
</plugin>
```