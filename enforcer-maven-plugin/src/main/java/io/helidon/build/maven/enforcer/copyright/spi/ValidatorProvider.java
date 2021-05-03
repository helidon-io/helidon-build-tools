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

package io.helidon.build.maven.enforcer.copyright.spi;

import java.util.List;

import io.helidon.build.maven.enforcer.copyright.TemplateLine;
import io.helidon.build.maven.enforcer.copyright.Validator;

/**
 * Java service loader interface to extend the functionality of copyright checking.
 */
public interface ValidatorProvider {
    /**
     * Create a custom validator.
     *
     * @param config configuration
     * @param templateLines lines of copyright template
     * @return a new validator instance
     */
    Validator validator(Validator.ValidatorConfig config, List<TemplateLine> templateLines);
}
