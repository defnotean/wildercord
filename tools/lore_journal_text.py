"""The lore journal: its screen, discovery quests, connected legends and conversation lines.

Text only (no images). Canon for every line lives in docs/LORE_CANON.md. Combat instructions stay in the
existing lesson lines; these lines are background and short leads.
"""

# The ten first breaths and the six newer ones, each with a Master, plus shared lines for any other method.
# Per method: duelist greeting, hint and parting line; Master greeting, hint and parting line.
DIALOGUE = {
    "ember": (
        "Hessa kept the kiln gate with this breath. Slow in, then all at once.",
        "Breathe where water falls or wind is thin. The fire learns to wait.",
        "You stood in the heat and did not flinch. Take the breath with you.",
        "The kiln gate held eleven winters. Show me why you deserve it.",
        "A banked fire is patient. So am I, until I am not.",
        "The coals settle. You may call yourself a keeper of the gate.",
    ),
    "rime": (
        "Rime is the breath of the still lake. We move only when we must.",
        "Frost settles on the patient. Let your foe tire first.",
        "Your hands stayed steady in the cold. That is the whole art.",
        "I guarded the north ford through a winter that never ended.",
        "Hurry, and you slip. Stay still, and the ice holds you.",
        "The ford is yours to guard now. Do not let it thaw.",
    ),
    "thunder": (
        "Thunder breath lives in the moment before the storm breaks.",
        "Hold your breath at the top. Release it with the cut.",
        "Quick. Quicker than I thought. The storm likes you.",
        "I learned this on the bell tower, counting between flash and roar.",
        "Count the beat between my cuts. That is where you strike.",
        "The bell rings for you now. Count well.",
    ),
    "gale": (
        "Iven taught the road this breath. Never twice in the same place.",
        "The old scout kicked off a watchtower wall. Look for walls.",
        "You went where I was not. Good. Keep moving.",
        "The Marchkeepers' scout carved her last lesson under a tower. I read it.",
        "Wind does not fight the wall. It goes around.",
        "The scout's road is yours to walk. Leave cairns for the next one.",
    ),
    "stone": (
        "Maren kept the quarry gate. A gate does not stop the wind. It swings.",
        "The ledger of Masters is real. Grow strong, then ask a teacher twice.",
        "You did not move when I pushed. The mountain approves.",
        "I am the hinge the quarry turns on. Push, if you can.",
        "Strike my back, not my guard. Patience breaks stone.",
        "The hinge turns for you. Keep the gate as Maren did.",
    ),
    "verdant": (
        "Verdant breath takes in the light and gives it back.",
        "Gardens grew inside the Last Shelter's walls. Mend, then cut.",
        "You fought to keep standing, not to win. That is our way.",
        "I tend the garden the Last Shelter's keeper planted.",
        "Roots hold best where the ground was broken first.",
        "The garden is yours to keep. Let it outgrow you.",
    ),
    "hollow": (
        "Hollow breath empties you until the world leans in.",
        "Breathe out fully before you strike. The pull follows.",
        "Nothing left in you, and still you won. Good.",
        "I keep the empty places. They are fuller than they look.",
        "Step into the gap I leave. It is not always a trap.",
        "Carry the hollow lightly. It only takes what you let go.",
    ),
    "starlit": (
        "Starlit breath keeps time with the far lights.",
        "Field guides and star charts teach the same thing: look first.",
        "Your aura gathered faster than mine. The stars noticed.",
        "I read the sky for the Archive before the copyists left.",
        "Every star moves. Watch which way before you cut.",
        "Your light is on the chart now. Do not let it dim.",
    ),
    "hourglass": (
        "Hourglass breath lives between one grain and the next.",
        "A perfect guard stops time for a moment. Use it.",
        "You found the gap between my strokes. Few do.",
        "I have waited a long time for this duel. I can wait longer.",
        "The moment after my cut is yours. Do not waste it.",
        "Turn the glass. Your time starts now.",
    ),
    "crimson": (
        "Crimson breath keeps the beat of the blood.",
        "The Sword Tomb's keeper left openings on purpose. Find them.",
        "You bled and did not stop. The blood answers you now.",
        "I kept the tomb's oath long after the keeper slept.",
        "Every wound I give is a promise. Guard, and I owe you one.",
        "The oath passes to you. Keep it better than I did.",
    ),
    "tide": (
        "Tide breath rises and falls. Never fight the pull.",
        "Tideward surveyors read the water's edge. So should you.",
        "You gave ground and took it back. That is the tide.",
        "I walked the drowned road when the sea took it.",
        "Step back when I surge. Step in when I ebb.",
        "The tide answers you now. Let it carry you home.",
    ),
    "iron": (
        "Iron breath is the forge's breath. Heat, strike, cool, repeat.",
        "The Fallen Knights carried iron pages. Read what they left.",
        "You took the hammer and did not crack. Good metal.",
        "I forged blades for the Marchkeepers before they broke the line.",
        "Iron bends before it breaks. Test which one I am.",
        "Your edge is tempered. Do not let it rust.",
    ),
    "dune": (
        "Dune breath shifts like sand. Nothing stays where you left it.",
        "Old roads vanish under sand. The cairns still point the way.",
        "You kept your footing on loose ground. That is rare.",
        "I buried a city's name in the dunes so no one could take it.",
        "Watch my feet, not my blade. The sand tells you first.",
        "The dunes will remember your step. Few are remembered.",
    ),
    "echo": (
        "Echo breath answers. Every strike comes back to you.",
        "The Returned Step memorial teaches what an answer is. Listen there.",
        "You heard my cut before it landed. Well done.",
        "I sang in the Empty Bell until it rang back.",
        "Strike once. Wait. The second blow is mine.",
        "Your echo is in the stones now. It will answer for you.",
    ),
    "dawn": (
        "Dawn breath wakes slow and ends bright.",
        "The first light finds what the night hid. Explore early.",
        "You won as the light does: slowly, then all at once.",
        "I keep watch through the last hour of the night.",
        "Look away when I flare. Then strike what the light shows.",
        "The sun is up for you. Go and use the day.",
    ),
    "venom": (
        "Venom breath is patient. The cut is small. The end is sure.",
        "Learn which beasts carry poison. The Field Guide knows.",
        "You did not panic when it stung. That is the cure.",
        "I learned from the marsh, where every bite waits.",
        "Do not trade blows with me. Trade time, and lose less.",
        "The sting is yours now. Use it only when you must.",
    ),
    "any": (
        "Every breath has a teacher. I am one of many.",
        "Your journal remembers what you find. Read it often.",
        "Good fight. Remember what it taught you.",
        "Every school keeps a Master. You found this one.",
        "Watch my rhythm before you try to break it.",
        "You earned this. The ledger will remember.",
    ),
}

