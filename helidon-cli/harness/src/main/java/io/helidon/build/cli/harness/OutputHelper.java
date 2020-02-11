/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.cli.harness;

import java.util.Iterator;
import java.util.Map;

/**
 * Utility class to help with output.
 */
public final class OutputHelper {

    private static final String BEGIN_SPACING = "  ";
    private static final String COL_SPACING = "    ";

    private OutputHelper() {
    }

    /**
     * Render map as a table with the second column aligned.
     * @param map map to render
     * @return rendered table
     */
    public static String table(Map<String, String> map) {
        int maxOptNameLength = 0;
        for (String opt : map.keySet()) {
            int optNameLength = opt.length();
            if (optNameLength > maxOptNameLength) {
                maxOptNameLength = optNameLength;
            }
        }
        String out = "";
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> optEntry = it.next();
            String optName = optEntry.getKey();
            int curColPos = maxOptNameLength - optName.length();
            String colSpacing = "";
            for (int i = 0; i < curColPos; i++) {
                colSpacing += " ";
            }
            out += BEGIN_SPACING + optName + colSpacing + COL_SPACING + optEntry.getValue();
            if (it.hasNext()) {
                out += "\n";
            }
        }
        return out;
    }
}
