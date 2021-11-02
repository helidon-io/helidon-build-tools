package io.helidon.build.archetype.engine.v2.markdown;

/**
 * Factory for a block node for determining when a block starts.
 */
interface BlockStartFactory {

    BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser);

}
