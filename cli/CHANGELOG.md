# Helidon CLI Changelog

See this [document](https://github.com/oracle/helidon/blob/master/HELIDON-CLI.md)
for more info about the Helidon CLI.

All notable changes to the Helidon CLI will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.0.6]

### Fixes

- NoClassDefFoundError: io/helidon/build/cli/common/ArchetypesDataLoader [944](https://github.com/helidon-io/helidon-build-tools/issues/944)
- CLI: 3.0.5 does not handle latest vs default version correctly [945](https://github.com/helidon-io/helidon-build-tools/issues/945)
- CLI: 3.0.5 latest version listed first, but maybe it shouldn't be [946](https://github.com/helidon-io/helidon-build-tools/issues/946)
- Improve layout of helidon.zip [942](https://github.com/helidon-io/helidon-build-tools/issues/942)

## [3.0.5]

### Fixes

- Archetype templates scoped model is merged with global model [877](https://github.com/helidon-io/helidon-build-tools/pull/877)
- MergedModel.resolveModel should not require the root scope [878](https://github.com/helidon-io/helidon-build-tools/pull/878)

### New Features

- Add --props-file option to read properties during project generation [701](https://github.com/helidon-io/helidon-build-tools/issues/701)

## [3.0.4]

### Fixes

- Fix archetype optimizer and JSON serializer [884](https://github.com/helidon-io/helidon-build-tools/pull/884)
- Remove usage of `picocli.jansi.graalvm.AnsiConsole.systemInstall()` [860](https://github.com/helidon-io/helidon-build-tools/pull/860)
- Remove graal-sdk [861](https://github.com/helidon-io/helidon-build-tools/pull/861)

## 3.0.3

This release of helidon-build-tools does not contain changes related to the Helidon CLI.

## [3.0.2]

### Fixes

- Sanitize environment/system properties [810](https://github.com/helidon-io/helidon-build-tools/pull/810) [798](https://github.com/helidon-io/helidon-build-tools/pull/798)
- Handle Processing instructions in SimpleXmlParser [785](https://github.com/helidon-io/helidon-build-tools/pull/785) [780](https://github.com/helidon-io/helidon-build-tools/pull/780)
- `helidon init` add defaults in case of the user has not specify mandatory options [776](https://github.com/helidon-io/helidon-build-tools/pull/776) [774](https://github.com/helidon-io/helidon-build-tools/pull/774)
- Fix TemplateSupport caching [759](https://github.com/helidon-io/helidon-build-tools/pull/759)

## [3.0.0]

This change entry describes **all** changes made for the 3.0.0 version ; it is an aggregation of the non GA 3.x entries below.

### Fixes

- Add --errors options to print the exception stack traces [702](https://github.com/oracle/helidon-build-tools/issues/702) [741](https://github.com/oracle/helidon-build-tools/pull/741)
- Cli does not prompt input with external default in interactive mode [725](https://github.com/oracle/helidon-build-tools/issues/725) [733](https://github.com/oracle/helidon-build-tools/issues/733)
- Init command uses v1 catalog for a 3.x Helidon version. [727](https://github.com/oracle/helidon-build-tools/issues/727) [728](https://github.com/oracle/helidon-build-tools/pull/728)
- Interactive prompter display steps without prompting for any inputs. [735](https://github.com/oracle/helidon-build-tools/issues/735) [736](https://github.com/oracle/helidon-build-tools/issues/736)
- Add support for argument file (primary used by init). [690](https://github.com/oracle/helidon-build-tools/issues/690) [704](https://github.com/oracle/helidon-build-tools/pull/704)
- BuildCommand has no output [668](https://github.com/oracle/helidon-build-tools/issues/668) [671](https://github.com/oracle/helidon-build-tools/pull/671)
- Process monitor rework (forward-port) [511](https://github.com/oracle/helidon-build-tools/pull/511)
- Fix shutdown hooks in native-image (forward port) [520](https://github.com/oracle/helidon-build-tools/pull/520)
- Suppress download progress output during build [332](https://github.com/oracle/helidon-build-tools/issues/332) [568](https://github.com/oracle/helidon-build-tools/pull/568)
- Need to touch pom.xml twice for dev loop to process the change [429](https://github.com/oracle/helidon-build-tools/issues/429) [569](https://github.com/oracle/helidon-build-tools/pull/569) [576](https://github.com/oracle/helidon-build-tools/pull/576)

### Changes

- Validate Helidon version input. [628](https://github.com/oracle/helidon-build-tools/pull/628)
- Handle `latest` file with multiple lines [633](https://github.com/oracle/helidon-build-tools/pull/633)
- All archetype values can be passed as properties `-Dprop=value` [543](https://github.com/oracle/helidon-build-tools/issues/543)
- Remove project name [590](https://github.com/oracle/helidon-build-tools/issues/590) [620](https://github.com/oracle/helidon-build-tools/pull/620)
- Remove `project.directory` from the `.helidon` file [339](https://github.com/oracle/helidon-build-tools/issues/339) [620](https://github.com/oracle/helidon-build-tools/pull/620)
- Clarify comments in generated user config [624](https://github.com/oracle/helidon-build-tools/pull/624)

### Notable New Features

- New V2 archetype engine to support Helidon 3.x and onward [550](https://github.com/oracle/helidon-build-tools/issues/550) [345](https://github.com/oracle/helidon-build-tools/issues/345)

## [3.0.0-RC3]

### Fixes

- Add --errors options to print the exception stack traces [702](https://github.com/oracle/helidon-build-tools/issues/702) [741](https://github.com/oracle/helidon-build-tools/pull/741)

## [3.0.0-RC2]

### Fixes

- Cli does not prompt input with external default in interactive mode [725](https://github.com/oracle/helidon-build-tools/issues/725) [733](https://github.com/oracle/helidon-build-tools/issues/733)
- Init command uses v1 catalog for a 3.x Helidon version. [727](https://github.com/oracle/helidon-build-tools/issues/727) [728](https://github.com/oracle/helidon-build-tools/pull/728)
- Interactive prompter display steps without prompting for any inputs. [735](https://github.com/oracle/helidon-build-tools/issues/735) [736](https://github.com/oracle/helidon-build-tools/issues/736)

## [3.0.0-RC1]

### Changes

- Archetype Context and Expression updates [723](https://github.com/oracle/helidon-build-tools/pull/723)

## 3.0.0-M5

This release of helidon-build-tools does not contain changes related to the Helidon CLI.

## [3.0.0-M4]

### Changes

- Archetype updates [712](https://github.com/oracle/helidon-build-tools/pull/712)

### Fixes

- Add support for argument file (primary used by init). [690](https://github.com/oracle/helidon-build-tools/issues/690) [704](https://github.com/oracle/helidon-build-tools/pull/704)
- BuildCommand has no output [668](https://github.com/oracle/helidon-build-tools/issues/668) [671](https://github.com/oracle/helidon-build-tools/pull/671)

## [3.0.0-M3]

### Changes

- Validate Helidon version input. [628](https://github.com/oracle/helidon-build-tools/pull/628)
- Handle `latest` file with multiple lines [633](https://github.com/oracle/helidon-build-tools/pull/633)

### Fixes

- Fix prompter when default answer is null [630](https://github.com/oracle/helidon-build-tools/pull/630)
- Process monitor rework (forward-port) [511](https://github.com/oracle/helidon-build-tools/pull/511)
- Fix shutdown hooks in native-image (forward port) [520](https://github.com/oracle/helidon-build-tools/pull/520)

## [3.0.0-M2]

### Notable New Features

- New V2 archetype engine to support Helidon 3.x and onward [550](https://github.com/oracle/helidon-build-tools/issues/550) [345](https://github.com/oracle/helidon-build-tools/issues/345)

### Changes

- All archetype values can be passed as properties `-Dprop=value` [543](https://github.com/oracle/helidon-build-tools/issues/543)
- Remove project name [590](https://github.com/oracle/helidon-build-tools/issues/590) [620](https://github.com/oracle/helidon-build-tools/pull/620)
- Remove `project.directory` from the `.helidon` file [339](https://github.com/oracle/helidon-build-tools/issues/339) [620](https://github.com/oracle/helidon-build-tools/pull/620)
- Clarify comments in generated user config [624](https://github.com/oracle/helidon-build-tools/pull/624)

### Fixes

- Suppress download progress output during build [332](https://github.com/oracle/helidon-build-tools/issues/332) [568](https://github.com/oracle/helidon-build-tools/pull/568)
- Need to touch pom.xml twice for dev loop to process the change [429](https://github.com/oracle/helidon-build-tools/issues/429) [569](https://github.com/oracle/helidon-build-tools/pull/569) [576](https://github.com/oracle/helidon-build-tools/pull/576)

## 2.3.4

This release of helidon-build-tools does not contain changes related to the Helidon CLI.

## [2.3.3]

### Fixes

- Fix isSupportedVersion to handle PluginFailedException correctly. [639](https://github.com/oracle/helidon-build-tools/pull/639)
- Fix LineReader.drain. [643](https://github.com/oracle/helidon-build-tools/pull/643)
- Backport InitCommand fixes. [647](https://github.com/oracle/helidon-build-tools/pull/647)

## [2.3.2]

### Fixes

- Suppress download progress output during build [568](https://github.com/oracle/helidon-build-tools/issues/568) [332](https://github.com/oracle/helidon-build-tools/issues/332)
- Need to touch pom.xml twice for dev loop to process the change [429](https://github.com/oracle/helidon-build-tools/issues/429) [569](https://github.com/oracle/helidon-build-tools/issues/569) [576](https://github.com/oracle/helidon-build-tools/issues/576)
- Fix log level support  [560](https://github.com/oracle/helidon-build-tools/issues/560)
- Improve error message when unsupported Helidon version is used [627](https://github.com/oracle/helidon-build-tools/627)

## 2.3.1

This release of helidon-build-tools does not contain changes related to the Helidon CLI.

## [2.3.0]

### Fixes

- Metadata errors not reported [487](https://github.com/oracle/helidon-build-tools/issues/487) [506](https://github.com/oracle/helidon-build-tools/pull/506)
- dev-loop incremental recompilation fails with maven 3.8.2 [499](https://github.com/oracle/helidon-build-tools/issues/499) [500](https://github.com/oracle/helidon-build-tools/pull/500)
- dev-loop does not re-build for resource files on Windows [357](https://github.com/oracle/helidon-build-tools/issues/357) [483](https://github.com/oracle/helidon-build-tools/issues/483) [498](https://github.com/oracle/helidon-build-tools/pull/498)
- dev-loop ctrl+c on Windows [511](https://github.com/oracle/helidon-build-tools/pull/511)
- fix shutdown hooks with native image [520](https://github.com/oracle/helidon-build-tools/pull/520)

### Changes

- Consolidated dev-loop Maven integration code [504](https://github.com/oracle/helidon-build-tools/pull/504/files)
- Improved process monitoring, reduced CPU usage, improved console printing [511](https://github.com/oracle/helidon-build-tools/pull/511)

### Notes

The dev-loop bug fixes and changes described above are part of the `helidon-cli-maven-plugin`. You may run into the
described issues if you are using an older version of the plugin. You can use `helidon dev --current` to force the use
of the plugin matching that matches the cli version.


## [2.2.3]

### Fixes

- Update the changelog URL displayed for updates [458](https://github.com/oracle/helidon-build-tools/pull/468)

## [2.2.2]

### Fixes

- Fix Jansi usage to work with Maven 3.8+ [458](https://github.com/oracle/helidon-build-tools/issues/458) [460](https://github.com/oracle/helidon-build-tools/pull/460)

## 2.2.1

This release of helidon-build-tools does not contain changes related to the Helidon CLI.

## [2.2.0]

### Changes

- Update CLI code generation [366](https://github.com/oracle/helidon-build-tools/pull/366)
- Improve CLI plugins [368](https://github.com/oracle/helidon-build-tools/pull/368)
- CLI plugin embedded support [367](https://github.com/oracle/helidon-build-tools/pull/367)

### Fixes

- Do not error if the Maven version cannot be resolved. Allow support of shims [390](https://github.com/oracle/helidon-build-tools/pull/390) [364](https://github.com/oracle/helidon-build-tools/pull/364)
- CLI always generates project in the same directory [393](https://github.com/oracle/helidon-build-tools/pull/393) [333](https://github.com/oracle/helidon-build-tools/pull/333)
- Fix bad error message when no Maven is not found in the PATH [396](https://github.com/oracle/helidon-build-tools/pull/396) [304](https://github.com/oracle/helidon-build-tools/pull/304)
- Fix aether utility to use Maven settings [397](https://github.com/oracle/helidon-build-tools/pull/397) [383](https://github.com/oracle/helidon-build-tools/issues/383)

## [2.1.3]

### Notable New Features

- Windows cli support [319](https://github.com/oracle/helidon-build-tools/pull/319)

### Fixes

- Fix default project directory handling in init command [329](https://github.com/oracle/helidon-build-tools/pull/329) [326]([](https://github.com/oracle/helidon-build-tools/issues/326))
- Windows path fix [341](https://github.com/oracle/helidon-build-tools/pull/341)
- Change Proxies to avoid initializing Ansi for debug messages [318](https://github.com/oracle/helidon-build-tools/pull/318) [314](https://github.com/oracle/helidon-build-tools/issues/314)

## [2.1.2]

### Changes

- Display a message when exiting the dev-loop [269](https://github.com/oracle/helidon-build-tools/issues/269) [303](https://github.com/oracle/helidon-build-tools/pull/303)

### Fixes

- Dev-loop hangs on startups with new Helidon version [302](https://github.com/oracle/helidon-build-tools/issues/302) [303](https://github.com/oracle/helidon-build-tools/pull/303)
- Dev-loop hangs if the app cannot start [284](https://github.com/oracle/helidon-build-tools/issues/284) [300](https://github.com/oracle/helidon-build-tools/issues/300)
- Dev-loop stops on application failure [286](https://github.com/oracle/helidon-build-tools/issues/286) [291](https://github.com/oracle/helidon-build-tools/pull/291)
- Dev-loop maven extension mis-configured [293](https://github.com/oracle/helidon-build-tools/issues/293) [293](https://github.com/oracle/helidon-build-tools/pull/294)
- Name conflict between `jlink` and `native` build modes [295](https://github.com/oracle/helidon-build-tools/issues/295) [301](https://github.com/oracle/helidon-build-tools/pull/301)
- `default.artifact.id` user configuration is ignored [289](https://github.com/oracle/helidon-build-tools/issues/289) [292](https://github.com/oracle/helidon-build-tools/pull/292)

### Notable New Features

- App debug port option [173](https://github.com/oracle/helidon-build-tools/issues/173) [306](https://github.com/oracle/helidon-build-tools/pull/306)
- Dev-loop hooks [244](https://github.com/oracle/helidon-build-tools/issues/244) [281](https://github.com/oracle/helidon-build-tools/pull/281)

## 2.1.1

This release of helidon-build-tools does not contain changes related to the Helidon CLI.

## [2.1.0]

### Changes

- Split cli maven goal out of helidon-maven-plugin [247](https://github.com/oracle/helidon-build-tools/issues/247) [260](https://github.com/oracle/helidon-build-tools/pull/260) [271](https://github.com/oracle/helidon-build-tools/pull/271)
- Backward compatibility with new helidon-cli-maven-plugin [267](https://github.com/oracle/helidon-build-tools/pull/267) [259](https://github.com/oracle/helidon-build-tools/issues/259)
- Various fixes to prepare upcoming support of windows.

### Fixes

- Close process output readers [268](https://github.com/oracle/helidon-build-tools/pull/268) [273](https://github.com/oracle/helidon-build-tools/pull/273)
- Fix `STDOUT` processing [270](https://github.com/oracle/helidon-build-tools/pull/270)
- Redirect `STDIN` for all Maven processes [266](https://github.com/oracle/helidon-build-tools/pull/266)
- Fix CLI option parsing for non-global properties [261](https://github.com/oracle/helidon-build-tools/issues/261) [263](https://github.com/oracle/helidon-build-tools/pull/263)
- Use `File.pathSeparator` to create class-path [264](https://github.com/oracle/helidon-build-tools/pull/264)
- Fix `PATH` environment processing [250](https://github.com/oracle/helidon-build-tools/issues/250)
- Fix CLI update message [234](https://github.com/oracle/helidon-build-tools/issues/234) [235](https://github.com/oracle/helidon-build-tools/pull/235)

### Notable New Features

- Property pass-through [248](https://github.com/oracle/helidon-build-tools/issues/248) [272](https://github.com/oracle/helidon-build-tools/pull/272)
- Dev-loop pass-through [239](https://github.com/oracle/helidon-build-tools/issues/239) [239](https://github.com/oracle/helidon-build-tools/issues/239)
- Add option and user preference to disable ANSI colors [256](https://github.com/oracle/helidon-build-tools/issues/256) [236](https://github.com/oracle/helidon-build-tools/pull/236) [192](https://github.com/oracle/helidon-build-tools/issues/192)
- Support user configurable defaults of init options [252](https://github.com/oracle/helidon-build-tools/pull/252)

## [2.0.2]

### Changes

- Fix proxy settings, update info and version commands to not fail if metadata is not accessible [215](https://github.com/oracle/helidon-build-tools/pull/215)
- Make metadata updates quiet for all commands except init [218](https://github.com/oracle/helidon-build-tools/pull/218)
- Include url in error message [230](https://github.com/oracle/helidon-build-tools/pull/230)
- Fix Maven version detection [229](https://github.com/oracle/helidon-build-tools/pull/229)
- Only require JAVA_HOME when java is not found in the PATH [230](https://github.com/oracle/helidon-build-tools/pull/230)

## 2.0.1

This release of helidon-build-tools does not contain changes related to the Helidon CLI.


## [2.0.0]

Initial release of the Helidon CLI.

[3.0.6]:      https://github.com/oracle/helidon-build-tools/compare/3.0.5...3.0.6
[3.0.5]:      https://github.com/oracle/helidon-build-tools/compare/3.0.4...3.0.5
[3.0.4]:      https://github.com/oracle/helidon-build-tools/compare/3.0.2...3.0.4
[3.0.2]:      https://github.com/oracle/helidon-build-tools/compare/3.0.0...3.0.2
[3.0.0]:      https://github.com/oracle/helidon-build-tools/compare/3.0.0-M2...3.0.0
[3.0.0-RC3]:  https://github.com/oracle/helidon-build-tools/compare/3.0.0-RC2...3.0.0-RC3
[3.0.0-RC2]:  https://github.com/oracle/helidon-build-tools/compare/3.0.0-RC1...3.0.0-RC2
[3.0.0-RC1]:  https://github.com/oracle/helidon-build-tools/compare/3.0.0-M4...3.0.0-RC1
[3.0.0-M4]:   https://github.com/oracle/helidon-build-tools/compare/3.0.0-M3...3.0.0-M4
[3.0.0-M3]:   https://github.com/oracle/helidon-build-tools/compare/3.0.0-M2...3.0.0-M3
[3.0.0-M2]:   https://github.com/oracle/helidon-build-tools/tree/3.0.0-M2/cli
[2.3.3]:      https://github.com/oracle/helidon-build-tools/compare/2.3.2...2.3.3
[2.3.2]:      https://github.com/oracle/helidon-build-tools/compare/2.3.0...2.3.2
[2.3.0]:      https://github.com/oracle/helidon-build-tools/compare/2.2.3...2.3.0
[2.2.3]:      https://github.com/oracle/helidon-build-tools/compare/2.2.2...2.2.3
[2.2.2]:      https://github.com/oracle/helidon-build-tools/compare/2.2.0...2.2.2
[2.2.0]:      https://github.com/oracle/helidon-build-tools/compare/2.1.3...2.2.0
[2.1.3]:      https://github.com/oracle/helidon-build-tools/compare/2.1.2...2.1.3
[2.1.2]:      https://github.com/oracle/helidon-build-tools/compare/2.1.1...2.1.2
[2.1.0]:      https://github.com/oracle/helidon-build-tools/compare/2.0.2...2.1.0
[2.0.2]:      https://github.com/oracle/helidon-build-tools/compare/2.0.1...2.0.2
[2.0.0]:      https://github.com/oracle/helidon-build-tools/tree/2.0.0/helidon-cli
