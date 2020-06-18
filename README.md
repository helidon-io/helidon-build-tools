<p align="center">
    <img src="./etc/images/Primary_logo_blue.png" height="180">
</p>
<p align="center">
    <a href="https://github.com/oracle/helidon-build-tools/tags">
        <img src="https://img.shields.io/github/tag/oracle/helidon-build-tools.svg" alt="latest version">
    </a>
    <a href="https://github.com/oracle/helidon-build-tools/issues">
        <img src="https://img.shields.io/github/issues/oracle/helidon-build-tools.svg" alt="latest version">
    </a>
    <a href="https://twitter.com/intent/follow?screen_name=helidon_project">
        <img src="https://img.shields.io/twitter/follow/helidon_project.svg?style=social&logo=twitter" alt="follow on Twitter">
    </a>
</p>

# Helidon Build Tools

Build tools for the Helidon Project.

## Helidon Compatibility

The Helidon Build Tools version is aligned with the Helidon `major.minor` version.
 I.e Helidon Built Tools version `X.Y.?` is compatible with Helidon version
 `X.Y.?`.

## Build

You will need Java 11 and Maven 3.6.3 or newer.

**Full build**
```bash
$ mvn install
```

**Checkstyle**
```bash
# Cd to the component you want to check
$ mvn validate  -Pcheckstyle
```

**Copyright**

```bash
# Cd to the component you want to check
$ mvn validate  -Pcopyright
```

**Spotbugs**

```bash
# Cd to the component you want to check
$ mvn verify  -Pspotbugs
```
