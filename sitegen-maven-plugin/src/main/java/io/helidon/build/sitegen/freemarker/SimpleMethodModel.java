/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.sitegen.freemarker;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.asciidoctor.ast.ContentNode;

/**
 * A Freemarker template model for method invocations on {@link ContentNode}.
 *
 * @author rgrecour
 */
public class SimpleMethodModel implements TemplateMethodModelEx {

    private final BeansWrapper objectWrapper;
    private final Object object;
    private final String methodName;

    /**
     * Create a new simple method model instance.
     *
     * @param objectWrapper the object wrapper to use
     * @param object the object source on which the method will be invoked
     * @param methodName the name of the method to invoke
     */
    public SimpleMethodModel(BeansWrapper objectWrapper,
                             Object object,
                             String methodName) {
        Objects.requireNonNull(objectWrapper);
        this.objectWrapper = objectWrapper;
        Objects.requireNonNull(object);
        this.object = object;
        Objects.requireNonNull(methodName);
        this.methodName = methodName;
    }

    /**
     * Search if any method of the given name is available on the node.
     * @param object the object to search on
     * @param methodName the name of the method to search
     * @return true if the given node has any method for the given name, false
     * otherwise.
     */
    public static boolean hasMethodWithName(Object object, String methodName){
        for (Method m : object.getClass().getMethods()) {
            if (m.getName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {
        // get parameters and parameterTypes from args list
        int numArgs = arguments.size();
        Object[] parameters = new Object[numArgs];
        Class[] parameterTypes = new Class[numArgs];
        for (int i = 0; i < numArgs; i++) {
            Object arg = arguments.get(i);
            if (arg instanceof TemplateModel) {
                parameters[i] = objectWrapper.unwrap((TemplateModel) arg);
            } else if (arg == null) {
                parameters[i] = null;
            } else {
                throw new TemplateModelException(String.format(
                        "Unkown parameter type for method invocation: object=%s, methodname=%s, parameter=%s",
                        object,
                        methodName,
                        arg));
            }
            parameterTypes[i] = parameters[i] == null
                    ? null : parameters[i].getClass();
        }

        // find a method with matching parameters
        Method method = null;
        for (Method m : object.getClass().getMethods()) {
            if (methodName.equals(m.getName())) {
                int paramsOffset = m.getParameterCount() - numArgs;
                Class<?>[] mParameterTypes = m.getParameterTypes();
                if (!(paramsOffset == 0
                   || (paramsOffset == 1
                        && mParameterTypes[numArgs].isArray()))) {
                    // method params do not match
                    // or has more more but the last param is not an array
                    continue;
                }
                boolean paramsMatch = true;
                for (int i = 0; i < numArgs; i++) {
                    // treat null as a match
                    if (parameterTypes[i] == null) {
                        continue;
                    }
                    if (!mParameterTypes[i].isAssignableFrom(parameterTypes[i])) {
                        paramsMatch = false;
                        break;
                    }
                }
                if (paramsMatch) {
                    method = m;
                    if (paramsOffset == 1) {
                        // varargs, put an empty array of the right type
                        // as last parameter
                        Object[] newParameters = new Object[numArgs + 1];
                        System.arraycopy(parameters, 0, newParameters, 0,
                                numArgs);
                        newParameters[numArgs] = Array.newInstance(
                                m.getParameterTypes()[numArgs], 0);
                        parameters = newParameters;
                    }
                    break;
                }
            }
        }

        // throw an exception if no method found
        if (method == null) {
            throw new TemplateModelException(String.format(
                    "Unable to find method to invoke: object=%s, methodname=%s, parameters=%s",
                    object,
                    methodName,
                    arguments));
        }

        // invoke the method
        try {
            Object value = method.invoke(object, parameters);
            if (value == null) {
                return null;
            }
            return objectWrapper.wrap(value);
        } catch (SecurityException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException ex) {
            throw new TemplateModelException(String.format(
                    "Error during method invocation: object=%s, method=%s",
                    object, methodName),
                    ex);
        }
    }
}
