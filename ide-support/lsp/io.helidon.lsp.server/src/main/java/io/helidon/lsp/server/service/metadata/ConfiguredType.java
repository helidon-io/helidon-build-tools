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

package io.helidon.lsp.server.service.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import io.helidon.config.metadata.ConfiguredOption;

/**
 * Detailed information about configured type in Helidon application.
 */
public final class ConfiguredType {

    private final Set<ConfiguredProperty> allProperties = new HashSet<>();
    private final List<ProducerMethod> producerMethods = new LinkedList<>();
    /**
     * The type that is built by a builder, or created using create method.
     */
    private final String targetClass;
    private final boolean standalone;
    private final String prefix;
    private final String description;
    private final List<String> provides;
    private final List<String> inherited = new LinkedList<>();

    ConfiguredType(String targetClass, boolean standalone, String prefix, String description, List<String> provides) {
        this.targetClass = targetClass;
        this.standalone = standalone;
        this.prefix = prefix;
        this.description = description;
        this.provides = provides;
    }

    private static String paramsToString(String[] params) {
        String result = Arrays.toString(params);
        if (result.startsWith("[") && result.endsWith("]")) {
            return result.substring(1, result.length() - 1);
        }
        return result;
    }

    /**
     * Short description about configured type.
     *
     * @return short description.
     */
    public String description() {
        return description;
    }

    static ConfiguredType create(JsonObject type) {
        ConfiguredType ct = new ConfiguredType(
                type.getString("type"),
                type.getBoolean("standalone", false),
                type.getString("prefix", null),
                type.getString("description", null),
                toList(type.getJsonArray("provides"))
        );

        List<String> producers = toList(type.getJsonArray("producers"));
        for (String producer : producers) {
            ct.addProducer(ProducerMethod.parse(producer));
        }
        List<String> inherits = toList(type.getJsonArray("inherits"));
        for (String inherit : inherits) {
            ct.addInherited(inherit);
        }

        JsonArray options = type.getJsonArray("options");
        for (JsonValue option : options) {
            ct.addProperty(ConfiguredProperty.create(option.asJsonObject()));
        }

        return ct;
    }

    private static List<String> toList(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>(array.size());

        for (JsonValue jsonValue : array) {
            result.add(((JsonString) jsonValue).getString());
        }

        return result;
    }

    ConfiguredType addProducer(ProducerMethod producer) {
        producerMethods.add(producer);
        return this;
    }

    ConfiguredType addProperty(ConfiguredProperty property) {
        allProperties.add(property);
        return this;
    }

    /**
     * List of producers.
     *
     * @return list of producers.
     */
    public List<ProducerMethod> producers() {
        return producerMethods;
    }

    /**
     * Set of configured properties.
     *
     * @return set of configured properties.
     */
    public Set<ConfiguredProperty> properties() {
        return allProperties;
    }

    /**
     * Target class.
     *
     * @return target class.
     */
    public String targetClass() {
        return targetClass;
    }

    /**
     * Is this configured type standalone.
     *
     * @return True if standalone, false otherwise.
     */
    public boolean standalone() {
        return standalone;
    }

    /**
     * Prefix for this configured type.
     *
     * @return prefix for this configured type.
     */
    public String prefix() {
        return prefix;
    }

    /**
     * List of providers.
     *
     * @return list of providers.
     */
    public List<String> provides() {
        return provides;
    }

    @Override
    public String toString() {
        return targetClass;
    }

    void addInherited(String classOrIface) {
        inherited.add(classOrIface);
    }

    /**
     * List of inherited types.
     *
     * @return list of inherited types.
     */
    public List<String> inherited() {
        return inherited;
    }

    static final class ProducerMethod {
        private final boolean isStatic;
        private final String owningClass;
        private final String methodName;
        private final String[] methodParams;

        ProducerMethod(boolean isStatic, String owningClass, String methodName, String[] methodParams) {
            this.isStatic = isStatic;
            this.owningClass = owningClass;
            this.methodName = methodName;
            this.methodParams = methodParams;
        }

        public static ProducerMethod parse(String producer) {
            int methodSeparator = producer.indexOf('#');
            String owningClass = producer.substring(0, methodSeparator);
            int paramBraceStart = producer.indexOf('(', methodSeparator);
            String methodName = producer.substring(methodSeparator + 1, paramBraceStart);
            int paramBraceEnd = producer.indexOf(')', paramBraceStart);
            String parameters = producer.substring(paramBraceStart + 1, paramBraceEnd);
            String[] methodParams = parameters.split(",");

            return new ProducerMethod(false,
                    owningClass,
                    methodName,
                    methodParams);
        }

        @Override
        public String toString() {
            return owningClass
                    + "#"
                    + methodName + "("
                    + paramsToString(methodParams) + ")";
        }
    }

