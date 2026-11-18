# Sorrowed (Forge 1.20.1)

It is raining the moment you join. It never stops. This was your friend's world, he is gone, and he left it open for you.

## What it does
- **Programmer Art** switches on when you join (client side, once).
- **Eternal rain** in the overworld, with two exceptions the events can cause: a sudden *silence*, and thunder.
- **Typewriter thoughts** above the hotbar: monospaced letters, blinking cursor, soft key clicks. At high Sorrow, letters glitch.
- **Papers** on first join: four wrinkled sheets in his handwriting and one pencil drawing. Use one and it glides up onto the screen; click or press Escape and it slips away. Every note in the world (the keeper's log, the names, the ticket, the apologies, the reply) is a sheet of Papers now, found in chests.
- **Taken**: a little after the opening, the picture you are looking at comes apart (the frame is sliced into bands that slide and smear, the colours split, the edges draw in, it goes dark) and when it settles you are standing in Alex's world. Every doorway works the same way from then on.
- **A grave** a few steps from the first player on first join.

## The psychological layer: Sorrow
Every player has a hidden **Sorrow** value (0-100). There is no meter and no vignette on screen; you only ever see its effects. Rain on open sky, night, the Hush, the Hallway, the Drowned Hall and nearby Hollows/Listeners raise it. Shelter, graves, Alex's world, sleeping and the Mason lower it.

| Tier | Sorrow | What changes |
|---|---|---|
| 1 Uneasy | 30+ | Thoughts turn inward; footsteps behind you; the rain can stop dead |
| 2 Haunted | 55+ | Text glitches; vignette closes in; your "friend" types in chat; fake grave markers; Reflection; Listeners; thunder |
| 3 Breaking | 80+ | Heartbeat, Darkness pulses, static; fake "joined/left the game"; a gate appears behind you; ring of Hollows; an Echo right behind you |

Events roll every 10 seconds per player with a cooldown. Test with `/sorrowed sorrow <0-100>` and `/sorrowed event <name>` (op only): footsteps, silence, whisper, thunder, ghostgrave, reflection, listener, fakejoin, doorbehind, hollowcircle, echoclose, mason.

## Entities: new names, custom bodies, one gimmick each
Registry ids are unchanged (`sorrowed:echo` etc.); only the display names changed. Nine have jointed models (`client/ModModels.java`, generated together with their textures by `tools/gen_models.py`: elbows, knees, feet, necks, hoods, coats, with shaded faces) and one animation class that bends the joints as they walk, breathes, and lets the head drift when they stand still; the Afterwalker wears your skin on a player-shaped body.

| Name | Id | Body | Gimmick |
|---|---|---|---|
| **Vigil** | echo | thin, 2.3 blocks, head bowed, arms to the knees, see-through | Keeps its distance in the rain. **When the rain stops it walks straight at you.** Never attacks; fades on arrival (+Sorrow) and **leaves one Rain Tear** where it stood. |
| **The Unsaid** | hollow | hooded, head hung to one side, no face, ribs showing through the rags, fingers that reach the ground | Spawns on open ground only in rain, rarely. **Won't attack anyone holding a lantern**, and **puts out every torch and lantern it passes**. |
| **Hark** | listener | huge round head, eyes sewn shut, a mouth that is a black slot, torn ears, long fingers | Blind. **Only moves toward noise** (sprinting, jumping, swinging, using items, **breaking blocks nearby**). Crouching keeps you quiet, unless you are hurt: **it can hear your heart**. |
| **Afterwalker** | reflection | your own skin, translucent | **Walks the exact path you walked, 10 seconds behind.** Stop and it catches up. Stare for 3s or let it near and it's gone. |
| **The Finisher** | mason | stocky, yellow hard hat, tool belt, hammer | In Alex's world. **Lays a plank bridge under his own feet.** Near him, Sorrow falls. Hit him: "I wasn't done." |
| **The Archivist** | archivist | 3 blocks of ink-dark robe, a neck far too long, no eyes, a thin wide smile, a book held up to read | In the Archive. **Only moves while you are not looking at it.** When it reaches you it presses a page into your hand (a sheet of Papers, nothing on it but scribbles; +Sorrow) and is gone. |
| **The Neighbour** | neighbour | ordinary height, long tan coat, wide hat, and two pale points under the brim where eyes would be | On the Street, under the lights. **Always looks at you; walks away as you approach, never hurrying.** Sprint at it and a door closes somewhere, and it is gone. **Stand perfectly still for eight seconds** and it comes to you instead, knocks on nothing, and leaves a folded note. |
| **The Orderly** | orderly | three blocks tall, white coat, mask, bent over an empty hospital bed it pushes | In the Ward. **It only moves while you do**; stop and the wheels stop. If it reaches you, you wake up in the bed by the ward's gate (+Sorrow) with no memory of lying down. |
| **The Scarecrow** | scarecrow | burlap sack head with an X for each eye, a crossbar for shoulders, straw at every seam | In the Orchard, up on its pole, head always turned to you. **Come within 12 blocks without a light in your hand and it climbs down.** It takes whatever you are holding and carries it 20 blocks into the rows. A torch or lantern in hand stops it. |
| **The Carrier** | carrier | a wood-veneer television for a head, wires hanging from the neck, a face forming in the static | In the Static. **Watching the screen puts static over your own view** and raises Sorrow; stare long enough and it is suddenly behind you. The third time, it switches off for good. |

Other name ideas, if you want to swap: Echo: *The Lingerer, Standing Grief*. Hollow: *Faceless Regret, Hush-Born*. Listener: *The Still One, Earless*. Reflection: *Ten Seconds Ago, The Retrace*. Mason: *Last Builder, Foreman of Nothing*. Edit `assets/sorrowed/lang/en_us.json`.

## Cutscenes
A real camera system: the camera swaps to an invisible armor stand and follows keyframes (`cutscene/CutsceneLibrary.java`). The camera is placed every frame along a smooth spline through all the keys, its yaw/pitch/roll are set directly on the renderer's camera each frame (nothing is left for Minecraft to interpolate), and a short time-based follow soaks up uneven frame times, so there is no judder at keyframes, tick boundaries or in zooms. The HUD and hand are hidden, controls locked, letterbox bars and fades drawn, and the player gets Resistance V so nothing can hurt them. Captions use fast typewriter text.
- **awakening**: first join. The camera looks you in the face, then tips up into the rain.
- **arrive_the_backup / the_hush / the_drowned / the_hallway / the_archive / the_street / the_static / the_orchard / the_ward**: first arrival in each dimension. A lazy orbit around his house in the sun; a slow circle that ends with the camera behind you; a dive into the water; a dolly zoom down the corridor; a tilt up the shelves and a drift down an aisle; a descent from above the road past the streetlight; a slow push into the noise; low through the orchard rows; a dolly down the ward under the lamps that still work.
- **grave**: first time you touch a grave marker. Circles the stone, then lowers Sorrow.
- **door_behind**: the camera turns you around to see the gate that wasn't there.
- **breakdown**: Sorrow hits 100. The camera drops and rolls, fades to black, and Sorrow resets to 55.
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
| A small white building with a red cross | (22, 66) | past the bed: **The Ward** |

Every world has its own music and ambient loop (biome `music`/`ambient_sound`: meadow for the Backup, deep dark for the Hush, underwater for the Drowned, soul sand valley for the Hallway, the End for the Archive, basalt deltas for the Street, disc 11 for the Static, frozen peaks for the Orchard, disc 13 for the Ward) plus client-side distant sounds and drifting particles (`client/AmbienceEvents.java`).

- **The Hush**: a dark forest with the music gone, a black sky, ash in the air, and only Hollows and Harks spawning. No sky, close black-green fog, standing stones around a soul fire, and a **Chapel of Quiet**.
- **The Drowned Hall**: a 100-block-deep sea with a rolling seabed, long trenches, kelp forests, seagrass, sea pickles, rock spires and the streetlights from his road standing lit on the seabed; his house on the sea floor with an air pocket, and a **lighthouse that never lit**.
- **The Hallway**: no sky, yellow haze, four kinds of room: the living room with the television showing static, a bedroom with the lamp on, a kitchen nobody has cooked in, and the wrong room: black carpet, black walls, lights off.
- **The Archive**: an endless library under a low beamed ceiling. Aisles of shelves six high, hanging lanterns, candles that never burn down, ladders that reach nothing, books knocked into the aisles, and reading rooms with the chairs pulled out. Brown fog, no sky, every page blank.
- **The Street**: the road he grew up on, at night, in the rain. Puddles on the road, identical houses on identical lots, sodium streetlights, and one window lit in seven.
- **The Static**: a flat plain of black, white and grey noise. Antenna masts with one red light each, dead pixels standing up out of the ground, ghost houses in glass, and fog that breathes and flickers.
- **The Orchard** (new): his grandfather's orchard, dead. Rows of the same bare tree every 8 by 6 in grey fog, apples gone to stone lying where they fell, a cart track between the rows, a scarecrow on a pole every eleven trees, and the rows never end.
- **The Ward** (new): the hospital. One white corridor, lamps in the ceiling half burnt out, a door every 16 blocks on each side into a room with the bed made, a curtain, a chair, a barred window, and sometimes a small red light. A gurney left in the corridor now and then. A monitor beeping somewhere that never stops.

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
