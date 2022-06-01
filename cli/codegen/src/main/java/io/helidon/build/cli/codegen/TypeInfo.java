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
package io.helidon.build.cli.codegen;

import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor9;
import javax.lang.model.util.SimpleTypeVisitor9;
import javax.lang.model.util.Types;

/**
 * Type information.
 */
interface TypeInfo extends Comparable<TypeInfo> {

    /**
     * Cache of pseudo type.
     */
    Map<String, PseudoTypeInfo> PSEUDO_CACHE = new HashMap<>();

    /**
     * Cache of class info.
     */
    Map<String, ClassInfo> CLASS_CACHE = new HashMap<>();

    /**
     * Cache of element info.
     */
    Map<String, ElementInfo> ELEMENT_CACHE = new HashMap<>();

    /**
     * Cache of composite type info.
     */
    Map<String, CompositeTypeInfo> COMPOSITE_CACHE = new HashMap<>();

    /**
     * Get a {@link TypeInfo} that describes a qualified name.
     *
     * @param qualifiedName type qualified name
     * @param loadClass     if {@code true} the class of the given name is loaded to create the type info, otherwise
     *                      a pseudo type info is created
     * @return TypeInfo
     */
    static TypeInfo of(String qualifiedName, boolean loadClass) {
        Objects.requireNonNull(qualifiedName, "qualifiedName is null");
        if (loadClass) {
            return CLASS_CACHE.computeIfAbsent(qualifiedName, ClassInfo::create);
        }
        return PSEUDO_CACHE.computeIfAbsent(qualifiedName, PseudoTypeInfo::new);
    }

    /**
     * Get a {@link TypeInfo} for a given {@link Class}.
     *
     * @param aClass       class to process
     * @param paramClasses explicit classes to use for the type parameters
     * @return TypeInfo
     */
    static ClassInfo of(Class<?> aClass, Class<?>... paramClasses) {
        Objects.requireNonNull(aClass, "class is null");
        String key = ClassInfo.cacheKey(aClass, paramClasses);
        return CLASS_CACHE.computeIfAbsent(key, k -> new ClassInfo(aClass, paramClasses));
    }

    /**
     * Get a {@link TypeInfo} for a given {@link Element}.
     *
     * @param element element to process
     * @param types   types processing utils
     * @return ElementInfo
     */
    static ElementInfo of(Element element, Types types) {
        if (element instanceof TypeElement) {
            return of((TypeElement) element, types);
        } else if (element instanceof VariableElement) {
            return of((VariableElement) element, types);
        }
        throw new IllegalArgumentException("Unsupported element type: " + element);
    }

    /**
     * Get a {@link TypeInfo} for a given {@link TypeElement}.
     *
     * @param typeElement type element to process
     * @param types       types processing utils
     * @return ElementInfo
     */
    static ElementInfo of(TypeElement typeElement, Types types) {
        Objects.requireNonNull(typeElement, "typeElement is null");
        Objects.requireNonNull(types, "types is null");
        String key = typeElement.getQualifiedName().toString();
        return ELEMENT_CACHE.computeIfAbsent(key, k -> new ElementInfo(typeElement, types));
    }

    /**
     * Get a {@link TypeInfo} for a given {@link VariableElement}.
     *
     * @param variableElement variable element to process
     * @param types           types processing utils
     * @return ElementInfo
     */
    static ElementInfo of(VariableElement variableElement, Types types) {
        Objects.requireNonNull(variableElement, "variableElement is null");
        Objects.requireNonNull(types, "types is null");
        String key = ElementInfo.cacheKey(variableElement);
        return ELEMENT_CACHE.computeIfAbsent(key, k -> new ElementInfo(variableElement, types));
    }

    /**
     * Get a {@link TypeInfo} that is composed by a type and type parameters.
     *
     * @param typeInfo   delegate type info
     * @param typeParams delegate type parameters
     * @return CompositeTypeInfo
     */
    static CompositeTypeInfo of(TypeInfo typeInfo, TypeInfo... typeParams) {
        Objects.requireNonNull(typeInfo, "typeInfo is null");
        String key = CompositeTypeInfo.cacheKey(typeInfo, typeParams);
        return COMPOSITE_CACHE.computeIfAbsent(key, k -> new CompositeTypeInfo(typeInfo, typeParams));
    }

