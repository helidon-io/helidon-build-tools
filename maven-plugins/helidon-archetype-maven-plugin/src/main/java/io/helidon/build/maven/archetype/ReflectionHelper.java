/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * Reflection helper.
 */
class ReflectionHelper {

    private ReflectionHelper() {
    }

    /**
     * Invoke a static method via reflection.
     *
     * @param classLoader class loader
     * @param className   class name
     * @param methodName  method name
     * @param args        arguments
     * @return invocation return value
     */
    @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    static Object invokeMethod(ClassLoader classLoader, String className, String methodName, Object... args) {
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            ClassLoader cl = new ByteArrayClassLoader(classLoader);
            Class<?> clazz = cl.loadClass(className);
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }
            Method method = findMethod(clazz, methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(null, args);
        } catch (ClassNotFoundException
                 | NoSuchMethodException
                 | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(ex.getCause());
        } finally {
            Thread.currentThread().setContextClassLoader(ccl);
        }
    }

    private static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        for (Method method : clazz.getDeclaredMethods()) {
            Class<?>[] actualParamTypes = method.getParameterTypes();
            if (actualParamTypes.length == paramTypes.length) {
                boolean found = true;
                for (int i = 0; i < actualParamTypes.length; i++) {
                    if (!actualParamTypes[i].isAssignableFrom(paramTypes[i])) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    return method;
                }
            }
        }
        throw new NoSuchMethodException(clazz.getName() + "." + methodName);
    }

    private static class ByteArrayClassLoader extends ClassLoader {

        ByteArrayClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classBytes(name);
            if (bytes != null && bytes.length > 0) {
                return defineClass(name, bytes, 0, bytes.length);
            }
            return super.findClass(name);
        }

        private byte[] classBytes(String name) {
            URL resource = getClass().getResource("/" + name.replace('.', '/') + ".class");
            if (resource == null) {
                return new byte[0];
            }
            try (InputStream is = resource.openStream()) {
                return is.readAllBytes();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }
}
