package de.thkoeln.fentwums.netlist.backend.netlistreaderbackendspringboot;

import de.thkoeln.fentwums.netlist.backend.elkoptions.FEntwumSOptions;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

@WebMvcTest(NetlistReaderBackendSpringBootApplication.class)
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
	void testAddition(File testcaseInput, File expectedOutput, @Autowired MockMvcTester mockMvcTester) {
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
		};

		try (BufferedReader reader = new BufferedReader(new FileReader(testcaseInput))) {
			StringBuilder builder = new StringBuilder();

			String line;

			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append("\n");
			}

			String requestJsonString = builder.toString();


			MvcTestResult res = mockMvcTester.post()
					.uri("/graphRemoteFile?hash=1&performance-target=IntelligentAheadOfTime")
					.contentType(MediaType.APPLICATION_JSON)
					.multipart()
					.file("file", requestJsonString.getBytes(StandardCharsets.UTF_8))
					.exchange();

			HashMap<String, Object> actualMap = mapper.readValue(res.getResponse().getContentAsString(), typeRef);

			ElkAsserter.assertEquals(expectedOutput, actualMap);

		} catch (Exception e) {
			// do nothing
		}
	}

}
