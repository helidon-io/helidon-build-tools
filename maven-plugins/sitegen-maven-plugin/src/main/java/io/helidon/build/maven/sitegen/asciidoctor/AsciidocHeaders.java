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
package io.helidon.build.maven.sitegen.asciidoctor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.build.common.Instance;
import io.helidon.build.common.logging.Log;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;

import static io.helidon.build.common.FileUtils.requireFile;

/**
 * A standalone include processor.
 */
final class AsciidocHeaders {

    private static final ThreadLocal<Deque<List<Document>>> REGISTRY = ThreadLocal.withInitial(ArrayDeque::new);
    private static final IncludeProcessorImpl PROCESSOR = new IncludeProcessorImpl();
    private static final Instance<Asciidoctor> ASCIIDOC = new Instance<>(() -> newAsciidoc(PROCESSOR));
    private static final Instance<Asciidoctor> RAW_ASCIIDOC = new Instance<>(() -> newAsciidoc(null));

    private AsciidocHeaders() {
    }

    /**
     * Read a document's header.
     *
     * @param source the document to read the header from
     * @return the header as {@code Map<String, Object>}, never {@code null}
     */
    static Map<String, Object> readDocumentHeader(Path source) {
        requireFile(source);
        Path baseDir = source.getParent();

        // make a partial document that contains up to the first h2 (==)
        List<String> headerLines = headerLines(source);

        Map<String, Object> headerMap = new HashMap<>();

        // parse h1
        headerLines.stream()
                   .filter(line -> line.startsWith("= "))
                   .map(line -> line.substring(2).trim())
                   .findFirst()
                   .ifPresent(h1 -> headerMap.put("h1", h1));

        // do a full pass on the partial document
        String header = String.join(System.lineSeparator(), headerLines);

        try {
            ArrayList<Document> documents = new ArrayList<>();
            REGISTRY.get().push(documents);
            Log.debug("Reading header " + source);
            try (Asciidoctor asciidoctor = ASCIIDOC.instance()) {
                Options options = options(Map.of()).baseDir(baseDir.toFile()).build();
                Document doc = asciidoctor.load(header, options);
                headerMap.putAll(doc.getAttributes());
                for (Document includedDoc : documents) {
                    headerMap.putAll(includedDoc.getAttributes());
                }
                return headerMap;
            }
        } finally {
            REGISTRY.get().pop();
        }
    }

    private static List<String> headerLines(Path source) {
        try (Stream<String> lines = Files.lines(source)) {
            return lines.takeWhile(line -> !line.startsWith("=="))
                        .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static Asciidoctor newAsciidoc(IncludeProcessor includeProcessor) {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        if (includeProcessor != null) {
            asciidoctor.javaExtensionRegistry().includeProcessor(includeProcessor);
        }
        AsciidocLogHandler.init();
        return asciidoctor;
    }

    private static OptionsBuilder options(Map<String, Object> attributes) {
        return Options.builder()
                      .attributes(Attributes.builder()
                                            .attributes(attributes)
                                            .skipFrontMatter(true)
                                            .experimental(true)
                                            .build())
                      .safe(SafeMode.UNSAFE)
                      .headerFooter(false)
                      .eruby("");
    }

    private static final class IncludeProcessorImpl extends IncludeProcessor {

        @Override
        public boolean handles(String target) {
            return true;
        }

        @Override
        public void process(Document doc, PreprocessorReader reader, String target, Map<String, Object> attributes) {
            try {
                Log.debug("Processing include " + target);
                Path targetPath = Path.of(target);
                if (!Files.exists(targetPath)) {
                    Log.warn("include file not found: " + target);
                    return;
                }
                String source = Files.readString(targetPath);
                try (Asciidoctor asciidoctor = RAW_ASCIIDOC.instance()) {
                    Options options = options(doc.getAttributes()).build();
                    Document included = asciidoctor.load(source, options);
                    List<Document> documents = REGISTRY.get().peek();
                    if (documents == null) {
                        throw new IllegalStateException("include context is not set!");
                    }
                    documents.add(included);
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }
}
