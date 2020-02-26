/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
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

import io.helidon.config.Config;

/**
 * Backend provider for {@link VuetifyBackend}.
 */
public class VuetifyBackendProvider implements BackendProvider {

    @Override
    public VuetifyBackend create(String name, Config node) {
        if (VuetifyBackend.BACKEND_NAME.equals(name)) {
            VuetifyBackend.Builder builder = VuetifyBackend.builder();
            if (node != null) {
                builder.config(node);
            }
            return builder.build();
        }
        return null;
    }
}
