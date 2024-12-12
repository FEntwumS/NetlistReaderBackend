package de.thkoeln.fentwums.netlist.backend.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.jshell.spi.ExecutionControl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Handles the parsing of the netlist. The actual analysis and graph creation is the responsibility of helper classes.
 */
public class NetlistParser {
	private File netlistFile;
	private InputStream netlistStream;
	private ObjectMapper mapper;
	private final TypeReference<HashMap<String, Object>> typeRef;
	private HashMap<String, Object> readNetlist;
	private HashMap<String, Object> readModules;
	private HashMap<String, Object> moduleToParse;
	private String toplevelName;

	public NetlistParser() {
		netlistFile = null;
		mapper = new ObjectMapper();
		typeRef = new TypeReference<HashMap<String, Object>>() {
		};
		readNetlist = null;
		readModules = null;
		moduleToParse = null;
		toplevelName = null;
		netlistStream = null;
	}

	public NetlistParser(File netlistFile) {
		this.netlistFile = netlistFile;
		mapper = new ObjectMapper();
		typeRef = new TypeReference<HashMap<String, Object>>() {
		};
		readNetlist = null;
		readModules = null;
		moduleToParse = null;
		toplevelName = null;
		netlistStream = null;
	}

	public NetlistParser(String netlistFilePath) {
		netlistFile = new File(getClass().getResource(netlistFilePath).getFile());
		mapper = new ObjectMapper();
		typeRef = new TypeReference<HashMap<String, Object>>() {
		};
		readNetlist = null;
		readModules = null;
		moduleToParse = null;
		toplevelName = null;
		netlistStream = null;
	}

	public NetlistParser(InputStream netlistStream) {
		netlistFile = null;
		mapper = new ObjectMapper();
		typeRef = new TypeReference<HashMap<String, Object>>() {
		};
		readNetlist = null;
		readModules = null;
		moduleToParse = null;
		toplevelName = null;
		this.netlistStream = netlistStream;
	}

	/**
	 * Gets the netlist file
	 *
	 * @return the netlist file
	 */
	public File getNetlistFile() {
		return netlistFile;
	}

	/**
	 * Sets the netlist file
	 *
	 * @param netlistFile the netlist file
	 */
	public void setNetlistFile(File netlistFile) {
		this.netlistFile = netlistFile;
	}

	/**
	 * Gets the complete read netlist
	 *
	 * @return the complete netlist
	 */
	public HashMap<String, Object> getReadNetlist() {
		return readNetlist;
	}

	/**
	 * Sets the complete read netlist
	 *
	 * @param readNetlist the complete netlist
	 */
	public void setReadNetlist(HashMap<String, Object> readNetlist) {
		this.readNetlist = readNetlist;
	}

	/**
	 * Get the read modules
	 *
	 * @return the read modules
	 */
	public HashMap<String, Object> getReadModules() {
		return readModules;
	}

	/**
	 * Sets the read modules
	 *
	 * @param readModules the read modules
	 */
	public void setReadModules(HashMap<String, Object> readModules) {
		this.readModules = readModules;
	}

	/**
	 * Gets the module containing the top level entity
	 *
	 * @return The module containing the top level entity
	 */
	public HashMap<String, Object> getModuleToParse() {
		return moduleToParse;
	}

	/**
	 * Sets the module containing the top level entity
	 *
	 * @param moduleToParse The module containing the top level entity
	 */
	public void setModuleToParse(HashMap<String, Object> moduleToParse) {
		this.moduleToParse = moduleToParse;
	}

	/**
	 * Gets the name of the top level entity
	 *
	 * @return The name
	 */
	public String getToplevelName() {
		return toplevelName;
	}

	/**
	 * Sets the name of the top level entity
	 *
	 * @param toplevelName The name
	 */
	public void setToplevelName(String toplevelName) {
		this.toplevelName = toplevelName;
	}

	/**
	 * Gets the stream containg the netlist file
	 *
	 * @return The stream
	 */
	public InputStream getNetlistStream() {
		return netlistStream;
	}

	/**
	 * Sets the stream containing the netlist file
	 *
	 * @param netlistStream The stream
	 */
	public void setNetlistStream(InputStream netlistStream) {
		this.netlistStream = netlistStream;
	}

	/**
	 * Reads the previously stored netlist file
	 *
	 * @throws IOException
	 */
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

	/**
	 * Reads the netlist file from a previously stored stream
	 *
	 * @throws IOException
	 */
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

	/**
	 * Perfoms basic checks on the read netlist. Checks for a top level entity. If one is found, it is then saved to
	 * <code>moduleToParse</code>. The name of the top level entity is saved to <code>toplevelName</code>
	 *
	 * @throws RuntimeException when the netlist contains no module
	 */
	@SuppressWarnings("unchecked")
	public void checkReadNetlist() throws RuntimeException {
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

	/**
	 * Finds the module containing the toplevel attribute from several modules
	 *
	 * @param modules The deserialized modules found in the netlist
	 * @return The deserialized module containing the toplevel attribute
	 * @throws RuntimeException when no module containing the topllevel attribute can be found
	 */
	@SuppressWarnings("unchecked")
	private HashMap<String, Object> findTopLevel(HashMap<String, Object> modules) throws RuntimeException {
		HashMap<String, Object> currentModule;
		HashMap<String, Object> currentModuleAttrs;
		for (String modulename : modules.keySet()) {
			currentModule = (HashMap<String, Object>) modules.get(modulename);

			currentModuleAttrs = (HashMap<String, Object>) currentModule.get("attributes");

			if (currentModuleAttrs == null) {
				continue;
			}

			if (currentModuleAttrs.containsKey("top")) {
				currentModule.put("name", modulename);
				return currentModule;
			}
		}

		throw new RuntimeException("no toplevel module found");
	}
}