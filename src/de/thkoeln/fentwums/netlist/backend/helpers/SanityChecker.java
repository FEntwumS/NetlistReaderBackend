package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.options.FEntwumSOptions;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;

public class SanityChecker {
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
            unusedPortOptionremover(port);
        }
    }

    private void checkEdges(ElkNode currentNode) {
        for (ElkEdge edge : currentNode.getContainedEdges()) {
            checkEdgeDirection(edge);
        }
    }

    private void unusedPortOptionremover(ElkPort port) {
        port.setProperty(FEntwumSOptions.PORT_GROUP_NAME, null);
    }

    private void checkEdgeDirection(ElkEdge edge) {
        if (edge.getSources().getFirst() == null || edge.getTargets().getFirst() == null) {
            System.out.println("Edge " + edge + " has no target/source");

            return;
        }

        if (edge.getSources().getFirst().getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST) && edge.getTargets().getFirst().getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
            System.out.println("Edge " + edge + " goes from target to source");
        }
    }
}
