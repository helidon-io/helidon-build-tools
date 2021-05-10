package io.helidon.lsp.server.service.properties;

import io.helidon.lsp.server.core.LanguageServerContext;
import io.helidon.lsp.server.management.MavenSupport;
import io.helidon.lsp.server.model.ConfigurationMetadata;
import io.helidon.lsp.server.service.TextDocumentHandler;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.InsertTextFormat;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PropertiesTextDocumentHandler implements TextDocumentHandler {

    private final static String separator = "=";
    private final LanguageServerContext languageServerContext;

    public PropertiesTextDocumentHandler(LanguageServerContext languageServerContext) {
        this.languageServerContext = languageServerContext;
    }

    @Override
    public List<CompletionItem> completion(CompletionParams position) {
        List<CompletionItem> completionItems = new ArrayList<>();
        try {
            //TODO move obtaining ConfigurationMetadata to other class
            URI uri = new URI(position.getTextDocument().getUri());
            String pomForFile = MavenSupport.getInstance().getPomForFile(uri.getPath());
            List<String> dependencies = MavenSupport.getInstance().getDependencies(pomForFile);
            dependencies = dependencies.stream().filter(d -> d.contains("helidon")).collect(Collectors.toList());
            ConfigurationPropertiesService service = new ConfigurationPropertiesService();
            List<ConfigurationMetadata> metadataList = new ArrayList<>();
            for (String dependency : dependencies) {
                metadataList.add(service.getConfigMetadataFromJar(dependency));
            }

            for (ConfigurationMetadata metadata : metadataList) {
                if (metadata.getProperties() == null) {
                    continue;
                }
                metadata.getProperties().stream().forEach(property -> {
                    CompletionItem item = new CompletionItem();
                    item.setKind(CompletionItemKind.Snippet);
                    item.setLabel(property.getName());
                    item.setDetail(property.getType());
                    item.setInsertTextFormat(InsertTextFormat.Snippet);
                    //TODO add hints and default value
                    item.setInsertText(property.getName() + separator);
                    completionItems.add(item);
                });
            }
        } catch (Exception e) {
            //TODO: Handle the exception.
        }
        return completionItems;
    }
}
