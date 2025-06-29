package de.thkoeln.fentwums.netlist.backend.datatypes;

import org.eclipse.elk.graph.ElkPort;

import java.util.ArrayList;
import java.util.List;

public class SignalOccurences {
    private ElkPort sourcePort;
    private List<ElkPort> sinkPorts;

    public SignalOccurences() {
        sourcePort = null;
        sinkPorts = new ArrayList<ElkPort>();
    }

    public SignalOccurences(ElkPort sourcePort, List<ElkPort> sinkPorts) {
        this.sourcePort = sourcePort;
        this.sinkPorts = sinkPorts;
    }

    public void setSourcePort(ElkPort sourcePort) {
        this.sourcePort = sourcePort;
    }

    public ElkPort getSourcePort() {
        return sourcePort;
    }

    public void setSinkPorts(List<ElkPort> sinkPorts) {
        this.sinkPorts = sinkPorts;
    }

    public List<ElkPort> getSinkPorts() {
        return sinkPorts;
    }
}
