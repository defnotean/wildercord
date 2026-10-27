package dev.wildercord.aura.world;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraSlash;
import dev.wildercord.config.Config;
import dev.wildercord.gear.Gear;
import dev.wildercord.gear.GearDef;
import dev.wildercord.gear.GearSlot;
import dev.wildercord.gear.GearSlots;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Aura-forged gear: three weapons a smithing table makes from a diamond or netherite one (with an Aura Shard from a fallen
 * knight and a reagent of the world), each still the weapon it was (its tier, its enchantments, its durability) but carrying a
 * forging, the {@code wildercord:aura_forged} component, that does one thing for aura:
 * <ul>
 *   <li><b>Lumenedge</b> (a sword, with a Lumen Antler): more aura from every blow;</li>
 *   <li><b>Skyrend Glaive</b> (a spear, with a Fulgurite Shard): a stronger slash that flies further and wider, through more foes;</li>
 *   <li><b>Bulwark Maul</b> (an axe, with Geode Grit): a cheaper guard.</li>
 * </ul>
 * And the <b>Breath Sash</b>, worn in the gear tray's tome slot (casting gear, {@link GearDef#BREATH_SASH}): more aura held, and
 * a breathing stance that settles sooner and draws in more.
 *
 * <p>Everything here reads the server's {@code aura_world} settings (on a client, what it was sent), so a server can tune or
 * switch the effects without the items going anywhere.</p>
 */
public final class ForgedGear {
	private ForgedGear() {}

