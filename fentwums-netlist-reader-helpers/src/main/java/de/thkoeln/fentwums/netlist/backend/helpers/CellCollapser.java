package de.thkoeln.fentwums.netlist.backend.helpers;

import de.thkoeln.fentwums.netlist.backend.datatypes.HierarchicalNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.ModuleNode;
import de.thkoeln.fentwums.netlist.backend.interfaces.ICollapsableNode;
import de.thkoeln.fentwums.netlist.backend.interfaces.IGraphCreator;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.SizeConstraint;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.eclipse.emf.common.util.EList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Collapses and expands cells as directed. Enables the interactive viewing of the hierarchy. The more cells are
 * collapsed, the fewer cells need to actually be laid out. This improves the resulting layout and shortens the total
 * layouting time
 */
public class CellCollapser {
	private ICollapsableNode rootNode;
	private static Logger logger = LoggerFactory.getLogger(CellCollapser.class);

	public CellCollapser() {
	}

	/**
	 * Sets the root node
	 *
	 * @param rootNode
	 */
	public void setRootNode(ICollapsableNode rootNode) {
		this.rootNode = rootNode;
	}

	/**
	 * Gets the root node
	 *
	 * @return The root node
	 */
	public ICollapsableNode getRootNode() {
		return rootNode;
	}

	/**
	 * Collapses the cell at the location specified in <code>path</code>. If the cell is already collapsed, nothing
	 * happens
	 *
	 * @param cellPath The location of the cell that is to be collapsed
	 */
	public void collapseCellAt(String cellPath) {
		collapseCell(findNode(cellPath));

		// path should contain modulename as first parameter, but we can ignore it here
		// paths longer than 1 point to nodes below the toplevel, paths with length 1 point to the toplevel
	}

	/**
	 * Collapses all cells in the graph
	 */
	public void collapseAllCells() {
		// stores the children and contained edges of each node in the hierarchyTree and clears the respective
		// ElkNodes' lists

		collapseRecursively(rootNode);
	}

	/**
	 * Collapses the specified cell and all its children recursively
	 *
	 * @param hNode The cell that is to be collapsed
	 */
	public void collapseRecursively(ICollapsableNode hNode) {
		collapseCell(hNode);

		for (String hChild : hNode.getChildren().keySet()) {
			collapseRecursively(hNode.getChildren().get(hChild));
		}
	}

	/**
	 * Collapse the cell associated with <code>hNode</code>. This clears the lists referencing the children and
	 * contained edges of the associated <code>ElkNode</code>. The contents of both lists are saved in the hierarchy
	 * to allow for later re-expansion. If the cell is already collapsed, nothing happens
	 *
	 * @param hNode The cell to be collapsed
	 */
	public void collapseCell(ICollapsableNode hNode) {
		ElkNode currentGraphNode = hNode.getNode();

		if (hNode.getChildList() == null) {
			hNode.setChildList(new ArrayList<>());
		}

        if (!hNode.getChildList().isEmpty()) {
            hNode.getChildList().clear();
        }
        hNode.getChildList().addAll(currentGraphNode.getChildren());

        currentGraphNode.getChildren().clear();

		if (hNode.getEdgeList() == null) {
			hNode.setEdgeList(new ArrayList<ElkEdge>());
		}

        if (!hNode.getEdgeList().isEmpty()) {
            hNode.getEdgeList().clear();
        }
        hNode.getEdgeList().addAll(currentGraphNode.getContainedEdges());

        currentGraphNode.getContainedEdges().clear();

		if (hNode instanceof ModuleNode && hNode.getNode().getParent() != null) {

			if (((ModuleNode) hNode).getInnerSelfLoopEdgeList() == null) {
				((ModuleNode) hNode).setInnerSelfLoopEdgeList(new ArrayList<>());
			}

			for (ElkEdge edge : hNode.getNode().getParent().getContainedEdges()) {
				if (((ElkPort) edge.getTargets().getFirst()).getParent().equals(hNode.getNode()) && edge.getProperty(CoreOptions.INSIDE_SELF_LOOPS_YO).equals(true)) {
					((ModuleNode) hNode).getInnerSelfLoopEdgeList().add(edge);
				}
			}

			hNode.getNode().getParent().getContainedEdges().removeAll(((ModuleNode) hNode).getInnerSelfLoopEdgeList());
		}

		resetDimensionRecursively(currentGraphNode);
	}

	/**
	 * Expands all cells
	 */
	public void expandAllCells() {
		expandRecursively(rootNode);
	}

