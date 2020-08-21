/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
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

package io.helidon.build.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import static io.helidon.build.util.SourcePath.wildcardMatch;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author rgrecour
 */
public class SourcePathTest {

    private static void assertPath(boolean equals, String path1, String path2){
        SourcePath s1 = new SourcePath(path1);
        SourcePath s2 = new SourcePath(path2);
        String message = path1 + ".equals(" + path2 + ")";
        if (equals) {
            assertEquals(s1, s2,"!" + message);
        } else {
            assertNotEquals(s1, s2, message);
        }
    }

    @Test
    public void testNormalization(){
        assertPath(true, "./abc/def/index.html", "abc/def/index.html");
        assertPath(false, "../abc/def/index.html", "abc/def/index.html");
        assertPath(true, "/abc/def/index.html", "abc/def/index.html");
        assertPath(true, "//abc/def/index.html", "abc/def/index.html");
        assertPath(true, ".//abc/def/index.html", "abc/def/index.html");
        assertPath(true, ".//abc//def/index.html", "abc/def/index.html");
        assertPath(true, "/././abc//def/index.html", "abc/def/index.html");
    }

    @Test
    public void testSinglePathMatching(){
        SourcePath path = new SourcePath("abc/def/ghi/index.html");
        assertEquals(false, path.matches(""), "empty pattern");
        assertEquals(true, path.matches("abc/def/ghi/index.html"), "identical pattern");
        assertEquals(true, path.matches("abc/def/ghi/index.html*"), "trailing wildcard");
        assertEquals(true, path.matches("*abc/def/ghi/index.html*"), "leading wildcard");
        assertEquals(true, path.matches("abc/def/ghi/index.html**"), "trailing double wildcard");
        assertEquals(false, path.matches("abc/def/ghi/foo.html"), "bad pattern");
        assertEquals(true, path.matches("abc/*/ghi/index.html"), "abc/*/ghi/index.html");
        assertEquals(true, path.matches("*/def/ghi/index.html"), "*/def/ghi/index.html");
        assertEquals(true, path.matches("abc/def/ghi/*"), "abc/def/ghi/*");
        assertEquals(true, path.matches("abc/def/*/*.html"), "abc/def/*/*.html");
        assertEquals(true, path.matches("*/*/*/*"), "*/*/*/*");
        assertEquals(true, path.matches("**"), "**");
        assertEquals(true, path.matches("**/*.html"), "**/*.html");
        assertEquals(true, path.matches("**/ghi/*.html"), "**/ghi/*.html");
        assertEquals(false, path.matches("**/def/*.html"), "**/def/*.html");
        assertEquals(true, path.matches("**/*/*.html"), "**/*/*.html");
        assertEquals(true, path.matches("abc/**/*.html"), "abc/*/*.html");
        assertEquals(false, path.matches("**h*/*.html"), "**h*/*.html");
        assertEquals(false, path.matches("**h/*.html"), "**h/*.html");
        assertEquals(false, path.matches("**h*j/*.html"), "**h*j/*.html");
        assertEquals(false, path.matches("ab**/*.html"), "ab**/*.html");

        SourcePath path2 = new SourcePath("index.html");
        assertEquals(true, path2.matches("**/*.html"), "**/*.html");
    }

    private static String printPaths(List<SourcePath> paths){
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
    public void testFiltering(){
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
        assertNotNull(filtered);
        assertEquals(paths.size(), filtered.size(), "filtered list should be equal to original");

        filtered = SourcePath.filter(paths, List.of("**/*"), null);
        assertNotNull(filtered);
        assertEquals(paths.size(), filtered.size(),
                "filtered list is:\n\n" + printPaths(filtered)
                + "\n\ninstead of: \n" + printPaths(paths));

        filtered = SourcePath.filter(paths, List.of("**/*.html"), null);
        assertNotNull(filtered);
        assertEquals(paths.size(), filtered.size(),
                "filtered list is:\n\n" + printPaths(filtered)
                + "\n\ninstead of: \n" + printPaths(paths));

        filtered = SourcePath.filter(paths, List.of("*.html"), null);
        assertNotNull(filtered);
        assertEquals(3, filtered.size(),
                "filtered list is:\n\n" + printPaths(filtered)
                + "\n\ninstead of: \n" + printPaths(paths));

        filtered = SourcePath.filter(paths, List.of("abc/def/ghi/*.html"), null);
        assertNotNull(filtered);
        assertEquals(3, filtered.size(),
                "filtered list is:\n\n" + printPaths(filtered)
                + "\n\ninstead of: \n" + printPaths(paths));

        filtered = SourcePath.filter(paths, List.of("abc/**"), null);
        assertNotNull(filtered);
        assertEquals(9, filtered.size(),
                "filtered list is:\n\n" + printPaths(filtered)
                + "\n\ninstead of: \n" + printPaths(paths));

        filtered = SourcePath.filter(paths, List.of("abc/**"), List.of("*/def/ghi/*"));
        assertNotNull(filtered);
        assertEquals(6, filtered.size(),
                "filtered list is:\n\n" + printPaths(filtered)
                + "\n\ninstead of: \n\n" + printPaths(paths));

        filtered = SourcePath.filter(paths, List.of("**"), List.of("*/def/**"));
        assertNotNull(filtered);
        assertEquals(6, filtered.size(),
                "filtered list is:\n\n" + printPaths(filtered)
                + "\n\ninstead of: \n" + printPaths(paths));
    }

    private static void assertWildcardMatch(boolean expectedIsMatch,
                                            String segment,
                                            String pattern) {

        assertWildcardMatch(expectedIsMatch, segment, pattern, /* desc */ null);
    }

    private static void assertWildcardMatch(boolean expectedIsMatch,
                                            String segment,
                                            String pattern,
                                            String desc){

        boolean matched = wildcardMatch(segment, pattern);
        String message = "segment=" + segment + ", pattern=" + pattern;
        if(desc != null){
            message += ", desc=" + desc;
        }
        assertEquals(expectedIsMatch, matched, message);
    }

    @Test
    public void testMatchSegment(){

        // empty string, identical, wildcard only
        assertWildcardMatch(true, "", "", "both empty");
        assertWildcardMatch(true, "index.html", "index.html", "both identical");
        assertWildcardMatch(false,"index.html", "", "empty pattern");
        assertWildcardMatch(true, "index.html", "*", "single wildcard");
        assertWildcardMatch(true, "index.html", "**", "double wildcard");
        assertWildcardMatch(true, "index.html", "***", "triple wildcard");
        assertWildcardMatch(false, "index.html", "somethingelse", "different pattern");

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
