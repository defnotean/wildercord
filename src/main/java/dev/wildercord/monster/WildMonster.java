package dev.wildercord.monster;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * What the magical monsters of the wilds share: a few bits of what they're doing, synced in one byte and eased on the client
 * for their poses and glows (as the dungeon bosses do), and the spells one of them carries if it rolls Runebound.
 *
 * <ul>
 *   <li>{@link #WINDUP}: the tell before its signature attack (a Bramblewalker rearing back, a harpy's shriek...);</li>
 *   <li>{@link #ACTING}: the attack itself (the lash, the pounce, the dive, the roll, the tongue);</li>
 *   <li>{@link #GUARD}: its defence (a Geode Crawler curled up), or a frog's gulp;</li>
 *   <li>{@link #STUNNED}: caught out (a missed pounce or dive, a cracked shell): the moment to strike back;</li>
 *   <li>{@link #VEILED}: hidden in the dark (the Gloomstalker);</li>
 *   <li>{@link #ALT}: a second move (a harpy calling lightning, a frog's mouth opening, a Gloomstalker slinking away).</li>
 * </ul>
 */
public abstract class WildMonster extends Monster implements RuneboundKin {
	public static final int WINDUP = 1;
	public static final int ACTING = 2;
	public static final int GUARD = 4;
	public static final int STUNNED = 8;
	public static final int VEILED = 16;
	public static final int ALT = 32;
	private static final int FLAGS = 6;
	/** How fast each pose eases in and out on the client, per tick (a tell snaps in, the veil drifts). */
	private static final float[] EASE = {0.22F, 0.35F, 0.2F, 0.2F, 0.06F, 0.2F};

	private static final EntityDataAccessor<Byte> DATA_STATE = net.minecraft.network.syncher.SynchedEntityData.defineId(WildMonster.class,
		EntityDataSerializers.BYTE);

	private final float[] pose = new float[FLAGS];
	private final float[] poseO = new float[FLAGS];

	protected WildMonster(EntityType<? extends WildMonster> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_STATE, (byte) 0);
	}

	/** Turns one of the flags above on or off (server side; the client sees it a moment later). */
	protected void setState(int flag, boolean on) {
		byte state = entityData.get(DATA_STATE);
		byte next = (byte) (on ? state | flag : state & ~flag);
		if (next != state) {
			entityData.set(DATA_STATE, next);
		}
	}

	/** Whether one of the flags above is on, on either side. */
	public boolean state(int flag) {
		return (entityData.get(DATA_STATE) & flag) != 0;
	}

	/** One flag's pose on the client, eased: 0 off, 1 fully on. */
	public float pose(int flag, float partial) {
		int i = Integer.numberOfTrailingZeros(flag);
		return Mth.lerp(partial, poseO[i], pose[i]);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			for (int i = 0; i < FLAGS; i++) {
				poseO[i] = pose[i];
				pose[i] = Mth.approach(pose[i], state(1 << i) ? 1 : 0, EASE[i]);
			}
		}
	}

	/** Turns its body, head and look to face a creature (before an aimed attack goes off, so it flies true). */
	protected void face(LivingEntity target) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		setYRot(yaw);
		setYHeadRot(yaw);
		setYBodyRot(yaw);
		getLookControl().setLookAt(target, 60, 60);
	}

	/** The difficulty as a number: 0 Peaceful, 1 Easy, 2 Normal, 3 Hard. */
	protected int difficulty() {
		return level().getDifficulty().getId();
	}

	/** Whether this is the server's copy, in a level the server runs. */
	protected ServerLevel serverLevel() {
		return level() instanceof ServerLevel server ? server : null;
	}
}
