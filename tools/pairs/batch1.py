"""The first hundred pairs: chosen by hand for range across the ten elements and the three kinds of play
(harm, help, movement). Prints them in ten groups of ten, one group per agent file."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))

PAIRS = """
chill fire
fire shock
fire push
fire heal
fire venom
fire bleed
fire blink
fire stoneskin
ember windcut
flashfire frost
kindling cyclone
cinderbrand hex
searing_edge rend
meteor tremor
torchfall levitate
warm_cloak salve
fire tarry
ember leech
smite fire
fire harm
chill shock
chill push
chill heal
chill bleed
chill pull
chill stoneskin
icicle lightning
frost tremor
flash_freeze launch
coldsnap venom
undertow vinelash
bubble jolt
salve regrowth
mending_mist haven
frostward barrier
absolute_zero stasis
icicle smite
frost countdown
chill blink
coldsnap rampart
shock push
shock heal
lightning blink
shock venom
jolt bleed
thunderclap aftershock
galvanize empower
stormheart warcry
rain_cloud grow
lightning levitate
shock silence
jolt dash
lightning rewind
shock anchor
push heal
push venom
push bleed
dash leap
cyclone pull
windcut gash
repel barrier
gale_mantle stoneform
deflect shield
launch tusk_charge
evade phantom
swift accelerate
rally soothe
blink heal
pull rootsnare
anchor shackle
banish harm
blind spook
hex manaburn
hobble mire
grapple leap
umbra veil
stillbind tarry
lure taunt
wither bleed
heal leech
heal stoneskin
bramble rend
vinelash grapple
sporebloom hex
cleanse remedy
regrowth borrowed_time
slowburn venom
nourish managift
bleed clot
lifesteal leech
overdrive haste
warcry rally
taunt shieldwall
harm empower
barrier stoneskin
silence pacify
fangs stalactite
swap blink
sentry spellbrand
aftershock stalactite
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
        rest = [p for p in out if p != ("chill", "fire")]
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
