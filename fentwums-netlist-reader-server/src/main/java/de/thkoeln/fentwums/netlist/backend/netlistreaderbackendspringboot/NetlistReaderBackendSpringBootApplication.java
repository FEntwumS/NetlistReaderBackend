package de.thkoeln.fentwums.netlist.backend.netlistreaderbackendspringboot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.thkoeln.fentwums.netlist.backend.datatypes.ModuleNode;
import de.thkoeln.fentwums.netlist.backend.datatypes.NetlistCreationSettings;
import de.thkoeln.fentwums.netlist.backend.datatypes.PerformanceTarget;
import de.thkoeln.fentwums.netlist.backend.helpers.CellCollapser;
import de.thkoeln.fentwums.netlist.backend.helpers.NetlistDifferentiator;
import de.thkoeln.fentwums.netlist.backend.helpers.SignalBundler;
import de.thkoeln.fentwums.netlist.backend.hierarchical.parser.HierarchicalOrchestrator;
import de.thkoeln.fentwums.netlist.backend.netlistreaderbackendspringboot.types.NetlistInformation;
import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@SpringBootApplication
@RestController
public class NetlistReaderBackendSpringBootApplication {
    static HashMap<String, Object> blackboxmap = new HashMap<>();
    private static Logger logger = LoggerFactory.getLogger(NetlistReaderBackendSpringBootApplication.class);
    private static HashMap<Long, NetlistInformation> currentNets = new HashMap<Long, NetlistInformation>();
    private static ReentrantReadWriteLock mapLock = new ReentrantReadWriteLock(true);
    @Autowired
    private ApplicationContext context;

