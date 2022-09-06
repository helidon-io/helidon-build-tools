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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.helidon.config.metadata.ConfiguredOption;
import io.helidon.lsp.common.Dependency;
import io.helidon.lsp.server.management.MavenSupport;
import io.helidon.lsp.server.service.metadata.ConfigMetadata;
import io.helidon.lsp.server.service.metadata.ConfiguredType;
import io.helidon.lsp.server.service.metadata.ContainerConfigMetadata;
import io.helidon.lsp.server.service.metadata.MetadataProvider;
import io.helidon.lsp.server.service.metadata.ValueConfigMetadata;
import io.helidon.security.providers.common.EvictableCache;

/**
 * Service to obtain Helidon configuration properties.
 */
//TODO change or remove it
public class ConfigurationPropertiesService {

    private static ConfigurationPropertiesService INSTANCE = new ConfigurationPropertiesService();

    private static final Logger LOGGER = Logger.getLogger(ConfigurationPropertiesService.class.getName());
    private static final Set<String> TYPED_VALUES = Set.of(
            "java.lang.Integer",
            "java.lang.Boolean",
            "java.lang.Long",
            "java.lang.Short",
            "java.lang.String",
            "java.net.URI",
            "java.lang.Class",
            "java.time.ZoneId"
    );

    private final EvictableCache<String, Map<String, ConfigMetadata>> cache;
    private MetadataProvider metadataProvider;
    private MavenSupport mavenSupport;

    /**
     * Create a new instance.
     */
    private ConfigurationPropertiesService() {
        cache = EvictableCache.<String, Map<String, ConfigMetadata>>builder().build();
        metadataProvider = MetadataProvider.instance();
        mavenSupport = MavenSupport.instance();
    }

    public static ConfigurationPropertiesService instance() {
        return INSTANCE;
    }

    public void metadataProvider(MetadataProvider metadataProvider) {
        this.metadataProvider = metadataProvider;
    }

    public void mavenSupport(MavenSupport mavenSupport) {
        this.mavenSupport = mavenSupport;
    }

    public void cleanCache(String pomFile) {
        cache.remove(pomFile);
    }

    /**
     * Obtain Helidon configuration properties for the pom file.
     *
     * @param pom the pom file.
     * @return Helidon configuration properties.
     * @throws IOException IOException.
     */
    //TODO add tests if it is possible
    public Map<String, ConfigMetadata> metadataForPom(String pom) throws IOException {
        if (cache.get(pom).orElse(null) != null) {
            return cache.get(pom).orElse(Map.of());
        }

        List<String> dependencies = mavenSupport
                .getDependencies(pom).stream()
                .map(Dependency::path)
                .collect(Collectors.toList());
        dependencies = dependencies.stream().filter(d -> d.contains("helidon")).collect(Collectors.toList());
        List<ConfiguredType> configuredTypes = new LinkedList<>();
        Map<String, ConfiguredType> typesMap = new HashMap<>();
        Map<String, ConfigMetadata> configProperties = new LinkedHashMap<>();
        for (String dependency : dependencies) {
            List<ConfiguredType> types = metadataProvider.readMetadata(dependency);
            types.forEach(type -> {
                typesMap.put(type.targetClass(), type);
                configuredTypes.add(type);
            });
        }

        for (ConfiguredType configuredType : configuredTypes) {
            if (configuredType.standalone()) {
                processType(configuredType, typesMap, configProperties);
            }
        }

        cache.computeValue(pom, () -> Optional.of(configProperties));
        return configProperties;
    }

    private void processType(
            ConfiguredType configuredType,
            Map<String, ConfiguredType> typesMap,
            Map<String, ConfigMetadata> result
    ) {
        ConfigMetadata data = new ContainerConfigMetadata(
                configuredType.prefix(),
                configuredType.targetClass(),
                prepareDocs(configuredType.description(), true),
                0,
                configuredType.properties().stream()
                              .map(prop -> new ValueConfigMetadata(
                                      prop.key(),
                                      prop.defaultValue(),
                                      prop.type(),
                                      prepareDocs(prop.description(), prop.optional()),
                                      1,
                                      null,
                                      prop.kind(),
                                      prop.allowedValues()))
                              .collect(Collectors.toSet()));
        result.put(getPath(data, result), data);
        processType(configuredType, typesMap, 1, result);
    }

