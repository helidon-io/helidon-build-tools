package io.helidon.build.cli.harness;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class OutputHelperTest {

    @Test
    public void testTable() {
        Map<String, String> map = Map.of(
                "key1", "value1",
                "key2", "value2"
        );
        String expected = "  key1    value1\n  key2    value2";
        assertThat(OutputHelper.table(map), is(expected));
    }

    @Test
    public void testTableWithMaxKeyWidth() {
        Map<String, String> map = Map.of(
                "key1", "value1",
                "key2", "value2"
        );
        String expected = "  key1    value1\n  key2    value2";
        assertThat(OutputHelper.table(map, 4), is(expected));
    }

    @Test
    public void testMaxKeyWidth() {
        Map<String, String> map1 = Map.of(
                "key1", "value1",
                "longerKey", "value2"
        );
        Map<String, String> map2 = Map.of(
                "short", "value3",
                "longestKey", "value4"
        );
        int expected = 10; // length of "longestKey"
        assertThat(OutputHelper.maxKeyWidth(map1, map2), is(expected));
    }
}