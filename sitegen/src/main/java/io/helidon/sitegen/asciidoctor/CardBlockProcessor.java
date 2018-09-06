/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.sitegen.asciidoctor;

import java.util.Arrays;
import java.util.Map;

import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Reader;

import static org.asciidoctor.extension.Contexts.OPEN;

/**
 * A {@link BlockProcessor} implementation that provides custom asciidoc syntax
 * for creating cards.
 *
 * @author rgrecour
 */
public class CardBlockProcessor extends BlockProcessor {

    /**
     * Create a new instance of {@link CardBlockProcessor}.
     */
    public CardBlockProcessor() {
        super("CARD");
        // this block is of type open (delimited by --)
        config.put(CONTEXTS, Arrays.asList(OPEN));
        setConfigFinalized();
    }

    @Override
    public Object process(StructuralNode parent,
                          Reader reader,
                          Map<String, Object> attributes) {

        // create a block with context "card", and put the parsed content into it
        Block block = this.createBlock(parent, "card", reader.readLines(),
                attributes);
        return block;
    }
}
