package dev.wildercord.ritual;

import dev.wildercord.compat.McSavedDataType;
import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.monster.MonsterContent;
import dev.wildercord.party.Parties;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Inventories;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.ritual.RitualRules.Ritual;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Ritual spells: the tablet, the reagents, and what each ritual does. See {@link RitualRules}. */
public final class Rituals {
	private Rituals() {}

	/** The ritual a tablet is set to, by id. */
	public static final DataComponentType<String> RITUAL = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Wildercord.id("ritual"),
		DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

	public static final Item RITUAL_TABLET = item("ritual_tablet", p -> new RitualTabletItem(p.stacksTo(1).rarity(Rarity.UNCOMMON)));

	/** Why a ritual did or didn't take. */
	public enum Result { DONE, NO_CORD, LOW_CIRCLE, NO_REAGENT, LOW_MANA, WRONG_PLACE, NOTHING_TO_DO }

	/** A Sanctuary being held: where, in which world, and until when (that world's game time, which outlasts a restart). */
	private record Ward(ResourceKey<Level> level, Vec3 center, long until) {
		static final Codec<Ward> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Ward::level),
			Vec3.CODEC.fieldOf("center").forGetter(Ward::center),
			Codec.LONG.fieldOf("until").forGetter(Ward::until)
		).apply(i, Ward::new));
	}

	/** The Sanctuaries held when the server last stopped, taken up again when it starts. */
	public static final class Sanctuaries extends net.minecraft.world.level.saveddata.SavedData {
		static final Codec<Sanctuaries> CODEC = Ward.CODEC.listOf().optionalFieldOf("wards", List.of()).xmap(Sanctuaries::new, s -> s.wards).codec();
		static final dev.wildercord.compat.McSavedDataType<Sanctuaries> TYPE =
			new dev.wildercord.compat.McSavedDataType<>("sanctuaries", Sanctuaries::new, CODEC);

		@Override
		public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
			return (net.minecraft.nbt.CompoundTag) CODEC.encodeStart(
				net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, registries), this)
				.result().orElseThrow();
		}

		private List<Ward> wards;

		public Sanctuaries() {
			this(List.of());
		}

		private Sanctuaries(List<Ward> wards) {
			this.wards = List.copyOf(wards);
		}

		void set(List<Ward> wards) {
			this.wards = List.copyOf(wards);
			setDirty();
		}
	}

	private static Sanctuaries sanctuaries(net.minecraft.server.MinecraftServer server) {
		return dev.wildercord.compat.McSavedData.computeIfAbsent(server.overworld().getDataStorage(), Sanctuaries.TYPE);
	}

	/** The server is stopping: the Sanctuaries still held are saved for {@link #resumeWards}. */
	public static void suspendWards(net.minecraft.server.MinecraftServer server) {
		List<Ward> held = new ArrayList<>();
		for (Ward ward : WARDS) {
			ServerLevel level = server.getLevel(ward.level());
			if (level != null && level.getGameTime() < ward.until()) held.add(ward);
		}
		WARDS.clear();
		sanctuaries(server).set(held);
	}

	/** The server has started: the Sanctuaries saved by {@link #suspendWards} are held again for the time they had left. */
	public static void resumeWards(net.minecraft.server.MinecraftServer server) {
		Sanctuaries saved = sanctuaries(server);
		if (saved.wards.isEmpty()) return;
		WARDS.addAll(saved.wards);
		saved.set(List.of());
	}

	/** How many Sanctuaries are held now. */
	public static int wards() {
		return WARDS.size();
	}

	private static final List<Ward> WARDS = new ArrayList<>();

	private static Item item(String id, java.util.function.Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties() + 0.5, 3, 0.3, 0.2, 0.3, 0.0);
			grown++;
		}
		return grown;
	}

	/** Clear Skies and Call Storm. */
	public static boolean weather(ServerLevel level, boolean storm) {
		ServerLevelData weather = (ServerLevelData) level.getLevelData();
		if (storm) {
			weather.setClearWeatherTime(0);
			weather.setRaining(true);
			weather.setRainTime(RitualRules.STORM_TICKS);
			weather.setThundering(true);
			weather.setThunderTime(RitualRules.STORM_TICKS);
		} else {
			weather.setClearWeatherTime(RitualRules.CLEAR_TICKS);
			weather.setRaining(false);
			weather.setRainTime(0);
			weather.setThundering(false);
			weather.setThunderTime(0);
		}
		return true;
	}

	/** Dawn: the night ends now. */
	public static boolean dawn(ServerLevel level) {
		long day = level.getDayTime();
		level.setDayTime(day - Math.floorMod(day, 24000L) + 1000L);
		return true;
	}

	/** Sanctuary: a ward against monsters for five minutes. */
	public static boolean sanctuary(ServerLevel level, Vec3 center) {
		WARDS.add(new Ward(level.dimension(), center, level.getGameTime() + RitualRules.SANCTUARY_TICKS));
		for (Mob mob : warded(level, center)) {
			mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, RitualRules.SANCTUARY_TICKS, 1));
		}
		hold(level, center);
		return true;
	}

	/** Lifts every Sanctuary at once. */
	public static void liftWards() {
		WARDS.clear();
	}

	/** Whether {@code pos} lies inside a Sanctuary being held. */
	public static boolean warded(Level level, Vec3 pos) {
		long now = level.getGameTime();
		for (Ward ward : WARDS) {
			if (ward.level() == level.dimension() && now < ward.until() && ward.center().distanceTo(pos) <= RitualRules.SANCTUARY_RADIUS) return true;
		}
		return false;
	}

	private static List<Mob> warded(ServerLevel level, Vec3 center) {
		double r = RitualRules.SANCTUARY_RADIUS;
		return level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(center, center).inflate(r),
			m -> m instanceof Enemy && m.isAlive() && !held(m)
				&& m.position().distanceTo(center) <= r);
	}

	/** A Sanctuary turns aside the common monsters: no boss, and no Riftcaller, is pushed out of its own fight. */
	private static boolean held(Mob mob) {
		return dev.wildercord.cast.Spirits.isBoss(mob) || mob.getTags().contains(dev.wildercord.cast.events.RiftSiege.RIFTCALLER_TAG);
	}

	/** Monsters inside the ward forget who they were after and are pushed back towards its edge. */
	private static void hold(ServerLevel level, Vec3 center) {
		for (Mob mob : warded(level, center)) {
			if (mob.getTarget() instanceof Player) mob.setTarget(null);
			Vec3 away = mob.position().subtract(center);
			Vec3 flat = new Vec3(away.x, 0, away.z);
			if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(1, 0, 0);
			flat = flat.normalize().scale(0.6);
			mob.push(flat.x, 0.1, flat.z);
			dev.wildercord.compat.McEntities.syncVelocity(mob);
		}
		// The ward's edge, a few motes at a time.
		for (int i = 0; i < 8; i++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2;
			level.sendParticles(ParticleTypes.END_ROD, center.x + Math.cos(angle) * RitualRules.SANCTUARY_RADIUS, center.y + 0.5,
				center.z + Math.sin(angle) * RitualRules.SANCTUARY_RADIUS, 1, 0, 0.3, 0, 0.0);
		}
	}
}
