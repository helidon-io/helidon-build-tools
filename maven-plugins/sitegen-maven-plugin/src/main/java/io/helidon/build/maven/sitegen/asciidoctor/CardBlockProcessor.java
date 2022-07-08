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

package io.helidon.build.maven.sitegen.asciidoctor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.build.common.logging.Log;

import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Reader;

/**
 * A {@link BlockProcessor} implementation that provides custom asciidoc syntax for creating cards.
 */
public class CardBlockProcessor extends BlockProcessor {

    /**
     * Marker text for generated block links.
     */
    public static final String BLOCK_LINK_TEXT = "@@blocklink@@";

    // This block is of type open (delimited by --).
    private static final Map<String, Object> CONFIG = createConfig(Contexts.OPEN);

    /**
     * Create a new instance.
     */
    public CardBlockProcessor() {
        super("CARD", CONFIG);
    }

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        Map<Object, Object> opts = new HashMap<>();
        // means it can have nested blocks
        opts.put("content_model", "compound");

        // create a block with context "card", and put the parsed content into it
        Block block = this.createBlock(parent, "card", reader.readLines(), attributes, opts);

        // if the link attribute is present
        // add a link into the content with a marker as text
        String link = (String) attributes.get("link");
        if (link != null) {
            String linkPhrase;
            String linkType = (String) attributes.get("link-type");
            if (linkType == null || linkType.equals("xref")) {
                linkPhrase = String.format("xref:%s[%s]", link, BLOCK_LINK_TEXT);
            } else if (linkType.equals("url")) {
                linkPhrase = String.format("link:%s[%s]", link, BLOCK_LINK_TEXT);
            } else {
                linkPhrase = null;
                Log.warn(link);
            }
            if (linkPhrase != null){
                parseContent(block, List.of(linkPhrase));
                // trigger rendering for the nested content here to trigger the
                // converter so that the converter can catch the generated
                // phrase node and add it as an attribute named _link to the
                // block
                block.getContent();
            }
        }
        return block;
    }

    private static Map<String, Object> createConfig(String... blockTypes){
        Map<String, Object> config = new HashMap<>();
        config.put(Contexts.KEY, Arrays.asList(blockTypes));
        return config;
    }
}
