package io.helidon.build.archetype.engine.v2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.helidon.build.archetype.engine.v2.prompter.CLIPrompter;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ArchetypeEngineV2Test extends ArchetypeBaseTest {

    @Test
    void generateSkipOptional() throws IOException {
        File targetDir = new File(new File("").getAbsolutePath(), "target");
        File outputDir = new File(targetDir, "test-project");
        Path outputDirPath = outputDir.toPath();
        if (Files.exists(outputDirPath)) {
            Files.walk(outputDirPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        assertThat(Files.exists(outputDirPath), is(false));
        Map<String, String> initContextValues = new HashMap<>();
        initContextValues.put("flavor", "se");
        initContextValues.put("base", "bare");
        initContextValues.put("build-system", "maven");
        ArchetypeEngineV2 archetypeEngineV2 = new ArchetypeEngineV2(getArchetype(
                "bare-se-arch"),
                "flavor.xml",
                new CLIPrompter(),
                initContextValues,
                true,
                List.of());

        archetypeEngineV2.generate(outputDir);
        assertThat(Files.exists(outputDirPath), is(true));
    }
}