"""The fourth hundred pairs: the world and craft runes not yet paired, each crossed with a fighting rune so the pair
is a spell that wears the craft's theme. Chosen by hand; a pair already written is refused. Prints ten groups of ten,
one per agent file."""
import glob
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "..", "src", "main", "java", "dev", "wildercord", "pairs")

PAIRS = """
lamplighter smite
hearthcook nourish
kilnbake stoneskin
lava_sense echolocate
lavaseal fireward
smoke_signal blind
stewpot salve
thawfield chill
trail_blaze dash
calmsmoke pacify
cinder_bulwark barrier
ember_rest regrowth
quickbrew haste
barnwarmth warm_cloak
gold_parley lure
portal_sense blink
fortress_sense taunt
cinder_sieve ember
bakehouse heal
smelt searing_edge
angler_lure grapple
axolotl_kinship heal
coral_mend salve
currentkin swift
diving_bell bubble
dolphin_call dash
drift_net shackle
firebreak frostward
ice_auger icicle
icepath swift
kelpsong soothe
lava_crust meteor
oceans_favor tidebreath
reeling_tide pull
snuffout ember
soak_through jolt
spring_draw mending_mist
wring bleed
sluice undertow
rime_causeway frost
thunder_walk thunderstep
storm_glass lightning
sky_reading rain_cloud
dewfall mending_mist
leverflip swap
doorcall banish
ditchwater mire
buttonpush push
ripen regrowth
sun_reading sunbask
moon_reading umbra
hivehum venom
cloche barrier
trade_renew rewind
void_step blink
hollow_pocket devour
frame_veil phantom
grave_bearing harm
spawner_sense banish
collect pull
unburden levitate
portal_reckoning riftbolt
stronghold_compass singularity
arrowveil deflect
scarecrow spook
whistle warcry
wind_steps leap
leaffall cushion
pollinate sporebloom
thresherwind windcut
gentlehand heal
prune cleave
herdcall rally
fleece shield
glowvine vinelash
harvest lifesteal
root_bulwark rampart
root_carry grapple
petward haven
ancient_seed phoenix_pyre
light smite
manabraid managift
orbcall spellbrand
sanctuary haven
rally_light rally
lodestar magnetize
nightwatch sentry
night_eye echolocate
silklift levitate
starchart starfire
stillwell cleanse
break tremor
chisel rend
citadel shieldwall
excavate sinkhole
motherlode magnetize
luckstrike lightning
pitfloor gravity_well
riser launch
agestone stasis
"""


def written():
    have = set()
    for f in glob.glob(os.path.join(SRC, "b*", "*.java")):
        with open(f, encoding="utf-8") as src:
            have.update(re.findall(r'@Pair\(a = "(\w+)", b = "(\w+)"', src.read()))
    return have


def main():
    known = {}
    with open(os.path.join(HERE, "runes.tsv"), encoding="utf-8") as f:
        for line in f:
            if line.startswith("#") or not line.strip():
                continue
            r = line.rstrip("\n").split("\t")
            known[r[0]] = r
    have = written() if "--groups" not in sys.argv or "--fresh" in sys.argv else set()
    seen, out, bad = set(), [], []
    for line in PAIRS.split("\n"):
        if not line.strip():
            continue
        a, b = sorted(line.split())
        if a not in known or b not in known or a == b:
            bad.append(line)
            continue
        if (a, b) in seen or (a, b) in have:
            bad.append("dup " + line)
            continue
        seen.add((a, b))
        out.append((a, b))
    if bad:
        print("bad:", bad)
        return 1
    print(len(out), "pairs")
    if "--groups" in sys.argv:
        for g in range(0, len(out), 10):
            print(f"group {g // 10 + 1}:")
            for a, b in out[g:g + 10]:
                ra, rb = known[a], known[b]
                print(f"  {a} + {b}")
                print(f"    {a} ({ra[1]}, {ra[4]}, {ra[5]}): {ra[7]}")
                print(f"    {b} ({rb[1]}, {rb[4]}, {rb[5]}): {rb[7]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
