/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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

package io.helidon.build.maven.enforcer;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Files found based on changes in git and local changes.
 * Result of {@link io.helidon.build.maven.enforcer.FileFinder#findFiles(java.nio.file.Path)}
 */
public class FoundFiles {
    private final Path gitRepoDir;
    private final List<FileRequest> fileRequests;
    private final Set<String> locallyModified;
    private final boolean useGit;

    @Override
    public String toString() {
        return "FoundFiles{"
                + "gitRepoDir=" + gitRepoDir
                + ", locallyModified=" + locallyModified
                + ", useGit=" + useGit
                + ", fileRequests=" + fileRequests
                + '}';
    }

    private FoundFiles(Path gitRepoDir, List<FileRequest> fileRequests, Set<String> locallyModified, boolean useGit) {
        this.gitRepoDir = gitRepoDir;
        this.fileRequests = fileRequests;
        this.locallyModified = locallyModified;
        this.useGit = useGit;
    }

    /**
     * Create new found files.
     *
     * @param gitRepoDir path of git repository root
     * @param fileRequests file requests to process
     * @param locallyModified list of files that were locally modified
     * @param useGit whether git is in use
     * @return new found files
     */
    public static FoundFiles create(Path gitRepoDir,
                                    List<FileRequest> fileRequests,
                                    Set<String> locallyModified,
                                    boolean useGit) {
        return new FoundFiles(gitRepoDir, fileRequests, locallyModified, useGit);
    }

    /**
     * Root of git repository.
     *
     * @return root
     */
    public Path gitRepoDir() {
        return gitRepoDir;
    }

    /**
     * Files to process.
     * @return files
     */
    public List<FileRequest> fileRequests() {
        return fileRequests;
    }

    /**
     * Locally modified paths (relative paths).
     *
     * @return locally modified files
     */
    public Set<String> locallyModified() {
        return locallyModified;
    }

    /**
     * Whether to use git.
     *
     * @return use git
     */
    public boolean useGit() {
        return useGit;
    }
}
