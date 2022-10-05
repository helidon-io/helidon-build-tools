/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.maven.sitegen;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import freemarker.template.TemplateException;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

/**
 * An exception to represent any error occurring as part of site processing.
 */
public class RenderingException extends RuntimeException {

    /**
     * Create a new instance.
     *
     * @param msg exception message
     */
    public RenderingException(String msg) {
        this(msg, true);
    }

    /**
     * Create a new instance.
     *
     * @param errors exceptions to aggregate
     */
    public RenderingException(List<RenderingException> errors) {
        this(errors.stream()
                   .map(Throwable::getMessage)
                   .collect(joining(lineSeparator())));
    }

    /**
     * Create a new instance.
     *
     * @param msg   exception message
     * @param cause cause
     */
    public RenderingException(String msg, Throwable cause) {
        this(msg, cause, true);
    }

    private RenderingException(String msg, boolean filterStackTrace) {
        super(msg);
        if (filterStackTrace) {
            setStackTrace(filteredStackTrace(getStackTrace()));
        }
    }

    private RenderingException(String msg, Throwable cause, boolean filterStackTrace) {
        super(msg, cause);
        if (filterStackTrace) {
            setStackTrace(filteredStackTrace(getStackTrace()));
        }
    }

    /**
     * Cleanup a cause.
     *
     * @param cause cause
     * @return new instance
     */
    public static Throwable cause(Throwable cause) {
        Deque<Throwable> causes = new ArrayDeque<>();
        while (cause != null) {
            causes.push(cause);
            cause = cause.getCause();
        }
        while (!causes.isEmpty()) {
            Throwable ex = causes.pop();
            StackTraceElement[] ste = filteredStackTrace(ex.getStackTrace());
            if (ex instanceof TemplateException) {
                String msg = filterMsg(ex.getMessage());
                if (cause != null) {
                    ex = new RenderingException(msg, cause, false);
                } else {
                    ex = new RenderingException(msg, false);
                }
            }
            ex.setStackTrace(ste);
            cause = ex;
        }
        return cause;
    }

    private static String filterMsg(String message) {
        return Arrays.stream(message.split("\\R"))
                     .limit(2).collect(Collectors.joining(System.lineSeparator()));
    }

    private static StackTraceElement[] filteredStackTrace(StackTraceElement[] elements) {
        return Arrays.stream(elements)
                     .filter(RenderingException::filterStackTrace)
                     .toArray(StackTraceElement[]::new);
    }

    private static boolean filterStackTrace(StackTraceElement elt) {
        return !elt.getClassName().startsWith("org.jruby")
                && (elt.getFileName() == null || !elt.getFileName().endsWith(".rb"));
    }
}