    private void processType(
            ConfiguredType configuredType,
            Map<String, ConfiguredType> typesMap,
            int level,
            Map<String, ConfigMetadata> result
    ) {
        Set<ConfiguredType.ConfiguredProperty> properties = configuredType.properties();

        for (ConfiguredType.ConfiguredProperty property : properties) {
            if (property.key() != null && property.key().contains(".*.")) {
                // this is a nested key, must be resolved by the parent list node
                continue;
            }
            processProperty(property, properties, typesMap, level, result);
        }
        List<String> inherited = configuredType.inherited();
        for (String inheritedTypeName : inherited) {
            ConfiguredType inheritedType = typesMap.get(inheritedTypeName);
            if (inheritedType == null) {
                LOGGER.log(Level.INFO, "Missing inherited type: {0}", inheritedTypeName);
            } else {
                processType(inheritedType, typesMap, level, result);
            }
        }
    }

    private void processProperty
            (ConfiguredType.ConfiguredProperty property,
             Set<ConfiguredType.ConfiguredProperty> properties,
             Map<String, ConfiguredType> typesMap,
             int level,
             Map<String, ConfigMetadata> result
            ) {
        if (property.kind() == ConfiguredOption.Kind.LIST) {
            processListProperty(property, properties, typesMap, level, result);
            return;
        }

        if (property.kind() == ConfiguredOption.Kind.MAP) {
            processMapProperty(property, level, result);
            return;
        }

        boolean typed = TYPED_VALUES.contains(property.type());
        if (!typed) {
            // this is a nested type, or a missing type
            ConfiguredType nestedType = typesMap.get(property.type());
            if (nestedType == null) {
                // either we have a list of allowed values, default value, or this is really a missing type
                processAllowedValuesOrMissing(property, typesMap, level, result);
            } else {
                // proper nested type
                if (property.merge()) {
                    processType(nestedType, typesMap, level, result);
                } else {
                    ConfigMetadata metadata = new ContainerConfigMetadata(
                            property.outputKey(),
                            property.type(),
                            prepareDocs(property.description(), property.optional()),
                            level,
                            nestedType.properties().stream()
                                      .map(prop -> new ValueConfigMetadata(
                                              prop.key(),
                                              prop.defaultValue(),
                                              prop.type(),
                                              prepareDocs(prop.description(), prop.optional()),
                                              level + 1,
                                              null,
                                              prop.kind(),
                                              prop.allowedValues()))
                                      .collect(Collectors.toSet()));
                    result.put(getPath(metadata, result), metadata);
                    processType(nestedType, typesMap, level + 1, result);
                }
            }
        } else {
            // this is a "leaf" node
            ConfigMetadata metadata = new ValueConfigMetadata(
                    property.outputKey(),
                    property.defaultValue(),
                    property.type(),
                    prepareDocs(property.description(), property.optional()),
                    level,
                    null,
                    property.kind(),
                    property.allowedValues()
            );
            result.put(getPath(metadata, result), metadata);
        }
    }

    private void processAllowedValuesOrMissing(ConfiguredType.ConfiguredProperty property,
                                               Map<String, ConfiguredType> typesMap,
                                               int level, Map<String, ConfigMetadata> result) {
        if (property.provider()) {
            processFromProvider(property, typesMap, level, result);
            return;
        }

        ConfigMetadata metadata = new ValueConfigMetadata(
                property.outputKey(),
                property.defaultValue(),
                property.type(),
                prepareDocs(property.description(), property.optional()),
                level,
                null,
                property.kind(),
                property.allowedValues()
        );
        result.put(getPath(metadata, result), metadata);
    }

