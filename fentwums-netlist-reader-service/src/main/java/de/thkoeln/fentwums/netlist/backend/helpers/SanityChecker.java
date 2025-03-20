package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.options.FEntwumSOptions;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.PortSide;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs checks on the layoutable graph after it has been created to ensure that the layoutable graph is in fact
 * layoutable.
 */
public class SanityChecker {
	private static Logger logger = LoggerFactory.getLogger(SanityChecker.class);

	public SanityChecker() {
	}

	/**
	 * Starts the check of the whole graph
	 *
	 * @param graph The graph to be checked
	 */
	public void checkGraph(ElkNode graph) {
		for (ElkNode child : graph.getChildren()) {
			checkChildren(child);
		}
	}

	/**
	 * Checks the edges contained within and ports attached to <code>currentNode</code>. Then checks the children of
	 * <code>currentNode</code>
	 *
	 * @param currentNode The node to be checked
	 */
	private void checkChildren(ElkNode currentNode) {
		checkEdges(currentNode);
		checkPorts(currentNode);

		for (ElkNode child : currentNode.getChildren()) {
			checkChildren(child);
		}
	}

	/**
	 * Executes the check passes for ports
	 *
	 * @param currentNode The node the ports are attached to
	 */
	private void checkPorts(ElkNode currentNode) {
		for (ElkPort port : currentNode.getPorts()) {
			checkIfPortEmpty(port);

			// unusedPortOptionRemover must run after checkIfPortEmpty because the information being removed is needed
			unusedPortOptionRemover(port);
		}
	}

	/**
	 * Executes the check passes for edges
	 *
	 * @param currentNode The node the edge is contained in
	 */
	private void checkEdges(ElkNode currentNode) {
		for (ElkEdge edge : currentNode.getContainedEdges()) {
			checkEdgeDirection(edge);
		}
	}

	/**
	 * Removes the <code>PORT_GROUP_NAME</code>-attribute from <code>port</code>
	 *
	 * @param port The port
	 */
	private void unusedPortOptionRemover(ElkPort port) {
		port.setProperty(FEntwumSOptions.PORT_GROUP_NAME, null);
	}

	/**
	 * Checks the direction of the edge. Checks whether the edge has no sink or source and whether the edge provably goes
	 * in the wrong direction (sink to source)
	 *
	 * @param edge The edge to be checked
	 */
	private void checkEdgeDirection(ElkEdge edge) {
		if (edge.getSources().getFirst() == null || edge.getTargets().getFirst() == null) {
			logger.atError().setMessage("Edge {} has no target/source").addArgument(edge).log();

			return;
		}

		if (edge.getSources().getFirst().getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST) && !(edge.getTargets().getFirst().getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST) || edge.getTargets().getFirst().getProperty(CoreOptions.PORT_SIDE).equals(PortSide.SOUTH))) {
			logger.atError().setMessage("Edge {} goes from sink to source (wrong direction)").addArgument(edge).log();
		}
	}

	/**
	 * Checks if the port is missing incoming or outgoing edges, even it should have both
	 * @param port The port to be checked
	 */
	private void checkIfPortEmpty(ElkPort port) {
		if (port.getIncomingEdges().isEmpty() && !port.getParent().getParent().getIdentifier().equals("root") && !port.getParent().getChildren().isEmpty() && !port.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.EAST)) {
			logger.atWarn().setMessage("Port {} in port group {} has no incoming edges").addArgument(port).addArgument(port.getProperty(FEntwumSOptions.PORT_GROUP_NAME)).log();
		}

		if (port.getOutgoingEdges().isEmpty() && !port.getParent().getChildren().isEmpty() && !port.getProperty(CoreOptions.PORT_SIDE).equals(PortSide.WEST) && !port.getParent().getParent().getIdentifier().equals("root")) {
			logger.atWarn().setMessage("Port {} in port group {} has no outgoing edges").addArgument(port).addArgument(port.getProperty(FEntwumSOptions.PORT_GROUP_NAME)).log();
		}
	}
}
