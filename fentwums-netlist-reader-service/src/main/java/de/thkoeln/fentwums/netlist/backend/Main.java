package de.thkoeln.fentwums.netlist.backend;

import de.thkoeln.fentwums.netlist.backend.parser.GraphCreator;
import de.thkoeln.fentwums.netlist.backend.parser.NetlistParser;
import jdk.jshell.spi.ExecutionControl;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.json.ElkGraphJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;


public class Main {
    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, ExecutionControl.NotImplementedException {
        GraphCreator graphCreator = new GraphCreator();
        NetlistParser parser = new NetlistParser("/optimal-info2.json");

        Instant start = Instant.now();
        parser.readNetlistFile();
        parser.checkReadNetlist();
        Instant end = Instant.now();

        logger.atInfo().setMessage("Read time: {} ms").addArgument(Duration.between(start, end).toMillis()).log();

        start = Instant.now();
        logger.atInfo().setMessage("{}").addArgument(parser.getToplevelName()).log();
        graphCreator.createGraphFromNetlist(parser.getModuleToParse(), parser.getToplevelName());
        end = Instant.now();

        logger.atInfo().setMessage("Graph creation time: {} ms").addArgument(Duration.between(start, end).toMillis()).log();

        RecursiveGraphLayoutEngine layouter = new RecursiveGraphLayoutEngine();
        BasicProgressMonitor monitor = new BasicProgressMonitor();

        start = Instant.now();
        try {
            layouter.layout(graphCreator.getGraph(), monitor);
        } catch (StackOverflowError e) {
            logger.error("Stack overflow; Graph too big :(", e);
        }
        end = Instant.now();

        logger.atInfo().setMessage("Graph layouting time: {} ms").addArgument(Duration.between(start, end).toMillis()).log();

        String jsongraph =
                ElkGraphJson.forGraph(graphCreator.getGraph()).omitLayout(false).omitZeroDimension(true)
                        .omitZeroPositions(true).shortLayoutOptionKeys(true).prettyPrint(false).toJson();

        try {
            Path outputFile = Paths.get("graph.json");

            Files.writeString(outputFile, jsongraph);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.info("done");
    }
}
