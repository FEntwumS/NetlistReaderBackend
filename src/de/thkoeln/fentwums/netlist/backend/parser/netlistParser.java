package de.thkoeln.fentwums.netlist.backend.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;

public class netlistParser {
    private File netlistFile;
    private ObjectMapper mapper;
    private TypeReference<HashMap<String, Object>> typeRef;

    public netlistParser() {
        netlistFile = null;
        mapper = new ObjectMapper();
        typeRef = new TypeReference<HashMap<String, Object>>() {};
    }

    public netlistParser(File netlistFile) {
        this.netlistFile = netlistFile;
        mapper = new ObjectMapper();
        typeRef = new TypeReference<HashMap<String, Object>>() {};
    }

    public netlistParser(String netlistFilePath) {
        netlistFile = new File(netlistFilePath);
        mapper = new ObjectMapper();
        typeRef = new TypeReference<HashMap<String, Object>>() {};
    }

}
