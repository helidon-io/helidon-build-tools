/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
package io.helidon.build.maven.assembly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.maven.plugins.assembly.filter.ContainerDescriptorHandler;
import org.apache.maven.plugins.assembly.utils.AssemblyFileUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;

import static java.util.stream.Collectors.toMap;

/**
 * Maven Assembly plugin custom handler for Helidon service registry.
 */
@Component(role = ContainerDescriptorHandler.class, hint = "service-registry")
public class HelidonContainerDescriptorHandler implements ContainerDescriptorHandler {

    private final Map<String, AssemblyPluginAggregator> aggregators = Stream
            .of(
                    new JsonArrayAggregator.ServiceRegistryAggregator(),
                    new JsonArrayAggregator.ConfigMetadataAggregator(),
                    new JsonArrayAggregator.FeatureMetadataAggregator(),
                    new ServiceLoaderAggregator(),
                    new SerialConfigAggregator()
            ).collect(toMap(AssemblyPluginAggregator::path, Function.identity()));

    private boolean excludeOverride = false;

    @Override
    public void finalizeArchiveCreation(Archiver archiver) throws ArchiverException {
        for (ResourceIterator it = archiver.getResources(); it.hasNext();) {
            it.next();
        }
        for (AssemblyPluginAggregator aggregator : aggregators.values()) {
            if (!aggregator.isEmpty()) {
                File file = aggregator.writeFile();
                excludeOverride = true;
                archiver.addFile(file, aggregator.path());
                excludeOverride = false;
            }
        }
    }

    @Override
    public void finalizeArchiveExtraction(UnArchiver unarchiver) throws ArchiverException {
    }

    @Override
    public List<String> getVirtualFiles() {
        return new ArrayList<>(aggregators.keySet());
    }

    @Override
    public boolean isSelected(FileInfo fileInfo) throws IOException {
        if (excludeOverride) {
            return true;
        }
        String normalizedName = AssemblyFileUtils.normalizeFileInfo(fileInfo);
        if (fileInfo.isFile() && aggregators.containsKey(normalizedName)) {
            aggregators.get(normalizedName)
                    .aggregate(fileInfo);
            return false;
        }
        return true;
    }

}
