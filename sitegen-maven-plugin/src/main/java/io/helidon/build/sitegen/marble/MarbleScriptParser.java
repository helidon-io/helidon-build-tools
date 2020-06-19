package io.helidon.build.sitegen.marble;

import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

public class MarbleScriptParser {

    private static final Pattern MARBLE = Pattern.compile("\\s*[\\[\\{\\<\\(][a-zA-Z0-9\\s]+[\\]\\}\\>\\)]\\s*");
    private static final Pattern COMPLETE = Pattern.compile("\\s*\\|\\s*");
    private static final Pattern ERROR = Pattern.compile("\\s*X\\s*");

    private static final Map<String, String> MARBLE_COLORS =
            Map.of("[]", "#ff8080", "{}", "#00ffbf", "()", "#ccd9ff", "<>", "#66cc66");

    private String doc;

    public MarbleScriptParser(String doc) {
        this.doc = doc;
    }

    public MarbleDiagram parse() {
        String[] lines = doc.split("\n");
        MarbleDiagram diagram = new MarbleDiagram();

        for (String line : lines) {
            if (line.endsWith(">")) {
                diagram.stream(parseStream(line));
            } else {
                diagram.stream(Operator.create(line.trim()));
            }
        }
        return diagram;
    }

    /**
     * upstream   ----[1] ----(2) ----{3} ----<4> ----|---->
     */
    public static Stream parseStream(String line) {
        Scanner s = new Scanner(line);
        s.useDelimiter("-");
        String label = "";
        if (s.hasNext("^[^-]*")) {
            label = s.next("^[^-]*").trim();
        }

        int offset = 2;
        Stream stream = Stream.create(label);

        while (s.hasNext()) {
            while (s.hasNext("\\s*")) {
                s.next();
                offset++;
            }

            while (s.hasNext(MARBLE)) {
                String marble = s.next().trim();
                String marbleLabel = marble.substring(1, marble.length() - 1).trim();
                String prefix = marble.substring(0, 1);
                String postfix = marble.substring(marble.length() - 1);
                offset++;
                stream.signal(Marble.create(marbleLabel, offset * 30, MARBLE_COLORS.get(prefix + postfix)));
            }

            while (s.hasNext(ERROR)) {
                s.next();
                offset++;
                stream.signal(ErrorSignal.create(offset * 30));
            }

            while (s.hasNext(COMPLETE)) {
                s.next();
                offset++;
                stream.signal(CompleteSignal.create(offset * 30));
            }

            if (s.hasNext(">")) {
                // End of line
                break;
            }
        }
        return stream;
    }


}
