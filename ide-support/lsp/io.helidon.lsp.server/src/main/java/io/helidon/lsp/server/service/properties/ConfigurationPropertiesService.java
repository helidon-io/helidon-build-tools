package io.helidon.lsp.server.service.properties;

import com.google.gson.Gson;
import io.helidon.lsp.server.model.ConfigurationMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ConfigurationPropertiesService {

    private final Gson gson;
    private static final String HELIDON_PROPERTIES_FILE = "helidon-configuration-metadata.json";

    public ConfigurationPropertiesService() {
        gson = new Gson();
    }

    public ConfigurationMetadata getConfigMetadataFromJar(String jarFilePath) throws IOException {
        ConfigurationMetadata result = null;
        JarFile jarFile = new JarFile(jarFilePath);
        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            if (entry.getName().contains(HELIDON_PROPERTIES_FILE)) {
                InputStream input = jarFile.getInputStream(entry);
                result = gson.fromJson(new InputStreamReader(input), ConfigurationMetadata.class);
            }
        }
        return result;
    }
}
