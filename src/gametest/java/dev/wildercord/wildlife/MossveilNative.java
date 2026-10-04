package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraft.core.BlockPos;
import java.util.*;
import java.util.function.Predicate;
/** Native supplied habitat/ingredients only; taming, sitting, following and preparation are real runtime behavior. */
final class MossveilNative{
 static final BlockPos CANOPY=new BlockPos(1,102,1);
 static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
 static void floor(ServerLevel l){for(int x=-7;x<=10;x++)for(int z=-7;z<=8;z++){l.setBlock(new BlockPos(x,100,z),Blocks.MOSS_BLOCK.defaultBlockState(),2);for(int y=101;y<=108;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}l.setBlock(CANOPY,FungalGarden.NURSERY.defaultBlockState(),2);check(MossveilHome.supported(l,CANOPY),"Actual supported Fungal Nursery");}
 static void place(ServerPlayer p,double x,double z){p.teleportTo(p.level(),x,101,z,Set.<Relative>of(),0,20,false);}
 static MossveilDormouse mouse(ServerLevel l,double x,double z){var e=MossveilContent.DORMOUSE.create(l,EntitySpawnReason.COMMAND);check(e!=null,"Registered custom Dormouse factory");e.setPersistenceRequired();e.snapTo(x,101,z,0,0);check(!e.isNoAi()&&l.addFreshEntity(e),"Actual tracked original companion retains real AI");return e;}
 static void await(ClientGameTestContext c,TestSingleplayerContext w,Predicate<MinecraftServer> gate,int limit,String why){for(int i=0;i<limit;i++){if(w.getServer().computeOnServer(gate::test))return;c.waitTicks(1);}throw new AssertionError(why);}
 static void interact(ClientGameTestContext c,int id){c.runOnClient(mc->{var e=mc.level.getEntity(id);check(e instanceof MossveilDormouse,"Actual custom client creature tracked");mc.gameMode.interact(mc.player,e,new EntityHitResult(e,e.getBoundingBox().getCenter()),InteractionHand.MAIN_HAND);});c.waitTicks(3);}
 static void tame(ClientGameTestContext c,TestSingleplayerContext w,MossveilDormouse e){
  w.getServer().runOnServer(s->{var p=player(s);p.setGameMode(GameType.SURVIVAL);place(p,1.5,-.5);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FungalGarden.GILLS,6));});c.waitTicks(4);
  for(int i=0;i<3;i++){await(c,w,s->MossveilDormouse.clock(s.overworld())>=e.feedReady(),130,"Actual feeding rest elapsed without forcing a timestamp");interact(c,e.getId());final int step=i;w.getServer().runOnServer(s->{check(player(s).getMainHandItem().getCount()==5-step,"Actual client Survival packet consumes one Gill per accepted feeding");check(step==2?e.isTame()&&e.isOwnedBy(player(s)):e.feedings()==step+1&&!e.isTame(),"Real paid taming progress and ownership");});}
  await(c,w,s->e.isInSittingPose(),30,"Actual owner order settles into curl through ordinary AI");
 }
 static ServerPlayer guest(ServerPlayer p){var g=new FakePlayer(p.level(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"MossveilGuest")){};g.setGameMode(GameType.SURVIVAL);g.snapTo(2.5,101,-.5,0,0);p.level().addNewPlayer(g);return g;}
 static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
}
