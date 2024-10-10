package de.thkoeln.fentwums.netlist.backend.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.jshell.spi.ExecutionControl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class NetlistParser {
    private File netlistFile;
    private ObjectMapper mapper;
    private TypeReference<HashMap<String, Object>> typeRef;
    private HashMap<String, Object> readNetlist;
    private HashMap<String, Object> readModules;
    private HashMap<String, Object> moduleToParse;
    private String toplevelName;

    public NetlistParser() {
        netlistFile = null;
        mapper = new ObjectMapper();
        typeRef = new TypeReference<HashMap<String, Object>>() {};
        readNetlist = null;
        readModules = null;
        moduleToParse = null;
        toplevelName = null;
    }

    public NetlistParser(File netlistFile) {
        this.netlistFile = netlistFile;
        mapper = new ObjectMapper();
        typeRef = new TypeReference<HashMap<String, Object>>() {};
        readNetlist = null;
        readModules = null;
        moduleToParse = null;
        toplevelName = null;
    }

    public NetlistParser(String netlistFilePath) {
        netlistFile = new File(netlistFilePath);
        mapper = new ObjectMapper();
        typeRef = new TypeReference<HashMap<String, Object>>() {};
        readNetlist = null;
        readModules = null;
        moduleToParse = null;
        toplevelName = null;
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

    public HashMap<String, Object> getReadModules() {
        return readModules;
    }

    public void setReadModules(HashMap<String, Object> readModules) {
        this.readModules = readModules;
    }

    public HashMap<String, Object> getModuleToParse() {
        return moduleToParse;
    }

    public void setModuleToParse(HashMap<String, Object> moduleToParse) {
        this.moduleToParse = moduleToParse;
    }

    public String getToplevelName() {
        return toplevelName;
    }

    public void setToplevelName(String toplevelName) {
        this.toplevelName = toplevelName;
    }

    public void readNetlist() throws IOException {
        if (netlistFile == null) {
            throw new RuntimeException("netlistFile is null");
        }

        try {
            readNetlist = mapper.readValue(netlistFile, typeRef);
        } catch (IOException e) {
            /*
             * Needs handling in the future
             */

            throw e;
        }
    }

    public void checkReadNetlist() throws ExecutionControl.NotImplementedException {
        if (readNetlist == null) {
            throw new RuntimeException("readNetlist is null");
        }

        if (!readNetlist.containsKey("modules")) {
            // The parsed file does not contain a modules section
            // and is thus not a valid netlist
            throw new RuntimeException("modules is null");
        }

        readModules = (HashMap<String, Object>) readNetlist.get(readNetlist.get("modules"));

        // Check the number of modules
        if (readModules.keySet().size() > 1) {
            throw new ExecutionControl.NotImplementedException("Checking for not instantiated blackbox modules has " +
                    "not been implemented yet");
        }

        // For now, we can assume that the first (and only) element in readModules is the module that should be
        // displayed
        toplevelName = readModules.keySet().iterator().next();
        moduleToParse = (HashMap<String, Object>) readModules.get(toplevelName);

    }

}
