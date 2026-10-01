package dev.wildercord.content;
import dev.wildercord.player.Heart;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
/** Dungeon handling relics: each benefit pays for a drawback. The off-hand must carry the charm. */
public final class RelicCharmItem extends Item {
 public enum Kind{HOURGLASS,SEEDPOD,SKY_FEATHER}
 public final Kind kind;
 public RelicCharmItem(Kind k,Properties p){super(p);kind=k;}
 public void appendHoverText(ItemStack s,TooltipContext c,TooltipDisplay d,Consumer<Component> out,TooltipFlag flag){out.accept(Component.translatable(getDescriptionId()+".desc").withColor(0xB8B0C8));}
 public static Heart.Bonuses apply(Player p,Heart.Bonuses b){if(!(p.getOffhandItem().getItem() instanceof RelicCharmItem charm))return b;
  return switch(charm.kind){
   case HOURGLASS->new Heart.Bonuses(b.power()*.85,b.duration()*1.30,b.cost(),b.cooldown());
   case SEEDPOD->new Heart.Bonuses(b.power()*.85,b.duration(),b.cost()*.90,b.cooldown()*1.10);
   case SKY_FEATHER->new Heart.Bonuses(b.power(),b.duration(),b.cost()*1.15,b.cooldown()*.85);
  };
 }
}
