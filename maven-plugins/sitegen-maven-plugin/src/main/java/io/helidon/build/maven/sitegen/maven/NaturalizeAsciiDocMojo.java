/*
 * Copyright (c) 2019, 2023 Oracle and/or its affiliates.
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
package io.helidon.build.maven.sitegen.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Converts the "preprocessed" form of AsciiDoc files (with text pre-included
 * and includes described in comments) into "natural" form (using standard
 * {@code include::} directives).
 * <p>
 * Although the preprocessed form renders better on GitHub, developers can more
 * easily edit the natural form.
 * <p>
 * See {@link AbstractAsciiDocMojo} for a description of the parameters for
 * this goal. (This goal inherits the common settings and adds no others.)
 */
@Mojo(name = "naturalize-adoc",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class NaturalizeAsciiDocMojo extends AbstractAsciiDocMojo {

    @Override
    String outputType() {
        return "natural";
    }
}
