# Sorrowed (Forge 1.20.1)

It is raining the moment you join. It never stops. This was your friend's world, he is gone, and he left it open for you.

## What it does
- **Programmer Art** switches on when you join (client side, once).
- **Eternal rain** in the overworld, with two exceptions the events can cause: a sudden *silence*, and thunder.
- **Typewriter thoughts** above the hotbar: monospaced letters, blinking cursor, soft key clicks. At high Sorrow, letters glitch.
- **Papers** on first join: four wrinkled sheets in his handwriting and one pencil drawing. Use one and it glides up onto the screen; click or press Escape and it slips away. Every note in the world (the keeper's log, the names, the ticket, the apologies, the reply) is a sheet of Papers now, found in chests.
- **Taken**: a little after the opening, the picture you are looking at comes apart (the frame is sliced into bands that slide and smear, the colours split, the edges draw in, it goes dark) and when it settles you are standing in Alex's world. Every doorway works the same way from then on.
- **A grave** a few steps from the first player on first join.

## The story: one thing in every world, then him
Alex left a list. There is **one objective in every dimension**, eleven in all, each built near where you arrive (`story/Objectives.java`, `worldgen/MemorySites.java`) and tracked per player (it survives death):

| World | Objective | What happens |
|---|---|---|
| Home (overworld) | visit his grave | stand at the stone in the rain; a candle is left lit |
| The Backup | go back to the places you shared | stand at the pond, in the dirt house, up the treehouse, by the campfire |
| The Hallway | answer him | a desk with the chat still open: type a reply to his last messages |
| The Drowned Hall | light the lighthouse | climb to the dark lamp on the seabed lighthouse; it lights and two beams turn through the water |
| The Hush | touch every stone | count the eight standing stones (each lights up); then there is a ninth |
| The Archive | turn the page | the book left open on the blank page has writing on the next one |
| The Street | turn off the light in his window | his house has the light on and the door open; the switch is inside |
| The Static | find the signal | keep still by the television under the mast until the picture loads |
| The Orchard | find where he sat | the one living tree, and his laptop finishing the copy of his world |
| The Server | see who's online | the console types `list`: two players online, you and him |
| The Ward | sit with bed four | room four, the bed by the window; the lamp that never worked comes on |

When all eleven are done, the Backup turns to "the day after" (gold light, petals drifting) and **he is sitting at the end of the dock at the pond**. Walk up to him and he starts to say goodbye.

**Then the twist.** He won't let you finish it: mid-cutscene he comes apart into **The Memory Shatterer** (`entity/MemoryShattererEntity.java`), his own shape at twice his size, dark, cracked through with light, ghosts of him slipping off in red and cyan, pieces of him circling. A **sword falls out of the sky** into your hand (the *Borrowed Sword*: it's only lent, never drops, and every hit with it heals you a little). His world goes dark, and an original synthesized amen-break drum & bass track plays for the whole fight (`tools/gen_music.py`).

| Stage | Name | What it adds |
|---|---|---|
| I | remembering | it floats at you and swings after a wind-up; it blinks behind you if you keep away |
| II | forgetting | fans of glass shards thrown at you |
| III | shattering | marked spots on the ground, then shards falling out of the dark onto them; it rises and slams the ground (jump to dodge); two faceless Unsaid join in |
| IV | the last memory | faster, and a ring of shards with a pulse of darkness every few seconds |

It has its own health bar (name, four segments, stage label). No single hit can carry it past a stage, it can't be hurt while it changes, and only the sword hurts it properly (anything else does about a third). Die, or leave his world, and he's back on the dock, waiting for you to try again (a shorter scene the second time).

**Beat it** and the dark lifts, the sword goes back up, and **he comes down out of the sky with a halo**, himself again, to say goodbye properly. Then the screen goes white, and his letter writes itself out over the credits music. You wake by his grave at home, and **the rain stops for good** (world-wide). The title screen changes too: morning, nobody in the field.

