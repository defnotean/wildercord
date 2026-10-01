package dev.wildercord.gametest;

import com.mojang.authlib.GameProfile;
import dev.wildercord.cast.Inscriptions;
import dev.wildercord.cast.Mastery;
import dev.wildercord.cast.MasteryChoices;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.TrainingDummy;
import dev.wildercord.cast.WildercordEntities;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.MasteryClient;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.content.Inscription;
import dev.wildercord.content.SpellCircleOption;
import dev.wildercord.content.SpellScrollItem;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.MasteryAttachments;
import dev.wildercord.player.MasteryBook;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.MasteryRules;
import dev.wildercord.spell.MasterySigil;
import dev.wildercord.spell.MasteryTraits;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Spell mastery in a real world: casts that strike real foes earn a spell experience and casts at nothing earn none;
 * training dummies teach only up to their cap; reaching Practised offers three traits, the Cord screen opens the choice
 * by itself, and choosing Thrifty Weave with the real mouse makes the spell cost less, in the readout and when cast; a
 * re-roll costs levels and is allowed once per rank, as is unbinding; editing a spell starts a new record and changing
 * back finds the old one; an Adept spell inscribed onto a scroll carries its own traits and sigil, a second player
 * studies it into rank I with the traits borrowed, and nothing duplicates or stacks (no experience travels, a second
 * study is refused, a rank I spell inscribes nothing, a scroll's cast teaches nothing, a scroll that doesn't match its
 * runes can't be read); and a named Adept spell's name goes to a second player standing nearby (added to the world for
 * the moment of the cast) but not to one further off, shows on the caster's own screen, and stays hidden with spell
 * titles switched off. Screenshots: the trait choice ({@code mastery_choice}), the Cord screen ({@code mastery_cord_screen}),
 * a fully grown spell's panel ({@code mastery_panel_mythic}), a spoken name ({@code mastery_title}) and circles at ranks
 * I, III and V side by side ({@code mastery_circles}).
 *
 * <p>The player is in survival with 200 health; the monsters have 200 health and never roll as Runebound. Runs in the
 * full suite; {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE} skip it.</p>
 */
public class WildercordMasteryTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.mastery";
	private static final List<RuneDef> BURST_HARM = List.of(Runes.BURST, Runes.HARM);
	private static final List<RuneDef> BURST_SHOCK = List.of(Runes.BURST, Runes.SHOCK);
	private static final List<RuneDef> WIDE_HARM = List.of(Runes.BURST, Runes.HARM, Runes.WIDEN);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		context.runOnClient(mc -> {
			mc.getWindow().setWindowed(1920, 1080);
			mc.options.guiScale().set(2);
			mc.resizeGui();
		});
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule natural_regeneration false");
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			failures.addAll(realHits(context, world));
			cleanup(context, world);
			failures.addAll(dummies(context, world));
			cleanup(context, world);
			failures.addAll(ranksAndTraits(context, world));
			cleanup(context, world);
			failures.addAll(editing(context, world));
			cleanup(context, world);
			failures.addAll(inscription(context, world));
			cleanup(context, world);
			failures.addAll(names(context, world));
			cleanup(context, world);
			failures.addAll(grown(context, world));
			cleanup(context, world);
			circles(context, world);
			if (!failures.isEmpty()) {
				throw new AssertionError("Spell mastery went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
				MagicQuality.spellTitles = true;
			});
		}
	}

	// ------------------------------------------------------------------ experience

	/** Five Arcane Bursts at two husks earn experience; five more at nothing earn none. */
	private static List<String> realHits(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		on(world, player -> {
			SpellCaster.edit(player, 0, ids(BURST_HARM));
			Spellbooks.set(player, Spellbooks.get(player).withSelected(0));
			husks(player);
			return null;
		});
		context.waitTicks(2);
		castTimes(context, world, 0, 5);
		MasteryBook.Entry entry = on(world, player -> {
			Mastery.flush(player);
			return Mastery.entry(player, BURST_HARM);
		});
		if (entry == null || entry.xp() <= 2) {
			out.add("five Arcane Bursts striking two husks should earn some experience (has " + (entry == null ? "no record" : entry.xp()) + ")");
			return out;
		}
		if (entry.practice() != 0 || entry.casts() < 1) {
			out.add("real hits should count as real casts, not practice (practice " + entry.practice() + ", casts " + entry.casts() + ")");
		}
		if (!entry.counters().containsKey("day")) {
			out.add("casts at noon under the open sky should be counted as by day (has " + entry.counters() + ")");
		}
		if (entry.seed() != MasterySigil.seed(on(world, Entity::getUUID), Mastery.keyOf(BURST_HARM))) {
			out.add("a new record's sigil should be drawn from the spell and its owner");
		}
		double before = entry.xp();
		cleanup(context, world);
		castTimes(context, world, 0, 5);
		double after = on(world, player -> {
			Mastery.flush(player);
			return Mastery.entry(player, BURST_HARM).xp();
		});
		if (after != before) {
			out.add("casting at nothing shouldn't earn experience (went from " + before + " to " + after + ")");
		}
		return out;
	}

	/** Forty-five Storm Bursts at three training dummies: practice reaches its cap and no further, and the spell stays Kindled. */
	private static List<String> dummies(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		on(world, player -> {
			SpellCaster.edit(player, 1, ids(BURST_SHOCK));
			ServerLevel level = player.level();
			for (int i = 0; i < 3; i++) {
				TrainingDummy dummy = WildercordEntities.TRAINING_DUMMY.create(level, EntitySpawnReason.COMMAND);
				double a = i * Math.PI * 2 / 3;
				dummy.snapTo(player.getX() + Math.cos(a) * 2.2, player.getY(), player.getZ() + Math.sin(a) * 2.2, 0, 0);
				dummy.addTag(TAG);
				level.addFreshEntity(dummy);
			}
			return null;
		});
		context.waitTicks(2);
		castTimes(context, world, 1, 45);
		MasteryBook.Entry entry = on(world, player -> {
			Mastery.flush(player);
			return Mastery.entry(player, BURST_SHOCK);
		});
		if (entry == null) {
			out.add("striking training dummies should still teach a spell a little (no record)");
		} else {
			if (Math.abs(entry.practice() - MasteryRules.PRACTICE_CAP) > 1e-6) {
				out.add("dummies should teach up to the practice cap of " + MasteryRules.PRACTICE_CAP + " and no further (has " + entry.practice() + ")");
			}
			if (entry.xp() != 0) {
				out.add("dummies shouldn't give real experience (has " + entry.xp() + ")");
			}
			if (entry.rank() != MasteryRules.FIRST) {
				out.add("practice alone shouldn't rank a spell up (it's " + MasteryRules.name(entry.rank()) + ")");
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ ranks and traits

	private static List<String> ranksAndTraits(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		String name = on(world, player -> {
			MasteryBook.Entry entry = Mastery.entry(player, BURST_HARM);
			Mastery.grant(player, BURST_HARM, MasteryRules.threshold(2) - entry.total() + 0.5);
			return SpellCaster.nameOf(player, Spellbooks.get(player), 0, BURST_HARM);
		});
		context.waitTicks(3);
		MasteryBook.Entry practised = on(world, player -> Mastery.entry(player, BURST_HARM));
		if (practised.rank() != 2 || practised.pendingSlot() != 0) {
			out.add("reaching 100 experience should make the spell Practised with its first trait waiting (rank " + practised.rank() + ", waiting "
				+ practised.pendingSlot() + ")");
			return out;
		}
		MasteryTraits.Profile profile = MasteryTraits.Profile.of(BURST_HARM);
		if (practised.offer().size() != 3 || new HashSet<>(practised.offer()).size() != 3
				|| !practised.offer().stream().allMatch(id -> MasteryTraits.get(id).map(t -> t.fits(profile)).orElse(false))) {
			out.add("Practised should offer three different traits that fit a harming burst (offered " + practised.offer() + ")");
		}
		boolean toast = context.computeOnClient(mc -> mc.gui.toastManager().getToast(MasteryClient.RankToast.class, MasteryClient.RankToast.token(name, 2)) != null);
		if (!toast) {
			out.add("reaching a rank should show its toast");
		}
		// Thrifty Weave among the cards (as if drawn), so the choice can be measured.
		on(world, player -> {
			MasteryBook.Entry entry = Mastery.entry(player, BURST_HARM);
			List<String> offer = new ArrayList<>(entry.offer());
			if (!offer.contains(MasteryTraits.THRIFTY.id())) {
				offer.set(1, MasteryTraits.THRIFTY.id());
			}
			player.setAttached(MasteryAttachments.MASTERY, MasteryAttachments.book(player).with(entry.withOffer(offer), List.of()));
			return null;
		});
		int costBefore = cost(world);
		int spentBefore = spend(context, world, 0);
		context.waitTicks(3);
		context.setScreen(CordScreen::new);
		context.waitTicks(5);
		int open = context.computeOnClient(mc -> mc.gui.screen() instanceof CordScreen cord ? cord.masteryOpen() : -2);
		if (open != 0) {
			out.add("the Cord screen should open the waiting trait's choice by itself (open for row " + open + ")");
			context.setScreen(() -> null);
			return out;
		}
		context.getInput().setCursorPos(2, 2);
		context.waitTicks(3);
		shot(context, "mastery_choice");
		int card = on(world, player -> Mastery.entry(player, BURST_HARM).offer().indexOf(MasteryTraits.THRIFTY.id()));
		double[] at = context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).masteryCardPoint(card));
		click(context, at);
		context.waitTicks(5);
		MasteryBook.Entry chosen = on(world, player -> Mastery.entry(player, BURST_HARM));
		if (!chosen.traits().get(0).equals(MasteryTraits.THRIFTY.id()) || !chosen.settled(0) || chosen.pendingSlot() >= 0) {
			out.add("clicking Thrifty Weave's card should give the spell that trait (traits " + chosen.traits() + ", waiting " + chosen.pendingSlot() + ")");
		}
		int costAfter = cost(world);
		if (costAfter >= costBefore || costAfter > Math.round(costBefore * 0.9) + 1) {
			out.add("Thrifty Weave should take a tenth off the spell's price (was " + costBefore + ", now " + costAfter + ")");
		}
		context.runOnClient(mc -> mc.gui.setScreen(null));
		context.waitTicks(2);
		int spentAfter = spend(context, world, 0);
		if (spentAfter >= spentBefore) {
			out.add("cast for real, the spell should spend less mana once Thrifty (spent " + spentBefore + ", then " + spentAfter + ")");
		}
		context.setScreen(CordScreen::new);
		context.waitTicks(5);
		context.getInput().setCursorPos(2, 2);
		context.waitTicks(10);
		shot(context, "mastery_cord_screen");
		context.setScreen(() -> null);
		context.waitTicks(2);

		// Adept: a re-roll costs three levels and is allowed once; then a choice; unbinding is the other change.
		String rerolls = on(world, player -> {
			Mastery.grant(player, BURST_HARM, MasteryRules.threshold(MasteryRules.ADEPT) - Mastery.entry(player, BURST_HARM).total() + 0.5);
			MasteryBook.Entry adept = Mastery.entry(player, BURST_HARM);
			if (adept.rank() != MasteryRules.ADEPT || adept.pendingSlot() != 1) {
				return "350 experience should make it Adept with its second trait waiting (rank " + adept.rank() + ", waiting " + adept.pendingSlot() + ")";
			}
			List<String> first = adept.offer();
			player.setExperienceLevels(2);
			MasteryChoices.request(player, MasteryChoices.REROLL, 0, 1, "");
			if (!Mastery.entry(player, BURST_HARM).offer().equals(first)) {
				return "a re-roll without three levels should be refused";
			}
			player.setExperienceLevels(5);
			MasteryChoices.request(player, MasteryChoices.REROLL, 0, 1, "");
			List<String> second = Mastery.entry(player, BURST_HARM).offer();
			if (second.isEmpty() || second.stream().anyMatch(first::contains) || player.experienceLevel != 2) {
				return "a re-roll should draw other traits and cost three levels (offered " + first + ", then " + second + ", levels " + player.experienceLevel + ")";
			}
			player.setExperienceLevels(10);
			MasteryChoices.request(player, MasteryChoices.REROLL, 0, 1, "");
			if (!Mastery.entry(player, BURST_HARM).offer().equals(second) || player.experienceLevel != 10) {
				return "a rank's second re-roll should be refused";
			}
			Component refused = MasteryChoices.request(player, MasteryChoices.CHOOSE, 0, 1, first.getFirst());
			if (Mastery.entry(player, BURST_HARM).settled(1)) {
				return "a trait no longer on offer shouldn't be taken (" + refused.getString() + ")";
			}
			MasteryChoices.request(player, MasteryChoices.CHOOSE, 0, 1, second.getFirst());
			if (!Mastery.entry(player, BURST_HARM).traits().get(1).equals(second.getFirst())) {
				return "choosing an offered trait should take it";
			}
			// Unbinding the first trait: it opens again with a fresh offer that leaves the old trait out.
			MasteryChoices.request(player, MasteryChoices.UNBIND, 0, 0, "");
			MasteryBook.Entry unbound = Mastery.entry(player, BURST_HARM);
			if (unbound.settled(0) || unbound.pendingSlot() != 0 || unbound.offer().contains(MasteryTraits.THRIFTY.id()) || player.experienceLevel != 7) {
				return "unbinding should clear the slot, offer afresh without it, and cost three levels (traits " + unbound.traits() + ", offer "
					+ unbound.offer() + ", levels " + player.experienceLevel + ")";
			}
			MasteryChoices.request(player, MasteryChoices.CHOOSE, 0, 0, unbound.offer().getFirst());
			MasteryChoices.request(player, MasteryChoices.UNBIND, 0, 0, "");
			if (!Mastery.entry(player, BURST_HARM).settled(0)) {
				return "a rank's second change should be refused";
			}
			return null;
		});
		if (rerolls != null) {
			out.add(rerolls);
		}
		return out;
	}

	// ------------------------------------------------------------------ a spell is its exact runes

	private static List<String> editing(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		MasteryBook.Entry old = on(world, player -> {
			SpellCaster.edit(player, 0, ids(WIDE_HARM));
			husks(player);
			return Mastery.entry(player, BURST_HARM);
		});
		context.waitTicks(2);
		castTimes(context, world, 0, 2);
		String edited = on(world, player -> {
			Mastery.flush(player);
			MasteryBook.Entry wide = Mastery.entry(player, WIDE_HARM);
			MasteryBook.Entry kept = Mastery.entry(player, BURST_HARM);
			if (wide == null || wide.xp() <= 0 || wide.rank() != MasteryRules.FIRST || !wide.active().isEmpty()) {
				return "an edited spell should start its own record from Kindled, with no traits (has " + wide + ")";
			}
			if (!old.equals(kept.usedAt(old.used()))) {
				return "the old spell's record should stay as it was while it's edited away";
			}
			if (Mastery.costFactor(player, WIDE_HARM) != 1.0) {
				return "the old spell's traits shouldn't carry over to the edited one";
			}
			SpellCaster.edit(player, 0, ids(BURST_HARM));
			MasteryBook.Entry back = Mastery.entry(player, BURST_HARM);
			if (back == null || back.xp() != old.xp() || !back.traits().equals(old.traits())) {
				return "changing the spell back should find its old record";
			}
			return null;
		});
		if (edited != null) {
			out.add(edited);
			return out;
		}
		castTimes(context, world, 0, 1);
		double grew = on(world, player -> {
			Mastery.flush(player);
			return Mastery.entry(player, BURST_HARM).xp();
		});
		if (grew <= old.xp()) {
			out.add("the restored spell should go on growing its old record (was " + old.xp() + ", now " + grew + ")");
		}
		return out;
	}

	// ------------------------------------------------------------------ inscription

	private static List<String> inscription(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		String result = on(world, player -> {
			ServerLevel level = player.level();
			MasteryBook.Entry mine = Mastery.entry(player, BURST_HARM);
			ItemStack scroll = inscribe(player, 0);
			if (scroll == null) {
				return "inscribing an Adept spell should make a scroll";
			}
			Inscription inscription = scroll.get(Inscription.TYPE);
			if (inscription == null) {
				return "an Adept spell's scroll should carry its mastery";
			}
			if (!inscription.traits().equals(mine.own()) || inscription.seed() != mine.seed() || inscription.rank() != mine.rank()
					|| !inscription.author().equals(player.getGameProfile().name())) {
				return "the inscription should carry the spell's own traits, sigil, rank and author (has " + inscription + ")";
			}
			if (Mastery.entry(player, BURST_HARM).xp() != mine.xp()) {
				return "inscribing shouldn't change the spell's experience";
			}
			// A second player studies it.
			FakePlayer reader = new Visitor(level, new GameProfile(UUID.nameUUIDFromBytes("wildercord-mastery-reader".getBytes()), "Reader"));
			reader.setPos(player.getX() + 3, player.getY(), player.getZ());
			reader.setAttached(WildercordAttachments.CORD, new ItemStack(WildercordItems.ECHO_CORD));
			reader.setAttached(WildercordAttachments.SPELLBOOK, Spellbook.EMPTY.withStarterGiven().learn(Runes.BURST.id()).learn(Runes.HARM.id()));
			ItemStack spare = scroll.copy();
			Component unread = Inscriptions.study(reader, scroll);
			if (unread != null) {
				return "the reader should be able to study the scroll (" + unread.getString() + ")";
			}
			MasteryBook.Entry theirs = Mastery.entry(reader, BURST_HARM);
			if (theirs == null || theirs.total() != 0 || theirs.rank() != MasteryRules.FIRST || theirs.seed() != mine.seed()
					|| !theirs.active().equals(inscription.traits()) || !theirs.own().isEmpty() || !theirs.teacher().equals(inscription.author())) {
				return "studying should start the reader at Kindled with the traits borrowed and the teacher's sigil (has " + theirs + ")";
			}
			if (!Spellbooks.get(reader).spells().get(0).equals(ids(BURST_HARM)) || !scroll.isEmpty()) {
				return "studying should thread the spell into the reader's Cord and spend the scroll";
			}
			if (!Mastery.traitsOf(reader, BURST_HARM).equals(inscription.traits())) {
				return "borrowed traits should work on the reader's spell at once";
			}
			// No stacking: a second copy is refused.
			if (Inscriptions.study(reader, spare) == null || spare.isEmpty()) {
				return "a second study of the same spell should be refused, and keep its scroll";
			}
			if (!Mastery.entry(reader, BURST_HARM).equals(theirs)) {
				return "a refused study shouldn't touch the reader's record";
			}
			// Borrowed traits can't be passed on: the reader's spell is Kindled, so its scroll carries nothing.
			ItemStack theirScroll = new ItemStack(WildercordItems.SPELL_SCROLL);
			if (Inscriptions.inscribe(reader, BURST_HARM, theirScroll) || theirScroll.has(Inscription.TYPE)) {
				return "a Kindled spell's scroll shouldn't carry any mastery";
			}
			// A scroll that doesn't match its runes can't be studied.
			ItemStack forged = spare.copy();
			forged.set(Inscription.TYPE, new Inscription(Mastery.keyOf(WIDE_HARM), inscription.traits(), 7, 5, "Forger"));
			FakePlayer stranger = new Visitor(level, new GameProfile(UUID.nameUUIDFromBytes("wildercord-mastery-stranger".getBytes()), "Stranger"));
			stranger.setAttached(WildercordAttachments.CORD, new ItemStack(WildercordItems.ECHO_CORD));
			stranger.setAttached(WildercordAttachments.SPELLBOOK, Spellbook.EMPTY.withStarterGiven().learn(Runes.BURST.id()).learn(Runes.HARM.id()));
			if (Inscriptions.study(stranger, forged) == null || MasteryAttachments.book(stranger).entries().size() != 0) {
				return "a scroll whose inscription doesn't match its runes shouldn't teach anything";
			}
			// Read aloud, a scroll teaches nobody: its caster's record doesn't grow.
			husks(player);
			player.setItemInHand(InteractionHand.MAIN_HAND, spare);
			player.setShiftKeyDown(false);
			spare.use(level, player, InteractionHand.MAIN_HAND);
			return null;
		});
		if (result != null) {
			out.add(result);
			return out;
		}
		// The scroll's tooltip shows its sigil.
		int sigilHeight = context.computeOnClient(mc -> {
			Inscription shown = new Inscription(Mastery.keyOf(BURST_HARM), List.of(), 1234L, MasteryRules.ADEPT, "Someone");
			var component = net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(shown);
			return component == null ? 0 : component.getHeight(mc.font);
		});
		if (sigilHeight <= 0) {
			out.add("an inscribed scroll's tooltip should show its sigil");
		}
		context.waitTicks(10);
		String read = on(world, player -> {
			double before = Mastery.entry(player, BURST_HARM).xp();
			Mastery.flush(player);
			double after = Mastery.entry(player, BURST_HARM).xp();
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			return after == before ? null : "a scroll read aloud shouldn't teach its caster anything (went from " + before + " to " + after + ")";
		});
		if (read != null) {
			out.add(read);
		}
		return out;
	}

	/** Inscribes spell slot {@code spell} the ordinary way (with the paper, ink and mana it costs) and returns the scroll made, or null. */
	private static ItemStack inscribe(ServerPlayer player, int spell) {
		player.getInventory().clearContent();
		player.getInventory().add(new ItemStack(Items.PAPER));
		player.getInventory().add(new ItemStack(Items.INK_SAC));
		Spellbooks.setMana(player, 1000);
		SpellScrollItem.inscribe(player, spell);
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(WildercordItems.SPELL_SCROLL)) {
				ItemStack copy = stack.copy();
				player.getInventory().clearContent();
				return copy;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ spoken names

	private static List<String> names(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			MagicQuality.spellTitles = true;
		});
		String spoken = on(world, player -> {
			SpellCaster.rename(player, 0, "Emberfall");
			ServerLevel level = player.level();
			// A second player beside the caster, and a third out of earshot: in the world only for the moment of the cast.
			FakePlayer near = new Visitor(level, new GameProfile(UUID.nameUUIDFromBytes("wildercord-mastery-near".getBytes()), "Near"));
			FakePlayer far = new Visitor(level, new GameProfile(UUID.nameUUIDFromBytes("wildercord-mastery-far".getBytes()), "Far"));
			near.snapTo(player.getX() + 4, player.getY(), player.getZ(), 0, 0);
			far.snapTo(player.getX() + Mastery.TITLE_RANGE + 8, player.getY(), player.getZ(), 0, 0);
			level.addNewPlayer(near);
			level.addNewPlayer(far);
			Mastery.Title title;
			try {
				Spellbooks.setMana(player, Mana.max(player));
				Spellbooks.setReadyAt(player, 0, 0);
				SpellCaster.cast(player, 0);
				title = Mastery.lastTitle();
			} finally {
				level.removePlayerImmediately(near, Entity.RemovalReason.DISCARDED);
				level.removePlayerImmediately(far, Entity.RemovalReason.DISCARDED);
			}
			if (title == null || !title.name().equals("Emberfall") || title.rank() < MasteryRules.ADEPT) {
				return "casting a named Adept spell should speak its name (spoke " + title + ")";
			}
			if (!title.viewers().contains(near.getUUID()) || !title.viewers().contains(player.getUUID())) {
				return "the name should go to the player nearby and the caster";
			}
			if (title.viewers().contains(far.getUUID())) {
				return "the name shouldn't carry past " + Mastery.TITLE_RANGE + " blocks";
			}
			return null;
		});
		if (spoken != null) {
			out.add(spoken);
		}
		context.waitTicks(3);
		boolean showing = context.computeOnClient(mc -> MasteryClient.Titles.showing().contains("Emberfall"));
		if (!showing) {
			out.add("the caster's own screen should show the spoken name");
		}
		shot(context, "mastery_title");
		// A Kindled spell's name isn't spoken, however named.
		String quiet = on(world, player -> {
			Mastery.Title before = Mastery.lastTitle();
			SpellCaster.rename(player, 1, "Whisper");
			Spellbooks.setMana(player, Mana.max(player));
			Spellbooks.setReadyAt(player, 1, 0);
			SpellCaster.cast(player, 1);
			return Mastery.lastTitle() == before ? null : "a Kindled spell's name shouldn't be spoken";
		});
		if (quiet != null) {
			out.add(quiet);
		}
		// Switched off in the magic settings, no title shows.
		context.waitTicks(60);
		context.runOnClient(mc -> MagicQuality.spellTitles = false);
		on(world, player -> {
			Spellbooks.setMana(player, Mana.max(player));
			Spellbooks.setReadyAt(player, 0, 0);
			SpellCaster.cast(player, 0);
			return null;
		});
		context.waitTicks(3);
		boolean hidden = context.computeOnClient(mc -> MasteryClient.Titles.showing().isEmpty());
		if (!hidden) {
			out.add("with spell titles switched off, no name should show");
		}
		context.runOnClient(mc -> {
			MagicQuality.spellTitles = true;
			mc.options.setCameraType(CameraType.FIRST_PERSON);
		});
		return out;
	}

	// ------------------------------------------------------------------ a fully grown spell

	/** Grown to Mythic with a trait in every slot: the panel, filmed. */
	private static List<String> grown(ClientGameTestContext context, TestSingleplayerContext world) {
		List<String> out = new ArrayList<>();
		String mythic = on(world, player -> {
			Mastery.grant(player, BURST_HARM, MasteryRules.threshold(MasteryRules.MYTHIC) - Mastery.entry(player, BURST_HARM).total() + 1);
			for (int i = 0; i < MasteryRules.SLOTS; i++) {
				MasteryBook.Entry entry = Mastery.entry(player, BURST_HARM);
				if (entry.pendingSlot() >= 0) {
					MasteryChoices.request(player, MasteryChoices.CHOOSE, 0, entry.pendingSlot(), entry.offer().getFirst());
				}
			}
			MasteryBook.Entry entry = Mastery.entry(player, BURST_HARM);
			if (entry.rank() != MasteryRules.MYTHIC || entry.own().size() != MasteryRules.SLOTS || new HashSet<>(entry.own()).size() != MasteryRules.SLOTS) {
				return "a Mythic spell should hold four different traits of its own (has " + entry.traits() + ")";
			}
			return null;
		});
		if (mythic != null) {
			out.add(mythic);
		}
		context.waitTicks(3);
		context.setScreen(CordScreen::new);
		context.waitTicks(3);
		double[] badge = context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).masteryBadgePoint(0));
		click(context, badge);
		int open = context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).masteryOpen());
		if (open != 0) {
			out.add("clicking a spell's rank badge should open its mastery panel (open for row " + open + ")");
		}
		context.getInput().setCursorPos(2, 2);
		context.waitTicks(15);
		shot(context, "mastery_panel_mythic");
		context.setScreen(() -> null);
		context.waitTicks(2);
		return out;
	}

	/** Circles at ranks I, III and V side by side, at night, for the eye. */
	private static void circles(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set 18000");
		on(world, player -> {
			player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 0.0F, false);
			return null;
		});
		context.waitTicks(10);
		long seed = on(world, player -> MasterySigil.seed(player.getUUID(), Mastery.keyOf(WIDE_HARM)));
		context.runOnClient(mc -> {
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
			mc.player.setYRot(0);
			mc.player.setXRot(0);
			int[] ranks = {1, 3, 5};
			for (int i = 0; i < ranks.length; i++) {
				Vec3 at = mc.player.getEyePosition().add((1 - i) * 2.7, 0, 5.5);
				mc.particleEngine.createParticle(new SpellCircleOption(ids(WIDE_HARM), RuneColors.of(Runes.HARM), 1.1F, 0F, 0F, 400, ranks[i], seed,
					ranks[i] == 5 ? MasteryAttachments.Look.STARS : 0), at.x, at.y, at.z, 0, 0, 0);
			}
		});
		context.waitTicks(30);
		shot(context, "mastery_circles");
		context.runOnClient(mc -> {
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
	}

	// ------------------------------------------------------------------ helpers

	/** A player that can be made afresh each time (the shared one from {@code FakePlayer.get} can't be added to a world twice). */
	private static final class Visitor extends FakePlayer {
		Visitor(ServerLevel level, GameProfile profile) {
			super(level, profile);
		}
	}

	/** The mana the spell in slot 0 costs this player now, as the readout shows it. */
	private static int cost(TestSingleplayerContext world) {
		return on(world, player -> Heart.manaCost(player, SpellCompiler.compile(BURST_HARM), Mastery.costFactor(player, BURST_HARM)));
	}

	/** Casts slot {@code spell} once for real and returns the mana it spent. */
	private static int spend(ClientGameTestContext context, TestSingleplayerContext world, int spell) {
		int spent = on(world, player -> {
			Spellbooks.setReadyAt(player, spell, 0);
			Spellbooks.setMana(player, Mana.max(player));
			float before = Spellbooks.mana(player);
			SpellCaster.cast(player, spell);
			return Math.round(before - Spellbooks.mana(player));
		});
		context.waitTicks(4);
		return spent;
	}

	/** Casts slot {@code spell} {@code times} times for real, a few ticks apart (each releases three ticks after it's cast). */
	private static void castTimes(ClientGameTestContext context, TestSingleplayerContext world, int spell, int times) {
		for (int i = 0; i < times; i++) {
			on(world, player -> {
				Spellbooks.setReadyAt(player, spell, 0);
				Spellbooks.setMana(player, Mana.max(player));
				player.setHealth(player.getMaxHealth());
				SpellCaster.cast(player, spell);
				return null;
			});
			context.waitTicks(4);
		}
		context.waitTicks(4);
	}

	/** Two husks two blocks either side of the player, who can't move or fight back. */
	private static void husks(ServerPlayer player) {
		ServerLevel level = player.level();
		spawn(level, EntityTypes.HUSK, player.position().add(2, 0, 0));
		spawn(level, EntityTypes.HUSK, player.position().add(-2, 0, 0));
	}

	private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, Vec3 at) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		if (mob == null) {
			throw new AssertionError("couldn't make a " + type);
		}
		mob.snapTo(at.x, at.y, at.z, 180, 0);
		mob.setNoAi(true);
		mob.addTag(TAG);
		mob.addTag("wildercord.rolled");
		level.addFreshEntity(mob);
		sturdy(mob);
		return mob;
	}

	private static void sturdy(LivingEntity mob) {
		AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(200);
		}
		mob.setHealth(mob.getMaxHealth());
	}

	/** An Echo Cord, every rune known, no records yet, and the player in survival on a stone platform in the sky with 200 health. */
	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 8) + " " + (y - 1) + " " + (z - 8) + " " + (x + 8) + " " + (y - 1) + " " + (z + 8) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 8) + " " + y + " " + (z - 8) + " " + (x + 8) + " " + (y + 6) + " " + (z + 8) + " minecraft:air");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			Spellbook book = Spellbooks.get(player).withStarterGiven();
			for (RuneDef rune : Runes.all()) {
				book = book.learn(rune.id());
			}
			for (int i = 0; i < book.spells().size(); i++) {
				book = book.withSpell(i, List.of()).withName(i, "");
			}
			Spellbooks.set(player, book);
			player.setAttached(MasteryAttachments.MASTERY, MasteryBook.EMPTY);
			player.setGameMode(GameType.SURVIVAL);
			sturdy(player);
			player.teleportTo(server.overworld(), x + 0.5, y, z + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
			player.setDeltaMovement(Vec3.ZERO);
		});
	}

	private static void cleanup(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=wildercord:rune_bolt]");
		context.waitTicks(10);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.clearFire();
			player.setHealth(player.getMaxHealth());
			Mastery.flush(player);
		});
	}

	private static Path shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.waitTicks(1);
		return context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	/** Clicks a point given in GUI coordinates (the window is GUI scale times as large). */
	private static void click(ClientGameTestContext context, double[] gui) {
		if (gui == null) {
			throw new AssertionError("nothing to click there (not on screen)");
		}
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(gui[0] * scale, gui[1] * scale);
		context.waitTicks(1);
		context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(3);
	}

	private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> step) {
		return world.getServer().computeOnServer(server -> step.apply(player(server)));
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static List<String> ids(List<RuneDef> runes) {
		return runes.stream().map(RuneDef::id).toList();
	}
}
