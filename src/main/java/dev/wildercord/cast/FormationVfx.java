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
	/** Initial groups only: delayed/conditional effects must not prepare before their link fires. */
	public static void send(Cast cast,List<RuneDef> runes) {
		if(Fx.muted() || cast.passive || cast.info.root()==null)return;
		var glyphs=dev.wildercord.spell.Knots.flatten(runes).stream().map(RuneDef::id).filter(id->id.length()<=4096).limit(16).toList();
		boolean lead=true;
		for(var group:cast.info.root().groups) {
			var parts=new java.util.ArrayList<RuneDef>();parts.add(group.shape);parts.addAll(group.shapeMods);
			for(var node:group.effects){parts.add(node.effect);parts.addAll(node.mods);}
			var theme=cast.theme(group);var feel=theme.feel();
			float scale=feel==null?1F:(float)dev.wildercord.cast.feel.Feels.circleRadius(feel);
			String shape=group.shape.path();
			var payload=new FormationPayload(cast.caster.getId(),shape.length()<=128?shape:"self",parts.stream().map(RuneDef::id).filter(id->id.length()<=4096).limit(16).toList(),
				VisualElements.of(parts).stream().filter(id->id.length()<=128).limit(10).toList(),theme.primary(),scale,placement(shape),(float)(CastEngine.AIM_RANGE*Mastery.range(cast)),lead,glyphs);
			broadcast(cast.level,cast.caster,payload);lead=false;
		}
		if(lead)broadcast(cast.level,cast.caster,new FormationPayload(cast.caster.getId(),"self",List.of(),List.of(),0xA8D8C0,1,FormationPayload.CIRCLE_ONLY,24,true,glyphs));
		Fx.sound(cast.level,cast.caster.position(),dev.wildercord.content.WildercordSounds.CIRCLE_OPEN,.35F,1F);
		if(!cast.info.root().groups.isEmpty())Fx.sound(cast.level,cast.caster.position(),cast.theme(cast.info.root().groups.getFirst()).cast(),.55F,1F);
	}
	static int placement(String shape) {
		return switch(shape) {
			case "self","domain","orbit","trail","burst","nova","ring","wave" -> FormationPayload.CASTER;
			case "zone","rain","wall","pillar","mine","totem","vortex" -> FormationPayload.AIMED;
			default -> FormationPayload.LEGACY;
		};
	}
	private static void broadcast(ServerLevel level,LivingEntity caster,FormationPayload payload) {
		for(var player:level.players())if(player.distanceToSqr(caster)<=64*64){ServerPlayNetworking.send(player,payload);VisualMetrics.formation();}
	}
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
