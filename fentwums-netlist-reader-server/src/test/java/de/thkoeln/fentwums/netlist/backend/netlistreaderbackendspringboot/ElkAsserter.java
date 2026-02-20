package de.thkoeln.fentwums.netlist.backend.netlistreaderbackendspringboot;

import org.junit.jupiter.api.AssertionFailureBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

	}

	public static void assertEqualsEdge(HashMap<String, Object> expected, HashMap<String, Object> actual) {

	}
}