    private void processListFromProvider(ConfiguredType.ConfiguredProperty property,
                                         Map<String, ConfiguredType> typesMap,
                                         int level,
                                         Map<String, ConfigMetadata> result) {
        // let's find all supported providers
        List<ConfiguredType> providers = new LinkedList<>();
        for (ConfiguredType value : typesMap.values()) {
            if (value.provides().contains(property.type())) {
                providers.add(value);
            }
        }

        if (providers.isEmpty()) {
            LOGGER.log(Level.INFO, "There are no modules on classpath providing {0}", property.type());
            ConfigMetadata metadata = new ValueConfigMetadata(
                    property.outputKey(),
                    property.defaultValue(),
                    property.type(),
                    prepareDocs(property.description(), property.optional()),
                    level,
                    null,
                    property.kind(),
                    property.allowedValues()
            );
            result.put(getPath(metadata, result), metadata);
            return;
        }

        ConfigMetadata metadata = new ValueConfigMetadata(
                property.outputKey(),
                property.defaultValue(),
                property.type(),
                prepareDocs(property.description(), property.optional()),
                level,
                providers.stream()
                         .map(provider -> new ContainerConfigMetadata(
                                 provider.prefix(),
                                 provider.targetClass(),
                                 prepareDocs(provider.description(), true),
                                 level + 1,
                                 null))
                         .collect(Collectors.toSet()),
                property.kind(),
                property.allowedValues()
        );
        result.put(getPath(metadata, result), metadata);

        for (ConfiguredType provider : providers) {
            if (provider.prefix() != null) {
                ConfigMetadata data = new ContainerConfigMetadata(
                        provider.prefix(),
                        provider.targetClass(),
                        prepareDocs(provider.description(), true),
                        level + 1,
                        provider.properties().stream()
                                .map(prop -> new ValueConfigMetadata(
                                        prop.key(),
                                        prop.defaultValue(),
                                        prop.type(),
                                        prepareDocs(prop.description(), prop.optional()),
                                        level + 2,
                                        null,
                                        prop.kind(),
                                        prop.allowedValues()))
                                .collect(Collectors.toSet()));
                result.put(getPath(data, result), data);
                processType(provider, typesMap, level + 2, result);
            } else {
                processType(provider, typesMap, level + 1, result);
            }
        }
    }

    private void processListFromTypes(ConfiguredType.ConfiguredProperty property,
                                      Set<ConfiguredType.ConfiguredProperty> properties,
                                      Map<String, ConfiguredType> typesMap,
                                      int level,
                                      Map<String, ConfigMetadata> result) {
        // this may be a list defined in configuration itself (*)
        String prefix = property.outputKey() + ".*.";
        Map<String, ConfiguredType.ConfiguredProperty> children = new HashMap<>();
        for (ConfiguredType.ConfiguredProperty configuredProperty : properties) {
            if (configuredProperty.outputKey().startsWith(prefix)) {
                children.put(configuredProperty.outputKey().substring(prefix.length()), configuredProperty);
            }
        }
        if (children.isEmpty()) {
            // this may be an array of primitive types / String
            ConfigMetadata metadata = new ValueConfigMetadata(
                    property.outputKey(),
                    property.defaultValue(),
                    property.type(),
                    prepareDocs(property.description(), property.optional()),
                    level,
                    null,
                    property.kind(),
                    property.allowedValues()
            );
            result.put(getPath(metadata, result), metadata);
        } else {
            ConfigMetadata metadata = new ValueConfigMetadata(
                    property.outputKey(),
                    property.defaultValue(),
                    property.type(),
                    prepareDocs(property.description(), property.optional()),
                    level,
                    children.entrySet().stream()
                            .map(entry -> new ValueConfigMetadata(
                                    entry.getKey(),
                                    entry.getValue().defaultValue(),
                                    entry.getValue().type(),
                                    prepareDocs(entry.getValue().description(), entry.getValue().optional()),
                                    level + 1,
                                    null,
                                    entry.getValue().kind(),
                                    entry.getValue().allowedValues()))
                            .collect(Collectors.toSet()),
                    property.kind(),
                    property.allowedValues()
            );
            result.put(getPath(metadata, result), metadata);

            for (var entry : children.entrySet()) {
                ConfiguredType.ConfiguredProperty element = entry.getValue();
                // we must modify the key
                element.key(entry.getKey());
                processProperty(element, properties, typesMap, level + 1, result);
            }
        }
    }

