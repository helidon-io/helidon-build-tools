/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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
package io.helidon.build.maven.sitegen.asciidoctor;

import java.util.Collection;

import io.helidon.build.maven.sitegen.RenderingException;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

/**
 * Asciidoc rendering exception.
 */
class AsciidocRenderingException extends RenderingException {

    /**
     * Create a new instance.
     *
     * @param msg    message
     * @param frames frames
     * @param ex     cause
     */
    AsciidocRenderingException(String msg, Collection<String> frames, Throwable ex) {
        super(String.format("%n%n%s%n%s", msg, renderStack(frames)), ex);
    }

    /**
     * Create a new instance.
     *
     * @param msg    message
     * @param frames frames
     */
    AsciidocRenderingException(String msg, Collection<String> frames) {
        super(String.format("%n%n%s%n%s", msg, renderStack(frames)));
    }

    private static String renderStack(Collection<String> frames) {
        return frames.stream()
                     .distinct()
                     .collect(joining(lineSeparator()));
    }
}
