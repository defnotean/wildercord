package dev.wildercord.wildlife;

import dev.wildercord.aura.world.RootcoilSerpent;
import dev.wildercord.aura.world.SerpentRules;
import dev.wildercord.cast.Archivist;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.monster.KarakuriSentinel;
import dev.wildercord.monster.MonsterContent;
import dev.wildercord.monster.PaperLanternSpirit;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.world.dungeons.CatSanctuaryPiece;
import dev.wildercord.world.dungeons.LanternBathhousePiece;
import dev.wildercord.world.dungeons.SerpentGrovePiece;
import dev.wildercord.world.sites.wilds.SakuraVillagePiece;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * In-game verification for the four real Shrine Cats ({@code Yuki}, {@code Odie}, {@code Freya}, {@code Kuro}),
 * Sakura Village NPCs and architecture, the 4-level Cat Sanctuary dungeon (non-lethal yields, floor perks,
 * post-clear taming, unique companion powers, persistence), Lantern Bathhouse, Karakuri Sentinel,
 * Paper Lantern Spirit, Ricochet + shrine runes, and Archivist / Rootcoil Serpent anti-cheese fixes.
 */
public final class ShrineSanctuaryTest implements FabricClientGameTest {
	private static void check(boolean cond, String why) {
		if (!cond) {
			throw new AssertionError(why);
		}
	}

	private static ServerPlayer p(MinecraftServer s) {
		return s.getPlayerList().getPlayers().getFirst();
	}

