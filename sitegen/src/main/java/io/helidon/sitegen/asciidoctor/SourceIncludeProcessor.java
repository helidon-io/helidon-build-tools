/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package io.helidon.sitegen.asciidoctor;

import java.util.List;
import java.util.stream.Collectors;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.Treeprocessor;

/**
 *
 */
public class SourceIncludeProcessor extends Treeprocessor {

    private static final String LISTING_CONTEXT = "listing";
    private static final String STYLE_SOURCE = "source";

    @Override
    public Document process(Document document) {
        StringBuilder sb = new StringBuilder();
        for (StructuralNode sn : document.getBlocks()) {
            processNode(document, sn, sb);
        }

        return document;
    }

    private void processNode(Document document, StructuralNode sn, StringBuilder sb) {

        /*
         * Source listings will contain include-flagging comments and we need to
         * remove them or they will appear (confusingly) in the formatted output.
         */
        String context = sn.getContext();
        String style = sn.getStyle();
        Object content = sn.getContent();
        if (context.equals(LISTING_CONTEXT) && style.equals(STYLE_SOURCE)) {
            if (sn.isBlock()) {
                Block b = (Block) sn;
                List<String> origLines = b.getLines();
                List<String> newLines = origLines.stream()
                        .filter(line -> ! line.startsWith("// hd-include"))
                        .collect(Collectors.toList());
                if ( ! newLines.equals(origLines)) {
                    b.setLines(newLines);
                }
            }
        } else {
            if (sn.getBlocks() != null) {
                for (StructuralNode subSN : sn.getBlocks()) {
                    processNode(document, subSN, sb);
                }
            }
        }
    }

    private StringBuilder dumpNode(StringBuilder sb, StructuralNode sn, String indent) {
        sb.append(String.format("%scontext: %s, title: %s, style: %s%n%1$s'%s'%n%n",
                indent, sn.getContext(), sn.getTitle(), sn.getStyle(), sn.getContent()));
        if (sn.getBlocks() != null) {
            for (StructuralNode subNode : sn.getBlocks()) {
                dumpNode(sb, subNode, indent + "  ");
            }
        }
        return sb;
    }
}
