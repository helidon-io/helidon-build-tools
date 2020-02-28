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

package io.helidon.build.sitegen.asciidoctor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Reader;

/**
 * A {@link BlockProcessor} implementation that provides custom asciidoc syntax
 * for creating pillars.
 */
public class PillarsBlockProcessor extends BlockProcessor {

    /**
     * This block is of type example (delimited by ====).
     */
    private static final Map<String, Object> CONFIG = createConfig(Contexts.EXAMPLE);

    /**
     * Create a new instance of {@link PillarsBlockProcessor}.
     */
    public PillarsBlockProcessor() {
        super("PILLARS", CONFIG);
        setConfigFinalized();
    }

    @Override
    public Object process(StructuralNode parent,
                          Reader reader,
                          Map<String, Object> attributes) {

        Map<Object, Object> opts = new HashMap<>();
        // means it can have nested blocks
        opts.put("content_model", "compound");
        // create an empty block with context "pillars"
        Block block = this.createBlock(parent, "pillars",
                Collections.emptyList(), attributes, opts);
        return block;
    }

    /**
     * Create a block processor configuration.
     * @param blockType the type of block
     * @return map
     */
    private static Map<String, Object> createConfig(String... blockTypes){
        Map<String, Object> config = new HashMap<>();
        config.put(Contexts.KEY, Arrays.asList(blockTypes));
        return config;
    }
}
