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

package io.helidon.build.archetype.engine.v2;

import io.helidon.build.archetype.engine.v2.ast.Block;
import io.helidon.build.archetype.engine.v2.spi.TemplateSupport;
import io.helidon.build.archetype.engine.v2.spi.TemplateSupportProvider;

/**
 * Template support provider for {@link MustacheSupport}.
 */
public class MustacheProvider implements TemplateSupportProvider {

    @Override
    public String name() {
        return "mustache";
    }

    @Override
    public TemplateSupport create(Block block, Context context) {
        return new MustacheSupport(block, context);
    }
}
