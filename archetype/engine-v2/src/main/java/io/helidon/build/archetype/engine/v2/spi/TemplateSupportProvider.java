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

package io.helidon.build.archetype.engine.v2.spi;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;

import io.helidon.build.archetype.engine.v2.Context;
import io.helidon.build.archetype.engine.v2.MergedModel;

import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * Template support provider.
 */
public interface TemplateSupportProvider {

    /**
     * Get the name of the template support.
     *
     * @return name
     */
    String name();

    /**
     * Instantiate the template support.
     *
     * @param scope   scope
     * @param context context
     * @return template support
     */
    TemplateSupport create(MergedModel scope, Context context);

    /**
     * Provider cache.
     */
    class Cache {

        private Cache() {
        }

        /**
         * Template support providers cache.
         */
        static final Map<String, TemplateSupportProvider> PROVIDERS =
                ServiceLoader.load(TemplateSupportProvider.class)
                             .stream()
                             .map(ServiceLoader.Provider::get)
                             .collect(toUnmodifiableMap(TemplateSupportProvider::name, Function.identity()));
    }
}
