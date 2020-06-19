package io.helidon.build.sitegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import io.helidon.build.sitegen.marble.CompleteSignal;
import io.helidon.build.sitegen.marble.Marble;
import io.helidon.build.sitegen.marble.MarbleDiagram;
import io.helidon.build.sitegen.marble.MarbleScriptParser;
import io.helidon.build.sitegen.marble.Operator;
import io.helidon.build.sitegen.marble.Stream;

import org.junit.jupiter.api.Test;

public class MarbleDiagramRenderTest {
    @Test
    void name() throws IOException {
        MarbleDiagram diagram = new MarbleDiagram();

        Stream upstream = Stream.create("stream 1");
        upstream.signal(Marble.create("1", 200, "#ff8080"));
        upstream.signal(Marble.create("2", 300, "#00ffbf"));
        upstream.signal(Marble.create("3", 400, "#ccd9ff"));
        upstream.signal(CompleteSignal.create(450));

        Stream upstream3 = Stream.create("");
        upstream3.signal(Marble.create("11", 200, "#ff8080"));
        upstream3.signal(Marble.create("12", 300, "#00ffbf"));
        upstream3.signal(Marble.create("13", 400, "#ccd9ff"));
        upstream3.signal(CompleteSignal.create(450));


        diagram.stream(upstream);
        diagram.stream(Operator.create("Multi.map(i -> i + 10)"));
        diagram.stream(upstream3);

        Path path = Paths.get("target", "Marbles.html");
        byte[] strToBytes = diagram.render().getBytes();
        Files.write(path, strToBytes);
    }

    @Test
    void parse() throws IOException {
        render("Map");
        render("FlatMap");
        render("Concat");
    }

    private void render(String name) throws IOException {
        Path marbleScriptPath = Paths.get("src", "test", "resources", "marble", name + ".marble");
        String marbleScript = new String(Files.readAllBytes(marbleScriptPath));
        System.out.println(marbleScript);
        MarbleScriptParser scriptParser = new MarbleScriptParser(marbleScript);
        MarbleDiagram diagram = scriptParser.parse();

        Path path = Paths.get("target", "marble", name + ".html");
        path.getParent().toFile().mkdirs();
        byte[] strToBytes = diagram.render().getBytes();
        Files.write(path, strToBytes);
    }
//
//    @Test
//    void name3() {
//        Scanner s = new Scanner("stream2   ----         ----- {3} --<4>    - |  ->");
//        s.useDelimiter("-");
//        int i = 1;
//        while (s.hasNext()) {
//            System.out.println(i++ + ">" + s.next() + "<");
//        }
//    }
}
