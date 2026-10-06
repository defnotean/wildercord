package dev.wildercord.cast;

import dev.wildercord.content.PhysicalBlocks;
import dev.wildercord.content.MaterialOption;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Set;

/** Real terrain, source conservation, cancellation, collision, reactions, and original particle art. */
public final class PhysicalMagicTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext ctx){
		try(var world=ctx.worldBuilder().create()){
			ctx.waitTicks(40);world.getServer().runCommand("gamerule spawn_mobs false");world.getServer().runCommand("time set 6000");
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(p.level(),.5,100,-3.5,Set.<Relative>of(),0,0,false);});
			ctx.waitTicks(40);world.getServer().runCommand("fill -12 99 -12 12 99 18 polished_deepslate");
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),.5,100,-3.5,Set.<Relative>of(),0,0,false);});ctx.waitTicks(5);
			ctx.runOnClient(mc->{mc.getWindow().setWindowed(1600,900);mc.options.setCameraType(CameraType.FIRST_PERSON);});
			for(var rune:List.of(Runes.STRATA_RISE,Runes.CINDER_BULWARK,Runes.ROOT_BULWARK)){
				Cast cast=world.getServer().computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();Cast c=new Cast(p);Effects.apply(c,node(rune),wallHit(p));return c;});
				ctx.waitTicks(12);
				world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(PhysicalMagic.active()==15,"wall creates exactly fifteen cells");check(!p.level().getBlockState(new BlockPos(0,100,3)).getCollisionShape(p.level(),new BlockPos(0,100,3)).isEmpty(),"wall has real collision");check(TemporaryBlocks.recorded(p.level(),new BlockPos(0,100,3)),"wall undo is saved");});
				shot(ctx,"physical_"+rune.path());world.getServer().runOnServer(s->cast.cancel());ctx.waitTicks(3);
				world.getServer().runOnServer(s->{check(PhysicalMagic.active()==0,"cancel cleans wall");check(s.overworld().getBlockState(new BlockPos(0,100,3)).isAir(),"wall restores air");});
			}
			for(var rune:List.of(Runes.WIND_STEPS,Runes.RIME_CAUSEWAY,Runes.THUNDER_WALK)){
				Cast cast=world.getServer().computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();Cast c=new Cast(p);Effects.apply(c,node(rune),wallHit(p));return c;});ctx.waitTicks(18);
				world.getServer().runOnServer(s->{check(PhysicalMagic.active()==(rune==Runes.RIME_CAUSEWAY?15:5),"platform geometry differs by rune");var l=s.overworld();BlockPos first=new BlockPos(0,100,-2);check(PhysicalBlocks.isStep(l.getBlockState(first)),"platform is a real block");check(!l.getBlockState(first).getCollisionShape(l,first).isEmpty(),"platform can support players");});
				shot(ctx,"physical_"+rune.path());world.getServer().runOnServer(s->cast.cancel());ctx.waitTicks(3);
			}
			for(var rune:List.of(Runes.TIDAL_LIFT,Runes.BOILING_SURGE,Runes.THUNDER_TIDE)){
				world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),.5,100,-1.5,Set.<Relative>of(),0,0,false);p.level().setBlockAndUpdate(new BlockPos(0,100,1),Blocks.WATER.defaultBlockState());Effects.apply(new Cast(p),node(rune),waterHit(p));check(p.level().getBlockState(new BlockPos(0,100,1)).is(PhysicalBlocks.RESERVATION),"source reserved to prevent infinite-water refill");});
				ctx.waitTicks(12);shot(ctx,"physical_"+rune.path());
				world.getServer().runOnServer(s->check(PhysicalMagic.active()>0,"water actually lifts through world cells"));ctx.waitTicks(42);
				world.getServer().runOnServer(s->{check(s.overworld().getBlockState(new BlockPos(0,100,1)).is(Blocks.WATER),"source water returns");check(PhysicalMagic.active()==0,"lift leaves no constructs behind");s.overworld().setBlockAndUpdate(new BlockPos(0,100,1),Blocks.AIR.defaultBlockState());});ctx.waitTicks(10);
			}
			// An obstruction is not excavated to make a wall.
			int targetId=world.getServer().computeOnServer(s->{
				var p=s.getPlayerList().getPlayers().getFirst();
				var target=net.minecraft.world.entity.EntityTypes.HUSK.create(p.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				check(target!=null,"water target is created");
				// This scene needs an ordinary husk; entity-load Runebound promotion changes its health.
				target.addTag("wildercord.rolled");
				target.setNoAi(true);target.setNoGravity(true);target.snapTo(.5,102.5,5.5,0,0);
				check(p.level().addFreshEntity(target),"water target is admitted to the real world");
				waterReceipt("before",p,target);
				check(target.getHealth()==20 && target.getMaxHealth()==20 && target.getAbsorptionAmount()==0
					&& !target.hasAttached(dev.wildercord.player.WildercordAttachments.RUNEBOUND),
					"admitted water target has ordinary twenty-point health without Runebound or absorption");
				p.level().setBlockAndUpdate(new BlockPos(0,100,1),Blocks.WATER.defaultBlockState());
				Effects.apply(new Cast(p),node(Runes.TIDAL_LIFT),waterHit(p));return target.getId();
			});ctx.waitTicks(48);
			world.getServer().runOnServer(s->{var t=(net.minecraft.world.entity.LivingEntity)s.overworld().getEntity(targetId);
				waterReceipt("after",s.getPlayerList().getPlayers().getFirst(),t);
				check(t.getHealth()<20 && t.getHealth()>=16,"water attack deals one bounded hit; health="+t.getHealth());
				check(Reactions.has(t,Reactions.Mark.SOAKED),"water leaves a conductive soaked mark");t.discard();
				s.overworld().setBlockAndUpdate(new BlockPos(0,100,1),Blocks.AIR.defaultBlockState());
			});ctx.waitTicks(10);
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.level().setBlockAndUpdate(new BlockPos(0,100,3),Blocks.DIAMOND_BLOCK.defaultBlockState());Effects.apply(new Cast(p),node(Runes.STRATA_RISE),wallHit(p));});ctx.waitTicks(12);
			world.getServer().runOnServer(s->{check(s.overworld().getBlockState(new BlockPos(0,100,3)).is(Blocks.DIAMOND_BLOCK),"wall cannot replace valuable blocks");});ctx.waitTicks(165);
			world.getServer().runOnServer(s->{check(PhysicalMagic.active()==0,"expiry cleans loaded terrain");s.overworld().setBlockAndUpdate(new BlockPos(0,100,3),Blocks.AIR.defaultBlockState());});
			// The frost interaction condenses a platform without extending its lifetime.
			Cast steps=world.getServer().computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(p.level(),.5,100,-3.5,Set.<Relative>of(),0,0,false);Cast c=new Cast(p);Effects.apply(c,node(Runes.WIND_STEPS),wallHit(p));return c;});ctx.waitTicks(18);
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var pos=new BlockPos(0,100,-2);Effects.apply(new Cast(p),node(Runes.FROST),new Cast.Hit(List.of(),Vec3.atCenterOf(pos),new Vec3(0,0,1),p.position(),pos,Direction.UP,false));check(p.level().getBlockState(pos).is(PhysicalBlocks.RIME),"frost condenses a real wind step");steps.cancel();});ctx.waitTicks(3);
			// Every legacy palette entry becomes mod-owned, with materially different styles.
			world.getServer().runOnServer(s->{
				var p=s.getPlayerList().getPlayers().getFirst();
				var altar=new BlockPos(3,100,0);p.level().setBlockAndUpdate(altar,dev.wildercord.content.WildercordBlocks.FUSION_ALTAR.defaultBlockState());
				var menu=new dev.wildercord.menu.FusionAltarMenu(0,p.getInventory(),net.minecraft.world.inventory.ContainerLevelAccess.create(p.level(),altar));
				var pair=WovenRunes.bind(Runes.FIRE,Runes.HEAL);
				menu.getSlot(0).set(dev.wildercord.content.RuneItem.stack(pair));menu.getSlot(1).set(dev.wildercord.content.RuneItem.stack(Runes.SHOCK));
				menu.getSlot(3).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.AMETHYST_BLOCK));
				check(menu.clickMenuButton(p,0),"real altar accepts extending an exact weave");
				var made=dev.wildercord.content.RuneItem.runeOf(menu.getSlot(4).getItem()).orElseThrow();
				check(WovenRunes.contents(made).size()==3,"altar result preserves all three effects");
				check(menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty()&&menu.getSlot(3).getItem().isEmpty(),"fusion consumes inputs once");
				var soul=WovenRunes.bind(Runes.KINDLING,Runes.FIRE);
				dev.wildercord.player.Spellbooks.setCord(p,new net.minecraft.world.item.ItemStack(dev.wildercord.content.WildercordItems.ECHO_CORD));
				var book=dev.wildercord.player.Spellbooks.get(p).learn(Runes.SELF.id()).learn(soul.id());dev.wildercord.player.Spellbooks.set(p,book);
				p.setAttached(dev.wildercord.player.WildercordAttachments.INNATE,Runes.BLOOD_THREAD.id());
				SpellCaster.edit(p,0,List.of(Runes.SELF.id(),soul.id()));dev.wildercord.player.Spellbooks.setMana(p,100);dev.wildercord.player.Spellbooks.setReadyAt(p,0,0);
				p.setGameMode(GameType.SURVIVAL);SpellCaster.cast(p,0);
				check(dev.wildercord.player.Spellbooks.mana(p)==100,"foreign soul weave refuses before mana payment");
				check(dev.wildercord.player.Spellbooks.readyAt(p,0)==0,"foreign soul weave consumes no cooldown");p.setGameMode(GameType.CREATIVE);
				p.setGameMode(GameType.SURVIVAL);p.setHealth(20);p.removeAllEffects();
				p.setAttached(dev.wildercord.player.WildercordAttachments.SPELLGUARD,p.level().getGameTime()-1201);
				Cast burst=new Cast(p);for(int i=0;i<8;i++){Effects.readyToHurt(p);SpellDefence.hurt(p.level(),p,p.damageSources().magic(),4,burst);}
				check(p.isAlive()&&p.getHealth()==2,"spellguard sees the start of a multi-effect burst rather than each dwindling hit");
				p.setHealth(20);p.setGameMode(GameType.CREATIVE);
				p.teleportTo(p.level(),2.5,100,-1.5,Set.<Relative>of(),0,0,false);p.setGameMode(GameType.SURVIVAL);p.experienceLevel=3;p.setShiftKeyDown(true);
				p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(dev.wildercord.content.WildercordItems.BLANK_RUNE,2));
				p.gameMode.useItemOn(p,p.level(),p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(altar),Direction.UP,altar,false));
				check(p.experienceLevel==0&&p.getMainHandItem().getCount()==1,"native altar imprint consumes one blank and three XP levels");
				boolean found=false;for(int slot=0;slot<p.getInventory().getContainerSize();slot++)if(dev.wildercord.content.RuneItem.runeOf(p.getInventory().getItem(slot)).map(r->r==Runes.BLOOD_THREAD).orElse(false))found=true;
				check(found,"imprint gives exactly the caster's innate rune");
				p.gameMode.useItemOn(p,p.level(),p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(altar),Direction.UP,altar,false));
				check(p.getMainHandItem().getCount()==1,"insufficient XP leaves imprint material untouched");p.setShiftKeyDown(false);p.setGameMode(GameType.CREATIVE);
				p.teleportTo(p.level(),.5,100,-3.5,Set.<Relative>of(),0,0,false);
			});
			world.getServer().runOnServer(s->{
				check(SpellMaterials.custom(ParticleTypes.FLAME,Vec3.ZERO) instanceof MaterialOption,"legacy fire uses original sprite");
				check(!Vfx.theme("fire").mote().equals(Vfx.theme("wind").mote()),"elements have different material behavior");
				for(int style=0;style<12;style++)Fx.send(s.overworld(),new MaterialOption(style,0xFFFFFF,.45F,60),new Vec3(-4+style*.75,102,5),6,.25,.02);
			});ctx.waitTicks(5);shot(ctx,"physical_material_palette");
		}
	}
	private static SpellPlan.EffectNode node(RuneDef rune){return SpellCompiler.compile(List.of(Runes.BEAM,rune)).root().groups.getFirst().effects.getFirst();}
	private static Cast.Hit wallHit(ServerPlayer p){return new Cast.Hit(List.of(),new Vec3(.5,100,3.5),new Vec3(0,0,1),p.position(),new BlockPos(0,99,3),Direction.UP,false);}
	private static Cast.Hit waterHit(ServerPlayer p){return new Cast.Hit(List.of(),new Vec3(.5,100,8.5),new Vec3(0,0,1),p.position(),null,null,false);}
	private static void waterReceipt(String phase,ServerPlayer owner,net.minecraft.world.entity.LivingEntity target){
		check(target!=null,"water target remains in the real world for "+phase+" receipt");
		System.out.println("PHYSICAL_WATER_RECEIPT phase="+phase+" tick="+owner.level().getGameTime()
			+" owner="+owner.getUUID()+" ownerPos="+owner.position()+" target="+target.getUUID()+" targetPos="+target.position()
			+" health="+target.getHealth()+" maxHealth="+target.getMaxHealth()+" absorption="+target.getAbsorptionAmount()
			+" runebound="+target.hasAttached(dev.wildercord.player.WildercordAttachments.RUNEBOUND)
			+" adept="+target.entityTags().contains("wildercord.adept")+" soaked="+Reactions.has(target,Reactions.Mark.SOAKED)
			+" lastDamageByOwner="+(target.getLastDamageSource()!=null && target.getLastDamageSource().getEntity()==owner));
	}
	private static void shot(ClientGameTestContext c,String name){c.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());}
	private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
