package dev.wildercord.cast;

import dev.wildercord.net.FormationPayload;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.VisualElements;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

/** Compact, client-reconstructed formation. Gameplay still releases exclusively on the server. */
public final class FormationVfx {
	private FormationVfx() {}
	public static void send(LivingEntity caster, Vfx.Theme theme, List<RuneDef> runes) {
		if (Fx.muted() || !(caster.level() instanceof ServerLevel level)) return;
		String shape = dev.wildercord.spell.Knots.flatten(runes).stream().filter(r -> r.family() == RuneFamily.SHAPE).map(RuneDef::path).findFirst().orElse("self");
		float scale = theme.feel() == null ? 1F : (float) dev.wildercord.cast.feel.Feels.circleRadius(theme.feel());
		FormationPayload payload = new FormationPayload(caster.getId(), shape.length()<=128?shape:"self", dev.wildercord.spell.Knots.flatten(runes).stream().map(RuneDef::id).filter(id->id.length()<=4096).limit(16).toList(),
			VisualElements.of(runes).stream().filter(id->id.length()<=128).limit(10).toList(), theme.primary(), scale);
		for (var player : level.players()) if (player.distanceToSqr(caster) <= 64 * 64) {
			ServerPlayNetworking.send(player, payload);
			VisualMetrics.formation();
		}
		Fx.sound(level, caster.position(), dev.wildercord.content.WildercordSounds.CIRCLE_OPEN, 0.35F, 1F);
		Fx.sound(level, caster.position(), theme.cast(), 0.55F, 1F);
	}
}
