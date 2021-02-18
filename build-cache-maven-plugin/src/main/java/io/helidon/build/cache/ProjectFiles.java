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
package io.helidon.build.cache;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import io.helidon.build.util.SourcePath;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Project files is the project files count and the most recent last modified timestamp.
 * It is used to compare the project files and invalidate the project state.
 */
final class ProjectFiles {

    static final String XML_ELEMENT_NAME = "project-files";

    private final int filesCount;
    private final long lastModified;
    private final String checksum;
    private final Map<String, String> allChecksums;

    /**
     * Create a new project files instance.
     *
     * @param filesCount   files count
     * @param lastModified most recent last modified timestamp
     * @param checksum     checksum of all files, may be {@code null}
     * @param allChecksums all checksum of individual files, may be {@code null}
     */
    ProjectFiles(int filesCount, long lastModified, String checksum, Map<String, String> allChecksums) {
        this.filesCount = filesCount;
        this.lastModified = lastModified;
        this.checksum = checksum;
        this.allChecksums = allChecksums == null ? Map.of() : allChecksums;
    }

    /**
     * Get the files count.
     *
     * @return file count
     */
    int filesCount() {
        return filesCount;
    }

    /**
     * Get the last modified timestamp.
     *
     * @return last modified timestamp
     */
    long lastModified() {
        return lastModified;
    }

    /**
     * Get the files checksum.
     *
     * @return checksum, or {@code null} if not available
     */
    String checksum() {
        return checksum;
    }

    /**
     * Get all the individual checksums.
     *
     * @return map of file path to checksum
     */
    Map<String, String> allChecksums() {
        return allChecksums;
    }

    /**
     * Create a diff between this instance and another.
     *
     * @param projectFiles projectFiles to diff
     * @return iterator of diff
     */
    ProjectFilesDiffs diff(ProjectFiles projectFiles) {
        return new ProjectFilesDiffs(this, projectFiles);
    }

    /**
     * Create an instance from an XML DOM element.
     *
     * @param elt XML DOM element
     * @return ProjectFiles
     */
    static ProjectFiles fromXml(Xpp3Dom elt) {
        int filesCount = 0;
        long lastModified = 0L;
        String checksum = null;
        Map<String, String> allChecksums = new HashMap<>();
        if (elt != null) {
            String filesCountAttr = elt.getAttribute("count");
            if (filesCountAttr != null) {
                filesCount = Integer.parseInt(filesCountAttr);
            }
            String lastModifiedAttr = elt.getAttribute("last-modified");
            if (lastModifiedAttr != null) {
                lastModified = Long.parseLong(lastModifiedAttr);
            }
            checksum = elt.getAttribute("checksum");
            for (Xpp3Dom fileElt : elt.getChildren("file")) {
                String fsum = fileElt.getAttribute("checksum");
                String fpath = fileElt.getValue();
                if (fsum != null && !fsum.isEmpty() && fpath != null && !fpath.isEmpty()) {
                    allChecksums.put(fpath, fsum);
                }
            }
        }
        return new ProjectFiles(filesCount, lastModified, checksum, allChecksums);
    }

    /**
     * Convert this instance to an XML DOM element.
     *
     * @return Xpp3Dom
     */
    Xpp3Dom toXml() {
        Xpp3Dom elt = new Xpp3Dom(XML_ELEMENT_NAME);
        elt.setAttribute("count", Integer.toString(filesCount));
        elt.setAttribute("last-modified", Long.toString(lastModified));
        if (checksum != null) {
            elt.setAttribute("checksum", checksum);
        }
        for (Entry<String, String> entry : allChecksums.entrySet()) {
            Xpp3Dom fileElt = new Xpp3Dom("file");
            fileElt.setValue(entry.getKey());
            fileElt.setAttribute("checksum", entry.getValue());
            elt.addChild(fileElt);
        }
        return elt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectFiles that = (ProjectFiles) o;
        return filesCount == that.filesCount
                && (lastModified == that.lastModified || Objects.equals(checksum, that.checksum));
    }

    @Override
    public int hashCode() {
        return Objects.hash(filesCount, lastModified, checksum);
    }

    /**
     * Compute the project files for a given project.
     *
     * @param project Maven project
     * @param session Maven session
     * @return ProjectFiles
     * @throws IOException if an IO error occurs
     */
    static ProjectFiles of(MavenProject project, MavenSession session) throws IOException {
        Path projectDir = project.getModel().getProjectDirectory().toPath();
        List<Path> modules = project.getModules()
                                    .stream()
                                    .map(projectDir::resolve)
                                    .collect(Collectors.toList());
        Path buildDir = project.getModel().getProjectDirectory().toPath()
                               .resolve(project.getModel().getBuild().getDirectory());
        CacheConfig config = CacheConfig.of(project, session);
        FileVisitorImpl visitor = new FileVisitorImpl(projectDir, buildDir, modules, config.projectFilesExcludes());
        Files.walkFileTree(projectDir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, visitor);
        long lastModified = 0;
        Map<String, String> fileChecksums = new HashMap<>();
        MD5 md5 = config.enableChecksums() ? new MD5() : null;
        Collections.sort(visitor.files);
        for (String f : visitor.files) {
            Path file = projectDir.resolve(f);
            long lm = Files.getLastModifiedTime(file).toMillis();
            if (lastModified < lm) {
                lastModified = lm;
            }
            if (config.includeAllChecksums()) {
                fileChecksums.put(f, MD5.checksum(file));
            }
            if (md5 != null) {
                md5.update(file);
            }
        }
        String checksums = md5 != null ? md5.toHexString() : null;
        return new ProjectFiles(visitor.files.size(), lastModified, checksums, fileChecksums);
    }

    private static final class FileVisitorImpl implements FileVisitor<Path> {

        private final Path projectDir;
        private final Path buildDir;
        private final List<Path> moduleDirs;
        private final List<String> excludes;
        private final List<String> files;

        FileVisitorImpl(Path projectDir, Path buildDir, List<Path> moduleDirs, List<String> excludes) {
            this.files = new LinkedList<>();
            this.projectDir = projectDir;
            this.buildDir = buildDir;
            this.moduleDirs = moduleDirs;
            this.excludes = excludes;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (moduleDirs.contains(dir) || dir.startsWith(buildDir)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (new SourcePath(projectDir, file).matches(null, excludes)) {
                files.add(projectDir.relativize(file).toString());
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }

    private static final class MD5 {

        private static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();
        private static ByteBuffer buffer;
        private final MessageDigest md;

        MD5() {
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex);
            }
        }

        MD5 update(Path file) throws IOException {
            RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r");
            FileChannel fc = raf.getChannel();
            if (buffer == null) {
                synchronized (MD5.class) {
                    if (buffer == null) {
                        buffer = ByteBuffer.allocate(4096);
                    }
                }
            }
            while (fc.read(buffer) > 0) {
                buffer.flip();
                md.update(buffer);
                buffer.clear();
            }
            buffer.clear();
            fc.close();
            raf.close();
            return this;
        }

        String toHexString() {
            byte[] bytes = md.digest();
            StringBuilder r = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                r.append(HEX_CODE[(b >> 4) & 0xF]);
                r.append(HEX_CODE[(b & 0xF)]);
            }
            return r.toString();
        }

        static String checksum(Path file) throws IOException {
            return new MD5().update(file).toHexString();
        }
    }
}
