import de.thkoeln.fentwums.netlist.backend.parser.GraphCreator;
import de.thkoeln.fentwums.netlist.backend.parser.NetlistParser;
import jdk.jshell.spi.ExecutionControl;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.util.BasicProgressMonitor;
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

        RecursiveGraphLayoutEngine layouter = new RecursiveGraphLayoutEngine();
        BasicProgressMonitor monitor = new BasicProgressMonitor();

        start = Instant.now();
        layouter.layout(graphCreator.getGraph(), monitor);
        end = Instant.now();

        System.out.println("Graph layouting time: " + Duration.between(start, end).toMillis() + "ms");

        String jsongraph =
                ElkGraphJson.forGraph(graphCreator.getGraph()).omitLayout(false).omitZeroDimension(true)
                        .omitZeroPositions(true).shortLayoutOptionKeys(false).prettyPrint(true).toJson();

        System.out.println(jsongraph);

        System.out.println("done");
    }
}
