package dev.wildercord.content;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where magic is found. Structure chests get one extra roll for a rune (and some for a Mana
 * Crystal); mobs and bosses drop the runes that fit them. Only vanilla's own tables are
 * touched, so a datapack that replaces a table keeps full control of it.
 */
public final class WildercordLoot {
	private WildercordLoot() {}

	/** A chance (out of 100) of one rune, picked evenly from the list. */
	private record RunePool(int chance, List<RuneDef> runes) {}

	private static final Map<ResourceKey<LootTable>, Integer> CRYSTAL_CHANCE = Map.of(
		BuiltInLootTables.ANCIENT_CITY, 30,
		BuiltInLootTables.END_CITY_TREASURE, 25,
		BuiltInLootTables.STRONGHOLD_LIBRARY, 30,
		BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE, 25,
		BuiltInLootTables.BURIED_TREASURE, 20,
		BuiltInLootTables.BASTION_TREASURE, 25,
		BuiltInLootTables.WOODLAND_MANSION, 20
	);

	private static final Map<ResourceKey<LootTable>, RunePool> RUNE_POOLS = new HashMap<>();

	static {
		List<RuneDef> common = List.of(Runes.TOUCH, Runes.FEATHER_FALL, Runes.SWIFT, Runes.NIGHT_EYE, Runes.HEAL, Runes.HARM, Runes.LIGHT, Runes.GROW,
			Runes.AMPLIFY, Runes.EXTEND, Runes.DELAY, Runes.ARC, Runes.SHOCK, Runes.HASTE, Runes.REVEAL, Runes.FRUGAL_MOD,
			Runes.BLIND, Runes.CHILL, Runes.NOURISH, Runes.TIDEBREATH, Runes.LEAP, Runes.HARVEST, Runes.ICEPATH, Runes.COLLECT,
			Runes.RAMPART, Runes.WEIGH, Runes.AFTERSHOCK, Runes.SWAP);
		List<RuneDef> uncommon = List.of(Runes.BEAM, Runes.BURST, Runes.CONE, Runes.TRAIL, Runes.SHIELD, Runes.LAUNCH, Runes.DASH, Runes.PULL, Runes.FIRE,
			Runes.FROST, Runes.BREAK, Runes.REGROWTH, Runes.CLEANSE, Runes.STONESKIN, Runes.ROOT, Runes.VEIL, Runes.EMPOWER, Runes.LEVITATE,
			Runes.WIDEN, Runes.QUICKEN, Runes.PIERCE_MOD, Runes.BOUNCE_MOD, Runes.LINGER_MOD, Runes.VOLLEY_MOD, Runes.ON_HIT, Runes.ON_LAND,
			Runes.PULSE, Runes.ON_HURT, Runes.RING, Runes.PILLAR, Runes.WAVE, Runes.MINE, Runes.VENOM, Runes.THUNDERCLAP, Runes.SILENCE,
			Runes.FIREWARD, Runes.GRAPPLE, Runes.EXCAVATE, Runes.FOCUS_MOD, Runes.RAPID_MOD, Runes.IF_SNEAKING,
			Runes.CRESCENT, Runes.BARRAGE, Runes.BLITZ, Runes.DISMANTLE, Runes.RIPPLE, Runes.REPEL, Runes.DECREE, Runes.SHACKLE, Runes.BUBBLE,
			Runes.OVERDRIVE, Runes.ZIPPER, Runes.EXECUTE_MOD, Runes.IF_AIRBORNE, Runes.ACCELERATE, Runes.FORESIGHT);
		RUNE_POOLS.put(BuiltInLootTables.SIMPLE_DUNGEON, new RunePool(35, common));
		RUNE_POOLS.put(BuiltInLootTables.ABANDONED_MINESHAFT, new RunePool(25, common));
		RUNE_POOLS.put(BuiltInLootTables.SHIPWRECK_TREASURE, new RunePool(30, uncommon));
		RUNE_POOLS.put(BuiltInLootTables.BURIED_TREASURE, new RunePool(40, uncommon));
		RUNE_POOLS.put(BuiltInLootTables.DESERT_PYRAMID, new RunePool(35, List.of(Runes.EXPLODE, Runes.METEOR, Runes.FIRE, Runes.CONE, Runes.BURST, Runes.INFERNO,
			Runes.PRIMER)));
		RUNE_POOLS.put(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON, new RunePool(30, uncommon));
		RUNE_POOLS.put(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE, new RunePool(45,
			List.of(Runes.ZONE, Runes.SPLIT_MOD, Runes.CHAIN_MOD, Runes.WALL, Runes.ORBIT, Runes.FREEZE, Runes.VOLLEY_MOD, Runes.ON_HURT,
				Runes.COMBO, Runes.REFLECT, Runes.PRIMER, Runes.STAND)));
		RUNE_POOLS.put(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE, new RunePool(70,
			List.of(Runes.ZONE, Runes.SPLIT_MOD, Runes.CHAIN_MOD, Runes.WALL, Runes.ORBIT, Runes.FREEZE, Runes.TREMOR, Runes.GRAVITY_WELL,
				Runes.TOTEM, Runes.OVERCHARGE_MOD, Runes.ORB, Runes.BLACKSPARK, Runes.VOW_MOD, Runes.INFINITY, Runes.REVERSAL)));
		RUNE_POOLS.put(BuiltInLootTables.ANCIENT_CITY, new RunePool(45,
			List.of(Runes.ZONE, Runes.SPLIT_MOD, Runes.CHAIN_MOD, Runes.ECHO, Runes.FREEZE, Runes.GRAVITY_WELL, Runes.VEIL,
				Runes.ON_LOW_HEALTH, Runes.SMITE, Runes.STAND, Runes.SHADES, Runes.SHADOWSTEP, Runes.DOMAIN, Runes.VOW_MOD, Runes.BLACKFLAME)));
		RUNE_POOLS.put(BuiltInLootTables.END_CITY_TREASURE, new RunePool(45,
			List.of(Runes.RAIN, Runes.HOMING_MOD, Runes.BLINK, Runes.LEVITATE, Runes.GRAVITY_WELL, Runes.ORBIT, Runes.ORB, Runes.TIME_SKIP,
				Runes.STASIS, Runes.REWIND)));
		RUNE_POOLS.put(BuiltInLootTables.STRONGHOLD_LIBRARY, new RunePool(50,
			List.of(Runes.RAIN, Runes.HOMING_MOD, Runes.WALL, Runes.PULSE, Runes.ECHO, Runes.LINGER_MOD, Runes.SMITE, Runes.TOTEM,
				Runes.RESONANCE, Runes.FORESIGHT, Runes.RESTORE, Runes.STAND)));
		RUNE_POOLS.put(BuiltInLootTables.BASTION_TREASURE, new RunePool(45,
			List.of(Runes.ON_KILL, Runes.EXPLODE, Runes.METEOR, Runes.TREMOR, Runes.EMPOWER, Runes.INFERNO, Runes.OVERCHARGE_MOD,
				Runes.CLEAVE, Runes.BLACKSPARK, Runes.BLACKFLAME, Runes.OVERDRIVE, Runes.BLOOD_PRICE_MOD)));
		RUNE_POOLS.put(BuiltInLootTables.WOODLAND_MANSION, new RunePool(40,
			List.of(Runes.ON_KILL, Runes.VEIL, Runes.PULSE, Runes.ORBIT, Runes.CLEAVE, Runes.RESONANCE, Runes.SHADOWSTEP, Runes.BLOOD_PRICE_MOD)));
		RUNE_POOLS.put(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE, new RunePool(20, List.of(Runes.LIGHTNING, Runes.SHOCK)));
	}

