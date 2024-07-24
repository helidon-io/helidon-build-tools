/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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
package io.helidon.build.common.xml;

/**
 * XML reader exception.
 */
public class XMLReaderException extends XMLException {

    /**
     * Create a new XML reader exception.
     *
     * @param msg   message
     * @param cause cause
     */
    public XMLReaderException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Create a new XML reader exception.
     *
     * @param msg message
     */
    public XMLReaderException(String msg) {
        super(msg);
    }

    /**
     * Create a new XML reader exception.
     */
    public XMLReaderException() {
        super();
    }
}
