package io.helidon.build.archetype.engine.v2.template;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Map with unique key. For the same key, Object are merge together
 * instead of being replaced.
 *
 * @param <K>   key
 * @param <V>   Template Object
 */
public class MergingMap<K,V> extends HashMap<K,V> {

    @Override
    public V put(K key, V value) {
        if (this.containsKey(key)) {
            try {
                value = merge(value, this.get(key));
            } catch (IOException ignored) {
            }
        }
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        for (K key : map.keySet()) {
            this.put(key, map.get(key));
        }
    }

    private V merge(V first, V second) throws IOException {
        if (second == null) {
            return first;
        }
        if (first == null) {
            return second;
        }

        if (first instanceof TemplateValue) {
            return mergeValues((TemplateValue) first, (TemplateValue) second);
        }

        if (first instanceof TemplateList) {
            return mergeLists((TemplateList) first, (TemplateList) second);
        }

        if (first instanceof TemplateMap) {
            return mergeMaps((TemplateMap) first, (TemplateMap) second);
        }
        return null;
    }

    private V mergeMaps(TemplateMap first, TemplateMap second) {
        second.values().putAll(first.values());
        second.lists().putAll(first.lists());
        second.maps().putAll(first.maps());
        return (V) second;
    }

    private V mergeLists(TemplateList first, TemplateList second) {
        second.values().addAll(first.values());
        second.lists().addAll(first.lists());
        second.maps().addAll(first.maps());
        return (V) second;
    }

    private V mergeValues(TemplateValue first, TemplateValue second) throws IOException {
        String value = mergeValue(first, second);
        URL url = mergeURL(first, second);
        File file = mergeFile(first, second);
        String template = mergeTemplate(first, second);
        int order = mergeOrder(first, second);
        return (V) new TemplateValue(value, url, file, template, order);
    }

    private int mergeOrder(TemplateValue first, TemplateValue second) {
        return Math.min(first.order(), second.order());
    }

    private String mergeTemplate(TemplateValue first, TemplateValue second) {
        if (first.template().equals("mustache") || second.template().equals("mustache")) {
            return "mustache";
        }
        return first.template();
    }

    private File mergeFile(TemplateValue first, TemplateValue second) throws IOException {
        int read = 0;
        File mergedFile = Files.createTempFile("temp", "txt").toFile();
        OutputStream os = new FileOutputStream(mergedFile);
        InputStream is = new FileInputStream(first.file());
        while(read != -1) {
            read = is.read();
            os.write(read);
        }
        is = new FileInputStream(second.file());
        read = 0;
        while (read != -1) {
            read = is.read();
            os.write(read);
        }
        is.close();
        os.close();
        return mergedFile;
    }

    private URL mergeURL(TemplateValue first, TemplateValue second) {
        return null;
    }

    private String mergeValue(TemplateValue first, TemplateValue second) {
        return first.value() + second.value();
    }

}