**New UI.** Press **J** (or *Journal* on the pause menu) for the journal: his list on one page, crossed out in pencil as you go, and the chosen world's note, the way there and your progress on the other. A small note in the top-left shows the current world's objective with a pencil arrow and distance (`objectiveHud = false` hides it). Faint light rises from whatever you are looking for. Finishing something writes it large across the screen and crosses it out. The hallway, server and orchard screens open as an old monitor (scanlines, cursor, typing).

**Visuals.** Every world has its own colour at the edges of the view and a little film grain; the vignette closes in as Sorrow rises and beats with your heart past 80 (`screenEffects`). Dust, ash, marine snow, rain, noise and sparks drift around you per world (`ambientParticles`).

Testing (op): `/sorrowed story status`, `/sorrowed story complete <id|all>`, `/sorrowed story reset`.

## The title screen
The main menu is his house across a field at night, in the rain, one window lit and flickering (`client/SorrowedTitleScreen.java`). The painting pans slowly, rain falls across it, lightning whites it out now and then with thunder behind. Somebody is standing in the field. **Every time you come back to the menu they are standing a little closer.** The title is written crooked, letters slipping now and then, and one of his lines types itself out at the bottom. The buttons are just words: singleplayer, multiplayer, mods, options, quit. Once you have seen the story end, the menu is the same view the morning after: no rain, the window dark, nobody in the field. Turn it off with `titleScreen = false`.

## The psychological layer: Sorrow
Every player has a hidden **Sorrow** value (0-100). There is no meter on screen; you see it in its effects, and in how far the dark comes in at the edges of the view. Rain on open sky, night, the Hush, the Hallway, the Drowned Hall and nearby Hollows/Listeners raise it. Shelter, graves, Alex's world, sleeping and the Mason lower it.

| Tier | Sorrow | What changes |
|---|---|---|
| 1 Uneasy | 30+ | Thoughts turn inward; footsteps behind you; the rain can stop dead |
| 2 Haunted | 55+ | Text glitches; vignette closes in; your "friend" types in chat; fake grave markers; Reflection; Listeners; thunder |
| 3 Breaking | 80+ | Heartbeat, Darkness pulses, static; fake "joined/left the game"; a gate appears behind you; ring of Hollows; an Echo right behind you |

Events roll every 10 seconds per player with a cooldown. Test with `/sorrowed sorrow <0-100>` and `/sorrowed event <name>` (op only): footsteps, silence, whisper, thunder, ghostgrave, reflection, listener, fakejoin, doorbehind, hollowcircle, echoclose, mason.

## Entities: new names, custom bodies, one gimmick each
Registry ids are unchanged (`sorrowed:echo` etc.); only the display names changed. Ten have jointed models (`client/ModModels.java`, generated together with their textures by `tools/gen_models.py`: elbows, knees, feet, necks, hoods, coats, with shaded faces) and one animation class that bends the joints as they walk, breathes, and lets the head drift when they stand still; the Afterwalker wears your skin on a player-shaped body.

**They are meaner now.** Every one of them hurts you when it reaches you, not just your Sorrow: Vigil walks at you faster and goes through you (damage, darkness); the Unsaid is quicker, hits harder, darkens your sight and spawns twice as often; Hark hits harder, hears you for longer and reaches further; the Afterwalker follows only 7 seconds behind and hurts when it catches up; the Archivist, Neighbour, Carrier, Orderly, Scarecrow and the Copy all strike when they reach you, and most of them spawn more often. In every world the horrors start after 20 seconds instead of 30, come faster, and the world wants you gone after 5 minutes instead of 7.

