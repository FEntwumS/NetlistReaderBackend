package de.thkoeln.fentwums.netlist.backend.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.jshell.spi.ExecutionControl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class NetlistParser {
    private File netlistFile;
    private InputStream netlistStream;
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
        netlistStream = null;
    }

    public NetlistParser(File netlistFile) {
        this.netlistFile = netlistFile;
        mapper = new ObjectMapper();
        typeRef = new TypeReference<HashMap<String, Object>>() {};
        readNetlist = null;
        readModules = null;
        moduleToParse = null;
        toplevelName = null;
        netlistStream = null;
    }

    public NetlistParser(String netlistFilePath) {
        netlistFile = new File(getClass().getResource(netlistFilePath).getFile());
        mapper = new ObjectMapper();
        typeRef = new TypeReference<HashMap<String, Object>>() {};
        readNetlist = null;
        readModules = null;
        moduleToParse = null;
        toplevelName = null;
        netlistStream = null;
    }

    public NetlistParser(InputStream netlistStream) {
        netlistFile = null;
        mapper = new ObjectMapper();
        typeRef = new TypeReference<HashMap<String, Object>>() {};
        readNetlist = null;
        readModules = null;
        moduleToParse = null;
        toplevelName = null;
        this.netlistStream = netlistStream;
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

    public InputStream getNetlistStream() {
        return netlistStream;
    }

    public void setNetlistStream(InputStream netlistStream) {
        this.netlistStream = netlistStream;
    }

    public void readNetlistFile() throws IOException {
        if (netlistFile == null) {
            throw new RuntimeException("netlistFile is null");
        }

        try {
            readNetlist = mapper.readValue(netlistFile, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing netlist", e);
        }
    }

    public void readNetlistStream() throws IOException {
        if (netlistStream == null) {
            throw new RuntimeException("netlistStream is null");
        }

        try {
            readNetlist = mapper.readValue(netlistStream, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing netlist", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void checkReadNetlist() throws ExecutionControl.NotImplementedException {
        if (readNetlist == null) {
            throw new RuntimeException("readNetlist is null");
        }

        if (!readNetlist.containsKey("modules")) {
            // The parsed file does not contain a modules section
            // and is thus not a valid netlist
            throw new RuntimeException("modules is null");
        }

        readModules = (HashMap<String, Object>) readNetlist.get("modules");

        // Check the number of modules and find the toplevel, if necessary
        if (readModules.keySet().size() > 1) {
            moduleToParse = (HashMap<String, Object>) findTopLevel(readModules);
            toplevelName = (String) moduleToParse.get("name");
        } else {
            toplevelName = readModules.keySet().iterator().next();
            moduleToParse = (HashMap<String, Object>) readModules.get(toplevelName);
        }

    }

    // TODO rework this naive implementation
    @SuppressWarnings("unchecked")
    private HashMap<String, Object> findTopLevel(HashMap<String, Object> modules) {
        HashMap<String, Object> currentModule;
        HashMap<String, Object> currentModuleAttrs;
        for (String modulename: modules.keySet()) {
            currentModule = (HashMap<String, Object>) modules.get(modulename);

            currentModuleAttrs = (HashMap<String, Object>) currentModule.get("attributes");

            if (currentModuleAttrs == null) {
                throw new RuntimeException("module " + modulename + " is null");
            }

            if (currentModuleAttrs.containsKey("top")) {
                currentModule.put("name", modulename);
                return currentModule;
            }
        }

        throw new RuntimeException("no toplevel module found");
    }

}
