package io.helidon.build.archetype.engine.v2;

import java.util.List;

import io.helidon.build.archetype.engine.v2.interpreter.InputBooleanAST;
import io.helidon.build.archetype.engine.v2.interpreter.InputEnumAST;
import io.helidon.build.archetype.engine.v2.interpreter.InputListAST;
import io.helidon.build.archetype.engine.v2.interpreter.InputTextAST;

/**
 * Prompter for user input.
 */
public interface Prompter {

    /**
     * Prompt a text value.
     *
     * @param stepLabel brief information about the current step (is not changed for requests related to the step)
     * @param stepHelp  help information related to the current step
     * @param inputInfo information about the current requests.
     * @return user input (response from the user)
     */
    String prompt(String stepLabel, String stepHelp, InputTextAST inputInfo);

    /**
     * Prompt for a selection.
     *
     * @param stepLabel brief information about the current step (is not changed for requests related to the step)
     * @param stepHelp  help information related to the current step
     * @param inputInfo information about the current requests.
     * @return user input (the value of the chosen option)
     */
    String prompt(String stepLabel, String stepHelp, InputEnumAST inputInfo);

    /**
     * Prompt for a multiply selection.
     *
     * @param stepLabel brief information about the current step (is not changed for requests related to the step)
     * @param stepHelp  help information related to the current step
     * @param inputInfo information about the current requests.
     * @return user input (the values of the chosen options)
     */
    List<String> prompt(String stepLabel, String stepHelp, InputListAST inputInfo);

    /**
     * Prompt for a yes no.
     *
     * @param stepLabel brief information about the current step (is not changed for requests related to the step)
     * @param stepHelp  help information related to the current step
     * @param inputInfo information about the current requests.
     * @return user input (true if user chose yes, no - otherwise)
     */
    boolean prompt(String stepLabel, String stepHelp, InputBooleanAST inputInfo);
}