| Name | Id | Body | Gimmick |
|---|---|---|---|
| **Vigil** | echo | thin, 2.3 blocks, head bowed, arms to the knees, see-through | Keeps its distance in the rain. **When the rain stops it walks straight at you.** When it arrives it goes through you (damage, Darkness), fades (+Sorrow) and **leaves one Rain Tear** where it stood. |
| **The Unsaid** | hollow | hooded, head hung to one side, no face, ribs showing through the rags, fingers that reach the ground | Spawns on open ground only in rain, rarely. **Won't attack anyone holding a lantern**, and **puts out every torch and lantern it passes**. |
| **Hark** | listener | huge round head, eyes sewn shut, a mouth that is a black slot, torn ears, long fingers | Blind. **Only moves toward noise** (sprinting, jumping, swinging, using items, **breaking blocks nearby**). Crouching keeps you quiet, unless you are hurt: **it can hear your heart**. |
| **Afterwalker** | reflection | your own skin, translucent | **Walks the exact path you walked, 10 seconds behind.** Stop and it catches up. Stare for 3s or let it near and it's gone. |
| **The Finisher** | mason | stocky, yellow hard hat, tool belt, hammer | In Alex's world. **Lays a plank bridge under his own feet.** Near him, Sorrow falls. Hit him: "I wasn't done." |
| **The Archivist** | archivist | 3 blocks of ink-dark robe, a neck far too long, no eyes, a thin wide smile, a book held up to read | In the Archive. **Only moves while you are not looking at it.** When it reaches you it presses a page into your hand (a sheet of Papers, nothing on it but scribbles; +Sorrow) and is gone. |
| **The Neighbour** | neighbour | ordinary height, long tan coat, wide hat, and two pale points under the brim where eyes would be | On the Street, under the lights. **Always looks at you; walks away as you approach, never hurrying.** Sprint at it and a door closes somewhere, and it is gone. **Stand perfectly still for eight seconds** and it comes to you instead, knocks on nothing, and leaves a folded note. |
| **The Orderly** | orderly | three blocks tall, white coat, mask, bent over an empty hospital bed it pushes | In the Ward. **It only moves while you do**; stop and the wheels stop. If it reaches you, you wake up in the bed by the ward's gate (+Sorrow) with no memory of lying down. |
| **The Scarecrow** | scarecrow | burlap sack head with an X for each eye, a crossbar for shoulders, straw at every seam | In the Orchard, up on its pole, head always turned to you. **Come within 12 blocks without a light in your hand and it climbs down.** It takes whatever you are holding and carries it 20 blocks into the rows. A torch or lantern in hand stops it. |
| **The Copy** | copy | a player-shaped figure the grey of an unloaded skin, a black smear for a face, a cyan glitch line across the head | In the Server. **When it sees you it becomes your reflection** in a mirror halfway between you: step toward it and it steps toward you, turn left and it turns right. Meet it in the middle and one of you is deleted (darkness, nausea, +Sorrow). |
| **The Carrier** | carrier | a wood-veneer television for a head, wires hanging from the neck, a face forming in the static | In the Static. **Watching the screen puts static over your own view** and raises Sorrow; stare long enough and it is suddenly behind you. The third time, it switches off for good. |

Other name ideas, if you want to swap: Echo: *The Lingerer, Standing Grief*. Hollow: *Faceless Regret, Hush-Born*. Listener: *The Still One, Earless*. Reflection: *Ten Seconds Ago, The Retrace*. Mason: *Last Builder, Foreman of Nothing*. Edit `assets/sorrowed/lang/en_us.json`.

## Cutscenes
A real camera system: the camera swaps to an invisible armor stand and follows keyframes (`cutscene/CutsceneLibrary.java`). The camera is placed every frame along a smooth spline through all the keys, its yaw/pitch/roll are set directly on the renderer's camera each frame (nothing is left for Minecraft to interpolate), and a short time-based follow soaks up uneven frame times, so there is no judder at keyframes, tick boundaries or in zooms. The HUD and hand are hidden, controls locked, letterbox bars and fades drawn, and the player gets Resistance V so nothing can hurt them. Captions use fast typewriter text.
- **awakening**: first join. The camera looks you in the face, then tips up into the rain.
- **arrive_the_backup / the_hush / the_drowned / the_hallway / the_archive / the_street / the_static / the_orchard / the_ward**: first arrival in each dimension. A lazy orbit around his house in the sun; a slow circle that ends with the camera behind you; a dive into the water; a dolly zoom down the corridor; a tilt up the shelves and a drift down an aisle; a descent from above the road past the streetlight; a slow push into the noise; low through the orchard rows; a dolly down the ward under the lamps that still work.
- **grave**: first time you touch a grave marker. Circles the stone, then lowers Sorrow.
- **door_behind**: the camera turns you around to see the gate that wasn't there.
- **breakdown**: Sorrow hits 100. The camera drops and rolls, fades to black, and Sorrow resets to 55.
- **twist / rematch**: at the pond. He talks, then comes apart into the Memory Shatterer while the camera backs away and looks up at the sword falling. The rematch is the short version.
- **farewell**: after the fight. He comes down out of the sky with a halo, says goodbye, and the camera rises over the pond into the white.
Turn off with `cutscenes = false`.

