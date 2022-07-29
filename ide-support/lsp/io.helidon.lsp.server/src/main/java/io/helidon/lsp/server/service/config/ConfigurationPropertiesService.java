/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.lsp.server.service.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.helidon.lsp.common.Dependency;
import io.helidon.lsp.server.management.MavenSupport;
import io.helidon.lsp.server.model.ConfigurationMetadata;
import io.helidon.lsp.server.model.ConfigurationProperty;
import io.helidon.security.providers.common.EvictableCache;

import com.google.gson.Gson;

/**
 * Service to obtain Helidon configuration properties.
 */
public class ConfigurationPropertiesService {

    private static final Logger LOGGER = Logger.getLogger(ConfigurationPropertiesService.class.getName());
    private static final String HELIDON_PROPERTIES_FILE = "helidon-configuration-metadata.json";
    private final Gson gson;
    private final EvictableCache<String, List<ConfigurationMetadata>> cache;

    /**
     * Create a new instance.
     */
    public ConfigurationPropertiesService() {
        gson = new Gson();
        cache = EvictableCache.<String, List<ConfigurationMetadata>>builder().build();
    }

    /**
     * Obtain Helidon configuration properties from jar file.
     *
     * @param jarFilePath path to the jar file.
     * @return Helidon configuration properties.
     * @throws IOException IOException.
     */
    public ConfigurationMetadata getConfigMetadataFromJar(String jarFilePath) throws IOException {
        LOGGER.log(Level.FINEST, () -> "trying to read data from file " + jarFilePath);
        ConfigurationMetadata result = null;
        JarFile jarFile = new JarFile(jarFilePath);
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            if (entry.getName().contains(HELIDON_PROPERTIES_FILE)) {
                InputStream input = jarFile.getInputStream(entry);
                result = gson.fromJson(new InputStreamReader(input), ConfigurationMetadata.class);
            }
        }
        return result;
    }

    /**
     * Obtain Helidon configuration properties for the file from the Helidon project.
     *
     * @param fileUri the file from the Helidon project.
     * @return Helidon configuration properties.
     * @throws URISyntaxException URISyntaxException.
     * @throws IOException        IOException.
     */
    public List<ConfigurationMetadata> getConfigMetadataForFile(String fileUri) throws URISyntaxException, IOException {
        URI uri = new URI(fileUri);
        String pomForFile = MavenSupport.getInstance().getPomForFile(uri.getPath());

        //look in the cache
        List<ConfigurationMetadata> metadataList = cache.get(pomForFile).orElse(new ArrayList<>());
        if (!metadataList.isEmpty()) {
            return metadataList;
        }

        List<String> dependencies = MavenSupport.getInstance()
                                                .getDependencies(pomForFile).stream()
                                                .map(Dependency::path)
                                                .collect(Collectors.toList());
        dependencies = dependencies.stream().filter(d -> d.contains("helidon")).collect(Collectors.toList());
        ConfigurationPropertiesService service = new ConfigurationPropertiesService();
        for (String dependency : dependencies) {
            ConfigurationMetadata configMetadata = service.getConfigMetadataFromJar(dependency);
            if (configMetadata != null) {
                metadataList.add(configMetadata);
            }
        }

        cache.computeValue(pomForFile, () -> Optional.of(metadataList));
        return metadataList;
    }

    /**
     * Get top child properties part from the set of the properties for the given parent to property Map.
     *
     * @param properties set of the properties.
     * @param parent     string representation of the parent part.
     * @return top child properties part to property Map.
     */
    public Map<String, String> getTopChildPropertiesPartToPropertyMap(Set<String> properties, String parent) {
        return properties.stream()
                .map(property -> {
                    if (property.startsWith(parent + ".")) {
                        return Map.entry(
                                property.replaceFirst(parent + ".", ""),
                                property
                        );
                    }
                    return Map.entry(property, property);
                })
                .map(property -> {
                    if (property.getKey().contains(".")) {
                        return Map.entry(
                                property.getKey().substring(0, property.getKey().indexOf(".")),
                                property.getValue()
                        );
                    }
                    return property;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> newValue));
    }

    /**
     * Get top child properties part from the set of the properties for the given parent.
     *
     * @param properties set of the properties.
     * @param parent     string representation of the parent part.
     * @return set of the top child properties part.
     */
    public Set<String> getTopChildPropertiesParts(Set<String> properties, String parent) {
        return properties.stream()
                .map(property -> {
                    if (property.startsWith(parent + ".")) {
                        return property.replaceFirst(parent + ".", "");
                    }
                    return property;
                })
                .map(property -> {
                    if (property.contains(".")) {
                        return property.substring(0, property.indexOf("."));
                    }
                    return property;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Get bottom level of the property (for example for property  the result will be {@code grandchild}).
     *
     * @param property string representation of the property.
     * @return bottom level of the property.
     */
    public String keepBottomLevel(String property) {
        if (property.contains(".")) {
            return property.substring(property.lastIndexOf(".") + 1);
        }
        return property;
    }

    /**
     * Get bottom level for the set of the properties.
     *
     * @param properties set of the properties.
     * @return set of the bottom level of the property.
     */
    public Set<String> keepTopLevel(Set<String> properties) {
        return properties.stream()
                .map(property -> {
                    if (property.contains(".")) {
                        return property.substring(0, property.indexOf("."));
                    }
                    return property;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Get child properties for the given parent property.
     *
     * @param parentNodeProperty parent property.
     * @param allProps           properties.
     * @return child properties.
     */
    public Set<String> getChildProperties(String parentNodeProperty, Set<String> allProps) {
        String parentProperty = parentNodeProperty + ".";
        return allProps.stream()
                .filter(prop -> prop.startsWith(parentProperty))
                .collect(Collectors.toSet());
    }

    /**
     * Get root entries for the given set of the configuration properties.
     *
     * @param configProps set of the configuration properties.
     * @return set of the root entries.
     */
    public Set<String> getRootEntries(Set<String> configProps) {
        return configProps.stream()
                .map(config -> {
                    if (config.contains(".")) {
                        return config.substring(0, config.indexOf("."));
                    }
                    return config;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Extract property names from the list of the configuration metadata.
     *
     * @param configMetadataForFile list of the configuration metadata.
     * @return set of the property names.
     */
    public Set<String> extractPropertyNames(List<ConfigurationMetadata> configMetadataForFile) {
        if (configMetadataForFile == null) {
            return Collections.emptySet();
        }
        return configMetadataForFile.stream()
                .map(ConfigurationMetadata::getProperties)
                .flatMap(Collection::stream)
                .map(ConfigurationProperty::getName)
                .collect(Collectors.toSet());
    }
}
