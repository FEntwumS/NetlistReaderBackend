package de.thkoeln.fentwums.netlist.backend.datatypes;

import org.eclipse.elk.graph.ElkPort;

import java.util.ArrayList;
import java.util.List;

public class SignalOccurences {
    private ElkPort inPort;
    private List<ElkPort> outPorts;

    public SignalOccurences() {
        inPort = null;
        outPorts = new ArrayList<ElkPort>();
    }

    public SignalOccurences(ElkPort inPort, List<ElkPort> outPorts) {
        this.inPort = inPort;
        this.outPorts = outPorts;
    }

    public void setInPort(ElkPort inPort) {
        this.inPort = inPort;
    }

    public ElkPort getInPort() {
        return inPort;
    }

    public void setOutPorts(List<ElkPort> outPorts) {
        this.outPorts = outPorts;
    }

    public List<ElkPort> getOutPorts() {
        return outPorts;
    }
}