# Discovery quests and connected chapters: title, clue, objective, hint, completion.
QUESTS = {
    "scout_wall": (
        "The Scout's Wall",
        "A memorial says the Marchkeepers' scout carved a last lesson under a watchtower wall. Gale teachers still tell it.",
        "Speak with a Gale duelist.",
        "Duelists wander open country. Look for one breathing Gale, and use them to talk.",
        "The Gale duelist knew the scout's story. Ask Iven's students about Wall Turn when you reach Aura Form.",
    ),
    "kiln_gate": (
        "Hessa's Gate",
        "An Ember duelist spoke of Hessa, who held a kiln gate for eleven winters by breathing slow before every strike.",
        "Breathe in the stance at a waterfall or a mountain summit.",
        "You need a breathing method first: win a duel to learn one. Then breathe beside falling water or on a high peak.",
        "The fire learned to wait. Training grounds deepen every breath you take there.",
    ),
    "keeper_openings": (
        "The Keeper's Openings",
        "The Sword Tomb's testament: the old masters left openings on purpose. A perfect guard finds them.",
        "Land a perfect Aura guard.",
        "Guard with Aura just as a blow lands. Practise on any foe; a duelist is safest.",
        "You found the opening. After a perfect guard, your breath's own counter waits for you.",
    ),
    "warden_threshold": (
        "The Warden's Threshold",
        "Your field guide notes that some armour breaks only when two elements meet. The Cinder Warden is one.",
        "Set off any element reaction with your spells.",
        "Mark a foe with one element, then hit it with another. Frost then fire is a good start.",
        "Reactions break what plain force cannot. The Cinder Warden's lesson waits at Circle X.",
    ),
    "different_hands": (
        "Different Hands",
        "The tournament stewards keep a ledger of three schools and one rule: win a fair duel first.",
        "Win a duel against any duelist.",
        "Use a duelist to challenge them. Lose, and nothing is taken; try again.",
        "The stewards would bow to you now. Their board is open to you.",
    ),
    "masters_ledger": (
        "The Masters' Ledger",
        "Maren's student says every school keeps a Master, and teachers can call one for those who are ready.",
        "Meet a Sword Master.",
        "Reach Aura Form or Heart Circle VIII. Then sneak and use a teacher twice. The trial is dangerous.",
        "You met a Master. Their record of victories stays with you, win or lose.",
    ),
    "last_cairn": (
        "The Scout's Last Cairn",
        "The Gale teacher's story continues on the road. The Marchkeepers copied their scout's watch orders for the next traveller.",
        "Enter a road watchtower.",
        "Look on hills, meadows and open plains for a tower with a beacon. Its watch orders keep the scout's account.",
        "The watch orders remember two cairns: one for those walking, one for those carried home. The scout followed an older road.",
    ),
    "road_before_roads": (
        "The Road Before Roads",
        "The watch orders point toward the old rings. The Marchkeepers used the road-makers' stones long after the roads were lost.",
        "Enter a ring of standing stones.",
        "Search plains, meadows or taiga for tall stones in a ring. The First Roads book tells who walked there before the scouts.",
        "The rings belonged to travellers before they belonged to soldiers. The scout's last oath was to keep the way open.",
    ),
    "oath_at_rest": (
        "An Oath at Rest",
        "The old road ended in a promise. The Marchkeeper scout left Oathkeeper in stone so a blade could rest while its oath endured.",
        "Find and use a Sleeping Blade stone. You do not need to draw the blade.",
        "Seek a blade standing in stone in plains, meadows or windswept hills. Use the stone; drawing the sword is not required.",
        "The scout laid down the sword, not the duty. A guide keeps the road for the next walker, even with empty hands.",
    ),
}

