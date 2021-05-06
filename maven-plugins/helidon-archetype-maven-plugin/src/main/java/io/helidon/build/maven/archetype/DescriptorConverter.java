/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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
package io.helidon.build.maven.archetype;

import java.io.Writer;

import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor;
import io.helidon.build.archetype.engine.v1.ArchetypeDescriptor.Property;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;

/**
 * Converts an {@link ArchetypeDescriptor} to a minimal {@code archetype-metadata.xml}.
 */
final class DescriptorConverter {

    /**
     * Cannot be instantiated.
     */
    private DescriptorConverter() {
    }

    static void convert(ArchetypeDescriptor descriptor, Writer writer) {
        Xpp3Dom properties = new Xpp3Dom("requiredProperties");
        for (Property p : descriptor.properties()) {
            if (!p.isExported()) {
                continue;
            }
            Xpp3Dom prop = new Xpp3Dom("requiredProperty");
            prop.setAttribute("key", p.id());
            if (p.value().isPresent()) {
                Xpp3Dom defaultValue = new Xpp3Dom("defaultValue");
                defaultValue.setValue(p.value().get());
                prop.addChild(defaultValue);
            }
            properties.addChild(prop);
        }
        Xpp3Dom root = new Xpp3Dom("archetype-descriptor");
        root.addChild(properties);
        Xpp3DomWriter.write(writer, root);
    }
}
