package dev.wildercord.cast;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Keep real attacker attribution while defences see the actual paid ray or secondary arc origin. */
public final class RelayDamageSource extends DamageSource {
	private final Cast cast;
	private final Vec3 from;
	public RelayDamageSource(DamageSource original, Cast cast) {
		super(original.typeHolder(), original.getDirectEntity(), original.getEntity());
		this.cast = cast; this.from = cast.incoming();
	}
	@Override public Vec3 getSourcePosition() { return from; }
	@Override public Vec3 sourcePositionRaw() { return from; }
	public boolean admits(LivingEntity target) { return cast.admits(target); }
	Cast cast() { return cast; }
}
