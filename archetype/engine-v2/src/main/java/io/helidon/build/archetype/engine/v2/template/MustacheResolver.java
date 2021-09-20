/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.build.archetype.engine.v2.template;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.helidon.build.archetype.engine.v2.MustacheHandler;
import io.helidon.build.archetype.engine.v2.MustacheTemplateEngine;
import io.helidon.build.archetype.engine.v2.TemplateEngine;
import io.helidon.build.archetype.engine.v2.descriptor.ValueType;

/**
 * Class used to render Preprocessed and external values.
 */
public class MustacheResolver {

    private static MergingMap<String, ValueType> valuesMap;
    private static MergingMap<String, TemplateMap> mapsMap;
    private static final Set<String> FILES = new HashSet<>();

    private MustacheResolver() {
    }

    /**
     * Render preprocessed values ignoring template files. Template files are stored into the
     * Set templateFiles and must be processed later with
     * {@link MustacheResolver#renderTemplateFiles(MergingMap, MergingMap, MergingMap, Set, Map)}.
     *
     * @param values            Model values
     * @param lists             Model lists
     * @param maps              Model maps
     * @param templateFiles     Template file list
     */
    public static void render(MergingMap<String, ValueType> values,
                              MergingMap<String, TemplateList> lists,
                              MergingMap<String, TemplateMap> maps,
                              Set<String> templateFiles) {
        valuesMap = values;
        mapsMap = maps;

        parseValues(values);
        parseLists(lists);
        parseMaps(maps);

        templateFiles.addAll(FILES);
        FILES.clear();
    }

    /**
     * Render template files from list templateFiles.
     *
     * @param values            Model values
     * @param lists             Model lists
     * @param maps              Model maps
     * @param templateFiles     Template files list to be rendered
     * @param scope             scope used by Mustache
     */
    public static void renderTemplateFiles(MergingMap<String, ValueType> values,
                                           MergingMap<String, TemplateList> lists,
                                           MergingMap<String, TemplateMap> maps,
                                           Set<String> templateFiles,
                                           Map<String, Object> scope) {

        TemplateEngine engine = new MustacheTemplateEngine();
        for (String file : templateFiles) {
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            try (InputStream fileStream = new FileInputStream(file)) {
                engine.render(fileStream, file,  StandardCharsets.UTF_8, content, scope);
            } catch (IOException e) {
                System.out.println("file not found");
            }
            injectIntoModel(values, lists, maps, content, file);
        }
    }

    private static void injectIntoModel(MergingMap<String, ValueType> values,
                                        MergingMap<String, TemplateList> lists,
                                        MergingMap<String, TemplateMap> maps,
                                        OutputStream content,
                                        String file) {
        findFileIntoValues(values, content, file);
        findFileIntoLists(lists, content, file);
        findFileIntoMaps(maps, content, file);
    }

    private static void findFileIntoMaps(MergingMap<String, TemplateMap> maps, OutputStream content, String file) {
        for (String key : maps.keySet()) {
            TemplateMap map = maps.get(key);
            findFileIntoValues(map.values(), content, file);
            findFileIntoLists(map.lists(), content, file);
            findFileIntoMaps(map.maps(), content, file);
        }
    }

    private static void findFileIntoLists(MergingMap<String, TemplateList> lists, OutputStream content, String file) {
        for (String key : lists.keySet()) {
            TemplateList list = lists.get(key);
            findFileIntoValues(list.values(), content, file);
            findFileIntoLists(list.lists(), content, file);
            findFileIntoMaps(list.maps(), content, file);
        }
    }

    private static void findFileIntoValues(MergingMap<String, ValueType> values, OutputStream content, String file) {
        for (String key : values.keySet()) {
            ValueType value = values.get(key);
            if (value.file() != null
                    && value.file().equals(file)
                    && value.template() != null
                    && value.template().equals(MustacheHandler.MUSTACHE)) {
                value.value(content.toString());
            }
        }
    }

    private static void findFileIntoValues(List<ValueType> values, OutputStream content, String file) {
        for (ValueType value : values) {
            if (value.file() != null
                    && value.file().equals(file)
                    && value.template() != null
                    && value.template().equals(MustacheHandler.MUSTACHE)) {
                value.value(content.toString());
            }
        }
    }

    private static void findFileIntoMaps(List<TemplateMap> maps, OutputStream content, String file) {
        for (TemplateMap map : maps) {
            findFileIntoValues(map.values(), content, file);
            findFileIntoLists(map.lists(), content, file);
            findFileIntoMaps(map.maps(), content, file);
        }
    }

    private static void findFileIntoLists(List<TemplateList> lists, OutputStream content, String file) {
        for (TemplateList list : lists) {
            findFileIntoValues(list.values(), content, file);
            findFileIntoLists(list.lists(), content, file);
            findFileIntoMaps(list.maps(), content, file);
        }
    }

    private static void parseMaps(MergingMap<String, TemplateMap> maps) {
        for (String key : maps.keySet()) {
            parseValues(maps.get(key).values());
            parseLists(maps.get(key).lists());
            parseMaps(maps.get(key).maps());
        }
    }

    private static void parseMaps(List<TemplateMap> maps) {
        for (TemplateMap map : maps) {
            parseValues(map.values());
            parseLists(map.lists());
            parseMaps(map.maps());
        }
    }

    private static void parseLists(MergingMap<String, TemplateList> lists) {
        for (String key : lists.keySet()) {
            parseValues(lists.get(key).values());
            parseLists(lists.get(key).lists());
            parseMaps(lists.get(key).maps());
        }
    }

    private static void parseLists(List<TemplateList> lists) {
        for (TemplateList list : lists) {
            parseValues(list.values());
            parseLists(list.lists());
            parseMaps(list.maps());
        }
    }

    private static void parseValues(List<ValueType> values) {
        for (ValueType templateValue : values) {
            lookForKey(templateValue);
        }
    }

    private static void parseValues(MergingMap<String, ValueType> values) {
        for (String key : values.keySet()) {
            lookForKey(values.get(key));
        }
    }

    private static void lookForKey(ValueType value) {
        if (value.template() != null && value.template().equals(MustacheHandler.MUSTACHE)) {
            if (value.value() != null && value.value().startsWith("{{") && value.value().endsWith("}}")) {
                String keyWord = value.value().substring(2, value.value().length() - 2);
                String resolved = inspectModel(keyWord);
                if (resolved != null) {
                    value.value(resolved);
                }
            }

            if (value.file() != null) {
                FILES.add(value.file());
            }
        }
    }

    private static String inspectModel(String keyWord) {
        String[] path = keyWord.split("\\.");
        MergingMap<String, TemplateMap> maps = mapsMap;
        MergingMap<String, ValueType> values = valuesMap;
        int pathIx = 0;

        while (pathIx < path.length) {
            if (values.containsKey(path[pathIx]) && pathIx == path.length - 1) {
                return values.get(path[pathIx]).value();
            }
            if (maps.containsKey(path[pathIx])) {
                values = maps.get(path[pathIx]).values();
                maps = maps.get(path[pathIx]).maps();
            }

            pathIx++;
        }

        return null;
    }
}
