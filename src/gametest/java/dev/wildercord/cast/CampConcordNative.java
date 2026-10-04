package dev.wildercord.cast;
import dev.wildercord.config.*;
import dev.wildercord.content.*;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Supplied dry observation terrain and progression; all accepted casts still use client packets. */
final class CampConcordNative {
 static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
 static void arena(MinecraftServer s){var l=s.overworld();for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++){l.setBlock(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState(),2);for(int y=101;y<=106;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
  var p=player(s);p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.setHealth(p.getMaxHealth());Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));Spellbooks.set(p,Spellbooks.get(p).withStarterGiven().learn(Runes.TOUCH.id()).learn(Runes.BOLT.id()).learn(Runes.ECHO.id()).learn("wildercord:watchweft").learn("wildercord:manabraid"));aim(p,new Vec3(.5,100.999,.5));}
 static void aim(ServerPlayer p,Vec3 target){var at=new Vec3(2.5,101,2.5);var d=target.subtract(at.add(0,p.getEyeHeight(),0));p.teleportTo(p.level(),at.x,at.y,at.z,Set.<Relative>of(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())),false);}
 static int edit(ServerPlayer p,List<RuneDef> rs){check(SpellCaster.edit(p,0,rs.stream().map(RuneDef::id).toList())==null,"Actual normal editor admits supplied learned rune route");return Heart.manaCost(p,SpellCompiler.compile(rs),Heart.secretCost(p,rs)*Mastery.costFactor(p,rs));}
 static void cast(ClientGameTestContext c){c.runOnClient(mc->{mc.gui.setScreen(null);ClientPlayNetworking.send(new WildercordNetworking.CastSpell(0));});}
 static Cast.Hit hit(){return new Cast.Hit(List.of(),new Vec3(.5,101,.5),new Vec3(0,1,0),new Vec3(.5,101,.5),new BlockPos(0,100,0),net.minecraft.core.Direction.UP,false);}
 @SuppressWarnings("unchecked") static <T>T copy(T r,Map<String,Object> changes){try{var parts=r.getClass().getRecordComponents();var ts=new Class<?>[parts.length];var args=new Object[parts.length];for(int i=0;i<parts.length;i++){ts[i]=parts[i].getType();args[i]=changes.getOrDefault(parts[i].getName(),parts[i].getAccessor().invoke(r));}return (T)r.getClass().getDeclaredConstructor(ts).newInstance(args);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 static void config(WildercordConfig value){try{var f=Config.class.getDeclaredField("current");f.setAccessible(true);f.set(null,value);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
