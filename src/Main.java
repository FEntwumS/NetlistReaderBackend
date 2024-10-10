import de.thkoeln.fentwums.netlist.backend.parser.GraphCreator;
import de.thkoeln.fentwums.netlist.backend.parser.NetlistParser;
import jdk.jshell.spi.ExecutionControl;
import org.eclipse.elk.graph.json.ElkGraphJson;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, ExecutionControl.NotImplementedException {
        GraphCreator graphCreator = new GraphCreator();
        NetlistParser parser = new NetlistParser("C:\\Users\\flori\\Documents\\Semester\\7\\Praxisphase" +
                "\\NetlistReaderBackend\\src\\optimal-info.json");

        parser.readNetlist();
        parser.checkReadNetlist();

        System.out.println(parser.getToplevelName());
        graphCreator.createGraphFromNetlist(parser.getModuleToParse(), parser.getToplevelName());

        System.out.println(graphCreator.getGraph());
        System.out.println(graphCreator.getGraph().getChildren());

        String jsongraph =
                ElkGraphJson.forGraph(graphCreator.getGraph()).omitLayout(true).omitZeroDimension(true)
                        .omitZeroPositions(true).shortLayoutOptionKeys(false).prettyPrint(true).toJson();

        System.out.println(jsongraph);

        System.out.println("done");
    }
}
