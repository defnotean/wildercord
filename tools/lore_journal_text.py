"""The lore journal: its screen, entries, the six discovery quests and the teachers' conversation lines.

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

# The six discovery quests: title, clue, objective, hint, completion.
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
}

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
