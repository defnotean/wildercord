package dev.wildercord.gametest.stonehinge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.stonehinge.StoneHingeImpulseProbe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public abstract class StoneHingePlayerProbeMixin {
	@WrapMethod(method = "hurtServer")
	private boolean stoneHinge$hit(ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        dev.wildercord.gametest.stonehinge.peer.StoneHingeNaturalMotion.contaminate((Player) (Object) this, "Intervening hurt during natural dispatch");
		var hit = StoneHingeImpulseProbe.begin((Player) (Object) this, source);
		boolean returned = false, success = false;
		try { returned = original.call(level, source, amount); success = true; return returned; }
		finally { StoneHingeImpulseProbe.finish(hit, returned, success); }
	}
	@WrapMethod(method = "actuallyHurt")
	private void stoneHinge$wound(ServerLevel level, DamageSource source, float amount, Operation<Void> original) {
		var wound = StoneHingeImpulseProbe.beginWound((Player) (Object) this, source);
		boolean success = false;
		try { original.call(level, source, amount); success = true; }
		finally { StoneHingeImpulseProbe.finishWound(wound, success); }
	}
}
