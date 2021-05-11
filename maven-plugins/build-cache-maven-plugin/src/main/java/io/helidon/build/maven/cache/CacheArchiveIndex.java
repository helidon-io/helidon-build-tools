/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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
package io.helidon.build.maven.cache;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Cache index file.
 */
final class CacheArchiveIndex {

    private final List<ProjectEntry> projects;
    private final List<FileEntry> repoFiles;

    /**
     * Create a new index.
     *
     * @param projects  project entries
     * @param repoFiles list of path to maven repository files in the cache
     */
    CacheArchiveIndex(List<ProjectEntry> projects, List<FileEntry> repoFiles) {
        this.projects = projects;
        this.repoFiles = repoFiles == null ? List.of() : repoFiles;
    }

    /**
     * Get the repository files in the cache.
     *
     * @return list of file entry
     */
    List<FileEntry> repoFiles() {
        return repoFiles;
    }

    /**
     * Find a project in the index.
     *
     * @param groupId    project groupId
     * @param artifactId project artifactId
     * @return ProjectEntry or {@code null} if not found
     */
    ProjectEntry findProject(String groupId, String artifactId) {
        return projects.stream()
                       .filter(p -> p.groupId.equals(groupId) && p.artifactId.equals(artifactId))
                       .findAny()
                       .orElse(null);
    }

    /**
     * Save the index to a file.
     *
     * @param file output file
     * @throws IOException if an IO error occurs
     */
    void save(Path file) throws IOException {
        Xpp3Dom rootElt = toXml();
        FileWriter writer = new FileWriter(file.toFile());
        Xpp3DomWriter.write(writer, rootElt);
        writer.flush();
        writer.close();
    }

    /**
     * Convert this instance to an XML DOM element.
     *
     * @return Xpp3Dom
     */
    Xpp3Dom toXml() {
        Xpp3Dom rootElt = new Xpp3Dom("build-cache");
        Xpp3Dom repoFilesElt = new Xpp3Dom("repository");
        for (FileEntry repoFile : repoFiles) {
            Xpp3Dom repoFileElt = new Xpp3Dom("file");
            repoFileElt.setValue(String.valueOf(repoFile.index));
            repoFileElt.setAttribute("path", repoFile.path);
            repoFilesElt.addChild(repoFileElt);
        }
        rootElt.addChild(repoFilesElt);
        Xpp3Dom projectsElt = new Xpp3Dom("projects");
        for (ProjectEntry project : projects) {
            Xpp3Dom projectElt = new Xpp3Dom("project");
            projectElt.setAttribute("groupId", project.groupId);
            projectElt.setAttribute("artifactId", project.artifactId);
            projectElt.setAttribute("path", project.path);
            Xpp3Dom buildFilesElt = new Xpp3Dom("build");
            for (FileEntry buildFile : project.buildFiles) {
                Xpp3Dom buildFileElt = new Xpp3Dom("file");
                buildFileElt.setValue(String.valueOf(buildFile.index));
                buildFileElt.setAttribute("path", buildFile.path);
                buildFilesElt.addChild(buildFileElt);
            }
            projectElt.addChild(buildFilesElt);
            projectsElt.addChild(projectElt);
        }
        rootElt.addChild(projectsElt);
        return rootElt;
    }

    /**
     * Load the index from a file.
     *
     * @param is input stream
     * @return CacheIndex, never {@code null}
     * @throws IOException if an IO error occurs
     */
    static CacheArchiveIndex load(InputStream is) throws IOException, XmlPullParserException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(is)));
        Xpp3Dom rootElt = Xpp3DomBuilder.build(reader);
        reader.close();
        List<ProjectEntry> projects = new LinkedList<>();
        Xpp3Dom projectsElt = rootElt.getChild("projects");
        for (Xpp3Dom projectElt : projectsElt.getChildren("project")) {
            List<FileEntry> buildFiles = new LinkedList<>();
            Xpp3Dom buildFilesElt = projectElt.getChild("build");
            if (buildFilesElt != null) {
                for (Xpp3Dom fileElt : buildFilesElt.getChildren("file")) {
                    String index = fileElt.getValue();
                    String path = fileElt.getAttribute("path");
                    if (index != null && !index.isEmpty() && path != null && !path.isEmpty()) {
                        buildFiles.add(new FileEntry(path, index));
                    }
                }
            }
            projects.add(new ProjectEntry(
                    projectElt.getAttribute("groupId"),
                    projectElt.getAttribute("artifactId"),
                    projectElt.getAttribute("path"),
                    buildFiles));
        }
        List<FileEntry> repoFiles = new LinkedList<>();
        Xpp3Dom repositoryElt = rootElt.getChild("repository");
        for (Xpp3Dom fileElt : repositoryElt.getChildren("file")) {
            String index = fileElt.getValue();
            String path = fileElt.getAttribute("path");
            if (index != null && !index.isEmpty() && path != null && !path.isEmpty()) {
                repoFiles.add(new FileEntry(path, index));
            }
        }
        return new CacheArchiveIndex(projects, repoFiles);
    }

    /**
     * Project entry.
     */
    static final class ProjectEntry {

        private final String groupId;
        private final String artifactId;
        private final String path;
        private final List<FileEntry> buildFiles;

        /**
         * Create a new entry.
         *
         * @param groupId    project groupId
         * @param artifactId project artifactId
         * @param path       path of the build directory relative to the project root
         * @param buildFiles list of file entry for the project build files
         */
        ProjectEntry(String groupId, String artifactId, String path, List<FileEntry> buildFiles) {
            this.groupId = Objects.requireNonNull(groupId, "groupId is null");
            this.artifactId = Objects.requireNonNull(artifactId, "artifactId is null");
            this.path = Objects.requireNonNull(path, "path is null");
            this.buildFiles = buildFiles == null ? List.of() : buildFiles;
        }

        /**
         * Get the nested build files for this project entry.
         *
         * @return list file entry relative to the project build directory
         */
        List<FileEntry> buildFiles() {
            return buildFiles;
        }
    }

    /**
     * File entry.
     */
    static final class FileEntry {

        private final String path;
        private final String index;

        /**
         * Create a new entry.
         *
         * @param path  file path
         * @param index file index
         */
        FileEntry(String path, String index) {
            this.path = path;
            this.index = index;
        }

        /**
         * Create a new entry.
         *
         * @param path  file path
         * @param index file index
         */
        FileEntry(String path, int index) {
            this(path, Integer.toString(index));
        }

        /**
         * Get the file path.
         *
         * @return file path
         */
        String path() {
            return path;
        }

        /**
         * Get the file index.
         *
         * @return index
         */
        String index() {
            return index;
        }
    }
}
