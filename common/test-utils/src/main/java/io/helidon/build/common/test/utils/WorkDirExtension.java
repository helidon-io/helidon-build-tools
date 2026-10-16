/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
package io.helidon.build.common.test.utils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import static io.helidon.build.common.FileUtils.location;

/**
 * JUnit extension that supports a {@link Path} working directory parameter.
 */
public class WorkDirExtension implements ParameterResolver {

    @Override
    public ExtensionContextScope getTestInstantiationExtensionContextScope(
            ExtensionContext rootContext) {
        return ExtensionContextScope.TEST_METHOD;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return Path.class.equals(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {

        Class<?> testClass = extensionContext.getRequiredTestClass();
        Method testMethod = extensionContext.getRequiredTestMethod();
        return newWorkDir(testClass, testMethod.getName());
    }

    private static Path newWorkDir(Class<?> declaringClass, String methodName) {
        try {
            Path targetDir = location(declaringClass).resolve("..").normalize();

            // ensure unique directory
            String prefix = dirName(declaringClass.getSimpleName());
            String suffix = dirName(methodName);
            Path workDir = targetDir.resolve(prefix).resolve(suffix);
            for (int i = 1; Files.exists(workDir); i++) {
                workDir = targetDir.resolve(prefix + "-" + i).resolve(suffix);
            }
            return Files.createDirectories(workDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String dirName(String str) {
        var sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (sb.length() > 0) {
                    sb.append('-');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
