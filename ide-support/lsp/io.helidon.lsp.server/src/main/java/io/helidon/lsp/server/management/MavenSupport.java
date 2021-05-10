package io.helidon.lsp.server.management;

import io.helidon.lsp.server.utils.ShellCommandHelper;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.utils.cli.CommandLineException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class MavenSupport {

    private static final Logger LOGGER = Logger.getLogger(MavenSupport.class.getName());
    private static final String POM_FILE_NAME = "pom.xml";
    private static MavenSupport INSTANCE;

    private boolean isMavenInstalled = false;

    private MavenSupport() {
        initialize();
    }

    private void initialize() {
        List<String> mvnCommandResult = ShellCommandHelper.execute("mvn -version");
        for (String line : mvnCommandResult) {
            if (line.contains("Maven home")) {
                String[] split = line.split("Maven home:");
                for (String s : split) {
                    String part = s.trim();
                    if (part.length() > 0) {
                        System.setProperty("maven.home", part);
                        isMavenInstalled = true;
                    }
                }
            }
        }

    }

    public List<String> getDependencies(final String pomPath) {
        if (!isMavenInstalled) {
            return null;
        }
        List<String> result = new ArrayList<>();

        String dependencyMarker = "Dependencies classpath:";
        List<String> execute = ShellCommandHelper.execute(
                "mvn dependency:build-classpath",
                new File(pomPath).getParentFile()
        );
        for (int x = 0; x < execute.size(); x++) {
            if (execute.get(x).contains(dependencyMarker)) {
                String dependencies = execute.get(x + 1);
                result.addAll(Arrays.asList(dependencies.split(File.pathSeparator)));
                break;
            }
        }

        return result;
    }

    public CommandResult executeCommand(final String command, final String pomFilePath)
            throws MavenInvocationException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(pomFilePath));
        request.setGoals(Collections.singletonList(command));
        Invoker invoker = new DefaultInvoker();
        final List<String> output = new ArrayList<>();
        InvocationOutputHandler handler = output::add;
        invoker.setOutputHandler(handler);
        InvocationResult execute = invoker.execute(request);
        return new CommandResult(execute.getExitCode(), output, execute.getExecutionException());
    }

    public String getPomForFile(final String fileName) throws IOException {
        final Path currentPath = Paths.get(fileName);
        Path currentDirPath;
        if (currentPath.toFile().isDirectory()) {
            currentDirPath = currentPath;
        } else {
            currentDirPath = currentPath.getParent();
        }
        String pomForDir = findPomForDir(currentDirPath);
        while (pomForDir == null && currentDirPath != null) {
            currentDirPath = currentDirPath.getParent();
            pomForDir = findPomForDir(currentDirPath);
        }
        return pomForDir;
    }

    private String findPomForDir(final Path directoryPath) throws IOException {
        if (directoryPath == null) {
            return null;
        }
        File[] listFiles = directoryPath.toFile().listFiles();
        if (listFiles == null) {
            return null;
        }
        return Arrays.stream(listFiles)
                .filter(file ->
                        file.isFile() && file.getName().equals(POM_FILE_NAME))
                .findFirst()
                .map(File::getAbsolutePath).orElse(null);
    }

    public static MavenSupport getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MavenSupport();
        }
        return INSTANCE;
    }

    public boolean isMavenInstalled() {
        return isMavenInstalled;
    }

    public static class CommandResult {

        private final int code;
        private final List<String> output;
        private final CommandLineException commandLineException;

        public CommandResult(int code, List<String> output, CommandLineException commandLineException) {
            this.code = code;
            this.output = output;
            this.commandLineException = commandLineException;
        }

        public int getCode() {
            return code;
        }

        public List<String> getOutput() {
            return output;
        }

        public CommandLineException getCommandLineException() {
            return commandLineException;
        }
    }
}
