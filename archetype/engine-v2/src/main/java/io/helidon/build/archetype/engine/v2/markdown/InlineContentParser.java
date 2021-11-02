package io.helidon.build.archetype.engine.v2.markdown;

interface InlineContentParser {

    ParsedInline tryParse(InlineParser inlineParser);
}
