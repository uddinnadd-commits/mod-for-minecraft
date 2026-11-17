package com.sorrowed.story;

import com.sorrowed.SorrowedConfig;
import net.minecraft.util.RandomSource;

/** Everything the player thinks. {friend} is replaced by the configured friend name. */
public final class Thoughts {
    private Thoughts() {}

    public static String f(String s) {
        return s.replace("{friend}", SorrowedConfig.friend());
    }

    public static String pick(String[] lines, RandomSource random) {
        return f(lines[random.nextInt(lines.length)]);
    }

    public static final String[] INTRO = {
            "...it's raining.",
            "That's wrong. It was clear a second ago.",
            "This was {friend}'s world. I'd know it anywhere.",
            "He's gone. And he left it open for me.",
            "There's a book in my pack. I should read it."
    };

    public static final String[] RETURNING = {
            "Still raining.",
            "It's still raining. Of course it is.",
            "Back in {friend}'s world. Back in the rain."
    };

    // ---- calm ambient, by place -------------------------------------------------
    public static final String[] AMBIENT_OVERWORLD = {
            "The rain never changes pitch.",
            "I keep expecting {friend} to jump out from behind a tree.",
            "Everything looks older than I remember.",
            "It's raining again. It's always raining again.",
            "I should finish something for once.",
            "Somewhere in here is a house he never finished.",
            "Even the puddles look like they're waiting."
    };
    public static final String[] AMBIENT_NIGHT = {
            "Even the dark feels like it's waiting for him.",
            "I shouldn't be out here. He wouldn't have minded.",
            "The rain is louder at night. Or I'm quieter."
    };
    public static final String[] AMBIENT_UNFINISHED = {
            "The planks go out into nothing. He was going somewhere with every one of them.",
            "There's a lantern out there on the walkway. Then another. Then the dark.",
            "It's so bright. It's so empty.",
            "No rain here. I thought that would feel better.",
            "He never filled this in. Of course he didn't.",
            "All of this is held up by nothing.",
            "Every piece of it is the width of a chunk. Like he measured it, and then stopped."
    };
    public static final String[] AMBIENT_HUSH = {
            "Something is repeating my footsteps, a half-second late.",
            "Don't look back.",
            "The rain sounds careful here.",
            "I don't think {friend} made this place."
    };
    public static final String[] AMBIENT_DROWNED = {
            "The water is warm. That's the worst part.",
            "He built a house at the bottom. Why would he build a house at the bottom?",
            "I can hear the rain above me. Even down here."
    };
    public static final String[] AMBIENT_HALLWAY = {
            "I know this carpet. I know this smell.",
            "Why is it so long? Why is it all the same?",
            "Every door I haven't opened is a room I've already been in.",
            "The lights hum the way his old monitor did."
    };
    public static final String[] AMBIENT_BACKUP = {
            "It's perfect. It's exactly how it was.",
            "Nothing is moving. Not even the grass.",
            "It's a copy. Copies don't miss anyone.",
            "If this is the backup, what happened to the original?"
    };

    public static final String[] AMBIENT_ARCHIVE = {
            "There are more shelves every time I turn around.",
            "The pages are blank. So why do I feel like I've read them?",
            "A candle is burning on that table. It hasn't gotten any shorter.",
            "Somewhere in here is what he never told me. I'll never find the shelf."
    };
    public static final String[] AMBIENT_STREET = {
            "This is his street. I only ever saw it through a webcam.",
            "The rain sounds different here. Softer. Like it's inside.",
            "The lights are on in that house. Nobody is home.",
            "I keep walking and the same house keeps going past."
    };
    public static final String[] AMBIENT_STATIC = {
            "Snow. That's what he used to call it. The snow on the screen.",
            "If I stand still, I can hear the hiss.",
            "There's a red light on the mast. There's nothing it's warning.",
            "The ground is moving. No. The ground is noise."
    };

    // ---- the mind coming apart, by Sorrow tier ---------------------------------
    public static final String[] TIER_1 = {
            "I keep checking behind me.",
            "Was that my own footsteps?",
            "I haven't heard a bird since I got here.",
            "The rain is counting something.",
            "{friend} would've laughed at how scared I am."
    };
    public static final String[] TIER_2 = {
            "That wasn't the wind.",
            "He said I could stay as long as I wanted. He never said I could leave.",
            "I'm starting to answer things nobody asked.",
            "Every grave stone looks like it's facing me.",
            "Who's been lighting the lanterns?",
            "I think I've been here before. I think I never left."
    };
    public static final String[] TIER_3 = {
            "{friend}?",
            "stop looking at me",
            "I can't tell if I'm remembering him or being remembered.",
            "You were supposed to finish it. You didn't.",
            "If I stop, it catches up.",
            "It's not raining on the rain.",
            "I am the unfinished thing."
    };

