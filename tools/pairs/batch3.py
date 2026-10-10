"""The third hundred pairs: runes already paired once, crossed into new elements, and the helpful and movement runes
not yet paired. Chosen by hand; a pair already written is refused. Prints ten groups of ten, one per agent file."""
import glob
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "..", "src", "main", "java", "dev", "wildercord", "pairs")

PAIRS = """
firestorm tempest
fireward frostward
sunbask regrowth
meteor gravity_well
inferno updraft
kindling venom
seethe taunt
sunscorch blind
steam mire
ember jolt
cinderbrand reckoning
everburn tarry
hellmouth devour
starfire riftbolt
phoenix_pyre rewind
searing_edge windcut
warm_cloak gale_mantle
explode countdown
flashfire launch
skyburst levitate
absolute_zero singularity
avalanche tremor
black_ice dash
blizzard cyclone
bubble levitate
coldsnap jolt
drowning_word silence
frostbite bleed
glacier monolith
hail pelt
icicle windcut
tidal_lift undertow
tidecall pull
mirrorfrost reflect
air_pocket bubble
drown_ward cushion
tidebreath mending_mist
sounding echolocate
upwell updraft
divers_hands grapple
lightning meteor
magnetize pull
plasma explode
ripple resonance
stormclock countdown
thunderclap sonic_boom
thunderhead hail
surge haste
stormheart rally
dynamo_stride swift
thunderbird phoenix_pyre
thunderstep blink
jolt stillbind
sinkhole gravity_well
stalactite icicle
shackle stillbind
mire venom
root rootsnare
stoneform shieldwall
pacify soothe
caveward deepwarn
surefoot leap
hearthguard hearthglow
keenkeep searing_edge
long_arm grapple
tinker_hum dynamo_stride
venom sporebloom
moonpetal starfall
heal halo
dew_drink salve
luckcharm fortune
lullaby hush
second_wind evade
banish portalfall
blind eclipse
dragon_breath blizzard
hex malison
hobble shackle
lure pull
spook phantom
wither blackflame
starmaw singularity
enderhush silence
homeward wayline
softfoot shadowstep
lodepull magnetize
fair_wind swift
glidewind feather_fall
softsole cushion
repel deflect
razorgale windcut
dust_devil sandstorm
doomclock countdown
stasis stillbind
timesteal leech
accelerate haste
heartsense echolocate
lifesteal hemomancy
cleave rend
manaburn silence
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
