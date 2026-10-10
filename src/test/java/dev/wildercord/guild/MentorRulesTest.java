package dev.wildercord.guild;

import dev.wildercord.guild.MentorRules.Refusal;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MentorRulesTest {
	private static final UUID M = new UUID(0, 1), A = new UUID(0, 2), B = new UUID(0, 3);

	@Test
	void aSeasonedMageTakesANewOne() {
		assertEquals(Refusal.NONE, MentorRules.refusal(false, 10, 0, 0, false, false));
		assertEquals(Refusal.NONE, MentorRules.refusal(false, 20, 4, 2, false, false));
		assertEquals(Refusal.SELF, MentorRules.refusal(true, 20, 0, 0, false, false));
		assertEquals(Refusal.MENTOR_TOO_YOUNG, MentorRules.refusal(false, 9, 0, 0, false, false));
		assertEquals(Refusal.APPRENTICE_TOO_FAR_ALONG, MentorRules.refusal(false, 20, 5, 0, false, false));
		assertEquals(Refusal.ALREADY_APPRENTICED, MentorRules.refusal(false, 20, 1, 0, true, false));
		assertEquals(Refusal.MENTOR_IS_APPRENTICE, MentorRules.refusal(false, 20, 1, 0, false, true));
		assertEquals(Refusal.FULL, MentorRules.refusal(false, 20, 1, MentorRules.MAX_APPRENTICES, false, false));
	}

	@Test
	void anApprenticeLearnsFasterOnlyNearTheirMentor() {
		assertEquals(1.25, MentorRules.gain(true), 1e-9);
		assertEquals(1.0, MentorRules.gain(false), 1e-9);
	}

	@Test
	void anApprenticeshipEndsAtTheTenthCircle() {
		assertFalse(MentorRules.graduates(9));
		assertTrue(MentorRules.graduates(10));
		// Taken before the 5th, graduated at the 10th: at most ten circles of tutelage, a dozen-odd crystals.
		int crystals = 0;
		for (int n = 1; n <= MentorRules.GRADUATE_AT; n++) crystals += MentorRules.tutelage(n);
		assertEquals(16, crystals);
	}

	@Test
	void theBookKeepsWhoIsWhose() {
		MentorRules.Book book = new MentorRules.Book();
		book.take(M, B);
		book.take(M, A);
		assertEquals(List.of(A, B), book.apprenticesOf(M));
		assertEquals(M, book.mentorOf(A));
		assertTrue(book.end(M, A));
		assertNull(book.mentorOf(A));
		assertTrue(book.end(B, M));
		assertFalse(book.end(M, B));
	}
}
