package io.helidon.build.archetype.engine.v2.markdown;

import java.util.LinkedHashMap;
import java.util.Map;

class LinkReferenceDefinitions {

    // LinkedHashMap for determinism and to preserve document order
    private final Map<String, LinkReferenceDefinition> definitions = new LinkedHashMap<>();

    public void add(LinkReferenceDefinition definition) {
        String normalizedLabel = Escaping.normalizeLabelContent(definition.getLabel());

        // spec: When there are multiple matching link reference definitions, the first is used
        if (!definitions.containsKey(normalizedLabel)) {
            definitions.put(normalizedLabel, definition);
        }
    }

    public LinkReferenceDefinition get(String label) {
        String normalizedLabel = Escaping.normalizeLabelContent(label);
        return definitions.get(normalizedLabel);
    }
}
