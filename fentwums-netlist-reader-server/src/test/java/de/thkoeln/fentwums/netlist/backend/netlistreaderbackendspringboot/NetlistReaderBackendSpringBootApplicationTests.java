package de.thkoeln.fentwums.netlist.backend.netlistreaderbackendspringboot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class NetlistReaderBackendSpringBootApplicationTests {

	@Test
	void contextLoads() {
	}

	private static Stream<Arguments> provideTestCasesForAddition() {
		File inputFolder = new File("./src/test/resources/inputs/addition");
		File outputFolder = new File("./src/test/resources/outputs/addition");

		File[] inputs = inputFolder.listFiles();

		List<Arguments> argumentList = new ArrayList<>();

		Arrays.stream(inputs).forEach(i -> {
			File candidate = new File(outputFolder, i.getName());
			if (candidate.exists()) {
				argumentList.add(Arguments.of(i, candidate));
			}
		});

		return argumentList.stream();
	}

	@ParameterizedTest
	@MethodSource("provideTestCasesForAddition")
	void testAddition(File testcaseInput, File expectedOutput) {
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
		};

		// TODO: Get actual JSON from ELK graph
		HashMap<String, Object> actualMap = mapper.readValue(expectedOutput, typeRef);

		ElkAsserter.assertEquals(expectedOutput, actualMap);
		//assertEquals(input.getName(), output.getName());
	}

}
