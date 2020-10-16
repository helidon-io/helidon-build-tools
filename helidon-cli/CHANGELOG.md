# Helidon CLI Changelog

See this [document](https://github.com/oracle/helidon/blob/master/HELIDON-CLI.md)
 for more info about the Helidon CLI.

All notable changes to the Helidon CLI will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
- Fix CLI option parsing for non global properties [261](https://github.com/oracle/helidon-build-tools/issues/261) [263](https://github.com/oracle/helidon-build-tools/pull/263)
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

[2.1.2]: https://github.com/oracle/helidon-build-tools/compare/2.1.1...2.1.2
[2.1.0]: https://github.com/oracle/helidon-build-tools/compare/2.0.2...2.1.0
[2.0.2]: https://github.com/oracle/helidon-build-tools/compare/2.0.1...2.0.2
[2.0.0]: https://github.com/oracle/helidon-build-tools/tree/2.0.0/helidon-cli

