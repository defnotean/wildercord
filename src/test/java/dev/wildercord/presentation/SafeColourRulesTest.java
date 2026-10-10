package dev.wildercord.presentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SafeColourRulesTest {
	@Test
	void redAndGreenNeverMeet() {
		int red = SafeColourRules.safe(0xFF2020), green = SafeColourRules.safe(0x30E040);
		assertEquals(SafeColourRules.VERMILLION, red);
		assertEquals(SafeColourRules.BLUISH_GREEN, green);
		assertNotEquals(red, green);
	}

	@Test
	void everyHueFindsItsPlace() {
		assertEquals(SafeColourRules.ORANGE, SafeColourRules.safe(0xFF8A3A));
		assertEquals(SafeColourRules.YELLOW, SafeColourRules.safe(0xE8D86A));
		assertEquals(SafeColourRules.SKY_BLUE, SafeColourRules.safe(0x40E0F0));
		assertEquals(SafeColourRules.BLUE, SafeColourRules.safe(0x3060FF));
		assertEquals(SafeColourRules.REDDISH_PURPLE, SafeColourRules.safe(0xB070F0));
		assertEquals(SafeColourRules.VERMILLION, SafeColourRules.byHue(-10));
	}

	@Test
	void greysAndTheTopByteStay() {
		assertEquals(0xFFFFFF, SafeColourRules.safe(0xFFFFFF));
		assertEquals(0x808080, SafeColourRules.safe(0x808080));
		assertEquals(0, SafeColourRules.safe(0));
		assertEquals(0x80000000 | SafeColourRules.VERMILLION, SafeColourRules.safe(0x80FF0000));
	}
}