    private void processFromProvider(ConfiguredType.ConfiguredProperty property,
                                     Map<String, ConfiguredType> typesMap,
                                     int level, Map<String, ConfigMetadata> result) {
        // let's find all supported providers
        List<ConfiguredType> providers = new LinkedList<>();
        for (ConfiguredType value : typesMap.values()) {
            if (value.provides().contains(property.type())) {
                providers.add(value);
            }
        }

        if (providers.isEmpty()) {
            LOGGER.log(Level.INFO, "There are no modules on classpath providing ", property.type());
            ConfigMetadata metadata = new ValueConfigMetadata(
                    property.outputKey(),
                    property.defaultValue(),
                    property.type(),
                    prepareDocs(property.description(), property.optional()),
                    level,
                    null,
                    property.kind(),
                    property.allowedValues()
            );
            result.put(getPath(metadata, result), metadata);
            return;
        }

        for (ConfiguredType provider : providers) {
            if (provider.prefix() != null) {
                processType(provider, typesMap, level + 2, result);
            } else {
                processType(provider, typesMap, level + 1, result);
            }
        }
    }

    private void processListProperty(ConfiguredType.ConfiguredProperty property,
                                     Set<ConfiguredType.ConfiguredProperty> properties,
                                     Map<String, ConfiguredType> typesMap,
                                     int level,
                                     Map<String, ConfigMetadata> result) {
        ConfiguredType listType = typesMap.get(property.type());

        if (listType == null) {
            if (property.provider()) {
                processListFromProvider(property, typesMap, level, result);
            } else {
                processListFromTypes(property, properties, typesMap, level, result);
            }
        } else {
            ConfigMetadata data = new ContainerConfigMetadata(
                    property.outputKey(),
                    property.type(),
                    prepareDocs(property.description(), property.optional()),
                    level,
                    listType.properties().stream()
                            .map(prop -> new ValueConfigMetadata(
                                    prop.key(),
                                    prop.defaultValue(),
                                    prop.type(),
                                    prepareDocs(prop.description(), prop.optional()),
                                    level + 1,
                                    null,
                                    prop.kind(),
                                    prop.allowedValues()))
                            .collect(Collectors.toSet()));
            result.put(getPath(data, result), data);
            processType(listType, typesMap, level + 1, result);
        }
    }

    private void processMapProperty(ConfiguredType.ConfiguredProperty property,
                                    int level,
                                    Map<String, ConfigMetadata> result) {
        ConfigMetadata metadata = new ValueConfigMetadata(
                property.outputKey(),
                property.defaultValue(),
                property.type(),
                prepareDocs(property.description(), property.optional()),
                level,
                null,
                property.kind(),
                property.allowedValues()
        );
        result.put(getPath(metadata, result), metadata);
    }

    private String prepareDocs(String docs, boolean optional) {
        String description = (docs == null || docs.isBlank()) ? "" : docs;
        StringBuilder result = new StringBuilder();
        String spaces = "";

        if (!optional) {
            result.append("Required.");
            spaces = " ";
        }

        if (!description.isBlank()) {
            result.append(spaces).append(description.replace('\n', ' ').replaceAll("<.*>", ""));
        }

        if (result.length() > 0) {
            result.insert(0, "(").append(")");
        }

        return result.toString();
    }

    private String getPath(ConfigMetadata data, Map<String, ConfigMetadata> previousData) {
        StringBuilder path = new StringBuilder();
        Map<Integer, String> parents = new LinkedHashMap<>();
        previousData.values().forEach(prev -> parents.put(prev.level(), prev.key()));
        parents.entrySet().removeIf(e -> e.getKey() >= data.level());
        parents.values().forEach(parent -> path.append(parent).append("."));
        return path.append(data.key()).toString();
    }

    /**
     * Obtain Helidon configuration properties for the file from the Helidon project.
     *
     * @param fileUri the file from the Helidon project.
     * @return Helidon configuration properties.
     * @throws URISyntaxException URISyntaxException.
     * @throws IOException        IOException.
     */
    public Map<String, ConfigMetadata> metadataForFile(String fileUri) throws IOException, URISyntaxException {
        URI uri = new URI(fileUri);
        String pomForFile = MavenSupport.instance().getPomForFile(uri.getPath());
        return metadataForPom(pomForFile);
    }
}
