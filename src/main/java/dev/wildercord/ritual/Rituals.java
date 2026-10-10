package dev.wildercord.ritual;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.monster.MonsterContent;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.ritual.RitualRules.Ritual;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClock;

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

	/** A Sanctuary being held: where, in which world, and until when. */
	private record Ward(ResourceKey<Level> level, Vec3 center, long until) {}

	private static final List<Ward> WARDS = new ArrayList<>();

	private static Item item(String id, java.util.function.Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id(id));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}

	public static void init() {
		ResourceKey<CreativeModeTab> tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord"));
		CreativeModeTabEvents.modifyOutputEvent(tab).register(output -> output.accept(RITUAL_TABLET));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> WARDS.clear());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (WARDS.isEmpty() || server.getTickCount() % 10 != 0) return;
			for (Iterator<Ward> it = WARDS.iterator(); it.hasNext(); ) {
				Ward ward = it.next();
				ServerLevel level = server.getLevel(ward.level());
				if (level == null || level.getGameTime() >= ward.until()) {
					it.remove();
					continue;
				}
				hold(level, ward.center());
			}
		});
	}

	public static Ritual ritual(ItemStack stack) {
		return Ritual.byId(stack.getOrDefault(RITUAL, Ritual.BOUNTY.id));
	}

	public static void setRitual(ItemStack stack, Ritual ritual) {
		stack.set(RITUAL, ritual.id);
	}

	/** The reagent a ritual consumes. */
	public static Item reagent(Ritual ritual) {
		return switch (ritual) {
			case BOUNTY -> Items.BONE_BLOCK;
			case CLEAR_SKIES -> Items.SUNFLOWER;
			case CALL_STORM -> MonsterContent.STORM_FEATHER;
			case SANCTUARY -> WildercordItems.MANA_CRYSTAL;
			case DAWN -> Items.GLOWSTONE;
		};
	}

	/** The casters sharing the cost: the one channelling first, then every other Cord-wearer close by. */
	public static List<ServerPlayer> circle(ServerLevel level, ServerPlayer leader) {
		List<ServerPlayer> circle = new ArrayList<>();
		circle.add(leader);
		for (ServerPlayer other : level.players()) {
			if (other != leader && other.isAlive() && !other.isSpectator() && Spellbooks.tier(other) != null
				&& other.distanceTo(leader) <= RitualRules.CIRCLE_RANGE) {
				circle.add(other);
			}
		}
		return circle;
	}

	/** Whether {@code ritual} could be worked here and now by {@code leader}, without working it. */
	public static Result check(ServerLevel level, ServerPlayer leader, Ritual ritual) {
		if (Spellbooks.tier(leader) == null) return Result.NO_CORD;
		if (!RitualRules.canLead(ritual, Heart.circles(leader))) return Result.LOW_CIRCLE;
		boolean sky = level.dimension() == Level.OVERWORLD;
		if ((ritual == Ritual.CLEAR_SKIES || ritual == Ritual.CALL_STORM || ritual == Ritual.DAWN) && !sky) return Result.WRONG_PLACE;
		if (ritual == Ritual.DAWN && level.isBrightOutside()) return Result.WRONG_PLACE;
		if (!leader.isCreative() && leader.getInventory().findSlotMatchingItem(new ItemStack(reagent(ritual))) < 0) return Result.NO_REAGENT;
		if (!leader.isCreative() && split(level, leader, ritual) == null) return Result.LOW_MANA;
		return Result.DONE;
	}

	private static float[] split(ServerLevel level, ServerPlayer leader, Ritual ritual) {
		List<ServerPlayer> circle = circle(level, leader);
		float[] mana = new float[circle.size()];
		for (int i = 0; i < mana.length; i++) mana[i] = Spellbooks.mana(circle.get(i));
		return RitualRules.split(mana, ritual.cost);
	}

	/** Works {@code ritual}, paying for it from the circle and the reagent from the leader's pack. */
	public static Result perform(ServerLevel level, ServerPlayer leader, Ritual ritual) {
		Result result = check(level, leader, ritual);
		if (result != Result.DONE) return result;
		boolean worked = switch (ritual) {
			case BOUNTY -> bounty(level, leader.blockPosition()) > 0;
			case CLEAR_SKIES -> weather(level, false);
			case CALL_STORM -> weather(level, true);
			case SANCTUARY -> sanctuary(level, leader.position());
			case DAWN -> dawn(level);
		};
		if (!worked) return Result.NOTHING_TO_DO;
		if (!leader.isCreative()) {
			List<ServerPlayer> circle = circle(level, leader);
			float[] paid = split(level, leader, ritual);
			for (int i = 0; paid != null && i < paid.length; i++) {
				ServerPlayer caster = circle.get(i);
				Spellbooks.setMana(caster, Spellbooks.mana(caster) - paid[i]);
				if (caster != leader) {
					caster.sendOverlayMessage(Component.translatable("message.wildercord.ritual.shared", (int) paid[i]).withColor(0xB8A8FF));
				}
			}
			int slot = leader.getInventory().findSlotMatchingItem(new ItemStack(reagent(ritual)));
			if (slot >= 0) leader.getInventory().removeItem(slot, 1);
		}
		level.playSound(null, leader.getX(), leader.getY(), leader.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
		level.sendParticles(ParticleTypes.END_ROD, leader.getX(), leader.getY() + 1.5, leader.getZ(), 40, 1.2, 1.0, 1.2, 0.08);
		Component name = Component.translatable("ritual.wildercord." + ritual.id);
		for (ServerPlayer caster : circle(level, leader)) {
			caster.sendSystemMessage(Component.translatable("message.wildercord.ritual.done", name).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		return Result.DONE;
	}

	/** Bounty: every crop within reach grows up to three stages. Returns how many crops grew. */
	public static int bounty(ServerLevel level, BlockPos center) {
		int grown = 0;
		int r = RitualRules.BOUNTY_RADIUS, h = RitualRules.BOUNTY_HEIGHT;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -h, -r), center.offset(r, h, r))) {
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof CropBlock || state.getBlock() instanceof StemBlock
				|| state.getBlock() instanceof SweetBerryBushBlock || state.getBlock() instanceof CocoaBlock)) continue;
			BlockPos at = pos.immutable();
			for (int i = 0; i < RitualRules.BOUNTY_STAGES; i++) {
				if (!BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, at)) break;
			}
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.getX() + 0.5, at.getY() + 0.6, at.getZ() + 0.5, 3, 0.3, 0.2, 0.3, 0.0);
			grown++;
		}
		return grown;
	}

	/** Clear Skies and Call Storm. */
	public static boolean weather(ServerLevel level, boolean storm) {
		WeatherData weather = level.getWeatherData();
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
		weather.setDirty();
		return true;
	}

	/** Dawn: the night ends now. */
	public static boolean dawn(ServerLevel level) {
		Holder<WorldClock> clock = level.dimensionType().defaultClock().orElse(null);
		if (clock == null) return false;
		level.clockManager().moveToTimeMarker(clock, ClockTimeMarkers.DAY);
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
			m -> m instanceof Enemy && m.isAlive() && !(m instanceof net.minecraft.world.entity.boss.wither.WitherBoss)
				&& !(m instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)
				&& m.position().distanceTo(center) <= r);
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
			mob.syncVelocity = true;
		}
		// The ward's edge, a few motes at a time.
		for (int i = 0; i < 8; i++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2;
			level.sendParticles(ParticleTypes.END_ROD, center.x + Math.cos(angle) * RitualRules.SANCTUARY_RADIUS, center.y + 0.5,
				center.z + Math.sin(angle) * RitualRules.SANCTUARY_RADIUS, 1, 0, 0.3, 0, 0.0);
		}
	}
}
