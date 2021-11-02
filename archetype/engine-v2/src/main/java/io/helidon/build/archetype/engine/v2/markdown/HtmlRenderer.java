package io.helidon.build.archetype.engine.v2.markdown;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a tree of nodes to HTML.
 */
public class HtmlRenderer implements Renderer {

    private final UrlSanitizer urlSanitizer = new UrlSanitizer();
    private final List<HtmlNodeRendererFactory> nodeRendererFactories;

    private HtmlRenderer(Builder builder) {
        this.nodeRendererFactories = new ArrayList<>(builder.nodeRendererFactories.size() + 1);
        this.nodeRendererFactories.addAll(builder.nodeRendererFactories);
        // Add as last. This means clients can override the rendering of core nodes if they want.
        this.nodeRendererFactories.add(CoreHtmlNodeRenderer::new);
    }

    @Override
    public void render(Node node, Appendable output) {
        if (node == null) {
            throw new NullPointerException("node must not be null");
        }
        RendererContext context = new RendererContext(new HtmlWriter(output));
        context.render(node);
    }

    @Override
    public String render(Node node) {
        if (node == null) {
            throw new NullPointerException("node must not be null");
        }
        StringBuilder sb = new StringBuilder();
        render(node, sb);
        return sb.toString();
    }

    /**
     * Extension for {@link HtmlRenderer}.
     */
    public interface HtmlRendererExtension extends Extension {
        void extend(Builder rendererBuilder);
    }

    /**
     * Create a new builder for configuring an {@link HtmlRenderer}.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for configuring an {@link HtmlRenderer}. See methods for default configuration.
     */
    public static class Builder {

        private final List<HtmlNodeRendererFactory> nodeRendererFactories = new ArrayList<>();

        /**
         * @return the configured {@link HtmlRenderer}
         */
        public HtmlRenderer build() {
            return new HtmlRenderer(this);
        }

        /**
         * Add a factory for instantiating a node renderer (done when rendering). This allows to override the rendering
         * of node types or define rendering for custom node types.
         * <p>
         * If multiple node renderers for the same node type are created, the one from the factory that was added first
         * "wins". (This is how the rendering for core node types can be overridden; the default rendering comes last.)
         *
         * @param nodeRendererFactory the factory for creating a node renderer
         * @return {@code this}
         */
        public Builder nodeRendererFactory(HtmlNodeRendererFactory nodeRendererFactory) {
            if (nodeRendererFactory == null) {
                throw new NullPointerException("nodeRendererFactory must not be null");
            }
            this.nodeRendererFactories.add(nodeRendererFactory);
            return this;
        }

        /**
         * @param extensions extensions to use on this HTML renderer
         * @return {@code this}
         */
        public Builder extensions(Iterable<? extends Extension> extensions) {
            if (extensions == null) {
                throw new NullPointerException("extensions must not be null");
            }
            for (Extension extension : extensions) {
                if (extension instanceof HtmlRendererExtension) {
                    HtmlRendererExtension htmlRendererExtension = (HtmlRendererExtension) extension;
                    htmlRendererExtension.extend(this);
                }
            }
            return this;
        }
    }

    private class RendererContext implements HtmlNodeRendererContext {

        private final HtmlWriter htmlWriter;
        private final NodeRendererMap nodeRendererMap = new NodeRendererMap();

        private RendererContext(HtmlWriter htmlWriter) {
            this.htmlWriter = htmlWriter;

            // The first node renderer for a node type "wins".
            for (int i = nodeRendererFactories.size() - 1; i >= 0; i--) {
                HtmlNodeRendererFactory nodeRendererFactory = nodeRendererFactories.get(i);
                NodeRenderer nodeRenderer = nodeRendererFactory.create(this);
                nodeRendererMap.add(nodeRenderer);
            }
        }

        @Override
        public UrlSanitizer urlSanitizer() {
            return urlSanitizer;
        }

        @Override
        public String encodeUrl(String url) {
            return Escaping.percentEncodeUrl(url);
        }

        @Override
        public Map<String, String> extendAttributes(Node node, String tagName, Map<String, String> attributes) {
            return new LinkedHashMap<>(attributes);
        }

        @Override
        public HtmlWriter getWriter() {
            return htmlWriter;
        }

        @Override
        public void render(Node node) {
            nodeRendererMap.render(node);
        }
    }
}
