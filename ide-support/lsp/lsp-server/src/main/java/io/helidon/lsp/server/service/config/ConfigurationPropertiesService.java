/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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

import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.helidon.build.common.FileUtils;
import io.helidon.lsp.common.Dependency;
import io.helidon.lsp.server.management.MavenSupport;
import io.helidon.lsp.server.service.metadata.ConfigMetadata;
import io.helidon.lsp.server.service.metadata.ConfiguredOptionKind;
import io.helidon.lsp.server.service.metadata.ConfiguredType;
import io.helidon.lsp.server.service.metadata.ContainerConfigMetadata;
import io.helidon.lsp.server.service.metadata.MetadataProvider;
import io.helidon.lsp.server.service.metadata.ValueConfigMetadata;

/**
 * Service to obtain Helidon configuration properties.
 */
public class ConfigurationPropertiesService {

    private static final ConfigurationPropertiesService INSTANCE = new ConfigurationPropertiesService();
    private static final Logger LOGGER = Logger.getLogger(ConfigurationPropertiesService.class.getName());
    private static final Set<String> TYPED_VALUES = Set.of(
            "java.lang.Integer",
            "java.lang.Boolean",
            "java.lang.Long",
            "java.lang.Short",
            "java.lang.String",
            "java.net.URI",
            "java.lang.Class",
            "java.time.ZoneId");

    private final Map<String, WeakReference<Map<String, ConfigMetadata>>> cache;
    private MetadataProvider metadataProvider;
    private MavenSupport mavenSupport;

    /**
     * Create a new instance.
     */
    private ConfigurationPropertiesService() {
        cache = new HashMap<>();
        metadataProvider = MetadataProvider.instance();
        mavenSupport = MavenSupport.instance();
    }

    /**
     * Get the instance of the class.
     *
     * @return instance of the class.
     */
    public static ConfigurationPropertiesService instance() {
        return INSTANCE;
    }

    /**
     * Set metadataProvider.
     *
     * @param metadataProvider metadataProvider.
     */
    public void metadataProvider(MetadataProvider metadataProvider) {
        this.metadataProvider = metadataProvider;
    }

    /**
     * Set mavenSupport.
     *
     * @param mavenSupport mavenSupport.
     */
    public void mavenSupport(MavenSupport mavenSupport) {
        this.mavenSupport = mavenSupport;
    }

    /**
     * Clear cache for the pom.xml.
     *
     * @param pomFile pom.xml.
     */
    public void cleanCache(String pomFile) {
        cache.remove(pomFile);
    }

    /**
     * Obtain Helidon configuration properties for the pom file.
     *
     * @param pom {@code pom.xml}
     * @return Helidon configuration properties.
     */
    public Map<String, ConfigMetadata> metadataForPom(Path pom) {
        String pomPath = pom.toAbsolutePath().toString();
        if (cache.get(pomPath) != null) {
            Map<String, ConfigMetadata> result = cache.get(pomPath).get();
            if (result != null) {
                return result;
            }
        }

        long startTime = System.currentTimeMillis();

        List<ConfiguredType> configuredTypes = new LinkedList<>();
        Map<String, ConfiguredType> typesMap = new HashMap<>();
        Map<String, ConfigMetadata> configProperties = new LinkedHashMap<>();

        mavenSupport.dependencies(pom)
                .stream()
                .map(Dependency::path)
                .filter(d -> d.contains("helidon"))
                .forEach(dependency -> {
                    List<ConfiguredType> types = metadataProvider.readMetadata(dependency);
                    types.forEach(type -> {
                        typesMap.put(type.targetClass(), type);
                        configuredTypes.add(type);
                    });
                });

        for (ConfiguredType configuredType : configuredTypes) {
            if (configuredType.standalone()) {
                processType(configuredType, typesMap, configProperties);
            }
        }

        cache.put(pomPath, new WeakReference<>(configProperties));

        LOGGER.log(Level.FINEST,
                   "metadataForPom() for pom file {0} took {1} seconds",
                   new Object[] {pom, (double) (System.currentTimeMillis() - startTime) / 1000});

        return configProperties;
    }

    /**
     * Obtain Helidon configuration properties for the file from the Helidon project.
     *
     * @param uri project file
     * @return Helidon configuration properties
     */
    public Map<String, ConfigMetadata> metadataForFile(String uri) {
        try {
            Path pom = MavenSupport.resolvePom(FileUtils.pathOf(new URI(uri)));
            return pom != null ? metadataForPom(pom) : Map.of();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void processType(ConfiguredType configuredType,
                             Map<String, ConfiguredType> typesMap,
                             Map<String, ConfigMetadata> result) {

        ConfigMetadata data = new ContainerConfigMetadata(
                configuredType.prefix(),
                configuredType.targetClass(),
                null,
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

    private void processType(ConfiguredType configuredType,
                             Map<String, ConfiguredType> typesMap,
                             int level,
                             Map<String, ConfigMetadata> result) {

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

    private void processProperty(ConfiguredType.ConfiguredProperty property,
                                 Set<ConfiguredType.ConfiguredProperty> properties,
                                 Map<String, ConfiguredType> typesMap,
                                 int level,
                                 Map<String, ConfigMetadata> result) {

        if (property.kind() == ConfiguredOptionKind.LIST) {
            processListProperty(property, properties, typesMap, level, result);
            return;
        }

        if (property.kind() == ConfiguredOptionKind.MAP) {
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
                    processList(property, typesMap, level, result, nestedType);
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
                    property.allowedValues());
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
                property.allowedValues());
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
                    property.allowedValues());
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
                                null,
                                prepareDocs(provider.description(), true),
                                level + 1,
                                null))
                        .collect(Collectors.toSet()),
                property.kind(),
                property.allowedValues());
        result.put(getPath(metadata, result), metadata);

        for (ConfiguredType provider : providers) {
            if (provider.prefix() != null) {
                ConfigMetadata data = new ContainerConfigMetadata(
                        provider.prefix(),
                        provider.targetClass(),
                        null,
                        prepareDocs(provider.description(), true),
                        level + 1,
                        provider.properties()
                                .stream()
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
                    property.allowedValues());
            result.put(getPath(metadata, result), metadata);
        } else {
            ConfigMetadata metadata = new ValueConfigMetadata(
                    property.outputKey(),
                    property.defaultValue(),
                    property.type(),
                    prepareDocs(property.description(), property.optional()),
                    level,
                    children.entrySet()
                            .stream()
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
                    property.allowedValues());
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
            LOGGER.log(Level.INFO, "There are no modules on classpath providing ", property.type());
            ConfigMetadata metadata = new ValueConfigMetadata(
                    property.outputKey(),
                    property.defaultValue(),
                    property.type(),
                    prepareDocs(property.description(), property.optional()),
                    level,
                    null,
                    property.kind(),
                    property.allowedValues());
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
            processList(property, typesMap, level, result, listType);
        }
    }

    private void processList(ConfiguredType.ConfiguredProperty property,
                             Map<String, ConfiguredType> typesMap,
                             int level,
                             Map<String, ConfigMetadata> result,
                             ConfiguredType listType) {

        ConfigMetadata data = new ContainerConfigMetadata(
                property.outputKey(),
                property.type(),
                property.kind(),
                prepareDocs(property.description(), property.optional()),
                level,
                listType.properties()
                        .stream()
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
                property.allowedValues());
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
        if (!result.isEmpty()) {
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
}
