# Helidon Build Common Maven Enforcer Rules

This module provides common Helidon enforcer rules intended to integrate with the [Maven Enforcer Plugin](https://maven.apache.org/enforcer/maven-enforcer-plugin/).

## Rules
* [HelidonDependenciesRule](src/main/java/io/helidon/build/maven/enforcer/rules/HelidonDependenciesRule.java) - Verifies compile and runtime maven dependencies (including transitive dependencies) for compatibility with Helidon.

### General usage

Here is an example pom.xml:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-enforcer-plugin</artifactId>
            <dependencies>
                <dependency>
                    <groupId>io.helidon.build-tools</groupId>
                    <artifactId>helidon-build-maven-enforcer-rules</artifactId>
                    <version>${helidon.build-tools.version}</version>
                </dependency>
            </dependencies>
            <executions>
                <execution>
                    <id>enforce-helidon-dependencies</id>
                    <goals>
                        <goal>enforce</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <helidonDependenciesRule>
                                <namespace>JAKARTA | JAVAX</namespace>
                                 <!-- list of strings - can be used to exclude a package / group name from validation -->
<!--                                 <excludedGavRegExs>-->
<!--                                    &lt;!&ndash; for example only - we suggest not including this exclusion in your usage &ndash;&gt;-->
<!--                                    <excludedGavRegEx>javax.servlet.*</excludedGavRegEx>-->
<!--                                 </excludedGavRegExs>-->
                            </helidonDependenciesRule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        ...
```

```bash
mvn package
```
