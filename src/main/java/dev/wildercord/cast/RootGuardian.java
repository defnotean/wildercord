package dev.wildercord.cast;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.List;

/** Three visible root bindings protect its heart. Shears offer every player a physical puzzle solution. */
public final class RootGuardian extends DungeonBoss {
	private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> BINDINGS = net.minecraft.network.syncher.SynchedEntityData.defineId(RootGuardian.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
	private int bindings = 3;
	private long pruneAt, exposedUntil;
	public RootGuardian(EntityType<? extends RootGuardian> type, Level level) { super(type, level, BossEvent.BossBarColor.GREEN); }
	public static AttributeSupplier.Builder createAttributes() {
		return createMonsterAttributes().add(Attributes.MAX_HEALTH, 220).add(Attributes.MOVEMENT_SPEED, .20)
			.add(Attributes.FOLLOW_RANGE, 32).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.ATTACK_DAMAGE, 7);
	}
	public static RootGuardian rise(ServerLevel level, BlockPos altar) {
		RootGuardian boss = place(level, DungeonEntities.ROOT_GUARDIAN, altar, 1);
		if (boss != null) { boss.announce(level, "message.wildercord.root_guardian_wakes", 0x83CC55);
			Sigils.ground(level, boss.position(), 0x438835, 0xFFC95C, 4, 60); }
		return boss;
	}
	@Override protected List<List<RuneDef>> spells(int phase) {
		return switch (phase) {
			case 1 -> List.of(List.of(Runes.WAVE, Runes.ROOTSNARE), List.of(Runes.ARC, Runes.MIRE));
			case 2 -> List.of(List.of(Runes.RING, Runes.ROOTSNARE), List.of(Runes.PILLAR, Runes.TREMOR));
			default -> List.of(List.of(Runes.RAIN, Runes.VENOM), List.of(Runes.CONE, Runes.ROOTSNARE), List.of(Runes.WAVE, Runes.AFTERSHOCK));
		};
	}
	@Override protected String feat() { return "root_guardian"; }
	@Override protected int color() { return 0x438835; }
	@Override protected int accent() { return 0xFFC95C; }
	@Override protected void approach(ServerLevel level, LivingEntity target) {
		if (distanceTo(target) > 4) getNavigation().moveTo(target, 1); else getNavigation().stop();
	}
	@Override protected void mechanic(ServerLevel level, long now) {
		if (bindings == 0 && now >= exposedUntil) bindings = 3;
		entityData.set(BINDINGS, bindings);
		setState(GUARDED, bindings > 0); setState(EXPOSED, bindings == 0);
		if (now % 20 == 0) Vfx.emit(level, bindings > 0 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.WAX_ON,
			position().add(0, 1.5, 0), 3, .6, .01);
	}
	/** Pruning is shared by physical shears and fire reactions; it never requires a rare rune. */
	public boolean prune(ServerLevel level) {
		long now = level.getGameTime();
		if (bindings == 0 || now < pruneAt || state(SHIFTING) || !isAlive()) return false;
		bindings--; pruneAt = now + 12;
		entityData.set(BINDINGS, bindings);
		if (bindings == 0) { exposedUntil = now + 120; interrupt(); setState(EXPOSED, true); setState(GUARDED, false); }
		Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, position().add(0, 1.5, 0), 12, .7, .04);
		return true;
	}
	@Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!player.getItemInHand(hand).is(Items.SHEARS)) return super.mobInteract(player, hand);
		if (level() instanceof ServerLevel server && prune(server))
			player.sendOverlayMessage(Component.translatable("message.wildercord.root_pruned", bindings));
		return InteractionResult.SUCCESS;
	}
	@Override protected float resist(ServerLevel level, DamageSource source, float damage) {
		if (bindings > 0 && (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) || Dungeons.spellLanding() && isOnFire())) prune(level);
		return bindings == 0 ? damage : damage * .35F;
	}
	@Override protected void onPhase(ServerLevel level, int phase) { bindings = 3; exposedUntil = 0; announce(level, "message.wildercord.root_guardian_phase", color()); }
	@Override protected Component status() { return Component.translatable(bindings == 0 ? "boss.wildercord.root_open" : "boss.wildercord.root_bound", bindings); }
	@Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { super.defineSynchedData(builder); builder.define(BINDINGS, 3); }
	public int bindings() { return level().isClientSide() ? entityData.get(BINDINGS) : bindings; }
	@Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putInt("bindings", bindings); out.putLong("exposed_until", exposedUntil); }
	@Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); bindings = Math.clamp(in.getIntOr("bindings", 3), 0, 3); exposedUntil = in.getLongOr("exposed_until", 0); }
}
