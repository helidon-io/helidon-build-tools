package io.helidon.build.archetype.engine.v2.markdown;

import java.util.List;

class InlineParserContext {

    private final List<DelimiterProcessor> delimiterProcessors;

    public InlineParserContext(List<DelimiterProcessor> delimiterProcessors) {
        this.delimiterProcessors = delimiterProcessors;
    }

    /**
     * @return custom delimiter processors that have been configured with {@link Parser.Builder#customDelimiterProcessor(DelimiterProcessor)}
     */
    public List<DelimiterProcessor> getCustomDelimiterProcessors() {
        return delimiterProcessors;
    }
}
