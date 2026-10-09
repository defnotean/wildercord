package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.client.AuraFxClient;
import dev.wildercord.client.fx.Glimmer;
import dev.wildercord.gametest.AuraBodyMoteProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Every wrapper calls the original operation once; only a fixture-owned local RNG can be substituted. */
@Mixin(value = AuraFxClient.class, remap = false)
public abstract class AuraBodyMoteProbeMixin {
	private static final String MOTES = "motes(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/player/AbstractClientPlayer;Ldev/wildercord/aura/AuraAttachments$Look;F)V";
	@Shadow @Final private static Glimmer.Budget BODY;

	@WrapMethod(method = "tick(Lnet/minecraft/client/Minecraft;)V", require = 1, expect = 1, allow = 1)
	private static void wildercord$tick(Minecraft mc, Operation<Void> original) {
		var token = AuraBodyMoteProbe.beginTick(mc, BODY.out());
		try { original.call(mc); }
		finally { AuraBodyMoteProbe.endTick(token, BODY.out()); }
	}
	@WrapMethod(method = MOTES, require = 1, expect = 1, allow = 1)
	private static void wildercord$motes(Minecraft mc, ClientLevel level, AbstractClientPlayer player, AuraAttachments.Look look,
			float intensity, Operation<Void> original) {
		var call = AuraBodyMoteProbe.beginMotes(mc, level, player, look, intensity);
		try { original.call(mc, level, player, look, intensity); }
		finally { AuraBodyMoteProbe.endMotes(call); }
	}
	@WrapOperation(method = MOTES, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getRandom()Lnet/minecraft/util/RandomSource;"), require = 1, expect = 1, allow = 1)
	private static RandomSource wildercord$source(AbstractClientPlayer player, Operation<RandomSource> original) {
		return AuraBodyMoteProbe.source(player, original.call(player));
	}
	@WrapOperation(method = MOTES, at = @At(value = "INVOKE", target = "Ldev/wildercord/client/fx/Glimmer$Budget;hasRoom()Z", ordinal = 0), require = 1, expect = 1, allow = 1)
	private static boolean wildercord$room(Glimmer.Budget budget, Operation<Boolean> original) {
		boolean room = original.call(budget); AuraBodyMoteProbe.room(room); return room;
	}
	@WrapOperation(method = MOTES, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F", ordinal = 0), require = 1, expect = 1, allow = 1)
	private static float wildercord$sample(RandomSource random, Operation<Float> original) {
		float sample = original.call(random); AuraBodyMoteProbe.sample(random, sample); return sample;
	}
	@WrapOperation(method = MOTES, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;add(Lnet/minecraft/client/particle/Particle;)V", ordinal = 0), require = 1, expect = 1, allow = 1)
	private static void wildercord$added(ParticleEngine engine, Particle particle, Operation<Void> original) {
		original.call(engine, particle); AuraBodyMoteProbe.added(engine, particle);
	}
}
