/*
 * Copyright (c) 2018, 2023 Oracle and/or its affiliates.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.SourcePath.wildcardMatch;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit test for class {@link SourcePath}.
 */
public class SourcePathTest {

    private static void assertPath(boolean equals, String path1, String path2) {
        SourcePath s1 = new SourcePath(path1);
        SourcePath s2 = new SourcePath(path2);
        String message = path1 + ".equals(" + path2 + ")";
        if (equals) {
            assertThat(message, s1, is(s2));
        } else {
            assertThat(message, s1, is(not(s2)));
        }
    }

    @Test
    public void testEmptyPath() {
        assertThat(new SourcePath("./").matches("*"), is(true));
        assertThat(new SourcePath("./").matches("**"), is(true));
        assertThat(new SourcePath("").matches("*"), is(true));
        assertThat(new SourcePath("").matches("**"), is(true));
    }

    @Test
    public void testNormalization() {
        assertPath(true, "./abc/def/index.html", "abc/def/index.html");
        assertPath(false, "../abc/def/index.html", "abc/def/index.html");
        assertPath(true, "/abc/def/index.html", "abc/def/index.html");
        assertPath(true, "//abc/def/index.html", "abc/def/index.html");
        assertPath(true, ".//abc/def/index.html", "abc/def/index.html");
        assertPath(true, ".//abc//def/index.html", "abc/def/index.html");
        assertPath(true, "/././abc//def/index.html", "abc/def/index.html");
    }

    @Test
    public void testSinglePathMatching() {
        SourcePath path = new SourcePath("abc/def/ghi/index.html");
        assertThat("empty pattern", path.matches(""), is(false));
        assertThat("identical pattern", path.matches("abc/def/ghi/index.html"), is(true));
        assertThat("trailing wildcard", path.matches("abc/def/ghi/index.html*"), is(true));
        assertThat("leading wildcard", path.matches("*abc/def/ghi/index.html*"), is(true));
        assertThat("trailing double wildcard", path.matches("abc/def/ghi/index.html**"), is(true));
        assertThat("bad pattern", path.matches("abc/def/ghi/foo.html"), is(false));
        assertThat("abc/*/ghi/index.html", path.matches("abc/*/ghi/index.html"), is(true));
        assertThat("*/def/ghi/index.html", path.matches("*/def/ghi/index.html"), is(true));
        assertThat("abc/def/ghi/*", path.matches("abc/def/ghi/*"), is(true));
        assertThat("abc/def/*/*.html", path.matches("abc/def/*/*.html"), is(true));
        assertThat("*/*/*/*", path.matches("*/*/*/*"), is(true));
        assertThat("**", path.matches("**"), is(true));
        assertThat("**/*.html", path.matches("**/*.html"), is(true));
        assertThat("**/ghi/*.html", path.matches("**/ghi/*.html"), is(true));
        assertThat("**/def/*.html", path.matches("**/def/*.html"), is(false));
        assertThat("**/*/*.html", path.matches("**/*/*.html"), is(true));
        assertThat("abc/*/*.html", path.matches("abc/**/*.html"), is(true));
        assertThat("**h*/*.html", path.matches("**h*/*.html"), is(false));
        assertThat("**h/*.html", path.matches("**h/*.html"), is(false));
        assertThat("**h*j/*.html", path.matches("**h*j/*.html"), is(false));
        assertThat("ab**/*.html", path.matches("ab**/*.html"), is(false));

        SourcePath path2 = new SourcePath("index.html");
        assertThat("**/*.html", path2.matches("**/*.html"), is(true));
    }

