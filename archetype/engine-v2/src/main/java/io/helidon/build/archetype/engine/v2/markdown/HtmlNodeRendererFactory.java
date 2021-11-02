package io.helidon.build.archetype.engine.v2.markdown;

/**
 * Factory for instantiating new node renderers when rendering is done.
 */
interface HtmlNodeRendererFactory {

    /**
     * Create a new node renderer for the specified rendering context.
     *
     * @param context the context for rendering (normally passed on to the node renderer)
     * @return a node renderer
     */
    NodeRenderer create(HtmlNodeRendererContext context);
}
