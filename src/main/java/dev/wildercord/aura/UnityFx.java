package dev.wildercord.aura;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Two open breathing strands at the back and sides; no circles or first-person veil. */
public final class UnityFx {
	private UnityFx() {}
	private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Long> FLOW_AT=
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(dev.wildercord.Wildercord.id("unity_flow_at"),b -> b.initializer(() -> -100L));
	private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Long> VOICE_AT=
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(dev.wildercord.Wildercord.id("unity_voice_at"),b -> b.initializer(() -> -100L));
	private static Vec3 forward(ServerPlayer p) {double yaw=Math.toRadians(p.getYRot());return new Vec3(-Math.sin(yaw),0,Math.cos(yaw));}
	private static int auraColor(ServerPlayer p) { return AuraRules.mix(Aura.color(p),0xE9B461,.65); }
	public static void begin(ServerPlayer p) {
		var f=forward(p);var side=new Vec3(f.z,0,-f.x);var foot=p.position().subtract(f.scale(.46));
		for(int strand=0;strand<2;strand++)for(int i=0;i<7;i++) {
			double a=i/7.0,b=(i+1)/7.0,sign=strand==0?1:-1;
			var from=foot.add(side.scale(sign*.44*Math.cos(a*Math.PI*2))).add(0,.12+a*1.5,0);
			var to=foot.add(side.scale(sign*.44*Math.cos(b*Math.PI*2))).add(0,.12+b*1.5,0);
			ElementFx.ray(p.level(),from,to,strand==0?auraColor(p):0x79BFFF,.022,14);
		}
		Feels.sound(p.level(),p.position(),"aura_unity_begin",.7F,1);
	}
	public static void flow(ServerPlayer p,boolean manaToAura) {
		long now=Unity.now(p);if(now-p.getAttachedOrElse(FLOW_AT,-100L)<4)return;p.setAttached(FLOW_AT,now);
		var f=forward(p);var side=new Vec3(f.z,0,-f.x);var at=p.position().subtract(f.scale(.32)).add(side.scale(manaToAura?.42:-.42)).add(0,.65,0);
		for(int i=0;i<3;i++)ElementFx.ray(p.level(),at.add(0,i*.16,0),at.add(side.scale(manaToAura?.11:-.11)).add(0,i*.16+.12,0),manaToAura?auraColor(p):0x79BFFF,.018,8);
		if(now-p.getAttachedOrElse(VOICE_AT,-100L)>=20){p.setAttached(VOICE_AT,now);Feels.sound(p.level(),p.position(),"aura_unity_flow",.35F,1);}
	}
	public static void breathe(ServerPlayer p) {
		var f=forward(p);var side=new Vec3(f.z,0,-f.x);var at=p.position().subtract(f.scale(.4)).add(0,.6,0);
		for(int i=0;i<2;i++){var start=at.add(side.scale(i==0?.35:-.35));ElementFx.ray(p.level(),start,start.add(0,.45,0),i==0?auraColor(p):0x79BFFF,.012,10);}
	}
	public static void end(ServerPlayer p) {Feels.sound(p.level(),p.position(),"aura_unity_end",.45F,1);}
}
