package dev.wildercord.spell;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Every lesson grammar and storage problem the server and the Cord screen send has a lang key with the same English. */
class LessonTextTest {
	private static Map<String, String> knownProblems() {
		Map<String, String> problems = new LinkedHashMap<>();
		problems.put(ExciseRules.GRAMMAR_PROBLEM, ExciseRules.GRAMMAR_KEY);
		problems.put(ExciseRules.STORAGE_PROBLEM, ExciseRules.STORAGE_KEY);
		problems.put(RelayRules.GRAMMAR_PROBLEM, RelayRules.GRAMMAR_KEY);
		problems.put(RelayRules.STORAGE_PROBLEM, RelayRules.STORAGE_KEY);
		problems.put(ReweaveRules.GRAMMAR_PROBLEM, ReweaveRules.GRAMMAR_KEY);
		problems.put(ReweaveRules.STORAGE_PROBLEM, ReweaveRules.STORAGE_KEY);
		for (LessonPackRules.Lesson lesson : LessonPackRules.ALL) {
			problems.put(lesson.grammarProblem, lesson.grammarKey);
			problems.put(lesson.storageProblem, lesson.storageKey);
		}
		return problems;
	}

	@Test
	void everyKnownProblemMapsToItsKey() {
		for (Map.Entry<String, String> entry : knownProblems().entrySet()) {
			assertEquals(entry.getValue(), LessonText.key(entry.getKey()), entry.getKey());
		}
	}

	@Test
	void theLangFileCarriesEachKeyWithTheSameEnglish() throws Exception {
		var language = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/wildercord/lang/en_us.json"))).getAsJsonObject();
		for (Map.Entry<String, String> entry : knownProblems().entrySet()) {
			assertTrue(language.has(entry.getValue()), entry.getValue() + " is missing from en_us.json");
			assertEquals(entry.getKey(), language.get(entry.getValue()).getAsString(), entry.getValue());
		}
	}

	@Test
	void aKnownProblemRendersWithTheEnglishAsFallback() {
		for (String problem : knownProblems().keySet()) {
			assertEquals(problem, LessonText.problem(problem).getString(), problem);
		}
	}

	@Test
	void theCompilersOwnWordsStayAsWritten() {
		String warning = "An Imbue can't store another Imbue.";
		assertNull(LessonText.key(warning));
		assertEquals(warning, LessonText.problem(warning).getString());
	}
}
