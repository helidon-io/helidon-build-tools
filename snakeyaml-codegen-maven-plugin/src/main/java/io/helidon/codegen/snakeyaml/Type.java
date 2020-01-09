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
 *
 */
package io.helidon.codegen.snakeyaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Models a type for which parsing code needs to be generated.
 * <p>
 *     Each type has a fully-qualified name for its interface and implementation and can have
 *     enums, property parameters (types for generics), property name substitutions, and might be
 *     referencable.
 * </p>
 * <p>
 *     This class and its inner classes are designed to be used as the model passed to the
 *     Mustache template processor. Hence field names are unique across all the classes; mustache
 *     currently provides no way to refer to a parent field that would otherwise be overridden by
 *     a lower-level use of the same name.
 * </p>
 */
class Type {

    private final String fullName;
    private final String simpleName;
    private final List<String> interfacesImplemented;
    private final boolean isInterface;

    private String implementationType;
    private boolean isRef = false;
    private final Map<String, TypeEnum> typeEnumsByType = new HashMap<>();
    private final List<PropertyParameter> propertyParameters = new ArrayList<>();

    // Prop subs are recorded here but are set from the caller, not by the compilation/analysis.
    private final List<PropertySubstitution> substitutions = new ArrayList<>();

    Type(String fullName, String simpleName, boolean isInterface, List<String> interfacesImplemented) {
        this.fullName = fullName;
        this.simpleName = simpleName;
        this.isInterface = isInterface;
        this.interfacesImplemented = interfacesImplemented;
    }

    Type typeEnumByType(String type) {
        typeEnumsByType.computeIfAbsent(type, TypeEnum::byType);
        return this;
    }

    String fullName() {
        return fullName;
    }

    String simpleName() {
        return simpleName;
    }

    List<String> interfacesImplemented() {
        return interfacesImplemented;
    }

    boolean isInterface() {
        return isInterface;
    }

    String implementationType() {
        return implementationType;
    }

    boolean isRef() {
        return isRef;
    }

    Map<String, TypeEnum> typeEnumsByType() {
        return typeEnumsByType;
    }

    List<PropertyParameter> propertyParameters() {
        return propertyParameters;
    }

    List<PropertySubstitution> substitutions() {
        return substitutions;
    }

    Optional<TypeEnum> getTypeEnum(String type) {
        return Optional.ofNullable(typeEnumsByType.get(type));
    }

    Type propertyParameter(String name, List<String> types) {
        propertyParameters.add(new PropertyParameter(name, types));
        return this;
    }

    Type propertySubstitution(String name, String type, String getter, String setter) {
        substitutions.add(new PropertySubstitution(name, type, getter, setter));
        return this;
    }

    Type ref(boolean isRef) {
        this.isRef = isRef;
        return this;
    }

    Type implementationType(String implType) {
        implementationType = implType;
        return this;
    }

    @Override
    public String toString() {
        return "Type{"
                + "fullName='" + fullName + '\''
                + ", simpleName='" + simpleName + '\''
                + ", interfacesImplemented=" + interfacesImplemented
                + ", isInterface=" + isInterface
                + ", implementationType='" + implementationType + '\''
                + ", isRef=" + isRef
                + ", typeEnumsByType=" + typeEnumsByType
                + ", propertyParameters=" + propertyParameters
                + ", substitutions=" + substitutions
                + '}';
    }

    static class TypeEnum {
        private String enumName;
        private String enumType;

        static TypeEnum byType(String enumType) {
            TypeEnum result = new TypeEnum();
            result.enumType = enumType;
            return result;
        }

        static TypeEnum fromName(String name) {
            TypeEnum result = new TypeEnum();
            result.enumName = name;
            return result;
        }

        private TypeEnum() {}

        TypeEnum name(String name) {
            enumName = name;
            return this;
        }

        TypeEnum type(String type) {
            enumType = type;
            return this;
        }

        String enumName() {
            return enumName;
        }

        String enumType() {
            return enumType;
        }

        @Override
        public String toString() {
            return "TypeEnum{"
                    + "enumName='" + enumName + '\''
                    + ", enumType='" + enumType + '\''
                    + '}';
        }
    }

    static class PropertyParameter {
        private final String parameterName;
        private final List<PropertyParameterType> parameterTypes = new ArrayList<>();

        static class PropertyParameterType {
            private final String parameterType;

            PropertyParameterType(String type) {
                parameterType = type;
            }

            String parameterType() {
                return parameterType;
            }

            @Override
            public String toString() {
                return "PropertyParameterType{"
                        + "parameterType='" + parameterType + '\''
                        + '}';
            }
        }

        PropertyParameter(String name, String... types) {
            this(name, Arrays.stream(types));
        }

        PropertyParameter(String name, List<String> types) {
            this(name, types.stream());
        }

        String parameterName() {
            return parameterName;
        }

        List<PropertyParameterType> parameterTypes() {
            return parameterTypes;
        }

        @Override
        public String toString() {
            return "PropertyParameter{"
                    + "parameterName='" + parameterName + '\''
                    + ", parameterTypes=" + parameterTypes
                    + '}';
        }

        private PropertyParameter(String name, Stream<String> types) {
            parameterName = name;
            types.map(PropertyParameterType::new)
                 .forEach(parameterTypes::add);
        }
    }

    static class PropertySubstitution {
        private final String propertySubName;
        private final String propertySubType;
        private final String getter;
        private final String setter;

        PropertySubstitution(String name, String type, String getter, String setter) {
            propertySubName = name;
            propertySubType = type;
            this.getter = getter;
            this.setter = setter;
        }

        String propertySubName() {
            return propertySubName;
        }

        String propertySubType() {
            return propertySubType;
        }

        String getter() {
            return getter;
        }

        String setter() {
            return setter;
        }

        @Override
        public String toString() {
            return "PropertySubstitution{"
                    + "propertySubName='" + propertySubName + '\''
                    + ", propertySubType='" + propertySubType + '\''
                    + ", getter='" + getter + '\''
                    + ", setter='" + setter + '\''
                    + '}';
        }
    }

}
