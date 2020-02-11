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

import java.util.List;

/**
 * Utility to find the closest matching command name.
 */
final class CommandMatcher {

    private CommandMatcher() {
    }

    /**
     * Find the closest matching target that matches the source.
     * @param source input string to find a closest match for
     * @param targets the list of target that can be matched
     * @return closest match or {@code null} if no close match are found
     */
    static String match(String source, List<String> targets) {
        int score = 0;
        String match = null;
        for (String target : targets) {
            int curScore = score(source, target);
            if (curScore > score) {
                match = target;
                score = curScore;
            }
        }
        if (match != null) {
            if (((double) score / perfectScore(match)) >= 0.5) {
                return match;
            }
        }
        return null;
    }

    private static int uniqueIndexOf(char c, String target, boolean[] matches) {
        int index = 0;
        while (true) {
            index = target.indexOf(c, index);
            if (index < 0) {
                // c not found in target
                break;
            } else if (!matches[index]) {
                // c found and not yet matched
                return index;
            } else {
                // c found but already matched
                index++;
            }
        }
        return index;
    }

    private static int[] weight(String target) {
        int[] weight = new int[target.length()];
        for (int i = 0; i < weight.length; i++) {
            switch (i) {
                case 0:
                    weight[i] = 10;
                    break;
                case 1:
                    weight[i] = 6;
                    break;
                case 2:
                    weight[i] = 4;
                    break;
                case 3:
                    weight[i] = 2;
                    break;
                default:
                    weight[i] = 1;
            }
        }
        return weight;
    }

    private static int perfectScore(String target) {
        int perfectScore = 0;
        int[] weight = weight(target);
        for (int i = 0; i < weight.length; i++) {
            perfectScore += weight[i];
        }
        return perfectScore;
    }

    private static int score(String source, String target) {
        int[] weight = weight(target);
        char[] src = source.toCharArray();
        int tlen = target.length();
        boolean[] matches = new boolean[tlen];
        int score = 0;
        for (int i = 0; i < src.length && i < tlen; i++) {
            char c = src[i];
            if (target.charAt(i) == c) {
                score += weight[i];
                matches[i] = true;
            } else {
                int udx = uniqueIndexOf(c, target, matches);
                if (udx >= 0) {
                    matches[udx] = true;
                    score += (weight[udx] / Math.abs(udx - i));
                }
            }
        }
        return score;
    }
}
