package de.thkoeln.fentwums.netlist.backend.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class NetlistDifferentiator {
    private static final Logger logger = LoggerFactory.getLogger(NetlistDifferentiator.class);

    public static NetlistType differentiate(InputStream netlistStream) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};

            int hierarchicalModuleCount = 0;
            HashMap<String, Object> netlist, modules, currentModule, currentModuleAttributes;
            netlist = mapper.readValue(netlistStream, typeRef);
            modules = (HashMap<String, Object>) netlist.get("modules");

            for (String key : modules.keySet()) {
                currentModule = (HashMap<String, Object>) modules.get(key);
                currentModuleAttributes = (HashMap<String, Object>) currentModule.get("attributes");

                if (currentModuleAttributes.containsKey("top")) {
                    continue;
                } else if (currentModuleAttributes.containsKey("blackbox")) {
                    continue;
                }

                hierarchicalModuleCount++;

                if (hierarchicalModuleCount > 1) {
                    return NetlistType.HIERARCHICAL;
                }
            }
        } catch (IOException e) {
            logger.error("Could not parse netlist", e);
        }

        return NetlistType.FLATTENED_WITH_SEPERATOR;
    }
}
