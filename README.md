<p align="center">
    <img src="./etc/images/Primary_logo_blue.png">
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
 I.e. Helidon Built Tools version `X.Y.?` is compatible with Helidon version
 `X.Y.?`.

## Build

You will need Java 17 and Maven 3.8.2 or newer.

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

## Documentation

* For CLI documentation see [cli/README.md](cli/README.md)
* For maven plugin documentation see [maven-plugins/README.md](maven-plugins/README.md)

## Contributing

This project welcomes contributions from the community. Before submitting a pull request, please [review our contribution guide](./CONTRIBUTING.md)

## License

Copyright (c) 2017, 2024 Oracle and/or its affiliates.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.



