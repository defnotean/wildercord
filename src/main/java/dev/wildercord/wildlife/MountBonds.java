package dev.wildercord.wildlife;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.Fx;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Mount bonds (0.13, see {@link MountBondRules}): any tame horse-like mount (a horse, donkey, mule or camel, a Ridgeback
 * Stag, Reefback Turtle or Delver Mole) ridden by its owner grows a bond with them, saved on the mount. Its levels add
 * speed and health as permanent attribute modifiers; at level 3 its rider's sprint key makes it dash, and at level 5 a
 * landing from a jump strikes what's around it. A bond is with one owner: a mount that changes hands starts again.
 */
public final class MountBonds {
	private MountBonds() {}

	/** A mount's bond: whose it is and how many points it has. */
	public record Bond(Optional<UUID> owner, int points) {
		public static final Bond NONE = new Bond(Optional.empty(), 0);
		static final Codec<Bond> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Bond::owner),
			Codec.INT.optionalFieldOf("points", 0).forGetter(Bond::points)
		).apply(i, Bond::new));

		public int level() {
			return MountBondRules.level(points);
		}
	}

	public static final AttachmentType<Bond> BOND = AttachmentRegistry.create(Wildercord.id("mount_bond"),
		builder -> builder.initializer(() -> Bond.NONE).persistent(Bond.CODEC));

	private static final Identifier SPEED = Wildercord.id("mount_bond_speed");
	private static final Identifier HEALTH = Wildercord.id("mount_bond_health");

	/** What riding is doing now, not saved: where it was a second ago, its last dash, the sprint key, how far it has fallen. */
	private static final class Ride {
		Vec3 last;
		long seen = Long.MIN_VALUE;
		long lastDash = Long.MIN_VALUE / 2;
		boolean sprint;
		double fell;
		boolean airborne;
	}

	private static final Map<AbstractHorse, Ride> RIDES = new WeakHashMap<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.getVehicle() instanceof AbstractHorse mount && player.level() instanceof ServerLevel level) {
					tick(level, player, mount);
				}
			}
		});
	}

	/** The bond {@code player} has with {@code mount}: none unless it's theirs. */
	public static Bond bond(AbstractHorse mount, Player player) {
		Bond bond = mount.getAttachedOrElse(BOND, Bond.NONE);
		return bond.owner().filter(player.getUUID()::equals).isPresent() ? bond : Bond.NONE;
	}

	/** Whether {@code player} owns {@code mount} (a tame one they tamed or were given). */
	public static boolean owns(Player player, AbstractHorse mount) {
		EntityReference<LivingEntity> owner = ((OwnableEntity) mount).getOwnerReference();
		return mount.isTamed() && owner != null && owner.getUUID().equals(player.getUUID());
	}

	/**
	 * Adds bond points from {@code player}'s riding of {@code mount}; a bond with someone else is set aside. Tells them when
	 * it deepens. Returns the bond as it now is.
	 */
	public static Bond grow(ServerLevel level, ServerPlayer player, AbstractHorse mount, int points) {
		Bond stored = mount.getAttachedOrElse(BOND, Bond.NONE);
		Bond before = bond(mount, player);
		Bond after = new Bond(Optional.of(player.getUUID()), Math.max(0, before.points() + points));
		mount.setAttached(BOND, after);
		if (after.level() != stored.level()) {
			apply(mount, after.level());
		}
		if (after.level() > before.level()) {
			deepened(level, player, mount, after.level());
		}
		return after;
	}

	/** Puts a bond level's speed and health on {@code mount}, replacing what an older level gave. */
	public static void apply(AbstractHorse mount, int level) {
		AttributeInstance speed = mount.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(SPEED);
			if (level > 0) {
				speed.addPermanentModifier(new AttributeModifier(SPEED, MountBondRules.speed(level), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			}
		}
		AttributeInstance health = mount.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.removeModifier(HEALTH);
			if (level > 0) {
				health.addPermanentModifier(new AttributeModifier(HEALTH, MountBondRules.health(level), AttributeModifier.Operation.ADD_VALUE));
			}
			mount.setHealth(Math.min(mount.getHealth(), mount.getMaxHealth()));
		}
	}

	private static void deepened(ServerLevel level, ServerPlayer player, AbstractHorse mount, int bond) {
		String perk = bond == MountBondRules.DASH_LEVEL ? ".dash" : bond == MountBondRules.STRIKE_LEVEL ? ".strike" : "";
		player.sendSystemMessage(Component.translatable("message.wildercord.mount_bond.deepened" + perk, mount.getDisplayName(), bond)
			.withColor(0xE8B8C8));
		level.sendParticles(ParticleTypes.HEART, mount.getX(), mount.getY() + mount.getBbHeight(), mount.getZ(), 6, 0.6, 0.3, 0.6, 0);
		Fx.sound(level, mount.position(), SoundEvents.PLAYER_LEVELUP, 0.6F, 1.4F);
	}

	private static void tick(ServerLevel level, ServerPlayer player, AbstractHorse mount) {
		if (!owns(player, mount)) {
			return;
		}
		Ride ride = RIDES.computeIfAbsent(mount, m -> new Ride());
		long now = level.getGameTime();
		Bond bond = bond(mount, player);
		if (now - ride.seen > 1) {
			// A fresh ride: start measuring from here, and say how the bond stands.
			ride.last = mount.position();
			ride.airborne = false;
			ride.fell = 0;
			int next = MountBondRules.next(bond.level());
			player.sendOverlayMessage(next < 0
				? Component.translatable("message.wildercord.mount_bond.full", bond.level())
				: Component.translatable("message.wildercord.mount_bond.level", bond.level(), bond.points(), next));
		}
		ride.seen = now;

		if (now % 20 == 0) {
			int earned = MountBondRules.earned(mount.position().distanceTo(ride.last));
			ride.last = mount.position();
			if (earned > 0) {
				bond = grow(level, player, mount, earned);
			}
		}

		boolean sprint = player.getLastClientInput().sprint();
		if (sprint && !ride.sprint && mount.onGround() && MountBondRules.dashReady(bond.level(), now, ride.lastDash)) {
			dash(level, mount);
			ride.lastDash = now;
		}
		ride.sprint = sprint;

		if (!mount.onGround()) {
			ride.airborne = true;
			ride.fell = Math.max(ride.fell, mount.fallDistance);
		} else if (ride.airborne) {
			if (MountBondRules.strikeOnLanding(bond.level(), ride.fell)) {
				strike(level, player, mount);
			}
			ride.airborne = false;
			ride.fell = 0;
		}
	}

	/** Throws {@code mount} forward along its heading. */
	public static void dash(ServerLevel level, AbstractHorse mount) {
		double[] v = MountBondRules.dash(mount.getYRot());
		mount.setDeltaMovement(v[0], mount.getDeltaMovement().y, v[2]);
		mount.needsSync = true;
		level.sendParticles(ParticleTypes.CLOUD, mount.getX(), mount.getY() + 0.2, mount.getZ(), 8, 0.4, 0.1, 0.4, 0.02);
		Fx.sound(level, mount.position(), SoundEvents.HORSE_GALLOP, 1.0F, 1.2F);
	}

	/**
	 * Lands with a strike: everything living within reach, apart from {@code rider}, their mount, its other riders and
	 * their own tame animals, is hurt and thrown back. Players are spared. Returns how many it struck.
	 */
	public static int strike(ServerLevel level, ServerPlayer rider, AbstractHorse mount) {
		int struck = 0;
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, mount.getBoundingBox().inflate(MountBondRules.STRIKE_RADIUS),
			e -> e != mount && e != rider && !(e instanceof Player) && !e.isPassengerOfSameVehicle(rider) && e.isAlive()
				&& !(e instanceof OwnableEntity pet && pet.getOwnerReference() != null && pet.getOwnerReference().getUUID().equals(rider.getUUID())))) {
			if (target.distanceToSqr(mount) > MountBondRules.STRIKE_RADIUS * MountBondRules.STRIKE_RADIUS) {
				continue;
			}
			if (target.hurtServer(level, level.damageSources().mobAttack(mount), MountBondRules.STRIKE_DAMAGE)) {
				struck++;
			}
			Vec3 away = target.position().subtract(mount.position()).multiply(1, 0, 1).normalize().scale(MountBondRules.STRIKE_KNOCKBACK);
			target.push(away.x, 0.35, away.z);
			target.needsSync = true;
		}
		level.sendParticles(ParticleTypes.EXPLOSION, mount.getX(), mount.getY(), mount.getZ(), 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.CLOUD, mount.getX(), mount.getY() + 0.1, mount.getZ(), 20, 1.4, 0.1, 1.4, 0.05);
		Fx.sound(level, mount.position(), SoundEvents.MACE_SMASH_GROUND, 1.0F, 0.8F);
		return struck;
	}
}
