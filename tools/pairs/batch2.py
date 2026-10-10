"""The second hundred pairs, every rune in them new to pairing,: chosen by hand for range across the ten elements and the three kinds of play
(harm, help, movement). Prints them in ten groups of ten, one group per agent file."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))

PAIRS = """
cometfall glacier
starfall hail
starshard frostbite
decree truce
reflect riposte
halo lifebloom
aegis brace
nullify timesteal
prismatic_burst resonance
reveal echolocate
starlight_tether gravity_well
summon shades
twin_star mirrorfrost
worst_first stitchtime
manawell manatide
bloodboil seethe
cleave dismantle
crimson_mist dragon_breath
heartstopper doomclock
hemomancy sanguine_rite
parasite infest
blood_moss sapflow
transfusion soulbond
blood_thread magnetize
blood_escrow reversal
red_ledger reckoning
basalt_surge magma
bonespur monolith
fossilize black_ice
geode shulkershell
guardlink hearthbond
ironhold second_wind
pelt sandstorm
root tidehook
sinkhole hellmouth
thunderquake stormclock
weigh downdraft
clockroot time_skip
aftercare frostbloom
barkhide leafshade
blazecall skyburst
boiling_surge thunder_tide
cinderheart surge
conflagration everburn
explode primer
hearthglow zephyr
hearthsong morale
inferno blizzard
phoenix_pyre bloom
soulfire blackflame
starfire stormweave
steam tidecall
sunscorch ripple
ashen_veil emberguard
avalanche summit_wind
cryostasis ashen_mercy
drowning_word hush
freeze plasma
hoarfrost entropy
rime_seal corral
tidewrit recoil
current porpoise
skimstep skaters_edge
staunch honeydew
frost_molt quench
drowse lullaby
moonpetal eclipse
bloomstep warp_step
fortune blackspark
restore tend
pulse_ferry picnic
potion_steep prolong
frostwire tempest
riftbolt riftcall
thunderhead updraft
thunderbird soar
thunderstep shadowstep
dynamo_stride wayfarer_hymn
chronoshift foresight
second_bell resonant_shriek
devour starmaw
hollow portalfall
malison hexguard
singularity dust_devil
sonic_boom razorgale
warp disarm
zipper beeline
infinity cushion
withdraw inkveil
nudge feather_fall
skylatch hayloft
fieldstride steedsong
trot steedmend
sea_breeze shrug_off
heel faithful
beastguard grace
stoutheart shellback
quietus nullcatch
last_lantern wayline
savor feastday
"""


def main():
    known = {}
    with open(os.path.join(HERE, "runes.tsv"), encoding="utf-8") as f:
        for line in f:
            if line.startswith("#") or not line.strip():
                continue
            r = line.rstrip("\n").split("\t")
            known[r[0]] = r
    seen, out, bad = set(), [], []
    for line in PAIRS.split("\n"):
        if not line.strip():
            continue
        a, b = sorted(line.split())
        if a not in known or b not in known or a == b:
            bad.append(line)
            continue
        if (a, b) in seen:
            bad.append("dup " + line)
            continue
        seen.add((a, b))
        out.append((a, b))
    if bad:
        print("bad:", bad)
        return 1
    print(len(out), "pairs")
    if "--groups" in sys.argv:
        rest = out
        for g in range(0, len(rest), 10):
            print(f"group {g // 10 + 1}:")
            for a, b in rest[g:g + 10]:
                ra, rb = known[a], known[b]
                print(f"  {a} + {b}")
                print(f"    {a} ({ra[1]}, {ra[4]}, {ra[5]}): {ra[7]}")
                print(f"    {b} ({rb[1]}, {rb[4]}, {rb[5]}): {rb[7]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
