package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.options.FEntwumSOptions;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SanityChecker {
    private static Logger logger = LoggerFactory.getLogger(SanityChecker.class);

    public SanityChecker() {
    }

    public void checkGraph(ElkNode graph) {
        for (ElkNode child : graph.getChildren()) {
            checkChildren(child);
        }
    }

    private void checkChildren(ElkNode currentNode) {
        checkEdges(currentNode);
        checkPorts(currentNode);

        for (ElkNode child : currentNode.getChildren()) {
            checkChildren(child);
        }
    }

    private void checkPorts(ElkNode currentNode) {
        for (ElkPort port : currentNode.getPorts()) {
            checkIfPortEmpty(port);

            // unusedPortOptionRemover must run after checkIfPortEmpty because the information being removed is needed
            unusedPortOptionRemover(port);
        }
    }

    private void checkEdges(ElkNode currentNode) {
        for (ElkEdge edge : currentNode.getContainedEdges()) {
            checkEdgeDirection(edge);
        }
    }

    private void unusedPortOptionRemover(ElkPort port) {
        port.setProperty(FEntwumSOptions.PORT_GROUP_NAME, null);
    }

    private void checkEdgeDirection(ElkEdge edge) {
        if (edge.getSources().getFirst() == null || edge.getTargets().getFirst() == null) {
            logger.atError().setMessage("Edge {} has no target/source").addArgument(edge).log();

            return;
        }

        if (edge.getSources().getFirst().getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST) && !edge.getTargets().getFirst().getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST)) {
            logger.atError().setMessage("Edge {} goes fromm sink to source (wrong direction)").addArgument(edge).log();
        }
    }

    private void checkIfPortEmpty(ElkPort port) {
        if (port.getIncomingEdges().isEmpty() && !port.getParent().getParent().getIdentifier().equals("root") && !port.getParent().getChildren().isEmpty() && !port.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
            logger.atError().setMessage("Port {} in port group {} has no incoming edges").addArgument(port).addArgument(port.getProperty(FEntwumSOptions.PORT_GROUP_NAME)).log();
        }

        if (port.getOutgoingEdges().isEmpty() && !port.getParent().getChildren().isEmpty() && !port.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST) && !port.getParent().getParent().getIdentifier().equals("root")) {
            logger.atError().setMessage("Port {} in port group {} has no outgoing edges").addArgument(port).addArgument(port.getProperty(FEntwumSOptions.PORT_GROUP_NAME)).log();
        }
    }
}
