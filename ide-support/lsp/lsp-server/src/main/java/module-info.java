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

/**
 * Helidon Language Server.
 */
module io.helidon.lsp.server {
    requires java.logging;
    requires org.eclipse.lsp4j;
    requires org.eclipse.lsp4j.jsonrpc;
    requires io.helidon.build.common.maven;
    requires io.helidon.build.common;
    requires io.helidon.lsp.common;
    requires java.json;
    requires com.google.gson;
    requires helidon.archetype.engine.v2.json;
    opens io.helidon.lsp.server.service.config;
}
