package io.helidon.build.archetype.engine.v2.markdown;

/**
 * Parser factory for a block node for determining when a block starts.
 */
public interface BlockParserFactory {

    BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser);

}