## Typewriter text is now rare
Idle thoughts: at most one every `ambientThoughtMinutes` (default 8), at a low chance per check. Dimension-arrival thoughts happen once (as the cutscene). Returning to the world speaks one time in three. The overlay drops extra thoughts if two are already waiting. Thoughts caused by events, entities and graves still fire.

## Dimensions
All of them except the Hush come from one chunk generator (`worldgen/LiminalGenerator.java`, `"mode"` in the dimension JSON). Every block is a pure function of its coordinates, so worlds are identical across seeds and nothing depends on neighbouring chunks.

**The Backup is the hub: Alex's world, the day before.** A perfect meadow with the same tree in every chunk, flowers on a grid, lamp posts nobody lit, and the places he built (`worldgen/Sites.java`), each holding one doorway. You arrive on the path in front of his house. Nothing in it moves.

| Place | Where | Doorway inside |
|---|---|---|
| His house: oak and spruce, the bed still made, the monitor on, a chest of papers | (0, 0) | the back wall of his room: **The Hallway** |
| The stone arch at the end of the garden path | (0, -14) | **The Way Home** (back to the overworld) |
| The well: ladder down to a flooded chamber | (36, -28) | at the bottom: **The Drowned Hall** |
| The hollow oak, bigger than any oak should be | (-44, 24) | inside the trunk: **The Hush** |
| The shed where he kept everything he wrote down | (34, 36) | behind the shelves: **The Archive** |
| The bus stop at the end of the lane | (-12, -60) | the back of the shelter: **The Street** |
| The mast on the hill and the shed with the television | (62, 4) | behind the set: **The Static** |
| The orchard fence, two dead trees, a stone apple | (-58, -18) | through the gap: **The Orchard** |
| A concrete box half sunk in the grass, a vent humming on the roof | (-36, 60) | inside: **The Server** |
| A small white building with a red cross | (22, 66) | past the bed: **The Ward** |

**Things to find.** Every world has places worth coming across, rare enough that finding one feels like a find (`worldgen/Landmarks.java`), and **pages lie on the floors** everywhere between them: right-click a few loose sheets to pick them up as Papers. Each world has its own pages (`story/Findings.java`) in Alex's voice and others': his grandad, the nurses, a neighbour, the server's own logs. The same spot always gives the same page; walking further finds more.

| World | Places to find |
|---|---|
| Alex's world | the pond with its dock, the dirt house from the first night (rebuilt, no door), the treehouse you can see every door from, the campfire that will not go out; pages in his house, the shed and the bus shelter |
| The Hush | pages on the forest floor |
| The Drowned Hall | rafts with a lantern, drifting nowhere; the school bus on the seabed; a graveyard in rows under the sea |
| The Hallway | an office with every screen on; a child's room nobody remembers, crayon on the walls; stairs that climb into the ceiling and stop |
| The Archive | the catalogue (a drawer for every day); the map room; where the stacks came down |
| The Street | the corner shop, lit, nobody at the till; the school (the clock says ten to three); the park with bread floating on the pond |
| The Static | the hole where the picture did not load; a wall of screens; a hut at the foot of a mast with a radio and a chair |
| The Orchard | grandad's farmhouse; the pumpkin patch where every face is the same; the dry well |
| The Ward | the waiting room with the television on; the chapel; the cold room |
| The Server | the operations room and its wall of screens; the tape library; the core in its cage |

