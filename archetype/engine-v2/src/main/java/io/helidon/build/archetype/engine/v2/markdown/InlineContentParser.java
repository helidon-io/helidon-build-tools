package io.helidon.build.archetype.engine.v2.markdown;

public interface InlineContentParser {

    ParsedInline tryParse(InlineParser inlineParser);
}
