/*
 * Copyright (c) 2018-2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.codegen.openapi;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author rgrecour
 */
public abstract class TestHelper {

    public static final String SOURCE_DIR_PREFIX = "src/test/resources/";

    /**
     * Get the base directory path of the project.
     *
     * @return base directory path
     */
    static String getBasedirPath() {
        String basedirPath = System.getProperty("basedir");
        if (basedirPath == null) {
            basedirPath = new File("").getAbsolutePath();
        }
        return basedirPath.replace("\\","/");
    }

    /**
     * Get a file in the project.
     *
     * @param path a relative path within the project directory
     * @return the corresponding for the given path
     */
    public static File getFile(String path) {
        return new File(getBasedirPath(), path);
    }

    static void assertString(String expected, String actual, String name) {
        if (expected == null) {
            assertNull(actual, name);
        } else {
            assertNotNull(actual, name);
            assertEquals(expected, actual, name);
        }
    }

    static void assertList(int expectedSize, List list, String name) {
        assertNotNull(list, name);
        assertEquals(expectedSize, list.size(), name + ".size");
        for (int i = 0; i < expectedSize; i++) {
            assertNotNull(list.get(i), name + "[" + i + "]");
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T assertType(Object actual, Class<T> expected, String name) {
        assertNotNull(actual, name);
        assertEquals(actual.getClass(), expected, name);
        T t = (T) actual;
        return t;
    }

}