	/** Mob drops: chance out of 100, and the rune. Bosses always drop their first. */
	private static final Map<EntityType<?>, List<Map.Entry<Integer, RuneDef>>> MOB_DROPS = Map.ofEntries(
		Map.entry(EntityTypes.CREEPER, List.of(Map.entry(1, Runes.EXPLODE))),
		Map.entry(EntityTypes.ENDERMAN, List.of(Map.entry(2, Runes.BLINK), Map.entry(1, Runes.TIME_SKIP))),
		Map.entry(EntityTypes.SHULKER, List.of(Map.entry(5, Runes.HOMING_MOD))),
		Map.entry(EntityTypes.EVOKER, List.of(Map.entry(15, Runes.SUMMON), Map.entry(10, Runes.DECREE))),
		Map.entry(EntityTypes.VINDICATOR, List.of(Map.entry(4, Runes.CLEAVE))),
		Map.entry(EntityTypes.WITCH, List.of(Map.entry(5, Runes.BUBBLE))),
		Map.entry(EntityTypes.PHANTOM, List.of(Map.entry(5, Runes.IF_AIRBORNE))),
		Map.entry(EntityTypes.BREEZE, List.of(Map.entry(8, Runes.REPEL))),
		Map.entry(EntityTypes.WARDEN, List.of(Map.entry(100, Runes.SONIC_BOOM), Map.entry(35, Runes.DOMAIN))),
		Map.entry(EntityTypes.WITHER, List.of(Map.entry(100, Runes.WITHER), Map.entry(50, Runes.HOLLOW))),
		Map.entry(EntityTypes.ELDER_GUARDIAN, List.of(Map.entry(100, Runes.STARFALL), Map.entry(25, Runes.STASIS)))
	);

