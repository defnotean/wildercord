package dev.wildercord.wildlife;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.fish.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Predicate;
/** Provided real full-height clay bank, physically lined shallow pond and ordinary AI witnesses; no creature pose/clock injection. */
final class SiltcrestNative {
 private SiltcrestNative(){}
 static void floor(ServerLevel l){for(int x=-8;x<=10;x++)for(int z=-8;z<=8;z++){l.setBlock(new BlockPos(x,99,z),Blocks.CLAY.defaultBlockState(),2);l.setBlock(new BlockPos(x,100,z),Blocks.CLAY.defaultBlockState(),2);for(int y=101;y<=105;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}for(int x=1;x<=4;x++)for(int z=0;z<=3;z++)l.setBlock(new BlockPos(x,100,z),Blocks.WATER.defaultBlockState(),2);}
 static SiltcrestBittern bird(ServerLevel l,double x,double z){var b=SiltcrestContent.BITTERN.create(l,EntitySpawnReason.COMMAND);check(b!=null,"Actual bittern factory");b.snapTo(x,101,z,0,0);b.setPersistenceRequired();l.addFreshEntity(b);return b;}
 static AbstractFish fish(ServerLevel l,int i){var f=EntityTypes.COD.create(l,EntitySpawnReason.COMMAND);check(f!=null,"Actual vanilla prey factory");f.snapTo(1.5+i%4,100.1,.5+(i/4)%4,0,0);l.addFreshEntity(f);return f;}
 static void observer(ServerPlayer p,Vec3 eyeTarget){p.setGameMode(GameType.CREATIVE);place(p,5.5,101,-3.5,eyeTarget);}
 static void place(ServerPlayer p,double x,double y,double z,Vec3 target){var d=target.subtract(new Vec3(x,y+p.getEyeHeight(),z));p.teleportTo(p.level(),x,y,z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);}
 static void await(ClientGameTestContext c,TestSingleplayerContext w,int ticks,Predicate<MinecraftServer> gate,String why){for(int n=0;n<ticks;n++){if(w.getServer().computeOnServer(gate::test))return;c.waitTicks(1);}throw new AssertionError(w.getServer().computeOnServer(s->why+" actualbirds="+s.overworld().getEntitiesOfClass(SiltcrestBittern.class,new net.minecraft.world.phys.AABB(-16,98,-16,16,110,16)).stream().map(b->"body="+b.position()+" pose="+b.pose()+" ground="+b.onGround()+" bank="+BitternHabitat.bank(s.overworld(),b.blockPosition())+" water="+b.isInWater()+" pool="+b.preyPool(s.overworld()).size()+" appetite="+b.huntReady()+" navigationDone="+b.getNavigation().isDone()).toList()));}
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
}
