# Helidon Enforcer Maven Plugin

This plugin provides various validations.

#### Goals

* [check](#goal-check)

## Goal: `check`

Maven goal to enforce configured rules.

This goal binds to the `validate` phase by default.

### Parameters

| Property              | Type                 | Default Value     | Description                                                                                                               |
|-----------------------|----------------------|-------------------|---------------------------------------------------------------------------------------------------------------------------|
| skip                  | boolean              | `false`           | Skip execution of this goal                                                                                               |
| rules                 | String[]             | `copyright,typos` | Enforcer rules to execute                                                                                                 |
| enforcerOutputFile    | File                 |                   | File to write output to. Output is plain text, one failure per line, starts with either `ENFORCER OK` or `ENFORCER ERROR` |
| honorGitIgnore        | boolean              | `true`            | Whether to use git ignore to match files                                                                                  |
| useGit                | boolean              | `true`            | Use git. When `false`, all files will be checked and their last modification timestamp used                               |
| repositoryRoot        | File                 |                   | Root of the repository. When within git, this is not needed and will be computed using git command                        |
| copyrightConfig       | CopyrightConfig      |                   | see [CopyrightConfig](#CopyrightConfig)                                                                                   |
| typosConfig           | TypoConfig           |                   | see [TypoConfig](#TypoConfig)                                                                                             |
| nativeImageConfig     | NativeImageConfig    |                   | see [NativeImageConfig](#NativeImageConfig)                                                                               |
| inclusiveNamingConfig | InlusiveNamingConfig |                   | see [InlusiveNamingConfig](#InlusiveNamingConfig)                                                                         |

The above parameters are mapped to user properties of the form `helidon.enforcer.PROPERTY`, e.g. `-Dhelidon.enforcer.skip=true`.

#### CopyrightConfig

| Property      | Type    | Default Value | Description                                 |
|---------------|---------|---------------|---------------------------------------------|
| failOnError   | boolean | `false`       | Fail if copyright is invalid                |
| templateFile  | File    |               | File with the template to use for copyright |
| excludeFile   | File    |               | File with excludes                          |
| yearSeparator | String  | `, `          | Copyright year separator                    |

#### TypoConfig

| Property    | Type     | Default Value | Description                                                                      |
|-------------|----------|---------------|----------------------------------------------------------------------------------|
| failOnError | boolean  | `false`       | Whether to fail on error                                                         |
| excludes    | String[] |               | List of suffixes (such as `.js` and file names `Dockerfile` to include           |
| includes    | String[] |               | List of suffixes (such as `.js`) and file names (such as `Dockerfile` to include |
| typos       | String[] |               | Copyright year separator                                                         |

#### NativeImageConfig

| Property    | Type                | Default Value | Description               |
|-------------|---------------------|---------------|---------------------------|
| failOnError | boolean             | `false`       | Whether to fail on error  |
| rules       | List<VersionConfig> |               | List of forbidden version |

##### VersionConfig

| Property | Type   | Default Value | Description                                                                   |
|----------|--------|---------------|-------------------------------------------------------------------------------|
| version  | String |               | version to check                                                              |
| matcher  | String |               | One of `greaterThan`, `lessThan`, `greaterThanOrEqualTo`, `lessThanOrEqualTo` |

#### InlusiveNamingConfig

| Property            | Type      | Default Value | Description                                                                                                |
|---------------------|-----------|---------------|------------------------------------------------------------------------------------------------------------|
| failOnError         | boolean   | `false`       | Whether to fail on error                                                                                   |
| excludes            | String[]  |               | List of suffixes (such as `.js` and file names `Dockerfile` to include                                     |
| includes            | String[]  |               | List of suffixes (such as `.js`) and file names (such as `Dockerfile` to include                           |
| excludeTermsRegExps | String[]  |               | Regular expressions containing to exclude terms (such as {@code ((?i)master)})                             |
| additionalTerms     | XmlData[] |               | Additional terms                                                                                           |
| inclusiveNamingFile | File      |               | XML file equivalent to the inclusive naming JSON {@link https://inclusivenaming.org/word-lists/index.json} |

##### XmlData

| Property                | Type     | Description                                      |
|-------------------------|----------|--------------------------------------------------|
| term                    | String   | Non inclusive term                               |
| recommendedReplacements | String[] | Recommended replacements, or `None`              |
| tier                    | String   | Numerical value of the tier                      |
| recommendation          | String   | General recommendation on how to handle the term |
| termPage                | String   | URL that describes why the term is non inclusive |

### General usage

```xml
<project>
    <!-- ... -->
    <plugin>
        <groupId>io.helidon.build-tools</groupId>
        <artifactId>helidon-enforcer-plugin</artifactId>
        <version>${version.plugin.helidon-build-tools}</version>
        <configuration>
            <rules>
                <rule>copyright</rule>
                <rule>typos</rule>
            </rules>
            <copyrightConfig>
                <failOnError>true</failOnError>
                <templateFile>${maven.multiModuleProjectDirectory}/etc/copyright.txt</templateFile>
                <excludeFile>${maven.multiModuleProjectDirectory}/etc/copyright-exclude.txt</excludeFile>
            </copyrightConfig>
            <typosConfig>
                <failOnError>true</failOnError>
                <typos>
                    <typo>heldion</typo>
                    <typo>helidion</typo>
                    <typo>buidler</typo>
                    <typo>vuilder</typo>
                    <typo>bidler</typo>
                </typos>
            </typosConfig>
        </configuration>
        <executions>
            <execution>
                <id>helidon-enforcer</id>
                <phase>validate</phase>
                <goals>
                    <goal>check</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
</project>
```