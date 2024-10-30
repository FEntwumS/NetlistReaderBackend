import de.thkoeln.fentwums.netlist.backend.parser.GraphCreator;
import de.thkoeln.fentwums.netlist.backend.parser.NetlistParser;
import jdk.jshell.spi.ExecutionControl;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.core.util.LoggedGraph;
import org.eclipse.elk.graph.json.ElkGraphJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

public class Main {
    public static void main(String[] args) throws IOException, ExecutionControl.NotImplementedException {
        GraphCreator graphCreator = new GraphCreator();
        NetlistParser parser = new NetlistParser("src/optimal-info-postopt.json");

        Instant start = Instant.now();
        parser.readNetlist();
        parser.checkReadNetlist();
        Instant end = Instant.now();

        System.out.println("Read time: " + Duration.between(start, end).toMillis() + "ms");

        start = Instant.now();
        System.out.println(parser.getToplevelName());
        graphCreator.createGraphFromNetlist(parser.getModuleToParse(), parser.getToplevelName());
        end = Instant.now();

        System.out.println("Graph creation time: " + Duration.between(start, end).toMillis() + "ms");

        RecursiveGraphLayoutEngine layouter = new RecursiveGraphLayoutEngine();
        BasicProgressMonitor monitor = new BasicProgressMonitor();

        start = Instant.now();
        try {
            layouter.layout(graphCreator.getGraph(), monitor);
        } catch (StackOverflowError e) {
            System.out.println("Stack overflow; Graph too big :(");
            e.printStackTrace();
        }
        end = Instant.now();

        System.out.println("Graph layouting time: " + Duration.between(start, end).toMillis() + "ms");

        String jsongraph =
                ElkGraphJson.forGraph(graphCreator.getGraph()).omitLayout(false).omitZeroDimension(true)
                        .omitZeroPositions(true).shortLayoutOptionKeys(true).prettyPrint(false).toJson();

        try {
            jsongraph = jsongraph.replace("\"org.eclipse.elk.resolvedAlgorithm\": \"Layout Algorithm: org.eclipse.elk" +
                    ".layered\",", "");
            jsongraph = jsongraph.replace("\"org.eclipse.elk.resolvedAlgorithm\": \"Layout Algorithm: org.eclipse.elk" +
                    ".layered\",\n", "");
            jsongraph = jsongraph.replace("\"resolvedAlgorithm\": \"Layout Algorithm: org.eclipse.elk.layered\",\n",
                    "");
            jsongraph = jsongraph.replace("\"resolvedAlgorithm\": \"Layout Algorithm: org.eclipse.elk.layered\",",
                    "");

            Path outputFile = Paths.get("graph.json");

            Files.writeString(outputFile, jsongraph);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("done");
    }
}
