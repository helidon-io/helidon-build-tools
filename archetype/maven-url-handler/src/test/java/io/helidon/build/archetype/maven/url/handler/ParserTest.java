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

package io.helidon.build.archetype.maven.url.handler;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ParserTest {

    @Test
    public void testParser() throws MalformedURLException {
        String mvnPath = "io.helidon.archetypes:helidon-archetype:3.0.0-SNAPSHOT/some/useless/directory/helidon-archetype.xml";
        Parser parser = new Parser(mvnPath);

        assertThat(parser.getGroupId(), is("io.helidon.archetypes"));
        assertThat(parser.getArtifactId(), is("helidon-archetype"));
        assertThat(parser.getVersion(), is("3.0.0-SNAPSHOT"));
        assertThat(parser.filePath(), is(new String[]{"some", "useless", "directory", "helidon-archetype.xml"}));
    }

}
