# Important

This is a beta release. Bugs are to be expected. If you notice any issues, please submit bug reports to help us improve
the software.

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

# Prerequisites

- Minimum Java Version: 21
- Maven

# Build

By running

```
mvn clean package spring-boot:repackage
```

in the root directory, an über-JAR containing all dependencies can be created. The resulting jar under
`fentwums-netlist-reader-server/target` can then be executed using

```
java -jar fentwums-netlist-reader-server-1.0-SNAPSHOT-fentwums-netlist-reader-service.jar
```

# Notes

## System requirements

Since the layouting process is mostly single-threaded by nature, a modern machine with high single-core performance is
recommended for running the server. The CPU does not need to have a lot of cores, it should just be fast.

Depending on the size of the netlists that are to be layouted, the backend can use quite a lot of system memory to store
all the necessary information. Therefore, it is important to provide an appropriate amount of RAM to the server. The
software itself is currently configured to use up to 1.000 GB of system memory, but this limit can be raised to 4 TB if
desired.