**Ways out.** Every world except Alex's has an exit in each 96 by 96 region (`worldgen/Exits.java`), so wherever you wander there is one within a minute's walk: that world's own doorway in a frame of its own materials with a light on top (a red-signed frame in the Hallway, a pillar out of the sea in the Drowned, a red phone box on the Street, a black frame on a white square in the Static, a gate with no fence in the Orchard, the fire door under a green light in the Ward, a steel frame in the Server). Stepping through takes you back to where you came in. **The nearest exit hums**: every few seconds it makes a low sound you can follow. In the Hush, the stone gates in the forest are the ways out.

**The longer you stay, the worse it gets** (`mind/DimensionHorrors.java`). After half a minute's grace, something happens now and then, and more often each minute: the Hallway's lights die around you in a wave and stay dead, a door slams where there are no doors, footsteps on the carpet behind you; something vast looks at you through the water in the Drowned; candles go out one after another in the Archive and a shelf says your name; the one lit window on the Street goes dark while you watch; the static gets into your eyes and something steps out of it; the scarecrow you passed is suddenly behind you; the Ward's lamps go out down the corridor toward you and the monitor flatlines; the Server's fans all stop at once, and he types in chat. After seven minutes in one place a heartbeat that is not yours starts, and the dark closes in every half minute until you leave.

Every world has its own music and ambient loop (biome `music`/`ambient_sound`: meadow for the Backup, deep dark for the Hush, underwater for the Drowned, soul sand valley for the Hallway, the End for the Archive, basalt deltas for the Street, disc 11 for the Static, frozen peaks for the Orchard, disc 13 for the Ward) plus client-side distant sounds and drifting particles (`client/AmbienceEvents.java`).

- **The Hush**: a dark forest with the music gone, a black sky, ash in the air, and only Hollows and Harks spawning. No sky, close black-green fog, standing stones around a soul fire, and a **Chapel of Quiet**.
- **The Drowned Hall**: a 100-block-deep sea with a rolling seabed, long trenches, **the ribs of something enormous** arched over the seabed, **sunken rooms** with the roof gone and the door still shut, kelp forests, seagrass, sea pickles, rock spires and the streetlights from his road standing lit on the seabed; his house on the sea floor with an air pocket, and a **lighthouse that never lit**.
- **The Hallway**: no sky, yellow haze, ceiling panels gone dead in patches so the dark comes in pools, and five kinds of room: the **pool room** (white tile, a foot of still water, the lights reflected in it), the living room with the television showing static, a bedroom with the lamp on, a kitchen nobody has cooked in, and the wrong room: black carpet, black walls, lights off.
- **The Archive**: an endless library under a low beamed ceiling. **Burnt aisles** (half the shelves char, black runners, soul lanterns) and **the pit**, a square hole in the floor with a ladder going down a long way into the dark. Aisles of shelves six high, hanging lanterns, candles that never burn down, ladders that reach nothing, books knocked into the aisles, and reading rooms with the chairs pulled out. Brown fog, no sky, every page blank.
- **The Street**: the road he grew up on, at night, in the rain. **A car left in the road** with its lights on and nobody in it, **a playground** where a house should be, the one lit house has **its door open**, puddles on the road, identical houses on identical lots, sodium streetlights, and one window lit in seven.
- **The Static**: a flat plain of black, white and grey noise. Antenna masts with one red light each, dead pixels standing up out of the ground, ghost houses in glass, **the test card** (bars of colour standing in the noise), **a living room with no house around it** (carpet, sofa, the set on, facing nobody), and fog that breathes and flickers.
- **The Orchard** (new): his grandfather's orchard, dead. Rows of the same bare tree every 8 by 6 in grey fog, apples gone to stone lying where they fell, a cart track between the rows, a scarecrow on a pole every eleven trees, **the barn** (a long dark ruin with boards missing and something standing in the hay), and the rows never end.
- **The Ward** (new): the hospital. One white corridor, lamps in the ceiling half burnt out, a door every 16 blocks on each side into a room with the bed made, a curtain, a chair, a barred window, and sometimes a small red light. A gurney left in the corridor now and then, **a nurses' station** with nobody behind it, rooms **barred from the corridor side**, rooms with **the light off**. A monitor beeping somewhere that never stops.
- **The Server** (new): where his world was kept. Rows of racks under a low ceiling with lights blinking green and red, perforated floor tiles where the cold air comes up, cable trays, cables along the floor, a terminal at the end of a row still on, and the noise of the fans. A printed log and a readme in the chest by the gate. Home of **The Copy**.

