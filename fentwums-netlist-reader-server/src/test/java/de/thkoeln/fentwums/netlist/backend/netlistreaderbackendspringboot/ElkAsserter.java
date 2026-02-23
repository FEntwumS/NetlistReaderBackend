package de.thkoeln.fentwums.netlist.backend.netlistreaderbackendspringboot;

import org.junit.jupiter.api.AssertionFailureBuilder;
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

		if (expected.containsKey("children")) {
			if (actual.containsKey("children")) {
				ArrayList<Object> expectedChildrenList = (ArrayList<Object>) expected.get("children");
				ArrayList<Object> actualChildrenList = (ArrayList<Object>) actual.get("children");

				List<String> expectedChildrenIdList = expectedChildrenList.stream().map(c -> {
					return (String) ((HashMap<String, Object>) c).get("id");
				}).toList();

				List<String> actualChildrenIdList = actualChildrenList.stream().map(c -> {
					return (String) ((HashMap<String, Object>) c).get("id");
				}).toList();

				if (!expectedChildrenIdList.containsAll(actualChildrenIdList)) {
					AssertionFailureBuilder.assertionFailure()
							.message("ACTUAL has more children than EXPECTED has")
							.actual(actual)
							.expected(expected)
							.buildAndThrow();
				}

				if (!actualChildrenIdList.containsAll(expectedChildrenIdList)) {
					AssertionFailureBuilder.assertionFailure()
							.message("ACTUAL has less children than EXPECTED has")
							.actual(actual)
							.expected(expected)
							.buildAndThrow();
				}

				for (String id : expectedChildrenIdList) {
					HashMap<String, Object> expectedChild =
							(HashMap<String, Object>) expectedChildrenList.stream().
									filter(c -> ((String) ((HashMap<String, Object>) c).get("id")).equals(id));
					HashMap<String, Object> actualChild =
							(HashMap<String, Object>) actualChildrenList.stream().
									filter(c -> ((String) ((HashMap<String, Object>) c).get("id")).equals(id));

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

	}

	public static void assertEqualsLabel(HashMap<String, Object> expected, HashMap<String, Object> actual) {

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

	}
}