	private static void shot(ClientGameTestContext c, String name) {
		c.runOnClient(mc -> {
			mc.gui.hud.getChat().clearMessages(false);
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static void loadArea(ServerLevel l, BoundingBox b) {
		for (int cx = (b.minX() >> 4) - 1; cx <= (b.maxX() >> 4) + 1; cx++) {
			for (int cz = (b.minZ() >> 4) - 1; cz <= (b.maxZ() >> 4) + 1; cz++) {
				l.getChunk(cx, cz);
			}
		}
	}

	private static void placePiece(ServerLevel l, StructurePiece piece, long seed) {
		BoundingBox b = piece.getBoundingBox();
		loadArea(l, b);
		piece.postProcess(l, l.structureManager(), l.getChunkSource().getGenerator(), RandomSource.create(seed),
			b, new ChunkPos(b.minX() >> 4, b.minZ() >> 4), BlockPos.ZERO);
	}

	private static void hold(MinecraftServer s, ItemStack stack) {
		p(s).setItemInHand(InteractionHand.MAIN_HAND, stack);
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(30);
			w.getServer().runCommand("gamerule spawn_mobs false");
			w.getServer().runCommand("difficulty normal");
			w.getServer().runCommand("time set 6000");
			w.getServer().runCommand("weather clear");

			// 1. Clean lineup court at (0, 100, 0) showing Yuki, Odie, Freya, Kuro, Sakura Villagers & custom mobs.
			Map<ShrineCatRules.Variant, UUID> lineupIds = new EnumMap<>(ShrineCatRules.Variant.class);
			UUID teaId = w.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				for (int x = -10; x <= 10; x++) {
					for (int z = -8; z <= 12; z++) {
						l.setBlock(new BlockPos(x, 100, z), Blocks.CHERRY_PLANKS.defaultBlockState(), 2);
						for (int y = 101; y <= 108; y++) {
							l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
				double[] xs = {-3.0, -1.0, 1.0, 3.0};
				ShrineCatRules.Variant[] order = ShrineCatRules.Variant.values();
				for (int i = 0; i < order.length; i++) {
					ShrineCat cat = ShrineCatContent.SHRINE_CAT.create(l, EntitySpawnReason.COMMAND);
					check(cat != null, "ShrineCat created");
					cat.snapTo(xs[i], 101.0, 3.5, 180.0F, 0.0F);
					cat.configureVillage(order[i], new BlockPos((int) xs[i], 101, 3));
					cat.setNoAi(true);
					l.addFreshEntity(cat);
					lineupIds.put(order[i], cat.getUUID());
				}
				SakuraVillager tea = ShrineCatContent.SAKURA_VILLAGER.create(l, EntitySpawnReason.COMMAND);
				check(tea != null, "Tea Master created");
				tea.snapTo(-4.8, 101.0, 6.0, 180.0F, 0.0F);
				tea.configure(SakuraVillager.TEA_MASTER);
				tea.setNoAi(true);
				l.addFreshEntity(tea);

				SakuraVillager maiden = ShrineCatContent.SAKURA_VILLAGER.create(l, EntitySpawnReason.COMMAND);
				check(maiden != null, "Shrine Maiden created");
				maiden.snapTo(-1.5, 101.0, 6.0, 180.0F, 0.0F);
				maiden.configure(SakuraVillager.SHRINE_MAIDEN);
				maiden.setNoAi(true);
				l.addFreshEntity(maiden);

				KarakuriSentinel karakuri = MonsterContent.KARAKURI_SENTINEL.create(l, EntitySpawnReason.COMMAND);
				check(karakuri != null, "KarakuriSentinel created");
				karakuri.snapTo(1.8, 101.0, 6.0, 180.0F, 0.0F);
				karakuri.setNoAi(true);
				l.addFreshEntity(karakuri);

				PaperLanternSpirit spirit = MonsterContent.PAPER_LANTERN_SPIRIT.create(l, EntitySpawnReason.COMMAND);
				check(spirit != null, "PaperLanternSpirit created");
				spirit.snapTo(4.8, 101.5, 6.0, 180.0F, 0.0F);
				spirit.setNoAi(true);
				l.addFreshEntity(spirit);

				check(karakuri.runeboundSpells().size() == 2, "KarakuriSentinel runeboundSpells defined");
				check(spirit.runeboundSpells().size() == 2, "PaperLanternSpirit runeboundSpells defined");

				p(s).setGameMode(GameType.CREATIVE);
				p(s).teleportTo(l, 0.0, 101.0, -1.2, Set.<Relative>of(), 0.0F, 16.0F, false);
				return tea.getUUID();
			});
			c.waitTicks(12);
			shot(c, "shrine_cats_lineup_yuki_odie_freya_kuro");

			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				p(s).teleportTo(l, 0.0, 102.2, 0.5, Set.<Relative>of(), 0.0F, 14.0F, false);
				// Verify Tea Master emerald trade yields Cat Bell Charm.
				p(s).setGameMode(GameType.SURVIVAL);
				SakuraVillager tea = (SakuraVillager) l.getEntity(teaId);
				check(tea != null, "Tea Master present");
				hold(s, new ItemStack(Items.EMERALD, 1));
				tea.mobInteract(p(s), InteractionHand.MAIN_HAND);
				check(p(s).getInventory().hasAnyOf(Set.of(ShrineCatContent.CAT_BELL_CHARM)),
					"Tea Master emerald trade gives Suzu Cat-Bell Charm");
			});
			c.waitTicks(10);
			shot(c, "shrine_villagers_and_mobs");

			// 2. Sakura Village placement & inhabitants verification.
			BoundingBox villageBox = w.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				var village = new SakuraVillagePiece(64, 64, Direction.NORTH);
				village.move(0, 94, 0);
				placePiece(l, village, 101L);
				AABB area = AABB.of(village.getBoundingBox()).inflate(4.0);
				List<SakuraVillager> villagers = l.getEntitiesOfClass(SakuraVillager.class, area);
				List<ShrineCat> cats = l.getEntitiesOfClass(ShrineCat.class, area);
				check(villagers.size() == 2, "Sakura Village has 2 villagers (" + villagers.size() + ")");
				check(cats.size() == 4, "Sakura Village has all 4 Shrine Cats (" + cats.size() + ")");
				p(s).setGameMode(GameType.SPECTATOR);
				var b = village.getBoundingBox();
				p(s).teleportTo(l, b.minX() + b.getXSpan() * 0.5, b.minY() + 26.0, b.minZ() - 12.0,
					Set.<Relative>of(), 0.0F, 34.0F, false);
				return b;
			});
			c.waitTicks(10);
			shot(c, "shrine_sakura_village_overview");

			// 3. Cat Sanctuary 4-floor dungeon: non-lethal yield, floor perks, taming unlock, companion power, persistence.
			BoundingBox sanctuaryBox = w.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				var sanctuary = new CatSanctuaryPiece(192, 96, 64, Direction.NORTH);
				placePiece(l, sanctuary, 202L);
				p(s).setGameMode(GameType.SPECTATOR);
				var b = sanctuary.getBoundingBox();
				p(s).teleportTo(l, b.minX() - 14.0, b.minY() + 28.0, b.minZ() - 10.0,
					Set.<Relative>of(), -45.0F, 24.0F, false);
				return b;
			});
			c.waitTicks(10);
			shot(c, "shrine_cat_sanctuary_pagoda");

			Map<ShrineCatRules.Variant, UUID> guardianIds = w.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				AABB area = AABB.of(sanctuaryBox).inflate(4.0);
				List<ShrineCat> guardians = l.getEntitiesOfClass(ShrineCat.class, area).stream()
					.sorted(Comparator.comparingInt(c2 -> c2.variant().sanctuaryFloor))
					.toList();
				check(guardians.size() == 4, "Cat Sanctuary spawned 4 floor guardians (" + guardians.size() + ")");
				Map<ShrineCatRules.Variant, UUID> ids = new EnumMap<>(ShrineCatRules.Variant.class);
				for (ShrineCat cat : guardians) {
					check(cat.getRole() == ShrineCatRules.ROLE_GUARDIAN, cat.variant().displayName + " starts as guardian");
					ids.put(cat.variant(), cat.getUUID());
				}
				return ids;
			});

			// Yield Floor 1-3 guardians in Survival and verify sanctuary buffs + pre-Kuro tame refusal.
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				player.setGameMode(GameType.SURVIVAL);

				ShrineCat yuki = (ShrineCat) l.getEntity(guardianIds.get(ShrineCatRules.Variant.YUKI));
				check(yuki != null, "Guardian Yuki exists");
				player.teleportTo(l, yuki.getX(), yuki.getY(), yuki.getZ() - 2.0, Set.<Relative>of(), 0.0F, 0.0F, false);

				// Guardian cannot be tamed mid-trial.
				hold(s, new ItemStack(Items.COOKED_SALMON, 4));
				yuki.mobInteract(player, InteractionHand.MAIN_HAND);
				check(!yuki.isTame() && yuki.getRole() == ShrineCatRules.ROLE_GUARDIAN, "Guardian Yuki cannot be tamed mid-fight");

				// Drop below 22% HP -> yields cleanly without dying, restoring full HP and granting Resistance I.
				yuki.hurtServer(l, l.damageSources().playerAttack(player), yuki.getMaxHealth() * 0.85F);
				check(yuki.isAlive() && yuki.getRole() == ShrineCatRules.ROLE_CLEARED, "Yuki yields non-lethally");
				check(yuki.getHealth() >= yuki.getMaxHealth(), "Yuki restores full health on yield");
				check(player.hasEffect(MobEffects.RESISTANCE), "Yuki grants Whisker Ward (Resistance I)");

				// Before Kuro yields, cleared Yuki still refuses taming.
				yuki.mobInteract(player, InteractionHand.MAIN_HAND);
				check(!yuki.isTame(), "Cannot tame cleared Yuki before clearing Kuro on Floor 4");

				// Yield Odie & Freya and verify their sanctuary perks.
				ShrineCat odie = (ShrineCat) l.getEntity(guardianIds.get(ShrineCatRules.Variant.ODIE));
				check(odie != null, "Guardian Odie exists");
				odie.hurtServer(l, l.damageSources().playerAttack(player), odie.getMaxHealth() * 0.85F);
				check(odie.getRole() == ShrineCatRules.ROLE_CLEARED, "Odie yields non-lethally");
				check(player.hasEffect(MobEffects.REGENERATION) && player.hasEffect(MobEffects.FIRE_RESISTANCE),
					"Odie grants Sunbeam Loaf (Regeneration I + Fire Resistance)");

				ShrineCat freya = (ShrineCat) l.getEntity(guardianIds.get(ShrineCatRules.Variant.FREYA));
				check(freya != null, "Guardian Freya exists");
				freya.hurtServer(l, l.damageSources().playerAttack(player), freya.getMaxHealth() * 0.85F);
				check(freya.getRole() == ShrineCatRules.ROLE_CLEARED, "Freya yields non-lethally");
				check(player.hasEffect(MobEffects.ABSORPTION) && player.hasEffect(MobEffects.HASTE),
					"Freya grants Silver-Bell Chime (Absorption I + Haste I)");

				// Yield Kuro on Floor 4 -> unlocks taming for all 4 cats + grants Night-Stalker Grace.
				ShrineCat kuro = (ShrineCat) l.getEntity(guardianIds.get(ShrineCatRules.Variant.KURO));
				check(kuro != null, "Guardian Kuro exists");
				player.teleportTo(l, kuro.getX(), kuro.getY(), kuro.getZ() - 2.0, Set.<Relative>of(), 0.0F, 0.0F, false);
				kuro.hurtServer(l, l.damageSources().playerAttack(player), kuro.getMaxHealth() * 0.85F);
				check(kuro.isAlive() && kuro.getRole() == ShrineCatRules.ROLE_CLEARED, "Kuro yields non-lethally");
				check(player.hasEffect(MobEffects.SPEED) && player.hasEffect(MobEffects.STRENGTH) && player.hasEffect(MobEffects.NIGHT_VISION),
					"Kuro grants Night-Stalker Grace (Speed II, Strength I, Night Vision)");

				// Now tame Freya with cooked salmon and Kuro & Odie with Cat-Bell Charm.
				hold(s, new ItemStack(Items.COOKED_SALMON, 3));
				freya.mobInteract(player, InteractionHand.MAIN_HAND);
				check(freya.isTame() && freya.isOwnedBy(player) && freya.getRole() == ShrineCatRules.ROLE_TAMED, "Freya tames with salmon");
				check(player.getMainHandItem().getCount() == 2, "Taming Freya consumes one salmon");

				hold(s, new ItemStack(ShrineCatContent.CAT_BELL_CHARM, 1));
				kuro.mobInteract(player, InteractionHand.MAIN_HAND);
				odie.mobInteract(player, InteractionHand.MAIN_HAND);
				check(kuro.isTame() && kuro.isOwnedBy(player), "Kuro tames with Suzu Cat-Bell Charm");
				check(odie.isTame() && odie.isOwnedBy(player), "Odie tames with Suzu Cat-Bell Charm");

				// Sneak-interact Freya toggles loaf pose and persists via NBT save/load.
				player.setShiftKeyDown(true);
				freya.mobInteract(player, InteractionHand.MAIN_HAND);
				player.setShiftKeyDown(false);
				check(freya.isLoafing(), "Sneak-interact curls Freya into loaf");

				var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, s.registryAccess());
				freya.saveWithoutId(saved);
				ShrineCat restored = ShrineCatContent.SHRINE_CAT.create(l, EntitySpawnReason.LOAD);
				check(restored != null, "Restored cat created");
				restored.load(TagValueInput.create(ProblemReporter.DISCARDING, s.registryAccess(), saved.buildResult()));
				check(restored.variant() == ShrineCatRules.Variant.FREYA, "Variant preserved in save");
				check(restored.isTame() && restored.isOwnedBy(player), "Owner preserved in save");
				check(restored.getRole() == ShrineCatRules.ROLE_TAMED && restored.isLoafing(), "Role and loaf preserved in save");
				restored.discard();

				// Verify tamed Odie heals an injured owner after companion power cooldown (disable vanilla natural regen via food=14).
				player.removeEffect(MobEffects.REGENERATION);
				player.removeEffect(MobEffects.FIRE_RESISTANCE);
				player.getFoodData().setFoodLevel(14);
				player.setHealth(12.0F);
				odie.teleportTo(player.getX() + 1.5, player.getY(), player.getZ());
			});
			c.waitTicks(95);
			w.getServer().runOnServer(s -> {
				ServerPlayer player = p(s);
				check(player.hasEffect(MobEffects.REGENERATION) && player.hasEffect(MobEffects.FIRE_RESISTANCE),
					"Tamed Odie grants Regeneration and Fire Resistance");
				check(player.getHealth() > 12.0F,
					"Tamed Odie heals owner automatically (" + player.getHealth() + ")");
			});

			// 4. Lantern Bathhouse placement & screenshot.
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				var bath = new LanternBathhousePiece(320, 96, 64, Direction.NORTH);
				placePiece(l, bath, 303L);
				var b = bath.getBoundingBox();
				p(s).setGameMode(GameType.SPECTATOR);
				p(s).teleportTo(l, b.minX() - 10.0, b.minY() + 18.0, b.minZ() - 8.0,
					Set.<Relative>of(), -45.0F, 26.0F, false);
			});
			c.waitTicks(10);
			shot(c, "shrine_lantern_bathhouse_onsen");

			// 5. Ricochet rune bounce count (5 solo vs 2 with Split), Suzu Chime, Ofuda Ward, and Archivist/Serpent anti-cheese.
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				player.setGameMode(GameType.SURVIVAL);
				Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));

				var soloRicochet = SpellCompiler.compile(List.of(Runes.RICOCHET, Runes.SUZU_CHIME));
				check(soloRicochet.warnings().isEmpty(), "Ricochet + Suzu Chime compiles cleanly");
				check(SpellNumbers.ricochetBounces(soloRicochet.root().groups.getFirst()) == 5, "Solo Ricochet bounces 5 times");

				var splitRicochet = SpellCompiler.compile(List.of(Runes.RICOCHET, Runes.SPLIT_MOD, Runes.OFUDA_WARD));
				check(splitRicochet.warnings().isEmpty(), "Ricochet + Split + Ofuda Ward compiles cleanly");
				check(SpellNumbers.ricochetBounces(splitRicochet.root().groups.getFirst()) == 2, "Ricochet + Split caps at 2 bounces");

				for (int x = -6; x <= 6; x++) {
					for (int z = -42; z <= -24; z++) {
						l.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
						l.setBlock(new BlockPos(x, 105, z), Blocks.STONE.defaultBlockState(), 2);
						for (int y = 101; y <= 104; y++) {
							l.setBlock(new BlockPos(x, y, z), (Math.abs(x) == 6 || z == -42 || z == -24)
								? Blocks.STONE.defaultBlockState()
								: Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
				player.teleportTo(l, 0.5, 101.0, -33.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				CastEngine.cast(player, soloRicochet.root());
				CastEngine.cast(player, splitRicochet.root());

				// Archivist arena bounds check: blocks out-of-arena cheese hits > 14.5 blocks away.
				for (int x = -20; x <= 20; x++) {
					for (int z = 120; z <= 170; z++) {
						l.setBlock(new BlockPos(x, 100, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
						for (int y = 101; y <= 106; y++) {
							l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
				BlockPos lectern = new BlockPos(0, 101, 145);
				Archivist archivist = Archivist.rise(l, lectern);
				check(archivist != null, "Archivist rose at lectern");
				player.teleportTo(l, 0.5, 101.0, 162.5, Set.<Relative>of(), 180.0F, 0.0F, false);
				boolean refusedOutside = archivist.hurtServer(l, l.damageSources().playerAttack(player), 12.0F);
				check(!refusedOutside, "Archivist rejects out-of-arena cheese attack at dist > 14.5");
				player.teleportTo(l, 0.5, 101.0, 141.5, Set.<Relative>of(), 0.0F, 0.0F, false);
				boolean allowedInside = archivist.hurtServer(l, l.damageSources().playerAttack(player), 12.0F);
				check(allowedInside, "Archivist accepts normal in-arena attack");

				// SerpentGrovePiece + RootcoilSerpent tree navigation sanity check.
				var grove = new SerpentGrovePiece(-96, 80, 64, Direction.NORTH, 0);
				placePiece(l, grove, 404L);
				List<RootcoilSerpent> serpents = l.getEntitiesOfClass(RootcoilSerpent.class, AABB.of(grove.getBoundingBox()).inflate(2.0));
				check(!serpents.isEmpty(), "SerpentGrovePiece spawned RootcoilSerpent");
				RootcoilSerpent serpent = serpents.getFirst();
				BlockPos trunk = serpent.tree();
				player.teleportTo(l, trunk.getX() + 4.0, trunk.getY(), trunk.getZ() + 0.5, Set.<Relative>of(), 90.0F, 0.0F, false);
			});
			c.waitTicks(95);
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				List<RootcoilSerpent> serpents = l.getEntitiesOfClass(RootcoilSerpent.class, new AABB(-100, 75, 55, -20, 140, 140));
				check(!serpents.isEmpty(), "RootcoilSerpent active in grove");
				check(serpents.getFirst().move() != SerpentRules.DORMANT, "RootcoilSerpent woke cleanly when player entered grove");
			});
		}
	}
}
