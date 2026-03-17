# Repository Guidelines

## Project Structure & Module Organization
The root `pom.xml` aggregates the main modules: `common/` shared utilities,
`maven-enforcer-rules/`, `maven-plugins/`, `cli/`, `archetype/`, `linker/`, `dev-loop/`,
`ide-support/`, and `licensing/`. Java code follows the standard Maven layout:
`src/main/java` for production code and `src/test/java` for unit tests. Integration and
invoker-style coverage lives in `src/it` and fixture projects such as
`cli/tests/functional/src/it/projects/`. Shared CI scripts and quality rules live in `etc/`;
generated outputs under `target/` and `target/it/` should not be edited or committed.

## Build, Test, and Development Commands
Use JDK 17 and Maven 3.8.2+ for local work.

- `mvn install` builds the full multi-module repository.
- `mvn -pl <module> -am test` runs unit tests for one module and its dependencies.
- `mvn -pl <module> -am verify` is the right pre-PR check when you touch `src/it`, Maven plugins, or linker/dev-loop integration code.
- `mvn validate -Pcheckstyle` runs the repository Checkstyle rules.
- `mvn validate -Pcopyright` checks license headers against `etc/copyright.txt`.
- `mvn verify -Pspotbugs` runs static analysis.
- `mvn -pl ide-support/vscode-extension -Pvscode -DskipTests install` packages the VS Code
  extension; inside `ide-support/vscode-extension/`, use `npm run lint` and `npm test` for
  TypeScript changes.

## Coding Style & Naming Conventions
Java style is enforced by `etc/checkstyle.xml`: 4-space indentation, LF line endings,
130-character lines, and import groups ordered as `java`, `javax`, then `io.helidon`. Keep
packages under `io.helidon.build...`, use `UpperCamelCase` for types and `lowerCamelCase` for
members, and prefer one top-level type per file. Public and protected APIs are expected to have
Javadoc. Inline comments are not capitalized and do not end with a period. Javadoc comments are
capitalized and end with a period. Test classes use `*Test`; integration tests typically use
`*IT` or `src/it`.

## Testing Guidelines
JUnit 5 is the default test framework, with Hamcrest used heavily for assertions; some IDE support
tests also use Mockito. There is no repository-wide coverage percentage gate in the build, so add
or update tests for each behavior change instead of aiming at a numeric target. If you change
linker behavior, expect CI to exercise that area on JDK 25 as well.

## Commit & Pull Request Guidelines
Recent history favors short, imperative subjects, sometimes with a scope prefix, for example
`4.x: Fix archetype integration test` or `Linker code cleanup`. Before opening a PR, create or
link the tracking issue, use a branch name that includes the issue number, and ensure the Oracle
Contributor Agreement is signed. PR descriptions should explain the change, list the validation
commands you ran, and link the issue. Include screenshots only for user-visible CLI or VS Code
extension changes.
