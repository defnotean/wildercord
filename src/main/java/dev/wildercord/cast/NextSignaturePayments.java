package dev.wildercord.cast;
import java.util.Map;
import java.util.WeakHashMap;
/** Effect resource ledgers are weakly keyed by the real shared payment, never by a pulse-local cast. */
final class NextSignaturePayments {
 private NextSignaturePayments(){}
 private static final Map<Object,NextSignatureRules.Ledger> PAID=new WeakHashMap<>();
 static NextSignatureRules.Ledger of(Cast cast){return PAID.computeIfAbsent(cast.payment(),k -> new NextSignatureRules.Ledger());}
 static void clear(){PAID.clear();}
}
