package dev.wildercord.cast;

import dev.wildercord.spell.ClimateRules;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.spell.WayfarerRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Rotations;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The wayfarer's runes (the fx-explore pack): readings of the land, sky and light; bearings home, to spawn
 * and to where you fell; bounded structure senses (vanilla's own search, a shared rest, never a free map);
 * archaeology, trade, brewing and enchanting helpers; decoration (dye, glyphs, lamps, frames, stands); and
 * footing in the Nether and the End. {@link Effects} hands each of them here by name. Every block a rune
 * changes asks {@link Casters#mayEdit} first and spends the cast's block budget; every passing block (a
 * blaze, a crust, a platform) is written down in {@link TemporaryBlocks} and drops nothing. Numbers are in
 * {@link WayfarerRules}; player text is under {@code message.wildercord.wayfarer.*}.
 */
public final class WayfarerEffects {
	private WayfarerEffects() {}

	/** When each caster's structure senses last searched (game time): they share one rest. */
	private static final Map<UUID, Long> LOCATED = new HashMap<>();
	/** Lapis Thrift: until when (game time) each caster's next enchantment hands back a lapis. */
	private static final Map<UUID, Long> THRIFT = new HashMap<>();

	private static final int EARTH = 0xC89B5C, WIND = 0xCFF5E6, VOID = 0x9A6CFF, FIRE = 0xFF8A3D, ARCANE = 0xC9A6FF, TIME = 0xF2D27A,
		LIFE = 0x7FE08A, STORM = 0x8FC7FF, FROST = 0xBFEFFF;

	static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed, double power,
			double duration) {
		String path = node.effect.path();
		double radius = SpellNumbers.effectRadius(node);
		switch (path) {
			case "land_reading" -> { if (cast.once("wayfarer:" + path)) landReading(cast, hit); }
			case "depth_sounding" -> { if (cast.once("wayfarer:" + path)) depthSounding(cast, hit); }
			case "spawn_bearing" -> { if (cast.once("wayfarer:" + path)) spawnBearing(cast); }
			case "home_bearing" -> { if (cast.once("wayfarer:" + path)) homeBearing(cast); }
			case "grave_bearing" -> { if (cast.once("wayfarer:" + path)) graveBearing(cast); }
			case "portal_reckoning" -> { if (cast.once("wayfarer:" + path)) portalReckoning(cast, hit); }
			case "slime_sense" -> { if (cast.once("wayfarer:" + path)) slimeSense(cast, hit); }
			case "sky_reading" -> { if (cast.once("wayfarer:" + path)) skyReading(cast); }
			case "moon_reading" -> { if (cast.once("wayfarer:" + path)) moonReading(cast); }
			case "sun_reading" -> { if (cast.once("wayfarer:" + path)) sunReading(cast); }
			case "lux_reading" -> { if (cast.once("wayfarer:" + path)) luxReading(cast, hit); }
			case "chalk_line" -> { if (cast.once("wayfarer:" + path)) chalkLine(cast, hit, duration); }
			case "village_sense" -> locate(cast, path, tag(StructureTags.VILLAGE), null, true, radius, WIND);
			case "ruin_sense" -> locate(cast, path, key(cast, BuiltinStructures.TRAIL_RUINS), Level.OVERWORLD, true, radius, EARTH);
			case "shipwreck_sense" -> locate(cast, path, tag(StructureTags.SHIPWRECK), Level.OVERWORLD, true, radius, FROST);
			case "portal_sense" -> locate(cast, path, tag(StructureTags.RUINED_PORTAL), null, true, radius, FIRE);
			case "fortress_sense" -> locate(cast, path, key(cast, BuiltinStructures.FORTRESS), Level.NETHER, true, radius, FIRE);
			case "stronghold_compass" -> locate(cast, path, tag(StructureTags.EYE_OF_ENDER_LOCATED), Level.OVERWORLD, false, 1.0, VOID);
			case "spire_sense" -> locate(cast, path, key(cast, BuiltinStructures.END_CITY), Level.END, true, radius, VOID);
			case "trail_blaze" -> trailBlaze(cast, hit, duration);
			case "relic_sense" -> { if (cast.once("wayfarer:" + path)) relicSense(cast, hit, radius, duration); }
			case "spawner_sense" -> { if (cast.once("wayfarer:" + path)) spawnerSense(cast, hit, radius); }
			case "steady_brush" -> { if (cast.once("wayfarer:" + path)) steadyBrush(cast, hit, radius); }
			case "appraise" -> { if (cast.once("wayfarer:" + path)) appraise(cast, hit); }
			case "trade_renew" -> restock(cast, hit);
			case "haggle" -> helped.forEach(t -> haggle(cast, t, duration));
			case "folk_call" -> { if (cast.once("wayfarer:" + path)) folkCall(cast, hit, radius); }
			case "folk_census" -> { if (cast.once("wayfarer:" + path)) folkCensus(cast, hit, radius); }
			case "lapis_thrift" -> { if (cast.once("wayfarer:" + path)) lapisThrift(cast, duration); }
			case "quickbrew" -> { if (cast.once("wayfarer:" + path)) quickbrew(cast, hit, radius, duration); }
			case "potion_steep" -> helped.forEach(t -> potionSteep(cast, t));
			case "lore_reading" -> { if (cast.once("wayfarer:" + path)) loreReading(cast); }
			case "shelf_count" -> { if (cast.once("wayfarer:" + path)) shelfCount(cast, hit); }
			case "beacon_swell" -> { if (cast.once("wayfarer:" + path)) beaconSwell(cast, hit, duration); }
			case "dye_wash" -> dye(cast, hit, radius, false);
			case "checker_dye" -> dye(cast, hit, radius, true);
			case "glyph_carve" -> { if (cast.once("wayfarer:" + path)) glyphCarve(cast, hit); }
			case "lamplighter" -> lamps(cast, hit, radius, true);
			case "snuff_out" -> lamps(cast, hit, radius, false);
			case "sign_glow" -> signGlow(cast, hit, radius);
			case "frame_veil" -> { if (cast.once("wayfarer:" + path)) frameVeil(cast, hit, radius); }
			case "stand_pose" -> { if (cast.once("wayfarer:" + path)) standPose(cast, hit, radius); }
			case "lava_crust" -> lavaCrust(cast, hit, radius, duration);
			case "void_step" -> { if (cast.once("wayfarer:" + path)) voidStep(cast, duration); }
			case "lava_sense" -> { if (cast.once("wayfarer:" + path)) lavaSense(cast, radius); }
			case "gold_parley" -> { if (cast.once("wayfarer:" + path)) goldParley(cast, radius); }
			default -> { }
		}
	}

	// ------------------------------------------------------------------ readings

	private static void landReading(Cast cast, Cast.Hit hit) {
		ServerLevel level = cast.level;
		BlockPos pos = ground(hit);
		Holder<Biome> biome = level.getBiome(pos);
		Component name = biome.unwrapKey().map(k -> (Component) Component.translatable(Util.makeDescriptionId("biome", k.identifier())))
			.orElse(Component.literal("?"));
		float warmth = biome.value().getBaseTemperature();
		String feel = warmth < 0.15f ? "frozen" : warmth < 0.5f ? "cool" : warmth < 1.0f ? "mild" : "hot";
		int above = pos.getY() - level.getSeaLevel();
		tell(cast, "land_reading", name, Component.translatable("message.wildercord.wayfarer.warmth." + feel),
			String.format(Locale.ROOT, "%.1f", warmth), pos.getY(), (above >= 0 ? "+" : "") + above);
		mark(level, Vec3.atCenterOf(pos).add(0, 0.6, 0), EARTH, 10);
		chime(level, Vec3.atCenterOf(pos), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.1f);
	}

	private static void depthSounding(Cast cast, Cast.Hit hit) {
		ServerLevel level = cast.level;
		BlockPos start = ground(hit);
		chime(level, Vec3.atCenterOf(start), SoundEvents.STONE_HIT, 0.6f);
		for (int i = 1; i <= 64; i++) {
			BlockPos at = start.below(i);
			if (level.isOutsideBuildHeight(at)) {
				break;
			}
			BlockState state = level.getBlockState(at);
			if (state.getFluidState().is(FluidTags.LAVA)) {
				tell(cast, "depth_sounding.lava", i);
				mark(level, Vec3.atCenterOf(start).add(0, 1, 0), FIRE, 12);
				return;
			}
			if (state.isAir()) {
				tell(cast, "depth_sounding.cave", i);
				mark(level, Vec3.atCenterOf(start).add(0, 1, 0), EARTH, 12);
				return;
			}
		}
		tell(cast, "depth_sounding.solid");
	}

	private static void spawnBearing(Cast cast) {
		LevelData.RespawnData spawn = cast.level.getRespawnData();
		if (spawn == null || spawn.dimension() != cast.level.dimension()) {
			tell(cast, "spawn_bearing.elsewhere");
			return;
		}
		bearing(cast, "spawn_bearing", Vec3.atCenterOf(spawn.pos()), true, WIND);
	}

	private static void homeBearing(Cast cast) {
		if (!(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		ServerPlayer.RespawnConfig home = player.getRespawnConfig();
		if (home == null) {
			tell(cast, "home_bearing.none");
		} else if (home.respawnData().dimension() != cast.level.dimension()) {
			tell(cast, "home_bearing.elsewhere");
		} else {
			bearing(cast, "home_bearing", Vec3.atCenterOf(home.respawnData().pos()), true, WIND);
		}
	}

	private static void graveBearing(Cast cast) {
		if (!(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		Optional<GlobalPos> grave = player.getLastDeathLocation();
		if (grave.isEmpty()) {
			tell(cast, "grave_bearing.none");
		} else if (grave.get().dimension() != cast.level.dimension()) {
			tell(cast, "grave_bearing.elsewhere");
		} else {
			bearing(cast, "grave_bearing", Vec3.atCenterOf(grave.get().pos()), true, VOID);
		}
	}

	private static void portalReckoning(Cast cast, Cast.Hit hit) {
		Vec3 at = cast.caster.position();
		ResourceKey<Level> here = cast.level.dimension();
		if (here == Level.NETHER) {
			tell(cast, "portal_reckoning.overworld", WayfarerRules.toOverworld(at.x), WayfarerRules.toOverworld(at.z));
		} else if (here == Level.OVERWORLD) {
			tell(cast, "portal_reckoning.nether", WayfarerRules.toNether(at.x), WayfarerRules.toNether(at.z));
		} else {
			tell(cast, "portal_reckoning.none");
			return;
		}
		Fx.send(cast.level, ParticleTypes.REVERSE_PORTAL, at.add(0, 1, 0), 16, 0.4, 0.02);
		chime(cast.level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f);
	}

	private static void slimeSense(Cast cast, Cast.Hit hit) {
		ServerLevel level = cast.level;
		if (level.dimension() != Level.OVERWORLD) {
			tell(cast, "slime_sense.none");
			return;
		}
		BlockPos pos = BlockPos.containing(hit.point());
		boolean slimy = WorldgenRandom.seedSlimeChunk(pos.getX() >> 4, pos.getZ() >> 4, level.getSeed(), 987234911L).nextInt(10) == 0;
		tell(cast, slimy ? "slime_sense.yes" : "slime_sense.no");
		mark(level, hit.point().add(0, 0.5, 0), slimy ? LIFE : 0x808080, 10);
		chime(level, hit.point(), SoundEvents.SLIME_SQUISH_SMALL, slimy ? 1.2f : 0.6f);
	}

	private static void skyReading(Cast cast) {
		ServerLevel level = cast.level;
		if (level.dimensionType().hasFixedTime() || !level.dimensionType().hasSkyLight()) {
			tell(cast, "sky_reading.none");
			return;
		}
		WeatherData weather = level.getWeatherData();
		if (weather.getClearWeatherTime() > 0) {
			tell(cast, "sky_reading.held", WayfarerRules.minutes(weather.getClearWeatherTime()));
		} else if (weather.isRaining()) {
			tell(cast, weather.isThundering() ? "sky_reading.storm" : "sky_reading.rain", WayfarerRules.minutes(weather.getRainTime()));
		} else {
			tell(cast, "sky_reading.clear", WayfarerRules.minutes(weather.getRainTime()));
		}
		Vec3 above = cast.caster.getEyePosition().add(0, 1.5, 0);
		Fx.send(level, ParticleTypes.CLOUD, above, 8, 0.6, 0.01);
		chime(level, above, SoundEvents.AMETHYST_BLOCK_CHIME, 1.3f);
	}

	private static void moonReading(Cast cast) {
		ServerLevel level = cast.level;
		if (level.dimensionType().hasFixedTime()) {
			tell(cast, "moon_reading.none");
			return;
		}
		int phase = ClimateRules.moonPhase(level.getOverworldClockTime());
		tell(cast, "moon_reading", Component.translatable("message.wildercord.wayfarer.moon." + phase), WayfarerRules.nightsToFull(phase));
		Fx.send(level, ParticleTypes.END_ROD, cast.caster.getEyePosition().add(0, 1.5, 0), 6, 0.3, 0.01);
		chime(level, cast.caster.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f);
	}

	private static void sunReading(Cast cast) {
		ServerLevel level = cast.level;
		if (level.dimensionType().hasFixedTime()) {
			tell(cast, "sun_reading.none");
			return;
		}
		long clock = level.getOverworldClockTime();
		tell(cast, WayfarerRules.day(clock) ? "sun_reading.day" : "sun_reading.night", WayfarerRules.hour(clock),
			WayfarerRules.minutes(WayfarerRules.untilTurn(clock)));
		mark(level, cast.caster.getEyePosition().add(0, 1.5, 0), TIME, 8);
		chime(level, cast.caster.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f);
	}

	private static void luxReading(Cast cast, Cast.Hit hit) {
		ServerLevel level = cast.level;
		BlockPos pos = hit.block() != null && hit.face() != null ? hit.block().relative(hit.face()) : BlockPos.containing(hit.point());
		int blockLight = level.getBrightness(LightLayer.BLOCK, pos), skyLight = level.getBrightness(LightLayer.SKY, pos);
		tell(cast, blockLight == 0 ? "lux_reading.dark" : "lux_reading.lit", blockLight, skyLight);
		mark(level, Vec3.atCenterOf(pos), blockLight == 0 ? 0x5A3A7A : 0xFFF0A0, 8);
	}

	private static void chalkLine(Cast cast, Cast.Hit hit, double duration) {
		ServerLevel level = cast.level;
		Vec3 from = cast.caster.position().add(0, 0.1, 0), to = hit.point();
		double length = from.distanceTo(to);
		tell(cast, "chalk_line", String.format(Locale.ROOT, "%.1f", length));
		int pulses = (int) Math.min(30, Math.max(1, Math.round(10 * duration)));
		for (int i = 0; i < pulses; i++) {
			Scheduler.later(i * 20, () -> {
				if (!cast.alive()) {
					return;
				}
				int steps = (int) Math.min(96, Math.ceil(length * 2));
				for (int s = 0; s <= steps; s++) {
					Fx.send(level, Fx.dust(0xF4F1E8, 0.8f), from.lerp(to, steps == 0 ? 0 : (double) s / steps), 1, 0, 0);
				}
			});
		}
		chime(level, from, SoundEvents.UI_LOOM_TAKE_RESULT, 1.4f);
	}

	// ------------------------------------------------------------------ structure senses

	private static TagKey<Structure> tag(TagKey<Structure> tag) {
		return tag;
	}

	private static HolderSet<Structure> key(Cast cast, ResourceKey<Structure> key) {
		return cast.level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(key).<HolderSet<Structure>>map(HolderSet::direct).orElse(null);
	}

	/** One structure sense: in its realm only, then a shared rest, then vanilla's own nearest-structure search. */
	@SuppressWarnings("unchecked")
	private static void locate(Cast cast, String path, Object what, ResourceKey<Level> realm, boolean distance, double radius, int color) {
		if (!(cast.caster instanceof ServerPlayer player) || !cast.once("wayfarer:locate")) {
			return;
		}
		ServerLevel level = cast.level;
		if (realm != null && level.dimension() != realm || what == null) {
			tell(cast, path + ".realm");
			return;
		}
		long now = level.getGameTime();
		Long last = LOCATED.get(player.getUUID());
		if (WayfarerRules.resting(last == null ? Long.MIN_VALUE : last, now)) {
			tell(cast, "locate_resting", (WayfarerRules.LOCATE_REST - (now - last) + 19) / 20);
			return;
		}
		LOCATED.put(player.getUUID(), now);
		int cells = WayfarerRules.locateCells(radius);
		BlockPos found = what instanceof TagKey<?> tag ? level.findNearestMapStructure((TagKey<Structure>) tag, player.blockPosition(), cells, false)
			: level.findNearestMapStructure((HolderSet<Structure>) what, player.blockPosition(), cells, false);
		if (found == null) {
			tell(cast, path + ".none");
			chime(level, player.position(), SoundEvents.LODESTONE_COMPASS_LOCK, 0.6f);
			return;
		}
		bearing(cast, path, Vec3.atCenterOf(found), distance, color);
	}

	/** Tells which way (and, if {@code distance}, roughly how far) {@code to} lies, with a short pointer of light that way. */
	private static void bearing(Cast cast, String key, Vec3 to, boolean distance, int color) {
		LivingEntity caster = cast.caster;
		double dx = to.x - caster.getX(), dz = to.z - caster.getZ();
		Component way = Component.translatable("direction.wildercord." + WayfarerRules.COMPASS[WayfarerRules.compass(dx, dz)]);
		if (distance) {
			tell(cast, key, way, WayfarerRules.rough(Math.sqrt(dx * dx + dz * dz)));
		} else {
			tell(cast, key, way);
		}
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len > 0.01) {
			Vec3 dir = new Vec3(dx / len, 0, dz / len), from = caster.getEyePosition().add(0, -0.4, 0);
			for (int i = 1; i <= 8; i++) {
				Fx.send(cast.level, Fx.dust(color, 1.0f), from.add(dir.scale(0.5 * i)), 1, 0, 0);
			}
		}
		chime(cast.level, caster.position(), SoundEvents.LODESTONE_COMPASS_LOCK, 1.2f);
	}

	// ------------------------------------------------------------------ markers and archaeology

	private static void trailBlaze(Cast cast, Cast.Hit hit, double duration) {
		ServerLevel level = cast.level;
		Direction face = hit.block() != null && hit.face() != null ? hit.face() : Direction.UP;
		BlockPos pos = hit.block() != null && hit.face() != null ? hit.block().relative(hit.face()) : BlockPos.containing(hit.point());
		if (!level.getBlockState(pos).isAir() || !mayEdit(cast, pos)) {
			tell(cast, "trail_blaze.blocked");
			return;
		}
		BlockState rod = Blocks.END_ROD.defaultBlockState().setValue(BlockStateProperties.FACING, face);
		temporary(cast, pos, rod, Blocks.AIR.defaultBlockState(), Effects.ticks(300, duration));
		Fx.send(level, ParticleTypes.END_ROD, Vec3.atCenterOf(pos), 12, 0.3, 0.03);
		chime(level, Vec3.atCenterOf(pos), SoundEvents.LANTERN_PLACE, 1.2f);
	}

	private static void relicSense(Cast cast, Cast.Hit hit, double radius, double duration) {
		ServerLevel level = cast.level;
		double reach = WayfarerRules.area(12, radius, 24);
		List<BrushableBlockEntity> found = blockEntities(level, hit.point(), reach, BrushableBlockEntity.class, 32);
		if (found.isEmpty()) {
			tell(cast, "relic_sense.none");
			return;
		}
		tell(cast, "relic_sense", found.size());
		int pulses = (int) Math.min(30, Math.max(1, Math.round(20 * duration)));
		for (int i = 0; i < pulses; i++) {
			Scheduler.later(i * 20, () -> {
				for (BrushableBlockEntity relic : found) {
					if (!relic.isRemoved()) {
						Fx.send(level, ParticleTypes.WAX_ON, Vec3.atCenterOf(relic.getBlockPos()).add(0, 0.6, 0), 3, 0.3, 0.01);
					}
				}
			});
		}
		chime(level, hit.point(), SoundEvents.BRUSH_SAND, 1.2f);
	}

	private static void spawnerSense(Cast cast, Cast.Hit hit, double radius) {
		ServerLevel level = cast.level;
		double reach = WayfarerRules.area(32, radius, 48);
		List<BlockEntity> found = new ArrayList<>(blockEntities(level, hit.point(), reach, SpawnerBlockEntity.class, 64));
		found.addAll(blockEntities(level, hit.point(), reach, TrialSpawnerBlockEntity.class, 64));
		if (found.isEmpty()) {
			tell(cast, "spawner_sense.none");
			return;
		}
		Vec3 me = cast.caster.position();
		BlockEntity nearest = found.stream().min(Comparator.comparingDouble(b -> Vec3.atCenterOf(b.getBlockPos()).distanceToSqr(me))).orElseThrow();
		Vec3 at = Vec3.atCenterOf(nearest.getBlockPos());
		double dx = at.x - me.x, dz = at.z - me.z;
		tell(cast, "spawner_sense", found.size(), Component.translatable("direction.wildercord." + WayfarerRules.COMPASS[WayfarerRules.compass(dx, dz)]),
			WayfarerRules.rough(at.distanceTo(me)));
		Fx.send(level, Fx.dust(VOID, 1.4f), at, 16, 0.5, 0.01);
		chime(level, me, SoundEvents.SCULK_CLICKING, 0.8f);
	}

	/** Steady Brush: the held brush's strokes, a few blocks at once, through vanilla's own brushing (its loot, its rhythm). */
	private static void steadyBrush(Cast cast, Cast.Hit hit, double radius) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		EquipmentSlot slot = caster.getMainHandItem().is(Items.BRUSH) ? EquipmentSlot.MAINHAND : caster.getOffhandItem().is(Items.BRUSH) ? EquipmentSlot.OFFHAND : null;
		if (slot == null) {
			tell(cast, "steady_brush.no_brush");
			return;
		}
		List<BrushableBlockEntity> relics = new ArrayList<>();
		for (BrushableBlockEntity relic : blockEntities(level, hit.point(), WayfarerRules.area(2.5, radius, 4), BrushableBlockEntity.class, 8)) {
			if (relics.size() < WayfarerRules.BRUSH_BLOCKS && mayEdit(cast, relic.getBlockPos())) {
				relics.add(relic);
			}
		}
		if (relics.isEmpty()) {
			tell(cast, "steady_brush.none");
			return;
		}
		ItemStack brush = caster.getItemBySlot(slot);
		brush.hurtAndBreak(relics.size(), caster, slot);
		ItemStack stroke = brush.isEmpty() ? new ItemStack(Items.BRUSH) : brush.copy();
		for (int s = 0; s < WayfarerRules.BRUSH_STROKES; s++) {
			Scheduler.later(1 + s * (WayfarerRules.BRUSH_EVERY + 1), () -> {
				if (!cast.alive()) {
					return;
				}
				for (BrushableBlockEntity relic : relics) {
					if (!relic.isRemoved() && level.getBlockEntity(relic.getBlockPos()) == relic) {
						relic.brush(level.getGameTime(), level, caster, Direction.UP, stroke);
						Fx.send(level, ParticleTypes.DUST_PLUME, Vec3.atCenterOf(relic.getBlockPos()).add(0, 0.6, 0), 3, 0.25, 0.01);
						Fx.sound(level, Vec3.atCenterOf(relic.getBlockPos()), SoundEvents.BRUSH_SAND, 0.5f, 1.0f);
					}
				}
			});
		}
		tell(cast, "steady_brush", relics.size());
	}

	// ------------------------------------------------------------------ villagers

	private static Villager villager(Cast cast, Cast.Hit hit) {
		for (Entity e : hit.entities()) {
			if (e instanceof Villager v) {
				return v;
			}
		}
		Vec3 at = hit.point();
		return cast.level.getEntitiesOfClass(Villager.class, new AABB(at, at).inflate(4)).stream()
			.min(Comparator.comparingDouble(v -> v.distanceToSqr(at))).orElse(null);
	}

	private static boolean trades(Villager villager) {
		Holder<VillagerProfession> job = villager.getVillagerData().profession();
		return !villager.isBaby() && !job.is(VillagerProfession.NONE) && !job.is(VillagerProfession.NITWIT);
	}

	private static void appraise(Cast cast, Cast.Hit hit) {
		Villager villager = villager(cast, hit);
		if (villager == null) {
			tell(cast, "appraise.none");
			return;
		}
		if (!trades(villager)) {
			tell(cast, "appraise.idle");
			return;
		}
		int out = 0, all = 0;
		for (MerchantOffer offer : villager.getOffers()) {
			all++;
			if (offer.isOutOfStock()) {
				out++;
			}
		}
		tell(cast, "appraise", villager.getVillagerData().profession().value().name(), villager.getVillagerData().level(), out, all);
		Fx.send(cast.level, ParticleTypes.HAPPY_VILLAGER, villager.getEyePosition().add(0, 0.4, 0), 6, 0.3, 0.01);
		chime(cast.level, villager.position(), SoundEvents.VILLAGER_TRADE, 1.0f);
	}

	private static void restock(Cast cast, Cast.Hit hit) {
		Villager villager = villager(cast, hit);
		if (villager == null || !trades(villager)) {
			tell(cast, villager == null ? "appraise.none" : "appraise.idle");
			return;
		}
		long day = WayfarerRules.dayOf(cast.level.getOverworldClockTime());
		String tag = WayfarerRules.restockTag(day);
		if (villager.entityTags().contains(tag)) {
			tell(cast, "restock.done");
			return;
		}
		villager.entityTags().stream().filter(t -> t.startsWith("wildercord.restocked.")).toList().forEach(villager::removeTag);
		villager.addTag(tag);
		villager.restock();
		tell(cast, "restock");
		Fx.send(cast.level, ParticleTypes.HAPPY_VILLAGER, villager.getEyePosition(), 12, 0.4, 0.02);
		chime(cast.level, villager.position(), SoundEvents.VILLAGER_YES, 1.0f);
	}

	private static void haggle(Cast cast, LivingEntity target, double duration) {
		target.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, Effects.ticks(20, duration), 0, false, true));
		Fx.send(cast.level, ParticleTypes.HAPPY_VILLAGER, target.getEyePosition(), 8, 0.4, 0.02);
		chime(cast.level, target.position(), SoundEvents.VILLAGER_TRADE, 1.3f);
	}

	private static void folkCall(Cast cast, Cast.Hit hit, double radius) {
		BlockPos to = BlockPos.containing(hit.point());
		double reach = WayfarerRules.area(16, radius, 24);
		int called = 0;
		for (Villager villager : cast.level.getEntitiesOfClass(Villager.class, cast.caster.getBoundingBox().inflate(reach))) {
			if (called >= 12) {
				break;
			}
			villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(to, 0.6f, 1));
			Fx.send(cast.level, ParticleTypes.NOTE, villager.getEyePosition().add(0, 0.5, 0), 1, 0, 0);
			called++;
		}
		tell(cast, "folk_call", called);
		chime(cast.level, hit.point(), SoundEvents.BELL_BLOCK, 1.2f);
	}

	private static void folkCensus(Cast cast, Cast.Hit hit, double radius) {
		double reach = WayfarerRules.area(32, radius, 48);
		AABB box = new AABB(hit.point(), hit.point()).inflate(reach);
		int working = 0, jobless = 0, nitwits = 0, children = 0;
		for (Villager villager : cast.level.getEntitiesOfClass(Villager.class, box)) {
			Holder<VillagerProfession> job = villager.getVillagerData().profession();
			if (villager.isBaby()) {
				children++;
			} else if (job.is(VillagerProfession.NITWIT)) {
				nitwits++;
			} else if (job.is(VillagerProfession.NONE)) {
				jobless++;
			} else {
				working++;
			}
		}
		int golems = cast.level.getEntitiesOfClass(IronGolem.class, box).size();
		tell(cast, "folk_census", working, jobless, nitwits, children, golems);
		chime(cast.level, hit.point(), SoundEvents.BOOK_PAGE_TURN, 1.0f);
	}

	// ------------------------------------------------------------------ enchanting and brewing

	private static void lapisThrift(Cast cast, double duration) {
		if (!(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		long until = cast.level.getGameTime() + Effects.ticks(60, duration);
		boolean watching = THRIFT.containsKey(player.getUUID());
		THRIFT.put(player.getUUID(), until);
		tell(cast, "lapis_thrift");
		Fx.send(cast.level, ParticleTypes.ENCHANT, player.getEyePosition(), 20, 0.5, 0.5);
		if (!watching) {
			watchThrift(player, player.getEnchantmentSeed());
		}
	}

	/** Every few ticks while it lasts: an enchantment (the seed changes) hands back one lapis, once. */
	private static void watchThrift(ServerPlayer player, int seed) {
		Scheduler.later(5, () -> {
			Long until = THRIFT.get(player.getUUID());
			if (until == null || player.isRemoved() || !(player.level() instanceof ServerLevel level) || level.getGameTime() > until) {
				THRIFT.remove(player.getUUID());
				return;
			}
			if (player.getEnchantmentSeed() != seed) {
				THRIFT.remove(player.getUUID());
				ItemStack lapis = new ItemStack(Items.LAPIS_LAZULI);
				if (!player.getInventory().add(lapis)) {
					player.drop(lapis, false, net.minecraft.util.Prediction.SERVER_ONLY);
				}
				Casters.tell(player, Component.translatable("message.wildercord.wayfarer.lapis_thrift.refund"));
				Fx.sound(level, player.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.6f, 1.4f);
				return;
			}
			watchThrift(player, seed);
		});
	}

	private static void quickbrew(Cast cast, Cast.Hit hit, double radius, double duration) {
		ServerLevel level = cast.level;
		List<BrewingStandBlockEntity> stands = blockEntities(level, hit.point(), WayfarerRules.area(WayfarerRules.AREA, radius, WayfarerRules.AREA_MAX),
			BrewingStandBlockEntity.class, 4);
		if (stands.isEmpty()) {
			tell(cast, "quickbrew.none");
			return;
		}
		tell(cast, "quickbrew", stands.size());
		brewOn(cast, stands, Effects.ticks(20, duration));
		chime(level, hit.point(), SoundEvents.BREWING_STAND_BREW, 1.3f);
	}

	/** One extra brewing tick a game tick for each stand: twice as fast, through vanilla's own brewing. */
	private static void brewOn(Cast cast, List<BrewingStandBlockEntity> stands, int left) {
		if (left <= 0 || !cast.alive()) {
			return;
		}
		Scheduler.later(1, () -> {
			for (BrewingStandBlockEntity stand : stands) {
				if (!stand.isRemoved() && cast.level.getBlockEntity(stand.getBlockPos()) == stand) {
					BrewingStandBlockEntity.serverTick(cast.level, stand.getBlockPos(), stand.getBlockState(), stand);
					if (left % 10 == 0) {
						Fx.send(cast.level, Fx.dust(FIRE, 0.8f), Vec3.atCenterOf(stand.getBlockPos()).add(0, 0.6, 0), 2, 0.2, 0.01);
					}
				}
			}
			brewOn(cast, stands, left - 1);
		});
	}

	private static void potionSteep(Cast cast, LivingEntity target) {
		int steeped = 0;
		for (MobEffectInstance effect : List.copyOf(target.getActiveEffects())) {
			if (!effect.getEffect().value().isBeneficial() || effect.isInfiniteDuration() || effect.isAmbient()) {
				continue;
			}
			int longer = WayfarerRules.steeped(effect.getDuration());
			if (longer > effect.getDuration()) {
				target.addEffect(new MobEffectInstance(effect.getEffect(), longer, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
				steeped++;
			}
		}
		if (target == cast.caster) {
			tell(cast, steeped == 0 ? "potion_steep.none" : "potion_steep", steeped);
		}
		if (steeped > 0) {
			Fx.send(cast.level, Fx.dust(LIFE, 1.0f), target.getEyePosition(), 10, 0.4, 0.05);
			chime(cast.level, target.position(), SoundEvents.BREWING_STAND_BREW, 1.6f);
		}
	}

	private static void loreReading(Cast cast) {
		ItemStack held = cast.caster.getMainHandItem();
		if (held.isEmpty()) {
			tell(cast, "lore_reading.empty");
			return;
		}
		int enchantments = held.getEnchantments().size() + held.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).size();
		int cost = held.getOrDefault(DataComponents.REPAIR_COST, 0);
		tell(cast, WayfarerRules.tooCostly(cost) ? "lore_reading.costly" : "lore_reading", held.getHoverName(), enchantments, cost);
		Fx.send(cast.level, ParticleTypes.ENCHANT, cast.caster.getEyePosition(), 16, 0.4, 0.4);
		chime(cast.level, cast.caster.position(), SoundEvents.BOOK_PAGE_TURN, 1.2f);
	}

	private static void shelfCount(Cast cast, Cast.Hit hit) {
		ServerLevel level = cast.level;
		BlockPos table = null;
		if (hit.block() != null && level.getBlockState(hit.block()).getBlock() instanceof EnchantingTableBlock) {
			table = hit.block();
		} else {
			BlockPos c = BlockPos.containing(hit.point());
			double best = Double.MAX_VALUE;
			for (BlockPos p : BlockPos.betweenClosed(c.offset(-4, -2, -4), c.offset(4, 2, 4))) {
				if (level.getBlockState(p).getBlock() instanceof EnchantingTableBlock && p.distSqr(c) < best) {
					best = p.distSqr(c);
					table = p.immutable();
				}
			}
		}
		if (table == null) {
			tell(cast, "shelf_count.none");
			return;
		}
		int counted = 0, blocked = 0;
		for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
			if (EnchantingTableBlock.isValidBookShelf(level, table, offset)) {
				counted++;
			} else if (level.getBlockState(table.offset(offset)).is(BlockTags.ENCHANTMENT_POWER_PROVIDER)) {
				blocked++;
			}
		}
		tell(cast, "shelf_count", WayfarerRules.shelves(counted), blocked);
		Fx.send(level, ParticleTypes.ENCHANT, Vec3.atCenterOf(table).add(0, 1, 0), 24, 0.8, 0.6);
		chime(level, Vec3.atCenterOf(table), SoundEvents.CHISELED_BOOKSHELF_INSERT, 1.0f);
	}

	private static void beaconSwell(Cast cast, Cast.Hit hit, double duration) {
		ServerLevel level = cast.level;
		List<BeaconBlockEntity> beacons = blockEntities(level, hit.point(), 16, BeaconBlockEntity.class, 2);
		if (beacons.isEmpty()) {
			tell(cast, "beacon_swell.none");
			return;
		}
		tell(cast, "beacon_swell", beacons.size());
		int pulses = Math.max(1, Effects.ticks(60, duration) / 80);
		for (int i = 0; i < pulses; i++) {
			Scheduler.later(i * 80, () -> beacons.forEach(beacon -> swell(level, beacon)));
		}
		chime(level, hit.point(), SoundEvents.BEACON_POWER_SELECT, 1.2f);
	}

	/** One beacon's powers, as it gives them itself, to players in the wider ring (read from its own saved data). */
	private static void swell(ServerLevel level, BeaconBlockEntity beacon) {
		if (beacon.isRemoved() || level.getBlockEntity(beacon.getBlockPos()) != beacon) {
			return;
		}
		CompoundTag saved = beacon.saveCustomOnly(level.registryAccess());
		int levels = saved.getIntOr("Levels", 0);
		String primary = saved.getStringOr("primary_effect", ""), secondary = saved.getStringOr("secondary_effect", "");
		if (levels <= 0 || primary.isEmpty()) {
			return;
		}
		AABB box = new AABB(beacon.getBlockPos()).inflate(WayfarerRules.swelledReach(levels)).expandTowards(0, level.getHeight(), 0);
		int ticks = (9 + levels * 2) * 20;
		Optional<Holder.Reference<MobEffect>> first = effect(primary), second = secondary.isEmpty() || secondary.equals(primary) ? Optional.empty() : effect(secondary);
		int amplifier = levels >= 4 && primary.equals(secondary) ? 1 : 0;
		for (Player player : level.getEntitiesOfClass(Player.class, box)) {
			first.ifPresent(e -> player.addEffect(new MobEffectInstance(e, ticks, amplifier, true, true)));
			if (levels >= 4) {
				second.ifPresent(e -> player.addEffect(new MobEffectInstance(e, ticks, 0, true, true)));
			}
		}
		Fx.send(level, Fx.dust(ARCANE, 1.2f), Vec3.atCenterOf(beacon.getBlockPos()).add(0, 1.2, 0), 10, 0.5, 0.02);
	}

	private static Optional<Holder.Reference<MobEffect>> effect(String id) {
		Identifier parsed = Identifier.tryParse(id);
		return parsed == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.get(parsed);
	}

	// ------------------------------------------------------------------ decoration

	/** The dyed families a dye recolours, each one block per colour. */
	private static final List<ColorCollection<Block>> DYED = List.of(Blocks.WOOL, Blocks.WOOL_STAIRS, Blocks.WOOL_SLAB, Blocks.STAINED_GLASS,
		Blocks.DYED_TERRACOTTA, Blocks.STAINED_GLASS_PANE, Blocks.CARPET, Blocks.GLAZED_TERRACOTTA, Blocks.CONCRETE, Blocks.CONCRETE_STAIRS,
		Blocks.CONCRETE_SLAB, Blocks.CONCRETE_POWDER, Blocks.DYED_CANDLE);

	/** {@code state} recoloured to {@code color}, or null when it isn't dyeable or already is that colour. */
	static BlockState recolour(BlockState state, DyeColor color) {
		Block block = state.getBlock();
		ColorCollection<Block> family = block == Blocks.GLASS ? Blocks.STAINED_GLASS : block == Blocks.GLASS_PANE ? Blocks.STAINED_GLASS_PANE
			: block == Blocks.TERRACOTTA ? Blocks.DYED_TERRACOTTA : block == Blocks.CANDLE ? Blocks.DYED_CANDLE : null;
		if (family == null) {
			for (ColorCollection<Block> dyed : DYED) {
				if (dyed.asList().contains(block)) {
					family = dyed;
					break;
				}
			}
		}
		if (family == null || family.pick(color) == block) {
			return null;
		}
		return family.pick(color).withPropertiesOf(state);
	}

	private static void dye(Cast cast, Cast.Hit hit, double radius, boolean checker) {
		ServerLevel level = cast.level;
		ItemStack held = cast.caster.getOffhandItem();
		DyeColor color = held.get(DataComponents.DYE);
		if (color == null) {
			tell(cast, "dye_wash.no_dye");
			return;
		}
		boolean free = Casters.creative(cast.caster);
		int room = free ? WayfarerRules.MAX_DYED : WayfarerRules.dyeable(held.getCount());
		int dyed = 0;
		for (BlockPos pos : around(hit, WayfarerRules.area(2, radius, 4))) {
			if (dyed >= room) {
				break;
			}
			if (checker && !WayfarerRules.checker(pos.getX(), pos.getY(), pos.getZ())) {
				continue;
			}
			BlockState next = recolour(level.getBlockState(pos), color);
			if (next == null || !mayEdit(cast, pos)) {
				continue;
			}
			level.setBlock(pos, next, Block.UPDATE_ALL);
			Fx.send(level, Fx.dust(color.getTextureDiffuseColor(), 1.0f), Vec3.atCenterOf(pos), 3, 0.35, 0.01);
			dyed++;
		}
		if (dyed == 0) {
			tell(cast, "dye_wash.none");
			return;
		}
		if (!free) {
			held.shrink(WayfarerRules.dyesFor(dyed));
		}
		tell(cast, "dye_wash", dyed);
		chime(level, hit.point(), SoundEvents.DYE_USE, 1.0f);
	}

	private static SignBlockEntity sign(Cast cast, Cast.Hit hit) {
		if (hit.block() != null && cast.level.getBlockEntity(hit.block()) instanceof SignBlockEntity sign) {
			return sign;
		}
		List<SignBlockEntity> near = blockEntities(cast.level, hit.point(), 4, SignBlockEntity.class, 1);
		return near.isEmpty() ? null : near.getFirst();
	}

	private static void glyphCarve(Cast cast, Cast.Hit hit) {
		if (!(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		SignBlockEntity sign = sign(cast, hit);
		if (sign == null || sign.isWaxed() || !mayEdit(cast, sign.getBlockPos())) {
			tell(cast, sign == null ? "glyph_carve.none" : "glyph_carve.sealed");
			return;
		}
		SignTextSlot slot = sign.getSlotPlayerIsFacing(player);
		SignText text = sign.getText(slot);
		List<Component> lines = text.getMessages(false);
		int blank = -1;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).getString().isBlank()) {
				blank = i;
				break;
			}
		}
		if (blank < 0) {
			tell(cast, "glyph_carve.full");
			return;
		}
		Vec3 look = player.getLookAngle();
		Component glyph = Component.literal("✦ ").append(Component.translatable("direction.wildercord."
			+ WayfarerRules.COMPASS[WayfarerRules.compass(look.x, look.z)])).append(" ✦");
		sign.setText(text.asMutable().setLine(blank, glyph).setTextGlowing(true).asImmutable(), slot);
		refresh(cast.level, sign);
		tell(cast, "glyph_carve");
		Fx.send(cast.level, ParticleTypes.WAX_OFF, Vec3.atCenterOf(sign.getBlockPos()), 8, 0.3, 0.02);
		chime(cast.level, Vec3.atCenterOf(sign.getBlockPos()), SoundEvents.GLOW_INK_SAC_USE, 1.1f);
	}

	private static void signGlow(Cast cast, Cast.Hit hit, double radius) {
		int lit = 0;
		for (SignBlockEntity sign : blockEntities(cast.level, hit.point(), WayfarerRules.area(WayfarerRules.AREA, radius, WayfarerRules.AREA_MAX),
				SignBlockEntity.class, WayfarerRules.MAX_EDITS)) {
			if (sign.isWaxed() || !mayEdit(cast, sign.getBlockPos())) {
				continue;
			}
			for (SignTextSlot slot : List.of(SignTextSlot.FRONT, SignTextSlot.BACK)) {
				sign.setText(sign.getText(slot).withGlowingText(true), slot);
			}
			refresh(cast.level, sign);
			Fx.send(cast.level, ParticleTypes.GLOW, Vec3.atCenterOf(sign.getBlockPos()), 4, 0.3, 0.01);
			lit++;
		}
		tell(cast, lit == 0 ? "sign_glow.none" : "sign_glow", lit);
		if (lit > 0) {
			chime(cast.level, hit.point(), SoundEvents.GLOW_INK_SAC_USE, 1.0f);
		}
	}

	private static void refresh(ServerLevel level, BlockEntity entity) {
		entity.setChanged();
		level.sendBlockUpdated(entity.getBlockPos(), entity.getBlockState(), entity.getBlockState(), Block.UPDATE_ALL);
	}

	/** Lamplighter and Douse: candles (and candle cakes) and campfires, lit or put out in place. */
	private static void lamps(Cast cast, Cast.Hit hit, double radius, boolean light) {
		ServerLevel level = cast.level;
		int changed = 0;
		for (BlockPos pos : around(hit, WayfarerRules.area(WayfarerRules.AREA, radius, WayfarerRules.AREA_MAX))) {
			if (changed >= WayfarerRules.MAX_EDITS) {
				break;
			}
			BlockState state = level.getBlockState(pos);
			boolean lamp = state.getBlock() instanceof AbstractCandleBlock || state.getBlock() instanceof CampfireBlock;
			if (!lamp || !state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT) == light
				|| light && state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)
				|| !mayEdit(cast, pos)) {
				continue;
			}
			level.setBlock(pos, state.setValue(BlockStateProperties.LIT, light), Block.UPDATE_ALL);
			Fx.send(level, light ? ParticleTypes.SMALL_FLAME : ParticleTypes.SMOKE, Vec3.atCenterOf(pos).add(0, 0.4, 0), 4, 0.15, 0.01);
			changed++;
		}
		String key = light ? "lamplighter" : "snuff_out";
		tell(cast, changed == 0 ? key + ".none" : key, changed);
		if (changed > 0) {
			chime(level, hit.point(), light ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.CANDLE_EXTINGUISH, 1.0f);
		}
	}

	private static void frameVeil(Cast cast, Cast.Hit hit, double radius) {
		double reach = WayfarerRules.area(WayfarerRules.AREA, radius, WayfarerRules.AREA_MAX);
		List<ItemFrame> frames = new ArrayList<>();
		for (ItemFrame frame : cast.level.getEntitiesOfClass(ItemFrame.class, new AABB(hit.point(), hit.point()).inflate(reach))) {
			if (!frame.getItem().isEmpty() && frames.size() < WayfarerRules.MAX_EDITS && mayEdit(cast, frame.blockPosition())) {
				frames.add(frame);
			}
		}
		if (frames.isEmpty()) {
			tell(cast, "frame_veil.none");
			return;
		}
		boolean hide = frames.stream().anyMatch(f -> !f.isInvisible());
		for (ItemFrame frame : frames) {
			frame.setInvisible(hide);
			Fx.send(cast.level, hide ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.END_ROD, frame.position(), 4, 0.2, 0.01);
		}
		tell(cast, hide ? "frame_veil.hidden" : "frame_veil.shown", frames.size());
		chime(cast.level, hit.point(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, hide ? 0.7f : 1.3f);
	}

	/** Stand Pose's poses: head, body, left arm, right arm, left leg, right leg. */
	private static final Rotations[][] POSES = {
		{new Rotations(0, 0, 0), new Rotations(0, 0, 0), new Rotations(-10, 0, -10), new Rotations(-15, 0, 10), new Rotations(-1, 0, -1), new Rotations(1, 0, 1)},
		{new Rotations(-10, 0, 0), new Rotations(0, 0, 0), new Rotations(-110, 30, 0), new Rotations(-110, -30, 0), new Rotations(0, 0, 0), new Rotations(0, 0, 0)},
		{new Rotations(15, 20, 0), new Rotations(0, 10, 0), new Rotations(20, 0, -20), new Rotations(-60, 15, 10), new Rotations(-20, 0, 0), new Rotations(20, 0, 0)},
		{new Rotations(-20, 0, 0), new Rotations(0, 0, 0), new Rotations(-170, 0, -15), new Rotations(-170, 0, 15), new Rotations(0, 0, -5), new Rotations(0, 0, 5)}};

	private static void standPose(Cast cast, Cast.Hit hit, double radius) {
		double reach = WayfarerRules.area(WayfarerRules.AREA, radius, WayfarerRules.AREA_MAX);
		int posed = 0;
		for (ArmorStand stand : cast.level.getEntitiesOfClass(ArmorStand.class, new AABB(hit.point(), hit.point()).inflate(reach))) {
			if (posed >= WayfarerRules.MAX_EDITS || !mayEdit(cast, stand.blockPosition())) {
				continue;
			}
			int current = -1;
			for (int i = 0; i < POSES.length; i++) {
				if (POSES[i][3].equals(stand.getRightArmPose())) {
					current = i;
				}
			}
			Rotations[] pose = POSES[WayfarerRules.nextPose(current)];
			stand.setShowArms(true);
			stand.setHeadPose(pose[0]);
			stand.setBodyPose(pose[1]);
			stand.setLeftArmPose(pose[2]);
			stand.setRightArmPose(pose[3]);
			stand.setLeftLegPose(pose[4]);
			stand.setRightLegPose(pose[5]);
			Fx.send(cast.level, Fx.dust(ARCANE, 0.9f), stand.position().add(0, 1.2, 0), 6, 0.3, 0.01);
			posed++;
		}
		tell(cast, posed == 0 ? "stand_pose.none" : "stand_pose", posed);
		if (posed > 0) {
			chime(cast.level, hit.point(), SoundEvents.ARMOR_STAND_PLACE, 1.2f);
		}
	}

	// ------------------------------------------------------------------ Nether and End footing

	private static void lavaCrust(Cast cast, Cast.Hit hit, double radius, double duration) {
		ServerLevel level = cast.level;
		BlockPos centre = BlockPos.containing(hit.point());
		int half = WayfarerRules.crust(radius), ticks = Effects.ticks(20, duration), crusted = 0;
		BlockState crust = Blocks.SMOOTH_BASALT.defaultBlockState();
		for (int dx = -half; dx <= half; dx++) {
			for (int dz = -half; dz <= half; dz++) {
				if (dx * dx + dz * dz > half * half + 1) {
					continue;
				}
				for (int dy = 1; dy >= -3; dy--) {
					BlockPos pos = centre.offset(dx, dy, dz);
					BlockState state = level.getBlockState(pos);
					if (!state.is(Blocks.LAVA) || level.getBlockState(pos.above()).is(Blocks.LAVA)) {
						continue;
					}
					if (mayEdit(cast, pos)) {
						temporary(cast, pos, crust, state, ticks);
						Fx.send(level, ParticleTypes.CLOUD, Vec3.atCenterOf(pos).add(0, 0.6, 0), 2, 0.3, 0.02);
						crusted++;
					}
					break;
				}
			}
		}
		if (crusted == 0) {
			tell(cast, "lava_crust.none");
			return;
		}
		chime(level, hit.point(), SoundEvents.BASALT_PLACE, 0.8f);
		Fx.sound(level, hit.point(), SoundEvents.LAVA_POP, 0.6f, 0.8f);
	}

	private static void voidStep(Cast cast, double duration) {
		ServerLevel level = cast.level;
		LivingEntity caster = cast.caster;
		BlockPos base = BlockPos.containing(caster.position()).below();
		int ticks = Effects.ticks(6, duration), placed = 0;
		BlockState stone = Blocks.END_STONE.defaultBlockState();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos pos = base.offset(dx, 0, dz);
				if (!level.getBlockState(pos).isAir() || !mayEdit(cast, pos)) {
					continue;
				}
				temporary(cast, pos, stone, Blocks.AIR.defaultBlockState(), WayfarerRules.crumble(Math.max(Math.abs(dx), Math.abs(dz)), ticks));
				Fx.send(level, ParticleTypes.REVERSE_PORTAL, Vec3.atCenterOf(pos).add(0, 0.6, 0), 3, 0.3, 0.02);
				placed++;
			}
		}
		if (placed == 0) {
			tell(cast, "void_step.none");
			return;
		}
		caster.resetFallDistance();
		Vec3 v = caster.getDeltaMovement();
		caster.setDeltaMovement(v.x, Math.max(0, v.y), v.z);
		caster.needsSync = true;
		chime(level, caster.position(), SoundEvents.END_PORTAL_FRAME_FILL, 1.1f);
	}

	private static void lavaSense(Cast cast, double radius) {
		ServerLevel level = cast.level;
		BlockPos me = cast.caster.blockPosition();
		int reach = (int) WayfarerRules.area(8, radius, 12), count = 0;
		BlockPos nearest = null;
		for (BlockPos pos : BlockPos.betweenClosed(me.offset(-reach, -reach, -reach), me.offset(reach, reach, reach))) {
			if (!level.hasChunkAt(pos) || !level.getFluidState(pos).is(FluidTags.LAVA)) {
				continue;
			}
			count++;
			if (nearest == null || pos.distSqr(me) < nearest.distSqr(me)) {
				nearest = pos.immutable();
			}
		}
		if (nearest == null) {
			tell(cast, "lava_sense.none", reach);
			chime(level, cast.caster.position(), SoundEvents.STONE_HIT, 1.2f);
			return;
		}
		int dy = nearest.getY() - me.getY();
		tell(cast, "lava_sense", count, Component.translatable("direction.wildercord."
				+ WayfarerRules.COMPASS[WayfarerRules.compass(nearest.getX() - me.getX(), nearest.getZ() - me.getZ())]),
			Math.round(Math.sqrt(nearest.distSqr(me))), (dy >= 0 ? "+" : "") + dy);
		Fx.send(level, Fx.dust(FIRE, 1.2f), cast.caster.getEyePosition().add(Vec3.atCenterOf(nearest).subtract(cast.caster.getEyePosition()).normalize()),
			6, 0.1, 0.01);
		chime(level, cast.caster.position(), SoundEvents.LAVA_POP, 1.0f);
	}

	private static void goldParley(Cast cast, double radius) {
		LivingEntity caster = cast.caster;
		double reach = WayfarerRules.area(12, radius, 16);
		int calmed = 0;
		for (Piglin piglin : cast.level.getEntitiesOfClass(Piglin.class, caster.getBoundingBox().inflate(reach))) {
			boolean angry = piglin.getBrain().getMemory(MemoryModuleType.ANGRY_AT).filter(caster.getUUID()::equals).isPresent() || piglin.getTarget() == caster;
			if (!angry || Spirits.isBoss(piglin)) {
				continue;
			}
			piglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
			piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
			piglin.setTarget(null);
			Fx.send(cast.level, Fx.dust(0xFFD24A, 1.0f), piglin.getEyePosition().add(0, 0.4, 0), 6, 0.3, 0.01);
			calmed++;
		}
		tell(cast, calmed == 0 ? "gold_parley.none" : "gold_parley", calmed);
		chime(cast.level, caster.position(), SoundEvents.PIGLIN_ADMIRING_ITEM, 1.0f);
	}

	// ------------------------------------------------------------------ helpers

	private static void tell(Cast cast, String key, Object... args) {
		Casters.tell(cast.caster, Component.translatable("message.wildercord.wayfarer." + key, args));
	}

	private static void chime(ServerLevel level, Vec3 at, SoundEvent sound, float pitch) {
		Fx.sound(level, at, sound, 0.7f, pitch);
	}

	private static void mark(ServerLevel level, Vec3 at, int color, int count) {
		Fx.send(level, Fx.dust(color, 1.2f), at, count, 0.3, 0.01);
	}

	/** The block a reading stands on: the one hit, or the one under the point (under your feet for Self). */
	private static BlockPos ground(Cast.Hit hit) {
		return hit.block() != null ? hit.block() : BlockPos.containing(hit.point()).below();
	}

	/** May the cast change this block: the caster may build here, nothing protects it, and the cast's block budget has room. */
	private static boolean mayEdit(Cast cast, BlockPos pos) {
		return Casters.mayBuild(cast.caster) && Casters.mayEdit(cast.caster, cast.level, pos) && cast.takeBlock();
	}

	/** The positions within {@code reach} of where it landed, nearest first. */
	private static List<BlockPos> around(Cast.Hit hit, double reach) {
		BlockPos c = hit.block() != null ? hit.block() : BlockPos.containing(hit.point());
		int r = (int) Math.ceil(reach);
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
			if (p.distSqr(c) <= reach * reach) {
				out.add(p.immutable());
			}
		}
		out.sort(Comparator.comparingDouble(p -> p.distSqr(c)));
		return out;
	}

	/** The loaded block entities of a kind within {@code reach} of {@code at}, nearest first, at most {@code cap}. */
	static <T extends BlockEntity> List<T> blockEntities(ServerLevel level, Vec3 at, double reach, Class<T> type, int cap) {
		List<T> out = new ArrayList<>();
		int x0 = ((int) Math.floor(at.x - reach)) >> 4, x1 = ((int) Math.floor(at.x + reach)) >> 4;
		int z0 = ((int) Math.floor(at.z - reach)) >> 4, z1 = ((int) Math.floor(at.z + reach)) >> 4;
		for (int cx = x0; cx <= x1; cx++) {
			for (int cz = z0; cz <= z1; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (type.isInstance(entity) && Vec3.atCenterOf(entity.getBlockPos()).distanceToSqr(at) <= reach * reach) {
						out.add(type.cast(entity));
					}
				}
			}
		}
		out.sort(Comparator.comparingDouble(e -> Vec3.atCenterOf(e.getBlockPos()).distanceToSqr(at)));
		return out.size() > cap ? new ArrayList<>(out.subList(0, cap)) : out;
	}

	/** Puts a passing block up, written down so it goes back on time even across a restart, and drops nothing. */
	private static void temporary(Cast cast, BlockPos pos, BlockState placed, BlockState replaced, int ticks) {
		ServerLevel level = cast.level;
		BlockPos at = pos.immutable();
		level.setBlock(at, placed, Block.UPDATE_ALL);
		TemporaryBlocks.put(level, at, placed, replaced, level.getGameTime() + ticks);
		Scheduler.later(ticks, () -> {
			if (level.getBlockState(at).is(placed.getBlock())) {
				TemporaryBlocks.remove(level, at);
				level.setBlock(at, replaced, Block.UPDATE_ALL);
			}
		});
	}

	/** A player left: their Thrift goes; their locate rest only once it has run, so a relog never skips it. */
	static void forget(UUID player, long now) {
		THRIFT.remove(player);
		Long last = LOCATED.get(player);
		if (last != null && !WayfarerRules.resting(last, now)) {
			LOCATED.remove(player);
		}
		dev.wildercord.spell.StatePrune.rested(LOCATED, now, WayfarerRules.LOCATE_REST);
	}
}
