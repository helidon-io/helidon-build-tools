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

package io.helidon.build.sitegen;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import io.helidon.config.Config;

import static io.helidon.build.sitegen.Helper.checkNonNullNonEmpty;
import static io.helidon.common.CollectionsHelper.listOf;

/**
 * Service Provider Interface (SPI) for site backend.
 */
public interface BackendProvider {

    /**
     * Create the {@link Backend} instance from configuration.
     * @param name the backend name
     * @param node the {@link Config} node containing relevant configuration
     * @return the created backend if the name matches, {@code null} otherwise
     */
    Backend create(String name, Config node);

    /**
     * The built-in backends.
     */
    List<BackendProvider> BUILTINS =
            listOf(new BasicBackendProvider(), new VuetifyBackendProvider());

    /**
     * Get a backend by its name.
     * @param backendName the identity of the backend to retrieve
     * @param node the {@link Config} node containing relevant configuration
     * @return the backend instance
     * @throws IllegalArgumentException if no backend is found
     */
    static Backend get(String backendName, Config node){
        checkNonNullNonEmpty(backendName, "backend name");
        for (BackendProvider provider : BUILTINS) {
            Backend backend = provider.create(backendName, node);
            if (backend != null) {
                return backend;
            }
        }
        Iterator<BackendProvider> it = ServiceLoader
                .load(BackendProvider.class).iterator();
        while (it.hasNext()) {
            BackendProvider provider = it.next();
            Backend backend = provider.create(backendName, node);
            if (backend != null) {
                return backend;
            }
        }
        throw new IllegalArgumentException(
                "backend: " + backendName + "not found");
    }
}
