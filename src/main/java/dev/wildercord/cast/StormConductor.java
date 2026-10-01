package dev.wildercord.cast;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import java.util.List;

/** Ground its charge by standing beside an arena rod. The safe spot moves between three rods. */
public final class StormConductor extends DungeonBoss {
	private long groundedUntil, nextGround;
	private int rodIndex;
	private List<BlockPos> rods;
	public StormConductor(EntityType<? extends StormConductor> type, Level level) { super(type, level, BossEvent.BossBarColor.BLUE); }
	public static AttributeSupplier.Builder createAttributes() {
		return createMonsterAttributes().add(Attributes.MAX_HEALTH, 240).add(Attributes.MOVEMENT_SPEED, .24)
			.add(Attributes.FOLLOW_RANGE, 32).add(Attributes.KNOCKBACK_RESISTANCE, 1).add(Attributes.ATTACK_DAMAGE, 7);
	}
	public static StormConductor rise(ServerLevel level, BlockPos altar) {
		StormConductor boss = place(level, DungeonEntities.STORM_CONDUCTOR, altar, 1);
		if (boss != null) boss.announce(level, "message.wildercord.storm_conductor_wakes", 0x60CAFF);
		return boss;
	}
	@Override protected List<List<RuneDef>> spells(int phase) {
		return switch (phase) {
			case 1 -> List.of(List.of(Runes.ARC, Runes.SHOCK), List.of(Runes.WAVE, Runes.WINDCUT));
			case 2 -> List.of(List.of(Runes.RAIN, Runes.SHOCK), List.of(Runes.PRISM, Runes.WINDCUT));
			default -> List.of(List.of(Runes.RING, Runes.THUNDERCLAP), List.of(Runes.BARRAGE, Runes.SHOCK), List.of(Runes.LANCE, Runes.WINDCUT));
		};
	}
	@Override protected String feat() { return "storm_conductor"; }
	@Override protected int color() { return 0x60CAFF; }
	@Override protected int accent() { return 0xFFCC72; }
	@Override protected void approach(ServerLevel level, LivingEntity target) { getNavigation().stop(); getLookControl().setLookAt(target, 30, 30); }
	public BlockPos activeRod() {
		if(home()==null) return blockPosition();
		if(rods==null || rods.isEmpty()&&level().getGameTime()%100==0 || rods!=null&&rods.stream().anyMatch(p->level().hasChunkAt(p)&&!(level().getBlockState(p).getBlock() instanceof LightningRodBlock))) {
			var found=new java.util.ArrayList<BlockPos>();
			for(BlockPos p:BlockPos.betweenClosed(home().offset(-6,0,-6),home().offset(6,0,6)))
				if(level().hasChunkAt(p) && level().getBlockState(p).getBlock() instanceof LightningRodBlock) found.add(p.immutable());
			found.sort(java.util.Comparator.<BlockPos>comparingInt(p -> p.getX()).thenComparingInt(p -> p.getZ()));
			rods=List.copyOf(found);
		}
		return rods.isEmpty()?home():rods.get(rodIndex%rods.size());
	}
	@Override protected void mechanic(ServerLevel level, long now) {
		setState(EXPOSED, now < groundedUntil); setState(GUARDED, now >= groundedUntil);
		if (now < nextGround || home() == null || now % 10 != 0) return;
		BlockPos rod = activeRod();
		// The worldgen rods and manually placed rods obey exactly the same encounter rule.
		if (!(level.getBlockState(rod).getBlock() instanceof LightningRodBlock)) return;
		Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, net.minecraft.world.phys.Vec3.atCenterOf(rod).add(0, .8, 0), 6, .2, .01);
		for (var player : level.players()) if (player.isAlive() && !player.isSpectator() && player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(rod)) < 4) {
			ground(level); break;
		}
	}
	public boolean ground(ServerLevel level) {
		long now = level.getGameTime();
		if (!isAlive() || state(SHIFTING) || now < nextGround) return false;
		groundedUntil = now + 100; nextGround = now + 160; rodIndex = (rodIndex + 1) % 3;
		interrupt(); readyAt = now + 40; setState(EXPOSED, true); setState(GUARDED, false);
		Sigils.ground(level, position(), color(), accent(), 3, 40);
		announce(level, "message.wildercord.storm_grounded", accent()); return true;
	}
	@Override protected float resist(ServerLevel level, DamageSource source, float damage) { return level.getGameTime() < groundedUntil ? damage : damage * .5F; }
	@Override protected void onPhase(ServerLevel level, int phase) { groundedUntil = 0; nextGround = 0; rodIndex = phase % 3; announce(level, "message.wildercord.storm_conductor_phase", color()); }
	@Override protected Component status() { return Component.translatable(level().getGameTime() < groundedUntil ? "boss.wildercord.storm_grounded" : "boss.wildercord.storm_charged"); }
	@Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
		super.addAdditionalSaveData(out); out.putInt("rod_index",rodIndex); out.putLong("grounded_until",groundedUntil); out.putLong("next_ground",nextGround);
	}
	@Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
		super.readAdditionalSaveData(in); rodIndex=Math.floorMod(in.getIntOr("rod_index",0),3); groundedUntil=in.getLongOr("grounded_until",0); nextGround=in.getLongOr("next_ground",0);
	}
}
