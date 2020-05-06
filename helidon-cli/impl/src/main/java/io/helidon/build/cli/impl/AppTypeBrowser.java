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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.helidon.build.cli.impl.InitCommand.Flavor;

import io.helidon.build.util.Log;
import org.apache.maven.model.Model;

import static io.helidon.build.cli.impl.PomReader.readPomModel;

/**
 * Class ArchetypeBrowser. Unix style path only for now.
 */
class AppTypeBrowser {

    private static final String REMOTE_REPO = "https://repo.maven.apache.org/maven2";
    private static final String LOCAL_REPOSITORY = System.getProperty("user.home") + "/.m2/repository";
    private static final String ARCHETYPE_DIRECTORY = "/io/helidon/build-tools/archetype";
    private static final String ARCHETYPE_PREFIX = "helidon-archetype-apptypes-";

    private AppTypeBrowser() {
    }

    static List<String> appTypes(Flavor flavor, String helidonVersion) {
        List<String> remoteAppTypes = appTypesRemoteRepo(flavor, helidonVersion);
        if (remoteAppTypes.isEmpty()) {
            Log.warn("Unable to find apptypes in remote repository");
        }
        List<String> localAppTypes = appTypesLocalRepo(flavor, helidonVersion);
        if (localAppTypes.isEmpty()) {
            Log.warn("Unable to find apptypes in local repository");
        }
        remoteAppTypes.addAll(localAppTypes);
        return remoteAppTypes;
    }

    static List<String> appTypesRemoteRepo(Flavor flavor, String helidonVersion) {
        String pomFile = String.format("%s%s-%s.pom", ARCHETYPE_PREFIX, flavor, helidonVersion);
        String location = String.format("%s%s/%s/%s/%s", REMOTE_REPO, ARCHETYPE_DIRECTORY,
                ARCHETYPE_PREFIX + flavor, helidonVersion, pomFile);
        try {
            URL url = new URL(location);
            try (InputStream is = url.openConnection().getInputStream()) {
                Model model = readPomModel(is);
                return model.getModules();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            // falls through
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<>();
    }

    static List<String> appTypesLocalRepo(Flavor flavor, String helidonVersion) {
        String pomFile = String.format("%s%s-%s.pom", ARCHETYPE_PREFIX, flavor, helidonVersion);
        Path path = Path.of(LOCAL_REPOSITORY, ARCHETYPE_DIRECTORY,
                            ARCHETYPE_PREFIX + flavor, helidonVersion, pomFile);
        if (path.toFile().exists()) {
            Model model = readPomModel(path.toFile());
            return model.getModules();
        }
        return new ArrayList<>();
    }

    static Path jarFileLocalRepo(Flavor flavor, String helidonVersion, String apptype) {
        String dirFile = String.format("%s%s-%s", ARCHETYPE_PREFIX, flavor, apptype);
        String jarFile = String.format("%s%s-%s-%s.jar", ARCHETYPE_PREFIX, flavor, apptype, helidonVersion);
        Path path = Path.of(LOCAL_REPOSITORY + ARCHETYPE_DIRECTORY, dirFile, helidonVersion, jarFile);
        return path.toFile().exists() ? path : null;
    }

    public static void main(String[] args) {
        appTypesRemoteRepo(Flavor.SE, "2.0.0-SNAPSHOT").forEach(System.out::println);
        System.out.println(jarFileLocalRepo(Flavor.SE, "2.0.0-SNAPSHOT", "basic"));
    }
}