    /**
     * Test if this type qualified name is equal to the given class name.
     *
     * @param aClass class to be compared
     * @return {@code true} if equal, {@code false} otherwise
     */
    default boolean is(Class<?> aClass) {
        if (qualifiedName().equals(aClass.getTypeName())) {
            return true;
        }
        if (isEnum()) {
            return superClass().map(s -> s.is(aClass)).orElse(false);
        }
        return false;
    }

    /**
     * Test if this type is compatible with any of the given classes (equal, interface, superclass).
     *
     * @param classes classes to be compared
     * @return {@code true} if equal, {@code false} otherwise
     */
    default boolean is(List<Class<?>> classes) {
        for (Class<?> aClass : classes) {
            if (is(aClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a flat list of all the types represented by this instance.
     *
     * @return list of type info
     */
    default List<TypeInfo> allTypes() {
        List<TypeInfo> allTypes = new LinkedList<>();
        allTypes.add(this);
        allTypes.addAll(Arrays.asList(interfaces()));
        allTypes.addAll(superClass().stream().flatMap(s -> s.allTypes().stream()).toList());
        return allTypes;
    }

    /**
     * Search this type and its parent for the given interface.
     *
     * @param aClass interface class to look for
     * @return {@code true} if found, {@code} false otherwise
     */
    default boolean hasInterface(Class<?> aClass) {
        return allTypes().stream().anyMatch(t -> t.is(aClass));
    }

    /**
     * Get the package of this type.
     *
     * @return package
     */
    String pkg();

    /**
     * Get the qualified name of this type.
     *
     * @return type qualified name
     */
    String qualifiedName();

    /**
     * Get the type name.
     *
     * @return type name
     */
    String typeName();

    /**
     * Get the simple name of this type.
     *
     * @return type simple name
     */
    String simpleName();

    /**
     * Indicate if this type describes a primitive type.
     *
     * @return {@code true} if primitive, {@code false} otherwise
     */
    boolean isPrimitive();

    /**
     * Indicate if this type describes an array type.
     *
     * @return {@code true} if array, {@code false} otherwise
     */
    boolean isArray();

    /**
     * Indicate if this type describes an interface.
     *
     * @return {@code true} if interface, {@code false} otherwise
     */
    boolean isInterface();

    /**
     * Indicate if this type describes an annotation.
     *
     * @return {@code true} if annotation, {@code false} otherwise
     */
    boolean isAnnotation();

    /**
     * Indicate if this type describes an enum type.
     *
     * @return {@code true} if enum, {@code false} otherwise
     */
    boolean isEnum();

    /**
     * Get the super class.
     *
     * @return optional of TypeInfo
     */
    Optional<? extends TypeInfo> superClass();

    /**
     * Get the type interfaces.
     *
     * @return TypeInfo[]
     */
    TypeInfo[] interfaces();

    /**
     * Get type info for the type parameters.
     *
     * @return TypeInfo[]
     */
    TypeInfo[] typeParams();

    @Override
    default int compareTo(TypeInfo o) {
        return qualifiedName().compareTo(o.qualifiedName());
    }

    /**
     * {@link TypeInfo} implementation for a pseudo type.
     */
    final class PseudoTypeInfo implements TypeInfo {

        private final String pkg;
        private final String qualifiedName;
        private final String typeName;
        private final String simpleName;
        private final boolean isArray;

        private PseudoTypeInfo(String qualifiedName) {
            if (qualifiedName.charAt(0) == '[' && qualifiedName.charAt(qualifiedName.length() - 1) == ';') {
                isArray = true;
                int startIndex = qualifiedName.indexOf('I');
                if (startIndex < 0) {
                    startIndex = qualifiedName.indexOf('L');
                    if (startIndex < 0 || qualifiedName.length() - startIndex < 2) {
                        throw new IllegalArgumentException("Invalid type qualified name: " + qualifiedName);
                    }
                }
                typeName = qualifiedName.substring(startIndex + 1, qualifiedName.length() - 1) + "[]";
            } else {
                isArray = false;
                typeName = qualifiedName;
            }
            this.qualifiedName = qualifiedName;
            int index = typeName.lastIndexOf('.');
            if (index < 0) {
                simpleName = typeName;
                pkg = "";
            } else {
                simpleName = typeName.substring(index + 1);
                pkg = typeName.substring(0, index);
            }
        }

        @Override
        public String pkg() {
            return pkg;
        }

        @Override
        public String qualifiedName() {
            return qualifiedName;
        }

        @Override
        public String typeName() {
            return typeName;
        }

        @Override
        public String simpleName() {
            return simpleName;
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public boolean isArray() {
            return isArray;
        }

        @Override
        public boolean isInterface() {
            return false;
        }

        @Override
        public boolean isAnnotation() {
            return false;
        }

        @Override
        public boolean isEnum() {
            return false;
        }

        @Override
        public Optional<TypeInfo> superClass() {
            return Optional.empty();
        }

        @Override
        public TypeInfo[] interfaces() {
            return new TypeInfo[0];
        }

        @Override
        public TypeInfo[] typeParams() {
            return new TypeInfo[0];
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PseudoTypeInfo that = (PseudoTypeInfo) o;
            return qualifiedName.equals(that.qualifiedName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(qualifiedName);
        }

        @Override
        public String toString() {
            return qualifiedName;
        }
    }

    /**
     * {@link TypeInfo} implementation based on an actual class.
     */
    final class ClassInfo implements TypeInfo {

        private final Class<?> aClass;
        private final Class<?>[] paramClasses;
        private final String pkg;
        private final String qualifiedName;
        private final String typeName;
        private final String simpleName;
        private TypeInfo[] typeParams;

        private ClassInfo(Class<?> aClass, Class<?>... paramClasses) {
            this.aClass = aClass;
            this.paramClasses = paramClasses;
            this.typeName = aClass.getTypeName();
            this.qualifiedName = aClass.getName();
            int pkgIndex = typeName.lastIndexOf(".");
            if (pkgIndex < 0) {
                this.pkg = "";
            } else {
                this.pkg = typeName.substring(0, pkgIndex);
            }
            this.simpleName = typeName.substring(pkgIndex + 1);
        }

        @Override
        public String pkg() {
            return pkg;
        }

        @Override
        public String qualifiedName() {
            return qualifiedName;
        }

        @Override
        public String typeName() {
            return typeName;
        }

        @Override
        public String simpleName() {
            return simpleName;
        }

        @Override
        public boolean isPrimitive() {
            return aClass.isPrimitive();
        }

        @Override
        public boolean isArray() {
            return aClass.isArray();
        }

        @Override
        public boolean isInterface() {
            return aClass.isInterface();
        }

        @Override
        public boolean isAnnotation() {
            return aClass.isAnnotation();
        }

        @Override
        public boolean isEnum() {
            return aClass.isEnum();
        }

        @Override
        public Optional<ClassInfo> superClass() {
            if (aClass.getSuperclass() != null) {
                return Optional.of(TypeInfo.of(aClass.getSuperclass()));
            }
            return Optional.empty();
        }

        @Override
        public TypeInfo[] interfaces() {
            return Arrays.stream(aClass.getInterfaces())
                         .map(TypeInfo::of)
                         .toArray(TypeInfo[]::new);
        }

        @Override
        public TypeInfo[] typeParams() {
            if (typeParams != null) {
                return typeParams;
            }
            typeParams = resolveTypeParams(this);
            return typeParams;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassInfo classInfo = (ClassInfo) o;
            return aClass.equals(classInfo.aClass) && Arrays.equals(paramClasses, classInfo.paramClasses);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(aClass);
            result = 31 * result + Arrays.hashCode(paramClasses);
            return result;
        }

        @Override
        public String toString() {
            return qualifiedName;
        }

        private static TypeInfo[] resolveTypeParams(ClassInfo classInfo) {
            TypeInfo[] typeParams;
            if (classInfo.paramClasses.length > 0) {
                typeParams = new TypeInfo[classInfo.paramClasses.length];
                for (int i = 0; i < classInfo.paramClasses.length; i++) {
                    typeParams[i] = TypeInfo.of(classInfo.paramClasses[i]);
                }
            } else {
                TypeVariable<? extends Class<?>>[] reflectedTypeParams = classInfo.aClass.getTypeParameters();
                typeParams = new TypeInfo[reflectedTypeParams.length];
                for (int i = 0; i < reflectedTypeParams.length; i++) {
                    String boundTypeName = reflectedTypeParams[i].getBounds()[0].getTypeName();
                    int paramIndex = boundTypeName.indexOf('<');
                    if (paramIndex > 0) {
                        boundTypeName = boundTypeName.substring(0, paramIndex);
                    }
                    if (boundTypeName.equals(classInfo.typeName)) {
                        typeParams[i] = classInfo;
                    } else {
                        typeParams[i] = TypeInfo.of(reflectedTypeParams[i].getBounds()[0].getTypeName(), true);
                    }
                }
            }
            return typeParams;
        }

        private static String cacheKey(Class<?> aClass, Class<?>... paramClasses) {
            String key = aClass.getTypeName();
            if (paramClasses.length > 0) {
                StringJoiner joiner = new StringJoiner(", ");
                for (Class<?> typeParam : paramClasses) {
                    String name = typeParam.getName();
                    joiner.add(name);
                }
                key += "<" + joiner + ">";
            }
            return key;
        }

        private static ClassInfo create(String qualifiedName) {
            try {
                return new ClassInfo(Class.forName(qualifiedName));
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    /**
     * {@link TypeInfo} implementation for the processing environment.
     */
    final class ElementInfo implements TypeInfo {

        private static final IsPrimitiveVisitor IS_PRIMITIVE_VISITOR = new IsPrimitiveVisitor();
        private static final IsNotTypeVisitor IS_NOT_TYPE_VISITOR = new IsNotTypeVisitor();

        private final TypeElement typeElement;
        private final Element element;
        private final Types types;
        private final String pkg;
        private final String qualifiedName;
        private final String simpleName;
        private String typeName;
        private TypeInfo[] typeParams;

        private ElementInfo(TypeElement element, Types types) {
            this.element = element;
            this.typeElement = element;
            this.types = types;
            qualifiedName = element.getQualifiedName().toString();
            simpleName = element.getSimpleName().toString();
            Element enclosingElement = element.getEnclosingElement();
            while (enclosingElement.getKind() != ElementKind.PACKAGE) {
                enclosingElement = enclosingElement.getEnclosingElement();
            }
            pkg = ((PackageElement) enclosingElement).getQualifiedName().toString();
        }

        private ElementInfo(VariableElement variableElement, Types types) {
            TypeMirror varType = variableElement.asType();
            boolean primitive = varType.accept(IS_PRIMITIVE_VISITOR, null);
            TypeElement type;
            if (primitive) {
                type = types.boxedClass((PrimitiveType) varType);
            } else {
                type = (TypeElement) types.asElement(variableElement.asType());
            }
            if (type == null) {
                throw new IllegalStateException("Unable to resolve type for variable: " + variableElement);
            }
            this.types = types;
            element = variableElement;
            typeElement = type;
            qualifiedName = type.getQualifiedName().toString();
            simpleName = type.getSimpleName().toString();
            pkg = resolvePackage(type);
        }

        /**
         * Get the described {@link Element}.
         *
         * @return Element
         */
        Element element() {
            return element;
        }

        @Override
        public String pkg() {
            return pkg;
        }

        @Override
        public String qualifiedName() {
            return qualifiedName;
        }

        @Override
        public String typeName() {
            if (typeName != null) {
                return typeName;
            }
            typeName = pkg + "." + simpleName;
            if (isArray()) {
                typeName += "[]";
            }
            return typeName;
        }

        @Override
        public String simpleName() {
            return simpleName;
        }

        @Override
        public boolean isPrimitive() {
            return typeElement.asType().accept(new IsPrimitiveVisitor(), null);
        }

        @Override
        public boolean isArray() {
            return typeElement.asType().getKind() == TypeKind.ARRAY;
        }

        @Override
        public boolean isInterface() {
            return typeElement.getKind() == ElementKind.INTERFACE;
        }

        @Override
        public boolean isAnnotation() {
            return typeElement.getKind() == ElementKind.ANNOTATION_TYPE;
        }

        @Override
        public boolean isEnum() {
            return typeElement.getKind() == ElementKind.ENUM;
        }

        @Override
        public Optional<ElementInfo> superClass() {
            TypeMirror superClassMirror = typeElement.getSuperclass();
            if (superClassMirror.getKind() == TypeKind.NONE) {
                return Optional.empty();
            }
            return Optional.of(TypeInfo.of((TypeElement) types.asElement(superClassMirror), types));
        }

        @Override
        public TypeInfo[] interfaces() {
            return typeElement.getInterfaces()
                              .stream()
                              .map(types::asElement)
                              .map(element -> TypeInfo.of((TypeElement) element, types))
                              .toArray(TypeInfo[]::new);
        }

        @Override
        public TypeInfo[] typeParams() {
            if (typeParams != null) {
                return typeParams;
            }
            typeParams = element.asType().accept(new TypeParamVisitor(), null);
            return typeParams;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ElementInfo that = (ElementInfo) o;
            return qualifiedName.equals(that.qualifiedName) && Arrays.equals(typeParams, that.typeParams);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(qualifiedName);
            result = 31 * result + Arrays.hashCode(typeParams());
            return result;
        }

        @Override
        public String toString() {
            return qualifiedName;
        }

        private static String cacheKey(VariableElement variableElement) {
            StringBuilder key = new StringBuilder(variableElement.toString());
            Element element = variableElement.getEnclosingElement();
            while (element.accept(IS_NOT_TYPE_VISITOR, null)) {
                key.insert(0, element + ".");
                element = element.getEnclosingElement();
            }
            key.insert(0, element + ".");
            return key.toString();
        }

        private static final class IsNotTypeVisitor extends SimpleElementVisitor9<Boolean, Void> {

            @Override
            protected Boolean defaultAction(Element e, Void v) {
                return true;
            }

            @Override
            public Boolean visitType(TypeElement e, Void unused) {
                return false;
            }
        }

        private static final class IsPrimitiveVisitor extends SimpleTypeVisitor9<Boolean, Void> {

            @Override
            protected Boolean defaultAction(TypeMirror e, Void v) {
                return false;
            }

            @Override
            public Boolean visitPrimitive(PrimitiveType t, Void v) {
                return true;
            }
        }

        private final class TypeParamVisitor extends SimpleTypeVisitor9<TypeInfo[], Void> {

            TypeInfo paramTypeInfo(TypeMirror type) {
                Element element;
                if (type.getKind() == TypeKind.TYPEVAR) {
                    element = types.asElement(((javax.lang.model.type.TypeVariable) type).getUpperBound());
                } else {
                    element = types.asElement(type);
                }
                return TypeInfo.of((TypeElement) element, types);
            }

            @Override
            public TypeInfo[] visitDeclared(DeclaredType type, Void p) {
                return type.getTypeArguments().stream()
                           .map(this::paramTypeInfo)
                           .toArray(TypeInfo[]::new);
            }
        }

        private static String resolvePackage(TypeElement type) {
            Element enclosingElement = type.getEnclosingElement();
            while (enclosingElement.getKind() != ElementKind.PACKAGE) {
                enclosingElement = enclosingElement.getEnclosingElement();
            }
            return ((PackageElement) enclosingElement).getQualifiedName().toString();
        }
    }

    /**
     * {@link TypeInfo} implementation that is composed a type delegate and type parameters delegates.
     */
    final class CompositeTypeInfo implements TypeInfo {

        private final TypeInfo type;
        private final TypeInfo[] typeParams;

        private CompositeTypeInfo(TypeInfo type, TypeInfo[] typeParams) {
            this.type = type;
            this.typeParams = typeParams;
        }

        @Override
        public String pkg() {
            return type.pkg();
        }

        @Override
        public String qualifiedName() {
            return type.qualifiedName();
        }

        @Override
        public String typeName() {
            return type.typeName();
        }

        @Override
        public String simpleName() {
            return type.simpleName();
        }

        @Override
        public boolean isPrimitive() {
            return type.isPrimitive();
        }

        @Override
        public boolean isArray() {
            return type.isArray();
        }

        @Override
        public boolean isInterface() {
            return type.isInterface();
        }

        @Override
        public boolean isAnnotation() {
            return type.isAnnotation();
        }

        @Override
        public boolean isEnum() {
            return type.isEnum();
        }

        @Override
        public Optional<? extends TypeInfo> superClass() {
            return type.superClass();
        }

        @Override
        public TypeInfo[] interfaces() {
            return new TypeInfo[0];
        }

        @Override
        public TypeInfo[] typeParams() {
            return typeParams;
        }

        @Override
        public String toString() {
            return type.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CompositeTypeInfo that = (CompositeTypeInfo) o;
            return Objects.equals(type, that.type) && Arrays.equals(typeParams, that.typeParams);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(type);
            result = 31 * result + Arrays.hashCode(typeParams);
            return result;
        }

        private static String cacheKey(TypeInfo type, TypeInfo[] typeParams) {
            String key = type.typeName();
            if (typeParams.length > 0) {
                StringJoiner joiner = new StringJoiner(", ");
                for (TypeInfo typeParam : typeParams) {
                    String name = typeParam.qualifiedName();
                    joiner.add(name);
                }
                key += "<" + joiner + ">";
            }
            return key;
        }
    }
}