	public static void init() {
		Map<ResourceKey<LootTable>, List<Map.Entry<Integer, RuneDef>>> mobTables = new HashMap<>();
		MOB_DROPS.forEach((type, drop) -> type.getDefaultLootTable().ifPresent(key -> mobTables.put(key, drop)));

		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			if (!source.isBuiltin()) {
				return;
			}
			Integer crystal = CRYSTAL_CHANCE.get(key);
			if (crystal != null) {
				table.withPool(chance(crystal, LootItem.lootTableItem(WildercordItems.MANA_CRYSTAL)));
			}
			RunePool pool = RUNE_POOLS.get(key);
			if (pool != null) {
				LootPool.Builder builder = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1));
				// The pool's chance of a rune is shared out by tier: high tiers are much rarer finds.
				int total = pool.chance() * 10;
				int weights = pool.runes().stream().mapToInt(r -> tierWeight(r.tier())).sum();
				for (RuneDef rune : pool.runes()) {
					builder.add(runeEntry(rune).setWeight(Math.max(1, Math.round((float) total * tierWeight(rune.tier()) / weights))));
				}
				builder.add(EmptyLootItem.emptyItem().setWeight(Math.max(1, (100 - pool.chance()) * 10)));
				table.withPool(builder);
			}
			for (Map.Entry<Integer, RuneDef> drop : mobTables.getOrDefault(key, List.of())) {
				table.withPool(chance(drop.getKey(), runeEntry(drop.getValue())));
			}
		});

		// The Ender Dragon has no loot table: its runes are dropped where it dies.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof EnderDragon && entity.level() instanceof ServerLevel level) {
				for (RuneDef rune : List.of(Runes.DRAGON_BREATH, Runes.INFINITY)) {
					ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), RuneItem.stack(rune));
					drop.setGlowingTag(true);
					drop.setUnlimitedLifetime();
					level.addFreshEntity(drop);
				}
			}
		});
	}

	/** How likely a rune of this tier is next to the others in its pool: Tier I 8, II 5, III 2, IV 1. */
	static int tierWeight(int tier) {
		return switch (tier) {
			case 1 -> 8;
			case 2 -> 5;
			case 3 -> 2;
			default -> 1;
		};
	}

	private static net.minecraft.world.level.storage.loot.entries.UniformContainerBase.Builder<?> runeEntry(RuneDef rune) {
		return LootItem.lootTableItem(WildercordItems.RUNE).apply(SetComponentsFunction.setComponent(WildercordComponents.RUNE, rune.id()));
	}

	private static LootPool.Builder chance(int chance, net.minecraft.world.level.storage.loot.entries.UniformContainerBase.Builder<?> entry) {
		LootPool.Builder builder = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1)).add(entry.setWeight(chance));
		if (chance < 100) {
			builder.add(EmptyLootItem.emptyItem().setWeight(100 - chance));
		}
		return builder;
	}
}
