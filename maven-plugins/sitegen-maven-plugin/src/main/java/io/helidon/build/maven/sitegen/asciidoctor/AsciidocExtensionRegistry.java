/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
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

package io.helidon.build.maven.sitegen.asciidoctor;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.converter.JavaConverterRegistry;
import org.asciidoctor.extension.JavaExtensionRegistry;
import org.asciidoctor.jruby.extension.spi.ExtensionRegistry;

/**
 * An implementation of {@link ExtensionRegistry} to register custom extensions in Asciidoctorj.
 */
public class AsciidocExtensionRegistry implements ExtensionRegistry {

    private final String backendName;

    private AsciidocExtensionRegistry(String backendName) {
        this.backendName = backendName;
    }

    @Override
    public void register(Asciidoctor asciidoctor) {
        JavaConverterRegistry javaConverterRegistry = asciidoctor.javaConverterRegistry();
        javaConverterRegistry.register(AsciidocConverter.class, backendName);
        JavaExtensionRegistry javaExtensionRegistry = asciidoctor.javaExtensionRegistry();
        javaExtensionRegistry.block(new CardBlockProcessor());
        javaExtensionRegistry.block(new PillarsBlockProcessor());
        javaExtensionRegistry.preprocessor(new IncludePreprocessor());
    }

    /**
     * Create a new instance.
     *
     * @param backendName backend name
     * @return new instance
     */
    public static AsciidocExtensionRegistry create(String backendName) {
        return new AsciidocExtensionRegistry(backendName);
    }
}
