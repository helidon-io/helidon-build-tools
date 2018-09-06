/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.sitegen.maven;

/**
 * Constants shared by Maven goals.
 * @author rgrecour
 */
abstract class Constants {
    static final String PROPERTY_PREFIX = "helidon.sitegen.";
    static final String DEFAULT_SITE_OUTPUT_DIR = "${project.build.directory}/site";
    static final String DEFAULT_SITE_SOURCE_DIR = "${project.basedir}/src/main/site";
}
