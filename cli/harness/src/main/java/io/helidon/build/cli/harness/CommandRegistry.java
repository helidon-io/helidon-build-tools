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
package io.helidon.build.cli.harness;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Registry of {@link CommandModel}, keyed by their name.
 */
public class CommandRegistry {

    private final Map<String, CommandModel> commandsByName;
    private final String aClass;
    private final String cliName;
    private final String cliDescription;

    /**
     * Create a new command registry.
     *
     * @param cliClass       the class this registry is derived from
     * @param cliName        the CLI cliName
     * @param cliDescription the CLI cliDescription
     */
    protected CommandRegistry(String cliClass, String cliName, String cliDescription) {
        this.aClass = Objects.requireNonNull(cliClass, "cliClass is null");
        this.cliName = Objects.requireNonNull(cliName, "cliName is null");
        this.cliDescription = Objects.requireNonNull(cliDescription, "cliDescription is null");
        commandsByName = new LinkedHashMap<>();
        // built-in commands
        register(new UsageCommand());
        register(new HelpCommand());
    }

    /**
     * Register a command model in the registry.
     *
     * @param models command models to register
     */
    protected final void register(CommandModel... models) {
        for (CommandModel model : models) {
            String name = model.command().name();
            if (commandsByName.containsKey(name)) {
                throw new IllegalArgumentException("Command already registered for name: " + name);
            }
            commandsByName.put(name, model);
        }
    }

    /**
     * Get the commands by name.
     *
     * @return map of command model keyed by name
     */
    final Map<String, CommandModel> commandsByName() {
        return commandsByName;
    }

    /**
     * Get all the commands.
     *
     * @return list of all visible registered commands
     */
    public final List<CommandModel> all() {
        return commandsByName.values().stream().filter(CommandModel::visible).collect(Collectors.toList());
    }

    /**
     * Get a command by name.
     *
     * @param name command name
     * @return optional of {@link CommandModel}
     */
    public final Optional<CommandModel> get(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(commandsByName.get(name));
    }

    /**
     * Get the CLI name.
     *
     * @return CLI name
     */
    public String cliName() {
        return cliName;
    }

    /**
     * Get the CLI description.
     *
     * @return CLI description
     */
    public String cliDescription() {
        return cliDescription;
    }

    /**
     * Load a {@link CommandRegistry} instance.
     *
     * @param aClass class to match with a registry
     * @return command registry, never {@code null}
     */
    public static CommandRegistry load(Class<?> aClass) {
        Objects.requireNonNull(aClass, "aClass is null");
        return ServiceLoader.load(CommandRegistry.class)
                            .stream()
                            .filter((r) -> aClass.getName().equals(r.get().aClass))
                            .findFirst()
                            .map(ServiceLoader.Provider::get)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "No command registry found for class: " + aClass));
    }
}
