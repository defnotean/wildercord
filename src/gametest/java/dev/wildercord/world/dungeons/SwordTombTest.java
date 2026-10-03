package dev.wildercord.world.dungeons;

import dev.wildercord.aura.*;
import dev.wildercord.aura.world.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import java.util.*;

/** Real structure geometry, client gate/challenge packets, locked attack evasion and saved finite rewards. */
public final class SwordTombTest implements FabricClientGameTest {
	@Override public void runTest(ClientGameTestContext c){
		try(var world=c.worldBuilder().create()){
			c.waitTicks(40);world.getServer().runCommand("gamerule spawn_mobs false");world.getServer().runCommand("difficulty normal");world.getServer().runCommand("time set 6000");
			BlockPos altar=world.getServer().computeOnServer(server->{
				var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);
				BlockPos chosen=null;
				for(var facing:Direction.Plane.HORIZONTAL)for(int variant=0;variant<3;variant++){
					int x=variant*128+facing.get2DDataValue()*512;SwordTombPiece piece;
					do{piece=new SwordTombPiece(x++,100,128,facing);}while(piece.variant()!=variant);
					var b=piece.getBoundingBox();for(int cx=b.minX()>>4;cx<=b.maxX()>>4;cx++)for(int cz=b.minZ()>>4;cz<=b.maxZ()>>4;cz++)l.getChunk(cx,cz);
					piece.postProcess(l,l.structureManager(),l.getChunkSource().getGenerator(),RandomSource.create(56),b,new ChunkPos(b.minX()>>4,b.minZ()>>4),BlockPos.ZERO);
					var at=piece.localPosition(20,1,44);var t=(TombReliquaryEntity)l.getBlockEntity(at);check(t.authentic(),"Generated tomb has provenance");
					check(t.arena().equals(piece.localPosition(20,1,36)),"Reliquary facing maps to arena in every orientation");
					check(l.getBlockState(piece.localPosition(20,1,15)).getValue(IntentGate.STAGE)==2,"Outer gate asks for Flow");
					check(l.getBlockState(piece.localPosition(20,1,24)).getValue(IntentGate.STAGE)==3,"Inner gate asks for Edge");
					check(l.getBlockState(piece.localPosition(20,13,1)).isAir() && l.getBlockState(piece.localPosition(20,14,1)).isAir(),"Barrow preserves two-block headroom beneath the entrance arch");
					var restored=BlockEntity.loadStatic(at,t.getBlockState(),t.saveWithFullMetadata(l.registryAccess()),l.registryAccess());
					check(restored instanceof TombReliquaryEntity r && r.authentic() && r.guardian()==null,"Dormant provenance survives save/load");
					if(chosen==null)chosen=at;
				}
				return chosen;
			});
			c.runOnClient(mc->{mc.getWindow().setWindowed(1280,720);mc.options.guiScale().set(2);mc.resizeGui();});
			var facing=world.getServer().computeOnServer(s->s.overworld().getBlockState(altar).getValue(TombReliquary.FACING));
			BlockPos inner=altar.relative(facing.getOpposite(),20),outer=altar.relative(facing.getOpposite(),29);
			placePlayer(world,outer.relative(facing.getOpposite(),2),1);c.waitTicks(15);use(c,outer);c.waitTicks(10);
			world.getServer().runOnServer(s->check(s.overworld().getBlockState(outer).is(SwordTombs.GATE),"Glow cannot open the Flow gate"));
			placePlayer(world,outer.relative(facing.getOpposite(),2),2);c.waitTicks(10);use(c,outer);c.waitTicks(10);
			world.getServer().runOnServer(s->check(s.overworld().getBlockState(outer).isAir() && s.overworld().getBlockState(outer.above(2)).isAir(),"Flow opens the entire panel group"));
			placePlayer(world,inner.relative(facing.getOpposite(),2),2);c.waitTicks(10);use(c,inner);c.waitTicks(10);
			world.getServer().runOnServer(s->check(s.overworld().getBlockState(inner).is(SwordTombs.GATE),"Flow cannot open the Edge gate"));
			placePlayer(world,inner.relative(facing.getOpposite(),2),3);c.waitTicks(10);use(c,inner);c.waitTicks(10);
			world.getServer().runOnServer(s->check(s.overworld().getBlockState(inner).isAir(),"Edge opens the inner threshold"));
			placePlayer(world,altar.relative(facing.getOpposite(),2),3);c.waitTicks(15);aim(c,altar,0);shot(c,"sword_tomb_reliquary");use(c,altar);c.waitTicks(5);use(c,altar);c.waitTicks(5);
			UUID keeper=world.getServer().computeOnServer(s->{var t=(TombReliquaryEntity)s.overworld().getBlockEntity(altar);check(t.guardian()!=null,"Challenge spawns a keeper");check(dev.wildercord.cast.Spirits.isBoss(s.overworld().getEntity(t.guardian())),"Keeper participates in shared boss protections and blade progression");check(s.overworld().getEntitiesOfClass(Gravekeeper.class,new AABB(altar).inflate(20)).size()==1,"Repeated challenge cannot duplicate the keeper");return t.guardian();});
			BlockPos home=world.getServer().computeOnServer(s->((Gravekeeper)s.overworld().getEntity(keeper)).home());
			placePlayer(world,home.offset(0,0,4),3);c.waitTicks(5);aim(c,home,1.2);
			await(c,world,keeper,SwordTombRules.SWEEP);lane(world,keeper,0);c.waitTicks(8);aim(c,home,1.2);shot(c,"sword_tomb_sweep_windup");
			world.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().setHealth(20));c.waitTicks(38);
			world.getServer().runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().getHealth()<20,"Staying in the locked sweep takes damage"));
			world.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().setHealth(20));
			await(c,world,keeper,SwordTombRules.THRUST);lane(world,keeper,0);c.waitTicks(8);aim(c,home,1.2);shot(c,"sword_tomb_thrust_windup");
			lane(world,keeper,3);c.waitTicks(38);
			world.getServer().runOnServer(s->check(s.getPlayerList().getPlayers().getFirst().getHealth()==20,"Stepping out of the locked thrust lane avoids damage"));
			await(c,world,keeper,SwordTombRules.GUARD);c.waitTicks(8);aim(c,home,1.2);shot(c,"sword_tomb_forward_guard");
			world.getServer().runOnServer(s->{var k=(Gravekeeper)s.overworld().getEntity(keeper);var p=s.getPlayerList().getPlayers().getFirst();var at=k.position().add(k.getViewVector(1).multiply(1,0,1).normalize().scale(2));p.teleportTo(s.overworld(),at.x,at.y,at.z,Set.<Relative>of(),180,0,false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_AXE));k.hurtServer(s.overworld(),s.overworld().damageSources().playerAttack(p),8);check(k.move()==SwordTombRules.BROKEN,"Frontal axe breaks the actual guard");});
			c.waitTicks(8);aim(c,home,1.2);shot(c,"sword_tomb_broken_guard");c.waitTicks(15);
			world.getServer().runOnServer(s->{var k=(Gravekeeper)s.overworld().getEntity(keeper);var saved=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,s.registryAccess());k.saveWithoutId(saved);var restored=SwordTombs.KEEPER.create(s.overworld(),net.minecraft.world.entity.EntitySpawnReason.LOAD);restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,s.registryAccess(),saved.buildResult()));check(restored.home().equals(k.home()) && restored.getHealth()==k.getHealth(),"Keeper home and damaged health survive save/load");check(restored.move()==SwordTombRules.RECOVER,"Reload grants recovery instead of releasing a stale attack");restored.discard();});
			world.getServer().runOnServer(s->{var k=(Gravekeeper)s.overworld().getEntity(keeper);var p=s.getPlayerList().getPlayers().getFirst();k.setHealth(1);k.hurtServer(s.overworld(),s.overworld().damageSources().playerAttack(p),20);check(((TombReliquaryEntity)s.overworld().getBlockEntity(altar)).cleared(),"Keeper death resolves the saved encounter");});
			placePlayer(world,altar.relative(facing.getOpposite(),2),3);c.waitTicks(15);use(c,altar);c.waitTicks(10);
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(scrolls(p)==2,"Participant receives two distinct technique scrolls");var t=(TombReliquaryEntity)s.overworld().getBlockEntity(altar);var restored=(TombReliquaryEntity)BlockEntity.loadStatic(altar,t.getBlockState(),t.saveWithFullMetadata(s.registryAccess()),s.registryAccess());check(restored.cleared() && restored.guardian()==null,"Cleared state survives load");s.overworld().setBlockEntity(restored);});
			use(c,altar);c.waitTicks(10);world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(scrolls(p)==2,"Saved reward ledger blocks a second claim");var parts=new HashSet<String>();for(int i=0;i<p.getInventory().getContainerSize();i++){var part=p.getInventory().getItem(i).get(TechniqueScrollItem.PART);if(part!=null)parts.add(part);}check(parts.size()==2,"The two awarded scrolls carry different valid parts");});aim(c,altar,0);shot(c,"sword_tomb_rewards");
			world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int i=0;i<p.getInventory().getContainerSize();i++){var item=p.getInventory().getItem(i);if(item.has(DataComponents.WRITTEN_BOOK_CONTENT)){p.setItemInHand(InteractionHand.MAIN_HAND,item.copy());break;}}});c.waitTicks(10);c.runOnClient(mc->mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND));c.waitTicks(10);c.runOnClient(mc->check(mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen,"The actual awarded testament opens for reading"));shot(c,"sword_tomb_testament");
		}
		try(var natural=c.worldBuilder().setUseConsistentSettings(false).create()){
			c.waitTicks(40);natural.getServer().runCommand("gamerule spawn_mobs false");natural.getServer().runCommand("time set 6000");
			BlockPos habitat=natural.getServer().computeOnServer(s->{var source=s.overworld().getChunkSource();var b=source.getGenerator().getBiomeSource().findBiomeHorizontal(1024,128,1024,6400,32,v->v.is(net.minecraft.world.level.biome.Biomes.PLAINS),RandomSource.create(811),true,source.randomState());check(b!=null,"Normal world has a surface tomb habitat");return b.getFirst();});
			BlockPos found=null;
			for(int i=0;i<16 && found==null;i++){
				int x=habitat.getX()+(i%4)*64,z=habitat.getZ()+(i/4)*64;
				natural.getServer().runOnServer(s->{for(int cx=(x>>4)-4;cx<=(x>>4)+4;cx++)for(int cz=(z>>4)-4;cz<=(z>>4)+4;cz++)s.overworld().getChunk(cx,cz);});
				natural.getServer().runCommand("place structure wildercord:sword_tomb "+x+" 70 "+z);
				found=natural.getServer().computeOnServer(s->{for(int cx=(x>>4)-4;cx<=(x>>4)+4;cx++)for(int cz=(z>>4)-4;cz<=(z>>4)+4;cz++)for(var be:s.overworld().getChunk(cx,cz).getBlockEntities().values())if(be instanceof TombReliquaryEntity t && t.authentic())return t.getBlockPos();return null;});
			}
			check(found!=null,"Registered tomb placement generates an authentic reliquary on normal terrain");BlockPos at=found;
			BlockPos entrance=natural.getServer().computeOnServer(s->at.relative(s.overworld().getBlockState(at).getValue(TombReliquary.FACING).getOpposite(),42).above(12));
			natural.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);p.teleportTo(s.overworld(),entrance.getX()+7.5,entrance.getY()+5,entrance.getZ()+7.5,Set.<Relative>of(),0,25,false);p.getAbilities().flying=true;p.onUpdateAbilities();});
			c.waitTicks(30);aim(c,entrance,0);c.waitTicks(30);natural.getConnection().waitForChunksRender();shot(c,"sword_tomb_natural_entrance");
		}
	}
	private static void placePlayer(TestSingleplayerContext w,BlockPos at,int stage){w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(s.overworld(),at.getX()+.5,at.getY(),at.getZ()+.5,Set.<Relative>of(),0,0,false);p.setAttached(AuraAttachments.AURA,new AuraAttachments.Data("stone",stage,0,70,0));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));p.fallDistance=0;});}
	private static void await(ClientGameTestContext c,TestSingleplayerContext w,UUID id,int move){for(int i=0;i<240;i++){if(w.getServer().computeOnServer(s->s.overworld().getEntity(id) instanceof Gravekeeper k && k.move()==move))return;c.waitTicks(1);}throw new AssertionError("Keeper did not reach move "+move);}
	private static int scrolls(net.minecraft.server.level.ServerPlayer p){int n=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).has(TechniqueScrollItem.PART))n+=p.getInventory().getItem(i).getCount();return n;}
	private static void use(ClientGameTestContext c,BlockPos p){c.runOnClient(mc->mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false)));}
	private static void shot(ClientGameTestContext c,String n){c.takeScreenshot(TestScreenshotOptions.of(n).disableCounterPrefix());}
	private static void lane(TestSingleplayerContext w,UUID id,double sideways){w.getServer().runOnServer(s->{var k=(Gravekeeper)s.overworld().getEntity(id);var d=k.attackDirection();var v=k.position().add(d.scale(4)).add(new Vec3(-d.z,0,d.x).scale(sideways));var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),v.x,v.y,v.z,Set.<Relative>of(),p.getYRot(),p.getXRot(),false);p.setHealth(20);});}
	private static void aim(ClientGameTestContext c,BlockPos at,double lift){c.runOnClient(mc->{var v=Vec3.atCenterOf(at).add(0,lift,0).subtract(mc.player.getEyePosition());mc.player.setYRot((float)(Math.atan2(-v.x,v.z)*180/Math.PI));mc.player.setXRot((float)(-Math.atan2(v.y,Math.sqrt(v.x*v.x+v.z*v.z))*180/Math.PI));});}
	private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
