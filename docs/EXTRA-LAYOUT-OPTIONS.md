This document lists all the custom ELK layout options that are part of this project. Their intended uses are listed as well.

# `de.thkoeln.fentwums.netlist.backend.signalname`
<details open>

### Shortened option key: `signalname`
### Type: `String`
### Intended use: Contains the name of the signal this edge belongs to
### Target: Edges
</details>

# `de.thkoeln.fentwums.netlist.backend.signalindex`
<details open>

### Shortened option key: `signalindex`
### Type: `Integer`
### Intended use: Contains the bitindex of the signal this edge belongs to. It is globally unique within the netlist and is a positive integer greater than 1
### Target: Edges
</details>

# `de.thkoeln.fentwums.netlist.backend.cellname`
<details open>

### Shortened option key: `cellname`
### Type: `String`
### Intended use: Contains the name of the cell described by this node
### Target: Nodes
</details>

# `de.thkoeln.fentwums.netlist.backend.celltype`
<details open>

### Shortened option key: `celltype`
### Type: `String`
### Intended use: Contains the type of the cell described by this node. Constant drivers are of type `Constant driver`. A list of the celltypes yosys is able to generate can be found [here](https://yosyshq.readthedocs.io/projects/yosys/en/latest/cell_index.html).
### Target: `Nodes`
</details>

# `de.thkoeln.fentwums.netlist.backend.location-path`
<details open>

### Shortened option key: `location-path`
### Type: `String`
### Intended use: Contains the location of the cell or part of the signal described by this element inside the netlist hierarchy
### Targets: `Nodes` and `Edges`
</details>

# `de.thkoeln.fentwums.netlist.backend.src-location`
<details open>

### Shortened option key: `src-location`
### Type: `String`
### Intended use: Contains the location of the HDL-Code that is represented (in part or completely) by this element. Contains the absolute file path as well as line and column numbers
### Targets: `Nodes` and `Edges`
</details>

# `de.thkoeln.fentwums.netlist.backend.signalvalue`
<details open>

### Shortened option key: `signalvalue`
### Type: `String`
### Intended use: Contains the current value of the signal represented by this edge. Each character is the value of one of the contained bitindices.
### Target: `Edges`
</details>

# `de.thkoeln.fentwums.netlist.backend.index-in-signal`
<details open>

### Shortened option key: `index-in-signal`
### Type: `Integer`
### Intended use: Contains the index of the signal described by this edge inside its containing signal (a vector). `-1` if no containing signal exists
### Target: `Edges`
</details>

# `de.thkoeln.fentwums.netlist.backend.signaltype`
<details open>

### Shortened option key: `signaltype`
### Type: `String`
### Value: One of `CONSTANT`, `BUNDLED_CONSTANT`, `BUNDLED`, `SINGLE` or `UNDEFINED`
### Intended use: Contains information about the type of signal represented by this edge
### Target: `Edges`
</details>

# `de.thkoeln.fentwums.netlist.backend.port-group-name`
<details open>

### Shortened option key: `port-group-name`
### Type: `String`
### Intended use: Contains the group a port belongs to, e.g. `D`, `Q`, `S` ...
### Target: `Ports`
</details>