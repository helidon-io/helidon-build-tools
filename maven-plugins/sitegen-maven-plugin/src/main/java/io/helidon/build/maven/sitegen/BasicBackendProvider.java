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

import io.helidon.build.maven.sitegen.spi.BackendProvider;

/**
 * Backend provider for {@link BasicBackend}.
 */
public final class BasicBackendProvider implements BackendProvider {

    @Override
    public BasicBackend create(String name, Config node) {
        if (BasicBackend.BACKEND_NAME.equals(name)) {
            return BasicBackend.create();
        }
        return null;
    }
}
