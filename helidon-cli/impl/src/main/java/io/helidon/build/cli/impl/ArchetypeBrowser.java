/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.build.cli.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.build.cli.impl.InitCommand.Flavor;
import io.helidon.build.util.FileUtils;

import static io.helidon.build.util.Constants.DIR_SEP;

/**
 * Class ArchetypeBrowser.
 */
class ArchetypeBrowser {

    private static final String LOCAL_REPOSITORY = System.getProperty("user.home") + DIR_SEP + "/.m2/repository";
    private static final String ARCHETYPE_DIRECTORY = "/io/helidon/build-tools/archetype";
    private static final String ARCHETYPE_PREFIX = "helidon-archetype-apptypes-";

    private ArchetypeBrowser() {
    }

    static ArrayList<Path> jarsLocalRepo(Flavor flavor, String helidonVersion) {
        List<Path> paths = FileUtils.list(Path.of(LOCAL_REPOSITORY + ARCHETYPE_DIRECTORY));
        return paths.stream()
                .filter(p -> p.getFileName().toString().startsWith(ARCHETYPE_PREFIX + flavor + "-"))
                .map(p -> p.resolve(helidonVersion))
                .flatMap(p -> FileUtils.listFiles(p, f -> f.endsWith(".jar")).stream())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    static ArrayList<String> appTypesLocalRepo(Flavor flavor, String helidonVersion) {
        return appTypesLocalRepo(jarsLocalRepo(flavor, helidonVersion));
    }

    static ArrayList<String> appTypesLocalRepo(List<Path> jars) {
        return jars.stream()
                .map(p -> {
                    String s = p.getFileName().toString().substring(ARCHETYPE_PREFIX.length());
                    return s.split("-")[1];     // apptype
                }).collect(Collectors.toCollection(ArrayList::new));
    }
}
