/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
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
package io.helidon.build.common.ansi;

import io.helidon.build.common.logging.Log;

import org.fusesource.jansi.Ansi;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit test for class {@link AnsiTextStyles}.
 */
class AnsiTextStylesTest {

    @Test
    void testAll() {
        boolean enabled = AnsiTextProvider.isEnabled();
        Ansi.setEnabled(enabled);
        for (AnsiTextStyles function : AnsiTextStyles.values()) {
            String example = function.apply("example");
            assertThat(AnsiTextStyle.isStyled(example), is(enabled));
            Log.info("%23s [ %s ]", function, example);
            String name = function.name().toLowerCase();
            String styleName;
            if (name.equals("plain") || name.equals("bold") || name.equals("italic")) {
                styleName = name;
            } else if (name.equals("bolditalic")) {
                styleName = "_bold_";
            } else {
                boolean bold = name.contains("bold");
                boolean italic = name.contains("italic");
                boolean bright = name.contains("bright");
                String color = name.replace("bold", "")
                                   .replace("italic", "")
                                   .replace("bright", "");
                styleName = bold ? color.toUpperCase() : color;
                if (italic) {
                    styleName = "_" + styleName + "_";
                }
                if (bright) {
                    styleName += "!";
                }
            }

            String expected = AnsiTextStyle.named(styleName,true).apply("example");
            assertThat(example, is(expected));
        }
    }
}
