/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
package io.helidon.build.cli.common;

/**
 * Native image information.
 */
public class ImageInfo {

    private static final String PROPERTY_IMAGE_CODE_KEY = "org.graalvm.nativeimage.imagecode";
    private static final String PROPERTY_IMAGE_CODE_RUNTIME_VALUE = "runtime";

    private ImageInfo() {
    }

    /**
     * Check if (at the time of the call) code is executing at image runtime. This method will be const-folded.
     * It can be used to hide parts of an application that only work when running as native image.
     *
     * @return true if code is executing at image runtime
     */
    public static boolean inImageRuntimeCode() {
        return PROPERTY_IMAGE_CODE_RUNTIME_VALUE.equals(System.getProperty(PROPERTY_IMAGE_CODE_KEY));
    }
}
