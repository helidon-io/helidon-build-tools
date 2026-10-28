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

import java.lang.reflect.Method;

import io.helidon.build.common.logging.LogLevel;
import io.helidon.build.common.logging.LogRecorder;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * JUnit extension that supports {@link io.helidon.build.common.logging.LogRecorder}.
 */
public class LogRecordExtension implements InvocationInterceptor, ParameterResolver {

    private static final Namespace NAMESPACE = Namespace.create(LogRecordExtension.class);

    @Override
    public ExtensionContextScope getTestInstantiationExtensionContextScope(
            ExtensionContext rootContext) {
        return ExtensionContextScope.TEST_METHOD;
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
                                    ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) throws Throwable {

        intercept(invocation, extensionContext);
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
                                            ReflectiveInvocationContext<Method> invocationContext,
                                            ExtensionContext extensionContext) throws Throwable {

        intercept(invocation, extensionContext);
    }

    private static void intercept(Invocation<Void> invocation, ExtensionContext extensionContext) throws Throwable {
        try (LogRecorder ignored = recorder(extensionContext).start()) {
            invocation.proceed();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {

        Class<?> paramType = parameterContext.getParameter().getType();
        return LogRecorder.class.equals(paramType);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {

        return recorder(extensionContext);
    }

    private static LogRecorder recorder(ExtensionContext ctx) {
        ExtensionContext.Store store = ctx.getStore(NAMESPACE);
        return store.getOrComputeIfAbsent(LogRecorder.class, k -> new LogRecorder(LogLevel.DEBUG), LogRecorder.class);
    }
}