QUESTS.update({
    "drowned_road": (
        "The Drowned Road",
        "The Reedkeeper remembers a road lost to a flood. Stay for the account of the last boatman.",
        "Hear the Reedkeeper's Ferryman story.",
        "Sneak-use a Tideward Reedkeeper between pauses. After the garden and crossing stories comes the last boatman.",
        "The Ferryman counted his passengers twice. Coastal boatkeepers still keep that second count in their ledgers.",
    ),
    "second_count": (
        "The Second Count",
        "The old boatkeepers wrote two numbers for every crossing: those aboard and those safely ashore.",
        "Enter a coastal pier boathouse.",
        "Follow beaches or stony shores to a long wooden pier. Its boathouse keeps The Second Count beside the boat slip.",
        "No passenger became a number missing from the shore. The boatkeepers asked the lighthouse to wait for the last boat.",
    ),
    "last_boat_light": (
        "A Light for the Last Boat",
        "The road is under water, but its promise survives. A lighthouse can still guide the next traveller home.",
        "Stand beside a working lighthouse lamp.",
        "Find a coastal lighthouse, climb its ladder and throw the lever beside the lamp. A light already tended also counts.",
        "The shore has a light again. You kept the Ferryman's promise: leave the way visible for whoever comes after you.",
    ),
    "paired_cairns": (
        "The Paired Cairns",
        "A Highland Cairn Keeper in the Windscar Highlands keeps the Marchkeepers' ridge survey notes.",
        "Hear the Cairn Keeper's story about the high meridian survey ring.",
        "Sneak-use a Highland Cairn Keeper at a Cairn Lodge until they speak of the stone survey ring.",
        "The Marchkeepers' paired cairns link the hill watchtowers and standing stone rings to the high shelves.",
    ),
    "ridge_orders": (
        "The Ridge Watch",
        "The Cairn Keeper says watchtowers and standing stone rings record how the old ridge routes were laid.",
        "Visit a road watchtower or a ring of standing stones.",
        "Follow hill and meadow roads to a stone watchtower or a ring of nine standing stones.",
        "The watch orders and road stones agree: a ridge path endures only while someone keeps its cairn lamp lit.",
    ),
    "cairn_beacon": (
        "A Lamp on the Ridge",
        "Each Highland Cairn Lodge carries a shuttered cairn lamp on its eastern stone point.",
        "Stand beside a lit cairn lamp at a Highland Cairn Lodge.",
        "Return to a Highland Cairn Lodge in the Windscar Highlands and flip the lever on its eastern cairn lamp.",
        "The ridge cairn shines across the shelf. Travellers coming down from the wind will see the way home.",
    ),
    "empty_bell_kept": (
        "What the Empty Bell Kept",
        "A Belowkeeper Listener in the Belowkeeper Hollows tends the Glowcap basins and acoustic pressure logs.",
        "Hear the Belowkeeper Listener's story about The Hollow Bell.",
        "Sneak-use a Belowkeeper Listener at a Listening Station until they speak of the Bell Sanctum.",
        "The Belowkeepers cast a bronze resonance bell in a deeper sanctum to measure how pressure waves move through stone.",
    ),
    "pressure_records": (
        "The Deep Pressure Logs",
        "The Listener says the buried Crystal Survey Labs, Deepslate Vaults, and the Bell Sanctum share the same pressure records.",
        "Visit a Crystal Survey Lab, a Deepslate Vault, or a Belowkeeper Bell Sanctum.",
        "Explore underground beneath forests, hills, or the Belowkeeper Hollows to enter a Crystal Lab, Deepslate Vault, or Bell Sanctum.",
        "The deep ledgers agree: a gallery stays safe only while someone keeps the listening station's shutter lamp lit.",
    ),
    "resonance_shutter": (
        "A Lamp in the Hollows",
        "Each Belowkeeper Listening Station carries an acoustic shutter lamp on its northeast copper pillar.",
        "Stand beside a lit shutter lamp at a Belowkeeper Listening Station.",
        "Return to a Belowkeeper Listening Station in the Belowkeeper Hollows and flip the lever on its northeast shutter lamp.",
        "The shutter lamp shines across the damp vault. Any watch coming up from the lower galleries will know the roof holds.",
    ),
    "eleven_winters": (
        "Eleven Winters at the Gate",
        "An Emberwood Kiln Keeper in the Emberwood Reach tends the charcoal kilns and Cinder Fern ash beds.",
        "Hear the Kiln Keeper's story about The Kiln Widow.",
        "Sneak-use an Emberwood Kiln Keeper at a Charcoal Yard until they speak of the Kiln Court.",
        "Ashwin's burners built a ceramic kiln court where The Kiln Widow still guards the old firing tallies.",
    ),
    "forge_records": (
        "The Southern Forge Ledgers",
        "The Kiln Keeper says Mountain Forge Halls, Prospector's Camps, Basalt Foundries, and the Kiln Court share the burner tallies.",
        "Visit a Mountain Forge Hall, Prospector's Camp, Basalt Foundry, or Emberwood Kiln Court.",
        "Explore mountains, badlands, or the Emberwood Reach to enter a Forge Hall, Prospector's Camp, or Kiln Court.",
        "The forge tallies agree: a charcoal yard stays safe from sparks only while someone keeps its draft lamp lit.",
    ),
    "kiln_draft_lamp": (
        "A Lamp by the Kilns",
        "Each Emberwood Charcoal Yard carries a shuttered draft lamp on its eastern brick stack.",
        "Stand beside a lit draft lamp at an Emberwood Charcoal Yard.",
        "Return to an Emberwood Charcoal Yard in the Emberwood Reach and flip the lever on its eastern draft lamp.",
        "The draft lamp shines across the firebreak. Any burner walking the ash road at dusk will see the kiln is banked.",
    ),
    "four_lights": (
        "Four Lamps, One Road",
        "Four regional lamps of yours now burn. Each region's keeper may have something to say.",
        "Hear the answer of the Reedkeeper, Cairn Keeper, Listener and Kiln Keeper.",
        "Sneak-use each regional keeper again now that every lamp is lit.",
        "Four keepers, four lamps, one road. Whoever walks it at night is never wholly lost.",
    ),
})

