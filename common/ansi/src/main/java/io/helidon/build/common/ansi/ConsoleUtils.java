/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
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

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Console utilities.
 */
public class ConsoleUtils {
    private static final boolean ENABLED = AnsiConsoleInstaller.areAnsiEscapesEnabled();
    private static final char FIRST_ESC_CHAR = 27;
    private static final char SECOND_ESC_CHAR = '[';
    private static final char[] SHOW_CURSOR = {FIRST_ESC_CHAR, SECOND_ESC_CHAR, '?', '2', '5', 'h'};
    private static final char[] HIDE_CURSOR = {FIRST_ESC_CHAR, SECOND_ESC_CHAR, '?', '2', '5', 'l'};

    /**
     * Clears the screen if Ansi escapes are enabled.
     *
     * @return {@code true} if Ansi escapes are enabled.
     */
    public static boolean clearScreen() {
        return clearScreen(0);
    }

    /**
     * Clears the screen from the given row if Ansi escapes are enabled.
     *
     * @param startRow The row at which to start clearing.
     * @return {@code true} if Ansi escapes are enabled.
     */
    public static boolean clearScreen(int startRow) {
        if (ENABLED) {
            System.out.print(ansi().cursor(startRow, 0).eraseScreen());
            System.out.flush();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Moves the cursor left by the length of the message before printing it. Does nothing if
     * Ansi escapes are enabled.
     *
     * @param message The message.
     * @return {@code true} if Ansi escapes are enabled.
     */
    public static boolean rewriteLine(String message) {
        return rewriteLine(message.length(), message);
    }

    /**
     * Moves the cursor left by the specified number of characters before printing the message. Does nothing if
     * Ansi escapes are enabled.
     *
     * @param charsToBackUp The number of characters to move left.
     * @param message The message.
     * @return {@code true} if Ansi escapes are enabled.
     */
    public static boolean rewriteLine(int charsToBackUp, String message) {
        if (ENABLED) {
            System.out.print(ansi().cursorLeft(charsToBackUp).a(message));
            System.out.flush();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Hides the cursor if Ansi escapes are enabled.
     *
     * @return {@code true} if Ansi escapes are enabled.
     */
    public static boolean hideCursor() {
        if (ENABLED) {
            System.out.print(HIDE_CURSOR);
            System.out.flush();
            return true;
        } else {
            return false;
        }

    }

    /**
     * Shows the cursor if Ansi escapes are enabled.
     *
     * @return {@code true} if Ansi escapes are enabled.
     */
    public static boolean showCursor() {
        if (ENABLED) {
            System.out.print(SHOW_CURSOR);
            System.out.flush();
            return true;
        } else {
            return false;
        }
    }

    private ConsoleUtils() {
    }
}
