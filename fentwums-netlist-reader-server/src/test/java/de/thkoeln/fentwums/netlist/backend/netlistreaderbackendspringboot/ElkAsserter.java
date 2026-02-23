package de.thkoeln.fentwums.netlist.backend.netlistreaderbackendspringboot;

import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.*;

public class ElkAsserter {
	private static final Set<String> checkedLayoutOptions = new HashSet<>(Arrays.asList(
			"canonical-index-in-port-group",
			"msb-first",
			"port.index",
			"port.side",
			"port-group-name",
			"celltype",
			"src-location",
			"signaltype",
			"signalname",
			"edge.thickness",
			"index-in-signal",
			"celltype",
			"cellname"
	));

	private static Logger logger = LoggerFactory.getLogger(ElkAsserter.class);

	public static void assertEquals(File expected, HashMap<String, Object> actual) {
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
		};

		HashMap<String, Object> expectedMap = mapper.readValue(expected, typeRef);

		assertEqualsNode(expectedMap, actual);
	}

	public static void assertEqualsNode(HashMap<String, Object> expected, HashMap<String, Object> actual) {
		HashMap<String, Object> expectedLayoutOptions = (HashMap<String, Object>) expected.get("layoutOptions");
		HashMap<String, Object> actualLayoutOptions = (HashMap<String, Object>) actual.get("layoutOptions");
		assertEqualsLayoutOptions(expectedLayoutOptions, actualLayoutOptions);

		assertEqualsLabel(expected, actual);
		assertEqualsPort(expected, actual);
		assertEqualsEdge(expected, actual);

		if (expected.containsKey("children")) {
			if (actual.containsKey("children")) {
				ArrayList<Object> expectedChildrenList = (ArrayList<Object>) expected.get("children");
				ArrayList<Object> actualChildrenList = (ArrayList<Object>) actual.get("children");

				List<String> expectedChildrenIdList =
						((ArrayList<Object>) expectedChildrenList.clone()).stream().map(c -> {
					return (String) ((HashMap<String, Object>) c).get("id");
				}).toList();

				List<String> actualChildrenIdList =
						((ArrayList<Object>) actualChildrenList.clone()).stream().map(c -> {
					return (String) ((HashMap<String, Object>) c).get("id");
				}).toList();

				if (!expectedChildrenIdList.containsAll(actualChildrenIdList)) {
					AssertionFailureBuilder.assertionFailure()
							.message("ACTUAL has children that EXPECTED has not")
							.actual(actual)
							.expected(expected)
							.buildAndThrow();
				}

				if (!actualChildrenIdList.containsAll(expectedChildrenIdList)) {
					AssertionFailureBuilder.assertionFailure()
							.message("EXPECTED has children that ACTUAL has not")
							.actual(actual)
							.expected(expected)
							.buildAndThrow();
				}

				for (String id : expectedChildrenIdList) {
					HashMap<String, Object> expectedChild =
							(HashMap<String, Object>) expectedChildrenList.stream().filter(c -> ((String) ((HashMap<String, Object>) c).get("id")).equals(id)).toList().getFirst();
					HashMap<String, Object> actualChild =
							(HashMap<String, Object>) actualChildrenList.stream().
									filter(c -> ((String) ((HashMap<String, Object>) c).get("id")).equals(id)).toList().getFirst();

					assertEqualsNode(expectedChild, actualChild);
				}

			} else {
				AssertionFailureBuilder.assertionFailure()
						.message("ACTUAL has no children, EXPECTED has")
						.actual(actual)
						.expected(expected)
						.buildAndThrow();
			}
		} else {
			if (actual.containsKey("children")) {
				AssertionFailureBuilder.assertionFailure().message("ACTUAL has children, EXPECTED has not")
						.actual(actual).expected(expected).buildAndThrow();
			}
		}
	}

	public static void assertEqualsPort(HashMap<String, Object> expected, HashMap<String, Object> actual) {
		if (expected.containsKey("ports")) {
			if (actual.containsKey("ports")) {
				ArrayList<Object> expectedPortList = (ArrayList<Object>) expected.get("ports");
				ArrayList<Object> actualPortList = (ArrayList<Object>) actual.get("ports");

				List<String> expectedPortIdList = ((ArrayList<Object>) expectedPortList.clone()).stream().map(l -> {
					return (String) ((HashMap<String, Object>) l).get("id");
				}).toList();

				List<String> actualPortIdList = ((ArrayList<Object>) actualPortList.clone()).stream().map(l -> {
					return (String) ((HashMap<String, Object>) l).get("id");
				}).toList();

				if (!expectedPortIdList.containsAll(actualPortIdList)) {
					AssertionFailureBuilder.assertionFailure()
							.message("ACTUAL has ports that EXPECTED has not")
							.actual(actual)
							.expected(expected)
							.buildAndThrow();
				}

				if (!actualPortIdList.containsAll(expectedPortIdList)) {
					AssertionFailureBuilder.assertionFailure()
							.message("EXPECTED has ports that ACTUAL has not")
							.actual(actual)
							.expected(expected)
							.buildAndThrow();
				}

				for (String id : expectedPortIdList) {
					HashMap<String, Object> expectedPort =
							(HashMap<String, Object>) expectedPortList.stream().filter(c -> ((String) ((HashMap<String, Object>) c).get("id")).equals(id)).toList().getFirst();
					HashMap<String, Object> actualPort =
							(HashMap<String, Object>) actualPortList.stream().
									filter(c -> ((String) ((HashMap<String, Object>) c).get("id")).equals(id)).toList().getFirst();

					assertEqualsLabel(expectedPort, actualPort);

					assertEqualsLayoutOptions((HashMap<String, Object>) expectedPort.get("layoutOptions"), (HashMap<String, Object>) actualPort.get("layoutOptions"));
				}
			} else {
				AssertionFailureBuilder.assertionFailure()
						.message("ACTUAL has no ports, EXPECTED has")
						.actual(actual)
						.expected(expected)
						.buildAndThrow();
			}
		} else {
			if (actual.containsKey("ports")) {
				AssertionFailureBuilder.assertionFailure()
						.message("EXPECTED has no ports, ACTUAL has")
						.actual(actual)
						.expected(expected)
						.buildAndThrow();
			}
		}
	}

	public static void assertEqualsLabel(HashMap<String, Object> expected, HashMap<String, Object> actual) {
		if (expected.containsKey("labels")) {
			if (actual.containsKey("labels")) {
				ArrayList<Object> expectedLabelList = (ArrayList<Object>) expected.get("labels");
				ArrayList<Object> actualLabelList = (ArrayList<Object>) actual.get("labels");

				List<String> expectedLabelTextList = ((ArrayList<Object>) expectedLabelList.clone()).stream().map(l -> {
					return (String) ((HashMap<String, Object>) l).get("text");
				}).toList();

				List<String> actualLabelTextList = ((ArrayList<Object>) actualLabelList.clone()).stream().map(l -> {
					return (String) ((HashMap<String, Object>) l).get("text");
				}).toList();

				if (!expectedLabelTextList.containsAll(actualLabelTextList)) {
					AssertionFailureBuilder.assertionFailure()
							.message("ACTUAL has labels that EXPECTED has not")
							.actual(actual)
							.expected(expected)
							.buildAndThrow();
				}

				if (!actualLabelTextList.containsAll(expectedLabelTextList)) {
					AssertionFailureBuilder.assertionFailure()
							.message("EXPECTED has labels that ACTUAL has not")
							.actual(actual)
							.expected(expected)
							.buildAndThrow();
				}

				if (expectedLabelList.size() > 1) {
					logger.atWarn()
							.setMessage("EXPECTED contains more than one label: {}")
							.addArgument(expectedLabelList)
							.log();
				}

				if (actualLabelList.size() > 1) {
					logger.atWarn()
							.setMessage("ACTUAL contains more than one label: {}")
							.addArgument(actualLabelList)
							.log();
				}

				for (String id : expectedLabelTextList) {
					HashMap<String, Object> expectedLabel =
							(HashMap<String, Object>) expectedLabelList.stream().filter(c -> ((String) ((HashMap<String, Object>) c).get("text")).equals(id)).toList().getFirst();
					HashMap<String, Object> actualLabel =
							(HashMap<String, Object>) actualLabelList.stream().
									filter(c -> ((String) ((HashMap<String, Object>) c).get("text")).equals(id)).toList().getFirst();

					Assertions.assertEquals(expectedLabel.get("text"), actualLabel.get("text"));
				}
			} else {
				AssertionFailureBuilder.assertionFailure()
						.message("ACTUAL has no labels, EXPECTED has")
						.actual(actual)
						.expected(expected)
						.buildAndThrow();
			}
		} else {
			if (actual.containsKey("labels")) {
				AssertionFailureBuilder.assertionFailure()
						.message("EXPECTED has no labels, ACTUAL has")
						.actual(actual)
						.expected(expected)
						.buildAndThrow();
			}
		}
	}

	public static void assertEqualsLayoutOptions(HashMap<String, Object> expected, HashMap<String, Object> actual) {
		HashSet<String> expectedKeys = new HashSet<>(expected.keySet());
		expectedKeys.retainAll(checkedLayoutOptions);

		HashSet<String> actualKeys = new HashSet<>(actual.keySet());
		actualKeys.retainAll(checkedLayoutOptions);

		if (!expectedKeys.containsAll(actualKeys)) {
			AssertionFailureBuilder.assertionFailure()
					.message("ACTUAL has keys that EXPECTED has not")
					.actual(actual)
					.expected(expected)
					.buildAndThrow();
		}

		if (!actualKeys.containsAll(expectedKeys)) {
			AssertionFailureBuilder.assertionFailure()
					.message("EXPECTED has keys that ACTUAL has not")
					.actual(actual)
					.expected(expected)
					.buildAndThrow();
		}

		for (String key : expectedKeys) {
			Assertions.assertEquals(expected.get(key), actual.get(key), "Value for key " + key + " has changed");
		}
	}

	public static void assertEqualsEdge(HashMap<String, Object> expected, HashMap<String, Object> actual) {
		if (expected.containsKey("edges")) {
			if (actual.containsKey("edges")) {
				ArrayList<Object> expectedEdgeList = (ArrayList<Object>) expected.get("edges");
				ArrayList<Object> actualEdgeList = (ArrayList<Object>) actual.get("edges");

				List<String> expectedEdgeIdList = ((ArrayList<Object>) expectedEdgeList.clone()).stream().map(l -> {
					return (String) ((HashMap<String, Object>) l).get("id");
				}).toList();

				List<String> actualEdgeIdList = ((ArrayList<Object>) actualEdgeList.clone()).stream().map(l -> {
					return (String) ((HashMap<String, Object>) l).get("id");
				}).toList();

				if (!expectedEdgeIdList.containsAll(actualEdgeIdList)) {
					AssertionFailureBuilder.assertionFailure()
							.message("ACTUAL has ports that EXPECTED has not")
							.actual(actual)
							.expected(expected)
							.buildAndThrow();
				}

				if (!actualEdgeIdList.containsAll(expectedEdgeIdList)) {
					AssertionFailureBuilder.assertionFailure()
							.message("EXPECTED has ports that ACTUAL has not")
							.actual(actual)
							.expected(expected)
							.buildAndThrow();
				}

				for (String id : expectedEdgeIdList) {
					HashMap<String, Object> expectedEdge =
							(HashMap<String, Object>) expectedEdgeList.stream().filter(c -> ((String) ((HashMap<String, Object>) c).get("id")).equals(id)).toList().getFirst();
					HashMap<String, Object> actualEdge =
							(HashMap<String, Object>) actualEdgeList.stream().
									filter(c -> ((String) ((HashMap<String, Object>) c).get("id")).equals(id)).toList().getFirst();

					assertEqualsLabel(expectedEdge, actualEdge);

					assertEqualsLayoutOptions((HashMap<String, Object>) expectedEdge.get("layoutOptions"), (HashMap<String, Object>) actualEdge.get("layoutOptions"));
				}
			} else {
				AssertionFailureBuilder.assertionFailure()
						.message("ACTUAL has no edges, EXPECTED has")
						.actual(actual)
						.expected(expected)
						.buildAndThrow();
			}
		} else {
			if (actual.containsKey("edges")) {
				AssertionFailureBuilder.assertionFailure()
						.message("EXPECTED has no edges, ACTUAL has")
						.actual(actual)
						.expected(expected)
						.buildAndThrow();
			}
		}
	}
}
