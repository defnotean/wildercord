package dev.wildercord.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.spell.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Small permanent experiments, not repeatable chores. Rewards are granted once and offer fusion hints. */
public final class RuneResearch {
 private RuneResearch() {}
 public record Notebook(List<String> shapes,List<String> fusions,boolean garden,int rewarded) {
  public Notebook {shapes=List.copyOf(shapes);fusions=List.copyOf(fusions);}
  public static final Notebook EMPTY=new Notebook(List.of(),List.of(),false,0);
  static final Codec<Notebook> CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.listOf(0,64).fieldOf("shapes").forGetter(Notebook::shapes),
   Codec.STRING.listOf(0,128).fieldOf("fusions").forGetter(Notebook::fusions),Codec.BOOL.fieldOf("garden").forGetter(Notebook::garden),
   Codec.intRange(0,7).fieldOf("rewarded").forGetter(Notebook::rewarded)).apply(i,Notebook::new));
 }
 public static final AttachmentType<Notebook> NOTES=AttachmentRegistry.create(Wildercord.id("rune_research"),b->b.initializer(()->Notebook.EMPTY).persistent(Notebook.CODEC).copyOnDeath());
 public static void init() {}
 public static void cast(ServerPlayer p,List<RuneDef> runes) {
  if(p.isCreative()||p.isSpectator())return;
  var s=p.getAttachedOrElse(NOTES,Notebook.EMPTY);var shapes=new LinkedHashSet<>(s.shapes);var fusions=new LinkedHashSet<>(s.fusions);
  for(var rune:Knots.flatten(runes)) {
   if(rune.family()==RuneFamily.SHAPE && shapes.size()<64)shapes.add(rune.id());
   if((Fusions.recipeFor(rune).isPresent()||WovenRunes.isWoven(rune))&&fusions.size()<128)fusions.add(rune.id());
  }
  update(p,new Notebook(List.copyOf(shapes),List.copyOf(fusions),s.garden,s.rewarded));
 }
 public static void garden(ServerPlayer p) {if(p.isCreative()||p.isSpectator())return;var s=p.getAttachedOrElse(NOTES,Notebook.EMPTY);update(p,new Notebook(s.shapes,s.fusions,true,s.rewarded));}
 private static void update(ServerPlayer p,Notebook s) {
  int reward=s.rewarded;
  if(s.shapes.size()>=5&&(reward&1)==0){reward|=1;give(p,new ItemStack(WildercordItems.TORN_PAGE));p.sendSystemMessage(Component.translatable("message.wildercord.research_shapes"));}
  if(s.fusions.size()>=3&&(reward&2)==0){reward|=2;give(p,new ItemStack(WildercordItems.BLANK_RUNE,2));p.sendSystemMessage(Component.translatable("message.wildercord.research_fusions"));}
  if(s.garden&&(reward&4)==0){reward|=4;give(p,new ItemStack(WildercordItems.MANA_CRYSTAL));p.sendSystemMessage(Component.translatable("message.wildercord.research_garden"));}
  var next=new Notebook(s.shapes,s.fusions,s.garden,reward);if(!next.equals(p.getAttachedOrElse(NOTES,Notebook.EMPTY)))p.setAttached(NOTES,next);
 }
 private static void give(ServerPlayer p,ItemStack stack){p.getInventory().add(stack);if(!stack.isEmpty())p.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);}
 public static int show(ServerPlayer p) {
  var s=p.getAttachedOrElse(NOTES,Notebook.EMPTY);p.sendSystemMessage(Component.translatable("message.wildercord.research_board",Math.min(5,s.shapes.size()),Math.min(3,s.fusions.size()),s.garden?"✓":"—"));
  // Offer one recipe the player can explore with learned ingredients, rather than spoil the entire journal.
  for(var recipe:Fusions.RECIPES) {
   if(Spellbooks.knows(p,recipe.result().id()))continue;
   var first=Runes.all().stream().filter(r->r.family()==RuneFamily.EFFECT&&r.element().equals(recipe.first())&&Spellbooks.knows(p,r.id())).findFirst();
   var second=Runes.all().stream().filter(r->r.family()==RuneFamily.EFFECT&&r.element().equals(recipe.second())&&Spellbooks.knows(p,r.id())).findFirst();
   if(first.isPresent()&&second.isPresent()){p.sendSystemMessage(Component.translatable("message.wildercord.research_hint",first.get().name(),second.get().name(),Fusions.COMBINE_XP));break;}
  }
  return 1;
 }
}