    public static void main(String[] args) {
        SpringApplication.run(NetlistReaderBackendSpringBootApplication.class, args);

        // Register custom ELK options
        LayoutMetaDataService service = LayoutMetaDataService.getInstance();
        service.registerLayoutMetaDataProviders(new FEntwumSOptions());
        service.registerLayoutMetaDataProviders(
                new LayeredOptions());  // https://github.com/eclipse/elk/issues/654#issuecomment-656184498

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
                    logger.atInfo().log("Reading blackbox description file: " + file.getAbsolutePath());
                    HashMap<String, Object> map = mapper.readValue(file, typeRef);
                    ArrayList<String> keyRemovalList = new ArrayList<>();

                    for (String key : map.keySet()) {
                        if (blackboxmap.containsKey(key)) {
                            keyRemovalList.add(key);

                            logger.atWarn().setMessage(
                                    "Blackbox description file {} contains the previously defined" + " " + "cell" +
                                            " " + "{}").addArgument(file.getName()).addArgument(key).log();
                            logger.atWarn().setMessage(
                                    "Full path of blackbox description file containing the " + "conflicting " +
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

    @RequestMapping(value = "/graphRemoteFile", method = RequestMethod.POST)
    public ResponseEntity<String> createNetlistGraphFromRemoteFile(@RequestParam("file") MultipartFile file,
                                                                   @RequestParam(value = "hash") String hash,
                                                                   @RequestParam(value = "entityLabelFontSize",
                                                                           defaultValue = "25") int entityLabelFontSize,
                                                                   @RequestParam(value = "cellLabelFontSize",
                                                                           defaultValue = "15") int cellLabelFontSize,
                                                                   @RequestParam(value = "edgeLabelFontSize",
                                                                           defaultValue = "10") int edgeLabelFontSize,
                                                                   @RequestParam(value = "portLabelFontSize",
                                                                           defaultValue = "10") int portLabelFontSize,
                                                                   @RequestParam(value = "performance-target",
                                                                           defaultValue = "UNKNOWN")
                                                                   String performanceTarget,
                                                                   @RequestParam(value = "test-mode", defaultValue =
                                                                           "0") String testMode) {
        System.gc();
        System.gc();

        logger.info("Used memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

        long startTime = System.nanoTime();

        GraphCreator creator = new GraphCreator();
        NetlistParser parser = new NetlistParser();
        PerformanceTarget target;

        logger.atInfo().setMessage("Selected performance target: {}").addArgument(performanceTarget).log();

        try {
            target = PerformanceTarget.valueOf(performanceTarget);
        } catch (IllegalArgumentException e) {
            target = PerformanceTarget.Preloading;
        }

        NetlistCreationSettings settings = new NetlistCreationSettings(entityLabelFontSize, cellLabelFontSize,
                                                                       edgeLabelFontSize, portLabelFontSize,
                                                                       target);
        try {
            switch (NetlistDifferentiator.differentiate(file.getInputStream())) {
                case HIERARCHICAL -> {
                    HierarchicalOrchestrator orchestrator = new HierarchicalOrchestrator();

                    return graphHierarchicalNetlist(file, orchestrator, settings, Long.parseUnsignedLong(hash),
                                                    startTime, testMode);
                }
                case FLATTENED_WITH_SEPERATOR -> {
                    try {
                        parser.setNetlistStream(file.getInputStream());
                        // TODO remove
                        parser.setNetlistFile(null);
                    } catch (Exception e) {
                        logger.error("Error reading netlist file", e);

                        return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                    return graphFlattenedNetlist(creator, parser, Long.parseUnsignedLong(hash), settings, startTime,
                                                 testMode);
                }
                case UNKNOWN -> {
                    if (target == PerformanceTarget.UNKNOWN) {
                        try {
                            parser.setNetlistStream(file.getInputStream());
                            // TODO remove
                            parser.setNetlistFile(null);
                        } catch (Exception e) {
                            logger.error("Error reading netlist file", e);

                            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                        }

                        return graphFlattenedNetlist(creator, parser, Long.parseUnsignedLong(hash), settings, startTime,
                                                     testMode);
                    } else {
                        HierarchicalOrchestrator orchestrator = new HierarchicalOrchestrator();

                        return graphHierarchicalNetlist(file, orchestrator, settings, Long.parseUnsignedLong(hash),
                                                        startTime, testMode);
                    }
                }
                case INCOMPLETE -> {
                    return new ResponseEntity<>("Netlist is incomplete", HttpStatus.BAD_REQUEST);
                }
                default -> {
                    return new ResponseEntity<>("Netlist could not be differentiated", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        } catch (IOException e) {
            logger.error("Error receiving netlist file", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private ResponseEntity<String> graphHierarchicalNetlist(MultipartFile file, HierarchicalOrchestrator orchestrator,
                                                            NetlistCreationSettings settings, long hash,
                                                            long startTime, String testMode) {
        try {
            String layoutedGraph = "";
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
            };
            CellCollapser collapser = new CellCollapser();

            HashMap<String, Object> netlist = mapper.readValue(file.getInputStream(), typeRef);

            orchestrator.createGraphFromNetlist(netlist, "", blackboxmap, settings);

            collapser.setRootNode(orchestrator.getRoot());

            for (String key : orchestrator.getRoot().getChildren().keySet()) {
                collapser.collapseRecursively(orchestrator.getRoot().getChildren().get(key));
            }

            if (!testMode.equals("1")) {
                logger.info("Start layouting");
                layoutedGraph = orchestrator.layoutGraph();
                logger.info("Graph layouted successfully");
            }

            logger.info("done");

            if (settings.getPerformanceTarget() == PerformanceTarget.IntelligentAheadOfTime) {
                Thread t = new Thread(() -> {
                    orchestrator.loadModulesIntelligently();
                });

                t.start();
                logger.info("Started intelligent loading");

                if (testMode.equals("1")) {
                    try {
                        t.join();
                    } catch (InterruptedException e) {

                    }
                }
            }

            if (testMode.equals("1")) {
                long endTime = System.nanoTime();

                logger.info("Test mode is active. Returning performance results");
                long executionTime = endTime - startTime;
                return new ResponseEntity<>("Execution time: " + executionTime , HttpStatus.OK);
            }

            NetlistInformation newNetlist = new NetlistInformation(orchestrator, null, collapser);

            mapLock.writeLock().lock();
            try {
                currentNets.put(hash, newNetlist);
            } finally {
                mapLock.writeLock().unlock();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, "application/json");

            System.gc();
            System.gc();

            logger.info("Used memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

            return new ResponseEntity<>(layoutedGraph, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<String> graphFlattenedNetlist(GraphCreator creator, NetlistParser parser, long hash,
                                                         NetlistCreationSettings settings, long startTime,
                                                         String testMode) {
        CellCollapser collapser = new CellCollapser();
        SignalBundler bundler = new SignalBundler();

        logger.atInfo().setMessage("Font sizes: Entity = {}; Cell = {}; Edge = {}; Port = {}").addArgument(
                settings.getEntityLabelFontSize()).addArgument(settings.getCellLabelFontSize()).addArgument(
                settings.getEdgeLabelFontSize()).addArgument(settings.getPortLabelFontSize()).log();

        logger.atInfo().setMessage("Netlist hash: {}").addArgument(hash).log();

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
        creator.createGraphFromNetlist(parser.getModuleToParse(), parser.getToplevelName(), blackboxmap, settings);
        logger.info("Graph created successfully");

        collapser.setRootNode(creator.getHierarchyTree().getRoot());

        bundler.setHierarchy(creator.getHierarchyTree());
        bundler.setTreeMap(creator.getSignalTreeMap());

        for (String child : creator.getHierarchyTree().getRoot().getChildren().keySet()) {
            collapser.collapseRecursively(creator.getHierarchyTree().getRoot().getChildren().get(child));
        }

        long endTime = System.nanoTime();

        if (testMode.equals("1")) {
            logger.info("Test mode is active. Returning performance results");
            long executionTime = endTime - startTime;
            return new ResponseEntity<>("Execution time: " + executionTime, HttpStatus.OK);
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

        System.gc();
        System.gc();

        logger.info("Used memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

        return new ResponseEntity<>(layoutedGraph, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/expandNode", method = RequestMethod.POST)
    public ResponseEntity<String> expandNode(@RequestParam(value = "hash") String hash,
                                             @RequestParam(value = "nodePath") String nodePath) {
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

                    // load module first, if necessary

                    if (currentNetlist.getCreator() instanceof HierarchicalOrchestrator) {
                        HierarchicalOrchestrator orchestrator = (HierarchicalOrchestrator) currentNetlist.getCreator();

                        switch (orchestrator.getSettings().getPerformanceTarget()) {
                            case JustInTime -> {
                                ModuleNode node = (ModuleNode) currentNetlist.getCollapser().findNode(nodePath);

                                if (!node.isLoaded()) {
                                    orchestrator.loadModule(node, nodePath);
                                }
                            }
                            case IntelligentAheadOfTime -> {
                                // here, wait for lock to release

                                ((HierarchicalOrchestrator) currentNetlist.getCreator()).waitForLock();
                            }
                        }

                    }
                    currentNetlist.getCollapser().toggleCollapsed(nodePath);

                    expandedGraph = currentNetlist.getCreator().layoutGraph();

                    if (currentNetlist.getCreator() instanceof HierarchicalOrchestrator && ((HierarchicalOrchestrator) currentNetlist.getCreator()).getSettings().getPerformanceTarget() == PerformanceTarget.IntelligentAheadOfTime) {
                        Thread t = new Thread(() -> {
                            ((HierarchicalOrchestrator) currentNetlist.getCreator()).loadModulesIntelligently();
                        });

                        t.start();
                        logger.info("Started intelligent loading");
                    }
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

        logger.info("Sending expanded graph to client");

        if (expandedGraph.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            return new ResponseEntity<>(expandedGraph, headers, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "/set-signal-value", method = RequestMethod.POST)
    public ResponseEntity<String> setSignalValue(@RequestParam(value = "hash") String hash,
                                                 @RequestParam(value = "sid") int sid,
                                                 @RequestParam(value = "newValue") char newVal) {
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

                    expandedGraph = ElkGraphJson.forGraph(currentNetlist.getCreator().getGraphRoot()).omitLayout(false)
                            .omitZeroDimension(true).omitZeroPositions(true).shortLayoutOptionKeys(true).prettyPrint(
                                    false).toJson();
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

                    serializedNetInformation = writer.writeValueAsString(
                            currentNetlist.getCreator().getNetInformationMap());
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

        logger.info(
                "Used memory after GC: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
