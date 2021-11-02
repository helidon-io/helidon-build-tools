package io.helidon.build.archetype.engine.v2.markdown;

import java.util.List;

class InlineParserContext {

    private final List<DelimiterProcessor> delimiterProcessors;
    private final LinkReferenceDefinitions linkReferenceDefinitions;

    public InlineParserContext(List<DelimiterProcessor> delimiterProcessors,
                               LinkReferenceDefinitions linkReferenceDefinitions) {
        this.delimiterProcessors = delimiterProcessors;
        this.linkReferenceDefinitions = linkReferenceDefinitions;
    }

    /**
     * @return custom delimiter processors that have been configured with {@link Parser.Builder#customDelimiterProcessor(DelimiterProcessor)}
     */
    public List<DelimiterProcessor> getCustomDelimiterProcessors() {
        return delimiterProcessors;
    }

    /**
     * Look up a {@link LinkReferenceDefinition} for a given label.
     * <p>
     * Note that the label is not normalized yet; implementations are responsible for normalizing before lookup.
     *
     * @param label the link label to look up
     * @return the definition if one exists, {@code null} otherwise
     */
    public LinkReferenceDefinition getLinkReferenceDefinition(String label) {
        return linkReferenceDefinitions.get(label);
    }
}
