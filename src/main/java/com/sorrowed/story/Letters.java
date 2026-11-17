package com.sorrowed.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The books. Pages stay under ~230 characters so they fit on one book page. */
public final class Letters {
    private Letters() {}

    private static final String[] WELCOME = {
            "If you're reading this, you got in, and I'm not here to say hi. I'm sorry about the rain. I didn't do that. It started the day I stopped logging in, and it hasn't stopped since.",
            "Everything looks older than it should. That's on purpose. I wanted you to see it the way we first did, before the nice textures. Ugly, crooked, ours.",
            "This was our world. The dirt house, the first night, the creeper you swore was a bush. I never deleted it. I left it open. I told myself it was a backup.",
            "Here's why I wanted you here. I never finished anything. Half-built houses, half-written messages. You were always the one who dragged me to the end of a thing.",
            "I can't finish this one. So I'm asking. Walk it once more. Find the house by the trees, the stone gates standing in the rain, the stones I left where you'd look.",
            "The gates lead somewhere dry. I built that place first, back when I thought there was time. It's empty. Don't be scared of that. Empty isn't the same as abandoned.",
            "There's a second gate past that one. I didn't build it. I think the world did, after I was gone. It's quiet there. Be careful. The quiet remembers.",
            "If someone is standing far off in the rain, don't run. It's only an echo of me, or what the world thinks I was. It can't hurt you. It just doesn't know how to leave.",
            "The ones with no faces are different. They're everything I never said. Don't let them hold you. Keep walking.",
            "Thank you for coming back. Finish it, or don't. Just keep it a little loud in here for me. - {friend}"
    };

    private static final String[] NOTES = {
            "TODO\n\n- finish the roof\n- finish the door\n- finish the tower\n- tell them\n- tell them\n- tell them",
            "The rain started when the server went quiet. I think this place only knows how to wait. It's learning how to miss people.",
            "If you find the dry place, don't stay long. It's the part I never filled in. A world with nothing in it is the loudest kind of quiet.",
            "I'll leave the lantern on. In case you forget the way back."
    };

    private static final String[] LAST = {
            "I didn't get to finish. You did. That was the whole point.\n\nGo home. Leave the door open for whoever comes next.\n\n- {friend}"
    };

    private static final String[] KEEPER = {
            "THE KEEPER'S LOG\n\nThe lamp has never been lit. I keep it dark on purpose.",
            "If I light it, someone will see this place from the sea. And then they will come looking for what is in it.",
            "Day 1. Day 2. Day 3. The rain has no day. I stopped counting. {friend} would have kept counting.",
            "The ladder is the only way up. The only way down is the same ladder. I think that is the point."
    };

    private static final String[] NAMES = {
            "NAMES\n\n{friend}\n\n(crossed out)\n(crossed out)\n(crossed out)",
            "These are the people who stayed. Most of them did not stay long.",
            "The last name on the page is yours. I wrote it in pencil, in case you wanted out."
    };

    private static final String[] TICKET = {
            "ONE WAY\n\nDeparts: never\nArrives: here\nPassenger: you",
            "Please remain seated. The bus is coming. The bus has always been coming."
    };

    private static final String[] UNSAID = {
            "WHAT HE NEVER SAID\n\nI'm sorry I stopped answering.",
            "I'm sorry I said I was fine.",
            "I'm sorry I made you carry the quiet.",
            "I'm not sorry I let you in.",
            "(The rest of the pages are blank. Maybe you can fill them in.)"
    };

    private static final String[] REPLY = {
            "THE REPLY\n\nI knew you'd come. I set the table, just in case.",
            "This place is only what I remember, so it is smaller than it should be. Nothing here can hurt you.",
            "That is how you know it isn't me. I would have found some way to hurt you. I always did. - {friend}"
    };

    public static ItemStack keeperLog() {
        return written("The Keeper's Log", KEEPER);
    }

    public static ItemStack names() {
        return written("Names", NAMES);
    }

    public static ItemStack ticket() {
        return written("One Way", TICKET);
    }

    public static ItemStack unsaid() {
        return written("What He Never Said", UNSAID);
    }

    public static ItemStack reply() {
        return written("The Reply", REPLY);
    }

    /** The Book and Quill given on first join. */
    public static ItemStack welcomeBook() {
        ItemStack stack = new ItemStack(Items.WRITABLE_BOOK);
        ListTag pages = new ListTag();
        for (String page : WELCOME) {
            pages.add(StringTag.valueOf(Thoughts.f(page)));
        }
        stack.getOrCreateTag().put("pages", pages);
        return stack;
    }

    public static ItemStack notesBook() {
        return written("Unfinished Notes", NOTES);
    }

    public static ItemStack lastPage() {
        return written("The Last Page", LAST);
    }

    private static ItemStack written(String title, String[] pageTexts) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("title", title);
        tag.putString("author", Thoughts.f("{friend}"));
        ListTag pages = new ListTag();
        for (String page : pageTexts) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(Thoughts.f(page)))));
        }
        tag.put("pages", pages);
        tag.putBoolean("resolved", true);
        return stack;
    }
}
