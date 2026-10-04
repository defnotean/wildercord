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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.*;
/** A real paid helpful cast cannot grant a wild non-ally bird a water response or helpful benefit. */
public final class SiltcrestWaterRefusalTest implements FabricClientGameTest {
 private SiltcrestBittern bird;
 public void runTest(ClientGameTestContext c){try(var w=c.worldBuilder().create()){c.waitTicks(25);w.getServer().runCommand("gamerule spawn_mobs false");w.getServer().runCommand("time set 6000");
  var original=w.getServer().computeOnServer(s->Config.get());
  try{w.getServer().runOnServer(s->{current(copy(original,Map.of("manaRegenMultiplier",0.0)));floor(s.overworld());var p=s.getPlayerList().getPlayers().getFirst();bird=bird(s.overworld(),.5,-1.5);p.setGameMode(GameType.SURVIVAL);place(p,.5,101,-3.5,bird.getBoundingBox().getCenter());Spellbooks.setCord(p,new ItemStack(WildercordItems.ECHO_CORD));Spellbooks.set(p,Spellbooks.get(p).withStarterGiven().learn(Runes.TOUCH.id()).learn(Runes.TIDEBREATH.id()));check(SpellCaster.edit(p,0,List.of(Runes.TOUCH.id(),Runes.TIDEBREATH.id()))==null,"Actual known spell editor");check(!Targets.canHelp(p,bird),"Actual wild bird is not a spell ally");Spellbooks.setMana(p,200);});
   c.waitTicks(5);int price=w.getServer().computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return Heart.manaCost(p,SpellCompiler.compile(List.of(Runes.TOUCH,Runes.TIDEBREATH)),Heart.secretCost(p,List.of(Runes.TOUCH,Runes.TIDEBREATH))*Mastery.costFactor(p,List.of(Runes.TOUCH,Runes.TIDEBREATH)));});
   c.runOnClient(mc->ClientPlayNetworking.send(new WildercordNetworking.CastSpell(0)));c.waitTicks(4);
   w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(price>0&&Spellbooks.mana(p)==200-price,"Refused helpful target still retains actual paid spell cost");check(bird.controlReady()==0&&!bird.hasEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING)&&!bird.hasEffect(net.minecraft.world.effect.MobEffects.DOLPHINS_GRACE)&&bird.pose()!=SiltcrestBittern.PREENING,"Helpful refusal produces no bird response, no target benefit and no successful preening");});
  }finally{w.getServer().runOnServer(s->current(original));}
 }}
 @SuppressWarnings("unchecked")private static <T>T copy(T record,Map<String,Object> changes){try{var parts=record.getClass().getRecordComponents();var types=new Class<?>[parts.length];var args=new Object[parts.length];for(int i=0;i<parts.length;i++){types[i]=parts[i].getType();args[i]=changes.getOrDefault(parts[i].getName(),parts[i].getAccessor().invoke(record));}return (T)record.getClass().getDeclaredConstructor(types).newInstance(args);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
 private static void current(WildercordConfig v){try{var f=Config.class.getDeclaredField("current");f.setAccessible(true);f.set(null,v);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
}
