/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import static io.helidon.build.util.FileUtils.assertDir;
import static io.helidon.build.util.FileUtils.assertFile;

/**
 * A Helidon application.
 */
public class Application {
    private static final String PROJECT_BUILD_SOURCE_ENCODING = "project.build.sourceEncoding";
    private static final String UTF_8 = "UTF-8";
    private static final String POM_FILE = "pom.xml";
    private final Path rootDir;
    private final Path pomFile;
    private final MavenProject project;

    public Application(Path projectRoot) throws IOException {
        this.rootDir = assertDir(projectRoot);
        this.pomFile = assertFile(rootDir.resolve(POM_FILE));
        final Model model = readModel(pomFile);
        model.getBuild().setDirectory(rootDir.toString());
        project = new MavenProject(model);
        project.getProperties().put(PROJECT_BUILD_SOURCE_ENCODING, UTF_8);
        project.setFile(pomFile.toFile());
    }

    private static Model readModel(final Path pomXml) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(pomXml)) {
            return new MavenXpp3Reader().read(reader);
        } catch (XmlPullParserException e) {
            throw new IOException("Error parsing " + pomXml, e);
        }
    }
}
