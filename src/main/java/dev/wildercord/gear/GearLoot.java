package dev.wildercord.gear;

import dev.wildercord.config.Config;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Where casting gear is found. Staffs are crafted; the Tome of the Fifth Page and the foci are found
 * in the Archive (its vault and library) and a few of the world's old libraries; greater staffs come
 * from bosses. Every chance is scaled by the server's loot.gear_chance_multiplier.
 */
public final class GearLoot {
	private GearLoot() {}

	/** Chest tables: the chance (out of 100) of one piece, and which pieces. */
	private record Pool(int chance, List<GearDef> pieces) {}

	private static final Map<ResourceKey<LootTable>, Pool> CHESTS = new HashMap<>();
	/** Bosses: the chance (out of 100) of a greater staff, and its elements. */
	private static final Map<EntityType<?>, Pool> BOSSES = new HashMap<>();

	/** The Archive's chests (as in WildercordWorldgen, named here so loading this class doesn't set up worldgen early). */
	private static final ResourceKey<LootTable> ARCHIVE_VAULT = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
		dev.wildercord.Wildercord.id("chests/archive_vault"));
	private static final ResourceKey<LootTable> ARCHIVE_LIBRARY = ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
		dev.wildercord.Wildercord.id("chests/archive_library"));

	/** The Archivist's chance of a greater staff (out of 100). */
	public static final int ARCHIVIST_CHANCE = 50;

	static {
		List<GearDef> foci = List.of(GearDef.HASTE, GearDef.THRIFT, GearDef.DEEP_WELL, GearDef.ECHOES, GearDef.RESOLVE);
		List<GearDef> tomeAndFoci = List.of(GearDef.TOME, GearDef.HASTE, GearDef.THRIFT, GearDef.DEEP_WELL, GearDef.ECHOES, GearDef.RESOLVE);
		java.util.ArrayList<GearDef> vault = new java.util.ArrayList<>(tomeAndFoci);
		GearDef.ELEMENTS.forEach(e -> vault.add(GearDef.greaterStaff(e)));
		CHESTS.put(ARCHIVE_VAULT, new Pool(45, List.copyOf(vault)));
		CHESTS.put(ARCHIVE_LIBRARY, new Pool(20, tomeAndFoci));
		CHESTS.put(BuiltInLootTables.STRONGHOLD_LIBRARY, new Pool(10, tomeAndFoci));
		CHESTS.put(BuiltInLootTables.ANCIENT_CITY, new Pool(12, foci));
		CHESTS.put(BuiltInLootTables.WOODLAND_MANSION, new Pool(8, foci));
		BOSSES.put(EntityTypes.WITHER, new Pool(35, List.of(GearDef.greaterStaff("void"), GearDef.greaterStaff("blood"))));
		BOSSES.put(EntityTypes.WARDEN, new Pool(35, List.of(GearDef.greaterStaff("earth"), GearDef.greaterStaff("storm"))));
		BOSSES.put(EntityTypes.ELDER_GUARDIAN, new Pool(35, List.of(GearDef.greaterStaff("frost"), GearDef.greaterStaff("life"))));
	}

	/** The Ender Dragon's greater staffs, dropped by the killer (it has no loot table). */
	private static final Pool DRAGON = new Pool(50, List.of(GearDef.greaterStaff("void"), GearDef.greaterStaff("arcane"), GearDef.greaterStaff("time")));

	public static void init() {
		Map<ResourceKey<LootTable>, Pool> mobTables = new HashMap<>();
		BOSSES.forEach((type, pool) -> type.getDefaultLootTable().ifPresent(key -> mobTables.put(key, pool)));
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			if (!source.isBuiltin()) {
				return;
			}
			Pool pool = CHESTS.containsKey(key) ? CHESTS.get(key) : mobTables.get(key);
			if (pool == null) {
				return;
			}
			int chance = dev.wildercord.config.WildercordConfig.scaledChance(pool.chance(), Config.get().gearLootChance());
			if (chance <= 0) {
				return;
			}
			LootPool.Builder builder = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1));
			// The chance is shared out evenly, ten weight per point so small shares still round well.
			int each = Math.max(1, Math.round(chance * 10.0F / pool.pieces().size()));
			for (GearDef piece : pool.pieces()) {
				builder.add(LootItem.lootTableItem(GearItems.get(piece)).setWeight(each));
			}
			if (chance < 100) {
				builder.add(EmptyLootItem.emptyItem().setWeight((100 - chance) * 10));
			}
			table.withPool(builder);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof EnderDragon && entity.level() instanceof ServerLevel level) {
				net.minecraft.world.entity.Entity at = source.getEntity() instanceof net.minecraft.world.entity.player.Player killer && killer.level() == level
					? killer : level.getNearestPlayer(entity, 256);
				roll(level.getRandom(), DRAGON.chance(), DRAGON.pieces()).ifPresent(stack ->
					drop(level, (at == null ? entity : at).position().add(0, 0.5, 0), stack));
			}
		});
	}

	/**
	 * A boss's greater staff, if the roll allows: the Archivist's, and any boss another feature adds
	 * (call it from the boss's death loot).
	 *
	 * @param chance   out of 100, before the server's multiplier
	 * @param elements the staffs' elements to pick from
	 */
	public static Optional<ItemStack> bossStaff(RandomSource random, int chance, List<String> elements) {
		return roll(random, chance, elements.stream().map(GearDef::greaterStaff).toList());
	}

	private static Optional<ItemStack> roll(RandomSource random, int chance, List<GearDef> pieces) {
		int scaled = dev.wildercord.config.WildercordConfig.scaledChance(chance, Config.get().gearLootChance());
		if (pieces.isEmpty() || random.nextInt(100) >= scaled) {
			return Optional.empty();
		}
		return Optional.of(new ItemStack(GearItems.get(pieces.get(random.nextInt(pieces.size())))));
	}

	private static void drop(ServerLevel level, Vec3 at, ItemStack stack) {
		ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
		item.setGlowingTag(true);
		item.setUnlimitedLifetime();
		level.addFreshEntity(item);
	}
}
