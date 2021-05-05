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
package io.helidon.build.common.test.utils;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

/**
 * An argument provider that supports the {@link ConfigurationParameterSource} annotation.
 */
class ConfigurationParameterArgumentsProvider
        implements ArgumentsProvider, AnnotationConsumer<ConfigurationParameterSource> {

    private String[] argumentNames;

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Arrays.stream(argumentNames)
                       .flatMap(name -> context.getConfigurationParameter(name).stream())
                       .map(Arguments::of);
    }

    @Override
    public void accept(ConfigurationParameterSource configurationParameterSource) {
        this.argumentNames = configurationParameterSource.value();
    }
}
