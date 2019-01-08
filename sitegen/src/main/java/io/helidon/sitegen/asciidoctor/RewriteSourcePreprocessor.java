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
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Preprocessor;
import org.asciidoctor.extension.PreprocessorReader;

/**
 *
 */
public class RewriteSourcePreprocessor extends Preprocessor {

    @Override
    public void process(Document document, PreprocessorReader reader) {
        String includesAttr = (String) document.getAttribute("includes");

        /*
         * We want to overwrite the input file with our augmented
         * content. We get the augmented content not from the raw
         * input (i.e., not using reader.lines()) but from the
         * processed input (i.e., readLines()). But once we read that
         * it seems we have to restore it so the rest of the extensions
         * and the main processing can see the content.
         */
        List<String> processedContent = reader.readLines();
        String processedContentString = processedContent.stream()
                .collect(Collectors.joining(System.lineSeparator()));
        int i=6;
        reader.restoreLines(processedContent);
//        System.err.println(String.format("In rewrite preproc: processed output is ----\n%s\n----",
//                processedContent));
    }

}
