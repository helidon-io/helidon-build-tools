/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
import java.util.List;
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

    private final List<PropertyParameter> propertyParameters = new ArrayList<>();

    // Prop subs are recorded here but are set from the caller, not by the compilation/analysis.
    private final List<PropertySubstitution> substitutions = new ArrayList<>();

    /**
     * Creates a new {@code Type}.
     *
     * @param fullName fully-qualified name of the type
     * @param simpleName short name of the type
     * @param isInterface whether the type represents an interface
     * @param interfacesImplemented names of all interfaces directly implemented (if the type is a class) or extended (if the
     *                              type is an interface)
     */
    Type(String fullName, String simpleName, boolean isInterface, List<String> interfacesImplemented) {
        this.fullName = fullName;
        this.simpleName = simpleName;
        this.isInterface = isInterface;
        this.interfacesImplemented = interfacesImplemented;
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

    List<PropertyParameter> propertyParameters() {
        return propertyParameters;
    }

    List<PropertySubstitution> propertySubstitutions() {
        return substitutions;
    }

    Type propertyParameter(String name, List<String> types) {
        propertyParameters.add(new PropertyParameter(name, types));
        return this;
    }

    Type propertySubstitution(String name, String type, String getter, String setter) {
        substitutions.add(new PropertySubstitution(name, type, getter, setter));
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
                + ", propertyParameters=" + propertyParameters
                + ", substitutions=" + substitutions
                + '}';
    }

    /**
     * Models the need for a SnakeYAML {@code PropertyParameter} to be added to a {@code TypeDescription}.
     * <p>
     *     A property parameter allows us to specify the type parameter(s) for a parameterized type such as a {@code List} or
     *     {@code Map}.
     * </p>
     */
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

    /**
     * Models the need for a property substitution on a type description.
     * <p>
     *     If the serialized property name (in YAML or JSON) does not match the property name derived from the type according
     *     to the bean pattern, then SnakeYAML provides a {@code PropertySubstitution} to prescribe the name of the property,
     *     its type, and its getter and setter method names.
     * </p>
     */
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
