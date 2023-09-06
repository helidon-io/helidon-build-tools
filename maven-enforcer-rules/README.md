# Helidon Build Common Maven Enforcer Rules

This module provides common Helidon enforcer rules intended to integrate with the [Maven Enforcer Plugin](https://maven.apache.org/enforcer/maven-enforcer-plugin/).

## Rules
* [HelidonJakartaDependenciesRule](src/main/java/io/helidon/build/common/maven/enforcer/rules/HelidonJakartaDependenciesRule.java) - Verifies compile and runtime maven dependencies (including transitive dependencies) for compatibility with Helidon.

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
                                <namespace>JARKTA | JAVAX</namespace>
                                 <!-- list of strings - can be used to exclude a package / group name from validation -->
                                 <excludedGavRegExs>
                                    <excludedGavRegEx>javax.servlet.*</excludedGavRegEx>
                                 </excludedGavs>
                            </helidonJakartaDependenciesRule>
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