	/** Which forging a weapon carries, by id ({@link AuraWorldRules.Forged}). */
	public static final DataComponentType<String> FORGED = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Wildercord.id("aura_forged"),
		DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());

	private static final DustParticleOptions FETCH_MINT = new DustParticleOptions(0x7CFFB2, 1.4F);

	public static void init() {
		// Lumenedge & Mojito's Fetchblade: a blow's aura, more of it. The sash: the stance's breath, more of it.
		AuraApi.onGain((player, amount, source) -> {
			if (!Config.forgedGear(player)) {
				return amount;
			}
			double a = amount;
			AuraWorldRules.Forged main = held(player);
			if (source.equals("hit") && main == AuraWorldRules.Forged.LUMENEDGE) {
				a *= Config.get().auraWorld().lumenedgeGain();
			} else if (source.equals("hit") && main == AuraWorldRules.Forged.FETCHBLADE) {
				a *= AuraWorldRules.FETCHBLADE_GAIN;
			}
			if ((source.equals("stance") || source.equals("beat")) && sash(player)) {
				a *= AuraWorldRules.SASH_BREATH;
			}
			return a;
		});
		// Mojito's Fetchblade: right-click to bound ahead in a burst of mint-green magic and loose a wide crescent slash.
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (hand != InteractionHand.MAIN_HAND || !Config.forgedGear(player)) {
				return InteractionResult.PASS;
			}
			ItemStack stack = player.getItemInHand(hand);
			if (of(stack) != AuraWorldRules.Forged.FETCHBLADE || player.getCooldowns().isOnCooldown(stack)) {
				return InteractionResult.PASS;
			}
			player.getCooldowns().addCooldown(stack, AuraWorldRules.FETCHBLADE_DASH_COOLDOWN);
			Vec3 look = player.getLookAngle();
			Vec3 flat = new Vec3(look.x, 0, look.z);
			Vec3 dir = flat.lengthSqr() > 1.0E-4 ? flat.normalize() : Vec3.ZERO;
			player.setDeltaMovement(player.getDeltaMovement().multiply(0.3, 0.5, 0.3).add(dir.x * 0.95, 0.25, dir.z * 0.95));
			player.needsSync = true;
			player.fallDistance = 0;
			if (world instanceof ServerLevel server && player instanceof ServerPlayer sp) {
				sp.addEffect(new MobEffectInstance(MobEffects.SPEED, 80, 1, false, true));
				server.sendParticles(FETCH_MINT, sp.getX(), sp.getY() + 0.9, sp.getZ(), 24, 0.5, 0.4, 0.5, 0);
				server.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0F, 1.15F);
				AuraSlash.fetchCrescent(sp, AuraWorldRules.FETCHBLADE_DASH_DAMAGE);
			}
			return InteractionResult.SUCCESS;
		});
	}

	/** The forging {@code stack} carries, or null. */
	public static AuraWorldRules.Forged of(ItemStack stack) {
		String id = stack.get(FORGED);
		return id == null ? null : AuraWorldRules.Forged.byId(id);
	}

	/** The forging of what {@code entity} holds in its main hand, or null. */
	public static AuraWorldRules.Forged held(LivingEntity entity) {
		return entity == null ? null : of(entity.getMainHandItem());
	}

	private static boolean working(LivingEntity entity, AuraWorldRules.Forged forged) {
		return entity instanceof Player player && Config.forgedGear(player) && held(player) == forged;
	}

	/** Skyrend Glaive & Fetchblade: what a slash's harm is multiplied by (with its element, under the cap against a player). */
	public static double slashBonus(LivingEntity player) {
		if (working(player, AuraWorldRules.Forged.SKYREND_GLAIVE)) {
			return Config.get().auraWorld().skyrendSlash();
		}
		if (working(player, AuraWorldRules.Forged.FETCHBLADE)) {
			return AuraWorldRules.FETCHBLADE_SLASH;
		}
		return 1.0;
	}

	/** Skyrend Glaive & Fetchblade: how much further and wider a slash flies. */
	public static double slashReach(LivingEntity player) {
		if (working(player, AuraWorldRules.Forged.SKYREND_GLAIVE)) {
			return AuraWorldRules.SKYREND_REACH;
		}
		if (working(player, AuraWorldRules.Forged.FETCHBLADE)) {
			return AuraWorldRules.FETCHBLADE_REACH;
		}
		return 1.0;
	}

	/** Skyrend Glaive & Fetchblade: how many more foes a slash cuts. */
	public static int slashTargets(LivingEntity player) {
		if (working(player, AuraWorldRules.Forged.SKYREND_GLAIVE)) {
			return AuraWorldRules.SKYREND_TARGETS;
		}
		if (working(player, AuraWorldRules.Forged.FETCHBLADE)) {
			return AuraWorldRules.FETCHBLADE_TARGETS;
		}
		return 0;
	}

	/** Bulwark Maul: what the guard costs, raised and held, times this. */
	public static double guardCost(LivingEntity player) {
		double forged = working(player, AuraWorldRules.Forged.BULWARK_MAUL) ? Config.get().auraWorld().bulwarkGuardCost() : 1.0;
		return forged * (player instanceof Player p ? SleepingBlades.guardCost(p) : 1);
	}

	// ------------------------------------------------------------------ the Breath Sash

	/**
	 * Whether {@code player} wears the Breath Sash: in the tome slot, or held in the off-hand while that slot is empty (as casting
	 * gear works). Both sides: the gear tray is synced to its owner.
	 */
	public static boolean sash(Player player) {
		if (player == null || !Config.forgedGear(player)) {
			return false;
		}
		ItemStack slotted = GearSlots.get(player, GearSlot.TOME);
		if (!slotted.isEmpty()) {
			return Gear.defOf(slotted) == GearDef.BREATH_SASH;
		}
		return Gear.defOf(player.getOffhandItem()) == GearDef.BREATH_SASH;
	}

	/** A stage's capacity for {@code player}: more with the sash on. */
	public static int capacity(Player player, int capacity) {
		return sash(player) ? AuraWorldRules.sashCapacity(capacity, Config.sashCapacity(player)) : capacity;
	}

	/** How long the breathing stance takes to settle for {@code player}: sooner with the sash on. */
	public static int settleTicks(Player player) {
		return sash(player) ? AuraWorldRules.SASH_SETTLE : AuraRules.SETTLE_TICKS;
	}

	// ------------------------------------------------------------------ words

	/** The lines a forged weapon's tooltip gains: the forging, what it does, and what it was forged from. */
	public static List<Component> describe(ItemStack stack) {
		AuraWorldRules.Forged forged = of(stack);
		if (forged == null) {
			return List.of();
		}
		Component base = Component.translatable(stack.getItem().getDescriptionId());
		String percent = switch (forged) {
			case LUMENEDGE -> pct(Config.get().auraWorld().lumenedgeGain() - 1);
			case SKYREND_GLAIVE -> pct(Config.get().auraWorld().skyrendSlash() - 1);
			case BULWARK_MAUL -> pct(1 - Config.get().auraWorld().bulwarkGuardCost());
			case FETCHBLADE -> pct(AuraWorldRules.FETCHBLADE_GAIN - 1);
		};
		return List.of(
			Component.translatable("tooltip.wildercord.aura_forged." + forged.id + ".lore").withStyle(ChatFormatting.ITALIC).withColor(0xB8A8D8),
			Component.translatable("tooltip.wildercord.aura_forged." + forged.id, percent).withColor(0xE8D8B0),
			Component.translatable("tooltip.wildercord.aura_forged.from", base).withStyle(ChatFormatting.DARK_GRAY));
	}

	private static String pct(double fraction) {
		return Long.toString(Math.round(fraction * 100));
	}
}
