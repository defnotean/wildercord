package dev.wildercord.cast;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class SpellDamageAllowanceTest {
 @Test void everyManaBelow200KeepsOnePaymentBelowItsPrice(){
  for(int mana=0;mana<200;mana++){var b=new SpellDamageAllowance(mana);var id=UUID.randomUUID();double total=0;
   for(int i=0;i<200;i++)total+=b.take(id,1000000);
   assertTrue(total<=4+1.2*mana+1e-6,"mana="+mana+" total="+total);assertTrue(total<=256);
  }
 }
 @Test void aSingleHitAndWholeCastAreBounded(){var b=new SpellDamageAllowance(100000);var id=UUID.randomUUID();assertEquals(40,b.take(id,100000));double total=40;for(int i=0;i<100;i++)total+=b.take(id,100000);assertEquals(256,total);}
 @Test void separateTargetsAndPaymentsKeepOwnAllowance(){var a=UUID.randomUUID();var b=UUID.randomUUID();var budget=new SpellDamageAllowance(8);assertEquals(13.6,budget.take(a,100),1e-5);assertEquals(0,budget.take(a,100),1e-5);assertEquals(13.6,budget.take(b,100),1e-5);assertEquals(13.6,new SpellDamageAllowance(8).take(a,100),1e-5);}
 @Test void malformedAmountsCannotPoisonOrResetBudget(){var b=new SpellDamageAllowance(Double.NaN);var id=UUID.randomUUID();for(double bad:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,-1,0})assertEquals(0,b.take(id,bad));assertEquals(4,b.take(id,100));b.price(200);assertEquals(0,b.take(id,100));}
 @Test void fractionalHitsNeverOverspend(){var b=new SpellDamageAllowance(.123);var id=UUID.randomUUID();double total=0;for(int i=0;i<1000;i++)total+=b.take(id,.123456789);assertTrue(total<=SpellDamageAllowance.total(.123));}
 @Test void manyTargetsShareOutPastTheFirstThree(){var b=new SpellDamageAllowance(10);double[] got=new double[6];for(int i=0;i<6;i++)got[i]=b.take(new UUID(0,i),1000);for(int i=0;i<3;i++)assertEquals(16,got[i],1e-4);assertEquals(12,got[3],1e-4);assertEquals(9.6,got[4],1e-4);assertEquals(8,got[5],1e-4);}
 @Test void targetBookkeepingIsBoundedWithoutEviction(){var b=new SpellDamageAllowance(10);for(int i=0;i<256;i++)assertTrue(b.take(new UUID(0,i),0.01)>0);assertEquals(0,b.take(new UUID(1,0),1));assertEquals(16-0.01,b.take(new UUID(0,0),100),1e-4);}
}
