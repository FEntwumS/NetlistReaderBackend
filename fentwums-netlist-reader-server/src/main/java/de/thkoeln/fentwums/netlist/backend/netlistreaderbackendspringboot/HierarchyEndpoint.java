package de.thkoeln.fentwums.netlist.backend.netlistreaderbackendspringboot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.thkoeln.fentwums.netlist.backend.helpers.CellCollapser;
import de.thkoeln.fentwums.netlist.backend.hierarchy.view.HierarchyExtractor;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;

@RestController
public class HierarchyEndpoint {
	private final static Logger logger = LoggerFactory.getLogger(HierarchyEndpoint.class);

	@PostConstruct
	public void init() {
		logger.info("Initializing HierarchyEndpoint");
	}

	@RequestMapping(value = "/extractHierarchy", method = RequestMethod.POST)
	public ResponseEntity<String> extractHierarchyFromNetlist(@RequestParam("file") MultipartFile file,
															  @RequestParam(value = "hash") String hash) {
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
		};

		HashMap<String, Object> netlist;

		try {
			netlist = mapper.readValue(file.getInputStream(), typeRef);
		} catch (IOException e) {
			logger.error("Could not parse netlist", e);

			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		HierarchyExtractor extractor = new HierarchyExtractor();

		String layoutedGraph = extractor.extractHierarchy(netlist);

		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, "application/json");

		return new ResponseEntity<>(layoutedGraph, headers, HttpStatus.OK);
	}
}
