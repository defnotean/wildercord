package dev.wildercord.gametest;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.chorus.Chorus;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordComponents;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.runesmith.ContractRules;
import dev.wildercord.runesmith.Contracts;
import dev.wildercord.runesmith.DuplicateSwap;
import dev.wildercord.runesmith.Runesmith;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The social magic, checked in a real world: a Runesmith and its trades (novice and master, and
 * the rune on the wandering trader's shelf), the duplicate buyback and reroll, a contract counted
 * up and handed in at a Scribing Desk, and two casters' spells merging into a chorus (the second
 * voice is a husk calling the chorus logic directly, since a test has only one real player).
 *
 * <p>Runs in the full suite; skipped by {@code WILDERCORD_TOUR_ONLY} and {@code WILDERCORD_CORDS_ONLY}.</p>
 */
public class WildercordSocialTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				player.setGameMode(GameType.SURVIVAL);
				Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
				Spellbooks.set(player, Spellbooks.get(player).withStarterGiven());
			});
			context.waitTicks(5);
			List<String> failures = new ArrayList<>();
			attempt(failures, "Runesmith trades", () -> runesmith(world));
			attempt(failures, "duplicate swap", () -> duplicateSwap(world));
			context.waitTicks(2);
			attempt(failures, "contracts", () -> contracts(context, world));
			context.waitTicks(2);
			attempt(failures, "chorus", () -> chorus(context, world));
			context.waitTicks(40);
			world.getServer().runCommand("kill @e[tag=wildercord.social]");
			if (!failures.isEmpty()) {
				throw new AssertionError("The Runesmith, contracts or chorus went wrong:\n  " + String.join("\n  ", failures));
			}
		}
	}

	private static void attempt(List<String> failures, String what, Runnable test) {
		try {
			test.run();
		} catch (AssertionError | RuntimeException e) {
			failures.add(what + ": " + e.getMessage());
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	private static Villager runesmith(ServerLevel level, Vec3 at, int tradeLevel) {
		Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
		check(villager != null, "a villager should spawn");
		villager.snapTo(at.x, at.y, at.z, 180, 0);
		villager.setNoAi(true);
		villager.addTag("wildercord.social");
		villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), Runesmith.PROFESSION).withLevel(tradeLevel));
		level.addFreshEntity(villager);
		return villager;
	}

	private static boolean isRune(ItemStack stack, int tier) {
		return stack.is(WildercordItems.RUNE) && RuneItem.runeOf(stack).filter(r -> r.tier() == tier && !Runes.innate(r)).isPresent();
	}

	// ------------------------------------------------------------------ the Runesmith

	private static void runesmith(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			check(BuiltInRegistries.POINT_OF_INTEREST_TYPE.containsKey(Runesmith.POI), "the Scribing Desk should be a job site");
			check(BuiltInRegistries.VILLAGER_PROFESSION.containsKey(Runesmith.PROFESSION), "the Runesmith should be a profession");

			Villager novice = runesmith(level, player.position().add(3, 0, 0), 1);
			check(Runesmith.is(novice), "the villager should be a Runesmith");
			List<MerchantOffer> offers = List.copyOf(novice.getOffers());
			check(offers.size() == 2, "a novice Runesmith should stock two trades (has " + offers.size() + ")");
			for (MerchantOffer offer : offers) {
				ItemStack result = offer.getResult();
				check(result.is(Items.EMERALD) || result.is(WildercordItems.BLANK_RUNE) || isRune(result, 1),
					"a novice Runesmith should trade emeralds, Blank Runes or Tier I runes (offers " + result + ")");
			}

			Villager master = runesmith(level, player.position().add(-3, 0, 0), 5);
			List<MerchantOffer> masterOffers = List.copyOf(master.getOffers());
			check(masterOffers.stream().anyMatch(o -> isRune(o.getResult(), 3)), "a master Runesmith should sell a Tier III rune");
			check(masterOffers.stream().anyMatch(o -> o.getResult().is(WildercordItems.MANA_CRYSTAL)), "a master Runesmith should sell a Mana Crystal");

			// A wandering trader can stock a rune too.
			var trades = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
			ResourceKey<VillagerTrade> wandering = ResourceKey.create(Registries.VILLAGER_TRADE, Identifier.fromNamespaceAndPath("wildercord", "wandering_trader/emerald_rune"));
			check(trades.get(wandering).isPresent(), "the wandering trader's rune trade should load");
			check(trades.get(VillagerTradeTags.WANDERING_TRADER_UNCOMMON).map(set -> set.stream().anyMatch(h -> h.is(wandering))).orElse(false),
				"the wandering trader's uncommon wares should include a rune");
		});
	}

	// ------------------------------------------------------------------ buyback and reroll

	private static void duplicateSwap(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			player.getInventory().clearContent();
			Spellbook book = Spellbooks.get(player);
			for (RuneDef rune : List.of(Runes.HEAL, Runes.BEAM, Runes.BURST)) {
				book = book.learn(rune.id());
			}
			Spellbooks.set(player, book);
			ItemStack heals = RuneItem.stack(Runes.HEAL);
			heals.setCount(2);
			player.getInventory().add(heals);
			player.getInventory().add(RuneItem.stack(Runes.BEAM));
			player.getInventory().add(RuneItem.stack(Runes.BURST));
			// A rune that isn't known yet is never bought back.
			player.getInventory().add(RuneItem.stack(Runes.ZONE));
			// Nor a Lightning rune (a lightning rod makes them from cheap blanks), known or not.
			Spellbooks.set(player, Spellbooks.get(player).learn(Runes.LIGHTNING.id()));
			ItemStack lightning = RuneItem.stack(Runes.LIGHTNING);
			lightning.setCount(8);
			player.getInventory().add(lightning);

			Villager villager = runesmith(level, player.position().add(0, 0, 3), 1);
			int vanilla = villager.getOffers().size();
			int added = DuplicateSwap.offer(player, villager);
			// Buybacks for Heal, Beam and Burst; a Tier I reroll (Heal twice) and a Tier II one (Beam and Burst).
			check(added == 5, "a Runesmith should offer 3 buybacks and 2 rerolls (offered " + added + ")");
			List<MerchantOffer> swaps = villager.getOffers().stream().filter(DuplicateSwap::isSwap).toList();
			check(swaps.size() == 5, "the swap offers should be in the villager's list");

			MerchantOffer healBack = swaps.stream().filter(o -> o.getCostB().isEmpty() && Runes.HEAL.id().equals(o.getCostA().get(WildercordComponents.RUNE)))
				.findFirst().orElseThrow(() -> new AssertionError("no buyback for a known Heal rune"));
			check(healBack.getResult().is(Items.EMERALD) && healBack.getResult().getCount() >= 1, "a buyback should pay emeralds");
			check(healBack.satisfiedBy(RuneItem.stack(Runes.HEAL), ItemStack.EMPTY), "a known Heal rune should pay for its buyback");
			check(!healBack.satisfiedBy(RuneItem.stack(Runes.HARM), ItemStack.EMPTY), "another rune shouldn't pay for Heal's buyback");
			check(swaps.stream().noneMatch(o -> Runes.ZONE.id().equals(o.getCostA().get(WildercordComponents.RUNE))), "an unknown rune shouldn't be bought back");
			check(swaps.stream().noneMatch(o -> Runes.LIGHTNING.id().equals(o.getCostA().get(WildercordComponents.RUNE))
				|| Runes.LIGHTNING.id().equals(o.getCostB().get(WildercordComponents.RUNE))), "a Lightning rune shouldn't be bought back or rerolled");
			ItemStack ranked = RuneItem.stack(Runes.HEAL);
			ranked.set(WildercordComponents.RANK, 3);
			check(!healBack.satisfiedBy(ranked, ItemStack.EMPTY), "a rank III rune shouldn't pay a rank I's buyback");
			check(healBack.getXp() == 0, "a buyback shouldn't give experience");

			MerchantOffer reroll = swaps.stream().filter(o -> !o.getCostB().isEmpty() && isRune(o.getResult(), 1)).findFirst()
				.orElseThrow(() -> new AssertionError("no Tier I reroll for two known Heal runes"));
			check(reroll.satisfiedBy(RuneItem.stack(Runes.HEAL), RuneItem.stack(Runes.HEAL)), "two Heal runes should pay for the reroll");
			String rolled = reroll.getResult().get(WildercordComponents.RUNE);
			check(!Spellbooks.knows(player, rolled), "a reroll should give a rune not known yet (gave " + rolled + ")");
			check(swaps.stream().anyMatch(o -> !o.getCostB().isEmpty() && isRune(o.getResult(), 2)), "Beam and Burst should reroll into a Tier II rune");

			DuplicateSwap.strip(villager);
			check(villager.getOffers().size() == vanilla && villager.getOffers().stream().noneMatch(DuplicateSwap::isSwap),
				"closing the trade should take the swap offers away again");
			player.getInventory().clearContent();
		});
	}

	// ------------------------------------------------------------------ contracts

	/** Something for the contracts' spells to land on: a husk (a real creature), or a Training Dummy. */
	private static net.minecraft.world.entity.LivingEntity mark(ServerLevel level, Vec3 at, boolean dummy) {
		net.minecraft.world.entity.LivingEntity mark = dummy ? dev.wildercord.cast.WildercordEntities.TRAINING_DUMMY.create(level, EntitySpawnReason.COMMAND)
			: EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		check(mark != null, "a target should spawn");
		mark.snapTo(at.x, at.y, at.z, 0, 0);
		if (mark instanceof Mob mob) {
			mob.setNoAi(true);
		}
		mark.addTag("wildercord.social");
		level.addFreshEntity(mark);
		return mark;
	}

	private static void contracts(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] marks = world.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			player.getInventory().clearContent();
			ContractRules.Board today = Contracts.board(player);
			check(today.contracts().size() == ContractRules.COUNT, "the board should hold three contracts");
			check(Contracts.board(player) == today || Contracts.board(player).equals(today), "reading the board twice in a day should give the same contracts");

			// A board with a ley line contract two casts long.
			player.setAttached(Contracts.BOARD, new ContractRules.Board(Contracts.day(player), List.of(
				new ContractRules.Contract(ContractRules.LEY, "", 2, 0, false, "emerald:8"),
				new ContractRules.Contract(ContractRules.REACTION, "conduct", 3, 0, false, "blank_rune:6"),
				new ContractRules.Contract(ContractRules.SPELL_KILLS, "", 10, 0, false, "emerald:6"))));
			player.setAttached(WildercordAttachments.ON_LEY, true);
			return new int[] {mark(level, player.position().add(4, 0, 4), false).getId(), mark(level, player.position().add(-4, 0, 4), true).getId()};
		});
		// Each step in a tick of its own: one spell landing credits one cast.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Contracts.onCast(player, List.of(Runes.BOLT, Runes.HARM));
			check(Contracts.board(player).contracts().getFirst().progress() == 0, "a cast that hasn't landed on anything shouldn't count yet");
			Contracts.onSpellHit(player, (net.minecraft.world.entity.LivingEntity) player.level().getEntity(marks[1]), "");
			check(Contracts.board(player).contracts().getFirst().progress() == 0, "a spell landing on a Training Dummy shouldn't count");
		});
		context.waitTicks(1);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Contracts.onSpellHit(player, (net.minecraft.world.entity.LivingEntity) player.level().getEntity(marks[0]), "");
			check(Contracts.board(player).contracts().getFirst().progress() == 1, "a cast on a ley line that lands on a creature should count");
		});
		context.waitTicks(1);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Contracts.onSpellHit(player, (net.minecraft.world.entity.LivingEntity) player.level().getEntity(marks[0]), "");
			Contracts.onCast(player, List.of(Runes.SELF, Runes.HARM));
			check(Contracts.board(player).contracts().getFirst().done(), "a second cast that landed should finish the contract");
			player.setAttached(WildercordAttachments.ON_LEY, false);
		});
		context.waitTicks(1);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Contracts.onReaction(player, "conduct");
			Contracts.onSpellHit(player, (net.minecraft.world.entity.LivingEntity) player.level().getEntity(marks[1]), "storm");
			check(Contracts.board(player).contracts().get(1).progress() == 0, "a reaction on a Training Dummy shouldn't count");
		});
		context.waitTicks(1);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			net.minecraft.world.entity.LivingEntity husk = (net.minecraft.world.entity.LivingEntity) level.getEntity(marks[0]);
			Contracts.onReaction(player, "shatter");
			Contracts.onSpellHit(player, husk, "fire");
			check(Contracts.board(player).contracts().get(1).progress() == 0, "the wrong reaction shouldn't count");
			Contracts.onReaction(player, "conduct");
			check(Contracts.board(player).contracts().get(1).progress() == 1, "a Conduct reaction on a creature should count");
			for (int id : marks) {
				if (level.getEntity(id) != null) {
					level.getEntity(id).discard();
				}
			}

			// Handed in at a Scribing Desk: right-clicking it pays out.
			BlockPos desk = player.blockPosition().offset(2, 0, 2);
			level.setBlockAndUpdate(desk, Runesmith.SCRIBING_DESK.defaultBlockState());
			level.getBlockState(desk).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(desk), Direction.UP, desk, false));
			int emeralds = player.getInventory().countItem(Items.EMERALD);
			check(emeralds == 8, "handing in the contract should pay 8 emeralds (has " + emeralds + ")");
			check(Contracts.board(player).contracts().getFirst().claimed(), "the contract should be marked handed in");
			level.getBlockState(desk).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(desk), Direction.UP, desk, false));
			check(player.getInventory().countItem(Items.EMERALD) == 8, "a contract should only pay once");
			level.removeBlock(desk, false);
			player.getInventory().clearContent();
		});
	}

	// ------------------------------------------------------------------ chorus

	/** Another caster beside the player: a husk, to sing the same Burst at the same moment. */
	private static Mob singer(ServerPlayer player) {
		ServerLevel level = player.level();
		Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		check(husk != null, "a husk should spawn");
		Vec3 beside = player.position().add(3, 0, 0);
		husk.snapTo(beside.x, beside.y, beside.z, 0, 0);
		husk.setNoAi(true);
		husk.addTag("wildercord.social");
		level.addFreshEntity(husk);
		return husk;
	}

	private static void chorus(ClientGameTestContext context, TestSingleplayerContext world) {
		List<RuneDef> spell = List.of(Runes.BURST, Runes.HARM);
		// A foe never sings with you: the husk, not an ally, sings alone, and so does the player after it.
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Mob husk = singer(player);
			SpellCompiler.Compiled compiled = SpellCompiler.compile(spell);
			Cast foe = new Cast(husk, 1, Heart.Bonuses.NONE, false, null, new Cast.Info(compiled.root(), spell.size(), "", spell));
			check(Chorus.sing(foe, spell, compiled.root()).voices() == 1, "the husk's voice should sing alone");
			Cast lone = new Cast(player, 1, Heart.bonuses(player), false, null, new Cast.Info(compiled.root(), spell.size(), "", spell));
			check(Chorus.sing(lone, spell, SpellCompiler.compile(spell).root()).voices() == 1, "a caster who isn't an ally shouldn't join (or cut short) a foe's spell");
			check(foe.alive(), "another caster's spell should never be cut short");
			husk.discard();
		});
		// Past the chorus window, so the two voices above are forgotten.
		context.waitTicks(dev.wildercord.chorus.ChorusRules.WINDOW + 5);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			// On the same team, a husk is an ally: the two sing together.
			SpellCompiler.Compiled compiled = SpellCompiler.compile(spell);
			net.minecraft.world.scores.PlayerTeam team = server.getScoreboard().addPlayerTeam("wildercord_chorus");
			Mob ally = singer(player);
			server.getScoreboard().addPlayerToTeam(ally.getScoreboardName(), team);
			server.getScoreboard().addPlayerToTeam(player.getScoreboardName(), team);
			Cast first = new Cast(ally, 1, Heart.Bonuses.NONE, false, null, new Cast.Info(compiled.root(), spell.size(), "", spell));
			Chorus.Sung alone = Chorus.sing(first, spell, compiled.root());
			check(alone.voices() == 1 && alone.cast() == first, "the first voice should sing alone");

			Cast second = new Cast(player, 2, Heart.bonuses(player), false, null, new Cast.Info(compiled.root(), spell.size(), "", spell));
			Chorus.Sung sung = Chorus.sing(second, spell, SpellCompiler.compile(spell).root());
			server.getScoreboard().removePlayerTeam(team);
			check(sung.voices() == 2, "allies casting the same shape, together and close, should become a chorus of two (got " + sung.voices() + ")");
			check(Math.abs(sung.cast().power - second.power * 1.5) < 1e-6, "a chorus of two should be half again as strong");
			check(sung.cast().gear() == second.gear(), "a chorus should keep its caster's casting gear");
			check(sung.root().groups.getFirst().count(Runes.WIDEN) == 1, "a chorus of two should widen the Burst once");
			check(first.alive(), "the first voice's spell should go on as it was");
			check(Heart.discovered(player, "feat:" + Feats.CHORUS), "singing a chorus should earn the Chorus feat");
			CastEngine.cast(sung.cast(), sung.root());

			// Different shapes never sing together.
			Cast bolt = new Cast(player, 1, Heart.bonuses(player), false, null, Cast.Info.NONE);
			List<RuneDef> boltSpell = List.of(Runes.BOLT, Runes.HARM);
			check(Chorus.sing(bolt, boltSpell, SpellCompiler.compile(boltSpell).root()).voices() == 1, "a Bolt shouldn't join a chorus of Bursts");
		});
	}
}
