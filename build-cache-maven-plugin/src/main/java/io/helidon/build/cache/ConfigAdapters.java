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
package io.helidon.build.cache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Config adapters.
 */
final class ConfigAdapters {

    private ConfigAdapters() {
    }

    /**
     * Create an {@link Xpp3Dom} adapter.
     *
     * @param orig dom
     * @return ConfigAdapter
     */
    static ConfigAdapter create(Xpp3Dom orig) {
        return new Xpp3DomAdapter(orig);
    }

    /**
     * Create an {@link Xpp3Dom} adapter.
     *
     * @param orig orig
     * @return ConfigAdapter
     */
    static ConfigAdapter create(PlexusConfiguration orig) {
        return new PlexusConfigurationAdapter(orig);
    }

    private static final class Xpp3DomAdapter implements ConfigAdapter {

        private final Xpp3Dom orig;

        Xpp3DomAdapter(Xpp3Dom orig) {
            this.orig = orig;
        }

        @Override
        public String name() {
            return orig.getName();
        }

        @Override
        public String value() {
            return orig.getValue();
        }

        @Override
        public Map<String, String> attributes() {
            Map<String, String> attributes = new HashMap<>();
            for (String attrName : orig.getAttributeNames()) {
                attributes.put(attrName, orig.getAttribute(attrName));
            }
            return attributes;
        }

        @Override
        public List<ConfigAdapter> children() {
            return Arrays.stream(orig.getChildren())
                    .map(Xpp3DomAdapter::new)
                    .collect(Collectors.toList());
        }
    }

    private static final class PlexusConfigurationAdapter implements ConfigAdapter {

        private final PlexusConfiguration orig;

        PlexusConfigurationAdapter(PlexusConfiguration orig) {
            this.orig = orig;
        }

        @Override
        public String name() {
            return orig.getName();
        }

        @Override
        public String value() {
            return orig.getValue();
        }

        @Override
        public Map<String, String> attributes() {
            Map<String, String> attributes = new HashMap<>();
            for (String attrName : orig.getAttributeNames()) {
                attributes.put(attrName, orig.getAttribute(attrName));
            }
            return attributes;
        }

        @Override
        public List<ConfigAdapter> children() {
            return Arrays.stream(orig.getChildren())
                    .map(PlexusConfigurationAdapter::new)
                    .collect(Collectors.toList());
        }
    }
}