    /** What the "friend" types in chat when Sorrow is high. Lowercase, no punctuation, like he did. */
    public static final String[] FRIEND_CHAT = {
            "are you still there",
            "dont turn around",
            "i left the door open for you",
            "its so quiet without you",
            "why did you take so long",
            "you are doing it wrong again",
            "i can see you"
    };

    // ---- places ------------------------------------------------------------------
    public static final String ENTER_UNFINISHED_1 = "No rain.";
    public static final String ENTER_UNFINISHED_2 = "Just... nothing. All of it hanging in the light.";
    public static final String ENTER_HUSH_1 = "Why is it so quiet? The rain should be louder.";
    public static final String ENTER_HUSH_2 = "I don't think this was him. Something else made this.";
    public static final String ENTER_DROWNED_1 = "Everything's heavy. Even the dark is wet.";
    public static final String ENTER_DROWNED_2 = "There's a house down there. It still has air in it.";
    public static final String ENTER_HALLWAY_1 = "I know this carpet.";
    public static final String ENTER_HALLWAY_2 = "It's the hallway from his old house. It just never ends.";
    public static final String ENTER_BACKUP_1 = "It's perfect.";
    public static final String ENTER_BACKUP_2 = "Nothing here is moving. Not even the grass.";
    public static final String ENTER_ARCHIVE_1 = "Books. Every wall, floor to ceiling.";
    public static final String ENTER_ARCHIVE_2 = "I opened one. Every page is blank. They all are.";
    public static final String ENTER_STREET_1 = "This is {friend}'s street.";
    public static final String ENTER_STREET_2 = "Every house is dark. Except one.";
    public static final String ENTER_STATIC_1 = "It's not fog. It's noise.";
    public static final String ENTER_STATIC_2 = "A channel nobody is broadcasting on. And I'm standing in it.";
    public static final String ENTER_OVERWORLD = "Back in the rain.";
    public static final String TAKEN = "Something is drawing over everything. Pencil. It's not stopping.";
    public static final String FALL = "I fell. I kept falling. And then I was back.";

    // ---- entities and events ----------------------------------------------------------
    public static final String ECHO_SEEN = "Someone's standing out there. In the rain.";
    public static final String ECHO_VANISH = "...{friend}?";
    public static final String ECHO_CLOSE = "Don't turn around.";
    public static final String LISTENER_NEAR = "Don't make a sound.";
    public static final String LISTENER_HIT = "It heard me.";
    public static final String REFLECTION = "That's how I walk.";
    public static final String REFLECTION_VANISH = "I don't remember walking like that.";
    public static final String MASON_SEEN = "Someone's building. Someone's finishing something.";
    public static final String MASON_HURT = "\"I wasn't done.\"";
    public static final String DOOR_BEHIND = "That door wasn't there a minute ago.";
    public static final String ARCHIVIST_SEEN = "It's reading. It hasn't turned the page since I got here.";
    public static final String ARCHIVIST_TOUCH = "It put something in my hand. A page. I can't read it.";
    public static final String NEIGHBOUR_SEEN = "Someone's under the streetlight. They're not looking at the house. They're looking at me.";
    public static final String NEIGHBOUR_KNOCK = "It knocked. There's no door. It knocked anyway, and left me something.";
    public static final String NEIGHBOUR_GONE = "A door closed. Somewhere down the street. Then nobody.";
    public static final String CARRIER_MOVED = "The screen was in front of me a second ago.";
    public static final String CARRIER_GONE = "The screen went dark. I think it was showing me.";
    public static final String SILENCE = "It stopped. Why is that worse?";
    public static final String SILENCE_END = "There it is again.";
    public static final String CIRCLE = "They're all facing me.";
    public static final String GHOST_GRAVE = "That wasn't there. Was it?";
    public static final String FAKE_JOIN = "...";

    public static final String[] GRAVE = {
            "A stone with no name on it. He wouldn't have needed one.",
            "Someone kept the lantern lit.",
            "I'm sorry I wasn't here.",
            "{friend}. I came back. I hope that counts.",
            "I don't know what to say to a stone."
    };
}
