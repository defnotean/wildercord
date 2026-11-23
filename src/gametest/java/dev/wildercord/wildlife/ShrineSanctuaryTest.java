package dev.wildercord.wildlife;

import dev.wildercord.aura.world.RootcoilSerpent;
import dev.wildercord.aura.world.SerpentRules;
import dev.wildercord.cast.Archivist;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Reactions;
import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.content.WildercordComponents;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.monster.KarakuriSentinel;
import dev.wildercord.monster.MonsterContent;
import dev.wildercord.monster.PaperLanternSpirit;
import dev.wildercord.monster.WildMonster;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.world.ArchivePiece;
import dev.wildercord.world.dungeons.CatSanctuaryPiece;
import dev.wildercord.world.dungeons.DungeonWards;
import dev.wildercord.world.dungeons.LanternBathhousePiece;
import dev.wildercord.world.dungeons.SerpentGrovePiece;
import dev.wildercord.world.sites.wilds.SakuraCanalTownPiece;
import dev.wildercord.world.sites.wilds.SakuraCastleTownPiece;
import dev.wildercord.world.sites.wilds.SakuraOnsenVillagePiece;
import dev.wildercord.world.sites.wilds.SakuraVillagePiece;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import dev.wildercord.compat.McEntities;

/**
 * Full end-to-end in-game verification suite for:
 * <ol>
 *   <li>Four real Shrine Cats ({@code Yuki}, {@code Odie}, {@code Freya}, {@code Kuro}) standing &amp; loafing silhouettes</li>
 *   <li>Six custom Sakura Villagers ({@code Tea Master}, {@code Shrine Maiden}, {@code Katana Swordsmith},
 *       {@code Lantern Artisan}, {@code Onsen Hostess}, {@code Koi Canal Fisherman}) + {@code KarakuriSentinel} &amp; {@code PaperLanternSpirit} AI</li>
 *   <li>All four Sakura Village archetypes ({@code Sakura Village}, {@code Canal Harbor}, {@code Onsen Resort},
 *       {@code Castle-Town}) and dry-interior assertions</li>
 *   <li>Cat Sanctuary 4-floor pagoda exterior, all 4 floor interiors, wards, non-lethal yields, buffs, taming, all 4 companion powers &amp; persistence</li>
 *   <li>Lantern Bathhouse exterior, onsen interior, sentries, wards, and {@code Fire + Wind} {@code RuneSeal} vault</li>
 *   <li>Ricochet ({@code 5} solo / {@code 2} Split, {@code 15%} decay), {@code Suzu Chime} bounce hit, and {@code Ofuda Ward} thorns retaliation</li>
 *   <li>Archivist ({@code ArchivePiece}) &amp; {@code RootcoilSerpent} ({@code SerpentGrovePiece}) anti-cheese hardening</li>
 *   <li>Blank Rune deepslate recipe and all 9 custom {@code #wildercord:*_variants} + vanilla block-variant crafting recipes</li>
 * </ol>
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
			mc.gui.getChat().clearMessages(false);
			if (!mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
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

	private static void face(LivingEntity entity, float yaw) {
		entity.setYRot(yaw);
		entity.setYBodyRot(yaw);
		entity.setYHeadRot(yaw);
		entity.yRotO = yaw;
		entity.yBodyRotO = yaw;
		entity.yHeadRotO = yaw;
	}

	private static void lookAt(ServerLevel l, ServerPlayer player, Vec3 feetPos, Vec3 targetEye) {
		Vec3 eye = feetPos.add(0, 1.62, 0);
		Vec3 delta = targetEye.subtract(eye);
		double len = Math.max(1.0E-4, delta.length());
		float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
		float pitch = (float) Math.toDegrees(-Math.asin(Math.max(-1.0, Math.min(1.0, delta.y / len))));
		player.teleportTo(l, feetPos.x, feetPos.y, feetPos.z, Set.<RelativeMovement>of(), yaw, pitch);
		player.setYHeadRot(yaw);
	}

	private static Husk spawnHusk(ServerLevel l, double x, double y, double z, boolean noAi) {
		Husk husk = McEntities.create(EntityType.HUSK, l, MobSpawnType.COMMAND);
		check(husk != null, "Husk created");
		husk.moveTo(x, y, z, 0.0F, 0.0F);
		husk.setNoAi(noAi);
		husk.setPersistenceRequired();
		var maxHp = husk.getAttribute(Attributes.MAX_HEALTH);
		if (maxHp != null) {
			maxHp.setBaseValue(60.0);
		}
		var armor = husk.getAttribute(Attributes.ARMOR);
		if (armor != null) {
			armor.setBaseValue(0.0);
		}
		husk.setHealth(60.0F);
		husk.addTag("wildercord.rolled");
		husk.addTag(dev.wildercord.monster.Tempering.TAG);
		l.addFreshEntity(husk);
		return husk;
	}

	private static Item item(String id) {
		Item resolved = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
		check(resolved != Items.AIR, "Registered item exists: " + id);
		return resolved;
	}

	private static void craftCheck(ServerLevel l, MinecraftServer s, int w, int h, List<ItemStack> inputs,
			Item expectedItem, int expectedCount,  String expectedRuneId, String msg) {
		var in = CraftingInput.of(w, h, inputs);
		var matched = s.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, in, l);
		check(matched.isPresent(), msg + " (recipe missing)");
		ItemStack out = matched.get().value().assemble(in, s.registryAccess());
		check(out.is(expectedItem) && out.getCount() == expectedCount,
			msg + " (unexpected output: " + out.getItem() + " x" + out.getCount() + ")");
		if (expectedRuneId != null) {
			check(expectedRuneId.equals(out.get(WildercordComponents.RUNE)),
				msg + " (unexpected rune: " + out.get(WildercordComponents.RUNE) + ")");
		}
	}

	private static BlockPos findSolidInBox(ServerLevel l, BoundingBox box) {
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			BlockState st = l.getBlockState(pos);
			if (!st.isAir() && !st.liquid() && st.getDestroySpeed(l, pos) >= 0.0F) {
				return pos.immutable();
			}
		}
		throw new AssertionError("Expected solid breakable block in warded box " + box);
	}

	private static void assertNoWaterInBox(ServerLevel l, BlockPos a, BlockPos b, String area) {
		BoundingBox box = BoundingBox.fromCorners(a, b);
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			BlockState st = l.getBlockState(pos);
			boolean wet = st.is(Blocks.WATER)
				|| (st.hasProperty(BlockStateProperties.WATERLOGGED) && st.getValue(BlockStateProperties.WATERLOGGED));
			check(!wet, "Unexpected water or waterlogged block " + st + " inside dry interior " + area + " at " + pos);
		}
	}

	@Override
	public void runTest(ClientGameTestContext c) {
		try (var w = c.worldBuilder().create()) {
			c.waitTicks(30);
			w.getServer().runCommand("gamerule doMobSpawning false");
			w.getServer().runCommand("difficulty normal");
			w.getServer().runCommand("time set 6000");
			w.getServer().runCommand("weather clear");

			// =========================================================================
			// Scene 1: Four Real Shrine Cats (Yuki, Odie, Freya, Kuro) Standing & Loaf
			// =========================================================================
			Map<ShrineCatRules.Variant, UUID> lineupIds = new EnumMap<>(ShrineCatRules.Variant.class);
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				for (int x = -10; x <= 10; x++) {
					for (int z = -8; z <= 12; z++) {
						l.setBlock(new BlockPos(x, 100, z), Blocks.CHERRY_PLANKS.defaultBlockState(), 2);
						for (int y = 101; y <= 108; y++) {
							l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
				double[] xs = {-2.1, -0.7, 0.7, 2.1};
				ShrineCatRules.Variant[] order = ShrineCatRules.Variant.values();
				check(order.length == 4, "Four real Shrine Cat variants registered");
				for (int i = 0; i < order.length; i++) {
					ShrineCat cat = McEntities.create(ShrineCatContent.SHRINE_CAT, l, MobSpawnType.COMMAND);
					check(cat != null, "ShrineCat created");
					cat.moveTo(xs[i], 101.0, 3.2, 180.0F, 0.0F);
					cat.configureVillage(order[i], new BlockPos((int) xs[i], 101, 3));
					cat.setLoafing(false);
					check(order[i].sanctuaryFloor == i + 1, "Ordered sanctuary floor " + order[i].displayName);
					face(cat, 180.0F);
					cat.setNoAi(true);
					l.addFreshEntity(cat);
					lineupIds.put(order[i], cat.getUUID());
				}
				p(s).setGameMode(GameType.SPECTATOR);
				p(s).teleportTo(l, 0.0, 100.1, -0.2, Set.<RelativeMovement>of(), 0.0F, 10.0F);
			});
			c.waitTicks(14);
			shot(c, "shrine_cats_lineup_yuki_odie_freya_kuro");

			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				for (var e : lineupIds.entrySet()) {
					if (l.getEntity(e.getValue()) instanceof ShrineCat cat) {
						switch (e.getKey()) {
							case YUKI, ODIE -> cat.setLoafing(true);
							case FREYA -> {
								cat.setOrderedToSit(true);
								cat.setInSittingPose(true);
							}
							case KURO -> face(cat, 125.0F);
						}
					}
				}
				lookAt(l, p(s), new Vec3(-2.5, 100.3, 0.2), new Vec3(0.0, 101.2, 3.2));
			});
			c.waitTicks(12);
			shot(c, "shrine_cats_loaf_and_rear_silhouettes");

			// =========================================================================
			// Scene 2: All 6 Sakura Villagers, Trades, Bow/Immunity & Mobs Active Combat AI
			// =========================================================================
			UUID[] sixVillagerIds = w.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				UUID[] ids = new UUID[SakuraVillager.ROLE_COUNT];
				double[] vx = {-4.5, -2.7, -0.9, 0.9, 2.7, 4.5};
				for (int role = 0; role < SakuraVillager.ROLE_COUNT; role++) {
					SakuraVillager v = McEntities.create(ShrineCatContent.SAKURA_VILLAGER, l, MobSpawnType.COMMAND);
					check(v != null, "SakuraVillager role " + role + " created");
					v.moveTo(vx[role], 101.0, 6.8, 180.0F, 0.0F);
					v.configure(role);
					face(v, 180.0F);
					v.setNoAi(true);
					l.addFreshEntity(v);
					ids[role] = v.getUUID();
				}
				p(s).setGameMode(GameType.SPECTATOR);
				p(s).teleportTo(l, 0.0, 100.3, 0.2, Set.<RelativeMovement>of(), 0.0F, 6.0F);
				return ids;
			});
			c.waitTicks(14);
			shot(c, "shrine_villagers_all_six");

			UUID[] duoIds = w.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				SakuraVillager tea = McEntities.create(ShrineCatContent.SAKURA_VILLAGER, l, MobSpawnType.COMMAND);
				check(tea != null, "Tea Master created");
				tea.moveTo(-3.0, 101.0, 5.5, 180.0F, 0.0F);
				tea.configure(SakuraVillager.TEA_MASTER);
				face(tea, 180.0F);
				tea.setNoAi(true);
				l.addFreshEntity(tea);

				SakuraVillager maiden = McEntities.create(ShrineCatContent.SAKURA_VILLAGER, l, MobSpawnType.COMMAND);
				check(maiden != null, "Shrine Maiden created");
				maiden.moveTo(-1.0, 101.0, 5.5, 180.0F, 0.0F);
				maiden.configure(SakuraVillager.SHRINE_MAIDEN);
				face(maiden, 180.0F);
				maiden.setNoAi(true);
				l.addFreshEntity(maiden);

				for (UUID id : sixVillagerIds) {
					if (l.getEntity(id) instanceof SakuraVillager v) {
						v.setPos(v.getX(), 101.0, -25.0);
					}
				}

				KarakuriSentinel karakuri = McEntities.create(MonsterContent.KARAKURI_SENTINEL, l, MobSpawnType.COMMAND);
				check(karakuri != null, "KarakuriSentinel created");
				karakuri.moveTo(1.0, 101.0, 5.5, 180.0F, 0.0F);
				face(karakuri, 180.0F);
				karakuri.setNoAi(true);
				l.addFreshEntity(karakuri);

				PaperLanternSpirit spirit = McEntities.create(MonsterContent.PAPER_LANTERN_SPIRIT, l, MobSpawnType.COMMAND);
				check(spirit != null, "PaperLanternSpirit created");
				spirit.moveTo(3.0, 101.5, 5.5, 180.0F, 0.0F);
				face(spirit, 180.0F);
				spirit.setNoAi(true);
				l.addFreshEntity(spirit);

				check(karakuri.runeboundSpells().size() == 2, "KarakuriSentinel runeboundSpells defined");
				check(spirit.runeboundSpells().size() == 2, "PaperLanternSpirit runeboundSpells defined");

				p(s).setGameMode(GameType.SPECTATOR);
				p(s).teleportTo(l, 0.0, 100.3, 0.6, Set.<RelativeMovement>of(), 0.0F, 6.0F);
				return new UUID[]{tea.getUUID(), maiden.getUUID()};
			});
			c.waitTicks(14);
			shot(c, "shrine_villagers_and_mobs");

			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				AABB box = new AABB(-8.0, 99.0, -2.0, 8.0, 108.0, 8.0);
				for (KarakuriSentinel k : l.getEntitiesOfClass(KarakuriSentinel.class, box)) {
					k.holdPose(WildMonster.WINDUP);
				}
				for (PaperLanternSpirit sp : l.getEntitiesOfClass(PaperLanternSpirit.class, box)) {
					sp.holdPose(WildMonster.ACTING);
				}
			});
			c.waitTicks(12);
			shot(c, "shrine_villagers_and_mobs_action_poses");

			UUID[] combatMobIds = w.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				player.setGameMode(GameType.SURVIVAL);

				SakuraVillager tea = (SakuraVillager) l.getEntity(duoIds[0]);
				SakuraVillager maiden = (SakuraVillager) l.getEntity(duoIds[1]);
				check(tea != null && maiden != null, "Sakura villagers present");

				// Bow, invulnerability, trades & NBT save/load.
				hold(s, ItemStack.EMPTY);
				tea.mobInteract(player, InteractionHand.MAIN_HAND);
				maiden.mobInteract(player, InteractionHand.MAIN_HAND);
				check(tea.bowTick() == 26 && maiden.bowTick() == 26, "Sakura villagers bow on greeting");
				check(!tea.hurt(l.damageSources().playerAttack(player), 10.0F)
					&& !maiden.hurt(l.damageSources().playerAttack(player), 10.0F),
					"Sakura villagers are protected from player harm");

				hold(s, new ItemStack(Items.EMERALD, 2));
				tea.mobInteract(player, InteractionHand.MAIN_HAND);
				check(player.getMainHandItem().is(Items.EMERALD) && player.getMainHandItem().getCount() == 1,
					"Tea Master trade consumes 1 emerald");
				check(player.getInventory().hasAnyOf(Set.of(ShrineCatContent.CAT_BELL_CHARM)),
					"Tea Master emerald trade gives Suzu Cat-Bell Charm");

				hold(s, new ItemStack(Items.EMERALD, 2));
				maiden.mobInteract(player, InteractionHand.MAIN_HAND);
				check(player.getMainHandItem().is(Items.EMERALD) && player.getMainHandItem().getCount() == 1,
					"Shrine Maiden trade consumes 1 emerald");
				check(player.getInventory().hasAnyOf(Set.of(Items.COOKED_SALMON)),
					"Shrine Maiden emerald trade gives Cooked Salmon");

				SakuraVillager smith = (SakuraVillager) l.getEntity(sixVillagerIds[SakuraVillager.SWORDSMITH]);
				SakuraVillager lanternMaker = (SakuraVillager) l.getEntity(sixVillagerIds[SakuraVillager.LANTERN_MAKER]);
				SakuraVillager onsen = (SakuraVillager) l.getEntity(sixVillagerIds[SakuraVillager.ONSEN_KEEPER]);
				SakuraVillager fisher = (SakuraVillager) l.getEntity(sixVillagerIds[SakuraVillager.FISHERMAN]);
				check(smith != null && lanternMaker != null && onsen != null && fisher != null, "All 4 new Sakura villager roles present");

				hold(s, new ItemStack(Items.EMERALD, 4));
				smith.mobInteract(player, InteractionHand.MAIN_HAND);
				check(player.getInventory().hasAnyOf(Set.of(WildercordItems.BLANK_RUNE)), "Swordsmith trades Blank Runes");
				lanternMaker.mobInteract(player, InteractionHand.MAIN_HAND);
				check(player.getInventory().hasAnyOf(Set.of(Items.LANTERN)), "Lantern Artisan trades Lanterns");
				onsen.mobInteract(player, InteractionHand.MAIN_HAND);
				check(player.getInventory().hasAnyOf(Set.of(Items.GOLDEN_CARROT)), "Onsen Hostess trades Golden Carrots");
				fisher.mobInteract(player, InteractionHand.MAIN_HAND);
				check(player.getInventory().hasAnyOf(Set.of(Items.SALMON)), "Koi Fisherman trades fresh Salmon");

				CompoundTag savedV = new CompoundTag();
				maiden.saveWithoutId(savedV);
				SakuraVillager loadedV = McEntities.create(ShrineCatContent.SAKURA_VILLAGER, l, MobSpawnType.MOB_SUMMONED);
				check(loadedV != null, "Restored villager created");
				loadedV.load(savedV);
				check(loadedV.role() == SakuraVillager.SHRINE_MAIDEN, "SakuraVillager role persisted via NBT");
				loadedV.discard();

				CompoundTag savedF = new CompoundTag();
				fisher.saveWithoutId(savedF);
				SakuraVillager loadedF = McEntities.create(ShrineCatContent.SAKURA_VILLAGER, l, MobSpawnType.MOB_SUMMONED);
				check(loadedF != null, "Restored fisherman created");
				loadedF.load(savedF);
				check(loadedF.role() == SakuraVillager.FISHERMAN, "SakuraVillager fisherman role persisted via NBT");
				loadedF.discard();

				for (UUID id : sixVillagerIds) {
					if (l.getEntity(id) instanceof SakuraVillager v) {
						v.discard();
					}
				}

				// Active combat AI ring for KarakuriSentinel + PaperLanternSpirit at x = -34..-22.
				for (int x = -34; x <= -22; x++) {
					for (int z = -10; z <= 2; z++) {
						boolean wall = x == -34 || x == -22 || z == -10 || z == 2;
						l.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
						l.setBlock(new BlockPos(x, 106, z), Blocks.STONE.defaultBlockState(), 2);
						for (int y = 101; y <= 105; y++) {
							l.setBlock(new BlockPos(x, y, z), wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
				player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 3, true, false));
				player.teleportTo(l, -28.0, 101.0, -4.0, Set.<RelativeMovement>of(), 0.0F, 0.0F);

				KarakuriSentinel k2 = McEntities.create(MonsterContent.KARAKURI_SENTINEL, l, MobSpawnType.COMMAND);
				PaperLanternSpirit p2 = McEntities.create(MonsterContent.PAPER_LANTERN_SPIRIT, l, MobSpawnType.COMMAND);
				check(k2 != null && p2 != null, "Active combat test mobs created");
				k2.moveTo(-25.5, 101.0, -4.0, 90.0F, 0.0F);
				p2.moveTo(-30.5, 101.5, -4.0, -90.0F, 0.0F);
				l.addFreshEntity(k2);
				l.addFreshEntity(p2);
				return new UUID[]{k2.getUUID(), p2.getUUID()};
			});
			c.waitTicks(65);
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				var k2 = (KarakuriSentinel) l.getEntity(combatMobIds[0]);
				var p2 = (PaperLanternSpirit) l.getEntity(combatMobIds[1]);
				check(k2 != null && k2.isAlive() && k2.getTarget() == player, "KarakuriSentinel actively targets Survival player");
				check(p2 != null && p2.isAlive() && p2.getTarget() == player, "PaperLanternSpirit actively targets Survival player");
				k2.discard();
				p2.discard();
				player.removeAllEffects();
				player.setHealth(player.getMaxHealth());
				player.getFoodData().setFoodLevel(20);
			});

			// =========================================================================
			// Scene 3: All 4 Sakura Village Archetypes, Dry Interiors & Residents
			// =========================================================================
			SakuraVillagePiece village = w.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				var piece = new SakuraVillagePiece(64, 64, Direction.SOUTH);
				piece.move(0, 94, 0);
				placePiece(l, piece, 101L);
				AABB area = AABB.of(piece.getBoundingBox()).inflate(4.0);
				List<SakuraVillager> villagers = l.getEntitiesOfClass(SakuraVillager.class, area);
				List<ShrineCat> cats = l.getEntitiesOfClass(ShrineCat.class, area);
				check(villagers.size() == 4, "Sakura Village has 4 villagers (" + villagers.size() + ")");
				check(villagers.stream().anyMatch(v -> v.role() == SakuraVillager.TEA_MASTER)
					&& villagers.stream().anyMatch(v -> v.role() == SakuraVillager.SHRINE_MAIDEN)
					&& villagers.stream().anyMatch(v -> v.role() == SakuraVillager.SWORDSMITH)
					&& villagers.stream().anyMatch(v -> v.role() == SakuraVillager.LANTERN_MAKER),
					"Sakura Village has Tea Master, Shrine Maiden, Swordsmith, and Lantern Artisan");
				check(cats.size() == 4, "Sakura Village has all 4 Shrine Cats (" + cats.size() + ")");
				check(cats.stream().allMatch(cat -> cat.getRole() == ShrineCatRules.ROLE_VILLAGE),
					"Sakura Village cats hold ROLE_VILLAGE");
				check(cats.stream().anyMatch(cat -> cat.variant() == ShrineCatRules.Variant.FREYA && cat.isOrderedToSit()),
					"Sakura Village Freya sits cleanly on her shrine dais");
				assertNoWaterInBox(l, piece.localPosition(6, 2, 23), piece.localPosition(22, 11, 41), "Sakura Village Tea House");
				assertNoWaterInBox(l, piece.localPosition(25, 1, 24), piece.localPosition(29, 4, 30), "Sakura Village Dango Stall");
				p(s).setGameMode(GameType.SPECTATOR);
				var b = piece.getBoundingBox();
				p(s).teleportTo(l, b.minX() + b.getXSpan() * 0.5, b.minY() + 26.0, b.minZ() - 14.0,
					Set.<RelativeMovement>of(), 0.0F, 22.0F);
				return piece;
			});
			c.waitTicks(50);
			shot(c, "shrine_sakura_village_overview");

			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				p(s).setGameMode(GameType.SPECTATOR);
				lookAt(l, p(s), Vec3.atBottomCenterOf(village.localPosition(32, 1, 2)),
					Vec3.atCenterOf(village.localPosition(32, 3, 32)));
			});
			c.waitTicks(12);
			shot(c, "shrine_sakura_village_courtyard");

			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				var canal = new SakuraCanalTownPiece(64, 180, Direction.SOUTH);
				canal.move(0, 94, 0);
				placePiece(l, canal, 102L);
				AABB area = AABB.of(canal.getBoundingBox()).inflate(4.0);
				List<SakuraVillager> villagers = l.getEntitiesOfClass(SakuraVillager.class, area);
				List<ShrineCat> cats = l.getEntitiesOfClass(ShrineCat.class, area);
				check(villagers.size() == 4, "Sakura Canal Harbor has 4 villagers (" + villagers.size() + ")");
				check(cats.size() == 2, "Sakura Canal Harbor has 2 shrine cats (" + cats.size() + ")");
				assertNoWaterInBox(l, canal.localPosition(7, 4, 24), canal.localPosition(20, 7, 39), "Canal Watermill");
				assertNoWaterInBox(l, canal.localPosition(7, 4, 49), canal.localPosition(19, 8, 60), "Canal Lantern Workshop");
				p(s).setGameMode(GameType.SPECTATOR);
				var b = canal.getBoundingBox();
				p(s).teleportTo(l, b.minX() + b.getXSpan() * 0.5, b.minY() + 26.0, b.minZ() - 14.0,
					Set.<RelativeMovement>of(), 0.0F, 22.0F);
			});
			c.waitTicks(45);
			shot(c, "shrine_sakura_canal_town");

			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				var onsen = new SakuraOnsenVillagePiece(180, 180, Direction.SOUTH);
				onsen.move(0, 94, 0);
				placePiece(l, onsen, 103L);
				AABB area = AABB.of(onsen.getBoundingBox()).inflate(4.0);
				List<SakuraVillager> villagers = l.getEntitiesOfClass(SakuraVillager.class, area);
				List<ShrineCat> cats = l.getEntitiesOfClass(ShrineCat.class, area);
				check(villagers.size() == 4, "Sakura Onsen Village has 4 villagers (" + villagers.size() + ")");
				check(cats.size() == 2, "Sakura Onsen Village has 2 shrine cats (" + cats.size() + ")");
				assertNoWaterInBox(l, onsen.localPosition(7, 4, 25), onsen.localPosition(22, 12, 42), "Onsen Ryokan Inn");
				assertNoWaterInBox(l, onsen.localPosition(40, 3, 20), onsen.localPosition(54, 6, 28), "Onsen Ramen Pavilion");
				p(s).setGameMode(GameType.SPECTATOR);
				var b = onsen.getBoundingBox();
				p(s).teleportTo(l, b.minX() + b.getXSpan() * 0.5, b.minY() + 27.0, b.minZ() - 14.0,
					Set.<RelativeMovement>of(), 0.0F, 22.0F);
			});
			c.waitTicks(45);
			shot(c, "shrine_sakura_onsen_village");

			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				var castle = new SakuraCastleTownPiece(296, 180, Direction.SOUTH);
				castle.move(0, 94, 0);
				placePiece(l, castle, 104L);
				AABB area = AABB.of(castle.getBoundingBox()).inflate(4.0);
				List<SakuraVillager> villagers = l.getEntitiesOfClass(SakuraVillager.class, area);
				List<ShrineCat> cats = l.getEntitiesOfClass(ShrineCat.class, area);
				check(villagers.size() == 4, "Sakura Castle-Town has 4 villagers (" + villagers.size() + ")");
				check(cats.size() == 2, "Sakura Castle-Town has 2 shrine cats (" + cats.size() + ")");
				assertNoWaterInBox(l, castle.localPosition(7, 2, 19), castle.localPosition(20, 6, 41), "Castle-Town Kendo Dojo");
				assertNoWaterInBox(l, castle.localPosition(42, 1, 17), castle.localPosition(57, 5, 39), "Castle-Town Market Arcade");
				assertNoWaterInBox(l, castle.localPosition(22, 1, 50), castle.localPosition(38, 5, 60), "Castle-Town Herbalist Cottage");
				p(s).setGameMode(GameType.SPECTATOR);
				var b = castle.getBoundingBox();
				p(s).teleportTo(l, b.minX() + b.getXSpan() * 0.5, b.minY() + 28.0, b.minZ() - 14.0,
					Set.<RelativeMovement>of(), 0.0F, 22.0F);
			});
			c.waitTicks(45);
			shot(c, "shrine_sakura_castle_town");

			// =========================================================================
			// Scene 4: Cat Sanctuary 4-Floor Pagoda (Exterior, 4 Floors, Wards, Trials,
			//          Sanctuary Buffs, Taming, All 4 Companion Powers & Persistence)
			// =========================================================================
			CatSanctuaryPiece sanctuary = w.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				var piece = new CatSanctuaryPiece(192, 96, 64, Direction.SOUTH);
				placePiece(l, piece, 202L);
				p(s).setGameMode(GameType.SPECTATOR);
				var b = piece.getBoundingBox();
				p(s).teleportTo(l, b.minX() - 18.0, b.minY() + 40.0, b.minZ() - 18.0,
					Set.<RelativeMovement>of(), -42.0F, 14.0F);
				return piece;
			});
			c.waitTicks(50);
			shot(c, "shrine_cat_sanctuary_pagoda");

			Map<ShrineCatRules.Variant, UUID> guardianIds = w.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				AABB area = AABB.of(sanctuary.getBoundingBox()).inflate(4.0);
				List<ShrineCat> guardians = l.getEntitiesOfClass(ShrineCat.class, area).stream()
					.sorted(Comparator.comparingInt(c2 -> c2.variant().sanctuaryFloor))
					.toList();
				check(guardians.size() == 4, "Cat Sanctuary spawned 4 floor guardians (" + guardians.size() + ")");
				Map<ShrineCatRules.Variant, UUID> ids = new EnumMap<>(ShrineCatRules.Variant.class);
				for (ShrineCat cat : guardians) {
					check(cat.getRole() == ShrineCatRules.ROLE_GUARDIAN, cat.variant().displayName + " starts as guardian");
					check(Math.abs(cat.getMaxHealth() - cat.variant().trialHealth) < 0.05F,
						cat.variant().displayName + " trialHealth set");
					ids.put(cat.variant(), cat.getUUID());
				}
				List<SakuraVillager> keepers = l.getEntitiesOfClass(SakuraVillager.class, area);
				check(keepers.size() == 1 && keepers.getFirst().role() == SakuraVillager.TEA_MASTER,
					"Cat Sanctuary has welcoming entry Tea Master");
				int spirits = l.getEntitiesOfClass(PaperLanternSpirit.class, area).size();
				int sentinels = l.getEntitiesOfClass(KarakuriSentinel.class, area).size();
				check(spirits == 18 && sentinels == 18,
					"Cat Sanctuary spawned 18 PaperLanternSpirits & 18 KarakuriSentinels (" + spirits + "/" + sentinels + ")");

				// Verify Survival ward refuses breaking Cat Sanctuary walls & Floor 2 tatami is 100% dry.
				assertNoWaterInBox(l, sanctuary.localPosition(18, CatSanctuaryPiece.F2 + 1, 30),
					sanctuary.localPosition(38, CatSanctuaryPiece.F2 + 4, 54), "Cat Sanctuary Floor 2 Odie tatami hall");
				ServerPlayer player = p(s);
				player.setGameMode(GameType.SURVIVAL);
				BlockPos wall = sanctuary.localPosition(6, CatSanctuaryPiece.F1 + 2, 30);
				check(!DungeonWards.admits(l, player, wall, l.getBlockState(wall), false),
					"Cat Sanctuary pagoda wall is protected by DungeonWards in Survival");
				player.setGameMode(GameType.SPECTATOR);
				return ids;
			});

			// Capture interior viewpoints of all 4 Cat Sanctuary floors before yielding guardians.
			ShrineCatRules.Variant[] floorVariants = {
				ShrineCatRules.Variant.YUKI, ShrineCatRules.Variant.ODIE,
				ShrineCatRules.Variant.FREYA, ShrineCatRules.Variant.KURO
			};
			int[] camFloorY = {
				CatSanctuaryPiece.F1 + 1, CatSanctuaryPiece.F2 + 1,
				CatSanctuaryPiece.F3 + 1, CatSanctuaryPiece.F4 + 1
			};
			int[] camFloorZ = {21, 23, 25, 27};
			String[] floorShots = {
				"shrine_cat_sanctuary_floor1_yuki",
				"shrine_cat_sanctuary_floor2_odie",
				"shrine_cat_sanctuary_floor3_freya",
				"shrine_cat_sanctuary_floor4_kuro"
			};
			for (int i = 0; i < 4; i++) {
				int idx = i;
				w.getServer().runOnServer(s -> {
					ServerLevel l = s.overworld();
					ServerPlayer player = p(s);
					player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0, true, false));
					ShrineCat cat = (ShrineCat) l.getEntity(guardianIds.get(floorVariants[idx]));
					check(cat != null, "Floor guardian present for interior view: " + floorVariants[idx].displayName);
					lookAt(l, player, Vec3.atBottomCenterOf(sanctuary.localPosition(28, camFloorY[idx], camFloorZ[idx])),
						cat.getEyePosition());
				});
				c.waitTicks(12);
				shot(c, floorShots[i]);
			}

			// Verify Floor Progression Seal prevents skipping Floor 1 while Yuki is undefeated.
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				ShrineCat odie = (ShrineCat) l.getEntity(guardianIds.get(ShrineCatRules.Variant.ODIE));
				check(odie != null, "Odie present for floor seal check");
				player.removeAllEffects();
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(l, odie.getX(), odie.getY(), odie.getZ(), Set.<RelativeMovement>of(), 0.0F, 0.0F);
			});
			c.waitTicks(4);

			// Verify Guardian Shrine Aegis & non-lethal guardian yields, floor perks, taming unlock, and tame all 4 cats.
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				ShrineCat yuki = (ShrineCat) l.getEntity(guardianIds.get(ShrineCatRules.Variant.YUKI));
				ShrineCat odie = (ShrineCat) l.getEntity(guardianIds.get(ShrineCatRules.Variant.ODIE));
				ShrineCat freya = (ShrineCat) l.getEntity(guardianIds.get(ShrineCatRules.Variant.FREYA));
				ShrineCat kuro = (ShrineCat) l.getEntity(guardianIds.get(ShrineCatRules.Variant.KURO));
				check(yuki != null && odie != null && freya != null && kuro != null, "All 4 sanctuary cats present");
				check(player.getY() < yuki.getY() + 4.0,
					"Floor Progression Seal returned player trying to skip Floor 1 (y=" + player.getY() + ")");

				player.teleportTo(l, yuki.getX(), yuki.getY(), yuki.getZ() - 2.0, Set.<RelativeMovement>of(), 0.0F, 0.0F);
				float preAegisHp = yuki.getHealth();
				yuki.hurt(l.damageSources().playerAttack(player), 10.0F);
				check(yuki.getHealth() > preAegisHp - 3.5F,
					"Guardian Shrine Aegis mitigates normal damage while Floor 1 sentries live (" + yuki.getHealth() + "/" + preAegisHp + ")");
				hold(s, new ItemStack(Items.COOKED_SALMON, 4));
				yuki.mobInteract(player, InteractionHand.MAIN_HAND);
				check(!yuki.isTame() && yuki.getRole() == ShrineCatRules.ROLE_GUARDIAN, "Guardian Yuki refuses taming mid-fight");

				yuki.hurt(l.damageSources().playerAttack(player), yuki.getMaxHealth() * 0.85F);
				check(yuki.isAlive() && yuki.getRole() == ShrineCatRules.ROLE_CLEARED, "Yuki yields non-lethally");
				check(yuki.getHealth() >= yuki.getMaxHealth(), "Yuki restores full health on yield");
				check(player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "Yuki grants Whisker Ward (Resistance I)");

				yuki.mobInteract(player, InteractionHand.MAIN_HAND);
				check(!yuki.isTame(), "Cannot tame cleared Yuki before clearing Kuro on Floor 4");

				odie.hurt(l.damageSources().playerAttack(player), odie.getMaxHealth() * 0.85F);
				check(odie.getRole() == ShrineCatRules.ROLE_CLEARED, "Odie yields non-lethally");
				check(player.hasEffect(MobEffects.REGENERATION) && player.hasEffect(MobEffects.FIRE_RESISTANCE),
					"Odie grants Sunbeam Loaf (Regeneration I + Fire Resistance)");

				freya.hurt(l.damageSources().playerAttack(player), freya.getMaxHealth() * 0.85F);
				check(freya.getRole() == ShrineCatRules.ROLE_CLEARED, "Freya yields non-lethally");
				check(player.hasEffect(MobEffects.ABSORPTION) && player.hasEffect(MobEffects.DIG_SPEED),
					"Freya grants Silver-Bell Chime (Absorption I + Haste I)");

				player.teleportTo(l, kuro.getX(), kuro.getY(), kuro.getZ() - 2.0, Set.<RelativeMovement>of(), 0.0F, 0.0F);
				kuro.hurt(l.damageSources().playerAttack(player), kuro.getMaxHealth() * 0.85F);
				check(kuro.isAlive() && kuro.getRole() == ShrineCatRules.ROLE_CLEARED, "Kuro yields non-lethally");
				check(player.hasEffect(MobEffects.MOVEMENT_SPEED) && player.hasEffect(MobEffects.DAMAGE_BOOST) && player.hasEffect(MobEffects.NIGHT_VISION),
					"Kuro grants Night-Stalker Grace (Speed II, Strength I, Night Vision)");

				// Now tame all 4 Shrine Cats (Freya & Yuki with salmon; Kuro & Odie with Suzu Cat-Bell Charm).
				hold(s, new ItemStack(Items.COOKED_SALMON, 4));
				freya.mobInteract(player, InteractionHand.MAIN_HAND);
				yuki.mobInteract(player, InteractionHand.MAIN_HAND);
				check(freya.isTame() && freya.isOwnedBy(player) && freya.getRole() == ShrineCatRules.ROLE_TAMED, "Freya tames with salmon");
				check(yuki.isTame() && yuki.isOwnedBy(player) && yuki.getRole() == ShrineCatRules.ROLE_TAMED, "Yuki tames with salmon");
				check(player.getMainHandItem().getCount() == 2, "Taming Freya & Yuki consumes two salmon");

				hold(s, new ItemStack(ShrineCatContent.CAT_BELL_CHARM, 1));
				kuro.mobInteract(player, InteractionHand.MAIN_HAND);
				odie.mobInteract(player, InteractionHand.MAIN_HAND);
				check(kuro.isTame() && kuro.isOwnedBy(player) && kuro.getRole() == ShrineCatRules.ROLE_TAMED, "Kuro tames with Suzu Cat-Bell Charm");
				check(odie.isTame() && odie.isOwnedBy(player) && odie.getRole() == ShrineCatRules.ROLE_TAMED, "Odie tames with Suzu Cat-Bell Charm");

				// Build clean 19x19 test terrace at (180, 110, 0) to verify all 4 companion powers independently without i-frame overlap.
				for (int x = 171; x <= 189; x++) {
					for (int z = -9; z <= 9; z++) {
						l.setBlock(new BlockPos(x, 110, z), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
						for (int y = 111; y <= 116; y++) {
							l.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
			});

			UUID[] powerHusks = w.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				player.setGameMode(GameType.SURVIVAL);
				player.removeAllEffects();
				player.getFoodData().setFoodLevel(14);
				player.setHealth(12.0F);
				player.teleportTo(l, 180.5, 111.0, 0.5, Set.<RelativeMovement>of(), 0.0F, 0.0F);

				ShrineCat yuki = (ShrineCat) l.getEntity(guardianIds.get(ShrineCatRules.Variant.YUKI));
				ShrineCat odie = (ShrineCat) l.getEntity(guardianIds.get(ShrineCatRules.Variant.ODIE));
				ShrineCat freya = (ShrineCat) l.getEntity(guardianIds.get(ShrineCatRules.Variant.FREYA));
				ShrineCat kuro = (ShrineCat) l.getEntity(guardianIds.get(ShrineCatRules.Variant.KURO));
				check(yuki != null && odie != null && freya != null && kuro != null, "Tamed cats present for power test");

				Husk huskA = spawnHusk(l, 176.0, 111.0, 0.5, true);
				Husk huskB = spawnHusk(l, 185.0, 111.0, 0.5, true);
				Husk huskC = spawnHusk(l, 180.5, 111.0, 5.5, true);

				for (ShrineCat cat : new ShrineCat[]{yuki, kuro, freya, odie}) {
					cat.setTarget(null);
					cat.setLoafing(false);
					cat.setOrderedToSit(true);
					cat.setInSittingPose(true);
				}
				yuki.teleportTo(177.5, 111.0, 0.5);
				kuro.teleportTo(183.5, 111.0, 0.5);
				freya.teleportTo(180.5, 111.0, 4.0);
				odie.teleportTo(180.5, 111.0, -1.0);

				player.setLastHurtByMob(huskA);
				player.setLastHurtMob(huskB);
				return new UUID[]{huskA.getUUID(), huskB.getUUID(), huskC.getUUID()};
			});
			for (int step = 0; step < 60; step++) {
				c.waitTicks(10);
				boolean ready = w.getServer().computeOnServer(s -> {
					ServerLevel l = s.overworld();
					ServerPlayer player = p(s);
					var huskA = (LivingEntity) l.getEntity(powerHusks[0]);
					var huskB = (LivingEntity) l.getEntity(powerHusks[1]);
					var huskC = (LivingEntity) l.getEntity(powerHusks[2]);
					if (huskA != null) {
						// Companion powers run on a 100-tick cooldown; keep the attacker fresh until they fire.
						player.setLastHurtByMob(huskA);
					}
					if (huskB != null) {
						player.setLastHurtMob(huskB);
					}
					return player.hasEffect(MobEffects.DAMAGE_RESISTANCE)
						&& huskA != null && huskA.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && huskA.getHealth() < huskA.getMaxHealth()
						&& player.hasEffect(MobEffects.REGENERATION) && player.hasEffect(MobEffects.FIRE_RESISTANCE)
						&& player.getHealth() > 12.0F
						&& player.hasEffect(MobEffects.ABSORPTION)
						&& huskC != null && huskC.getHealth() < huskC.getMaxHealth()
						&& player.hasEffect(MobEffects.MOVEMENT_SPEED) && player.hasEffect(MobEffects.DAMAGE_BOOST)
						&& huskB != null && huskB.getHealth() < huskB.getMaxHealth();
				});
				if (ready) {
					break;
				}
			}
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				var huskA = (LivingEntity) l.getEntity(powerHusks[0]);
				var huskB = (LivingEntity) l.getEntity(powerHusks[1]);
				var huskC = (LivingEntity) l.getEntity(powerHusks[2]);
				check(huskA != null && huskB != null && huskC != null, "Companion power targets present");

				// 1. Yuki: Resistance + Frost-Whisker Counter on attacker.
				check(player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "Tamed Yuki grants Resistance");
				check(huskA.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && huskA.getHealth() < huskA.getMaxHealth(),
					"Tamed Yuki slows and damages owner's attacker");

				// 2. Odie: Regeneration, Fire Resistance & heal.
				check(player.hasEffect(MobEffects.REGENERATION) && player.hasEffect(MobEffects.FIRE_RESISTANCE),
					"Tamed Odie grants Regeneration and Fire Resistance");
				check(player.getHealth() > 12.0F, "Tamed Odie heals owner automatically (" + player.getHealth() + ")");

				// 3. Freya: Absorption & Suzu bell AoE knockback/damage on nearby monster.
				check(player.hasEffect(MobEffects.ABSORPTION), "Tamed Freya grants Absorption");
				check(huskC.getHealth() < huskC.getMaxHealth(), "Tamed Freya Suzu Resonance damages nearby monster");

				// 4. Kuro: Speed II, Strength & Shadow Pounce on prey.
				check(player.hasEffect(MobEffects.MOVEMENT_SPEED) && player.hasEffect(MobEffects.DAMAGE_BOOST),
					"Tamed Kuro grants Speed and Strength");
				check(huskB.getHealth() < huskB.getMaxHealth(), "Tamed Kuro strikes owner's prey");

				// Heal hurt tamed Freya with salmon, verify tamed immortality against thorns/lethal hits, toggle loaf, and NBT save/load.
				ShrineCat freya = (ShrineCat) l.getEntity(guardianIds.get(ShrineCatRules.Variant.FREYA));
				check(freya != null, "Freya present for heal & NBT test");
				freya.setHealth(18.0F);
				hold(s, new ItemStack(Items.COOKED_SALMON, 2));
				freya.mobInteract(player, InteractionHand.MAIN_HAND);
				check(freya.getHealth() >= 24.0F && player.getMainHandItem().getCount() == 1,
					"Feeding cooked salmon heals hurt tamed ShrineCat (+6 HP)");
				freya.hurt(l.damageSources().thorns(player), 500.0F);
				freya.hurt(l.damageSources().magic(), 500.0F);
				check(freya.isAlive() && freya.getHealth() >= 10.0F,
					"Tamed Freya cannot die from thorns or lethal non-/kill damage (" + freya.getHealth() + ")");

				hold(s, ItemStack.EMPTY);
				player.setShiftKeyDown(true);
				freya.mobInteract(player, InteractionHand.MAIN_HAND);
				player.setShiftKeyDown(false);
				check(freya.isLoafing(), "Sneak-interact curls Freya into loaf");

				CompoundTag saved = new CompoundTag();
				freya.saveWithoutId(saved);
				ShrineCat restored = McEntities.create(ShrineCatContent.SHRINE_CAT, l, MobSpawnType.MOB_SUMMONED);
				check(restored != null, "Restored cat created");
				restored.load(saved);
				check(restored.variant() == ShrineCatRules.Variant.FREYA, "Variant preserved in save");
				check(restored.isTame() && restored.isOwnedBy(player), "Owner preserved in save");
				check(restored.getRole() == ShrineCatRules.ROLE_TAMED && restored.isLoafing(), "Role and loaf preserved in save");
				restored.discard();

				huskA.discard();
				huskB.discard();
				huskC.discard();
				player.removeAllEffects();
				player.setHealth(player.getMaxHealth());
				player.getFoodData().setFoodLevel(20);
			});

			// =========================================================================
			// Scene 5: Lantern Bathhouse Exterior, Interior Hall, Sentries & RuneSeal
			// =========================================================================
			LanternBathhousePiece bath = w.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				var piece = new LanternBathhousePiece(320, 96, 64, Direction.SOUTH);
				placePiece(l, piece, 303L);
				var b = piece.getBoundingBox();
				p(s).setGameMode(GameType.SPECTATOR);
				p(s).teleportTo(l, b.minX() - 8.0, b.minY() + 18.0, b.minZ() - 10.0,
					Set.<RelativeMovement>of(), -40.0F, 16.0F);
				return piece;
			});
			c.waitTicks(50);
			shot(c, "shrine_lantern_bathhouse_onsen");

			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				AABB area = AABB.of(bath.getBoundingBox()).inflate(2.0);
				int spirits = l.getEntitiesOfClass(PaperLanternSpirit.class, area).size();
				int sentinels = l.getEntitiesOfClass(KarakuriSentinel.class, area).size();
				check(spirits == 7 && sentinels == 7,
					"Lantern Bathhouse spawned 7 PaperLanternSpirits & 7 KarakuriSentinels (" + spirits + "/" + sentinels + ")");

				BlockState sealA = l.getBlockState(bath.localPosition(18, LanternBathhousePiece.F + 2, 34));
				BlockState sealB = l.getBlockState(bath.localPosition(19, LanternBathhousePiece.F + 2, 34));
				check(sealA.is(WildercordBlocks.RUNE_SEAL) && sealB.is(WildercordBlocks.RUNE_SEAL),
					"Lantern Bathhouse vault guarded by RuneSeal door");
				Set<RuneSealBlock.Element> elements = Set.of(
					sealA.getValue(RuneSealBlock.ELEMENT),
					sealB.getValue(RuneSealBlock.ELEMENT));
				check(elements.equals(Set.of(RuneSealBlock.Element.FIRE, RuneSealBlock.Element.WIND)),
					"Lantern Bathhouse RuneSeal requires Fire + Wind (" + elements + ")");

				player.setGameMode(GameType.SURVIVAL);
				BlockPos wall = bath.localPosition(4, LanternBathhousePiece.F + 2, 20);
				check(!DungeonWards.admits(l, player, wall, l.getBlockState(wall), false),
					"Lantern Bathhouse wall is protected by DungeonWards in Survival");

				player.setGameMode(GameType.SPECTATOR);
				lookAt(l, player, Vec3.atBottomCenterOf(bath.localPosition(19, LanternBathhousePiece.F + 2, 11)),
					Vec3.atCenterOf(bath.localPosition(19, LanternBathhousePiece.F + 2, 34)));
			});
			c.waitTicks(12);
			shot(c, "shrine_lantern_bathhouse_interior");

			// =========================================================================
			// Scene 6: Ricochet (5/2 + 15% Decay), Suzu Chime Wall-Bounce Hit & Ofuda Ward
			// =========================================================================
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				loadArea(l, new BoundingBox(-16, 96, -48, 16, 112, -16));
				for (int x = -5; x <= 5; x++) {
					for (int z = -44; z <= -24; z++) {
						l.setBlock(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState(), 2);
						l.setBlock(new BlockPos(x, 106, z), Blocks.AIR.defaultBlockState(), 2);
						for (int y = 101; y <= 105; y++) {
							boolean wall = Math.abs(x) == 5 || z == -44 || z == -24 || (z == -35 && x >= 0 && x <= 4);
							l.setBlock(new BlockPos(x, y, z), wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
						}
					}
				}
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(l, 1.0, 101.0, -30.5, Set.<RelativeMovement>of(), 125.0F, -11.0F);
				player.setYHeadRot(125.0F);
			});
			c.waitTicks(30);

			UUID[] scene6Husks = w.getServer().computeOnServer(s -> {
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

				check(Math.abs(SpellNumbers.ricochetBounceScale(0) - 1.0) < 1e-6
					&& Math.abs(SpellNumbers.ricochetBounceScale(1) - 0.85) < 1e-6
					&& Math.abs(SpellNumbers.ricochetBounceScale(3) - 0.55) < 1e-6
					&& Math.abs(SpellNumbers.ricochetBounceScale(5) - 0.25) < 1e-6,
					"Ricochet loses 15% power per bounce down to 0.25 floor");

				Husk bounceTarget = spawnHusk(l, 1.56, 101.0, -37.89, true);
				Husk thornsAttacker = spawnHusk(l, 2.0, 101.0, -29.0, true);

				player.teleportTo(l, 1.0, 101.0, -30.5, Set.<RelativeMovement>of(), 125.0F, -11.0F);
				player.setYRot(125.0F);
				player.setXRot(-11.0F);
				player.setYHeadRot(125.0F);
				CastEngine.cast(player, soloRicochet.root());
				CastEngine.cast(player, SpellCompiler.compile(List.of(Runes.SELF, Runes.OFUDA_WARD)).root());
				return new UUID[]{bounceTarget.getUUID(), thornsAttacker.getUUID()};
			});
			c.waitTicks(6);
			shot(c, "shrine_ricochet_and_runes_cast");
			c.waitTicks(24);
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				var bounceTarget = (LivingEntity) l.getEntity(scene6Husks[0]);
				var thornsAttacker = (LivingEntity) l.getEntity(scene6Husks[1]);
				check(bounceTarget != null && thornsAttacker != null, "Scene 6 targets present");
				check(bounceTarget.getHealth() < bounceTarget.getMaxHealth(),
					"Ricochet + Suzu Chime bounced around stone baffle and damaged target (hp="
						+ bounceTarget.getHealth() + "/" + bounceTarget.getMaxHealth() + ")");
				check(Reactions.has(bounceTarget, Reactions.Mark.WINDSWEPT),
					"Suzu Chime applied WINDSWEPT mark");
				check(player.hasEffect(MobEffects.ABSORPTION), "Ofuda Ward granted Absorption I");

				Effects.readyToHurt(player);
				boolean hurt = player.hurt(l.damageSources().mobAttack(thornsAttacker), 2.0F);
				check(hurt, "Hostile strike registered against Ofuda Ward");
			});
			c.waitTicks(6);
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				var bounceTarget = l.getEntity(scene6Husks[0]);
				var thornsAttacker = (LivingEntity) l.getEntity(scene6Husks[1]);
				check(thornsAttacker != null && thornsAttacker.getHealth() <= thornsAttacker.getMaxHealth() - 2.5F,
					"Ofuda Ward returned 3.0 arcane thorns damage to attacker (hp="
						+ (thornsAttacker == null ? "null" : thornsAttacker.getHealth() + "/" + thornsAttacker.getMaxHealth()) + ")");
				if (bounceTarget != null) bounceTarget.discard();
				thornsAttacker.discard();
				player.removeAllEffects();
				player.setHealth(player.getMaxHealth());
			});

			// =========================================================================
			// Scene 7: Archivist (ArchivePiece) & RootcoilSerpent (SerpentGrovePiece)
			//          Anti-Cheese Verification + Screenshots
			// =========================================================================
			ArchivePiece archive = w.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				var piece = new ArchivePiece(-240, 74, 120, Direction.SOUTH);
				placePiece(l, piece, 505L);
				player.setGameMode(GameType.SPECTATOR);
				player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
				player.teleportTo(l, -230.0, 80.0, 130.0, Set.<RelativeMovement>of(), 0.0F, 0.0F);
				return piece;
			});
			c.waitTicks(30);

			UUID archivistId = w.getServer().computeOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				player.setGameMode(GameType.SURVIVAL);

				List<BoundingBox> wardBoxes = archive.wardedBoxes();
				check(wardBoxes.size() == 5, "ArchivePiece declares all 5 warded boxes (Shelves, Braziers, Corridor, Arena, Vault)");
				for (int i = 0; i < wardBoxes.size(); i++) {
					BlockPos solid = findSolidInBox(l, wardBoxes.get(i));
					check(!DungeonWards.admits(l, player, solid, l.getBlockState(solid), false),
						"ArchivePiece ward box " + i + " refuses Survival breaking");
				}
				// Verify waterlogged stair inside Archive arena ward cannot be broken without being player-placed.
				BoundingBox arenaBox = wardBoxes.get(3);
				BlockPos wlPos = new BlockPos(arenaBox.minX() + 3, arenaBox.minY() + 2, arenaBox.minZ() + 3);
				BlockState wlState = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
				l.setBlock(wlPos, wlState, 2);
				check(!DungeonWards.admits(l, player, wlPos, l.getBlockState(wlPos), false),
					"Waterlogged stair inside Archive ward cannot bypass DungeonWards");

				// Locate actual Archive lectern inside Arena box and summon Archivist.
				BlockPos lectern = null;
				for (BlockPos pos : BlockPos.betweenClosed(arenaBox.minX(), arenaBox.minY(), arenaBox.minZ(),
						arenaBox.maxX(), arenaBox.maxY(), arenaBox.maxZ())) {
					if (l.getBlockState(pos).is(WildercordBlocks.ARCHIVE_LECTERN)) {
						lectern = pos.immutable();
						break;
					}
				}
				check(lectern != null, "ArchivePiece placed ARCHIVE_LECTERN in Arena");
				for (Archivist existing : l.getEntitiesOfClass(Archivist.class, AABB.of(arenaBox).inflate(8.0))) {
					existing.discard();
				}
				Archivist archivist = Archivist.rise(l, lectern);
				check(archivist != null, "Archivist rose at Archive lectern");
				archivist.setNoAi(true);

				// (a) Dist > 14.5 rejected.
				player.teleportTo(l, lectern.getX() + 0.5, lectern.getY() + 1.0, lectern.getZ() + 17.5, Set.<RelativeMovement>of(), 180.0F, 0.0F);
				check(!archivist.hurt(l.damageSources().playerAttack(player), 12.0F),
					"Archivist rejects out-of-arena attack at dist > 14.5");

				// (b) Vertical dy > 16.0 rejected.
				player.teleportTo(l, lectern.getX() + 0.5, lectern.getY() + 18.0, lectern.getZ() + 0.5, Set.<RelativeMovement>of(), 0.0F, 90.0F);
				check(!archivist.hurt(l.damageSources().playerAttack(player), 12.0F),
					"Archivist rejects overhead roof attack at dy > 16.0");

				// (c) Wall-occluded outer-rim peephole cheese (dist ~ 13.1 > 12.6 with !hasLineOfSight) rejected.
				for (int dy = 1; dy <= 4; dy++) {
					l.setBlock(lectern.offset(11, dy, 0), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
					l.setBlock(lectern.offset(13, dy, 0), Blocks.AIR.defaultBlockState(), 2);
				}
				player.teleportTo(l, lectern.getX() + 13.6, lectern.getY() + 1.0, lectern.getZ() + 0.5, Set.<RelativeMovement>of(), 90.0F, 0.0F);
				check(!archivist.hasLineOfSight(player), "Outer rim wall blocks Archivist line of sight");
				check(!archivist.hurt(l.damageSources().playerAttack(player), 12.0F),
					"Archivist rejects wall-hole cheese attack at dist > 12.6 without line of sight");

				// (d) Direct in-arena fight hit accepted.
				player.teleportTo(l, lectern.getX() + 0.5, lectern.getY() + 1.0, lectern.getZ() - 4.0, Set.<RelativeMovement>of(), 0.0F, 0.0F);
				check(archivist.hurt(l.damageSources().playerAttack(player), 12.0F),
					"Archivist accepts normal in-arena attack");

				player.setGameMode(GameType.SPECTATOR);
				lookAt(l, player, Vec3.atBottomCenterOf(lectern.offset(0, 2, -8)), archivist.getEyePosition());
				return archivist.getUUID();
			});
			c.waitTicks(20);
			shot(c, "shrine_archivist_anti_cheese");
			w.getServer().runOnServer(s -> {
				if (s.overworld().getEntity(archivistId) instanceof Archivist a) {
					a.discard();
				}
				p(s).removeAllEffects();
			});

			// RootcoilSerpent tree navigation & uncoil sanity check.
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				loadArea(l, new BoundingBox(-112, 70, 48, -48, 120, 112));
				player.setGameMode(GameType.SPECTATOR);
				player.teleportTo(l, -80.0, 88.0, 80.0, Set.<RelativeMovement>of(), 180.0F, 15.0F);
			});
			c.waitTicks(25);
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				player.setGameMode(GameType.SURVIVAL);
				var grove = new SerpentGrovePiece(-96, 80, 64, Direction.NORTH, 0);
				placePiece(l, grove, 404L);
				List<RootcoilSerpent> serpents = l.getEntitiesOfClass(RootcoilSerpent.class, AABB.of(grove.getBoundingBox()).inflate(2.0));
				check(!serpents.isEmpty(), "SerpentGrovePiece spawned RootcoilSerpent");
				RootcoilSerpent serpent = serpents.getFirst();
				BlockPos trunk = serpent.tree();
				player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 3, true, false));
				player.teleportTo(l, trunk.getX() + 4.5, trunk.getY(), trunk.getZ() + 0.5, Set.<RelativeMovement>of(), 90.0F, 0.0F);
			});
			c.waitTicks(95);
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				ServerPlayer player = p(s);
				List<RootcoilSerpent> serpents = l.getEntitiesOfClass(RootcoilSerpent.class, new AABB(-100, 75, 55, -20, 140, 140));
				check(!serpents.isEmpty(), "RootcoilSerpent active in grove");
				RootcoilSerpent serpent = serpents.getFirst();
				check(serpent.move() != SerpentRules.DORMANT, "RootcoilSerpent woke cleanly when player entered grove");
				BlockPos trunk = serpent.tree();
				player.removeAllEffects();
				player.setHealth(player.getMaxHealth());
				player.setGameMode(GameType.SPECTATOR);
				lookAt(l, player, Vec3.atBottomCenterOf(trunk.offset(10, 5, 10)), serpent.getEyePosition());
			});
			c.waitTicks(20);
			shot(c, "shrine_serpent_grove_anti_cheese");

			// =========================================================================
			// Scene 8: Blank Rune Deepslate + All 9 Custom #wildercord:*_variants Tags
			// =========================================================================
			w.getServer().runOnServer(s -> {
				ServerLevel l = s.overworld();
				Item lapis = Items.LAPIS_LAZULI;

				// Blank Rune: deepslate, cobbled deepslate, bricks, tiles, blackstone, mossy cobble, mixed.
				for (Item[] cross : new Item[][]{
					{Items.COBBLED_DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.COBBLED_DEEPSLATE},
					{Items.DEEPSLATE, Items.DEEPSLATE, Items.DEEPSLATE, Items.DEEPSLATE},
					{Items.POLISHED_DEEPSLATE, Items.DEEPSLATE_BRICKS, Items.DEEPSLATE_TILES, Items.MOSSY_COBBLESTONE},
					{Items.COBBLESTONE, Items.COBBLED_DEEPSLATE, Items.STONE, Items.BLACKSTONE}
				}) {
					craftCheck(l, s, 3, 3, List.of(
						ItemStack.EMPTY, new ItemStack(cross[0]), ItemStack.EMPTY,
						new ItemStack(cross[1]), new ItemStack(lapis), new ItemStack(cross[2]),
						ItemStack.EMPTY, new ItemStack(cross[3]), ItemStack.EMPTY
					), WildercordItems.BLANK_RUNE, 4, null, "Blank Rune variant " + cross[0]);
				}

				// 1. #wildercord:cobblestone_variants -> rune_brace
				craftCheck(l, s, 3, 1, List.of(
					new ItemStack(WildercordItems.BLANK_RUNE), new ItemStack(Items.COBBLED_DEEPSLATE), new ItemStack(Items.IRON_INGOT)
				), WildercordItems.RUNE, 1, Runes.BRACE.id(), "rune_brace accepts cobbled_deepslate");

				// 2. #wildercord:deepslate_variants -> focus_of_the_deep_well & fusion_altar
				craftCheck(l, s, 3, 3, List.of(
					ItemStack.EMPTY, new ItemStack(Items.LAPIS_BLOCK), ItemStack.EMPTY,
					new ItemStack(Items.POLISHED_DEEPSLATE), new ItemStack(WildercordItems.MANA_CRYSTAL), new ItemStack(Items.POLISHED_DEEPSLATE),
					ItemStack.EMPTY, new ItemStack(Items.POLISHED_DEEPSLATE), ItemStack.EMPTY
				), item("wildercord:focus_of_the_deep_well"), 1, null, "focus_of_the_deep_well accepts polished_deepslate");

				craftCheck(l, s, 3, 3, List.of(
					new ItemStack(Items.DEEPSLATE_BRICKS), new ItemStack(Items.AMETHYST_BLOCK), new ItemStack(Items.DEEPSLATE_BRICKS),
					new ItemStack(Items.AMETHYST_BLOCK), new ItemStack(Items.LODESTONE), new ItemStack(Items.AMETHYST_BLOCK),
					new ItemStack(Items.DEEPSLATE_BRICKS), new ItemStack(Items.AMETHYST_BLOCK), new ItemStack(Items.DEEPSLATE_BRICKS)
				), WildercordBlocks.FUSION_ALTAR.asItem(), 1, null, "fusion_altar accepts deepslate_bricks");

				// 3. #wildercord:stone_brick_variants -> arena_stone & rune_agestone
				craftCheck(l, s, 3, 3, List.of(
					new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.GOLD_INGOT),
					new ItemStack(Items.DEEPSLATE_TILES), new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.DEEPSLATE_TILES),
					new ItemStack(Items.DEEPSLATE_TILES), new ItemStack(Items.DEEPSLATE_TILES), new ItemStack(Items.DEEPSLATE_TILES)
				), WildercordBlocks.ARENA_STONE.asItem(), 1, null, "arena_stone accepts deepslate_tiles");

				craftCheck(l, s, 3, 2, List.of(
					new ItemStack(WildercordItems.BLANK_RUNE), new ItemStack(Items.MOSS_BLOCK), new ItemStack(Items.DEEPSLATE_BRICKS),
					new ItemStack(lapis), new ItemStack(lapis), new ItemStack(Items.GOLD_INGOT)
				), WildercordItems.RUNE, 1, Runes.AGESTONE.id(), "rune_agestone accepts deepslate_bricks");

				// 4. #wildercord:mossy_stone_variants -> earth_staff
				craftCheck(l, s, 3, 3, List.of(
					ItemStack.EMPTY, new ItemStack(Items.MOSSY_STONE_BRICKS), new ItemStack(WildercordItems.MANA_CRYSTAL),
					ItemStack.EMPTY, new ItemStack(Items.STICK), new ItemStack(Items.MOSSY_STONE_BRICKS),
					new ItemStack(Items.STICK), ItemStack.EMPTY, ItemStack.EMPTY
				), item("wildercord:earth_staff"), 1, null, "earth_staff accepts mossy_stone_bricks");

				// 5. #wildercord:stone_slab_variants -> rune_level_ground
				craftCheck(l, s, 3, 1, List.of(
					new ItemStack(WildercordItems.BLANK_RUNE), new ItemStack(Items.COBBLED_DEEPSLATE_SLAB), new ItemStack(Items.COMPASS)
				), WildercordItems.RUNE, 1, Runes.LEVEL_GROUND.id(), "rune_level_ground accepts cobbled_deepslate_slab");

				// 6. #wildercord:stone_stair_variants -> rune_stairwell
				craftCheck(l, s, 3, 3, List.of(
					new ItemStack(WildercordItems.BLANK_RUNE), new ItemStack(Items.COBBLED_DEEPSLATE_STAIRS), new ItemStack(Items.IRON_PICKAXE),
					new ItemStack(Items.TORCH), new ItemStack(lapis), new ItemStack(lapis),
					new ItemStack(Items.GOLD_INGOT), ItemStack.EMPTY, ItemStack.EMPTY
				), WildercordItems.RUNE, 1, Runes.STAIRWELL.id(), "rune_stairwell accepts cobbled_deepslate_stairs");

				// 7. #wildercord:sandstone_variants + #minecraft:smelts_to_glass -> rune_sandbar
				craftCheck(l, s, 3, 2, List.of(
					new ItemStack(WildercordItems.BLANK_RUNE), new ItemStack(Items.RED_SANDSTONE), new ItemStack(Items.RED_SAND),
					new ItemStack(lapis), new ItemStack(lapis), new ItemStack(Items.GOLD_INGOT)
				), WildercordItems.RUNE, 1, Runes.SANDBAR.id(), "rune_sandbar accepts red_sandstone + red_sand");

				// 8. #wildercord:glass_variants -> dewglass_lens
				craftCheck(l, s, 2, 2, List.of(
					new ItemStack(item("wildercord:moonreed_floss")), new ItemStack(item("wildercord:dusk_pearl")),
					new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.TINTED_GLASS)
				), item("wildercord:dewglass_lens"), 1, null, "dewglass_lens accepts tinted_glass");

				// 9. #wildercord:glass_pane_variants -> dewglass_spectacles
				craftCheck(l, s, 3, 1, List.of(
					new ItemStack(Items.BLUE_STAINED_GLASS_PANE), new ItemStack(Items.COPPER_INGOT), new ItemStack(item("wildercord:moonreed_floss"))
				), item("wildercord:dewglass_spectacles"), 1, null, "dewglass_spectacles accepts blue_stained_glass_pane");

				// Vanilla wood/wool/log tags -> rune_barnwarmth, rune_fleece, rune_barkhide
				craftCheck(l, s, 3, 2, List.of(
					new ItemStack(WildercordItems.BLANK_RUNE), new ItemStack(Items.CAMPFIRE), new ItemStack(Items.HAY_BLOCK),
					new ItemStack(Items.CHERRY_PLANKS), ItemStack.EMPTY, ItemStack.EMPTY
				), WildercordItems.RUNE, 1, Runes.BARNWARMTH.id(), "rune_barnwarmth accepts cherry_planks");

				craftCheck(l, s, 3, 2, List.of(
					new ItemStack(WildercordItems.BLANK_RUNE), new ItemStack(Items.SHEARS), new ItemStack(Items.PINK_WOOL),
					new ItemStack(Items.FEATHER), ItemStack.EMPTY, ItemStack.EMPTY
				), WildercordItems.RUNE, 1, Runes.FLEECE.id(), "rune_fleece accepts pink_wool");

				craftCheck(l, s, 3, 3, List.of(
					new ItemStack(WildercordItems.BLANK_RUNE), new ItemStack(Items.CHERRY_LOG), new ItemStack(Items.CHERRY_LOG),
					new ItemStack(Items.IRON_INGOT), new ItemStack(lapis), new ItemStack(lapis),
					new ItemStack(Items.GOLD_INGOT), ItemStack.EMPTY, ItemStack.EMPTY
				), WildercordItems.RUNE, 1, Runes.BARKHIDE.id(), "rune_barkhide accepts cherry_log");
			});
		}
	}
}