    private static String printPaths(List<SourcePath> paths) {
        StringBuilder sb = new StringBuilder();
        Iterator<SourcePath> it = paths.iterator();
        while (it.hasNext()) {
            sb.append(it.next().asString());
            if (it.hasNext()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    @Test
    public void testFiltering() {
        List<SourcePath> paths = new ArrayList<>();
        paths.add(new SourcePath("abc/def/ghi/index.html"));
        paths.add(new SourcePath("abc/def/ghi/foo.html"));
        paths.add(new SourcePath("abc/def/ghi/bar.html"));
        paths.add(new SourcePath("abc/def/index.html"));
        paths.add(new SourcePath("abc/def/foo.html"));
        paths.add(new SourcePath("abc/def/bar.html"));
        paths.add(new SourcePath("abc/index.html"));
        paths.add(new SourcePath("abc/foo.html"));
        paths.add(new SourcePath("abc/bar.html"));
        paths.add(new SourcePath("index.html"));
        paths.add(new SourcePath("foo.html"));
        paths.add(new SourcePath("bar.html"));

        List<SourcePath> filtered;

        filtered = SourcePath.filter(paths, null, null);
        assertThat(filtered, is(not(nullValue())));
        assertThat("filtered list should be equal to original", paths.size(), is(filtered.size()));

        filtered = SourcePath.filter(paths, List.of("**/*"), null);
        assertThat(filtered, is(not(nullValue())));
        assertThat("filtered list is:\n\n" + printPaths(filtered)
                + "\n\ninstead of: \n" + printPaths(paths), paths.size(), is(filtered.size()));

        filtered = SourcePath.filter(paths, List.of("**/*.html"), null);
        assertThat(filtered, is(not(nullValue())));
        assertThat("filtered list is:\n\n" + printPaths(filtered)
                + "\n\ninstead of: \n" + printPaths(paths), paths.size(), is(filtered.size()));

        filtered = SourcePath.filter(paths, List.of("*.html"), null);
        assertThat(filtered, is(not(nullValue())));
        assertThat("filtered list is:\n\n" + printPaths(filtered)
                + "\n\ninstead of: \n" + printPaths(paths), 3, is(filtered.size()));

        filtered = SourcePath.filter(paths, List.of("abc/def/ghi/*.html"), null);
        assertThat(filtered, is(not(nullValue())));
        assertThat("filtered list is:\n\n" + printPaths(filtered)
                + "\n\ninstead of: \n" + printPaths(paths), 3, is(filtered.size()));

        filtered = SourcePath.filter(paths, List.of("abc/**"), null);
        assertThat(filtered, is(not(nullValue())));
        assertThat("filtered list is:\n\n" + printPaths(filtered)
                + "\n\ninstead of: \n" + printPaths(paths), 9, is(filtered.size()));

        filtered = SourcePath.filter(paths, List.of("abc/**"), List.of("*/def/ghi/*"));
        assertThat(filtered, is(not(nullValue())));
        assertThat("filtered list is:\n\n" + printPaths(filtered)
                + "\n\ninstead of: \n\n" + printPaths(paths), 6, is(filtered.size()));

        filtered = SourcePath.filter(paths, List.of("**"), List.of("*/def/**"));
        assertThat(filtered, is(not(nullValue())));
        assertThat("filtered list is:\n\n" + printPaths(filtered)
                + "\n\ninstead of: \n" + printPaths(paths), 6, is(filtered.size()));
    }

    private static void assertWildcardMatch(boolean expectedIsMatch,
                                            String segment,
                                            String pattern) {

        assertWildcardMatch(expectedIsMatch, segment, pattern, /* desc */ null);
    }

    private static void assertWildcardMatch(boolean expectedIsMatch,
                                            String segment,
                                            String pattern,
                                            String desc) {

        boolean matched = wildcardMatch(segment, pattern);
        String message = "segment=" + segment + ", pattern=" + pattern;
        if (desc != null) {
            message += ", desc=" + desc;
        }
        assertThat(message, expectedIsMatch, is(matched));
    }

    @Test
    public void testMatchSegment() {

        // empty string, identical, wildcard only
        assertWildcardMatch(true, "", "", "both empty");
        assertWildcardMatch(true, "index.html", "index.html", "both identical");
        assertWildcardMatch(false, "index.html", "", "empty pattern");
        assertWildcardMatch(true, "index.html", "*", "single wildcard");
        assertWildcardMatch(true, "index.html", "**", "double wildcard");
        assertWildcardMatch(true, "index.html", "***", "triple wildcard");
        assertWildcardMatch(false, "index.html", "something-else", "different pattern");

        // simple matching
        assertWildcardMatch(true, "index.html", "*.html");
        assertWildcardMatch(true, "index.html", "index.*");
        assertWildcardMatch(true, "index.html", "index*");
        assertWildcardMatch(true, "index.html", "i*");
        assertWildcardMatch(true, "index.html", "in*");
        assertWildcardMatch(true, "index.html", "i*l");
        assertWildcardMatch(true, "index.html", "in*ml");
        assertWildcardMatch(true, "index.html", "index*html");
        assertWildcardMatch(true, "index.html", "index*.html");
        assertWildcardMatch(true, "index.html", "*html");
        assertWildcardMatch(true, "index.html", "*ml");
        assertWildcardMatch(true, "index.html", "*l");
        assertWildcardMatch(true, "index.html", "*index.html");
        assertWildcardMatch(true, "index.html", "index.html*");
        assertWildcardMatch(false, "index.html", "id*");
        assertWildcardMatch(false, "index.html", "*x");
        assertWildcardMatch(false, "index.html", "*dex");
        assertWildcardMatch(false, "index.html", "ii*");
        assertWildcardMatch(false, "index.html", "*ll");
        assertWildcardMatch(false, "index.html", "index.html*bad");

        // simple matching with double wildcards
        assertWildcardMatch(true, "index.html", "**index.html");
        assertWildcardMatch(false, "index.html", "**index.htm");
        assertWildcardMatch(false, "index.html", "i**ndex.htm");
        assertWildcardMatch(false, "index.html", ".**");
        assertWildcardMatch(true, "index.html", "i**");
        assertWildcardMatch(true, "index.html", "index.html**");
        assertWildcardMatch(false, "index.html", "index.**whatever");

        // simple matching with multiple wildcards
        assertWildcardMatch(true, "index.html", "*.*");
        assertWildcardMatch(false, "index.html", "*.*.*");
        assertWildcardMatch(true, "index.html", "*.*ml");
        assertWildcardMatch(true, "index.html", "*.*ml*");
        assertWildcardMatch(true, "index.html", "i*x.h*ml");
        assertWildcardMatch(false, "index.html", "i*x.h*ml*a");
    }
}
