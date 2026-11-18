package com.sorrowed.story;

import com.sorrowed.registry.Dimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * One thing to do in every world, from his list. The words are his (lowercase, the way he wrote);
 * the line when you finish is yours. Finish all of them and he is waiting at the pond.
 *
 * steps: how many separate parts the objective has (the four places, the nine stones). Parts are
 * kept as a bitmask in {@link Progress}. way: how to get to that world, for the journal.
 */
public final class Objectives {
    private Objectives() {}

    public record Objective(String id, ResourceKey<Level> dim, String place, String task, String hint,
                            String done, int steps, String way) {
        public int fullMask() {
            return (1 << steps) - 1;
        }
    }

    public static final List<Objective> ALL = List.of(
            new Objective("overworld", Level.OVERWORLD, "home", "visit his grave",
                    "there's a stone in the rain near where you first came in. it hasn't got a name on it. it doesn't need one.\n\n"
                            + "you don't have to say anything. just go and stand there for a minute.",
                    "I stood there in the rain until it felt like he'd noticed.", 1,
                    "through the arch at the end of his garden path. the way home."),
            new Objective("backup", Dimensions.BACKUP, "the backup", "go back to the places you shared",
                    "the pond where we never caught anything. the dirt house from the first night. the treehouse. the campfire.\n\n"
                            + "i put them all back exactly how they were. go and stand in them. somebody should.",
                    "All four. Exactly how we left them. Even the torch.", 4,
                    "you're taken there the first time. every door leads back to it."),
            new Objective("hallway", Dimensions.HALLWAY, "the hallway", "answer him",
                    "somewhere in the hallway there's a desk with the computer still on. the chat is still open.\n\n"
                            + "i asked if you were on. you weren't. that's okay. you can answer now. i'll know.",
                    "It says delivered. It doesn't say read.", 1,
                    "the door in the back wall of his house."),
            new Objective("drowned", Dimensions.DROWNED, "the drowned hall", "light the lighthouse",
                    "there's a lighthouse on the bottom, past the house. i never lit it. if it's lit someone will see this place "
                            + "and come looking.\n\nyou're already here. light it anyway. climb all the way up.",
                    "It's lit. If anyone's still out there, they'll see it now.", 1,
                    "at the bottom of the well."),
            new Objective("hush", Dimensions.HUSH, "the hush", "touch every stone",
                    "the stones in the clearing. i counted them every night and there was always one more than i counted.\n\n"
                            + "put your hand on every one. count them for me. tell me i wasn't imagining it.",
                    "Nine. There were always nine. One for every door he left open.", 9,
                    "inside the hollow oak."),
            new Objective("archive", Dimensions.ARCHIVE, "the archive", "turn the page",
                    "down the aisle behind you there's a book left open on a stand. it's been open on the same blank page forever.\n\n"
                            + "turn the page. i never could.",
                    "There was writing on the next page. The only words in the whole archive.", 1,
                    "behind the shelves in the shed."),
            new Objective("street", Dimensions.STREET, "the street", "turn off the light in his window",
                    "my house is the one with the light on and the door open. the switch is just inside, by the door.\n\n"
                            + "i left it on so you'd know i was home. you can turn it off now. it's okay. you know.",
                    "Nobody's home. He doesn't have to leave the light on for me any more.", 1,
                    "in the back of the bus shelter."),
            new Objective("static", Dimensions.STATIC, "the static", "find the signal",
                    "out in the noise, ahead of you, there's a television under a mast. stand by it and keep still. let it load.\n\n"
                            + "there's a picture under the snow. it's been trying to get through to you.",
                    "The picture came through. It was his world, and the sun was out.", 1,
                    "behind the television in the shed under the mast."),
            new Objective("orchard", Dimensions.ORCHARD, "the orchard", "find where he sat",
                    "one tree in grandad's orchard is still alive. i sat under it on sundays with the laptop, building.\n\n"
                            + "go and look. it's still running. it'll tell you what i was making.",
                    "He finished it here. On a Sunday, under the last tree that was still alive.", 1,
                    "through the gap in the orchard fence."),
            new Objective("server", Dimensions.SERVER, "the server", "see who's online",
                    "there's a console along the cross aisle, past the racks. it'll tell you who's on.\n\n"
                            + "it's always said two. i don't know who the second one is any more.",
                    "Two players online. Me. And him.", 1,
                    "in the concrete box with the vent on the roof."),
            new Objective("ward", Dimensions.WARD, "the ward", "sit with bed four",
                    "bed four is by the window, in the room with the lamp that never worked.\n\n"
                            + "i asked you not to come looking for me in here. i'm glad you didn't listen. sit down for a bit. "
                            + "visiting hours aren't over.",
                    "I'm here. I came. I'm sorry it took so long.", 1,
                    "past the bed, in the clinic."));

    public static Objective get(String id) {
        for (Objective o : ALL) {
            if (o.id().equals(id)) return o;
        }
        return null;
    }

    /** The objective that lives in this dimension, or null. */
    public static Objective in(ResourceKey<Level> dim) {
        for (Objective o : ALL) {
            if (o.dim().equals(dim)) return o;
        }
        return null;
    }

    /** Once everything is done: the pond. */
    public static final String FINALE_PLACE = "his world";
    public static final String FINALE_TASK = "go to the pond";
    public static final String FINALE_HINT = "that's all of them. thank you.\n\ni'm at the pond. come and sit with me for a bit. "
            + "bring nothing. we never caught anything anyway.";
    public static final String ENDED_HINT = "the rain stopped.\n\nleave the door open for whoever comes next. somebody always comes next.";
}
