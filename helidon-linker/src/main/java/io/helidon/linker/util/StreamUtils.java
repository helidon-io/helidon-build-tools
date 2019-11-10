/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.linker.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Stream utilities.
 */
public class StreamUtils {
    private static final byte[] BUFFER = new byte[8196];

    /**
     * Transfers the contents of the given input stream to the given output stream.
     *
     * @param in The input stream.
     * @param out The output stream.
     * @throws IOException If an error occurs.
     */
    public static void transfer(InputStream in, OutputStream out) throws IOException {
        synchronized (BUFFER) {
            try (InputStream data = in) {
                int bytesRead;
                while ((bytesRead = data.read(BUFFER)) != -1) {
                    out.write(BUFFER, 0, bytesRead);
                }
            }
        }
    }

    /**
     * Reads the contents of the given input stream as a UTF8 string.
     *
     * @param in The input stream.
     * @return The string.
     * @throws IOException If an error occurs.
     */
    public static String toString(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transfer(in, out);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private StreamUtils() {
    }
}