## Structures
Nothing generates in the overworld any more except the grave a few steps from where you first stood, and the gate that appears behind you at Sorrow 80+ (it leads to Alex's world). Everything else stands in the Hush: **Stone Gate**, **Half-Built House**, **Grave**, **Lighthouse** (dark lamp, a keeper's log in a chest at the top), **Vigil Ring** (18 lit candles around a chest with the names; the last name is yours), **Bus Stop** (a ticket in the lost-property chest), **Standing Stones**, **Chapel of Quiet** (the apologies, in a chest on the altar).

## Config (`config/sorrowed-common.toml`)
`friendName` (default "Alex"), `eternalRain`, `programmerArt`, `giveBook`, `echoes`, `psychologicalEvents`, `cutscenes`, `ambientThoughtMinutes`.

## Building
Push to GitHub, **upload the extracted files** (not the zip), and the workflow in `.github/workflows/build.yml` builds the jar. Download it from the run's **Artifacts** (`sorrowed-jar`).
Locally: JDK 17 and `gradle build` (Gradle 8.1.1). The jar lands in `build/libs/sorrowed-1.0.0.jar`.

## If the jar errors out
| Symptom | Cause / fix |
|---|---|
| `No settings.gradle found` | The repo holds the zip or a nested folder. The workflow now auto-extracts a zip and finds a nested root; still best to upload the extracted contents. `.github` is a hidden folder, make sure it uploaded. |
| `Unsupported class file major version` / toolchain errors | Not building on JDK 17. |
| `Could not find net.minecraftforge:forge:1.20.1-...` | Bump `forge_version` in `gradle.properties` to a current 47.x. |
| Game crashes with `NoSuchMethodError` / `NoSuchFieldError` | Jar not reobfuscated. `build.gradle` must have `finalizedBy 'reobfJar'` on the jar task. |
| `Mod file ... has missing/invalid metadata` | Unexpanded `${...}` in `mods.toml`, or `mod_id` in `gradle.properties` does not match `@Mod("sorrowed")`. The workflow checks both. |
| Missing textures (purple/black) or `Missing model` in the log | Typo in a model/blockstate path. The workflow validates every reference. |
| World fails to load with a registry/dimension error | Bad JSON in `data/sorrowed/`. The workflow parses all JSON before building. |
| `Duplicate registry key` / `Failed to load registry` on a world made before the mod | Back up the world; a datapack-style dimension added later loads fine, but a broken JSON blocks the whole world. |

Textures are generated placeholders (`python3 tools/gen_textures.py`); replace the PNGs freely.

## More ideas (not built yet)
- **Sorrow**: a lantern item that lowers it, a journal that records what you've seen, a dimension that only opens at tier 3.
- **Entities**: *The Typist* (hunched at a desk in the Hush, types your thoughts back), *Umbrella Man* (stands in the rain holding an umbrella, shelters you), *Reflection* (appears in puddles), *The Kept* (mobs that only stare).
- **Dimensions**: *The Drowned Hall* (a flooded copy of his house), *The Static* (the world glitching between versions), *The Backup* (the world before he left: sunny, perfect, frozen).
- **Portals**: puddle gates that open on stormy nights, sleeping in his bed, typing a sign at the right place.
- **Structures**: flooded library, a lighthouse that never lights, memorial wall of names, a server-room ruin, an unfinished tower.
