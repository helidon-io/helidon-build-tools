package io.helidon.build.archetype.engine.v2.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parses input text to a tree of nodes.
 */
public class Parser {

    private final List<BlockStartFactory> blockStartFactories;
    private final List<DelimiterProcessor> delimiterProcessors;
    private final List<PostProcessor> postProcessors;

    private Parser(Builder builder) {
        this.blockStartFactories = DocumentParser.calculateBlockParserFactories(builder.blockStartFactories, builder.enabledBlockTypes);
        this.postProcessors = builder.postProcessors;
        this.delimiterProcessors = builder.delimiterProcessors;
    }

    /**
     * Parse the specified input text into a tree of nodes.
     *
     * @param input the text to parse - must not be null
     * @return the root node
     */
    public Node parse(String input) {
        if (input == null) {
            throw new NullPointerException("input must not be null");
        }
        DocumentParser documentParser = createDocumentParser();
        Node document = documentParser.parse(input);
        return postProcess(document);
    }

    private DocumentParser createDocumentParser() {
        return new DocumentParser(blockStartFactories, delimiterProcessors);
    }

    private Node postProcess(Node document) {
        for (PostProcessor postProcessor : postProcessors) {
            document = postProcessor.process(document);
        }
        return document;
    }

    /**
     * Create a new builder for configuring a {@link Parser}.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for configuring a {@link Parser}.
     */
    public static class Builder {
        private final List<BlockStartFactory> blockStartFactories = new ArrayList<>();
        private final List<DelimiterProcessor> delimiterProcessors = new ArrayList<>();
        private final List<PostProcessor> postProcessors = new ArrayList<>();
        private final Set<Class<? extends Block>> enabledBlockTypes = DocumentParser.getDefaultBlockParserTypes();

        /**
         * @return the configured {@link Parser}
         */
        public Parser build() {
            return new Parser(this);
        }

        /**
         * @param extensions extensions to use on this parser
         * @return {@code this}
         */
        public Parser.Builder extensions(Iterable<? extends Extension> extensions) {
            if (extensions == null) {
                throw new NullPointerException("extensions must not be null");
            }
            for (Extension extension : extensions) {
                if (extension instanceof Parser.ParserExtension) {
                    Parser.ParserExtension parserExtension = (Parser.ParserExtension) extension;
                    parserExtension.extend(this);
                }
            }
            return this;
        }

        /**
         * Adds a custom delimiter processor.
         * <p>
         * Note that multiple delimiter processors with the same characters can be added, as long as they have a
         * different minimum length. In that case, the processor with the shortest matching length is used. Adding more
         * than one delimiter processor with the same character and minimum length is invalid.
         *
         * @param delimiterProcessor a delimiter processor implementation
         * @return {@code this}
         */
        public Parser.Builder customDelimiterProcessor(
                DelimiterProcessor delimiterProcessor) {
            if (delimiterProcessor == null) {
                throw new NullPointerException("delimiterProcessor must not be null");
            }
            delimiterProcessors.add(delimiterProcessor);
            return this;
        }

        public Parser.Builder postProcessor(PostProcessor postProcessor) {
            if (postProcessor == null) {
                throw new NullPointerException("postProcessor must not be null");
            }
            postProcessors.add(postProcessor);
            return this;
        }
    }

    /**
     * Extension for {@link Parser}.
     */
    public interface ParserExtension extends Extension {
        void extend(Builder parserBuilder);
    }
}
