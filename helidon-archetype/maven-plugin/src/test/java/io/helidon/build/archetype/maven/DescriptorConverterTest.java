/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.archetype.maven;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Base64;

import io.helidon.build.archetype.engine.ArchetypeDescriptor;
import io.helidon.build.util.Strings;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests {@link DescriptorConverter}.
 */
public class DescriptorConverterTest {

    @Test
    public void testConvert() throws IOException {
        InputStream is = DescriptorConverterTest.class.getResourceAsStream("test.properties");
        assertThat(is, is(not(nullValue())));
        Properties testProps = new Properties();
        testProps.load(is);
        String mavenArchetypeMetadata = testProps.getProperty("archetype-metadata.xml");
        assertThat(mavenArchetypeMetadata, is(not(nullValue())));
        ArchetypeDescriptor desc = ArchetypeDescriptor.read(getClass().getResourceAsStream("helidon-archetype.xml"));
        StringWriter sw = new StringWriter();
        DescriptorConverter.convert(desc, sw);
        String convertedDescriptor = Strings.normalizeNewLines(sw.toString());
        assertThat(convertedDescriptor, is (new String(Base64.getDecoder().decode(mavenArchetypeMetadata), StandardCharsets.UTF_8)));
    }
}
