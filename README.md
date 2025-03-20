# Important

This is a beta release. Bugs are to be expected. If you notice any issues, please submit bug reports to help us improve
the software.

## System requirements

The system running this backend needs to have Java 21 or higher installed.

# Limitations

## Netlist layout

The backend can only process a subset of all possible netlist layouts. It is recommended to generate the input netlist
by running the following command after first reading the HDL code:

```
yosys -p "hierarchy -top <toplevel name>; proc; memory -nomap; flatten -scopename; write_json -compat-int netlist.json"
```

# Structure

This repository contains two modules that together represent the backend component of the FEntwumS Netlist Viewer.

## fentwums-netlist-viewer-service

The service modules contains the low-level logic of the backend. It is responsible for parsing the netlist and
translating it into a layoutable graph. It also provides basic functions to allow interactions with the netlist, e.g.
collapsing or expanding a layer.

## fentwums-netlist-viewer-server

The server module provides the communications interface to allow a suitable frontend (such as our OneWare Studio
Plugin [Oneware.NetlistReaderFrontend](https://github.com/FEntwumS/Oneware.NetlistReaderFrontend)) to layout a given
netlist. The server can be used locally (on the same machine as the frontend) or it can be used remotely to allow
multiple users to experience a fast and responsive viewing experience.

# That's great! Where (and how) can I view my netlists?

This repository only contains the backend components of the viewer system. To actually view your HDL-Designs, please use
a designated frontend such as our OneWare Studio
Plugin [Oneware.NetlistReaderFrontend](https://github.com/FEntwumS/Oneware.NetlistReaderFrontend) or you can always
build your own frontend.

# Can I use the backend separately from the frontend?

&#x26A0;**IMPORTANT**&#x26A0;: The connection between client and server is currently not encrypted or otherwise
protected, therefore anybody on your network may read and modify the data exchanged between client and server.

Yes, you can use our precompiled jar file from the latest release or build your own jar. The jar itself contains
everything the server needs to start and run. Use `java -jar <name-of-jar>` to start the server. The server can be shut
down by accessing `<server-ip>:<server-port>/shutdown-backend` e.g. from your browser.

# Prerequisites

- Minimum Java Version: 21
- Maven

# Build

By running

```
mvn clean package spring-boot:repackage install
```

in the root directory, an über-JAR containing all dependencies can be created. The resulting jar located under
`fentwums-netlist-reader-server/target` can then be executed using

```
java -Xmx16G -jar fentwums-netlist-reader-server-0.8.0-exec.jar
```

Alternatively, from the `fentwums-netlist-reader-server` directory, you can execute the `spring-boot:run` goal like so

```
mvn clean package spring-boot:run
```

# Notes

## FEntwumS

This project is part of the [DI-FEntwumS research project](https://www.elektronikforschung.de/projekte/di-fentwums).
This project aims to establish an Open-Source IDE for FPGA development. The FEntwumS Netlist Viewer is a component in
the visualisation and simulation software that is to be developed.

## System requirements

Since the layouting process is mostly single-threaded by nature, a modern machine with high single-core performance is
recommended for running the server. The CPU does not need to have a lot of cores, it should just be fast.

Depending on the size of the netlists that are to be layouted, the backend can use quite a lot of system memory to store
all the necessary information. Therefore, it is important to provide an appropriate amount of RAM to the server using
the java -Xmx option. 

## Log generation

The backend automatically stores its logs in directory where the jar is located. Older log files automatically get
deleted.
