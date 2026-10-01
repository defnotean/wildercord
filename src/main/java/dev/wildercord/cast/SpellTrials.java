package dev.wildercord.cast;
import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;
/** Optional challenges use paid casts and actual dummy hits. First completions award a single page. */
public final class SpellTrials {
 private SpellTrials() {}
 private static final AttachmentType<List<String>> COMPLETED=AttachmentRegistry.create(Wildercord.id("completed_trials"),b->b.initializer(List::of).persistent(com.mojang.serialization.Codec.STRING.listOf(0,3)).copyOnDeath());
 private static final WeakHashMap<ServerPlayer,Attempt> RUNNING=new WeakHashMap<>();
 private static final class Attempt {String kind;long until;double mana;int hits,casts;Set<String> shapes=new HashSet<>(),fusions=new HashSet<>();Attempt(String k,long t){kind=k;until=t;}}
 public static void init(){ServerLifecycleEvents.SERVER_STOPPED.register(s->RUNNING.clear());ServerTickEvents.END_SERVER_TICK.register(s->{
  for(var e:new ArrayList<>(RUNNING.entrySet()))if(e.getKey().isRemoved()||!e.getKey().isAlive()||e.getKey().isCreative()||e.getKey().isSpectator()||!e.getKey().level().dimension().equals(PracticeRoom.DIMENSION)||e.getKey().level().getGameTime()>e.getValue().until){RUNNING.remove(e.getKey());e.getKey().sendSystemMessage(Component.translatable("message.wildercord.trial_ended"));}
 });}
 public static int start(ServerPlayer p,String kind){if(!List.of("precision","variety","fusion").contains(kind)||!p.level().dimension().equals(PracticeRoom.DIMENSION)||p.isCreative()||p.isSpectator()){
  p.sendSystemMessage(Component.translatable("message.wildercord.trial_start_hint"));return 0;}
  RUNNING.put(p,new Attempt(kind,p.level().getGameTime()+1800));p.sendSystemMessage(Component.translatable("message.wildercord.trial_started",Component.translatable("trial.wildercord."+kind)));return 1;
 }
 public static void cast(ServerPlayer p,List<RuneDef> runes,double mana){var a=RUNNING.get(p);if(a==null||!valid(p,a))return;a.casts++;a.mana+=mana;
  for(var rune:Knots.flatten(runes)){if(rune.family()==RuneFamily.SHAPE)a.shapes.add(rune.id());if(Fusions.recipeFor(rune).isPresent()||WovenRunes.isWoven(rune))a.fusions.add(rune.id());}check(p,a);
 }
 public static void hit(ServerPlayer p,float damage){var a=RUNNING.get(p);if(a==null||damage<=0||!valid(p,a))return;if(a.kind.equals("precision")&&damage>6){RUNNING.remove(p);p.sendSystemMessage(Component.translatable("message.wildercord.trial_precision_failed"));return;}a.hits++;check(p,a);}
 private static boolean valid(ServerPlayer p,Attempt a){if(p.isAlive()&&!p.isRemoved()&&!p.isCreative()&&!p.isSpectator()&&p.level().dimension().equals(PracticeRoom.DIMENSION)&&p.level().getGameTime()<=a.until)return true;RUNNING.remove(p);p.sendSystemMessage(Component.translatable("message.wildercord.trial_ended"));return false;}
 private static void check(ServerPlayer p,Attempt a){if(a.mana>100){RUNNING.remove(p);p.sendSystemMessage(Component.translatable("message.wildercord.trial_mana_failed"));return;}
  boolean won=a.hits>=5&&switch(a.kind){case "variety"->a.shapes.size()>=4;case "fusion"->a.fusions.size()>=3;default->a.casts>=5;};
  if(!won)return;RUNNING.remove(p);var done=new ArrayList<>(p.getAttachedOrElse(COMPLETED,List.of()));if(!done.contains(a.kind)){done.add(a.kind);p.setAttached(COMPLETED,List.copyOf(done));var reward=new ItemStack(WildercordItems.TORN_PAGE);p.getInventory().add(reward);if(!reward.isEmpty())p.drop(reward,false,net.minecraft.util.Prediction.SERVER_ONLY);}
  p.sendSystemMessage(Component.translatable("message.wildercord.trial_won",Component.translatable("trial.wildercord."+a.kind),String.format(java.util.Locale.ROOT,"%.1f",a.mana)));
 }
}
