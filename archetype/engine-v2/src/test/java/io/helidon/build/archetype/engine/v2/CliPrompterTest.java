package io.helidon.build.archetype.engine.v2;

import org.junit.jupiter.api.Test;

import static io.helidon.build.common.ansi.AnsiTextStyles.Bold;
import static io.helidon.build.common.ansi.AnsiTextStyles.BoldBlue;

public class CliPrompterTest {

    @Test
    public void test() {
        System.out.println("defaultResponse");
        String def = BoldBlue.apply("defaultResponse");
        System.out.println(def);
        def = Bold.apply("defaultResponse");
        System.out.println(def);
    }
}
