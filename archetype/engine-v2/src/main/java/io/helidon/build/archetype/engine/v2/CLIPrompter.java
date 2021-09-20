package io.helidon.build.archetype.engine.v2;

import java.util.List;

import io.helidon.build.archetype.engine.v2.interpreter.InputBooleanAST;
import io.helidon.build.archetype.engine.v2.interpreter.InputEnumAST;
import io.helidon.build.archetype.engine.v2.interpreter.InputListAST;
import io.helidon.build.archetype.engine.v2.interpreter.InputTextAST;

/**
 * Prompter that uses CLI for input/output.
 */
public class CLIPrompter implements Prompter {

    private String lastStepLabel;

    @Override
    public String prompt(String stepLabel, String stepHelp, InputTextAST inputInfo) {
        if (stepLabel != null && !stepLabel.equals(lastStepLabel)) {

        }
        return null;
    }

    @Override
    public String prompt(String stepLabel, String stepHelp, InputEnumAST inputInfo) {
        return null;
    }

    @Override
    public List<String> prompt(String stepLabel, String stepHelp, InputListAST inputInfo) {
        return null;
    }

    @Override
    public boolean prompt(String stepLabel, String stepHelp, InputBooleanAST inputInfo) {
        return false;
    }
}
