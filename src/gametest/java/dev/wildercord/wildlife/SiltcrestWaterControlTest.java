package dev.wildercord.wildlife;
import static dev.wildercord.wildlife.SiltcrestNative.*;
import dev.wildercord.cast.*;
import dev.wildercord.config.*;
import dev.wildercord.content.*;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.*;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.*;
/** Actual allied Survival Tidebreath packet interrupts real coiling only after actual target effect admission. Needs the explicit owner hook. */
public final class SiltcrestWaterControlTest implements FabricClientGameTest {
 private SiltcrestBittern bird;
 public void runTest(ClientGameTestContext c){boolean shift=c.computeOnClient(mc->mc.options.keyShift.isDown());try(var w=c.worldBuilder().create()){c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 18000");w.getServer().runCommand("weather clear");var original=w.getServer().computeOnServer(s->Config.get());
  try{w.getServer().runOnServer(s->{current(copy(original,Map.of("manaRegenMultiplier",0.0)));floor(s.overworld());var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.CREATIVE);place(p,.5,101,-3.5,new net.minecraft.world.phys.Vec3(.5,101.8,-1.5));for(int i=0;i<3;i++)fish(s.overworld(),i);bird=bird(s.overworld(),.5,.5);Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));Spellbooks.set(p,Spellbooks.get(p).withStarterGiven().learn(Runes.TOUCH.id()).learn(Runes.TIDEBREATH.id()));check(SpellCaster.edit(p,0,List.of(Runes.TOUCH.id(),Runes.TIDEBREATH.id()))==null,"Ordinary spell editor accepts known Tidebreath");Spellbooks.setMana(p,200);});
   w.getServer().runCommand("team add bittern_observers");String member=w.getServer().computeOnServer(s->s.getPlayerList().getPlayers().getFirst().getName().getString());w.getServer().runCommand("team join bittern_observers "+member);w.getServer().runCommand("team join bittern_observers "+bird.getUUID());
   w.getServer().runOnServer(s->check(Targets.canHelp(s.getPlayerList().getPlayers().getFirst(),bird),"Actual scoreboard ally admission precedes helpful water control; wild strangers are not silently made allies"));
   c.runOnClient(mc->mc.options.keyShift.setDown(true));c.waitTicks(5);w.getServer().runOnServer(s->s.getPlayerList().getPlayers().getFirst().setGameMode(GameType.SURVIVAL));
   await(c,w,500,s->bird.pose()==SiltcrestBittern.COILING,"Actual stalking coils near genuine crouched Survival observer");
   int price=w.getServer().computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.isShiftKeyDown(),"Real client crouch reaches server");place(p,bird.getX(),101,bird.getZ()-2.5,bird.getBoundingBox().getCenter());return Heart.manaCost(p,SpellCompiler.compile(List.of(Runes.TOUCH,Runes.TIDEBREATH)),Heart.secretCost(p,List.of(Runes.TOUCH,Runes.TIDEBREATH))*Mastery.costFactor(p,List.of(Runes.TOUCH,Runes.TIDEBREATH)));});
   c.waitTicks(2);c.runOnClient(mc->ClientPlayNetworking.send(new WildercordNetworking.CastSpell(0)));c.waitTicks(4);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(price>0&&Spellbooks.mana(p)==200-price,"Actual Survival cast pays authoritative finite mana price");check(bird.controlReady()>s.overworld().getGameTime()&&bird.pose()==SiltcrestBittern.PREENING&&bird.hasEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING),"Actual Tidebreath recipient change owns finite hunt interruption/preening");check(bird.huntReady()<=s.overworld().getGameTime()+200&&bird.preyPool(s.overworld()).size()==3,"Water response creates no meal or fish death");});
  }finally{w.getServer().runOnServer(s->current(original));c.runOnClient(mc->mc.options.keyShift.setDown(shift));}
 }}
 @SuppressWarnings("unchecked")private static <T>T copy(T record,Map<String,Object> changes){try{var parts=record.getClass().getRecordComponents();var types=new Class<?>[parts.length];var args=new Object[parts.length];for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();args[i]=changes.getOrDefault(parts[i].getName(),parts[i].getAccessor().invoke(record));}return (T)record.getClass().getDeclaredConstructor(types).newInstance(args);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void current(WildercordConfig v){try{var f=Config.class.getDeclaredField("current");f.setAccessible(true);f.set(null,v);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
