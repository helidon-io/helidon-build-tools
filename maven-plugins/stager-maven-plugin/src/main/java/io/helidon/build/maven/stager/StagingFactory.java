/*
 * Copyright (c) 2022, 2026 Oracle and/or its affiliates.
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
package io.helidon.build.maven.stager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import io.helidon.build.common.xml.XMLElement;

/**
 * Staging factory.
 */
final class StagingFactory implements XMLElement.Visitor {

    private final Deque<List<StagingTask>> frames = new ArrayDeque<>();

    private StagingFactory() {
        frames.push(new ArrayList<>());
    }

    /**
     * Create staging tasks from the given staging configuration.
     *
     * @param config config
     * @return tasks
     */
    static StagingTasks createTasks(XMLElement config) {
        var factory = new StagingFactory();
        config.visit(factory);
        for (StagingTask task : factory.frames.getFirst()) {
            if (task instanceof StagingTasks tasks) {
                return tasks;
            }
        }
        var element = XMLElement.builder().name("tasks").build();
        return new StagingTasks(element, factory.frames.getFirst());
    }

    @Override
    public boolean visitElement(XMLElement element) {
        frames.push(new ArrayList<>());
        return !"model".equals(element.name());
    }

    @Override
    public void postVisitElement(XMLElement element) {
        StagingTask task = createTask(element, frames.pop());
        if (task != null) {
            frames.getFirst().add(task);
        }
    }

    private StagingTask createTask(XMLElement element, List<StagingTask> tasks) {
        return switch (element.name()) {
            case "directory" -> new StagingDirectory(element, tasks);
            case "archive" -> new ArchiveTask(element, tasks);
            case "download" -> new DownloadTask(element);
            case "copy" -> new CopyTask(element);
            case "copy-artifact" -> new CopyArtifactTask(element);
            case "file" -> new FileTask(element, tasks);
            case "symlink" -> new SymlinkTask(element);
            case "template" -> new TemplateTask(element);
            case "unpack" -> new UnpackTask(element);
            case "unpack-artifact" -> new UnpackArtifactTask(element);
            case "list-files" -> new ListFilesTask(element);
            default -> switch (element.name()) {
                case "directories",
                     "archives",
                     "downloads",
                     "copies",
                     "copy-artifacts",
                     "files",
                     "symlinks",
                     "templates",
                     "unpacks",
                     "unpack-artifacts" -> new StagingTasks(element, tasks);
                default -> null;
            };
        };
    }
}
