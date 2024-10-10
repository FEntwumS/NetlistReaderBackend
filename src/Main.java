import de.thkoeln.fentwums.netlist.backend.parser.GraphCreator;
import de.thkoeln.fentwums.netlist.backend.parser.NetlistParser;
import jdk.jshell.spi.ExecutionControl;
import org.eclipse.elk.graph.json.ElkGraphJson;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class Main {
    public static void main(String[] args) throws IOException, ExecutionControl.NotImplementedException {
        GraphCreator graphCreator = new GraphCreator();
        NetlistParser parser = new NetlistParser("src/optimal-info.json");

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

        System.out.println(graphCreator.getGraph());
        System.out.println(graphCreator.getGraph().getChildren());

        String jsongraph =
                ElkGraphJson.forGraph(graphCreator.getGraph()).omitLayout(true).omitZeroDimension(true)
                        .omitZeroPositions(true).shortLayoutOptionKeys(false).prettyPrint(true).toJson();

        System.out.println(jsongraph);

        System.out.println("done");
    }
}
