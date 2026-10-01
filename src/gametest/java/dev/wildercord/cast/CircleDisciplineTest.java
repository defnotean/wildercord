package dev.wildercord.cast;

import dev.wildercord.content.SpellCircleOption;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Set;

/** Real damage/duration, release snapshots, group isolation, and every circle's animated presentation. */
public final class CircleDisciplineTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext ctx) {
		try(var world=ctx.worldBuilder().setUseConsistentSettings(false).create()) {
			ctx.waitTicks(60);
			world.getServer().runCommand("time set 6000");world.getServer().runCommand("weather clear");world.getServer().runCommand("gamerule advance_time false");world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(p.level(),.5,160,.5,Set.<Relative>of(),0,0,false);});
			ctx.waitTicks(40);world.getServer().runCommand("fill -12 159 -12 12 159 18 polished_deepslate");
			world.getServer().runOnServer(s->{
				var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),.5,160,.5,Set.<Relative>of(),0,0,false);
				p.setDeltaMovement(Vec3.ZERO);p.setShiftKeyDown(false);
				var readout=SpellCompiler.compile(List.of(Runes.BEAM,Runes.ANCHOR_CIRCLE,Runes.FIRE,Runes.SHOCK));
				check(readout.lines().size()>=3 && readout.lines().stream().allMatch(line->line.length()<=56),"runtime circle readout keeps complete tradeoffs on short lines");
				double base=damage(p,null),needle=damage(p,Runes.NEEDLE_CIRCLE),bloom=damage(p,Runes.BLOOM_CIRCLE),mercy=damage(p,Runes.MERCY_CIRCLE);
				check(Math.abs(needle/base-1.2)<.001,"Needle power reaches real damage");check(Math.abs(bloom/base-.8)<.001,"Bloom power tradeoff reaches real damage");check(Math.abs(mercy/base-.75)<.001,"Mercy does not amplify damage");
				p.removeAllEffects();CastEngine.cast(new Cast(p),SpellCompiler.compile(List.of(Runes.SELF,Runes.HASTE)).root());int duration=p.getEffect(MobEffects.HASTE).getDuration();
				p.removeAllEffects();CastEngine.cast(new Cast(p),SpellCompiler.compile(List.of(Runes.SELF,Runes.ANCHOR_CIRCLE,Runes.HASTE)).root());
				check(p.getEffect(MobEffects.HASTE).getDuration()==Math.round(duration*1.4),"Anchor changes actual status duration");p.removeAllEffects();
				var vigil=group(Runes.VIGIL_CIRCLE);Cast cast=new Cast(p);p.setShiftKeyDown(true);cast.prepareCircle(vigil);p.setShiftKeyDown(false);
				check(cast.circleEffect(vigil,EffectKind.HARMFUL).power==1.2,"stance remembered after release");
				check(cast.circleEffect(vigil,EffectKind.HARMFUL).id()==cast.id(),"circle retains shared cast budget identity");
				var plain=SpellCompiler.compile(List.of(Runes.BOLT,Runes.HARM)).root().groups.getFirst();
				check(cast.circleEffect(plain,EffectKind.HARMFUL)==cast,"circle does not leak into another group");
				var pilgrim=group(Runes.PILGRIM_CIRCLE);Cast moving=new Cast(p);p.setDeltaMovement(new Vec3(.2,0,0));moving.prepareCircle(pilgrim);p.setDeltaMovement(Vec3.ZERO);
				check(moving.circleEffect(pilgrim,EffectKind.HARMFUL).power==1.15,"movement sampled once");
				var book=Spellbooks.get(p).withStarterGiven();for(var rune:Runes.all())book=book.learn(rune.id());Spellbooks.set(p,book);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));
			});
			ctx.runOnClient(mc->{mc.getWindow().setWindowed(1600,900);mc.options.guiScale().set(2);mc.resizeGui();mc.gui.toastManager().clear();if(!mc.gui.hud.isHidden())mc.gui.hud.toggle();});
			for(var circle:Runes.all().stream().filter(CircleDisciplines::isCircle).toList()) {
				ctx.runOnClient(mc->{mc.particleEngine.clearParticles();mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);});
				world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),.5,160,.5,Set.<Relative>of(),0,0,false);
					// A labelled close-up display beside the caster, still in the rear plane.
					Fx.particle(p.level(),new SpellCircleOption(List.of(Runes.BEAM.id(),circle.id(),Runes.FIRE.id(),Runes.SHOCK.id()),0xE8C46A,.85F,0,0,45),p.getEyePosition().add(1.1,-.2,-1.0),1,0,0);
				});
				ctx.waitTicks(3);shot(ctx,circle.path()+"_opening");ctx.waitTicks(8);shot(ctx,circle.path()+"_formed");ctx.waitTicks(8);shot(ctx,circle.path()+"_motion");ctx.waitTicks(28);
				ctx.runOnClient(mc->mc.options.setCameraType(CameraType.FIRST_PERSON));
				world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();SpellCaster.edit(p,0,List.of(Runes.BEAM.id(),circle.id(),Runes.FIRE.id(),Runes.SHOCK.id()));Spellbooks.setReadyAt(p,0,0);Spellbooks.setMana(p,400);SpellCaster.cast(p,0);});
				ctx.waitTicks(4);shot(ctx,circle.path()+"_first_person_release");ctx.waitTicks(22);
			}
			world.getServer().runCommand("time set 18000");ctx.waitTicks(2);
			for(var preview:List.of(Runes.NEEDLE_CIRCLE,Runes.ANCHOR_CIRCLE,Runes.CONFLUENCE_CIRCLE)) {
				world.getServer().runOnServer(s->SpellCaster.edit(s.getPlayerList().getPlayers().getFirst(),0,List.of(Runes.BEAM.id(),preview.id(),Runes.FIRE.id(),Runes.SHOCK.id())));
				ctx.waitTicks(4);ctx.setScreen(dev.wildercord.client.CordScreen::new);ctx.waitTicks(8);shot(ctx,preview.path()+"_cord_preview");ctx.setScreen(()->null);ctx.waitTicks(2);
			}
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var g=group(Runes.ECLIPSE_CIRCLE);Cast c=new Cast(p);c.prepareCircle(g);check(c.circleEffect(g,EffectKind.HARMFUL).power==1.2,"night activates Eclipse");});
			world.getServer().runCommand("time set 6000");ctx.waitTicks(2);
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
				var target=EntityTypes.HUSK.create(p.level(),EntitySpawnReason.COMMAND);target.setCustomName(net.minecraft.network.chat.Component.literal("Circle link fixture"));target.setNoAi(true);target.snapTo(.5,160,5.5,0,0);p.level().addFreshEntity(target);
				var root=SpellCompiler.compile(List.of(Runes.BEAM,Runes.NEEDLE_CIRCLE,Runes.HARM,Runes.ON_HIT,Runes.BURST,Runes.BLOOM_CIRCLE,Runes.FROST)).root();
				CastEngine.cast(new Cast(p),root);
				check(target.getHealth()<12,"linked group lands its effects after a circle-modified beam");
			});ctx.waitTicks(4);shot(ctx,"linked_needle_bloom_impact");
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var g=group(Runes.ECLIPSE_CIRCLE);Cast c=new Cast(p);c.prepareCircle(g);check(c.circleEffect(g,EffectKind.HARMFUL).power==.9,"day pays Eclipse penalty");
				var knot=Knots.def(Knots.id(List.of(Runes.BEAM,Runes.CONFLUENCE_CIRCLE,Runes.FIRE,Runes.SHOCK),"Braided beam")).orElseThrow();
				check(CircleDisciplines.selected(SpellCompiler.compile(List.of(knot)).root().groups.getFirst().shapeMods)==Runes.CONFLUENCE_CIRCLE,"fused Knot preserves discipline");
			});
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.level().setBlockAndUpdate(new net.minecraft.core.BlockPos(0,160,0),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());});ctx.waitTicks(5);
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.isInWater(),"Tempest wet fixture reaches real water");
				var g=group(Runes.TEMPEST_CIRCLE);Cast c=new Cast(p);c.prepareCircle(g);
				p.level().setBlockAndUpdate(new net.minecraft.core.BlockPos(0,160,0),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
				check(c.circleEffect(g,EffectKind.HARMFUL).power==1.2,"wet Tempest activates and preserves its release snapshot");
				p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.setHealth(5);
				CastEngine.cast(new Cast(p),SpellCompiler.compile(List.of(Runes.SELF,Runes.HEAL)).root());float baseHeal=p.getHealth()-5;
				p.setHealth(5);CastEngine.cast(new Cast(p),SpellCompiler.compile(List.of(Runes.SELF,Runes.MERCY_CIRCLE,Runes.HEAL)).root());
				check(Math.abs((p.getHealth()-5)/baseHeal-1.2)<.001,"Mercy strengthens actual healing");p.setGameMode(GameType.CREATIVE);
			});
		}
	}
	private static SpellPlan.Group group(RuneDef rune){return SpellCompiler.compile(List.of(Runes.BOLT,rune,Runes.HARM)).root().groups.getFirst();}
	private static double damage(ServerPlayer p,RuneDef circle){
		// Named fixtures cannot randomly awaken as Runebound with bonus health/equipment.
		var target=EntityTypes.HUSK.create(p.level(),EntitySpawnReason.COMMAND);target.setCustomName(net.minecraft.network.chat.Component.literal("Circle damage fixture"));target.setNoAi(true);target.snapTo(.5,160,5.5,0,0);p.level().addFreshEntity(target);float before=target.getHealth();
		var g=SpellCompiler.compile(circle==null?List.of(Runes.BOLT,Runes.HARM):List.of(Runes.BOLT,circle,Runes.HARM)).root().groups.getFirst();
		Cast cast=new Cast(p);cast.prepareCircle(g);CastEngine.onHit(cast,g,new Cast.Hit(List.of(target),target.position(),new Vec3(0,0,1),p.position(),null,null,false),null);
		double result=before-target.getHealth();target.discard();check(result>0,"real damage was applied for "+(circle==null?"baseline":circle.name()));return result;
	}
	private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
	private static void shot(ClientGameTestContext ctx,String name){ctx.takeScreenshot(TestScreenshotOptions.of("circle_"+name).disableCounterPrefix());}
}
