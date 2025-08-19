/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
package io.helidon.build.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Checksum.
 */
public interface Checksum {

    /**
     * Update the checksum.
     *
     * @param bytes data
     * @return this instance
     */
    Checksum update(byte[] bytes);

    /**
     * Update the checksum.
     *
     * @param is input stream
     * @return this instance
     */
    Checksum update(InputStream is);

    /**
     * Update the checksum.
     *
     * @param file data
     * @return this instance
     */
    Checksum update(Path file);

    /**
     * Get a hex string representation of the checksum.
     *
     * @return hex string
     */
    String toHexString();

    /**
     * Generate an MD5 checksum of the given object.
     *
     * @param object object
     * @return hex string
     */
    static String md5(Object object) {
        return md5(object.toString());
    }

    /**
     * Generate an MD5 checksum of the given string.
     *
     * @param str str
     * @return hex string
     */
    static String md5(String str) {
        return md5(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate an MD5 checksum of the given bytes.
     *
     * @param bytes bytes
     * @return hex string
     */
    static String md5(byte[] bytes) {
        return new MD5().update(bytes).toHexString();
    }

    /**
     * Generate an MD5 checksum of the given file.
     *
     * @param file file
     * @return hex string
     */
    static String md5(Path file) {
        return new MD5().update(file).toHexString();
    }

    /**
     * Generate an MD5 checksum of the given input stream.
     *
     * @param in input stream
     * @return hex string
     */
    static String md5(InputStream in) {
        return new MD5().update(in).toHexString();
    }

    /**
     * MD5 checksum.
     */
    final class MD5 implements Checksum {

        private static final char[] HEX_CODE = "0123456789ABCDEF".toCharArray();
        private static volatile ByteBuffer buffer;
        private final MessageDigest md;

        /**
         * Create a new instance.
         */
        public MD5() {
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public MD5 update(byte[] bytes) {
            md.update(bytes);
            return this;
        }

        @Override
        public MD5 update(InputStream in) {
            try (InputStream data = in) {
                int i;
                while ((i = data.read()) != -1) {
                    md.update((byte) i);
                }
                return this;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public MD5 update(Path file) {
            try {
                RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r");
                FileChannel fc = raf.getChannel();
                if (buffer == null) {
                    synchronized (MD5.class) {
                        if (buffer == null) {
                            buffer = ByteBuffer.allocate(4096);
                        }
                    }
                }
                while (fc.read(buffer) > 0) {
                    buffer.flip();
                    md.update(buffer);
                    buffer.clear();
                }
                buffer.clear();
                fc.close();
                raf.close();
                return this;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public String toHexString() {
            byte[] bytes = md.digest();
            StringBuilder r = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                r.append(HEX_CODE[(b >> 4) & 0xF]);
                r.append(HEX_CODE[(b & 0xF)]);
            }
            return r.toString();
        }
    }
}
