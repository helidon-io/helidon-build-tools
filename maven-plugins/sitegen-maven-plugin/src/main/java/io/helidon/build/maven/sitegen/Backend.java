/*
 * Copyright (c) 2018, 2024 Oracle and/or its affiliates.
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

import java.nio.file.Path;
import java.util.List;

import io.helidon.build.common.LazyValue;
import io.helidon.build.common.Strings;

/**
 * Backend base class.
 */
public abstract class Backend implements Model {

    private final String name;
    private final LazyValue<List<PageRenderer>> renderers;

    /**
     * Create a new backend instance.
     *
     * @param name the name of the backend
     */
    protected Backend(String name) {
        this.name = Strings.requireValid(name, "name");
        renderers = new LazyValue<>(this::renderers);
    }

    /**
     * Get the backend name.
     *
     * @return the backend name
     */
    public String name() {
        return name;
    }

    /**
     * Generate.
     *
     * @param ctx context
     */
    public void generate(Context ctx) {
    }

    /**
     * Get the renderers.
     *
     * @return map of renderers keyed by extensions, never {@code null}
     */
    public List<PageRenderer> renderers() {
        return List.of(Context.get().site().engine().asciidoc().pageRenderer());
    }

    /**
     * Get a renderer for the given file.
     *
     * @param source the file to be processed by the renderer
     * @return the renderer associated with the file
     * @throws IllegalArgumentException if no renderer is found
     */
    public PageRenderer renderer(Path source) {
        for (PageRenderer renderer : renderers.get()) {
            if (renderer.supports(source)) {
                return renderer;
            }
        }
        throw new IllegalArgumentException("no renderer found for: " + source);
    }

    @Override
    public Object get(String attr) {
        throw new IllegalArgumentException("Unknown attribute: " + attr);
    }
}
