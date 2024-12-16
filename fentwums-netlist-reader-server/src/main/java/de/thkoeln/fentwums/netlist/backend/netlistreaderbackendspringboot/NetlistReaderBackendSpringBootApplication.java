package de.thkoeln.fentwums.netlist.backend.netlistreaderbackendspringboot;

import de.thkoeln.fentwums.netlist.backend.helpers.CellCollapser;
import de.thkoeln.fentwums.netlist.backend.helpers.ElkElementCreator;
import de.thkoeln.fentwums.netlist.backend.helpers.SignalBundler;
import de.thkoeln.fentwums.netlist.backend.netlistreaderbackendspringboot.types.NetlistInformation;
import de.thkoeln.fentwums.netlist.backend.options.FEntwumSOptions;
import de.thkoeln.fentwums.netlist.backend.parser.GraphCreator;
import de.thkoeln.fentwums.netlist.backend.parser.NetlistParser;
import org.eclipse.elk.graph.json.ElkGraphJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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

	public static void main(String[] args) {
		SpringApplication.run(NetlistReaderBackendSpringBootApplication.class, args);
	}

	@GetMapping("/hello")
	public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
		return String.format("Hello %s!", name);
	}

	@RequestMapping(value = "/graphLocalFile", method = RequestMethod.POST)
	public ResponseEntity<String> createNetlistGraphFromLocalFile(@RequestParam(value = "filename", defaultValue = "C" +
			":\\Users\\Florian\\Documents\\Semester\\7\\Praxisphase\\NetlistReaderBackend\\fentwums-netlist-reader" +
			"-service\\src\\main\\resources\\optimal-info2.json") String filename,
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

	public ResponseEntity<String> graphNetlist(GraphCreator creator, NetlistParser parser, long hash) {
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
		creator.createGraphFromNetlist(parser.getModuleToParse(), parser.getToplevelName());
		logger.info("Graph created successfully");

		collapser.setHierarchy(creator.getHierarchyTree());

		bundler.setHierarchy(creator.getHierarchyTree());
		bundler.setTreeMap(creator.getSignalTreeMap());

//		collapser.collapseRecursively(creator.getHierarchyTree().getRoot());

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


	// Inspired by: https://stackoverflow.com/a/55196987
	@GetMapping("/shutdown-backend")
	public void shutdownBackend() {
		int exitCode = SpringApplication.exit(context, (ExitCodeGenerator) () -> 0);
		System.exit(exitCode);
	}
}