	/**
	 * Expands the specified cell and all its children recursively
	 *
	 * @param hNode The cell that is to be expanded
	 */
	public void expandRecursively(ICollapsableNode hNode) {
		expandCell(hNode);

		for (String hChild : hNode.getChildren().keySet()) {
			expandRecursively((HierarchicalNode) hNode.getChildren().get(hChild));
		}
	}

	/**
	 * Expands the cell at the location indicated by <code>cellPath</code>
	 *
	 * @param cellPath The location of the cell to be expanded
	 */
	public void expandCellAt(String cellPath) {
		expandCell(findNode(cellPath));
		// find layer in hierarchy, then restore children and edges from hierarchyTree
		// allows  storing of state of collapsed nodes inside the node pointed to by cellPath without additional code
	}

	/**
	 * Expands the cell associated with <code>cNode</code>. The associated <code>ElkNode</code>'s child and contained
	 * edge lists are repopulated from the hierarchy. If the cell is already expanded, do nothing
	 *
	 * @param cNode The cell to be expanded
	 */
	public void expandCell(ICollapsableNode cNode) {
		ElkNode currentGraphNode = cNode.getNode();
		EList<ElkNode> graphChildren = currentGraphNode.getChildren();
		EList<ElkEdge> graphContainedEdges = currentGraphNode.getContainedEdges();

		if (!graphChildren.isEmpty() || !graphContainedEdges.isEmpty()) {
			return;
		}

		List<ElkNode> storedChildren = cNode.getChildList();
		List<ElkEdge> storedEdges = cNode.getEdgeList();

		graphChildren.addAll(storedChildren);

		graphContainedEdges.addAll(storedEdges);

		if (cNode instanceof ModuleNode && cNode.getNode().getParent() != null) {
			if (((ModuleNode) cNode).getInnerSelfLoopEdgeList() != null) {
				cNode.getNode().getParent().getContainedEdges().addAll(((ModuleNode) cNode).getInnerSelfLoopEdgeList());
			}
		}

		resetDimensionRecursively(currentGraphNode);
	}

	/**
	 * Finds the <code>HierarchyNode</code> at the position indicated by <code>cellPath</code>. The path should not
	 * contain the name of the top level entity. Each fragment of the path is to be separated by a single space
	 * character
	 *
	 * @param cellPath The location of the wanted cell
	 * @return The found HierarchicalNode or null, if no matching cell could be found
	 */
	public ICollapsableNode findNode(String cellPath) {
		String[] cellPathSplit = cellPath.trim().split(" ");

		ICollapsableNode currentNode = rootNode;

		for (int i = 0; i < cellPathSplit.length; i++) {
			if (i == 0 && cellPathSplit.length > 1 && rootNode.getChildren().containsKey(cellPathSplit[i + 1])) {
				continue;
			}

			currentNode = currentNode.getChildren().get(cellPathSplit[i]);

			if (currentNode == null) {
				logger.atError().setMessage("Could not find cell {} from cellpath {}").addArgument(cellPathSplit[i]).addArgument(cellPath).log();

				return null;
			}
		}

		return currentNode;
	}

	/**
	 * Toggles whether the cell at the location indicated by <code>cellPath</code> is collapsed or not. If the cell is
	 * currently collapsed, it is expanded. If the cell is currently expanded, it is collapsed. This allows for a
	 * simpler interactive frontend
	 *
	 * @param cellPath The path of the cell
	 */
	public void toggleCollapsed(String cellPath) {
		ICollapsableNode node = findNode(cellPath);

		if (!node.getEdgeList().isEmpty() || !node.getChildList().isEmpty()) {
			// node can have child elements

			if (!node.getNode().getChildren().isEmpty() || !node.getNode().getContainedEdges().isEmpty()) {
				collapseCell(node);
			} else {
				expandCell(node);
			}
		} else {
			logger.atInfo().setMessage("Cell with path {} does not contain any children and can therefore neither be" +
					" " +
					"expanded nor collapsed").addArgument(cellPath).log();
		}
	}

	/**
	 * Removes the <code>NODE_SIZE_MINIMUM</code> property and resets the dimension and
	 * <code>NODE_SIZE_CONSTRAINTS</code> option of the specified node and its parents recursively. This forces a
	 * complete re-layout for all nodes affected by a collapse/expansion during the layout process
	 *
	 * @param node The node that is to be re-layouted
	 */
	private void resetDimensionRecursively(ElkNode node) {
		if (node.getParent() != null) {
			node.setProperty(CoreOptions.NODE_SIZE_MINIMUM, null);
			node.setProperty(CoreOptions.NODE_SIZE_CONSTRAINTS, EnumSet.allOf(SizeConstraint.class));
			node.setDimensions(0, 0);

			resetDimensionRecursively(node.getParent());
		}
	}
}
