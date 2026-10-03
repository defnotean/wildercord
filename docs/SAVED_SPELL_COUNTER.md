# Saved spell counter

The Cord creation screen (K → Spells) displays **Saved spells: N / 24** beneath the page tabs. N counts named builds in the spell library. Saving another named build raises the count; replacing its name, ignoring case, keeps the count; deleting it lowers the count. The server synchronizes the bounded library only to its owner, and an open screen reads that state each frame.

The header has a dedicated line, with the spell rows and remaining layout shifted together. The existing responsive scaling keeps the count visible at smaller resolutions.

## Verification

The native ResearchLibraryTest passes library capacity, invalid and locked slot checks, atomic loading, the actual notebook save/load/delete buttons, live client synchronization, replacement without count inflation and deletion while the creation screen is open. Actual Minecraft screenshots verify the layout at 1600 × 900 and 854 × 480. Review files live in artifacts/review/saved-spell-counter.

This change does not complete the living-world expansion. Mountain attunement, Storm Spire architecture and the wetland milestone continue separately.
