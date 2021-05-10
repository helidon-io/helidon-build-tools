package io.helidon.lsp.server.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShellCommandHelper {

    private static final Runtime RUNTIME = Runtime.getRuntime();
    private static final Logger LOGGER = Logger.getLogger(ShellCommandHelper.class.getName());

    public static List<String> execute(final String command) {
        Process process = null;
        List<String> result = new ArrayList<>();
        try {
            process = RUNTIME.exec(command);
            process.waitFor();

            BufferedReader buf = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = buf.readLine()) != null) {
                result.add(line);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "error executing the command : " + command, e);
        }
        return result;
    }

    public static List<String> execute(final String command, final File dir) {
        Process process = null;
        List<String> result = new ArrayList<>();
        try {
            process = RUNTIME.exec(command, new String[]{}, dir);
            process.waitFor();

            BufferedReader buf = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = buf.readLine()) != null) {
                result.add(line);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "error executing the command : " + command, e);
        }
        return result;
    }
}
