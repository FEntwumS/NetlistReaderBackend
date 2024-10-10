package de.thkoeln.fentwums.netlist.backend.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;

public class netlistParser {
    private File netlistFile;
    private ObjectMapper mapper;
    private TypeReference<HashMap<String, Object>> typeRef;
    private HashMap<String, Object> readNetlist;

    public netlistParser() {
        netlistFile = null;
        mapper = new ObjectMapper();
        typeRef = new TypeReference<HashMap<String, Object>>() {};
        readNetlist = null;
    }

    public netlistParser(File netlistFile) {
        this.netlistFile = netlistFile;
        mapper = new ObjectMapper();
        typeRef = new TypeReference<HashMap<String, Object>>() {};
        readNetlist = null;
    }

    public netlistParser(String netlistFilePath) {
        netlistFile = new File(netlistFilePath);
        mapper = new ObjectMapper();
        typeRef = new TypeReference<HashMap<String, Object>>() {};
        readNetlist = null;
    }

    public File getNetlistFile() {
        return netlistFile;
    }

    public void setNetlistFile(File netlistFile) {
        this.netlistFile = netlistFile;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public HashMap<String, Object> getReadNetlist() {
        return readNetlist;
    }

    public void setReadNetlist(HashMap<String, Object> readNetlist) {
        this.readNetlist = readNetlist;
    }



}
