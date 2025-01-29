package de.thkoeln.fentwums.netlist.backend.netlistreaderbackendspringboot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.thkoeln.fentwums.netlist.backend.helpers.CellCollapser;
import de.thkoeln.fentwums.netlist.backend.helpers.SignalBundler;
import de.thkoeln.fentwums.netlist.backend.netlistreaderbackendspringboot.types.NetlistInformation;
import de.thkoeln.fentwums.netlist.backend.options.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.parser.GraphCreator;
import de.thkoeln.fentwums.netlist.backend.parser.NetlistParser;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.graph.json.ElkGraphJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SpringBootApplication
@RestController
public class NetlistReaderBackendSpringBootApplication {
	private static Logger logger = LoggerFactory.getLogger(NetlistReaderBackendSpringBootApplication.class);
	private static HashMap<Long, NetlistInformation> currentNets = new HashMap<Long, NetlistInformation>();
	private static ReentrantReadWriteLock mapLock = new ReentrantReadWriteLock(true);

	@Autowired
	private ApplicationContext context;

	@Value("${JAVA_HOME}")
	private static String javaHome;

	static HashMap<String, Object> blackboxmap = new HashMap<>();

	public static void main(String[] args) {
		SpringApplication.run(NetlistReaderBackendSpringBootApplication.class, args);

		// Register custom ELK options
		LayoutMetaDataService service = LayoutMetaDataService.getInstance();
		service.registerLayoutMetaDataProviders(new FEntwumSOptions());
		service.registerLayoutMetaDataProviders(new LayeredOptions());  // https://github.com/eclipse/elk/issues/654#issuecomment-656184498

		logger.info("Successfully registered options");

		logger.info("Start reading blackbox description files");

		try {
			ObjectMapper mapper = new ObjectMapper();
			final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
			};

			ApplicationHome home = new ApplicationHome(NetlistReaderBackendSpringBootApplication.class);

			File dir = new File(home.getDir().getAbsolutePath() + "/blackbox-descriptions");
			File[] files = dir.listFiles();

			if (files != null) {

				logger.info("Start reading bundled blackbox description files");

				for (File file : files) {
					logger.atInfo().setMessage("JAVA_HOME: {}").addArgument(javaHome).log();
					logger.atInfo().log("Reading blackbox description file: " + file.getAbsolutePath());
					HashMap<String, Object> map = mapper.readValue(file, typeRef);
					ArrayList<String> keyRemovalList = new ArrayList<>();

					for (String key : map.keySet()) {
						if (blackboxmap.containsKey(key)) {
							keyRemovalList.add(key);

							logger.atWarn().setMessage("Blackbox description file {} contains the previously defined cell" +
									" " +
									"{}").addArgument(file.getName()).addArgument(key).log();
							logger.atWarn().setMessage("Full path of blackbox description file containing the conflicting " +
									"definition: {}").addArgument(file.getAbsolutePath()).log();
						}
					}

					for (String key : keyRemovalList) {
						map.remove(key);
					}

					blackboxmap.putAll(map);
				}
			}
		} catch (Exception e) {
			logger.error("Error reading blackboxes", e);
		}
	}

	@RequestMapping(value = "/graphLocalFile", method = RequestMethod.POST)
	public ResponseEntity<String> createNetlistGraphFromLocalFile(@RequestParam(value = "filename") String filename,
																  @RequestParam(value = "hash") String hash) {
		GraphCreator creator = new GraphCreator();
		NetlistParser parser = new NetlistParser();

		logger.info(filename);
		logger.info(hash);

		parser.setNetlistFile(new File(filename));
		// TODO remove
		parser.setNetlistStream(null);

		return graphNetlist(creator, parser, Long.parseUnsignedLong(hash));
	}

	@RequestMapping(value = "/graphRemoteFile", method = RequestMethod.POST)
	public ResponseEntity<String> createNetlistGraphFromRemoteFile(@RequestParam("file") MultipartFile file,
																   @RequestParam(value = "hash") String hash) {
		GraphCreator creator = new GraphCreator();
		NetlistParser parser = new NetlistParser();

		logger.info("Max heap size: " + Runtime.getRuntime().maxMemory());
		logger.info(hash);

		try {
			parser.setNetlistStream(file.getInputStream());
			// TODO remove
			parser.setNetlistFile(null);
		} catch (Exception e) {
			logger.error("Error reading netlist file", e);

			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return graphNetlist(creator, parser, Long.parseUnsignedLong(hash));
	}

	private ResponseEntity<String> graphNetlist(GraphCreator creator, NetlistParser parser, long hash) {
		CellCollapser collapser = new CellCollapser();
		SignalBundler bundler = new SignalBundler();

		try {
			logger.info("Start reading netlist file");

			// TODO remove assumptions
			if (parser.getNetlistStream() != null) {
				parser.readNetlistStream();
			} else {
				parser.readNetlistFile();
			}

			logger.info("Netlist file read successfully");
			logger.info("Start checking read netlist");
			parser.checkReadNetlist();
			logger.info("Netlist file checked successfully");
		} catch (Exception e) {
			logger.error("Error reading netlist", e);

			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		logger.info("Start creating graph");
		creator.createGraphFromNetlist(parser.getModuleToParse(), parser.getToplevelName(), blackboxmap);
		logger.info("Graph created successfully");

		collapser.setHierarchy(creator.getHierarchyTree());

		bundler.setHierarchy(creator.getHierarchyTree());
		bundler.setTreeMap(creator.getSignalTreeMap());

		for (String child : creator.getHierarchyTree().getRoot().getChildren().keySet()) {
			collapser.collapseRecursively(creator.getHierarchyTree().getRoot().getChildren().get(child));
		}

		logger.info("Start layouting");
		String layoutedGraph = creator.layoutGraph();
		logger.info("Graph layouted successfully");

		logger.info("done");

		NetlistInformation newNetlist = new NetlistInformation(creator, bundler, collapser);

		mapLock.writeLock().lock();
		try {
			currentNets.put(hash, newNetlist);
		} finally {
			mapLock.writeLock().unlock();
		}

		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, "application/json");

		logger.info("Used memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

		return new ResponseEntity<>(layoutedGraph, headers, HttpStatus.OK);
	}

	@RequestMapping(value = "/expandNode", method = RequestMethod.POST)
	public ResponseEntity<String> expandNode(@RequestParam(value = "hash") String hash, @RequestParam(value =
			"nodePath") String nodePath) {
		NetlistInformation currentNetlist;
		String expandedGraph = "";

		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, "application/json");

		mapLock.readLock().lock();
		try {
			if (currentNets.containsKey(Long.parseUnsignedLong(hash))) {
				currentNets.get(Long.parseUnsignedLong(hash)).lock.lock();

				try {
					currentNetlist = currentNets.get(Long.parseUnsignedLong(hash));

					currentNetlist.getCollapser().toggleCollapsed(nodePath);

					expandedGraph = currentNetlist.getCreator().layoutGraph();
				} catch (Exception e) {
					logger.error("Error expanding cell", e);

					return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
				} finally {
					currentNets.get(Long.parseUnsignedLong(hash)).lock.unlock();
				}
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} finally {
			mapLock.readLock().unlock();
		}

		if (expandedGraph.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		} else {
			return new ResponseEntity<>(expandedGraph, headers, HttpStatus.OK);
		}
	}

	@RequestMapping(value = "/set-signal-value", method = RequestMethod.POST)
	public ResponseEntity<String> setSignalValue(@RequestParam(value = "hash") String hash, @RequestParam(value = "sid"
	) int sid, @RequestParam(value = "newValue") char newVal) {
		NetlistInformation currentNetlist;

		mapLock.readLock().lock();
		try {
			if (currentNets.containsKey(Long.parseUnsignedLong(hash))) {
				currentNets.get(Long.parseUnsignedLong(hash)).lock.lock();

				try {
					currentNetlist = currentNets.get(Long.parseUnsignedLong(hash));

					currentNetlist.getBundler().getTreeMap().get(sid).setSValue(newVal);
				} catch (Exception e) {
					logger.error("Setting signal value", e);

					return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
				} finally {
					currentNets.get(Long.parseUnsignedLong(hash)).lock.unlock();
				}
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} finally {
			mapLock.readLock().unlock();
		}

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	@RequestMapping(value = "/get-current-graph", method = RequestMethod.POST)
	public ResponseEntity<String> getCurrentGraph(@RequestParam(value = "hash") String hash) {
		NetlistInformation currentNetlist;
		String expandedGraph = "";

		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, "application/json");

		mapLock.readLock().lock();
		try {
			if (currentNets.containsKey(Long.parseUnsignedLong(hash))) {
				currentNets.get(Long.parseUnsignedLong(hash)).lock.lock();

				try {
					currentNetlist = currentNets.get(Long.parseUnsignedLong(hash));

					expandedGraph =
							ElkGraphJson.forGraph(currentNetlist.getCreator().getGraph()).omitLayout(false).omitZeroDimension(true)
									.omitZeroPositions(true).shortLayoutOptionKeys(true).prettyPrint(false).toJson();
				} catch (Exception e) {
					logger.error("Error expanding cell", e);

					return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
				} finally {
					currentNets.get(Long.parseUnsignedLong(hash)).lock.unlock();
				}
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} finally {
			mapLock.readLock().unlock();
		}

		if (expandedGraph.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		} else {
			return new ResponseEntity<>(expandedGraph, headers, HttpStatus.OK);
		}
	}

	@RequestMapping(value = "/get-net-information", method = RequestMethod.POST)
	public ResponseEntity<String> getNetInformation(@RequestParam(value = "hash") String hash) {
		NetlistInformation currentNetlist;
		String serializedNetInformation = "";

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
		ObjectWriter writer = mapper.writer().withRootName("signals");

		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.CONTENT_TYPE, "application/json");

		mapLock.readLock().lock();
		try {
			if (currentNets.containsKey(Long.parseUnsignedLong(hash))) {
				currentNets.get(Long.parseUnsignedLong(hash)).lock.lock();

				try {
					currentNetlist = currentNets.get(Long.parseUnsignedLong(hash));

					serializedNetInformation =
							writer.writeValueAsString(currentNetlist.getCreator().getNetInformationMap());
				} catch (Exception e) {
					logger.error("Error expanding cell", e);

					return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
				} finally {
					currentNets.get(Long.parseUnsignedLong(hash)).lock.unlock();
				}
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} finally {
			mapLock.readLock().unlock();
		}

		if (serializedNetInformation.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		} else {
			return new ResponseEntity<>(serializedNetInformation, headers, HttpStatus.OK);
		}
	}

	// Inspired by: https://stackoverflow.com/a/55196987
	@GetMapping("/shutdown-backend")
	public void shutdownBackend() {
		int exitCode = SpringApplication.exit(context, (ExitCodeGenerator) () -> 0);
		System.exit(exitCode);
	}

	@GetMapping("/server-active")
	public ResponseEntity<String> serverActive() {
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequestMapping(value = "/close-netlist", method = RequestMethod.POST)
	public ResponseEntity<String> closeNetlist(@RequestParam(value = "hash") String hash) {

		mapLock.writeLock().lock();
		try {
			if (currentNets.containsKey(Long.parseUnsignedLong(hash))) {
				try {
					currentNets.remove(Long.parseUnsignedLong(hash));
				} catch (Exception e) {
					logger.error("Error closing netlist", e);

					return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
				}
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} finally {
			mapLock.writeLock().unlock();
		}

		System.gc();
		System.gc();

		logger.info("Used memory after GC: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
