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

package io.helidon.build.common;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Represents a full type including generics declaration, to avoid information loss due to type erasure.
 * An object that represents any parameterized type may be obtained by sub-classing {@code GenericType}.
 *
 * @param <T> the generic type parameter
 */
public class GenericType<T> {

    private static final Map<Class<?>, WeakReference<?>> CACHE = new HashMap<>();

    private final Type type;
    private final Class<?> rawType;

    /**
     * Constructs a new generic type instance representing the given class.
     *
     * @param <N>   generic type of the returned GenericType
     * @param clazz the class to represent
     * @return new type wrapping the provided class
     */
    @SuppressWarnings("unchecked")
    public static <N> GenericType<N> create(Class<N> clazz) {
        return (GenericType<N>) CACHE.compute(clazz, (p, r) -> {
            if (r == null || r.get() == null) {
                return new WeakReference<>(new GenericType<>(clazz, clazz));
            }
            return r;
        }).get();
    }

    private GenericType(Type type, Class<?> rawType) {
        this.type = type;
        this.rawType = rawType;
    }

    /**
     * Constructs a new generic type, deriving the generic type and class from
     * type parameter. Note that this constructor is protected, users should create
     * a (usually anonymous) subclass as shown above.
     *
     * @throws IllegalArgumentException in case the generic type parameter value is not
     *                                  provided by any of the subclasses.
     */
    protected GenericType() throws IllegalArgumentException {
        this.type = typeArgument(getClass(), GenericType.class);
        this.rawType = rawClass(type);
    }

    /**
     * The type represented by this generic type instance.
     * <p>
     * For {@code new GenericType<List<String>>(){}}, this would return a {@link ParameterizedType}
     * for {@code java.util.List<java.lang.String>}.
     *
     * @return the actual type represented by this generic type instance.
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the object representing the class or interface that declared
     * the type represented by this generic type instance.
     * <p>
     * For {@code new GenericType<List<String>>(){}}, this would return an
     * {@code interface java.util.List}.
     *
     * @return the class or interface that declared the type represented by this
     * generic type instance.
     */
    public Class<?> rawType() {
        return rawType;
    }

    /**
     * Casts the parameter to the type of this generic type.
     * This is a utility method to use in stream processing etc.
     *
     * @param object instance to cast
     * @return typed instance
     * @throws ClassCastException in case the object is not of the expected type
     */
    @SuppressWarnings("unchecked")
    public T cast(Object object) throws ClassCastException {
        return (T) object;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof GenericType) {
            return ((GenericType<?>) obj).type.equals(this.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return type.toString();
    }

    private static Type typeArgument(Class<?> clazz, Class<?> baseClass) {
        // collect superclasses
        Stack<Type> superclasses = new Stack<>();
        Type currentType;
        Class<?> currentClass = clazz;
        do {
            currentType = currentClass.getGenericSuperclass();
            superclasses.push(currentType);
            if (currentType instanceof Class) {
                currentClass = (Class<?>) currentType;
            } else if (currentType instanceof ParameterizedType) {
                currentClass = (Class<?>) ((ParameterizedType) currentType).getRawType();
            }

            if (currentClass.equals(Object.class)) {
                break;
            }

            if (currentClass.equals(baseClass)) {
                break;
            }
        } while (true);

        // find which one supplies type argument and return it
        TypeVariable<?> tv = baseClass.getTypeParameters()[0];
        while (!superclasses.isEmpty()) {
            currentType = superclasses.pop();

            if (currentType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) currentType;
                Class<?> rawType = (Class<?>) pt.getRawType();
                int argIndex = Arrays.asList(rawType.getTypeParameters()).indexOf(tv);
                if (argIndex > -1) {
                    Type typeArg = pt.getActualTypeArguments()[argIndex];
                    if (typeArg instanceof TypeVariable) {
                        // type argument is another type variable - look for the value of that
                        // variable in subclasses
                        tv = (TypeVariable<?>) typeArg;
                        continue;
                    } else {
                        // found the value - return it
                        return typeArg;
                    }
                }
            }

            // needed type argument not supplied - break and throw exception
            break;
        }
        throw new IllegalArgumentException(String.format(
                "%s does not specify the type parameter T of GenericType<T>",
                currentType));
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType() instanceof Class) {
                return (Class<?>) parameterizedType.getRawType();
            }
        } else if (type instanceof GenericArrayType) {
            GenericArrayType array = (GenericArrayType) type;
            final Class<?> componentRawType = rawClass(array.getGenericComponentType());
            return getArrayClass(componentRawType);
        }
        throw new IllegalArgumentException(String.format(
                "Type parameter %s  is not a class or parameterized type whose raw type is a class",
                type));
    }

    private static Class<?> getArrayClass(Class<?> c) {
        try {
            Object o = Array.newInstance(c, 0);
            return o.getClass();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
