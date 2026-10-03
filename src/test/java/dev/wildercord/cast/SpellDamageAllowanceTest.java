package dev.wildercord.cast;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class SpellDamageAllowanceTest {
 @Test void everyManaBelow200KeepsOnePaymentBelow1000(){
  for(int mana=0;mana<200;mana++){var b=new SpellDamageAllowance(mana);var id=UUID.randomUUID();double total=0;
   for(int i=0;i<200;i++)total+=b.take(id,1000000);
   assertTrue(total<=12+2*mana,"mana="+mana+" total="+total);assertTrue(total<1000);
  }
 }
 @Test void aSingleHitAndWholeCastAreBounded(){var b=new SpellDamageAllowance(100000);var id=UUID.randomUUID();assertEquals(96,b.take(id,100000));double total=96;for(int i=0;i<100;i++)total+=b.take(id,100000);assertEquals(512,total);}
 @Test void separateTargetsAndPaymentsKeepOwnAllowance(){var a=UUID.randomUUID();var b=UUID.randomUUID();var budget=new SpellDamageAllowance(8);assertEquals(28,budget.take(a,100));assertEquals(0,budget.take(a,100));assertEquals(28,budget.take(b,100));assertEquals(28,new SpellDamageAllowance(8).take(a,100));}
 @Test void malformedAmountsCannotPoisonOrResetBudget(){var b=new SpellDamageAllowance(Double.NaN);var id=UUID.randomUUID();for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-1,0})assertEquals(0,b.take(id,bad));assertEquals(12,b.take(id,100));b.price(200);assertEquals(0,b.take(id,100));}
 @Test void fractionalHitsNeverOverspend(){var b=new SpellDamageAllowance(.123);var id=UUID.randomUUID();double total=0;for(int i=0;i<1000;i++)total+=b.take(id,.123456789);assertTrue(total<=SpellDamageAllowance.total(.123));}
 @Test void targetBookkeepingIsBoundedWithoutEviction(){var b=new SpellDamageAllowance(10);for(int i=0;i<256;i++)assertEquals(1,b.take(new UUID(0,i),1));assertEquals(0,b.take(new UUID(1,0),1));assertEquals(31,b.take(new UUID(0,0),100));}
}
