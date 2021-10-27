/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

import io.helidon.build.archetype.engine.v2.TemplateEngine;
import io.helidon.build.archetype.engine.v2.MustacheTemplateEngine;

/**
 * Helidon archetype engine.
 */
module io.helidon.build.archetype.engine.v2 {
    requires io.helidon.build.common;
    requires io.helidon.build.common.ansi;
    requires com.github.mustachejava;
    requires io.helidon.build.common.xml;
    requires java.logging;
    requires org.commonmark;

    exports io.helidon.build.archetype.engine.v2;
    exports io.helidon.build.archetype.engine.v2.prompter;
    exports io.helidon.build.archetype.engine.v2.archive;

    uses TemplateEngine;
    provides TemplateEngine with MustacheTemplateEngine;
}
