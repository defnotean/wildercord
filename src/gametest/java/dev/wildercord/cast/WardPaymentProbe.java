package dev.wildercord.cast;
/** Test-only capture of the actual paid owner passed into the production ward. */
public final class WardPaymentProbe {
 public static volatile Cast admitted;
 public static volatile boolean armed;
 public static volatile float manaAtAdmission;
 public static volatile float manaBeforePayment,manaAfterPayment;
 public static volatile int beforeCount,afterCount,actualCost,actualSpent;
 private static java.util.UUID actor;
 private static boolean installed;
 public static void install(){if(installed)return;installed=true;
  dev.wildercord.api.WildercordEvents.BEFORE_CAST.register((p,slot,runes,cost)->{if(armed && p.getUUID().equals(actor) && slot==0){beforeCount++;actualCost=cost;manaBeforePayment=dev.wildercord.player.Spellbooks.mana(p);}return true;});
  dev.wildercord.api.WildercordEvents.AFTER_CAST.register((p,slot,runes,spent)->{if(armed && p.getUUID().equals(actor) && slot==0){afterCount++;actualSpent=spent;manaAfterPayment=dev.wildercord.player.Spellbooks.mana(p);}});
 }
 public static void arm(net.minecraft.server.level.ServerPlayer p){clear();actor=p.getUUID();armed=true;}

 public static final java.util.Set<Object> payments=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
 public static void accept(Cast cast){if(armed){payments.add(cast.payment());if(admitted==null){manaAtAdmission=dev.wildercord.player.Spellbooks.mana((net.minecraft.server.level.ServerPlayer)cast.caster);admitted=cast;}}}
 public static void clear(){armed=false;admitted=null;payments.clear();manaAtAdmission=0;manaBeforePayment=0;manaAfterPayment=0;beforeCount=0;afterCount=0;actualCost=0;actualSpent=0;actor=null;}
}