    /**
     * Detailed information about configured property.
     */
    public static final class ConfiguredProperty {
        private final String builderMethod;
        private final String key;
        private final String description;
        private final String defaultValue;
        private final String type;
        private final boolean experimental;
        private final boolean optional;
        private final ConfiguredOption.Kind kind;
        private final List<AllowedValue> allowedValues;
        private final boolean provider;
        private final boolean merge;

        // if this is a nested type
        private ConfiguredType configuredType;
        private String outputKey;

        ConfiguredProperty(String builderMethod,
                           String key,
                           String description,
                           String defaultValue,
                           String type,
                           boolean experimental,
                           boolean optional,
                           ConfiguredOption.Kind kind,
                           boolean provider,
                           boolean merge,
                           List<AllowedValue> allowedValues) {
            this.builderMethod = builderMethod;
            this.key = key;
            this.description = description;
            this.defaultValue = defaultValue;
            this.type = type;
            this.experimental = experimental;
            this.optional = optional;
            this.kind = kind;
            this.allowedValues = allowedValues;
            this.outputKey = key;
            this.provider = provider;
            this.merge = merge;
        }

        /**
         * Create ConfiguredProperty from json object.
         *
         * @param json json object.
         * @return configuredProperty object.
         */
        public static ConfiguredProperty create(JsonObject json) {
            return new ConfiguredProperty(
                    json.getString("method", null),
                    json.getString("key", null),
                    json.getString("description"),
                    json.getString("defaultValue", null),
                    json.getString("type", "java.lang.String"),
                    json.getBoolean("experimental", false),
                    !json.getBoolean("required", false),
                    toKind(json.getString("kind", null)),
                    json.getBoolean("provider", false),
                    json.getBoolean("merge", false),
                    toAllowedValues(json.getJsonArray("allowedValues"))
            );
        }

        private static ConfiguredOption.Kind toKind(String kind) {
            if (kind == null) {
                return ConfiguredOption.Kind.VALUE;
            }
            return ConfiguredOption.Kind.valueOf(kind);
        }

        /**
         * List of allowed values for the property.
         *
         * @return list of allowed values for the property.
         */
        public List<AllowedValue> allowedValues() {
            return allowedValues;
        }

        private static List<AllowedValue> toAllowedValues(JsonArray allowedValues) {
            if (allowedValues == null) {
                return List.of();
            }
            List<AllowedValue> result = new ArrayList<>(allowedValues.size());

            for (JsonValue allowedValue : allowedValues) {
                JsonObject json = allowedValue.asJsonObject();
                result.add(new AllowedValue(json.getString("value"), json.getString("description", null)));
            }

            return result;
        }

        /**
         * Builder method.
         *
         * @return builder method.
         */
        public String builderMethod() {
            return builderMethod;
        }

        /**
         * Output Key.
         *
         * @return output key.
         */
        public String outputKey() {
            return outputKey;
        }

        /**
         * Key for the property.
         *
         * @return key for the property.
         */
        public String key() {
            return key;
        }

        /**
         * Set key for the property.
         *
         * @param key key for the property.
         */
        public void key(String key) {
            this.outputKey = key;
        }

        /**
         * Description for the property.
         *
         * @return description for the property.
         */
        public String description() {
            return description;
        }

        /**
         * Default value.
         *
         * @return default value.
         */
        public String defaultValue() {
            return defaultValue;
        }

        /**
         * Type of the property.
         *
         * @return type of the property.
         */
        public String type() {
            return type;
        }

        /**
         * Is the property experimental.
         *
         * @return true if experimental, false otherwise.
         */
        public boolean experimental() {
            return experimental;
        }

        /**
         * Is the property optional.
         *
         * @return true if optional, false otherwise.
         */
        public boolean optional() {
            return optional;
        }

        /**
         * Kind of the property.
         *
         * @return kind of the property.
         */
        public ConfiguredOption.Kind kind() {
            return kind;
        }

        /**
         * Is the property will be merged.
         *
         * @return true if it will be merged, false otherwise.
         */
        public boolean merge() {
            return merge;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ConfiguredProperty that = (ConfiguredProperty) o;
            return key.equals(that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }

        @Override
        public String toString() {
            return key;
        }

        /**
         * Is this property a provider.
         *
         * @return true if it is a provider, false otherwise.
         */
        public boolean provider() {
            return provider;
        }
    }

    /**
     * Allowed value for the configured type.
     */
    public static final class AllowedValue {
        private final String value;
        private final String description;

        private AllowedValue(String value, String description) {
            this.value = value;
            this.description = description;
        }

        /**
         * Allowed value.
         *
         * @return allowed value
         */
        public String value() {
            return value;
        }

        /**
         * Short description.
         *
         * @return short description.
         */
        public String description() {
            return description;
        }
    }
}
