package dev.wildercord;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.Awakening;
import dev.wildercord.aura.FormDash;
import dev.wildercord.aura.MasterForms;
import dev.wildercord.aura.Ways;
import dev.wildercord.player.MasteryBook;
import dev.wildercord.spell.CircleVows;
import dev.wildercord.spell.Circles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Old-save loading for every saved piece of progression: a save written before a field existed, as NBT (what a
 * world stores) and JSON, must load with sane defaults and never grant anything it did not hold.
 */
class ProgressionSaveCompatTest {
	private static <T> T nbt(Codec<T> codec, CompoundTag tag) { return codec.parse(NbtOps.INSTANCE, tag).getOrThrow(); }
	private static <T> T json(Codec<T> codec, JsonObject object) { return codec.parse(JsonOps.INSTANCE, object).getOrThrow(); }

	@Test
	void emptyPlayerSavesLoadAsNothingLearned() {
		assertEquals(AuraAttachments.Data.NONE, nbt(AuraAttachments.Data.CODEC, new CompoundTag()));
		assertEquals(Awakening.State.NONE, nbt(Awakening.State.CODEC, new CompoundTag()));
		assertEquals(Ways.State.NONE, nbt(Ways.State.CODEC, new CompoundTag()));
		assertEquals(FormDash.Progress.NONE, nbt(FormDash.Progress.CODEC, new CompoundTag()));
		assertEquals(MasterForms.Progress.NONE, nbt(MasterForms.Progress.CODEC, new CompoundTag()));
		assertEquals(MasteryBook.EMPTY, MasteryBook.CODEC.parse(NbtOps.INSTANCE, new net.minecraft.nbt.ListTag()).getOrThrow());
		for (Object[] pair : new Object[][] {{AuraAttachments.Data.CODEC, AuraAttachments.Data.NONE}, {FormDash.Progress.CODEC, FormDash.Progress.NONE},
				{MasterForms.Progress.CODEC, MasterForms.Progress.NONE}, {Awakening.State.CODEC, Awakening.State.NONE}, {Ways.State.CODEC, Ways.State.NONE}}) {
			@SuppressWarnings("unchecked") Codec<Object> codec = (Codec<Object>) pair[0];
			assertEquals(pair[1], codec.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow());
		}
	}

	@Test
	void auraSavedBeforePracticeKeepsItsStageAndClampsAnOutOfRangeOne() {
		CompoundTag old = new CompoundTag();
		old.putString("method", "wildercord:ember");
		old.putInt("stage", 3);
		old.putDouble("xp", 12.5);
		AuraAttachments.Data data = nbt(AuraAttachments.Data.CODEC, old);
		assertEquals(3, data.stage());
		assertEquals(0, data.practice(), "Practice did not exist yet: it starts at zero");
		old.putInt("stage", 999);
		old.putDouble("xp", -5);
		data = nbt(AuraAttachments.Data.CODEC, old);
		assertEquals(AuraRules.clampStage(999), data.stage());
		assertEquals(0, data.xp());
	}

	@Test
	void wallTurnSaveFromBeforeStoneHingeKeepsWallTurnOnly() {
		JsonObject old = new JsonObject();
		old.addProperty("learned", true);
		old.addProperty("equipped", MasterForms.WALL_TURN);
		old.addProperty("ready_at", 500L);
		MasterForms.Progress progress = json(MasterForms.Progress.CODEC, old);
		assertTrue(progress.knows(MasterForms.WALL_TURN));
		assertFalse(progress.knows(MasterForms.STONE_HINGE));
		assertEquals(MasterForms.WALL_TURN, progress.equipped());
		assertEquals(0, progress.recoveryUntil());
		// A hand-edited save cannot equip a form it never learned.
		old.addProperty("equipped", MasterForms.STONE_HINGE);
		assertEquals(0, json(MasterForms.Progress.CODEC, old).equipped());
	}

	@Test
	void fieldFormSaveWithoutPracticeOrUnknownBitsLoadsSafely() {
		CompoundTag old = new CompoundTag();
		old.putInt("learned", -1);
		old.putInt("equipped", 99);
		FormDash.Progress progress = nbt(FormDash.Progress.CODEC, old);
		assertEquals(0, progress.equipped(), "An unknown form is never equipped");
		assertEquals(0, progress.practiced());
		assertEquals(progress, nbt(FormDash.Progress.CODEC, (CompoundTag) FormDash.Progress.CODEC.encodeStart(NbtOps.INSTANCE, progress).getOrThrow()));
	}

	@Test
	void masterySaveWithOnlyAKeyGetsFreshDefaults() {
		JsonObject old = new JsonObject();
		old.addProperty("key", "wildercord:bolt");
		MasteryBook.Entry entry = json(MasteryBook.Entry.CODEC, old);
		assertEquals(0, entry.xp());
		assertEquals(0, entry.changed());
		assertTrue(entry.traits().stream().allMatch(String::isEmpty));
	}

	@Test
	void heartCirclesAndVowsFromOldIntegerSaves() {
		// Circles were always one saved int; vows did not exist, so the attachment is absent and reads as 0.
		Codec<Integer> saved = Codec.INT;
		assertEquals(8, Circles.count(saved.parse(NbtOps.INSTANCE, IntTag.valueOf(8)).getOrThrow()));
		assertEquals(Circles.MAX, Circles.count(Integer.MAX_VALUE));
		assertEquals(0, Circles.count(-4));
		assertEquals(CircleVows.Effect.NONE, CircleVows.effect(0, Circles.MAX));
		assertFalse(CircleVows.open(0, 20).isEmpty(), "An old Circle XX heart is offered its vows, not given them");
		assertEquals(CircleVows.Effect.NONE, CircleVows.effect(CircleVows.clean(0xFFFFFFFF), Circles.MAX));
	}
}
