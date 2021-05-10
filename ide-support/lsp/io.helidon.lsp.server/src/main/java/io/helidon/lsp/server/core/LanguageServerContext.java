package io.helidon.lsp.server.core;

import org.eclipse.lsp4j.WorkspaceFolder;

import java.util.List;

public class LanguageServerContext {

    private List<WorkspaceFolder> workspaceFolders;

    public List<WorkspaceFolder> getWorkspaceFolders() {
        return workspaceFolders;
    }

    public void setWorkspaceFolders(List<WorkspaceFolder> workspaceFolders) {
        this.workspaceFolders = workspaceFolders;
    }
}