ENTRIES = {
    "place:memorial": "Found a Marchkeeper memorial.",
    "place:tomb": "Entered a Sword Tomb.",
    "place:tournament": "Saw a village tournament board.",
    "place:sleeping_blade": "Found a Sleeping Blade in stone.",
    "place:crossroads": "Stood at the Crossroads of the Ways.",
    "place:training": "Breathed at a training ground.",
    "learned:way": "Chose a Way.",
    "learned:lineage": "Joined a lineage of master and disciple.",
    "learned:perfect_guard": "Landed a perfect Aura guard.",
    "learned:technique": "Wrote a technique of your own.",
    "learned:reaction": "Set off an element reaction.",
    "learned:field_guide": "Started a field guide.",
    "learned:coast_light": "Stood beside a working coastal lighthouse lamp. The last boat still has a light to follow.",
}

FORMS = {"wall_turn": "Wall Turn", "stone_hinge": "Stone Hinge", "cinder_lunge": "Cinder Lunge", "reed_slip": "Reed Slip"}

LANG = {
    "key.category.wildercord.lore": "Wildercord: Lore",
    "key.wildercord.lore_journal": "Open lore journal",
    "screen.wildercord.lore_journal.title": "Lore Journal",
    "screen.wildercord.lore_journal.tab.quests": "Leads",
    "screen.wildercord.lore_journal.tab.places": "Places",
    "screen.wildercord.lore_journal.tab.people": "People",
    "screen.wildercord.lore_journal.tab.learned": "Learned",
    "screen.wildercord.lore_journal.tab.talk": "Talk",
    "screen.wildercord.lore_journal.open": "Open: %s",
    "screen.wildercord.lore_journal.done": "Done: %s",
    "screen.wildercord.lore_journal.hint": "Hint: %s",
    "screen.wildercord.lore_journal.hidden": "Leads not found yet: %s. Explore ruins, talk to duelists, read your field guide.",
    "screen.wildercord.lore_journal.scroll": "%s / %s",
    "screen.wildercord.lore_journal.empty.places": "No places yet. Look for memorials, tombs and tournament boards.",
    "screen.wildercord.lore_journal.empty.people": "No one met yet. Duelists wander the world.",
    "screen.wildercord.lore_journal.empty.learned": "Nothing learned yet.",
    "screen.wildercord.lore_journal.empty.talk": "No conversations yet. Use a duelist to talk.",
    "journal.wildercord.updated": "Journal updated (%s)",
    "journal.wildercord.clue": "New lead: %s",
    "journal.wildercord.objective": "Goal: %s",
    "journal.wildercord.complete": "Lead complete: %s",
    "journal.wildercord.lead": "Journal, %s: %s",
    "journal.wildercord.reward": "Reward: %s",
    "journal.wildercord.reward.xp": "%s experience",
    "journal.wildercord.reward.shards": "%s Aura Shards",
    "journal.wildercord.reward.runes": "%s Blank Runes",
    "journal.wildercord.reward.aura": "%s Aura experience",
    "journal.wildercord.entry.method": "Learned %s.",
    "journal.wildercord.entry.stage": "Reached the %s stage.",
    "journal.wildercord.entry.form": "Learned the form %s.",
    "journal.wildercord.entry.duelist": "Met a duelist of %s.",
    "journal.wildercord.entry.won": "Won a duel against %s.",
    "journal.wildercord.entry.master": "Met the Sword Master of %s.",
    "journal.wildercord.entry.victory": "Won the trial of the %s Master.",
    "dialogue.wildercord.says": "%s: \"%s\"",
    "dialogue.wildercord.speaker.duelist": "Duelist of %s",
    "dialogue.wildercord.speaker.master": "Master of %s",
}

for _entry, _text in ENTRIES.items():
    LANG["journal.wildercord.entry." + _entry.replace(":", ".")] = _text
for _form, _name in FORMS.items():
    LANG["journal.wildercord.form." + _form] = _name
for _quest, (_title, _clue, _objective, _hint, _done) in QUESTS.items():
    _base = "journal.wildercord.quest." + _quest + "."
    LANG[_base + "title"] = _title
    LANG[_base + "clue"] = _clue
    LANG[_base + "objective"] = _objective
    LANG[_base + "hint"] = _hint
    LANG[_base + "done"] = _done
for _method, _lines in DIALOGUE.items():
    for _role, _offset in (("duelist", 0), ("master", 3)):
        for _kind, _index in (("greet", 0), ("hint", 1), ("victory", 2)):
            LANG[f"dialogue.wildercord.{_role}.{_method}.{_kind}"] = _lines[_offset + _index]
