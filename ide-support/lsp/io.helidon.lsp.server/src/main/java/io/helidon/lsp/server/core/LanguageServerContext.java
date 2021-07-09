/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.lsp.server.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.WorkspaceFolder;

/**
 * Context data for the running instance of the Helidon Language Server.
 */
public class LanguageServerContext {

    private List<WorkspaceFolder> workspaceFolders;
    private Map<Class, Object> beans = new HashMap<>();

    /**
     * Get workspace folders in IDE.
     *
     * @return List of workspace folders.
     */
    public List<WorkspaceFolder> getWorkspaceFolders() {
        return workspaceFolders;
    }

    /**
     * Set workspace folders.
     *
     * @param workspaceFolders List of workspace folders.
     */
    public void setWorkspaceFolders(List<WorkspaceFolder> workspaceFolders) {
        this.workspaceFolders = workspaceFolders;
    }

    /**
     * Get Map object that contains class types and corresponding instances of these classes
     * for the running instance of the Helidon Language Server.
     *
     * @return Map object that contains class types and corresponding instances of these classes.
     */
    public Map<Class, Object> getBeans() {
        return beans;
    }

    /**
     * Add instance of the class with corresponding class type to the current LanguageServerContext.
     *
     * @param clazz Class type for the added instance of this class.
     * @param bean  Instance of the added object.
     */
    public void setBean(Class clazz, Object bean) {
        beans.putIfAbsent(clazz, bean);
    }

    /**
     * Return instance of the class by the class type for the current LanguageServerContext.
     *
     * @param clazz Class of the returned object.
     * @return Instance of the class by the class type for the current LanguageServerContext.
     */
    public Object getBean(Class clazz) {
        return beans.get(clazz);
    }
}
