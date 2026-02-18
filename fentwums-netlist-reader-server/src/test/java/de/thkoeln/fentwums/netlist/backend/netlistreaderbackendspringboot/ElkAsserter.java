package de.thkoeln.fentwums.netlist.backend.netlistreaderbackendspringboot;

import org.junit.jupiter.api.AssertionFailureBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;

public class ElkAsserter {
	public static void assertEquals(File expected, HashMap<String, Object> actual) {
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
		};

		HashMap<String, Object> expectedMap = mapper.readValue(expected, typeRef);

		assertEqualsNode(expectedMap, actual);
	}

	public static void assertEqualsNode(HashMap<String, Object> expected, HashMap<String, Object> actual) {
		if (expected.containsKey("children")) {
			if (actual.containsKey("children")) {
				if (!expected.keySet().containsAll(actual.keySet())) {
					AssertionFailureBuilder.assertionFailure()
							.message("ACTUAL has more children than EXPECTED has")
							.actual(actual)
							.expected(expected)
							.buildAndThrow();
				}

				if (!actual.keySet().containsAll(expected.keySet())) {
					AssertionFailureBuilder.assertionFailure()
							.message("ACTUAL has less children than EXPECTED has")
							.actual(actual)
							.expected(expected)
							.buildAndThrow();
				}

				for (String key : expected.keySet()) {
					HashMap<String, Object> expectedChild = (HashMap<String, Object>) expected.get(key);
					HashMap<String, Object> actualChild = (HashMap<String, Object>) actual.get(key);

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

	}

	public static void assertEqualsEdge(HashMap<String, Object> expected, HashMap<String, Object> actual) {

	}
}
