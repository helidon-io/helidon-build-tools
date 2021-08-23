package io.helidon.build.archetype.engine.v2.interpreter;

import java.util.LinkedList;

/**
 * Prompter for the user input.
 */
public interface Prompter {

    /**
     * Send inputs to the user, wait for the answer and return the filled result.
     * Inputs in the result list must be in the same order as in the input list.
     *
     * @param inputs inputs for the user
     * @return resolved inputs
     */
    LinkedList<InputAST> resolve(LinkedList<InputAST> inputs);
}
